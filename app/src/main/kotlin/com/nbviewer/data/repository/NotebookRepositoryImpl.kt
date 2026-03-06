package com.nbviewer.data.repository

import android.net.Uri
import com.nbviewer.data.mapper.NotebookDtoMapper
import com.nbviewer.data.parser.NotebookParser
import com.nbviewer.data.source.FileDataSource
import com.nbviewer.domain.model.Notebook
import com.nbviewer.domain.model.NotebookSource
import com.nbviewer.domain.repository.INotebookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real implementation of [INotebookRepository].
 *
 * Wires: FileDataSource → NotebookParser → NotebookDtoMapper → Notebook
 *
 * THREADING: All work runs on [Dispatchers.IO].
 * The interface contract requires suspend; this implementation owns the dispatcher.
 * The ViewModel launches on viewModelScope (Main) and this switches to IO internally.
 *
 * ERROR PROPAGATION:
 * mapCatching chains preserve the original exception type so the ViewModel's
 * formatError() can provide context-appropriate messages.
 */
class NotebookRepositoryImpl(
    private val fileDataSource: FileDataSource,
    private val parser: NotebookParser,
    private val mapper: NotebookDtoMapper
) : INotebookRepository {

    override suspend fun loadNotebook(source: NotebookSource): Result<Notebook> =
        withContext(Dispatchers.IO) {
            val uri = resolveUri(source)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Unsupported notebook source: $source")
                )

            fileDataSource.openStream(uri)
                .mapCatching { stream ->
                    stream.use { parser.parse(it).getOrThrow() }
                }
                .mapCatching { dto ->
                    mapper.toDomain(dto)
                }
        }

    /**
     * Converts [NotebookSource] to android.net.Uri.
     * This is the only place in the data layer where the source type is inspected.
     * PathSource is reserved for future use and returns null in v1.
     */
    private fun resolveUri(source: NotebookSource): Uri? = when (source) {
        is NotebookSource.UriSource -> runCatching { Uri.parse(source.uriString) }.getOrNull()
        is NotebookSource.PathSource -> runCatching { Uri.parse(source.path) }.getOrNull()
    }
}
