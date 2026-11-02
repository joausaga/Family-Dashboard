package com.joausaga.familydashboard.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HourlyUsageLabelTest {
    @Test
    fun rendersOnlyActiveHourValuesAndCapsAtOneHour() {
        assertEquals("", formatHourlyMinutes(0))
        assertEquals("", formatHourlyMinutes(-3))
        assertEquals("1m", formatHourlyMinutes(1))
        assertEquals("30m", formatHourlyMinutes(30))
        assertEquals("60m", formatHourlyMinutes(60))
        assertEquals("60m", formatHourlyMinutes(75))
    }
}
