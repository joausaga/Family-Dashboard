package com.joausaga.familydashboard.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TvUsageTrendTest {
    @Test
    fun returnsFiveCompletedDaysAndFillsMissingDaysWithZero() {
        val today = LocalDate.parse("2026-10-30")
        val totals = mapOf(
            today.minusDays(5) to 30L * 60_000L,
            today.minusDays(3) to 90L * 60_000L,
            today to 45L * 60_000L,
            today.plusDays(1) to 8L * 60_000L
        )

        val result = tvUsageLastFiveCompletedDays(today, totals)

        assertEquals((1L..5L).map { today.minusDays(it) }, result.map { it.date })
        assertEquals(listOf(0L, 0L, 90L, 0L, 30L).map { it * 60_000L }, result.map { it.millis })
        assertEquals(false, result.any { it.date == today })
    }

    @Test
    fun emptyHistoryProducesFiveZeroPoints() {
        val today = LocalDate.parse("2026-10-30")

        val result = tvUsageLastFiveCompletedDays(today, emptyMap())

        assertEquals(5, result.size)
        assertEquals(listOf(0L, 0L, 0L, 0L, 0L), result.map { it.millis })
    }

    @Test
    fun averageIncludesZeroDaysAndExcludesToday() {
        val today = LocalDate.parse("2026-10-30")
        val totals = mapOf(
            today.minusDays(1) to 60L * 60_000L,
            today.minusDays(3) to 40L * 60_000L,
            today to 10L * 60L * 60_000L
        )

        assertEquals(20L * 60_000L, tvUsageAverageLastFiveCompletedDays(today, totals))
    }

    @Test
    fun usageDayCountCoversZeroThroughFiveAndExcludesOutsideRange() {
        val today = LocalDate.parse("2026-10-30")
        for (count in 0..5) {
            val totals = buildMap {
                repeat(count) { index -> put(today.minusDays(index + 1L), 1L) }
                put(today, 60L * 60_000L)
                put(today.minusDays(6), 60L * 60_000L)
            }

            assertEquals(count, tvUsageDaysWithUsageLastFiveCompletedDays(today, totals))
        }
    }
}
