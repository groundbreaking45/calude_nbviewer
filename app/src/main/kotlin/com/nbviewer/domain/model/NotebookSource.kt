package com.nbviewer.domain.model

/**
 * Represents the origin of a notebook file.
 *
 * DESIGN (ADR-006): The domain layer must not import android.net.Uri.
 * [UriSource] stores the Uri as a String (via Uri.toString()).
 * The data layer reconstructs the Uri via Uri.parse(uriString).
 *
 * This keeps the domain 100% JVM-testable: unit tests pass a [UriSource]
 * with any string and verify business logic without Android SDK.
 *
 * EXTENSION: [PathSource] is reserved for future direct-path access
 * (e.g., a files-of-authority scoped directory or internal storage).
 * It is not used in v1 but the sealed hierarchy accommodates it without
 * changing INotebookRepository's signature.
 */
sealed class NotebookSource {

    /**
     * A file identified by a content URI (from SAF or intent).
     * @param uriString The result of android.net.Uri.toString().
     */
    data class UriSource(val uriString: String) : NotebookSource()

    /**
     * A file identified by a direct filesystem path.
     * Reserved for future use — not active in v1.
     */
    data class PathSource(val path: String) : NotebookSource()
}
