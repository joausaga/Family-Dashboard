package com.joausaga.familydashboard.data.backup

import com.joausaga.familydashboard.data.TvUsageHistory
import com.joausaga.familydashboard.data.cache.Cached

data class BackupSnapshot(val configuration: BackupConfiguration, val history: Cached<TvUsageHistory>?)
data class BackupRollback(val selection: BackupSelection, val previous: BackupSnapshot)

interface BackupStorage {
    fun snapshot(): BackupSnapshot
    fun writeConfiguration(configuration: BackupConfiguration)
    fun writeHistory(history: Cached<TvUsageHistory>?)
    fun readJournal(): BackupRollback?
    fun writeJournal(rollback: BackupRollback)
    fun clearJournal()
}

/** El llamador excluye sondeos/escrituras concurrentes. El diario se confirma antes de tocar datos. */
class BackupTransaction(private val storage: BackupStorage) {
    fun restore(document: DashboardBackup, selection: BackupSelection) {
        require(selection.any) { "Selecciona qué quieres restaurar." }
        check(storage.readJournal() == null) { "Hay una recuperación pendiente." }
        storage.writeJournal(BackupRollback(selection, storage.snapshot()))
        try {
            if (selection.history) storage.writeHistory(document.tvHistory?.let { Cached(it, it.lastSyncMillis ?: 0L) })
            if (selection.configuration) storage.writeConfiguration(document.configuration)
            storage.clearJournal()
        } catch (failure: Exception) {
            try {
                recover()
            } catch (recoveryFailure: Exception) {
                throw IllegalStateException("No se pudo completar ni revertir la restauración. Hay una recuperación pendiente; no se reanudará el sondeo hasta resolverla.", recoveryFailure)
            }
            throw IllegalStateException("No se pudo restaurar la copia. Se han restablecido los datos anteriores.", failure)
        }
    }

    fun recover(): Boolean {
        val rollback = storage.readJournal() ?: return false
        require(rollback.selection.any) { "Diario de recuperación no válido." }
        if (rollback.selection.history) storage.writeHistory(rollback.previous.history)
        if (rollback.selection.configuration) storage.writeConfiguration(rollback.previous.configuration)
        storage.clearJournal()
        return true
    }
}
