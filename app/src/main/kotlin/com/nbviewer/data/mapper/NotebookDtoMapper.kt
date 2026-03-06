package com.nbviewer.data.mapper

import com.nbviewer.data.model.CellDto
import com.nbviewer.data.model.NotebookDto
import com.nbviewer.data.model.NotebookMetadataDto
import com.nbviewer.data.model.OutputDto
import com.nbviewer.domain.model.Cell
import com.nbviewer.domain.model.CellOutput
import com.nbviewer.domain.model.Notebook
import com.nbviewer.domain.model.NotebookMetadata
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Maps [NotebookDto] → [Notebook] (domain model).
 *
 * This is the only place where DTO structure knowledge meets domain model structure.
 * All mapping decisions are documented inline.
 *
 * KEY CORRECTNESS DECISIONS:
 * 1. source is ALWAYS List<String> → joinToString("") with no separator.
 *    Each string in the list already ends with \n where needed. Do NOT add \n.
 * 2. Language resolved: kernelspec.language → language_info.name → "python"
 * 3. Cell IDs: index-based ("cell_0") in v1. CellDto.id field (nbformat 4.5+) used
 *    when present to maintain stable IDs across saves.
 * 4. Unknown cell types → Cell.Unknown (never crash, never silently drop).
 * 5. MIME data values can be String or List<String> in the JSON — both are handled.
 */
class NotebookDtoMapper {

    fun toDomain(dto: NotebookDto): Notebook {
        val language = resolveLanguage(dto.metadata)
        return Notebook(
            formatVersion = dto.nbformat,
            language = language,
            metadata = mapMetadata(dto.metadata),
            cells = dto.cells.mapIndexed { index, cell -> mapCell(cell, index) }
        )
    }

    // -------------------------------------------------------------------------
    // Private mapping
    // -------------------------------------------------------------------------

    private fun resolveLanguage(meta: NotebookMetadataDto): String =
        meta.kernelspec?.language?.takeIf { it.isNotBlank() }
            ?: meta.languageInfo?.name?.takeIf { it.isNotBlank() }
            ?: "python"

    private fun mapMetadata(meta: NotebookMetadataDto) = NotebookMetadata(
        kernelName = meta.kernelspec?.name,
        kernelDisplayName = meta.kernelspec?.displayName,
        languageName = meta.languageInfo?.name
    )

    private fun mapCell(dto: CellDto, index: Int): Cell {
        // Use nbformat 4.5+ id if present, else generate stable index-based id
        val id = dto.id?.takeIf { it.isNotBlank() } ?: "cell_$index"
        val source = dto.source.joinToString("")   // CRITICAL: no separator

        return when (dto.cellType) {
            "markdown" -> Cell.Markdown(id = id, source = source)
            "code" -> Cell.Code(
                id = id,
                source = source,
                executionCount = dto.executionCount,
                outputs = dto.outputs.map { mapOutput(it) }
            )
            "raw" -> Cell.Raw(id = id, source = source)
            else  -> Cell.Unknown(id = id, cellType = dto.cellType)
        }
    }

    private fun mapOutput(dto: OutputDto): CellOutput = when (dto.outputType) {
        "stream" -> CellOutput.Stream(
            text = dto.text.joinToString("")
        )
        "display_data" -> CellOutput.DisplayData(
            mimeData = flattenMimeData(dto.data)
        )
        "execute_result" -> CellOutput.ExecuteResult(
            mimeData = flattenMimeData(dto.data),
            executionCount = dto.executionCount
        )
        "error" -> CellOutput.Error(
            ename = dto.ename ?: "Error",
            evalue = dto.evalue ?: "",
            traceback = dto.traceback
                .joinToString("\n")
                .replace(Regex("\u001B\\[[;\\d]*m"), "") // strip ANSI color codes
        )
        else -> CellOutput.Unknown(outputType = dto.outputType)
    }

    /**
     * MIME data values in the JSON can be either:
     *   - A plain String:       "text/plain": "hello"
     *   - A list of strings:    "text/plain": ["hello\n", "world"]
     * Both forms must produce the same String result.
     */
    private fun flattenMimeData(data: Map<String, kotlinx.serialization.json.JsonElement>): Map<String, String> =
        data.mapValues { (_, element) ->
            when (element) {
                is JsonArray -> element.jsonArray.joinToString("") { item ->
                    (item as? JsonPrimitive)?.content ?: ""
                }
                is JsonPrimitive -> element.content
                else -> element.toString()
            }
        }
}
