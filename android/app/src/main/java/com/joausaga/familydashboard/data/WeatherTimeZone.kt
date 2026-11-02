package com.joausaga.familydashboard.data

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

internal fun validWeatherZone(timezone: String?): ZoneId? =
    timezone?.takeIf { it.isNotBlank() }?.let { runCatching { ZoneId.of(it) }.getOrNull() }

/** Las cachés antiguas sin coordenadas se siguen mostrando; no acreditan una zona del pronóstico. */
internal fun Forecast.belongsTo(location: Location?): Boolean = location != null && (
    (locationLatitude == null && locationLongitude == null) ||
        (locationLatitude == location.latitude && locationLongitude == location.longitude)
    )

private fun Forecast.zoneFor(location: Location?): ZoneId? =
    if (belongsTo(location) && locationLatitude != null && locationLongitude != null) validWeatherZone(timezone) else null

/** Zona del clima asociado, luego la guardada para esa ubicación y finalmente la del dispositivo. */
internal fun weatherZoneId(location: Location?, forecast: Forecast?, deviceZone: ZoneId = ZoneId.systemDefault()): ZoneId =
    forecast?.zoneFor(location) ?: validWeatherZone(location?.timezone) ?: deviceZone

internal fun Location.withForecastTimeZone(forecast: Forecast?): Location =
    forecast?.zoneFor(this)?.let { zone -> if (timezone == zone.id) this else copy(timezone = zone.id) } ?: this

/** Mismo instante absoluto del reloj del dispositivo, expresado en la zona meteorológica para TV. */
internal fun tvTimeAt(
    deviceNow: LocalDateTime,
    location: Location?,
    forecast: Forecast?,
    deviceZone: ZoneId = ZoneId.systemDefault()
): ZonedDateTime = deviceNow.atZone(deviceZone).withZoneSameInstant(weatherZoneId(location, forecast, deviceZone))
