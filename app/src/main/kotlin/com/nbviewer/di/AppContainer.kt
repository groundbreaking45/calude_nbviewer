package com.nbviewer.di

import android.content.Context
import com.nbviewer.data.mapper.NotebookDtoMapper
import com.nbviewer.data.parser.NotebookParser
import com.nbviewer.data.repository.NotebookRepositoryImpl
import com.nbviewer.data.source.FileDataSource
import com.nbviewer.domain.usecase.LoadNotebookUseCase

/**
 * Manual dependency injection container (ADR-001).
 *
 * Constructs the complete object graph bottom-up:
 *   FileDataSource + NotebookParser + NotebookDtoMapper
 *     → NotebookRepositoryImpl
 *       → LoadNotebookUseCase
 *         → exposed to Presentation layer
 *
 * UPGRADE PATH: When Hilt is adopted, replace this class with @Module/@Provides.
 * All constructor signatures are already injection-ready.
 */
class AppContainer(context: Context) {

    // -------------------------------------------------------------------------
    // Data layer
    // -------------------------------------------------------------------------

    private val fileDataSource = FileDataSource(context)
    private val parser = NotebookParser()
    private val mapper = NotebookDtoMapper()

    private val repository = NotebookRepositoryImpl(
        fileDataSource = fileDataSource,
        parser = parser,
        mapper = mapper
    )

    // -------------------------------------------------------------------------
    // Domain layer — exposed to Presentation
    // -------------------------------------------------------------------------

    val loadNotebookUseCase = LoadNotebookUseCase(repository)
}
