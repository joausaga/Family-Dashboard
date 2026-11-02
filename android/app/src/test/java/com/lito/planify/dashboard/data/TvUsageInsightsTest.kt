package com.lito.planify.dashboard.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TvUsageInsightsTest {
    private fun min(m: Long) = m * 60_000L

    @Test
    fun weekComparisonUsesSameStretchOfPreviousWeek() {
        val today = LocalDate.parse("2026-11-04") // miércoles
        val totals = mapOf(
            LocalDate.parse("2026-11-02") to min(60), LocalDate.parse("2026-11-04") to min(30),
            LocalDate.parse("2026-10-26") to min(20), LocalDate.parse("2026-10-28") to min(10),
            LocalDate.parse("2026-10-29") to min(500) // jueves anterior: fuera del tramo
        )
        val result = tvUsageWeekComparison(today, totals)
        assertEquals(min(90), result.current)
        assertEquals(min(30), result.previous)
        assertEquals(min(60), result.delta)
    }

    @Test
    fun weekComparisonOnMondayComparesOnlyMondays() {
        val today = LocalDate.parse("2026-11-02")
        val totals = mapOf(today to min(15), LocalDate.parse("2026-10-26") to min(40), LocalDate.parse("2026-10-27") to min(99))
        val result = tvUsageWeekComparison(today, totals)
        assertEquals(min(15), result.current)
        assertEquals(min(40), result.previous)
    }

    @Test
    fun monthComparisonUsesSameDayOfPreviousMonth() {
        val today = LocalDate.parse("2026-11-03")
        val totals = mapOf(
            LocalDate.parse("2026-11-01") to min(10), LocalDate.parse("2026-11-03") to min(20),
            LocalDate.parse("2026-10-01") to min(5), LocalDate.parse("2026-10-03") to min(7),
            LocalDate.parse("2026-10-04") to min(900)
        )
        val result = tvUsageMonthComparison(today, totals)
        assertEquals(min(30), result.current)
        assertEquals(min(12), result.previous)
    }

    @Test
    fun monthComparisonHandlesShorterPreviousMonth() {
        val today = LocalDate.parse("2026-03-31")
        val totals = mapOf(LocalDate.parse("2026-03-31") to min(10), LocalDate.parse("2026-02-28") to min(25), LocalDate.parse("2026-02-01") to min(5))
        val result = tvUsageMonthComparison(today, totals)
        assertEquals(min(10), result.current)
        assertEquals(min(30), result.previous)
    }

    @Test
    fun comparisonsAcrossYearBoundary() {
        val today = LocalDate.parse("2027-01-02")
        val totals = mapOf(LocalDate.parse("2027-01-01") to min(10), LocalDate.parse("2026-12-01") to min(4), LocalDate.parse("2026-12-02") to min(6))
        val result = tvUsageMonthComparison(today, totals)
        assertEquals(min(10), result.current)
        assertEquals(min(10), result.previous)
    }

    @Test
    fun peakIgnoresZerosAndRespectsRange() {
        val totals = mapOf(
            LocalDate.parse("2026-10-01") to min(300), LocalDate.parse("2026-10-20") to min(120),
            LocalDate.parse("2026-10-21") to min(120), LocalDate.parse("2026-10-22") to 0L
        )
        assertEquals(LocalDate.parse("2026-10-01"), tvUsagePeak(totals)?.date)
        assertEquals(LocalDate.parse("2026-10-21"), tvUsagePeak(totals, from = LocalDate.parse("2026-10-10"))?.date)
        assertNull(tvUsagePeak(totals, from = LocalDate.parse("2026-10-22")))
        assertNull(tvUsagePeak(emptyMap()))
    }

    @Test
    fun busiestHourReturnsEarliestOnTie() {
        val hourly = (0..23).associateWith { 0L }.toMutableMap()
        hourly[20] = min(30); hourly[21] = min(30); hourly[22] = min(10)
        assertEquals(20 to min(30), tvBusiestHour(hourly))
        assertNull(tvBusiestHour((0..23).associateWith { 0L }))
    }

    @Test
    fun heatmapIsMondayAlignedAndMarksTodayAndFuture() {
        val today = LocalDate.parse("2026-11-04") // miércoles
        val totals = mapOf(today to min(30), LocalDate.parse("2026-10-19") to min(60))
        val weeks = tvHeatmapWeeks(today, 3, totals)
        assertEquals(3, weeks.size)
        assertTrue(weeks.all { it.size == 7 })
        assertEquals(LocalDate.parse("2026-10-19"), weeks[0][0].date)
        assertEquals(min(60), weeks[0][0].millis)
        val last = weeks.last()
        assertEquals(LocalDate.parse("2026-11-02"), last[0].date)
        assertTrue(last[2].today)
        assertTrue(last.take(3).none { it.future })
        assertTrue(last.drop(3).all { it.future })
    }

    @Test
    fun heatLevelsSplitByQuartersOfPeak() {
        assertEquals(0, tvHeatLevel(0, 100))
        assertEquals(0, tvHeatLevel(10, 0))
        assertEquals(1, tvHeatLevel(1, 100))
        assertEquals(1, tvHeatLevel(25, 100))
        assertEquals(2, tvHeatLevel(26, 100))
        assertEquals(3, tvHeatLevel(75, 100))
        assertEquals(4, tvHeatLevel(76, 100))
        assertEquals(4, tvHeatLevel(100, 100))
    }
}
