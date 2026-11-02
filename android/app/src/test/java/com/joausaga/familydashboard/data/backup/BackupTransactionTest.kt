package com.joausaga.familydashboard.data.backup

import com.joausaga.familydashboard.data.TvUsageHistory
import com.joausaga.familydashboard.data.cache.Cached
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTransactionTest {
    private class Storage : BackupStorage {
        var configuration = backupConfig().copy(brightness = 0.8f)
        var history: Cached<TvUsageHistory>? = Cached(TvUsageHistory(), 99L)
        var journal: BackupRollback? = null
        var configWrites = 0
        var historyWrites = 0
        var failConfigOnce = false
        var failClear = false
        var failJournal = false
        override fun snapshot() = BackupSnapshot(configuration, history)
        override fun writeConfiguration(configuration: BackupConfiguration) {
            configWrites++
            if (failConfigOnce) { failConfigOnce = false; error("disk full") }
            this.configuration = configuration
        }
        override fun writeHistory(history: Cached<TvUsageHistory>?) { historyWrites++; this.history = history }
        override fun readJournal() = journal
        override fun writeJournal(rollback: BackupRollback) { if (failJournal) error("disk full"); journal = rollback }
        override fun clearJournal() { if (failClear) error("disk full"); journal = null }
    }

    @Test fun restoreHistoryOnlyDoesNotWriteConfiguration() {
        val store = Storage()
        val original = store.configuration
        BackupTransaction(store).restore(backupDocument(), BackupSelection(true, false))
        assertEquals(original, store.configuration)
        assertEquals(0, store.configWrites)
        assertEquals(backupHistory(), store.history?.value)
        assertNull(store.journal)
    }

    @Test fun restoreConfigurationOnlyDoesNotWriteHistory() {
        val store = Storage()
        val original = store.history
        BackupTransaction(store).restore(backupDocument(), BackupSelection(false, true))
        assertEquals(backupConfig(), store.configuration)
        assertEquals(original, store.history)
        assertEquals(0, store.historyWrites)
        assertNull(store.journal)
    }

    @Test fun restoreBothAndExplicitlyEmptyHistory() {
        val store = Storage()
        BackupTransaction(store).restore(backupDocument(), BackupSelection(true, true))
        assertEquals(backupConfig(), store.configuration)
        assertEquals(backupHistory(), store.history?.value)
        BackupTransaction(store).restore(backupDocument().copy(tvHistory = null), BackupSelection(true, false))
        assertNull(store.history)
    }

    @Test fun noSelectionOrJournalFailureDoesNotTouchData() {
        val store = Storage()
        val before = store.snapshot()
        assertTrue(runCatching { BackupTransaction(store).restore(backupDocument(), BackupSelection(false, false)) }.isFailure)
        store.failJournal = true
        assertTrue(runCatching { BackupTransaction(store).restore(backupDocument(), BackupSelection(true, true)) }.isFailure)
        assertEquals(before, store.snapshot())
        assertEquals(0, store.historyWrites)
        assertEquals(0, store.configWrites)
    }

    @Test fun failureAfterHistoryWriteRollsBackBothAndClearsJournal() {
        val store = Storage()
        val before = store.snapshot()
        store.failConfigOnce = true
        val result = runCatching { BackupTransaction(store).restore(backupDocument(), BackupSelection(true, true)) }
        assertTrue(result.isFailure)
        assertEquals(before, store.snapshot())
        assertNull(store.journal)
    }

    @Test fun startupRecoversInterruptedRestoreAndIsIdempotent() {
        val store = Storage()
        val before = store.snapshot()
        store.journal = BackupRollback(BackupSelection(true, true), before)
        store.history = Cached(backupHistory(), 123)
        store.configuration = backupConfig()
        assertTrue(BackupTransaction(store).recover())
        assertEquals(before, store.snapshot())
        assertFalse(BackupTransaction(store).recover())
    }

    @Test fun failedRecoveryKeepsJournalForRetry() {
        val store = Storage()
        val before = store.snapshot()
        store.failClear = true
        assertTrue(runCatching { BackupTransaction(store).restore(backupDocument(), BackupSelection(true, true)) }.isFailure)
        assertTrue(store.journal != null)
        store.failClear = false
        assertTrue(BackupTransaction(store).recover())
        assertEquals(before, store.snapshot())
        assertNull(store.journal)
    }
}
