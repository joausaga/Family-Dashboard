package com.lito.planify.dashboard.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HomeCalendarRangeTest {
    @Test fun homeDatesStartTodayAndIncludeFourFollowingDays() {
        val today = LocalDate.of(2026, 10, 28)

        assertEquals((0L..4L).map(today::plusDays), homeCalendarDates(today))
    }

    @Test fun fiveDayRangeStartingFridayLoadsBothCalendarWeeks() {
        val friday = LocalDate.of(2026, 10, 30)

        assertEquals(
            listOf(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2)),
            homeCalendarWeekStarts(friday)
        )
    }

    @Test fun fiveDayRangeInsideTheWeekLoadsOnlyOneCalendarWeek() {
        assertEquals(listOf(LocalDate.of(2026, 10, 26)), homeCalendarWeekStarts(LocalDate.of(2026, 10, 26)))
    }
}
