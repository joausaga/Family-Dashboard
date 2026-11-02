package com.joausaga.familydashboard.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class UpcomingEventLabelTest {
    @Test
    fun formatsUpcomingTimeAsNaturalSpanishCountdown() {
        val now = LocalDateTime.of(2026, 10, 29, 10, 0)

        assertEquals("en 17 h 8 min", naturalCountdown(now, now.plusHours(17).plusMinutes(8)))
        assertEquals("en 25 min", naturalCountdown(now, now.plusMinutes(25)))
        assertEquals("en 2 d 3 h", naturalCountdown(now, now.plusDays(2).plusHours(3).plusMinutes(59)))
        assertEquals("ahora", naturalCountdown(now, now.plusSeconds(30)))
    }
}
