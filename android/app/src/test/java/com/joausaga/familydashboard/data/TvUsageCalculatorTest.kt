package com.joausaga.familydashboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TvUsageCalculatorTest {
    private val zone = ZoneId.of("America/Asuncion")

    @Test fun intervalIsSplitAtLocalDayAndMonthBoundaries() {
        val start = LocalDate.of(2026, 10, 31).atTime(23, 30).atZone(zone).toInstant()
        val end = LocalDate.of(2026, 11, 1).atTime(1, 15).atZone(zone).toInstant()
        val history = TvUsageCalculator.history(listOf(transition(start, TvPowerState.ON), transition(end, TvPowerState.OFF)), end, true)

        val view = TvUsageCalculator.summarize(history, end, zone)

        assertEquals(30 * 60_000L, view.dayTotals[LocalDate.of(2026, 10, 31)])
        assertEquals(75 * 60_000L, view.dayTotals[LocalDate.of(2026, 11, 1)])
        assertEquals(30 * 60_000L, view.monthTotals[java.time.YearMonth.of(2026, 10)])
        assertEquals(75 * 60_000L, view.monthTotals[java.time.YearMonth.of(2026, 11)])
    }

    @Test fun hourlyUsageSplitsAtHourAndMidnightBoundaries() {
        val start = LocalDate.of(2026, 10, 28).atTime(23, 30).atZone(zone).toInstant()
        val now = LocalDate.of(2026, 10, 29).atTime(0, 30).atZone(zone).toInstant()
        val history = TvUsageCalculator.history(
            listOf(transition(start, TvPowerState.ON), transition(now, TvPowerState.OFF)), now, true
        )

        val view = TvUsageCalculator.summarize(history, now, zone)

        assertEquals(30 * 60_000L, view.hourlyMillis[0])
        assertEquals(0L, view.hourlyMillis[23])
        assertEquals(30 * 60_000L, view.todayMillis)
        assertEquals(view.todayMillis, view.hourlyMillis.values.sum())
    }

    @Test fun partialAndCrossHourIntervalsAreCountedInEachHour() {
        val start = LocalDate.of(2026, 10, 28).atTime(10, 45).atZone(zone).toInstant()
        val now = LocalDate.of(2026, 10, 28).atTime(11, 15).atZone(zone).toInstant()
        val history = TvUsageCalculator.history(
            listOf(transition(start, TvPowerState.ON), transition(now, TvPowerState.OFF)), now, true
        )

        val view = TvUsageCalculator.summarize(history, now, zone)

        assertEquals(15 * 60_000L, view.hourlyMillis[10])
        assertEquals(15 * 60_000L, view.hourlyMillis[11])
        assertEquals(view.todayMillis, view.hourlyMillis.values.sum())
    }

    @Test fun openSessionCountsOnlyThroughNowAndEmptyHoursStayZero() {
        val start = LocalDate.of(2026, 10, 28).atTime(10, 15).atZone(zone).toInstant()
        val now = LocalDate.of(2026, 10, 28).atTime(10, 45).atZone(zone).toInstant()
        val history = TvUsageCalculator.history(listOf(transition(start, TvPowerState.ON)), start, true)

        val view = TvUsageCalculator.summarize(history, now, zone)

        assertEquals(30 * 60_000L, view.hourlyMillis[10])
        assertEquals(0L, view.hourlyMillis[9])
        assertEquals(0L, view.hourlyMillis[11])
        assertEquals(view.todayMillis, view.hourlyMillis.values.sum())
    }

    @Test fun equalTimestampsUseRowOrderAndProduceZeroDuration() {
        val instant = Instant.parse("2026-10-28T12:00:00Z")
        val history = TvUsageCalculator.history(
            listOf(transition(instant, TvPowerState.ON, 2), transition(instant, TvPowerState.OFF, 3)), instant, true
        )

        assertTrue(history.intervals.isEmpty())
        assertEquals(TvDisplayStatus.OFF, TvUsageCalculator.summarize(history, instant, zone).status)
    }

    @Test fun repeatedStateIsFlaggedAndDoesNotCreateAnExtraInterval() {
        val start = Instant.parse("2026-10-28T10:00:00Z")
        val end = start.plusSeconds(3600)
        val history = TvUsageCalculator.history(
            listOf(transition(start, TvPowerState.ON), transition(start.plusSeconds(600), TvPowerState.ON, 2), transition(end, TvPowerState.OFF, 3)), end, true
        )

        assertEquals(1, history.inconsistentRows)
        assertEquals(60 * 60_000L, TvUsageCalculator.summarize(history, end, zone).todayMillis)
    }

    @Test fun identicalTransitionIsDeduplicatedAndFreshOnSessionIsReported() {
        val start = Instant.parse("2026-10-28T10:00:00Z")
        val duplicate = transition(start, TvPowerState.ON)
        val now = start.plusSeconds(30 * 60L)
        val history = TvUsageCalculator.history(listOf(duplicate, duplicate, transition(now, TvPowerState.OFF, 2)), now, true)

        val view = TvUsageCalculator.summarize(history, now, zone)

        assertEquals(2, history.transitions.size)
        assertEquals(0, history.inconsistentRows)
        assertEquals(TvDisplayStatus.OFF, view.status)
        assertEquals(30 * 60_000L, view.todayMillis)
    }

    @Test fun freshOnSessionShowsElapsedTime() {
        val start = Instant.parse("2026-10-28T10:00:00Z")
        val now = start.plusSeconds(75 * 60L)
        val history = TvUsageCalculator.history(listOf(transition(start, TvPowerState.ON)), start, true)

        val view = TvUsageCalculator.summarize(history, now, zone)

        assertEquals(TvDisplayStatus.ON, view.status)
        assertEquals(75 * 60_000L, view.currentSessionMillis)
    }

    @Test fun openSessionIsCappedAtSixHoursAndThenMarkedUnknown() {
        val start = Instant.parse("2026-10-28T00:00:00Z")
        val history = TvUsageCalculator.history(listOf(transition(start, TvPowerState.ON)), start, true)
        val now = start.plusSeconds(8 * 3600L)

        val view = TvUsageCalculator.summarize(history, now, zone)

        assertEquals(TvDisplayStatus.UNKNOWN, view.status)
        assertNull(view.currentSessionMillis)
        assertEquals(3 * 3600_000L, view.todayMillis)
        assertEquals(6 * 3600_000L, view.dayTotals.values.sum())
    }

    @Test fun availableButEmptyHistoryHasUnknownState() {
        val history = TvUsageCalculator.history(emptyList(), Instant.now(), true)
        assertEquals(TvDisplayStatus.UNKNOWN, TvUsageCalculator.summarize(history, Instant.now(), zone).status)
    }

    @Test fun weeklyTotalStartsOnMondayAndSplitsAtWeekBoundary() {
        val start = LocalDate.of(2026, 10, 25).atTime(23, 30).atZone(zone).toInstant()
        val end = LocalDate.of(2026, 10, 26).atTime(1, 0).atZone(zone).toInstant()
        val history = TvUsageCalculator.history(listOf(transition(start, TvPowerState.ON), transition(end, TvPowerState.OFF)), end, true)

        val view = TvUsageCalculator.summarize(history, end, zone)

        assertEquals(60 * 60_000L, view.weekMillis)
        assertEquals(30 * 60_000L, view.dayTotals[LocalDate.of(2026, 10, 25)])
    }

    @Test fun sourceFailureIsVisibleWithoutTreatingItAsOff() {
        val on = Instant.parse("2026-10-28T10:00:00Z")
        val history = TvUsageCalculator.history(listOf(transition(on, TvPowerState.ON)), on, true)
            .copy(sourceAvailable = false, sourceError = "network")

        assertEquals(TvDisplayStatus.ERROR, TvUsageCalculator.summarize(history, on.plusSeconds(30), zone).status)
    }

    @Test fun severalThousandTransitionsRemainAggregatable() {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val transitions = buildList {
            repeat(5_000) { index ->
                val on = start.plusSeconds(index * 120L)
                add(transition(on, TvPowerState.ON, index * 2L))
                add(transition(on.plusSeconds(60), TvPowerState.OFF, index * 2L + 1))
            }
        }
        val now = transitions.last().let { Instant.ofEpochMilli(it.atMillis).plusSeconds(1) }
        val history = TvUsageCalculator.history(transitions, now, true)

        assertEquals(5_000, history.intervals.size)
        assertEquals(5_000 * 60_000L, TvUsageCalculator.summarize(history, now, zone).dayTotals.values.sum())
    }

    private fun transition(instant: Instant, state: TvPowerState, order: Long = 1) =
        TvTransition(instant.toEpochMilli(), state, "[TV] Samsung", order)
}
