package com.lito.planify.dashboard.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TasksGroupedByListTest {
    @Test
    fun groupsTasksByListAndPreservesIncomingOrder() {
        val tasks = listOf(
            task(1, 10, "Primera"),
            task(2, 20, "Otra lista"),
            task(3, 10, "Segunda")
        )

        val groups = tasksGroupedByList(tasks)

        assertEquals(listOf(10, 20), groups.map { it.first().task_list_id })
        assertEquals(listOf("Primera", "Segunda"), groups.first().map { it.title })
        assertEquals(tasks.map { it.id }.toSet(), groups.flatten().map { it.id }.toSet())
        assertEquals(emptyList<List<Task>>(), tasksGroupedByList(emptyList()))
    }

    @Test
    fun excludesTasksWithBlankOrWhitespaceOnlyTitles() {
        val tasks = listOf(
            task(1, 10, "Pago servicios"),
            task(2, 10, ""),
            task(3, 20, "   "),
            task(4, 20, "Llamar")
        )

        assertEquals(listOf(1, 4), tasksWithTitles(tasks).map { it.id })
        assertEquals(emptyList<Task>(), tasksWithTitles(tasks.filter { it.title.isBlank() }))
    }

    @Test
    fun selectionStartsAllVisibleAndActivatesOnlyNewListsOnReconcile() {
        val first = TaskListVisibilitySelection(emptySet(), emptySet()).reconcile(setOf("list/full-id", "second"))
        assertEquals(setOf("list/full-id", "second"), first.visibleIds)

        val updated = TaskListVisibilitySelection(
            knownIds = setOf("list/full-id", "second"),
            visibleIds = setOf("second")
        ).reconcile(setOf("list/full-id", "second", "new-list"))

        assertEquals(setOf("second", "new-list"), updated.visibleIds)
    }

    @Test
    fun filtersTasksByCompleteGoogleTaskListId() {
        val event = Event(9, 1, "Calendar", null, "Meeting", "2026-10-28 10:00:00", google_calendar_id = "calendar-id")
        val routine = event.copy(id = 10, title = "Workout", routine_id = 3)
        val content = Content(
            events = listOf(event),
            sessions = listOf(routine),
            tasks = listOf(
                task(1, 1, "First").copy(google_task_list_id = "google-list/alpha"),
                task(2, 2, "Second").copy(google_task_list_id = "google-list/beta")
            )
        )

        val filtered = content.visibleInTaskLists(setOf("google-list/beta"))

        assertEquals(listOf("Second"), filtered.tasks.map { it.title })
        assertEquals(listOf(event), filtered.events)
        assertEquals(listOf(routine), filtered.sessions)
        assertEquals(emptyList<Task>(), content.visibleInTaskLists(emptySet()).tasks)
        assertEquals(emptyList<Task>(), content.visibleInTaskLists(setOf("alpha")).tasks)
    }

    private fun task(id: Int, listId: Int, title: String) = Task(
        id = id,
        task_list_id = listId,
        task_list_name = "Lista $listId",
        task_list_color = null,
        title = title,
        order = id,
        completed_at = null
    )
}
