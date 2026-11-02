package com.joausaga.familydashboard.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class WeatherTimeZoneTest {
    private val deviceZone = ZoneId.of("UTC")
    private val location = Location("Madrid", "España", 40.4, -3.7, "Europe/Madrid")

    @Test fun geocodingAndOldJsonRemainCompatible() {
        val gson = Gson()
        val result = gson.fromJson("""{"name":"Madrid","latitude":40.4,"longitude":-3.7,"timezone":"Europe/Madrid"}""", GeoResult::class.java)
        assertEquals("Europe/Madrid", result.timezone)
        val old = gson.fromJson("""{"name":"Antes","detail":"","latitude":40.4,"longitude":-3.7}""", Location::class.java)
        assertNull(old.timezone)
        val oldForecast = gson.fromJson(gson.toJson(emptyForecast()), Forecast::class.java)
        assertTrue(oldForecast.belongsTo(old))
        assertEquals(deviceZone, weatherZoneId(old, oldForecast, deviceZone))
        assertEquals(location, gson.fromJson(gson.toJson(location), Location::class.java))
    }

    @Test fun associatedForecastIsAuthoritativeAndInvalidZonesFallBack() {
        val associated = emptyForecast().copy(timezone = "Europe/London", locationLatitude = location.latitude, locationLongitude = location.longitude)
        assertEquals(ZoneId.of("Europe/London"), weatherZoneId(location, associated, deviceZone))
        assertEquals(ZoneId.of("Europe/Madrid"), weatherZoneId(location, associated.copy(timezone = "not/a-zone"), deviceZone))
        assertEquals(deviceZone, weatherZoneId(location.copy(timezone = "invalid"), associated.copy(timezone = ""), deviceZone))
        assertEquals(deviceZone, weatherZoneId(null, associated, deviceZone))
        assertEquals(deviceZone, weatherZoneId(null, null, deviceZone))
    }

    @Test fun oldOrForeignForecastCannotAssignItsZoneToANewLocation() {
        val foreign = emptyForecast().copy(timezone = "Asia/Tokyo", locationLatitude = 35.6, locationLongitude = 139.7)
        assertFalse(foreign.belongsTo(location))
        assertEquals(ZoneId.of("Europe/Madrid"), weatherZoneId(location, foreign, deviceZone))
        assertEquals(location, location.withForecastTimeZone(foreign))
        assertEquals(ZoneId.of("Europe/Madrid"), weatherZoneId(location, foreign.copy(locationLatitude = null, locationLongitude = null), deviceZone))
        assertFalse(foreign.copy(locationLatitude = null).belongsTo(location))
    }

    @Test fun acceptedZoneIsSavedWithLocationAndWorksOfflineAfterRestart() {
        val old = location.copy(timezone = null)
        val forecast = emptyForecast().copy(timezone = "Europe/Madrid", locationLatitude = location.latitude, locationLongitude = location.longitude)
        val saved = old.withForecastTimeZone(forecast)
        val restored = Gson().fromJson(Gson().toJson(saved), Location::class.java)
        assertEquals(ZoneId.of("Europe/Madrid"), weatherZoneId(restored, null, deviceZone))
        assertEquals(location, restored)
    }

    @Test fun presentationUsesWeatherDateHourAndTheSameAbsoluteInstant() {
        val now = LocalDateTime.parse("2026-11-01T00:30:00")
        val place = location.copy(timezone = "America/New_York")
        val time = tvTimeAt(now, place, null, deviceZone)
        assertEquals(LocalDate.of(2026, 10, 31), time.toLocalDate())
        assertEquals(20, time.hour)
        assertEquals(Instant.parse("2026-11-01T00:30:00Z"), time.toInstant())
        assertEquals(now, tvTimeAt(now, null, null, deviceZone).toLocalDateTime())
    }

    @Test fun changingWeatherLocationRepartitionsMonthsWithoutChangingHistory() {
        val start = Instant.parse("2026-11-01T00:30:00Z")
        val end = start.plusSeconds(3600)
        val history = history(start, end)
        val before = Gson().toJson(history)
        val utc = TvUsageCalculator.summarize(history, end, weatherZoneId(location.copy(timezone = "UTC"), null, deviceZone))
        val west = TvUsageCalculator.summarize(history, end, weatherZoneId(location.copy(timezone = "America/New_York"), null, deviceZone))
        assertEquals(mapOf(YearMonth.of(2026, 11) to 3_600_000L), utc.monthTotals)
        assertEquals(mapOf(YearMonth.of(2026, 10) to 3_600_000L), west.monthTotals)
        assertEquals(utc.dayTotals.values.sum(), west.dayTotals.values.sum())
        assertEquals(before, Gson().toJson(history))
    }

    @Test fun changingWeatherLocationRepartitionsWeekAndTodayTogether() {
        val start = Instant.parse("2026-11-01T23:30:00Z")
        val end = start.plusSeconds(3600)
        val history = history(start, end)
        val utc = TvUsageCalculator.summarize(history, end, deviceZone)
        val westZone = weatherZoneId(location.copy(timezone = "America/New_York"), null, deviceZone)
        val west = TvUsageCalculator.summarize(history, end, westZone)
        assertEquals(1_800_000L, utc.todayMillis)
        assertEquals(1_800_000L, utc.weekMillis)
        assertEquals(3_600_000L, west.todayMillis)
        assertEquals(3_600_000L, west.weekMillis)
        val today = end.atZone(westZone).toLocalDate()
        assertEquals(west.todayMillis, west.dayTotals[today])
        assertEquals(west.todayMillis, west.hourlyMillis.values.sum())
        assertEquals(LocalDate.of(2026, 11, 1), today)
    }

    @Test fun weatherZoneHandlesSkippedAndRepeatedDstHoursWithoutLosingDuration() {
        val zone = weatherZoneId(location, null, deviceZone)
        val springStart = Instant.parse("2026-03-29T00:30:00Z")
        val springEnd = springStart.plusSeconds(7200)
        val spring = TvUsageCalculator.summarize(history(springStart, springEnd), springEnd, zone)
        assertEquals(7_200_000L, spring.todayMillis)
        assertEquals(0L, spring.hourlyMillis[2])
        assertEquals(spring.todayMillis, spring.hourlyMillis.values.sum())
        val fallStart = Instant.parse("2026-10-25T00:30:00Z")
        val fallEnd = fallStart.plusSeconds(7200)
        val fall = TvUsageCalculator.summarize(history(fallStart, fallEnd), fallEnd, zone)
        assertEquals(7_200_000L, fall.todayMillis)
        assertEquals(5_400_000L, fall.hourlyMillis[2])
        assertEquals(fall.todayMillis, fall.hourlyMillis.values.sum())
    }

    private fun history(start: Instant, end: Instant) = TvUsageCalculator.history(listOf(
        TvTransition(start.toEpochMilli(), TvPowerState.ON, "local", 1),
        TvTransition(end.toEpochMilli(), TvPowerState.OFF, "local", 2)
    ), end, true, sourceRowCounts = mapOf("legacy" to 2), sourceOrders = mapOf("legacy" to 1L))
}

/** Minimal valid Open-Meteo fixture, also used by weather repository regression tests. */
internal fun emptyForecast() = Forecast(
    Current("2026-11-02T12:00", 20.0, 50, 0, 5.0, 0.0, 1),
    Hourly(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
    Daily(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
)
