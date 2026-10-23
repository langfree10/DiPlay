package com.shilapi.xcertplay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import java.io.IOException

/** Writes only under a folder selected by the driver; works with USB document providers offline. */
internal object DiagnosticFolderExport {
    private const val PREFS = "diplay"
    private const val KEY = "diagnostic_folder_uri"
    private const val DIRECTORY = "DiPlay"
    fun remembered(context: Context): Uri? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY, null)?.let(Uri::parse)

    fun remember(context: Context, tree: Uri) {
        // Failure to persist a grant must not prevent this one user-authorized export.
        runCatching {
            context.contentResolver.takePersistableUriPermission(tree,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }.onSuccess {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tree.toString()).apply()
        }
    }

    fun write(context: Context, tree: Uri, fileName: String, text: String): Uri {
        require(fileName.endsWith(".txt") && '/' !in fileName && '\\' !in fileName)
        val resolver = context.contentResolver
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE)
        val cursor = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            resolver.query(children, projection, Bundle.EMPTY, null)
        else resolver.query(children, projection, null, null, null))
            ?: throw IOException("Selected folder could not be listed")
        val folder = cursor.use { cursor ->
            var found: Uri? = null
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == DIRECTORY && cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                    found = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
                    break
                }
            }
            found
        } ?: DocumentsContract.createDocument(resolver, root, DocumentsContract.Document.MIME_TYPE_DIR, DIRECTORY)
            ?: throw IOException("Selected folder is unavailable")
        val created = DocumentsContract.createDocument(resolver, folder, "text/plain", fileName)
            ?: throw IOException("Selected folder could not create the report")
        try { DiagnosticExportStore.write(resolver, created, text) }
        catch (error: Exception) {
            // Remove only the incomplete file created by this invocation. Local copy stays intact.
            runCatching { DocumentsContract.deleteDocument(resolver, created) }
            throw error
        }
        return created
    }
}
