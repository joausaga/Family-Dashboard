package com.lito.planify.dashboard.data.repository

import com.lito.planify.dashboard.data.Api
import com.lito.planify.dashboard.data.Forecast
import com.lito.planify.dashboard.data.Location
import com.lito.planify.dashboard.data.bodyOrThrow
import com.lito.planify.dashboard.data.withDailyFromHourly
import com.lito.planify.dashboard.data.belongsTo
import com.lito.planify.dashboard.data.cache.Cached
import com.lito.planify.dashboard.data.cache.CachedResource
import com.lito.planify.dashboard.data.cache.DiskCache
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
