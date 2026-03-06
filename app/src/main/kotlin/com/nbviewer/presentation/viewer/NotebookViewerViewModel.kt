package com.nbviewer.presentation.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nbviewer.domain.model.Cell
import com.nbviewer.domain.model.CellOutput
import com.nbviewer.domain.model.Notebook
import com.nbviewer.domain.model.NotebookSource
import com.nbviewer.domain.usecase.LoadNotebookUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the notebook viewer screen.
 *
 * RESPONSIBILITIES:
 *   1. Accepts a [NotebookSource] from the Fragment (never a raw Uri).
 *   2. Calls [LoadNotebookUseCase] on viewModelScope (survives rotation).
 *   3. Maps [Notebook] domain model → List<[CellUiModel]>.
 *   4. Emits [NotebookUiState] via [StateFlow] — single source of truth.
 *
 * LIFECYCLE SAFETY:
 *   - [load] guards against concurrent loads: if Loading is already in progress,
 *     the new call is ignored. This prevents double-triggers from orientation change.
 *   - viewModelScope is cancelled when the ViewModel is cleared — no leaks.
 *   - The Fragment collects uiState in a lifecycle-aware coroutine (repeatOnLifecycle).
 *
 * INTENT HANDLING:
 *   [loadFromIntent] is the entry point when the app is opened via an external
 *   ACTION_VIEW intent (e.g., tapping a .ipynb in a file manager).
 *   It converts the intent URI string directly to a NotebookSource.
 */
class NotebookViewerViewModel(
    private val loadNotebook: LoadNotebookUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotebookUiState>(NotebookUiState.Idle)
    val uiState: StateFlow<NotebookUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Request a notebook load from [source].
     *
     * Idempotent while loading: concurrent calls are dropped.
     * Safe to call from Fragment.onViewCreated or HomeFragment button click.
     */
    fun load(source: NotebookSource) {
        if (_uiState.value is NotebookUiState.Loading) return

        _uiState.value = NotebookUiState.Loading

        viewModelScope.launch {
            loadNotebook(source)
                .onSuccess { notebook ->
                    _uiState.value = NotebookUiState.Success(
                        cells = notebook.toCellUiModels()
                    )
                }
                .onFailure { error ->
                    _uiState.value = NotebookUiState.Error(
                        message = formatError(error)
                    )
                }
        }
    }

    /**
     * Entry point for ACTION_VIEW intents.
     * [uriString] comes from intent.data.toString() in MainActivity.
     * Returns false if the URI string is null or blank (caller should show error).
     */
    fun loadFromIntent(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        load(NotebookSource.UriSource(uriString))
        return true
    }

    // -------------------------------------------------------------------------
    // Private mapping — domain → UI models
    // -------------------------------------------------------------------------

    private fun Notebook.toCellUiModels(): List<CellUiModel> =
        cells.mapNotNull { cell ->
            when (cell) {
                is Cell.Markdown -> CellUiModel.MarkdownCell(
                    id = cell.id,
                    rawMarkdown = cell.source
                )
                is Cell.Code -> CellUiModel.CodeCell(
                    id = cell.id,
                    source = cell.source,
                    language = language,            // from Notebook, not cell
                    executionCount = cell.executionCount.toLabel(),
                    outputs = cell.outputs.toOutputUiModels()
                )
                is Cell.Raw -> CellUiModel.RawCell(
                    id = cell.id,
                    source = cell.source
                )
                is Cell.Unknown -> null             // silently drop unknowns in UI
            }
        }

    private fun List<CellOutput>.toOutputUiModels(): List<OutputUiModel> =
        mapNotNull { output ->
            when (output) {
                is CellOutput.Stream -> OutputUiModel.TextOutput(output.text)
                is CellOutput.DisplayData -> {
                    val text = output.mimeData["text/plain"]
                    if (text != null) OutputUiModel.TextOutput(text)
                    else OutputUiModel.ImageOutput(
                        mimeType = output.mimeData.keys.firstOrNull() ?: "unknown"
                    )
                }
                is CellOutput.ExecuteResult -> {
                    val text = output.mimeData["text/plain"]
                    if (text != null) OutputUiModel.TextOutput(text)
                    else OutputUiModel.ImageOutput(
                        mimeType = output.mimeData.keys.firstOrNull() ?: "unknown"
                    )
                }
                is CellOutput.Error -> OutputUiModel.ErrorOutput(
                    ename = output.ename,
                    evalue = output.evalue,
                    traceback = output.traceback
                )
                is CellOutput.Unknown -> null       // drop unrecognized outputs
            }
        }

    private fun Int?.toLabel(): String = if (this != null) "[$this]" else "[ ]"

    private fun formatError(error: Throwable): String = when {
        error.message?.contains("JSON") == true ->
            "This file does not appear to be a valid Jupyter Notebook (.ipynb)."
        error.message?.contains("null") == true ->
            "Could not open the file. Please try again."
        else ->
            "Failed to load notebook: ${error.message ?: "Unknown error"}"
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    companion object {
        /**
         * Factory for creating the ViewModel with [LoadNotebookUseCase] injected.
         * Called from Fragment: viewModels { NotebookViewerViewModel.factory(useCase) }
         */
        fun factory(useCase: LoadNotebookUseCase): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    NotebookViewerViewModel(useCase) as T
            }
    }
}
