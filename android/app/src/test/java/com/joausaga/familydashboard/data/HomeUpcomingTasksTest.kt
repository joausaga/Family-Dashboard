package com.joausaga.familydashboard.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeUpcomingTasksTest {
    @Test
    fun selectsTwoSoonestDeadlinesAcrossListsAndKeepsOrderForTies() {
        val tasks = listOf(
            task(1, "Later", "2026-11-04T00:00:00.000Z"),
            task(2, "Soon", "2026-10-30T00:00:00.000Z"),
            task(3, "Same day first", "2026-10-29T00:00:00.000Z"),
            task(4, "Same day second", "2026-10-29T00:00:00.000Z")
        )

        assertEquals(listOf("Same day first", "Same day second"), homeUpcomingTasks(tasks).map { it.title })
    }

    @Test
    fun limitTwoReturnsTheTwoSoonestPendingTasks() {
        val tasks = listOf(
            task(1, "Later", "2026-11-04T00:00:00.000Z"),
            task(2, "Soon", "2026-10-30T00:00:00.000Z"),
            task(3, "Undated", null)
        )

        assertEquals(listOf("Soon", "Later"), homeUpcomingTasks(tasks, limit = 2).map { it.title })
    }

    @Test
    fun undatedTasksFollowDatedTasksAndFillRemainingSlots() {
        val tasks = listOf(
            task(1, "Undated first", null),
            task(2, "Dated", "2026-11-01T00:00:00.000Z"),
            task(3, "Undated second", null),
            task(4, "   ", null)
        )

        assertEquals(listOf("Dated", "Undated first"), homeUpcomingTasks(tasks).map { it.title })
    }

    private fun task(id: Int, title: String, due: String?) = Task(
        id = id,
        task_list_id = id,
        task_list_name = "Lista $id",
        task_list_color = null,
        title = title,
        order = id,
        completed_at = null,
        due = due
    )
}
