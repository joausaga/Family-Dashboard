package com.lito.planify.dashboard.data.repository

import com.lito.planify.dashboard.data.GoogleCalendar
import com.lito.planify.dashboard.data.GoogleCalendarApi
import com.lito.planify.dashboard.data.GoogleCalendarList
import com.lito.planify.dashboard.data.GoogleEvent
import com.lito.planify.dashboard.data.GoogleEventList
import com.lito.planify.dashboard.data.GoogleEventTime
import com.lito.planify.dashboard.data.GoogleTask
import com.lito.planify.dashboard.data.GoogleTaskList
import com.lito.planify.dashboard.data.GoogleTaskListPage
import com.lito.planify.dashboard.data.GoogleTaskPage
import com.lito.planify.dashboard.data.GoogleTasksApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.Response
import java.time.LocalDate
import java.time.ZoneId

class GoogleWorkspaceRepositoryTest {
    @Test
    fun listsAllCalendarsAcrossPagesIncludingHiddenAndIdentifiesPrimary() = runBlocking {
        val requestedHiddenFlags = mutableListOf<Boolean>()
        val calendarApi = object : GoogleCalendarApi {
            override suspend fun calendars(auth: String, maxResults: Int, pageToken: String?, showHidden: Boolean): Response<GoogleCalendarList> {
                requestedHiddenFlags += showHidden
                return if (pageToken == null) {
                    Response.success(GoogleCalendarList(listOf(GoogleCalendar("owner@example.com", "Principal", null, primary = true)), "next"))
                } else {
                    Response.success(GoogleCalendarList(listOf(GoogleCalendar("shared", "Compartido", "#abcdef", hidden = true)), null))
                }
            }

            override suspend fun events(
                auth: String,
                calendarId: String,
                timeMin: String,
                timeMax: String,
                timeZone: String,
                singleEvents: Boolean,
                orderBy: String,
                maxResults: Int,
                pageToken: String?
            ) = Response.success(GoogleEventList(emptyList(), null))
        }
        val tasksApi = emptyTasksApi()

        val result = GoogleWorkspaceRepository(calendarApi, tasksApi).calendars("token")

        assertEquals(listOf(true, true), requestedHiddenFlags)
        assertEquals(listOf("owner@example.com", "shared"), result.map { it.id })
        assertEquals(true, result.first().primary)
        assertEquals(true, result.last().hidden)
    }

    @Test
    fun expandsRecurringEventsIntoRoutinesAndReturnsOnlyPendingTasks() = runBlocking {
        val calendarApi = object : GoogleCalendarApi {
            override suspend fun calendars(auth: String, maxResults: Int, pageToken: String?, showHidden: Boolean) =
                Response.success(GoogleCalendarList(listOf(GoogleCalendar("home", "Casa", "#123456")), null))

            override suspend fun events(
                auth: String,
                calendarId: String,
                timeMin: String,
                timeMax: String,
                timeZone: String,
                singleEvents: Boolean,
                orderBy: String,
                maxResults: Int,
                pageToken: String?
            ) = Response.success(
                GoogleEventList(
                    listOf(
                        GoogleEvent("one-off", "Reunión", "confirmed", GoogleEventTime("2026-10-27T10:00:00-03:00", null), null, GoogleEventTime("2026-10-27T11:30:00-03:00", null)),
                        GoogleEvent("monday-instance", "Monday run", "confirmed", GoogleEventTime("2026-10-26T08:00:00-03:00", null), "routine-series"),
                        GoogleEvent("repeat-instance", "Yoga", "confirmed", GoogleEventTime(null, "2026-10-28"), "routine-series"),
                        GoogleEvent("sunday-instance", "Sunday run", "confirmed", GoogleEventTime("2026-11-01T09:00:00-03:00", null), "routine-series"),
                        GoogleEvent("next-monday-instance", "Next week", "confirmed", GoogleEventTime("2026-11-02T09:00:00-03:00", null), "routine-series")
                    ),
                    null
                )
            )
        }
        val tasksApi = object : GoogleTasksApi {
            override suspend fun taskLists(auth: String, maxResults: Int, pageToken: String?) =
                Response.success(GoogleTaskListPage(listOf(GoogleTaskList("home-tasks", "Casa")), null))

            override suspend fun tasks(
                auth: String,
                taskListId: String,
                showCompleted: Boolean,
                showHidden: Boolean,
                maxResults: Int,
                pageToken: String?
            ) = Response.success(
                GoogleTaskPage(
                    listOf(GoogleTask("pending", "Comprar leche", "needsAction", "2026-10-30T00:00:00.000Z"), GoogleTask("done", "Terminado", "completed")),
                    null
                )
            )
        }

        val repository = GoogleWorkspaceRepository(calendarApi, tasksApi) { ZoneId.of("America/Asuncion") }
        val result = repository.content("access-token", LocalDate.of(2026, 10, 27))

        assertEquals(listOf("Reunión"), result.events.map { it.title })
        assertEquals("home", result.events.single().google_calendar_id)
        assertEquals("2026-10-27 10:00:00", result.events.single().date_at)
        assertEquals("2026-10-27 11:30:00", result.events.single().end_at)
        assertEquals(listOf("Monday run", "Yoga", "Sunday run"), result.sessions.map { it.title })
        assertEquals(listOf("2026-10-26 08:00:00", "2026-10-28 00:00:00", "2026-11-01 09:00:00"), result.sessions.map { it.date_at })
        assertEquals(listOf("Comprar leche"), result.tasks.map { it.title })
        assertEquals("Casa", result.tasks.single().task_list_name)
        assertEquals("home-tasks", result.tasks.single().google_task_list_id)
        assertEquals("2026-10-30T00:00:00.000Z", result.tasks.single().due)
    }

    @Test
    fun listsEveryTaskListAcrossPagesIncludingListsWithoutTasks() = runBlocking {
        val calendarApi = object : GoogleCalendarApi {
            override suspend fun calendars(auth: String, maxResults: Int, pageToken: String?, showHidden: Boolean) =
                Response.success(GoogleCalendarList(emptyList(), null))

            override suspend fun events(
                auth: String,
                calendarId: String,
                timeMin: String,
                timeMax: String,
                timeZone: String,
                singleEvents: Boolean,
                orderBy: String,
                maxResults: Int,
                pageToken: String?
            ) = Response.success(GoogleEventList(emptyList(), null))
        }
        val requestedPages = mutableListOf<String?>()
        val tasksApi = object : GoogleTasksApi {
            override suspend fun taskLists(auth: String, maxResults: Int, pageToken: String?): retrofit2.Response<GoogleTaskListPage> {
                requestedPages += pageToken
                return if (pageToken == null) {
                    Response.success(GoogleTaskListPage(listOf(GoogleTaskList("empty-list-id", "Vacía")), "next-page"))
                } else {
                    Response.success(GoogleTaskListPage(listOf(GoogleTaskList("second-list-id", "Personal")), null))
                }
            }

            override suspend fun tasks(
                auth: String,
                taskListId: String,
                showCompleted: Boolean,
                showHidden: Boolean,
                maxResults: Int,
                pageToken: String?
            ) = Response.success(GoogleTaskPage(emptyList(), null))
        }

        val lists = GoogleWorkspaceRepository(calendarApi, tasksApi).taskLists("token")

        assertEquals(listOf(null, "next-page"), requestedPages)
        assertEquals(listOf("empty-list-id", "second-list-id"), lists.map { it.id })
        assertEquals(listOf("Vacía", "Personal"), lists.map { it.title })
    }

    @Test
    fun preservesMovedRecurringOccurrencesInLocalTimeAndIgnoresCancelledInstances() = runBlocking {
        var requestedSingleEvents: Boolean? = null
        var requestedTimeZone: String? = null
        val calendarApi = object : GoogleCalendarApi {
            override suspend fun calendars(auth: String, maxResults: Int, pageToken: String?, showHidden: Boolean) =
                Response.success(GoogleCalendarList(listOf(GoogleCalendar("home", "Casa", null)), null))

            override suspend fun events(
                auth: String,
                calendarId: String,
                timeMin: String,
                timeMax: String,
                timeZone: String,
                singleEvents: Boolean,
                orderBy: String,
                maxResults: Int,
                pageToken: String?
            ): Response<GoogleEventList> {
                requestedSingleEvents = singleEvents
                requestedTimeZone = timeZone
                return Response.success(
                    GoogleEventList(
                        listOf(
                            GoogleEvent("moved-instance", "Yoga", "confirmed", GoogleEventTime("2026-10-29T13:30:00Z", null), "yoga-series"),
                            GoogleEvent("cancelled-instance", "Yoga", "cancelled", GoogleEventTime("2026-10-30T13:00:00Z", null), "yoga-series"),
                            GoogleEvent("next-week", "Yoga", "confirmed", GoogleEventTime("2026-11-03T13:00:00Z", null), "yoga-series")
                        ),
                        null
                    )
                )
            }
        }
        val tasksApi = object : GoogleTasksApi {
            override suspend fun taskLists(auth: String, maxResults: Int, pageToken: String?) =
                Response.success(GoogleTaskListPage(emptyList(), null))

            override suspend fun tasks(
                auth: String,
                taskListId: String,
                showCompleted: Boolean,
                showHidden: Boolean,
                maxResults: Int,
                pageToken: String?
            ) = Response.success(GoogleTaskPage(emptyList(), null))
        }

        val repository = GoogleWorkspaceRepository(calendarApi, tasksApi) { ZoneId.of("America/Asuncion") }
        val result = repository.content("access-token", LocalDate.of(2026, 10, 27))

        assertEquals(true, requestedSingleEvents)
        assertEquals("America/Asuncion", requestedTimeZone)
        assertEquals(listOf("Yoga"), result.sessions.map { it.title })
        assertEquals("2026-10-29 10:30:00", result.sessions.single().date_at)
        assertNotNull(result.sessions.single().routine_id)
    }

    private fun emptyTasksApi() = object : GoogleTasksApi {
        override suspend fun taskLists(auth: String, maxResults: Int, pageToken: String?) =
            Response.success(GoogleTaskListPage(emptyList(), null))

        override suspend fun tasks(
            auth: String,
            taskListId: String,
            showCompleted: Boolean,
            showHidden: Boolean,
            maxResults: Int,
            pageToken: String?
        ) = Response.success(GoogleTaskPage(emptyList(), null))
    }
}
