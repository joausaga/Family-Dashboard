package com.lito.planify.dashboard.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Modelos internos heredados de Planify; se conserva su formato para la caché existente.

data class Person(val id: Int, val name: String, val color: String)

/** Evento o sesión de rutina (`routine_id` no nulo). `date_at` es hora local flotante. */
data class Event(
    val id: Int,
    val calendar_id: Int,
    val calendar_name: String?,
    val calendar_color: String?,
    val title: String,
    val date_at: String,
    val routine_id: Int? = null,
    val persons: List<Person>? = null,
    /** ID completo de Google Calendar; null para eventos de la fuente Planify antigua. */
    val google_calendar_id: String? = null,
    /** Hora local de fin cuando la fuente la proporciona; null para eventos antiguos o de día completo. */
    val end_at: String? = null
) {
    val at: LocalDateTime get() = LocalDateTime.parse(date_at, SQL_DATE_TIME)
    val until: LocalDateTime? get() = end_at?.let { runCatching { LocalDateTime.parse(it, SQL_DATE_TIME) }.getOrNull() }
}

data class Task(
    val id: Int,
    val task_list_id: Int,
    val task_list_name: String?,
    val task_list_color: String?,
    val title: String,
    val order: Int,
    val completed_at: String?,
    /** ID opaco completo de Google Tasks; null para tareas de la fuente Planify antigua. */
    val google_task_list_id: String? = null,
    /** Fecha Google Tasks en formato RFC 3339; puede ser nula. */
    val due: String? = null
)

/** Omite tareas cuyo título vacío no tiene una fila útil que mostrar en la interfaz. */
fun tasksWithTitles(tasks: List<Task>): List<Task> = tasks.filter { it.title.isNotBlank() }

/** Hasta el límite solicitado: vencimientos próximos primero y tareas sin fecha al final, estables. */
fun homeUpcomingTasks(tasks: List<Task>, limit: Int = 2): List<Task> =
    tasksWithTitles(tasks)
        .sortedBy { task -> task.due?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.MAX }
        .take(limit.coerceAtLeast(0))

/** Agrupa tareas por lista, conservando el orden recibido desde Google Tasks. */
fun tasksGroupedByList(tasks: List<Task>): List<List<Task>> = tasks.groupBy { it.task_list_id }.values.toList()

/** Contenido de Hoy (ver ContentRepository). */
data class Content(
    val events: List<Event>,
    val sessions: List<Event>,
    val tasks: List<Task>
)

/** Una semana del calendario (lunes a domingo), descargada al navegar hasta ella. */
data class Week(val events: List<Event>, val sessions: List<Event>)

/** Cinco fechas para la fila de Inicio: hoy más los cuatro días siguientes. */
fun homeCalendarDates(today: LocalDate): List<LocalDate> = (0L..4L).map(today::plusDays)

/** Lunes distintos que deben cargarse para dibujar el rango de Inicio. */
fun homeCalendarWeekStarts(today: LocalDate): List<LocalDate> = homeCalendarDates(today)
    .map { it.with(java.time.DayOfWeek.MONDAY) }
    .distinct()

/** Agrupa las sesiones que caen en la semana local de lunes a domingo, ordenadas por día y hora. */
fun weeklyRoutineGroups(sessions: List<Event>, monday: LocalDate): Map<LocalDate, List<Event>> {
    val start = monday.with(java.time.DayOfWeek.MONDAY)
    val end = start.plusDays(7)
    return sessions
        .filter { it.at.toLocalDate() >= start && it.at.toLocalDate() < end }
        .sortedBy { it.at }
        .groupBy { it.at.toLocalDate() }
        .toSortedMap()
}

/** Estado persistido de selección; la reconciliación conserva lo conocido y activa los IDs recién descubiertos. */
data class CalendarVisibilitySelection(val knownIds: Set<String>, val visibleIds: Set<String>) {
    fun reconcile(availableIds: Set<String>): CalendarVisibilitySelection = CalendarVisibilitySelection(
        knownIds = availableIds,
        visibleIds = if (knownIds.isEmpty() && visibleIds.isEmpty()) availableIds
        else (visibleIds intersect availableIds) + (availableIds - knownIds)
    )
}

fun Content.visibleInCalendars(calendarIds: Set<String>): Content = copy(
    events = events.filter { it.google_calendar_id?.let(calendarIds::contains) == true },
    sessions = sessions.filter { it.google_calendar_id?.let(calendarIds::contains) == true }
)

/** Filtra tareas por IDs completos de Google; no compara los IDs enteros derivados usados por la UI. */
fun Content.visibleInTaskLists(taskListIds: Set<String>): Content = copy(
    tasks = tasks.filter { it.google_task_list_id?.let(taskListIds::contains) == true }
)

/** Estado de selección de listas, con las listas nuevas activadas al reconciliar la enumeración. */
data class TaskListVisibilitySelection(val knownIds: Set<String>, val visibleIds: Set<String>) {
    fun reconcile(availableIds: Set<String>): TaskListVisibilitySelection = TaskListVisibilitySelection(
        knownIds = availableIds,
        visibleIds = if (knownIds.isEmpty() && visibleIds.isEmpty()) availableIds
        else (visibleIds intersect availableIds) + (availableIds - knownIds)
    )
}

fun Week.visibleInCalendars(calendarIds: Set<String>): Week = copy(
    events = events.filter { it.google_calendar_id?.let(calendarIds::contains) == true },
    sessions = sessions.filter { it.google_calendar_id?.let(calendarIds::contains) == true }
)

val SQL_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

// --- Open-Meteo ---

data class Location(val name: String, val detail: String, val latitude: Double, val longitude: Double)

data class GeoResponse(val results: List<GeoResult>? = null)

data class GeoResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null
)

data class Forecast(
    val current: Current,
    val hourly: Hourly,
    val daily: Daily
)

/**
 * La máxima y la mínima de cada día, sacadas de sus horas. Open-Meteo calcula las diarias con más detalle que la
 * hora en punto (cada 15 minutos los primeros días, y a partir del octavo con otro modelo), así que no coincidían
 * con la gráfica por horas: 28,5 en la gráfica y 28,6 en el resumen (28° y 29° una vez redondeados), o hasta 2–3°
 * de diferencia la segunda semana. Se hace una vez al recibir la previsión, antes de guardarla.
 * Los días sin todas sus horas (23 el día del cambio de hora) se quedan con los valores de Open-Meteo.
 */
fun Forecast.withDailyFromHourly(): Forecast {
    val max = HashMap<String, Double>()
    val min = HashMap<String, Double>()
    val hours = HashMap<String, Int>()
    hourly.time.forEachIndexed { i, time ->
        val t = hourly.temperature_2m.getOrNull(i) ?: return@forEachIndexed
        val date = time.substring(0, 10)
        max[date] = maxOf(max[date] ?: t, t)
        min[date] = minOf(min[date] ?: t, t)
        hours[date] = (hours[date] ?: 0) + 1
    }
    fun complete(date: String) = (hours[date] ?: 0) >= 23
    return copy(
        daily = daily.copy(
            temperature_2m_max = daily.time.mapIndexed { i, date -> if (complete(date)) max[date] else daily.temperature_2m_max.getOrNull(i) },
            temperature_2m_min = daily.time.mapIndexed { i, date -> if (complete(date)) min[date] else daily.temperature_2m_min.getOrNull(i) }
        )
    )
}

data class Current(
    val time: String,
    val temperature_2m: Double,
    val relative_humidity_2m: Int?,
    val weather_code: Int,
    val wind_speed_10m: Double,
    val wind_direction_10m: Double,
    val is_day: Int
)

data class Hourly(
    val time: List<String>,
    val temperature_2m: List<Double?>,
    val precipitation_probability: List<Int?>,
    val precipitation: List<Double?>,
    val weather_code: List<Int?>,
    val is_day: List<Int?>
)

data class Daily(
    val time: List<String>,
    val weather_code: List<Int?>,
    val temperature_2m_max: List<Double?>,
    val temperature_2m_min: List<Double?>,
    val precipitation_probability_max: List<Int?>,
    val precipitation_sum: List<Double?>,
    val wind_speed_10m_max: List<Double?>,
    val sunrise: List<String>,
    val sunset: List<String>
)
