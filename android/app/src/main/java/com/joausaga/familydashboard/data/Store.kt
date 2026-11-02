package com.joausaga.familydashboard.data

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.google.gson.Gson
import com.joausaga.familydashboard.data.CalendarVisibilitySelection
import com.joausaga.familydashboard.data.backup.BackupConfiguration
import com.joausaga.familydashboard.data.backup.BackupPreferences

/** Horario de pantalla en minutos desde medianoche, por tipo de día. */
data class Schedule(
    val enabled: Boolean = false,
    val weekdayOn: Int = 7 * 60,
    val weekdayOff: Int = 23 * 60,
    val weekendOn: Int = 8 * 60 + 30,
    val weekendOff: Int = 23 * 60 + 30,
    /**
     * Fuera de horario: false (por defecto) muestra el reloj nocturno con el brillo al mínimo; true apaga la pantalla.
     * En negativo para que los horarios guardados antes de existir (Gson deja el campo a false) usen el reloj.
     */
    val screenOff: Boolean = false
)

/** Sesión y ajustes del dispositivo (SharedPreferences). Los datos descargados van en DiskCache. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("dashboard", Context.MODE_PRIVATE)
    private val gson = Gson()

    /** Devuelve la selección previa, marcando los calendarios nuevos como visibles y retirando los inexistentes. */
    fun reconcileVisibleCalendars(accountCalendarId: String, calendarIds: Set<String>): Set<String> {
        val key = calendarVisibilityKey(accountCalendarId)
        val saved = prefs.getString(key, null)?.let { json ->
            runCatching {
                gson.fromJson(json, CalendarVisibilitySelection::class.java)
            }.getOrNull()
        }
        val selection = saved?.reconcile(calendarIds) ?: CalendarVisibilitySelection(calendarIds, calendarIds)
        prefs.edit { putString(key, gson.toJson(selection)) }
        return selection.visibleIds
    }

    fun saveCalendarVisibility(accountCalendarId: String, knownIds: Set<String>, visibleIds: Set<String>) {
        val value = CalendarVisibilitySelection(knownIds, visibleIds intersect knownIds)
        prefs.edit { putString(calendarVisibilityKey(accountCalendarId), gson.toJson(value)) }
    }

    /** Devuelve la selección previa, activando listas nuevas y retirando las que ya no existen. */
    fun reconcileVisibleTaskLists(accountCalendarId: String, taskListIds: Set<String>): Set<String> {
        val key = taskListVisibilityKey(accountCalendarId)
        val saved = prefs.getString(key, null)?.let { json ->
            runCatching { gson.fromJson(json, TaskListVisibilitySelection::class.java) }.getOrNull()
        }
        val selection = saved?.reconcile(taskListIds) ?: TaskListVisibilitySelection(taskListIds, taskListIds)
        prefs.edit { putString(key, gson.toJson(selection)) }
        return selection.visibleIds
    }

    fun saveTaskListVisibility(accountCalendarId: String, knownIds: Set<String>, visibleIds: Set<String>) {
        val value = TaskListVisibilitySelection(knownIds, visibleIds intersect knownIds)
        prefs.edit { putString(taskListVisibilityKey(accountCalendarId), gson.toJson(value)) }
    }

    private fun calendarVisibilityKey(accountCalendarId: String): String =
        "calendar_visibility_" + Base64.encodeToString(
            accountCalendarId.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

    private fun taskListVisibilityKey(accountCalendarId: String): String =
        "task_list_visibility_" + Base64.encodeToString(
            accountCalendarId.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

    var token: String?
        get() = prefs.getString("token", null)
        set(value) = prefs.edit { putString("token", value) }

    var email: String?
        get() = prefs.getString("email", null)
        set(value) = prefs.edit { putString("email", value) }

    /** Marca que el usuario ya concedió los permisos Google; el access token corto nunca se guarda en preferencias. */
    var googleConnected: Boolean
        get() = prefs.getBoolean("google_connected", false)
        set(value) = prefs.edit { putBoolean("google_connected", value) }

    var tvMonitorSettings: TvMonitorSettings
        get() = (read<TvMonitorSettings>("tv_monitor_settings") ?: TvMonitorSettings()).let { settings ->
            if (settings.pollMinutes in TvMonitorIntervals.options) settings else settings.copy(pollMinutes = TvMonitorSettings().pollMinutes)
        }
        set(value) = write("tv_monitor_settings", value)

    var calendarTaskRefreshMinutes: Int
        get() = prefs.getInt("calendar_task_refresh_minutes", CalendarTaskRefreshInterval.DEFAULT_MINUTES)
            .takeIf(CalendarTaskRefreshInterval::isValid) ?: CalendarTaskRefreshInterval.DEFAULT_MINUTES
        set(value) {
            if (CalendarTaskRefreshInterval.isValid(value)) {
                prefs.edit { putInt("calendar_task_refresh_minutes", value) }
            }
        }

    var location: Location?
        get() = read("location")
        set(value) = write("location", value)

    var schedule: Schedule
        get() = read("schedule") ?: Schedule()
        set(value) = write("schedule", value)

    /** Brillo de la ventana (0.05–1); null = el del sistema. */
    var brightness: Float?
        get() = prefs.getFloat("brightness", -1f).takeIf { it > 0f }
        set(value) = prefs.edit { putFloat("brightness", value ?: -1f) }

    /** Última hora válida (epoch ms), para ReferenceClock. */
    var lastValidTime: Long
        get() = prefs.getLong("last_valid_time", 0L)
        set(value) = prefs.edit { putLong("last_valid_time", value) }

    /** Lista blanca portable: no incluye token, email, google_connected ni preferencias del sistema. */
    fun backupConfiguration(): BackupConfiguration = BackupPreferences.snapshot(prefs.all)

    /** Un único commit de preferencias seleccionadas; conserva sesión, reloj y cualquier clave ajena. */
    fun restoreBackupConfiguration(configuration: BackupConfiguration) {
        val editor = prefs.edit()
        BackupPreferences.keysToReplace(prefs.all.keys).forEach(editor::remove)
        BackupPreferences.replacementValues(configuration).forEach { (key, value) ->
            when (value) {
                null -> editor.remove(key)
                is String -> editor.putString(key, value)
                is Float -> editor.putFloat(key, value)
                is Int -> editor.putInt(key, value)
                else -> error("Tipo de preferencia de copia no permitido.")
            }
        }
        check(editor.commit()) { "No se pudo guardar la configuración restaurada." }
    }

    /** Diario privado de rollback; excluido de la lista blanca de exportación. */
    var backupJournal: String?
        get() = prefs.getString("backup_restore_journal", null)
        set(value) {
            val editor = prefs.edit()
            if (value == null) editor.remove("backup_restore_journal") else editor.putString("backup_restore_journal", value)
            check(editor.commit()) { "No se pudo guardar el diario de recuperación." }
        }

    fun logout() = prefs.edit { remove("token"); remove("email"); putBoolean("google_connected", false) }

    init {
        // La caché de datos estaba aquí antes de pasar a disco (DiskCache): se descarta, se vuelve a descargar.
        if (prefs.contains("content") || prefs.contains("forecast") || prefs.contains("weeks")) {
            prefs.edit {
                remove("content")
                remove("forecast")
                remove("weeks")
            }
        }
    }

    private inline fun <reified T> read(key: String): T? =
        prefs.getString(key, null)?.let { runCatching { gson.fromJson(it, T::class.java) }.getOrNull() }

    private fun write(key: String, value: Any?) = prefs.edit { if (value == null) remove(key) else putString(key, gson.toJson(value)) }
}
