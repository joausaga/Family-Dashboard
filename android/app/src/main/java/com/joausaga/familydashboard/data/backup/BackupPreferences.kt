package com.joausaga.familydashboard.data.backup

import com.google.gson.Gson
import com.joausaga.familydashboard.data.CalendarTaskRefreshInterval
import com.joausaga.familydashboard.data.CalendarVisibilitySelection
import com.joausaga.familydashboard.data.Location
import com.joausaga.familydashboard.data.Schedule
import com.joausaga.familydashboard.data.TaskListVisibilitySelection
import com.joausaga.familydashboard.data.TvMonitorSettings

/** Mapeo puro y comprobable: no exportar ni reemplazar claves que no estén en esta lista blanca. */
object BackupPreferences {
    private val gson = Gson()
    private val settingsKeys = setOf("location", "schedule", "brightness", "tv_monitor_settings", "calendar_task_refresh_minutes")

    fun keysToReplace(keys: Set<String>): Set<String> = settingsKeys + keys.filter {
        BackupCodec.CALENDAR_KEY.matches(it) || BackupCodec.TASK_KEY.matches(it)
    }

    fun snapshot(values: Map<String, *>): BackupConfiguration {
        fun <T> read(key: String, type: Class<T>): T? = (values[key] as? String)?.let { gson.fromJson(it, type) }
        return BackupConfiguration(
            read("location", Location::class.java), read("schedule", Schedule::class.java) ?: Schedule(),
            (values["brightness"] as? Number)?.toFloat()?.takeIf { it > 0f },
            read("tv_monitor_settings", TvMonitorSettings::class.java) ?: TvMonitorSettings(),
            (values["calendar_task_refresh_minutes"] as? Number)?.toInt() ?: CalendarTaskRefreshInterval.DEFAULT_MINUTES,
            values.keys.filter { BackupCodec.CALENDAR_KEY.matches(it) }.associateWith { requireNotNull(read(it, CalendarVisibilitySelection::class.java)) },
            values.keys.filter { BackupCodec.TASK_KEY.matches(it) }.associateWith { requireNotNull(read(it, TaskListVisibilitySelection::class.java)) }
        )
    }

    fun replacementValues(configuration: BackupConfiguration): Map<String, Any?> = buildMap {
        put("location", configuration.location?.let(gson::toJson))
        put("schedule", gson.toJson(configuration.schedule))
        put("brightness", configuration.brightness ?: -1f)
        put("tv_monitor_settings", gson.toJson(configuration.tvMonitorSettings))
        put("calendar_task_refresh_minutes", configuration.calendarTaskRefreshMinutes)
        configuration.calendarSelections.forEach { (key, value) ->
            require(BackupCodec.CALENDAR_KEY.matches(key))
            put(key, gson.toJson(value))
        }
        configuration.taskListSelections.forEach { (key, value) ->
            require(BackupCodec.TASK_KEY.matches(key))
            put(key, gson.toJson(value))
        }
    }
}
