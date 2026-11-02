package com.joausaga.familydashboard.data

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** Respuestas mínimas de Google Calendar y Google Tasks; los IDs de Google son cadenas opacas. */
data class GoogleCalendarList(val items: List<GoogleCalendar>?, val nextPageToken: String?)
data class GoogleCalendar(val id: String, val summary: String?, val backgroundColor: String?, val primary: Boolean? = null, val hidden: Boolean? = null)
data class GoogleEventList(val items: List<GoogleEvent>?, val nextPageToken: String?)
data class GoogleEvent(
    val id: String,
    val summary: String?,
    val status: String?,
    val start: GoogleEventTime?,
    val recurringEventId: String?,
    val end: GoogleEventTime? = null
)
data class GoogleEventTime(val dateTime: String?, val date: String?)
data class GoogleTaskListPage(val items: List<GoogleTaskList>?, val nextPageToken: String?)
data class GoogleTaskList(val id: String, val title: String?)
data class GoogleTaskPage(val items: List<GoogleTask>?, val nextPageToken: String?)
data class GoogleTask(val id: String, val title: String?, val status: String?, val due: String? = null)

interface GoogleCalendarApi {
    @GET("users/me/calendarList")
    suspend fun calendars(
        @Header("Authorization") auth: String,
        @Query("maxResults") maxResults: Int = 250,
        @Query("pageToken") pageToken: String? = null,
        @Query("showHidden") showHidden: Boolean = true
    ): Response<GoogleCalendarList>

    @GET("calendars/{calendarId}/events")
    suspend fun events(
        @Header("Authorization") auth: String,
        @Path("calendarId") calendarId: String,
        @Query("timeMin") timeMin: String,
        @Query("timeMax") timeMax: String,
        @Query("timeZone") timeZone: String,
        @Query("singleEvents") singleEvents: Boolean = true,
        @Query("orderBy") orderBy: String = "startTime",
        @Query("maxResults") maxResults: Int = 2500,
        @Query("pageToken") pageToken: String? = null
    ): Response<GoogleEventList>
}

interface GoogleTasksApi {
    @GET("users/@me/lists")
    suspend fun taskLists(
        @Header("Authorization") auth: String,
        @Query("maxResults") maxResults: Int = 1000,
        @Query("pageToken") pageToken: String? = null
    ): Response<GoogleTaskListPage>

    @GET("lists/{taskListId}/tasks")
    suspend fun tasks(
        @Header("Authorization") auth: String,
        @Path("taskListId") taskListId: String,
        @Query("showCompleted") showCompleted: Boolean = false,
        @Query("showHidden") showHidden: Boolean = false,
        @Query("maxResults") maxResults: Int = 100,
        @Query("pageToken") pageToken: String? = null
    ): Response<GoogleTaskPage>
}

interface GoogleOAuthApi {
    @FormUrlEncoded
    @POST("revoke")
    suspend fun revoke(@Field("token") token: String): Response<Unit>
}

/** Clientes REST pequeños para no incorporar las bibliotecas completas de Calendar ni Tasks. */
object GoogleWorkspaceApis {
    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val calendar: GoogleCalendarApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/calendar/v3/")
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleCalendarApi::class.java)
    }

    val tasks: GoogleTasksApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://tasks.googleapis.com/tasks/v1/")
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleTasksApi::class.java)
    }

    val oauth: GoogleOAuthApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://oauth2.googleapis.com/")
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleOAuthApi::class.java)
    }

}
