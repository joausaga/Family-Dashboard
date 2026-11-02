package com.joausaga.familydashboard.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyRoutineGroupsTest {
    @Test
    fun groupsOnlyMondayThroughSundayAndSortsDaysAndSessions() {
        val sessions = listOf(
            event(1, "Next Monday", "2026-11-02 08:00:00"),
            event(2, "Sunday", "2026-11-01 09:00:00"),
            event(3, "Wednesday later", "2026-10-28 18:00:00"),
            event(4, "Previous Sunday", "2026-10-25 10:00:00"),
            event(5, "Monday", "2026-10-26 08:00:00"),
            event(6, "Wednesday earlier", "2026-10-28 07:00:00")
        )

        val groups = weeklyRoutineGroups(sessions, LocalDate.of(2026, 10, 28))

        assertEquals(listOf(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 28), LocalDate.of(2026, 11, 1)), groups.keys.toList())
        assertEquals(listOf("Monday"), groups[LocalDate.of(2026, 10, 26)]?.map { it.title })
        assertEquals(listOf("Wednesday earlier", "Wednesday later"), groups[LocalDate.of(2026, 10, 28)]?.map { it.title })
        assertEquals(listOf("Sunday"), groups[LocalDate.of(2026, 11, 1)]?.map { it.title })
        assertEquals(emptyMap<LocalDate, List<Event>>(), weeklyRoutineGroups(emptyList(), LocalDate.of(2026, 10, 28)))
    }

    private fun event(id: Int, title: String, dateAt: String) = Event(
        id = id,
        calendar_id = 1,
        calendar_name = "Family",
        calendar_color = "#123456",
        title = title,
        date_at = dateAt,
        routine_id = 1,
        google_calendar_id = "family"
    )
}
