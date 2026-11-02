package com.joausaga.familydashboard.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Sección temporal de una tarea y la columna de la vista «Por fecha» a la que pertenece (0, 1 o 2). */
enum class TaskSection(val column: Int) {
    Overdue(0), Today(0), Tomorrow(1), ThisWeek(1), Later(2), NoDate(2)
}

/** Fecha de vencimiento de Google Tasks (RFC 3339) como fecha local; null si falta o no se puede interpretar. */
fun taskDueDate(due: String?): LocalDate? =
    due?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/** «Esta semana» son los días posteriores a mañana y hasta el domingo de la semana actual. */
fun taskSection(due: String?, today: LocalDate): TaskSection {
    val date = taskDueDate(due) ?: return TaskSection.NoDate
    val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    return when {
        date.isBefore(today) -> TaskSection.Overdue
        date == today -> TaskSection.Today
        date == today.plusDays(1) -> TaskSection.Tomorrow
        !date.isAfter(endOfWeek) -> TaskSection.ThisWeek
        else -> TaskSection.Later
    }
}

/** Orden por vencimiento (sin fecha al final); es estable, así que los empates conservan el orden recibido. */
fun tasksByDue(tasks: List<Task>): List<Task> = tasks.sortedBy { taskDueDate(it.due) ?: LocalDate.MAX }

/** Tareas por sección, en el orden de la enumeración y sin las secciones vacías. */
fun tasksBySection(tasks: List<Task>, today: LocalDate): Map<TaskSection, List<Task>> {
    val sorted = tasksByDue(tasks)
    return TaskSection.entries.associateWith { section -> sorted.filter { taskSection(it.due, today) == section } }
        .filterValues { it.isNotEmpty() }
}

/** Cifras de la franja de resumen; `week` cuenta desde mañana hasta el domingo. */
data class TasksSummary(val total: Int, val overdue: Int, val today: Int, val week: Int)

fun tasksSummary(tasks: List<Task>, today: LocalDate): TasksSummary {
    val sections = tasks.map { taskSection(it.due, today) }
    return TasksSummary(
        total = tasks.size,
        overdue = sections.count { it == TaskSection.Overdue },
        today = sections.count { it == TaskSection.Today },
        week = sections.count { it == TaskSection.Tomorrow || it == TaskSection.ThisWeek }
    )
}

data class TaskDayCount(val date: LocalDate, val count: Int)
data class TaskTimeline(val overdue: Int, val days: List<TaskDayCount>)

/** Vencidas y número de tareas por cada uno de los `days` días a partir de hoy. */
fun tasksTimeline(tasks: List<Task>, today: LocalDate, days: Int = 14): TaskTimeline {
    val byDate = tasks.mapNotNull { taskDueDate(it.due) }.groupingBy { it }.eachCount()
    return TaskTimeline(
        overdue = byDate.filterKeys { it.isBefore(today) }.values.sum(),
        days = (0 until days).map { TaskDayCount(today.plusDays(it.toLong()), byDate[today.plusDays(it.toLong())] ?: 0) }
    )
}

/** Filtro de la línea de tiempo: sin filtro, solo vencidas o un día concreto. */
sealed interface TaskDayFilter {
    object None : TaskDayFilter
    object Overdue : TaskDayFilter
    data class Day(val date: LocalDate) : TaskDayFilter
}

/** Aplica el filtro por lista (por `task_list_id`) y por día; ambos se combinan. */
fun filterTasks(tasks: List<Task>, listId: Int?, day: TaskDayFilter, today: LocalDate): List<Task> =
    tasks.filter { task ->
        (listId == null || task.task_list_id == listId) && when (day) {
            TaskDayFilter.None -> true
            TaskDayFilter.Overdue -> taskSection(task.due, today) == TaskSection.Overdue
            is TaskDayFilter.Day -> taskDueDate(task.due) == day.date
        }
    }
