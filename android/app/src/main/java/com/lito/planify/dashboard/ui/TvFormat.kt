package com.lito.planify.dashboard.ui

import com.lito.planify.dashboard.data.TvPeriodComparison

/** Duración en partes (valor, unidad), p. ej. [("1","h"), ("50","min")], para componer cifras con unidades pequeñas. */
internal fun tvDurationParts(milliseconds: Long): List<Pair<String, String>> {
    val minutes = milliseconds.coerceAtLeast(0) / 60_000
    val hours = minutes / 60
    val remainder = minutes % 60
    return buildList {
        if (hours > 0) add(hours.toString() to "h")
        if (remainder > 0 || hours == 0L) add(remainder.toString() to "min")
    }
}

/** Antigüedad legible: «hace unos segundos», «hace 5 min», «hace 2 h 10 min», «hace 3 d». */
internal fun tvAgoLabel(milliseconds: Long): String {
    val minutes = milliseconds.coerceAtLeast(0) / 60_000
    return when {
        minutes < 1 -> "hace unos segundos"
        minutes < 60 -> "hace $minutes min"
        minutes < 24 * 60 -> {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0L) "hace $h h" else "hace $h h $m min"
        }
        else -> "hace ${minutes / (24 * 60)} d"
    }
}

/** Frase para el uso de hoy frente al promedio de los cinco días completos previos. */
internal fun tvVsAverageLabel(todayMillis: Long, averageMillis: Long): String {
    if (averageMillis <= 0L) return "Sin historial para comparar"
    val diff = todayMillis - averageMillis
    return when {
        kotlin.math.abs(diff) < 60_000L -> "Igual que el promedio"
        diff > 0 -> "${formatTvDuration(diff)} sobre el promedio"
        else -> "${formatTvDuration(-diff)} bajo el promedio"
    }
}

/** Dirección (+1 sube, -1 baja, 0 igual o sin referencia) y texto de la comparación con el periodo anterior. */
internal fun tvComparisonText(comparison: TvPeriodComparison, previousLabel: String): Pair<Int, String> = when {
    comparison.previous <= 0L && comparison.current <= 0L -> 0 to "Sin uso en ambos periodos"
    comparison.previous <= 0L -> 0 to "Sin datos del periodo anterior"
    kotlin.math.abs(comparison.delta) < 60_000L -> 0 to "Igual que $previousLabel"
    comparison.delta > 0 -> 1 to "${formatTvDuration(comparison.delta)} más que $previousLabel"
    else -> -1 to "${formatTvDuration(-comparison.delta)} menos que $previousLabel"
}
