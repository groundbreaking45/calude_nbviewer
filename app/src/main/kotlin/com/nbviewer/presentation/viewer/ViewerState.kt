package com.nbviewer.presentation.viewer

// =============================================================================
// NotebookUiState.kt
// =============================================================================

/**
 * Sealed hierarchy representing every possible UI state for the notebook viewer.
 *
 * The ViewModel emits these via StateFlow<NotebookUiState>. The Fragment
 * collects them and drives the UI — it contains zero conditional logic beyond
 * switching on the state type.
 *
 * STATE MACHINE:
 *   Idle → Loading → Success
 *                  → Error
 *   Error → Loading (user retries or picks a new file)
 */
sealed class NotebookUiState {

    /** Initial state. No file has been requested. Shows the empty home screen. */
    object Idle : NotebookUiState()

    /**
     * A file is being read and parsed. Show progress indicator.
     * No additional data: the viewer should display a spinner only.
     */
    object Loading : NotebookUiState()

    /**
     * Parsing succeeded. [cells] is the ordered list of UI-ready cell models.
     * The adapter receives this list and DiffUtil handles incremental updates.
     */
    data class Success(val cells: List<CellUiModel>) : NotebookUiState()

    /**
     * Parsing or file access failed. [message] is a user-displayable string.
     * The ViewModel formats error messages; the Fragment displays them verbatim.
     */
    data class Error(val message: String) : NotebookUiState()
}

// =============================================================================
// CellUiModel.kt — flat, UI-ready representations of domain cells
// =============================================================================

/**
 * Sealed hierarchy of cell types as the presentation layer understands them.
 *
 * These are NOT domain models. The ViewModel maps [Cell] → [CellUiModel],
 * performing all formatting decisions (execution count label, language string).
 * The adapter and ViewHolders consume only [CellUiModel] — never domain types.
 *
 * [id] is the stable DiffUtil identity key, sourced from [Cell.id].
 */
sealed class CellUiModel {
    abstract val id: String

    /**
     * A Markdown cell.
     * [rawMarkdown] is passed directly to Markwon at bind time.
     * Pre-rendering is a future optimization (see M5 notes).
     */
    data class MarkdownCell(
        override val id: String,
        val rawMarkdown: String
    ) : CellUiModel()

    /**
     * A code cell.
     * [language] drives syntax highlighting token patterns.
     * [executionCount] is already formatted: "[3]", "[ ]" for null.
     * [outputs] are UI-ready output models, in display order.
     */
    data class CodeCell(
        override val id: String,
        val source: String,
        val language: String,
        val executionCount: String,
        val outputs: List<OutputUiModel>
    ) : CellUiModel()

    /** A raw cell — displayed as plain monospace text. */
    data class RawCell(
        override val id: String,
        val source: String
    ) : CellUiModel()
}

// =============================================================================
// OutputUiModel.kt — UI-ready output representations
// =============================================================================

/**
 * UI-ready representation of a code cell output.
 *
 * v1 renders [TextOutput] and [ErrorOutput].
 * [ImageOutput] and [HtmlOutput] are extension points — their sealed variants
 * are defined here so the adapter doesn't need structural changes when added.
 */
sealed class OutputUiModel {

    /** Plain text output (stream stdout/stderr, or text/plain mime data). */
    data class TextOutput(val text: String) : OutputUiModel()

    /** Python exception block. Displayed with distinct error styling. */
    data class ErrorOutput(
        val ename: String,
        val evalue: String,
        val traceback: String
    ) : OutputUiModel()

    /**
     * Image output (base64 PNG/JPEG from display_data or execute_result).
     * NOT rendered in v1 — ViewHolder shows "[Image output]" placeholder.
     * Adding real rendering requires only updating the ViewHolder.
     */
    data class ImageOutput(val mimeType: String) : OutputUiModel()
}
