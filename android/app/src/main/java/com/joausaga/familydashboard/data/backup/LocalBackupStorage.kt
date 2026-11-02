package com.joausaga.familydashboard.data.backup

import com.google.gson.GsonBuilder
import com.joausaga.familydashboard.data.Store
import com.joausaga.familydashboard.data.TvUsageHistory
import com.joausaga.familydashboard.data.cache.Cached
import com.joausaga.familydashboard.data.cache.DiskCache

class LocalBackupStorage(private val store: Store, private val cache: DiskCache) : BackupStorage {
    private val gson = GsonBuilder().serializeNulls().create()

    override fun snapshot(): BackupSnapshot {
        val history = cache.read<TvUsageHistory>(HISTORY_KEY, TvUsageHistory::class.java)
        check(history != null || HISTORY_KEY !in cache.keys()) { "No se pudo leer el historial TV existente; no se exportará una copia vacía." }
        return BackupSnapshot(store.backupConfiguration(), history)
    }

    override fun writeConfiguration(configuration: BackupConfiguration) = store.restoreBackupConfiguration(configuration)

    override fun writeHistory(history: Cached<TvUsageHistory>?) {
        if (history == null) {
            cache.remove(HISTORY_KEY)
            check(HISTORY_KEY !in cache.keys()) { "No se pudo reemplazar el historial TV." }
        } else cache.write(HISTORY_KEY, history, TvUsageHistory::class.java)
    }

    override fun readJournal(): BackupRollback? = store.backupJournal?.let {
        requireNotNull(gson.fromJson(it, BackupRollback::class.java)) { "No se pudo leer el diario de recuperación." }
    }

    override fun writeJournal(rollback: BackupRollback) { store.backupJournal = gson.toJson(rollback) }
    override fun clearJournal() { store.backupJournal = null }

    private companion object { const val HISTORY_KEY = "tv_usage_history" }
}
