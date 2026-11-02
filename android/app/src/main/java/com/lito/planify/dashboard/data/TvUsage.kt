package com.lito.planify.dashboard.data

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class TvPowerState { ON, OFF }

/** Evento de potencia remoto o transición detectada por sondeo local. */
data class TvTransition(
    val atMillis: Long,
    val state: TvPowerState,
    val device: String,
    val order: Long,
    /** true cuando el estado se deduce de la falta de respuesta del API local. */
    val inferred: Boolean = false
)

data class TvInterval(val startMillis: Long, val endMillis: Long?)

data class TvUsageHistory(
    val transitions: List<TvTransition> = emptyList(),
    val intervals: List<TvInterval> = emptyList(),
    /** Metadatos de importaciones antiguas: se conservan al guardar el historial local existente. */
    val sourceRowCounts: Map<String, Int> = emptyMap(),
    /** Orden de las fuentes antiguas; forma parte del historial persistido compatible. */
    val sourceOrders: Map<String, Long> = emptyMap(),
    val inconsistentRows: Int = 0,
    val lastSyncMillis: Long? = null,
    val sourceAvailable: Boolean = false,
    val sourceError: String? = null,
    val localMonitoringStartedAtMillis: Long? = null
)

enum class TvDisplayStatus { ON, OFF, OFF_INFERRED, UNKNOWN, LOADING, ERROR, NOT_CONFIGURED }
data class TvUsageView(
    val status: TvDisplayStatus = TvDisplayStatus.LOADING,
    val todayMillis: Long = 0,
    val currentSessionMillis: Long? = null,
    val dayTotals: Map<LocalDate, Long> = emptyMap(),
    val monthTotals: Map<YearMonth, Long> = emptyMap(),
    val weekMillis: Long = 0,
    val monthMillis: Long = 0,
    /** Duración de hoy agrupada por hora local (0..23), derivada de los intervalos y no persistida aparte. */
    val hourlyMillis: Map<Int, Long> = (0..23).associateWith { 0L },
    val inconsistentRows: Int = 0,
    val lastSyncMillis: Long? = null,
    val sourceError: String? = null,
    val lastObservationInferred: Boolean = false
)

data class TvDailyUsagePoint(val date: LocalDate, val millis: Long)

/** Cinco días completos anteriores al día actual para Inicio; los días sin actividad se muestran en cero. */
fun tvUsageLastFiveCompletedDays(today: LocalDate, dayTotals: Map<LocalDate, Long>): List<TvDailyUsagePoint> =
    (-1L downTo -5L).map { offset ->
        val date = today.plusDays(offset)
        TvDailyUsagePoint(date, (dayTotals[date] ?: 0L).coerceAtLeast(0L))
    }

/** Promedio diario de los cinco días completos previos; los días sin uso cuentan como cero. */
fun tvUsageAverageLastFiveCompletedDays(today: LocalDate, dayTotals: Map<LocalDate, Long>): Long =
    tvUsageLastFiveCompletedDays(today, dayTotals).sumOf { it.millis } / 5L

/** Cantidad de días, entre los cinco completos previos, con algún uso registrado. */
fun tvUsageDaysWithUsageLastFiveCompletedDays(today: LocalDate, dayTotals: Map<LocalDate, Long>): Int =
    tvUsageLastFiveCompletedDays(today, dayTotals).count { it.millis > 0L }

/** Uso acumulado de un periodo frente al mismo tramo del periodo anterior. */
data class TvPeriodComparison(val current: Long, val previous: Long) {
    val delta: Long get() = current - previous
}

/** Semana actual (lunes a hoy) frente al mismo tramo de la semana anterior. */
fun tvUsageWeekComparison(today: LocalDate, dayTotals: Map<LocalDate, Long>): TvPeriodComparison {
    val monday = today.with(DayOfWeek.MONDAY)
    val elapsed = ChronoUnit.DAYS.between(monday, today)
    fun sum(start: LocalDate) = (0L..elapsed).sumOf { (dayTotals[start.plusDays(it)] ?: 0L).coerceAtLeast(0L) }
    return TvPeriodComparison(sum(monday), sum(monday.minusWeeks(1)))
}

/**
 * Mes actual (día 1 a hoy) frente al mismo tramo del mes anterior; si el mes anterior es más corto, se compara
 * contra todo ese mes.
 */
fun tvUsageMonthComparison(today: LocalDate, dayTotals: Map<LocalDate, Long>): TvPeriodComparison {
    val start = today.withDayOfMonth(1)
    val previousStart = start.minusMonths(1)
    fun sum(from: LocalDate, days: Int) = (0 until days).sumOf { (dayTotals[from.plusDays(it.toLong())] ?: 0L).coerceAtLeast(0L) }
    return TvPeriodComparison(
        sum(start, today.dayOfMonth),
        sum(previousStart, minOf(today.dayOfMonth, previousStart.lengthOfMonth()))
    )
}

/** Día de mayor uso dentro del rango (inclusive); ante un empate, el más reciente. Null si no hay uso. */
fun tvUsagePeak(dayTotals: Map<LocalDate, Long>, from: LocalDate? = null, to: LocalDate? = null): TvDailyUsagePoint? =
    dayTotals.entries
        .filter { it.value > 0L && (from == null || !it.key.isBefore(from)) && (to == null || !it.key.isAfter(to)) }
        .maxWithOrNull(compareBy({ it.value }, { it.key }))
        ?.let { TvDailyUsagePoint(it.key, it.value) }

/** Hora (0–23) de mayor uso de hoy; ante un empate, la más temprana. Null si no hay uso. */
fun tvBusiestHour(hourlyMillis: Map<Int, Long>): Pair<Int, Long>? =
    (0..23).map { it to (hourlyMillis[it] ?: 0L) }.filter { it.second > 0L }.maxByOrNull { it.second }

data class TvHeatCell(val date: LocalDate, val millis: Long, val future: Boolean, val today: Boolean)

/** Semanas (de la más antigua a la actual) de lunes a domingo; los días posteriores a hoy se marcan como futuros. */
fun tvHeatmapWeeks(today: LocalDate, weeks: Int, dayTotals: Map<LocalDate, Long>): List<List<TvHeatCell>> {
    val firstMonday = today.with(DayOfWeek.MONDAY).minusWeeks((weeks - 1).coerceAtLeast(0).toLong())
    return (0 until weeks.coerceAtLeast(1)).map { week ->
        (0L..6L).map { day ->
            val date = firstMonday.plusWeeks(week.toLong()).plusDays(day)
            TvHeatCell(date, (dayTotals[date] ?: 0L).coerceAtLeast(0L), date.isAfter(today), date == today)
        }
    }
}

/** Nivel de intensidad 0–4 respecto al máximo del rango: 0 sin uso; el resto, por cuartos del máximo. */
fun tvHeatLevel(millis: Long, peak: Long): Int {
    if (millis <= 0L || peak <= 0L) return 0
    val ratio = millis.toDouble() / peak
    return when {
        ratio <= 0.25 -> 1
        ratio <= 0.5 -> 2
        ratio <= 0.75 -> 3
        else -> 4
    }
}

/** Reconstruye sesiones y reparte su duración por fronteras de calendario local sin asumir días de 24 horas. */
object TvUsageCalculator {
    const val STALE_AFTER_MILLIS = 6 * 60 * 60 * 1000L

    fun history(
        transitions: List<TvTransition>,
        syncAt: Instant?,
        sourceAvailable: Boolean,
        sourceError: String? = null,
        sourceRowCounts: Map<String, Int> = emptyMap(),
        sourceOrders: Map<String, Long> = emptyMap()
    ): TvUsageHistory {
        val sorted = transitions.distinctBy { Triple(it.atMillis, it.state, it.device) }
            .sortedWith(compareBy<TvTransition> { it.atMillis }.thenBy { it.order })
        val intervals = mutableListOf<TvInterval>()
        var open: Long? = null
        var inconsistencies = 0
        sorted.forEach { transition ->
            when (transition.state) {
                TvPowerState.ON -> {
                    if (open == null) open = transition.atMillis else inconsistencies++
                }
                TvPowerState.OFF -> {
                    val start = open
                    if (start != null && transition.atMillis >= start) {
                        if (transition.atMillis > start) intervals += TvInterval(start, transition.atMillis)
                        open = null
                    } else if (!transition.inferred) inconsistencies++
                }
            }
        }
        open?.let { intervals += TvInterval(it, null) }
        return TvUsageHistory(sorted, intervals, sourceRowCounts, sourceOrders, inconsistencies, syncAt?.toEpochMilli(), sourceAvailable, sourceError)
    }

    fun summarize(history: TvUsageHistory, now: Instant, zone: ZoneId): TvUsageView {
        val valid = history.transitions.lastOrNull()
        val observedAt = if (history.localMonitoringStartedAtMillis != null) history.lastSyncMillis else valid?.atMillis
        val fresh = observedAt != null && now.toEpochMilli() >= observedAt &&
            now.toEpochMilli() - observedAt <= STALE_AFTER_MILLIS
        val currentlyOn = valid != null && fresh && valid.state == TvPowerState.ON
        val openEnd = when {
            currentlyOn -> now.toEpochMilli()
            valid?.state == TvPowerState.ON -> minOf(now.toEpochMilli(), valid.atMillis + STALE_AFTER_MILLIS)
            else -> null
        }
        val dayTotals = mutableMapOf<LocalDate, Long>()
        val monthTotals = mutableMapOf<YearMonth, Long>()
        history.intervals.forEach { interval ->
            val end = interval.endMillis ?: openEnd ?: interval.startMillis
            addAcrossDays(interval.startMillis, end, zone, dayTotals)
            addAcrossMonths(interval.startMillis, end, zone, monthTotals)
        }
        val today = now.atZone(zone).toLocalDate()
        val hourlyMillis = (0..23).associateWith { 0L }.toMutableMap()
        val todayStart = today.atStartOfDay(zone).toInstant()
        val todayEnd = minOf(today.plusDays(1).atStartOfDay(zone).toInstant(), now)
        history.intervals.forEach { interval ->
            val intervalEnd = interval.endMillis ?: openEnd ?: interval.startMillis
            var cursor = maxOf(interval.startMillis, todayStart.toEpochMilli())
            val end = minOf(intervalEnd, todayEnd.toEpochMilli())
            while (cursor < end) {
                val local = Instant.ofEpochMilli(cursor).atZone(zone)
                val nextHour = local.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant().toEpochMilli()
                val segmentEnd = minOf(end, nextHour)
                if (segmentEnd <= cursor) break
                hourlyMillis[local.hour] = (hourlyMillis[local.hour] ?: 0L) + segmentEnd - cursor
                cursor = segmentEnd
            }
        }
        val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weekMillis = (0L..6L).sumOf { dayTotals[weekStart.plusDays(it)] ?: 0L }
        val month = YearMonth.from(today)
        return TvUsageView(
            status = when {
                !history.sourceAvailable && history.sourceError != null -> TvDisplayStatus.ERROR
                valid == null || !fresh -> TvDisplayStatus.UNKNOWN
                valid.state == TvPowerState.ON -> TvDisplayStatus.ON
                valid.inferred -> TvDisplayStatus.OFF_INFERRED
                else -> TvDisplayStatus.OFF
            },
            todayMillis = dayTotals[today] ?: 0L,
            currentSessionMillis = if (currentlyOn) (now.toEpochMilli() - (history.intervals.lastOrNull { it.endMillis == null }?.startMillis ?: now.toEpochMilli())).coerceAtLeast(0) else null,
            dayTotals = dayTotals.toSortedMap(compareByDescending { it }),
            monthTotals = monthTotals.toSortedMap(compareByDescending { it }),
            weekMillis = weekMillis,
            monthMillis = monthTotals[month] ?: 0L,
            hourlyMillis = hourlyMillis,
            inconsistentRows = history.inconsistentRows,
            lastSyncMillis = history.lastSyncMillis,
            sourceError = history.sourceError,
            lastObservationInferred = valid?.inferred == true
        )
    }

    private fun addAcrossDays(start: Long, end: Long, zone: ZoneId, out: MutableMap<LocalDate, Long>) {
        var cursor = Instant.ofEpochMilli(start)
        val until = Instant.ofEpochMilli(end.coerceAtLeast(start))
        while (cursor < until) {
            val local = cursor.atZone(zone)
            val boundary = local.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()
            val segmentEnd = minOf(until, boundary)
            if (segmentEnd <= cursor) break
            out[local.toLocalDate()] = (out[local.toLocalDate()] ?: 0L) + Duration.between(cursor, segmentEnd).toMillis()
            cursor = segmentEnd
        }
    }

    private fun addAcrossMonths(start: Long, end: Long, zone: ZoneId, out: MutableMap<YearMonth, Long>) {
        var cursor = Instant.ofEpochMilli(start)
        val until = Instant.ofEpochMilli(end.coerceAtLeast(start))
        while (cursor < until) {
            val local = cursor.atZone(zone)
            val month = YearMonth.from(local)
            val boundary = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
            val segmentEnd = minOf(until, boundary)
            if (segmentEnd <= cursor) break
            out[month] = (out[month] ?: 0L) + Duration.between(cursor, segmentEnd).toMillis()
            cursor = segmentEnd
        }
    }
}
