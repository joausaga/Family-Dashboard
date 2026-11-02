package com.lito.planify.dashboard.ui

import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Separación mínima entre encendido y apagado, en minutos. */
internal const val MIN_WINDOW_MINUTES = 15
internal const val MINUTES_PER_DAY = 24 * 60

/**
 * Mensaje de error si la hora elegida deja el horario inválido (encendido al menos 15 minutos antes del apagado), o
 * null si es válida. `editingOn` indica si `minutes` sustituye a la hora de encendido o a la de apagado.
 */
internal fun scheduleTimeError(editingOn: Boolean, minutes: Int, on: Int, off: Int): String? {
    val newOn = if (editingOn) minutes else on
    val newOff = if (editingOn) off else minutes
    return if (newOff - newOn < MIN_WINDOW_MINUTES) "El apagado debe ser al menos 15 minutos después del encendido." else null
}

/** «7:00 AM» / «11:30 PM» a partir de minutos desde medianoche (0 es «12:00 AM»), en el formato de 12 horas del selector. */
internal fun scheduleTimeLabel(minutes: Int): String {
    val hour = (minutes / 60) % 24
    val hour12 = if (hour % 12 == 0) 12 else hour % 12
    return "%d:%02d %s".format(hour12, minutes % 60, if (hour < 12) "AM" else "PM")
}

/** Un servicio está al día si se sincronizó hacía como mucho tres veces su frecuencia esperada. */
internal fun serviceIsFresh(ageMillis: Long?, expectedMinutes: Int): Boolean =
    ageMillis != null && ageMillis >= 0 && ageMillis <= 3L * expectedMinutes * 60_000L

/** Motivo legible de un fallo al probar la conexión con la TV. */
internal fun tvTestFailureMessage(error: Throwable): String = when (error) {
    is SocketTimeoutException -> "La TV no respondió a tiempo."
    is ConnectException, is NoRouteToHostException, is UnknownHostException -> "No se pudo conectar con la TV. Comprueba la IP y la red."
    is IllegalArgumentException -> error.message ?: "La dirección IP no es válida."
    is IOException -> error.message?.takeIf { it.isNotBlank() } ?: "La TV respondió con un error."
    else -> error.message?.takeIf { it.isNotBlank() } ?: "No se pudo contactar la TV."
}

/** Los elementos activos primero y después el resto; dentro de cada grupo se conserva el orden recibido. */
internal fun <T> activeFirst(items: List<T>, isActive: (T) -> Boolean): List<T> =
    items.filter(isActive) + items.filterNot(isActive)
