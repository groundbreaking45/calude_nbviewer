package com.nbviewer.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Data Transfer Objects for .ipynb JSON deserialization.
 *
 * All fields have defaults so missing keys never crash the parser (forward-compat).
 * Unknown fields are dropped via ignoreUnknownKeys=true in NotebookParser.
 *
 * Source field is always List<String> in the spec — lines must be joined with "".
 * This is the single most common parsing bug in notebook readers.
 */

@Serializable
data class NotebookDto(
    val nbformat: Int = 4,
    @SerialName("nbformat_minor") val nbformatMinor: Int = 0,
    val metadata: NotebookMetadataDto = NotebookMetadataDto(),
    val cells: List<CellDto> = emptyList()
)

@Serializable
data class NotebookMetadataDto(
    val kernelspec: KernelSpecDto? = null,
    @SerialName("language_info") val languageInfo: LanguageInfoDto? = null
)

@Serializable
data class KernelSpecDto(
    val language: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val name: String? = null
)

@Serializable
data class LanguageInfoDto(
    val name: String? = null
)

@Serializable
data class CellDto(
    @SerialName("cell_type") val cellType: String,
    val source: List<String> = emptyList(),
    val metadata: JsonObject = JsonObject(emptyMap()),
    val outputs: List<OutputDto> = emptyList(),
    @SerialName("execution_count") val executionCount: Int? = null,
    val id: String? = null   // nbformat 4.5+ optional field
)

@Serializable
data class OutputDto(
    @SerialName("output_type") val outputType: String = "",
    val text: List<String> = emptyList(),
    val data: Map<String, JsonElement> = emptyMap(),
    val ename: String? = null,
    val evalue: String? = null,
    val traceback: List<String> = emptyList(),
    @SerialName("execution_count") val executionCount: Int? = null
)
