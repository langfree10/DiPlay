package com.shilapi.xcertplay

import android.content.pm.ProviderInfo
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import java.io.File
import java.io.IOException
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class DiagnosticFolderExportTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var provider: UsbProvider
    private val tree = DocumentsContract.buildTreeDocumentUri("test.usb", "root")
    @Before fun setup() {
        provider = UsbProvider(File(context.cacheDir, "usb-report.txt"))
        provider.attachInfo(context, ProviderInfo().apply {
            authority = "test.usb"; exported = true; grantUriPermissions = true
            readPermission = "android.permission.MANAGE_DOCUMENTS"
            writePermission = readPermission
        })
        ShadowContentResolver.registerProviderInternal("test.usb", provider)
    }
    @Test fun offlineExportCreatesOnlyDiPlayAndWritesUtf8WithoutTouchingExistingFiles() {
        val report = "伊兰特 Siri 诊断\n音频：蓝牙\n"
        val uri = DiagnosticFolderExport.write(context, tree, "DiPlay-test.txt", report)
        assertEquals("new-report", DocumentsContract.getDocumentId(uri))
        assertEquals(report, provider.file.readText())
        assertEquals(listOf("root:DiPlay", "diplay:DiPlay-test.txt"), provider.created)
        assertTrue(provider.deleted.isEmpty())
    }
    @Test fun reusesExistingFolderInsteadOfCreatingAnotherOne() {
        provider.hasFolder = true
        DiagnosticFolderExport.write(context, tree, "report.txt", "report")
        assertEquals(listOf("diplay:report.txt"), provider.created)
    }
    @Test fun deniedWriteDeletesOnlyNewPartialReportAndPropagatesFailure() {
        provider.denyWrite = true
        assertThrows(SecurityException::class.java) { DiagnosticFolderExport.write(context, tree, "report.txt", "report") }
        assertEquals(listOf("new-report"), provider.deleted)
    }
    @Test fun unpluggedProviderCannotReportSuccessfulExport() {
        provider.offline = true
        assertThrows(IOException::class.java) { DiagnosticFolderExport.write(context, tree, "report.txt", "report") }
        assertTrue(provider.created.isEmpty()); assertTrue(provider.deleted.isEmpty())
    }
    private class UsbProvider(val file: File) : DocumentsProvider() {
        var hasFolder = false
        var denyWrite = false
        var offline = false
        val created = mutableListOf<String>()
        val deleted = mutableListOf<String>()
        override fun onCreate() = true
        override fun queryRoots(projection: Array<out String>?) = MatrixCursor(projection ?: emptyArray())
        override fun queryDocument(documentId: String, projection: Array<out String>?) = documents(projection)
        override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): MatrixCursor {
            if (offline) throw IOException("USB removed")
            return documents(projection).apply { if (hasFolder) addRow(arrayOf("diplay", "DiPlay", DocumentsContract.Document.MIME_TYPE_DIR)) }
        }
        private fun documents(projection: Array<out String>?) = MatrixCursor(projection ?: arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE))
        override fun isChildDocument(parentDocumentId: String, documentId: String) = true
        override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
            created += "$parentDocumentId:$displayName"
            if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) { hasFolder = true; return "diplay" }
            return "new-report"
        }
        override fun deleteDocument(documentId: String) { deleted += documentId; file.delete() }
        override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
            assertEquals("new-report", documentId)
            if (denyWrite) throw SecurityException("read-only USB")
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_CREATE or
                ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_TRUNCATE)
        }
    }
}
