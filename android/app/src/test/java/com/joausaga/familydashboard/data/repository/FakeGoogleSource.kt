package com.joausaga.familydashboard.data.repository

import com.joausaga.familydashboard.data.GoogleCalendar
import com.joausaga.familydashboard.data.GoogleCalendarApi
import com.joausaga.familydashboard.data.GoogleCalendarList
import com.joausaga.familydashboard.data.GoogleEvent
import com.joausaga.familydashboard.data.GoogleEventList
import com.joausaga.familydashboard.data.GoogleEventTime
import com.joausaga.familydashboard.data.GoogleTask
import com.joausaga.familydashboard.data.GoogleTaskList
import com.joausaga.familydashboard.data.GoogleTaskListPage
import com.joausaga.familydashboard.data.GoogleTaskPage
import com.joausaga.familydashboard.data.GoogleTasksApi
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.time.ZoneId

/** Google REST fake shared by cache/repository regression tests; never accesses the network. */
internal class FakeGoogleSource : GoogleCalendarApi, GoogleTasksApi {
    var failure: Exception? = null
    var status = 200
    val repository get() = GoogleWorkspaceRepository(this, this) { ZoneId.of("UTC") }

    private fun <T> respond(body: T): Response<T> {
        failure?.let { throw it }
        return if (status == 200) Response.success(body) else Response.error(status, "".toResponseBody())
    }

    override suspend fun calendars(auth: String, maxResults: Int, pageToken: String?, showHidden: Boolean) =
        respond(GoogleCalendarList(listOf(GoogleCalendar("home", "Casa", "#000000", primary = true)), null))

    override suspend fun events(auth: String, calendarId: String, timeMin: String, timeMax: String, timeZone: String,
        singleEvents: Boolean, orderBy: String, maxResults: Int, pageToken: String?) = respond(
        GoogleEventList(listOf(
            GoogleEvent("event", "Evento", "confirmed", GoogleEventTime("${timeMin.take(10)}T10:00:00Z", null), null),
            GoogleEvent("routine", "Rutina", "confirmed", GoogleEventTime("${timeMin.take(10)}T11:00:00Z", null), "series")
        ), null)
    )

    override suspend fun taskLists(auth: String, maxResults: Int, pageToken: String?) =
        respond(GoogleTaskListPage(listOf(GoogleTaskList("tasks", "Casa")), null))

    override suspend fun tasks(auth: String, taskListId: String, showCompleted: Boolean, showHidden: Boolean,
        maxResults: Int, pageToken: String?) =
        respond(GoogleTaskPage(listOf(GoogleTask("task", "Tarea", "needsAction")), null))
}
