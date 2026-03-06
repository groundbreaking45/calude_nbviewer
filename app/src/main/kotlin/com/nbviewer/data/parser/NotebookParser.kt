package com.nbviewer.data.parser

import com.nbviewer.data.model.NotebookDto
import kotlinx.serialization.json.Json
import java.io.InputStream

/**
 * Parses a raw .ipynb InputStream into a [NotebookDto].
 *
 * Configuration:
 *   ignoreUnknownKeys = true  → forward-compat with nbformat 5+, custom metadata
 *   isLenient           = true  → tolerates trailing commas, unquoted keys (rare but real)
 *   coerceInputValues   = true  → null for unexpected types rather than exception
 *
 * Returns Result<NotebookDto> — never throws. All errors are captured in the failure path.
 * The caller (NotebookRepositoryImpl) propagates the Result up to the ViewModel.
 */
class NotebookParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parse(input: InputStream): Result<NotebookDto> = runCatching {
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }

        if (text.isBlank()) {
            error("File is empty.")
        }

        if (!text.trimStart().startsWith('{')) {
            error("File does not appear to be a Jupyter Notebook. Expected JSON object.")
        }

        val dto = json.decodeFromString<NotebookDto>(text)

        if (dto.cells.isEmpty() && dto.nbformat < 4) {
            error("Unsupported notebook format version: ${dto.nbformat}. Expected nbformat 4+.")
        }

        dto
    }
}
