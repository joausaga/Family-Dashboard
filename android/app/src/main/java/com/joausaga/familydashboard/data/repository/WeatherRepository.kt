package com.joausaga.familydashboard.data.repository

import com.joausaga.familydashboard.data.Api
import com.joausaga.familydashboard.data.Forecast
import com.joausaga.familydashboard.data.Location
import com.joausaga.familydashboard.data.bodyOrThrow
import com.joausaga.familydashboard.data.withDailyFromHourly
import com.joausaga.familydashboard.data.belongsTo
import com.joausaga.familydashboard.data.cache.Cached
import com.joausaga.familydashboard.data.cache.CachedResource
import com.joausaga.familydashboard.data.cache.DiskCache
import kotlinx.coroutines.flow.StateFlow

/** Previsión de Open-Meteo para la ubicación elegida: el día anterior y 14 días. */
class WeatherRepository(private val api: Api, cache: DiskCache) {
    private val resource = CachedResource<Forecast>(cache, "forecast", Forecast::class.java, MAX_AGE_MS)

    val forecast: StateFlow<Cached<Forecast>?> = resource.data

    fun isStale(location: Location? = null) = resource.isStale() ||
        (location != null && resource.data.value?.value?.belongsTo(location) == false)

    suspend fun refresh(location: Location): Result<Forecast> = resource.refresh {
        api.forecast(Api.forecastUrl(location)).bodyOrThrow().withDailyFromHourly().copy(
            locationLatitude = location.latitude,
            locationLongitude = location.longitude
        )
    }

    /** Al cambiar de ubicación: la previsión anterior ya no sirve. */
    fun clear() = resource.clear()

    companion object {
        const val MAX_AGE_MS = 30 * 60_000L
    }
}
