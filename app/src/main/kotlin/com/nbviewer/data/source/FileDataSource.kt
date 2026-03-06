package com.nbviewer.data.source

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.InputStream

/**
 * Wraps Android ContentResolver for SAF-based file access.
 *
 * This is the only class in the project that touches android.net.Uri directly.
 * All other layers use NotebookSource (domain abstraction) or String URI representations.
 *
 * ContentResolver.openInputStream returns null rather than throwing for some failure modes
 * (e.g., revoked URI permission) — we convert null to a meaningful error.
 */
class FileDataSource(private val context: Context) {

    /**
     * Opens an InputStream for [uri]. The caller is responsible for closing it.
     * Uses runCatching to capture SecurityException (revoked permission),
     * FileNotFoundException, and other I/O failures.
     */
    fun openStream(uri: Uri): Result<InputStream> = runCatching {
        context.contentResolver.openInputStream(uri)
            ?: error("System returned no stream for this file. The file may have been moved or deleted.")
    }

    /**
     * Reads the display name of the file at [uri] from the ContentResolver.
     * Returns null if the query fails (e.g., file from a cloud provider not yet synced).
     */
    fun getDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
}
