package com.joausaga.familydashboard.data

object TvMonitorIntervals {
    val options = listOf(1, 3, 5, 10, 15, 30, 45, 60)
}

/** Ajustes locales del sondeo de la TV. La IP queda vacía hasta que la persona la configure. */
data class TvMonitorSettings(
    val ipAddress: String = "",
    val pollMinutes: Int = 1
) {
    val isConfigured: Boolean get() = ipAddress.isNotBlank()

    fun validationError(): String? = when {
        ipAddress.isNotBlank() && !TvLocalApi.isIpAddress(ipAddress.trim()) -> "Escribe una dirección IPv4 o IPv6 válida."
        pollMinutes !in TvMonitorIntervals.options -> "Elige una frecuencia de sondeo disponible."
        else -> null
    }
}
