package com.joausaga.familydashboard.data.repository

import com.joausaga.familydashboard.data.Api
import com.joausaga.familydashboard.data.Forecast
import com.joausaga.familydashboard.data.GeoResponse
import com.joausaga.familydashboard.data.Location
import com.joausaga.familydashboard.data.belongsTo
import com.joausaga.familydashboard.data.emptyForecast
import com.joausaga.familydashboard.data.weatherZoneId
import com.joausaga.familydashboard.data.withForecastTimeZone
import com.joausaga.familydashboard.data.cache.DiskCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.time.ZoneId

class WeatherRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    private val madrid = Location("Madrid", "", 40.4, -3.7)
    private val tokyo = Location("Tokyo", "", 35.6, 139.7, "Asia/Tokyo")

    @Test fun forecastZoneAndRequestedCoordinatesSurviveCacheRestart() = runBlocking {
        val api = object : Api {
            override suspend fun forecast(url: String): Response<Forecast> {
                assertTrue(url.contains("timezone=auto"))
                return Response.success(emptyForecast().copy(timezone = "Europe/Madrid"))
            }
            override suspend fun geocode(url: String): Response<GeoResponse> = error("not used")
        }
        val repo = WeatherRepository(api, DiskCache(folder.root))
        repo.refresh(madrid).getOrThrow()
        val restarted = WeatherRepository(api, DiskCache(folder.root))
        val saved = restarted.forecast.value!!.value
        assertTrue(saved.belongsTo(madrid))
        assertEquals(madrid.latitude, saved.locationLatitude!!, 0.0)
        assertEquals(madrid.longitude, saved.locationLongitude!!, 0.0)
        assertEquals(ZoneId.of("Europe/Madrid"), weatherZoneId(madrid, saved, ZoneId.of("UTC")))
        assertFalse(restarted.isStale(madrid))
        assertTrue(restarted.isStale(tokyo))
    }

    @Test fun lateResponseCannotAssignOldWeatherZoneToNewLocation() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val api = object : Api {
            override suspend fun forecast(url: String): Response<Forecast> {
                started.complete(Unit)
                finish.await()
                return Response.success(emptyForecast().copy(timezone = "Europe/Madrid"))
            }
            override suspend fun geocode(url: String): Response<GeoResponse> = error("not used")
        }
        val repo = WeatherRepository(api, DiskCache(folder.root))
        val pending = async(start = CoroutineStart.UNDISPATCHED) { repo.refresh(madrid) }
        started.await()
        repo.clear()
        finish.complete(Unit)
        val oldResponse = pending.await().getOrThrow()
        assertFalse(oldResponse.belongsTo(tokyo))
        assertEquals(tokyo, tokyo.withForecastTimeZone(oldResponse))
        assertEquals(ZoneId.of("Asia/Tokyo"), weatherZoneId(tokyo, oldResponse, ZoneId.of("UTC")))
        assertTrue(repo.isStale(tokyo))
    }
}
