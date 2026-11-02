package com.lito.planify.dashboard.data.repository

import com.lito.planify.dashboard.data.Week
import com.lito.planify.dashboard.data.cache.CachedResource
import com.lito.planify.dashboard.data.cache.DiskCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/**
 * Semanas del calendario (lunes a domingo), cada una con su copia en disco (`week-<lunes>`). Se descargan al
 * navegar hasta ellas y se conservan las [KEEP] vistas más recientemente.
 */
class CalendarRepository(
    private val google: GoogleWorkspaceRepository,
    private val cache: DiskCache,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val resources = mutableMapOf<LocalDate, CachedResource<Week>>()
    private val _weeks = MutableStateFlow<Map<LocalDate, Week>>(emptyMap())

    /** Semanas disponibles (en caché o descargadas), por su lunes. */
    val weeks: StateFlow<Map<LocalDate, Week>> = _weeks

    init {
        cache.keys(PREFIX).mapNotNull { key -> runCatching { LocalDate.parse(key.removePrefix(PREFIX)) }.getOrNull() }
            .forEach { monday -> resource(monday) }
    }

    fun isStale(monday: LocalDate, maxAgeMs: Long = MAX_AGE_MS) = resource(monday).isStale(maxAgeMs)

    /** Semanas ya cargadas: las que vuelve a pedir el botón de sincronizar. */
    fun loaded(): Set<LocalDate> = _weeks.value.keys

    suspend fun refresh(token: String, monday: LocalDate): Result<Week> {
        val result = resource(monday).refresh {
            google.week(token, monday)
        }
        result.onSuccess { week ->
            _weeks.update { it + (monday to week) }
            evict()
        }
        return result
    }

    fun clear() {
        resources.values.forEach { it.clear() }
        resources.clear()
        _weeks.value = emptyMap()
    }

    private fun resource(monday: LocalDate): CachedResource<Week> = resources.getOrPut(monday) {
        CachedResource<Week>(cache, PREFIX + monday, Week::class.java, MAX_AGE_MS, now).also { resource ->
            resource.data.value?.let { cached -> _weeks.update { it + (monday to cached.value) } }
        }
    }

    /** Deja en disco y en memoria solo las [KEEP] semanas descargadas más recientemente. */
    private fun evict() {
        val stale = resources.entries
            .sortedByDescending { it.value.data.value?.fetchedAt ?: 0 }
            .drop(KEEP)
        stale.forEach { (monday, resource) ->
            resource.clear()
            resources.remove(monday)
            _weeks.update { it - monday }
        }
    }

    companion object {
        const val KEEP = 12
        const val MAX_AGE_MS = 5 * 60_000L
        private const val PREFIX = "week-"
    }
}
