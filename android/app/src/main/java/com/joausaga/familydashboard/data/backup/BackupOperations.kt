package com.joausaga.familydashboard.data.backup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.io.InputStream
import java.io.OutputStream

interface BackupDocuments {
    fun openRead(document: String): InputStream
    fun openWrite(document: String): OutputStream
}

/** Operaciones de documento en memoria/IO; nunca aplica una copia solo por abrirla. */
class BackupOperations(
    private val scope: CoroutineScope,
    private val documents: BackupDocuments,
    private val state: () -> BackupUiState,
    private val publish: (BackupUiState) -> Unit,
    private val snapshot: suspend () -> BackupSnapshot,
    private val restore: suspend (DashboardBackup, BackupSelection) -> Unit,
    private val recover: suspend () -> Unit,
    private val recoveryPending: () -> Boolean
) {
    private val codec = BackupCodec()
    private var exportBytes: ByteArray? = null
    private var pending: DashboardBackup? = null

    fun requestExport() {
        if (!state().active) publish(BackupUiState(confirmExport = true))
    }

    fun dismiss() {
        if (state().busy || state().recoveryRequired) return
        pending = null
        exportBytes = null
        publish(BackupUiState())
    }

    fun pickerFailed() {
        exportBytes = null
        publish(BackupUiState(error = true, message = "No se pudo abrir el selector de archivos de Android."))
    }

    fun prepareExport(onReady: (String) -> Unit) {
        if (!state().confirmExport || state().busy) return
        publish(BackupUiState(busy = true, message = "Preparando copia…"))
        scope.launch {
            try {
                val current = snapshot()
                val createdAt = System.currentTimeMillis()
                exportBytes = withContext(Dispatchers.IO) {
                    codec.encode(DashboardBackup(createdAtMillis = createdAt, configuration = current.configuration, tvHistory = current.history?.value))
                }
                onReady("family-dashboard-${DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(Instant.ofEpochMilli(createdAt))}.json")
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) { fail("No se pudo preparar la copia.", failure) }
        }
    }

    fun exportTo(uri: String?) {
        val bytes = exportBytes
        exportBytes = null
        if (uri == null) { publish(BackupUiState()); return }
        if (bytes == null) { publish(BackupUiState(error = true, message = "La exportación se interrumpió. Vuelve a exportar.")); return }
        publish(BackupUiState(busy = true, message = "Guardando y verificando copia…"))
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    documents.openWrite(uri).use { it.write(bytes); it.flush() }
                    val saved = documents.openRead(uri).use(codec::read)
                    require(saved.contentEquals(bytes)) { "El documento guardado no coincide con la copia." }
                    codec.decode(saved)
                }
                publish(BackupUiState(message = "Copia exportada y verificada. Guárdala en un lugar seguro."))
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) { fail("No se pudo verificar la exportación. No uses el archivo como copia válida; vuelve a exportar.", failure) }
        }
    }

    fun beginImport(): Boolean {
        if (state().active) return false
        publish(BackupUiState(busy = true, message = "Selecciona una copia…"))
        return true
    }

    fun importFrom(uri: String?) {
        if (uri == null) { publish(BackupUiState()); return }
        publish(BackupUiState(busy = true, message = "Leyendo y validando copia…"))
        scope.launch {
            try {
                val document = withContext(Dispatchers.IO) {
                    val bytes = documents.openRead(uri).use(codec::read)
                    codec.decode(bytes)
                }
                pending = document
                publish(BackupUiState(preview = BackupPreview(document.createdAtMillis, document.tvHistory?.transitions?.size ?: 0,
                    (document.configuration.calendarSelections.keys.map { it.removePrefix("calendar_visibility_") } +
                        document.configuration.taskListSelections.keys.map { it.removePrefix("task_list_visibility_") }).toSet().size,
                    document.configuration.location?.name)))
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) { pending = null; fail("No se pudo importar la copia. No se ha cambiado ningún dato.", failure) }
        }
    }

    fun restoreSelected(selection: BackupSelection) {
        val document = pending ?: return
        if (!selection.any || state().busy || state().preview == null) return
        publish(BackupUiState(busy = true, message = "Restaurando selección…"))
        scope.launch {
            try {
                restore(document, selection)
                pending = null
                publish(BackupUiState(message = "Restauración completada: ${selectionLabel(selection)}. La sesión Google se conserva."))
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) {
                pending = null
                publish(BackupUiState(error = true, recoveryRequired = recoveryPending(), message = failure.message ?: "No se pudo restaurar la copia."))
            }
        }
    }

    fun retryRecovery() {
        if (state().busy || !state().recoveryRequired) return
        publish(state().copy(busy = true, message = "Recuperando datos anteriores…"))
        scope.launch {
            try {
                recover()
                publish(BackupUiState(message = "Datos anteriores recuperados."))
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) {
                publish(BackupUiState(error = true, recoveryRequired = true, message = "No se pudo recuperar. Comprueba el espacio disponible y vuelve a intentarlo."))
            }
        }
    }

    private fun fail(message: String, failure: Exception) {
        exportBytes = null
        publish(BackupUiState(error = true, message = "$message ${if (failure is IllegalArgumentException) failure.message.orEmpty() else "Comprueba el archivo, los permisos y el espacio disponible."}"))
    }
}

fun selectionLabel(selection: BackupSelection): String = when {
    selection.history && selection.configuration -> "historial TV y configuración"
    selection.history -> "historial TV"
    else -> "configuración"
}
