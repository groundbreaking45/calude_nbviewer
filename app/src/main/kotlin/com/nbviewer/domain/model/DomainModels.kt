package com.nbviewer.domain.model

// =============================================================================
// Notebook.kt — top-level domain model
// =============================================================================

/**
 * The fully-mapped domain representation of a .ipynb file.
 *
 * [language] is resolved by the mapper in priority order:
 *   kernelspec.language → language_info.name → "python"
 *
 * [cells] contains only domain-meaningful cells. Unknown cell types are
 * preserved as [Cell.Unknown] so no data is silently dropped.
 */
data class Notebook(
    val formatVersion: Int,
    val language: String,
    val cells: List<Cell>,
    val metadata: NotebookMetadata
)

data class NotebookMetadata(
    val kernelName: String?,
    val kernelDisplayName: String?,
    val languageName: String?
)

// =============================================================================
// Cell.kt — sealed hierarchy for all cell types
// =============================================================================

/**
 * Sealed hierarchy representing a single notebook cell.
 *
 * [id] is a stable string key used as the DiffUtil identity.
 * In v1: "cell_{index}" (ADR-007). Future: use nbformat 4.5 'id' field.
 *
 * COMPILER ENFORCEMENT: All when() expressions over Cell are exhaustive.
 * Adding a new variant forces every call site to handle it.
 */
sealed class Cell {
    abstract val id: String

    /** A Markdown-formatted text cell. [source] is the joined raw Markdown string. */
    data class Markdown(
        override val id: String,
        val source: String
    ) : Cell()

    /**
     * An executable code cell. NbViewer displays [source] as read-only syntax-highlighted text.
     * [outputs] contains any output captured when the notebook was last run.
     * NbViewer never executes code — outputs are display-only.
     */
    data class Code(
        override val id: String,
        val source: String,
        val executionCount: Int?,
        val outputs: List<CellOutput>
    ) : Cell()

    /** A raw (unformatted) text cell. Displayed as plain monospace text. */
    data class Raw(
        override val id: String,
        val source: String
    ) : Cell()

    /**
     * A cell type not recognized by this version of the mapper.
     * Preserves the [cellType] string for diagnostic display.
     * Ensures forward-compat: new nbformat cell types don't crash the app.
     */
    data class Unknown(
        override val id: String,
        val cellType: String
    ) : Cell()
}

// =============================================================================
// CellOutput.kt — sealed hierarchy for code cell outputs
// =============================================================================

/**
 * Sealed hierarchy for outputs attached to [Cell.Code].
 *
 * Matches the nbformat output_type values:
 *   stream, display_data, execute_result, error
 *
 * [mimeData] in [DisplayData] and [ExecuteResult] is a Map<mimeType, textContent>.
 * v1 renders only "text/plain". Future: render "image/png", "text/html".
 */
sealed class CellOutput {

    /** stdout/stderr stream output. */
    data class Stream(val text: String) : CellOutput()

    /**
     * Rich display output. [mimeData] key is MIME type (e.g., "text/plain", "image/png").
     * v1 renders text/plain only; other MIME types produce a placeholder.
     */
    data class DisplayData(val mimeData: Map<String, String>) : CellOutput()

    /**
     * Output of an execute_result. Functionally identical to DisplayData for display purposes.
     * Kept separate to preserve semantic distinction for future export features.
     */
    data class ExecuteResult(
        val mimeData: Map<String, String>,
        val executionCount: Int?
    ) : CellOutput()

    /** A Python exception raised during execution. Displayed as a formatted error block. */
    data class Error(
        val ename: String,
        val evalue: String,
        val traceback: String     // pre-joined with \n by mapper
    ) : CellOutput()

    /** Unrecognized output type — preserved for diagnostic display. */
    data class Unknown(val outputType: String) : CellOutput()
}
