package com.joausaga.familydashboard.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskSectionsTest {
    private fun task(id: Int, due: String?, list: Int = 1, title: String = "t$id") =
        Task(id, list, "L$list", null, title, id, null, due = due)

    private val wednesday = LocalDate.parse("2026-11-04")

    @Test
    fun sectionsFollowTheCalendarWeekUntilSunday() {
        assertEquals(TaskSection.Overdue, taskSection("2026-11-03T00:00:00.000Z", wednesday))
        assertEquals(TaskSection.Today, taskSection("2026-11-04T00:00:00.000Z", wednesday))
        assertEquals(TaskSection.Tomorrow, taskSection("2026-11-05T00:00:00.000Z", wednesday))
        assertEquals(TaskSection.ThisWeek, taskSection("2026-11-06", wednesday))
        assertEquals(TaskSection.ThisWeek, taskSection("2026-11-08", wednesday)) // domingo
        assertEquals(TaskSection.Later, taskSection("2026-11-09", wednesday))
        assertEquals(TaskSection.NoDate, taskSection(null, wednesday))
        assertEquals(TaskSection.NoDate, taskSection("no-es-fecha", wednesday))
    }

    @Test
    fun saturdayAndSundayHaveNoThisWeekSection() {
        val saturday = LocalDate.parse("2026-11-07")
        assertEquals(TaskSection.Tomorrow, taskSection("2026-11-08", saturday))
        assertEquals(TaskSection.Later, taskSection("2026-11-09", saturday))
        val sunday = LocalDate.parse("2026-11-08")
        assertEquals(TaskSection.Today, taskSection("2026-11-08", sunday))
        assertEquals(TaskSection.Tomorrow, taskSection("2026-11-09", sunday))
        assertEquals(TaskSection.Later, taskSection("2026-11-10", sunday))
    }

    @Test
    fun sectionsAcrossMonthAndYearBoundaries() {
        val newYearsEve = LocalDate.parse("2026-12-31") // jueves
        assertEquals(TaskSection.Tomorrow, taskSection("2027-01-01", newYearsEve))
        assertEquals(TaskSection.ThisWeek, taskSection("2027-01-03", newYearsEve))
        assertEquals(TaskSection.Later, taskSection("2027-01-04", newYearsEve))
    }

    @Test
    fun columnsGroupSectionsInPairs() {
        assertEquals(listOf(0, 0, 1, 1, 2, 2), TaskSection.entries.map { it.column })
    }

    @Test
    fun tasksAreSortedByDueDateWithStableTies() {
        val tasks = listOf(task(1, "2026-11-10"), task(2, null), task(3, "2026-11-05"), task(4, "2026-11-05"), task(5, "2026-11-01"))
        assertEquals(listOf(5, 3, 4, 1, 2), tasksByDue(tasks).map { it.id })
    }

    @Test
    fun bySectionOmitsEmptySectionsAndKeepsOrder() {
        val tasks = listOf(task(1, "2026-11-20"), task(2, "2026-11-04"), task(3, "2026-11-01"), task(4, null))
        val grouped = tasksBySection(tasks, wednesday)
        assertEquals(listOf(TaskSection.Overdue, TaskSection.Today, TaskSection.Later, TaskSection.NoDate), grouped.keys.toList())
        assertEquals(listOf(3), grouped.getValue(TaskSection.Overdue).map { it.id })
    }

    @Test
    fun summaryCountsWeekFromTomorrowToSunday() {
        val tasks = listOf(
            task(1, "2026-11-02"), task(2, "2026-11-04"), task(3, "2026-11-04"), task(4, "2026-11-05"),
            task(5, "2026-11-08"), task(6, "2026-11-09"), task(7, null)
        )
        assertEquals(TasksSummary(total = 7, overdue = 1, today = 2, week = 2), tasksSummary(tasks, wednesday))
        assertEquals(TasksSummary(0, 0, 0, 0), tasksSummary(emptyList(), wednesday))
    }

    @Test
    fun timelineCountsOverdueAndEachDayAndIgnoresOutOfRange() {
        val tasks = listOf(task(1, "2026-10-30"), task(2, "2026-11-01"), task(3, "2026-11-04"), task(4, "2026-11-04"), task(5, "2026-11-17"), task(6, "2026-11-18"), task(7, null))
        val timeline = tasksTimeline(tasks, wednesday, 14)
        assertEquals(2, timeline.overdue)
        assertEquals(14, timeline.days.size)
        assertEquals(wednesday, timeline.days.first().date)
        assertEquals(2, timeline.days.first().count)
        assertEquals(LocalDate.parse("2026-11-17"), timeline.days.last().date)
        assertEquals(1, timeline.days.last().count)
        assertEquals(0, timeline.days[1].count)
    }

    @Test
    fun filtersCombineListAndDay() {
        val tasks = listOf(task(1, "2026-11-04", list = 1), task(2, "2026-11-04", list = 2), task(3, "2026-11-01", list = 1), task(4, null, list = 2))
        assertEquals(listOf(1, 2, 3, 4), filterTasks(tasks, null, TaskDayFilter.None, wednesday).map { it.id })
        assertEquals(listOf(1, 3), filterTasks(tasks, 1, TaskDayFilter.None, wednesday).map { it.id })
        assertEquals(listOf(1, 2), filterTasks(tasks, null, TaskDayFilter.Day(wednesday), wednesday).map { it.id })
        assertEquals(listOf(2), filterTasks(tasks, 2, TaskDayFilter.Day(wednesday), wednesday).map { it.id })
        assertEquals(listOf(3), filterTasks(tasks, null, TaskDayFilter.Overdue, wednesday).map { it.id })
        assertEquals(emptyList<Int>(), filterTasks(tasks, 2, TaskDayFilter.Overdue, wednesday).map { it.id })
        assertNull(taskDueDate("   "))
    }
}
