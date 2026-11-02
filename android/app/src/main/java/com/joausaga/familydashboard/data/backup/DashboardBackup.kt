package com.joausaga.familydashboard.data.backup

import com.joausaga.familydashboard.data.CalendarVisibilitySelection
import com.joausaga.familydashboard.data.Location
import com.joausaga.familydashboard.data.Schedule
import com.joausaga.familydashboard.data.TaskListVisibilitySelection
import com.joausaga.familydashboard.data.TvMonitorSettings
import com.joausaga.familydashboard.data.TvUsageHistory

/** Lista blanca: nunca añadir credenciales, estado OAuth ni un volcado completo de preferencias. */
data class BackupConfiguration(
    val location: Location?,
    val schedule: Schedule,
    val brightness: Float?,
    val tvMonitorSettings: TvMonitorSettings,
    val calendarTaskRefreshMinutes: Int,
    val calendarSelections: Map<String, CalendarVisibilitySelection>,
    val taskListSelections: Map<String, TaskListVisibilitySelection>
)

data class DashboardBackup(
    val format: String = "family-dashboard-backup",
    val version: Int = 1,
    val createdAtMillis: Long,
    val configuration: BackupConfiguration,
    val tvHistory: TvUsageHistory?
)

data class BackupSelection(val history: Boolean, val configuration: Boolean) {
    val any: Boolean get() = history || configuration
}

data class BackupPreview(val createdAtMillis: Long, val transitions: Int, val accounts: Int, val locationName: String?)

data class BackupUiState(
    val busy: Boolean = false,
    val confirmExport: Boolean = false,
    val preview: BackupPreview? = null,
    val message: String? = null,
    val error: Boolean = false,
    val recoveryRequired: Boolean = false
) {
    val active: Boolean get() = busy || confirmExport || preview != null || recoveryRequired
}
