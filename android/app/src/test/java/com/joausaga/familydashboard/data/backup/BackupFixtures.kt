package com.joausaga.familydashboard.data.backup

import com.joausaga.familydashboard.data.CalendarVisibilitySelection
import com.joausaga.familydashboard.data.Location
import com.joausaga.familydashboard.data.Schedule
import com.joausaga.familydashboard.data.TaskListVisibilitySelection
import com.joausaga.familydashboard.data.TvMonitorSettings
import com.joausaga.familydashboard.data.TvPowerState
import com.joausaga.familydashboard.data.TvTransition
import com.joausaga.familydashboard.data.TvUsageCalculator
import java.time.Instant

internal val backupTime = Instant.parse("2026-11-02T20:00:00Z")
internal fun backupConfig() = BackupConfiguration(
    Location("Madrid", "España", 40.4, -3.7, "Europe/Madrid"), Schedule(enabled = true), 0.5f,
    TvMonitorSettings("192.168.1.20", 3), 10,
    mapOf("calendar_visibility_YWNjb3VudA" to CalendarVisibilitySelection(setOf("c1", "c2"), setOf("c1"))),
    mapOf("task_list_visibility_YWNjb3VudA" to TaskListVisibilitySelection(setOf("t1"), emptySet()))
)
internal fun backupHistory() = TvUsageCalculator.history(listOf(
    TvTransition(backupTime.minusSeconds(120).toEpochMilli(), TvPowerState.ON, "IFTTT", 1),
    TvTransition(backupTime.minusSeconds(60).toEpochMilli(), TvPowerState.OFF, "IFTTT", 2)
), backupTime, true, sourceRowCounts = mapOf("legacy-sheet" to 3), sourceOrders = mapOf("legacy-sheet" to 0L))

internal fun backupDocument() = DashboardBackup(createdAtMillis = backupTime.toEpochMilli(), configuration = backupConfig(), tvHistory = backupHistory())
