package com.lito.planify.dashboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshIntervalsTest {
    @Test
    fun calendarTaskRefreshOptionsAndDefaultAreStable() {
        assertEquals(5, CalendarTaskRefreshInterval.DEFAULT_MINUTES)
        assertEquals(listOf(5, 10, 20, 30, 40, 50, 60), CalendarTaskRefreshInterval.options)
        assertTrue(CalendarTaskRefreshInterval.isValid(20))
        assertFalse(CalendarTaskRefreshInterval.isValid(15))
        assertEquals(20 * 60_000L, CalendarTaskRefreshInterval.maxAgeMillis(20))
    }

    @Test
    fun tvPollOptionsAreRestrictedToTheSpecifiedValues() {
        assertEquals(listOf(1, 3, 5, 10, 15, 30, 45, 60), TvMonitorIntervals.options)
        assertEquals(null, TvMonitorSettings("192.168.1.2", 45).validationError())
        assertTrue(TvMonitorSettings("192.168.1.2", 2).validationError() != null)
    }
}
