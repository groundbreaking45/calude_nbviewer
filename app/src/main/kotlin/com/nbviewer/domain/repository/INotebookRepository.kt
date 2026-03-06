package com.nbviewer.domain.repository

import com.nbviewer.domain.model.Notebook
import com.nbviewer.domain.model.NotebookSource

/**
 * The domain's contract for loading a notebook from any source.
 *
 * Implemented by:
 *   - [com.nbviewer.data.repository.NotebookRepositoryImpl] (real, M3)

 *
 * DESIGN NOTES:
 * - Returns [Result<Notebook>] rather than throwing. The use case and ViewModel
 *   handle failure paths explicitly without try/catch at the call site.
 * - [NotebookSource] is a sealed class — not android.net.Uri — so this interface
 *   has zero Android SDK imports and is fully JVM-testable (ADR-006).
 * - suspend: all implementations must perform I/O on a background dispatcher.
 *   The interface does not prescribe which dispatcher — implementations own that.
 *
 * EXTENSION POINT: A caching decorator wraps this interface without modifying
 * any existing implementation (see extension_points in project_state.json).
 */
interface INotebookRepository {
    suspend fun loadNotebook(source: NotebookSource): Result<Notebook>
}
