package com.lito.planify.dashboard.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarVisibilitySelectionTest {
    @Test
    fun newSelectionStartsWithEveryCalendarVisible() {
        val selection = CalendarVisibilitySelection(emptySet(), emptySet()).reconcile(setOf("primary", "work"))

        assertEquals(setOf("primary", "work"), selection.knownIds)
        assertEquals(setOf("primary", "work"), selection.visibleIds)
    }

    @Test
    fun reconciliationPreservesHiddenCalendarsSelectsNewOnesAndDropsRemovedOnes() {
        val previous = CalendarVisibilitySelection(
            knownIds = setOf("primary", "work", "old"),
            visibleIds = setOf("primary", "old")
        )

        val selection = previous.reconcile(setOf("primary", "work", "new"))

        assertEquals(setOf("primary", "work", "new"), selection.knownIds)
        assertEquals(setOf("primary", "new"), selection.visibleIds)
    }

    @Test
    fun filteringContentLeavesTasksUntouchedAndFiltersEventsAndRoutines() {
        val content = Content(
            events = listOf(event("home-event", "home"), event("work-event", "work")),
            sessions = listOf(event("home-routine", "home"), event("work-routine", "work")),
            tasks = listOf(Task(1, 1, "Personal", null, "Comprar leche", 0, null))
        )

        val filtered = content.visibleInCalendars(setOf("home"))

        assertEquals(listOf("home-event"), filtered.events.map { it.title })
        assertEquals(listOf("home-routine"), filtered.sessions.map { it.title })
        assertEquals(content.tasks, filtered.tasks)
    }

    @Test
    fun filteringWeekSupportsEmptyCalendarSelection() {
        val week = Week(
            events = listOf(event("event", "home")),
            sessions = listOf(event("routine", "home"))
        )

        val filtered = week.visibleInCalendars(emptySet())

        assertEquals(emptyList<Event>(), filtered.events)
        assertEquals(emptyList<Event>(), filtered.sessions)
    }

    private fun event(title: String, calendarId: String) = Event(
        id = title.hashCode(),
        calendar_id = calendarId.hashCode(),
        calendar_name = calendarId,
        calendar_color = null,
        title = title,
        date_at = "2026-10-28 10:00:00",
        google_calendar_id = calendarId
    )
}
