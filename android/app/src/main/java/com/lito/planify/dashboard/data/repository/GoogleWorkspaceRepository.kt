package com.lito.planify.dashboard.data.repository

import com.lito.planify.dashboard.data.Content
import com.lito.planify.dashboard.data.Event
import com.lito.planify.dashboard.data.GoogleCalendarApi
import com.lito.planify.dashboard.data.GoogleCalendar
import com.lito.planify.dashboard.data.GoogleEvent
import com.lito.planify.dashboard.data.GoogleEventTime
import com.lito.planify.dashboard.data.GoogleTaskList
import com.lito.planify.dashboard.data.GoogleTasksApi
import com.lito.planify.dashboard.data.Task
import com.lito.planify.dashboard.data.Week
import com.lito.planify.dashboard.data.bodyOrThrow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Lectura de datos de Google. Las series recurrentes se expanden en Calendar y sus ocurrencias son rutinas. */
class GoogleWorkspaceRepository(
    private val calendarApi: GoogleCalendarApi,
    private val tasksApi: GoogleTasksApi,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() }
) {
    suspend fun calendars(token: String): List<GoogleCalendar> = fetchCalendars(token)

    suspend fun taskLists(token: String): List<GoogleTaskList> = fetchTaskLists(token)

    suspend fun content(token: String, today: LocalDate): Content {
        val from = today.withDayOfMonth(1).minusMonths(PAST_MONTHS).atStartOfDay()
        val until = today.plusDays(CONTENT_FUTURE_DAYS).atStartOfDay()
        val allEvents = fetchEvents(token, from, until)
        val weekStart = today.with(java.time.DayOfWeek.MONDAY).atStartOfDay()
        val weekEnd = weekStart.plusDays(7)
        val sessions = allEvents.filter { it.routine_id != null && it.at >= weekStart && it.at < weekEnd }
        return Content(
            events = allEvents.filter { it.routine_id == null }.sortedBy { it.at },
            sessions = sessions.sortedBy { it.at },
            tasks = fetchTasks(token)
        )
    }

    suspend fun week(token: String, monday: LocalDate): Week {
        val events = fetchEvents(token, monday.atStartOfDay(), monday.plusDays(7).atStartOfDay())
        return Week(
            events = events.filter { it.routine_id == null }.sortedBy { it.at },
            sessions = events.filter { it.routine_id != null }.sortedBy { it.at }
        )
    }

    private suspend fun fetchEvents(token: String, from: LocalDateTime, until: LocalDateTime): List<Event> {
        val auth = bearer(token)
        val zone = zone()
        val calendars = fetchCalendars(token)
        return buildList {
            calendars.forEach { calendar ->
                var page: String? = null
                do {
                    val response = calendarApi.events(
                        auth = auth,
                        calendarId = calendar.id,
                        timeMin = from.atZone(zone).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        timeMax = until.atZone(zone).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        timeZone = zone.id,
                        pageToken = page
                    ).bodyOrThrow()
                    response.items.orEmpty().asSequence()
                        .filter { it.status != "cancelled" }
                        .mapNotNull { it.toDashboardEvent(calendar.id, calendar.summary, calendar.backgroundColor, zone) }
                        .forEach(::add)
                    page = response.nextPageToken
                } while (page != null)
            }
        }
    }

    private suspend fun fetchCalendars(token: String): List<GoogleCalendar> {
        val auth = bearer(token)
        return buildList {
            var page: String? = null
            do {
                val response = calendarApi.calendars(auth, pageToken = page, showHidden = true).bodyOrThrow()
                addAll(response.items.orEmpty())
                page = response.nextPageToken
            } while (page != null)
        }
    }

    private suspend fun fetchTasks(token: String): List<Task> {
        val lists = fetchTaskLists(token)
        return buildList {
            lists.forEach { taskList ->
                var page: String? = null
                do {
                    val response = tasksApi.tasks(bearer(token), taskList.id, pageToken = page).bodyOrThrow()
                    response.items.orEmpty().asSequence()
                        .filter { it.status != "completed" }
                        .forEachIndexed { index, task ->
                            add(
                                Task(
                                    id = stableId(taskList.id, task.id),
                                    task_list_id = stableId(taskList.id),
                                    task_list_name = taskList.title.orEmpty(),
                                    task_list_color = null,
                                    title = task.title.orEmpty(),
                                    order = index,
                                    completed_at = null,
                                    google_task_list_id = taskList.id,
                                    due = task.due
                                )
                            )
                        }
                    page = response.nextPageToken
                } while (page != null)
            }
        }
    }

    private suspend fun fetchTaskLists(token: String): List<GoogleTaskList> {
        val auth = bearer(token)
        return buildList {
            var page: String? = null
            do {
                val response = tasksApi.taskLists(auth, pageToken = page).bodyOrThrow()
                addAll(response.items.orEmpty())
                page = response.nextPageToken
            } while (page != null)
        }
    }

    private fun GoogleEvent.toDashboardEvent(calendarId: String, calendarName: String?, color: String?, zone: ZoneId): Event? {
        fun GoogleEventTime?.toLocalDateTime(): LocalDateTime? = this?.let { time ->
            when {
                time.dateTime != null -> runCatching { OffsetDateTime.parse(time.dateTime).atZoneSameInstant(zone).toLocalDateTime() }
                    .recoverCatching { Instant.parse(time.dateTime).atZone(zone).toLocalDateTime() }.getOrNull()
                time.date != null -> runCatching { LocalDate.parse(time.date).atStartOfDay() }.getOrNull()
                else -> null
            }
        }
        val localStart = start.toLocalDateTime() ?: return null
        val localEnd = end.toLocalDateTime()
        return Event(
            id = stableId(calendarId, id),
            calendar_id = stableId(calendarId),
            calendar_name = calendarName,
            calendar_color = color,
            title = summary.orEmpty(),
            date_at = localStart.format(com.lito.planify.dashboard.data.SQL_DATE_TIME),
            routine_id = recurringEventId?.let(::stableId),
            google_calendar_id = calendarId,
            end_at = localEnd?.format(com.lito.planify.dashboard.data.SQL_DATE_TIME)
        )
    }

    companion object {
        private const val PAST_MONTHS = 12L
        private const val CONTENT_FUTURE_DAYS = 366L

        /** Solo se usa en los modelos visuales actuales; la caché y las llamadas conservan los IDs completos de Google. */
        private fun stableId(value: String) = value.hashCode() and Int.MAX_VALUE
        private fun stableId(parent: String, child: String) = "$parent/$child".hashCode() and Int.MAX_VALUE
    }
}
