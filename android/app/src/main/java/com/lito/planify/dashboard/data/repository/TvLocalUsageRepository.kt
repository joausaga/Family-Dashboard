package com.lito.planify.dashboard.data.repository

import com.google.gson.reflect.TypeToken
import com.lito.planify.dashboard.data.TvPowerState
import com.lito.planify.dashboard.data.TvTransition
import com.lito.planify.dashboard.data.TvUsageCalculator
import com.lito.planify.dashboard.data.TvUsageHistory
import com.lito.planify.dashboard.data.TvStatusProbe
import com.lito.planify.dashboard.data.cache.Cached
import com.lito.planify.dashboard.data.cache.DiskCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import kotlinx.coroutines.CancellationException

/** Conserva el historial importado y añade transiciones desde las observaciones de red local. */
class TvLocalUsageRepository(
    private val api: TvStatusProbe,
    private val cache: DiskCache
) {
    private val type = TypeToken.get(TvUsageHistory::class.java).type
    private val data = MutableStateFlow(cache.read<TvUsageHistory>(CACHE_KEY, type))
    val history: StateFlow<Cached<TvUsageHistory>?> = data

    suspend fun observe(ipAddress: String, at: Instant = Instant.now()): Result<TvUsageHistory> {
        val (state, inferred, error) = try {
            Triple(if (api.isOn(ipAddress)) TvPowerState.ON else TvPowerState.OFF, false, null)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Triple(TvPowerState.OFF, true, failure.message ?: "La TV no respondió al sondeo local.")
        }
        return runCatching {
            val previous = data.value?.value ?: TvUsageHistory()
            val transitions = previous.transitions.toMutableList()
            val last = transitions.maxWithOrNull(compareBy<TvTransition> { it.atMillis }.thenBy { it.order })
            var order = (transitions.maxOfOrNull { it.order } ?: -1L) + 1L
            val source = sourceId(ipAddress)
            val hasThisSource = transitions.any { it.device == source }

            // Cierra la sesión anterior al cambiar a la fuente local, sin atribuir horas desconocidas entre ambas.
            if (!hasThisSource && last?.state == TvPowerState.ON) {
                val cutoff = minOf(at.toEpochMilli(), last.atMillis + TvUsageCalculator.STALE_AFTER_MILLIS)
                transitions += TvTransition(cutoff, TvPowerState.OFF, "local-boundary", order++, inferred = true)
            }
            val previousLocalState = transitions.lastOrNull { it.device == source }?.state
            if (!hasThisSource || previousLocalState != state) {
                transitions += TvTransition(at.toEpochMilli(), state, source, order, inferred = inferred)
            }
            val history = TvUsageCalculator.history(
                transitions = transitions,
                syncAt = at,
                sourceAvailable = true,
                sourceError = error,
                sourceRowCounts = previous.sourceRowCounts,
                sourceOrders = previous.sourceOrders
            ).copy(localMonitoringStartedAtMillis = previous.localMonitoringStartedAtMillis ?: at.toEpochMilli())
            val cached = Cached(history, at.toEpochMilli())
            cache.write(CACHE_KEY, cached, type)
            data.value = cached
            history
        }
    }

    private fun sourceId(ipAddress: String) = "local-tv:$ipAddress"

    private companion object {
        const val CACHE_KEY = "tv_usage_history"
    }
}
