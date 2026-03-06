package com.nbviewer.presentation.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nbviewer.domain.model.Cell
import com.nbviewer.domain.model.CellOutput
import com.nbviewer.domain.model.Notebook
import com.nbviewer.domain.model.NotebookSource
import com.nbviewer.domain.usecase.LoadNotebookUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotebookViewerViewModel(
    private val loadNotebookUseCase: LoadNotebookUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotebookUiState>(NotebookUiState.Idle)
    val uiState: StateFlow<NotebookUiState> = _uiState

    fun load(source: NotebookSource) {
        if (_uiState.value is NotebookUiState.Loading) return
        _uiState.value = NotebookUiState.Loading

        viewModelScope.launch {
            loadNotebookUseCase(source)
                .onSuccess { notebook ->
                    _uiState.value = NotebookUiState.Success(
                        cells = mapCells(notebook)
                    )
                }
                .onFailure { error ->
                    _uiState.value = NotebookUiState.Error(
                        message = error.message ?: "Failed to load notebook."
                    )
                }
        }
    }

    private fun mapCells(notebook: Notebook): List<CellUiModel> =
        notebook.cells.mapNotNull { cell ->
            when (cell) {
                is Cell.Markdown -> CellUiModel.MarkdownCell(
                    id = cell.id,
                    rawMarkdown = cell.source
                )
                is Cell.Code -> CellUiModel.CodeCell(
                    id = cell.id,
                    source = cell.source,
                    language = notebook.language,
                    executionCount = cell.executionCount?.let { "[$it]" } ?: "[ ]",
                    outputs = cell.outputs.mapNotNull { output ->
                        when (output) {
                            is CellOutput.Stream -> OutputUiModel.TextOutput(output.text)
                            is CellOutput.ExecuteResult ->
                                output.mimeData["text/plain"]?.let {
                                    OutputUiModel.TextOutput(it)
                                }
                            is CellOutput.DisplayData ->
                                output.mimeData["text/plain"]?.let {
                                    OutputUiModel.TextOutput(it)
                                } ?: output.mimeData.keys.firstOrNull()?.let {
                                    OutputUiModel.ImageOutput(it)
                                }
                            is CellOutput.Error -> OutputUiModel.ErrorOutput(
                                ename = output.ename,
                                evalue = output.evalue,
                                traceback = output.traceback
                            )
                            is CellOutput.Unknown -> null
                        }
                    }
                )
                is Cell.Raw -> CellUiModel.RawCell(
                    id = cell.id,
                    source = cell.source
                )
                is Cell.Unknown -> null
            }
        }
}