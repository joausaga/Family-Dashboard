package com.joausaga.familydashboard.data.backup

import com.joausaga.familydashboard.data.cache.Cached
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

class BackupOperationsTest {
    private class Documents : BackupDocuments {
        val files = mutableMapOf<String, ByteArray>()
        var writes = 0
        var failWrite = false
        var failRead = false
        override fun openRead(document: String): ByteArrayInputStream {
            if (failRead) throw IOException("permission denied")
            return ByteArrayInputStream(files[document] ?: throw IOException("missing"))
        }
        override fun openWrite(document: String): OutputStream {
            writes++
            if (failWrite) throw IOException("disk full")
            return object : ByteArrayOutputStream() {
                override fun close() { files[document] = toByteArray(); super.close() }
            }
        }
    }
    private class Fixture(scope: CoroutineScope) {
        var state = BackupUiState()
        val documents = Documents()
        val restored = mutableListOf<BackupSelection>()
        var recoveryCalls = 0
        val operations = BackupOperations(scope, documents, { state }, { state = it },
            snapshot = { BackupSnapshot(backupConfig(), Cached(backupHistory(), backupTime.toEpochMilli())) },
            restore = { document, selection -> assertEquals(backupHistory(), document.tvHistory); restored += selection },
            recover = { recoveryCalls++ }, recoveryPending = { false })
    }
    private suspend fun CoroutineScope.finish() { coroutineContext[Job]!!.children.toList().joinAll() }

    @Test fun cancelledExportOrPickerFailureDoesNotWriteOrLeaveBusyState() = runBlocking {
        val f = Fixture(this)
        f.operations.requestExport()
        assertTrue(f.state.confirmExport)
        f.operations.dismiss()
        assertFalse(f.state.active)
        f.operations.requestExport()
        f.operations.prepareExport { assertTrue(it.endsWith(".json")) }
        finish()
        f.operations.exportTo(null)
        assertEquals(0, f.documents.writes)
        assertFalse(f.state.active)
        assertTrue(f.operations.beginImport())
        f.operations.pickerFailed()
        assertTrue(f.state.error)
        assertFalse(f.state.busy)
    }

    @Test fun exportReportsSuccessOnlyAfterReadingBackTheSavedFile() = runBlocking {
        val f = Fixture(this)
        f.operations.requestExport()
        f.operations.prepareExport { }
        finish()
        f.operations.exportTo("out")
        finish()
        assertFalse(f.state.error)
        assertTrue(f.state.message!!.contains("verificada"))
        assertEquals(backupHistory(), BackupCodec().decode(f.documents.files.getValue("out")).tvHistory)
        assertTrue(f.restored.isEmpty())
    }

    @Test fun writeAndVerificationFailuresNeverReportExportSuccess() = runBlocking {
        for (writeFailure in listOf(true, false)) {
            val f = Fixture(this)
            f.operations.requestExport()
            f.operations.prepareExport { }
            finish()
            f.documents.failWrite = writeFailure
            f.documents.failRead = !writeFailure
            f.operations.exportTo("out")
            finish()
            assertTrue(f.state.error)
            assertTrue(f.state.message!!.contains("No uses el archivo"))
            assertFalse(f.state.busy)
        }
    }

    @Test fun openingValidCopyOnlyShowsPreviewAndCancelNeverRestores() = runBlocking {
        val f = Fixture(this)
        f.documents.files["in"] = BackupCodec().encode(backupDocument())
        assertTrue(f.operations.beginImport())
        f.operations.importFrom("in")
        finish()
        assertEquals(2, f.state.preview!!.transitions)
        assertEquals(1, f.state.preview!!.accounts)
        assertTrue(f.restored.isEmpty())
        f.operations.dismiss()
        assertNull(f.state.preview)
        assertTrue(f.restored.isEmpty())
        f.operations.restoreSelected(BackupSelection(true, true))
        finish()
        assertTrue(f.restored.isEmpty())
    }

    @Test fun onlyExplicitNonEmptySelectionRestoresAndOnlyOnce() = runBlocking {
        val f = Fixture(this)
        f.documents.files["in"] = BackupCodec().encode(backupDocument())
        f.operations.beginImport()
        f.operations.importFrom("in")
        finish()
        f.operations.restoreSelected(BackupSelection(false, false))
        assertTrue(f.restored.isEmpty())
        f.operations.restoreSelected(BackupSelection(true, false))
        finish()
        assertEquals(listOf(BackupSelection(true, false)), f.restored)
        assertNull(f.state.preview)
        f.operations.restoreSelected(BackupSelection(true, false))
        finish()
        assertEquals(1, f.restored.size)
    }

    @Test fun invalidUnreadableOrCancelledImportNeverCallsRestore() = runBlocking {
        val f = Fixture(this)
        f.documents.files["bad"] = "not a backup".toByteArray()
        f.operations.beginImport()
        f.operations.importFrom("bad")
        finish()
        assertTrue(f.state.error)
        assertNull(f.state.preview)
        f.operations.beginImport()
        f.operations.importFrom("missing")
        finish()
        assertTrue(f.state.error)
        f.operations.beginImport()
        f.operations.importFrom(null)
        assertFalse(f.state.error)
        assertFalse(f.state.busy)
        assertTrue(f.restored.isEmpty())
    }
}
