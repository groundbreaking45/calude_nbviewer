package com.nbviewer.domain.usecase

import com.nbviewer.domain.model.Notebook
import com.nbviewer.domain.model.NotebookSource
import com.nbviewer.domain.repository.INotebookRepository

/**
 * Loads and returns a [Notebook] from the given [NotebookSource].
 *
 * This use case is the single public entry point the Presentation layer uses
 * to load a notebook. It delegates fully to [INotebookRepository].
 *
 * In v1, the use case adds no additional logic beyond delegation. The indirection
 * is preserved because use cases are the correct location for future rules such as:
 *   - Input validation (e.g., reject URIs with unsupported schemes)
 *   - Pre/post-load analytics events
 *   - Telemetry hooks
 *   - Cache-check-before-load logic
 *
 * INVOCATION: Called via operator fun invoke() — idiomatic Kotlin use case pattern.
 *
 *   val notebook = loadNotebookUseCase(NotebookSource.UriSource(uri.toString()))
 *
 * THREADING: The use case is not responsible for dispatchers. The ViewModel
 * wraps the call in viewModelScope.launch and the repository owns its dispatcher.
 */
class LoadNotebookUseCase(
    private val repository: INotebookRepository
) {
    suspend operator fun invoke(source: NotebookSource): Result<Notebook> {
        return repository.loadNotebook(source)
    }
}
