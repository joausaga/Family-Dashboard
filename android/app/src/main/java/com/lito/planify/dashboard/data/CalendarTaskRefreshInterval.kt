package com.lito.planify.dashboard.data

/** Opciones de actualización automática para eventos, rutinas y tareas de Google. */
object CalendarTaskRefreshInterval {
    const val DEFAULT_MINUTES = 5
    val options = listOf(5, 10, 20, 30, 40, 50, 60)

    fun isValid(minutes: Int): Boolean = minutes in options
    fun maxAgeMillis(minutes: Int): Long = minutes * 60_000L
}
