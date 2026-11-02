package com.lito.planify.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lito.planify.dashboard.data.TvDisplayStatus
import com.lito.planify.dashboard.data.TvHeatCell
import com.lito.planify.dashboard.data.TvUsageView
import com.lito.planify.dashboard.data.tvBusiestHour
import com.lito.planify.dashboard.data.tvHeatLevel
import com.lito.planify.dashboard.data.tvHeatmapWeeks
import com.lito.planify.dashboard.data.tvUsageAverageLastFiveCompletedDays
import com.lito.planify.dashboard.data.tvUsageMonthComparison
import com.lito.planify.dashboard.data.tvUsagePeak
import com.lito.planify.dashboard.data.tvUsageWeekComparison
import com.lito.planify.dashboard.ui.theme.Palette
import com.lito.planify.dashboard.ui.theme.Radius
import com.lito.planify.dashboard.ui.theme.Type
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.YearMonth
import java.time.format.TextStyle

private const val HOUR_MILLIS = 3_600_000L
private val WEEK_RANGES = listOf(4, 8, 26)
private const val DEFAULT_WEEKS = 8

@Composable
fun TvUsageScreen(usage: TvUsageView, now: ZonedDateTime, onGoToSettings: () -> Unit) {
    val today = now.toLocalDate()
    var weeks by remember { mutableIntStateOf(DEFAULT_WEEKS) }
    ScreenPadding {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            TvHero(usage, now, onGoToSettings, Modifier.weight(0.36f).fillMaxHeight())
            Column(Modifier.weight(0.64f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                HourStrip(usage.hourlyMillis, now.hour, Modifier.fillMaxWidth().weight(0.26f))
                Row(Modifier.fillMaxWidth().weight(0.74f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    HeatmapCard(usage, today, weeks, { weeks = it }, Modifier.weight(0.6f).fillMaxHeight())
                    Column(Modifier.weight(0.4f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        PeriodsCard(usage, today, Modifier.weight(0.56f).fillMaxWidth())
                        MonthsCard(usage, today, Modifier.weight(0.44f).fillMaxWidth())
                    }
                }
            }
        }
    }
}

private fun tvText(size: Int, weight: FontWeight = FontWeight.Medium, color: Color = Palette.Ink) =
    Type.Tiny.copy(fontSize = size.sp, lineHeight = (size * 1.2f).sp, fontWeight = weight, color = color, fontFeatureSettings = "tnum")

private fun Modifier.tap(onClick: () -> Unit): Modifier =
    clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClick)

private fun LocalDate.tvDayLabel(): String =
    "${dayOfWeek.getDisplayName(TextStyle.SHORT, Spanish).trimEnd('.')} $dayOfMonth ${month.getDisplayName(TextStyle.SHORT, Spanish).trimEnd('.')}"

private fun YearMonth.tvMonthLabel(): String =
    "${month.getDisplayName(TextStyle.SHORT, Spanish).trimEnd('.')} ${year % 100}"

private fun tvStatusLabel(status: TvDisplayStatus) = when (status) {
    TvDisplayStatus.ON -> "Encendida"
    TvDisplayStatus.OFF, TvDisplayStatus.OFF_INFERRED -> "Apagada"
    TvDisplayStatus.NOT_CONFIGURED -> "Sin configurar"
    TvDisplayStatus.LOADING -> "Consultando…"
    TvDisplayStatus.UNKNOWN, TvDisplayStatus.ERROR -> "Sin confirmación"
}

@Composable
private fun TvCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(Radius.Card)
    Column(modifier.background(Palette.Card, shape).border(1.dp, Palette.Border, shape).padding(24.dp), content = content)
}

@Composable
private fun TvCardTitle(title: String, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = tvText(28, FontWeight.SemiBold), maxLines = 1)
        trailing()
    }
}

@Composable
private fun TvHero(usage: TvUsageView, now: ZonedDateTime, onGoToSettings: () -> Unit, modifier: Modifier) {
    val on = usage.status == TvDisplayStatus.ON
    val gradient = if (on) listOf(Palette.TelevisionDeep, Palette.TelevisionBright) else listOf(Palette.TelevisionDim, Palette.TelevisionDimEnd)
    val today = now.toLocalDate()
    val average = remember(today, usage.dayTotals) { tvUsageAverageLastFiveCompletedDays(today, usage.dayTotals) }
    val nowMillis = now.toInstant().toEpochMilli()
    val white = Color.White
    Column(
        modifier.clip(RoundedCornerShape(Radius.Card)).background(Brush.verticalGradient(gradient)).padding(32.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(Radius.Pill)).background(white.copy(alpha = 0.18f))
                .heightIn(min = 52.dp).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(14.dp).background(if (on) Palette.TelevisionOn else white.copy(alpha = 0.7f), CircleShape))
            Text(tvStatusLabel(usage.status), style = tvText(28, FontWeight.SemiBold, white), maxLines = 1)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("HOY", style = tvText(24, FontWeight.SemiBold, white.copy(alpha = 0.9f)).copy(letterSpacing = 3.sp))
            val parts = tvDurationParts(usage.todayMillis)
            Text(
                buildAnnotatedString {
                    parts.forEachIndexed { i, (value, unit) ->
                        if (i > 0) append("  ")
                        withStyle(SpanStyle(fontSize = 116.sp, fontWeight = FontWeight.Bold)) { append(value) }
                        withStyle(SpanStyle(fontSize = 44.sp, fontWeight = FontWeight.Medium)) { append(" $unit") }
                    }
                },
                style = tvText(116, FontWeight.Bold, white).copy(lineHeight = 128.sp, letterSpacing = (-2).sp),
                maxLines = 1,
                softWrap = false
            )
            Text(
                "${today.weekdayName()} ${today.dayOfMonth} de ${today.month.getDisplayName(TextStyle.FULL, Spanish)}",
                style = tvText(26, FontWeight.Medium, white.copy(alpha = 0.92f)),
                maxLines = 1
            )
            Spacer(Modifier.height(14.dp))
            if (on) {
                usage.currentSessionMillis?.let {
                    Text("Encendida ${tvAgoLabel(it)}", style = tvText(28, FontWeight.SemiBold, white), maxLines = 1)
                }
            }
            Text(tvVsAverageLabel(usage.todayMillis, average), style = tvText(26, FontWeight.Medium, white.copy(alpha = 0.95f)), maxLines = 2)
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val notice = tvNotice(usage)
            if (notice.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.Chip)).background(white.copy(alpha = 0.16f)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    notice.forEachIndexed { i, line ->
                        Text(
                            line,
                            style = if (i == 0) tvText(24, FontWeight.SemiBold, white) else tvText(20, FontWeight.Normal, white.copy(alpha = 0.9f)),
                            maxLines = if (i == 0) 3 else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (usage.status == TvDisplayStatus.NOT_CONFIGURED) {
                PillButton("Ir a Ajustes", Modifier.fillMaxWidth(), primary = true, onClick = onGoToSettings)
            }
            Text(
                usage.lastSyncMillis?.let { "Última consulta ${tvAgoLabel(nowMillis - it)}" } ?: "Sin consultas todavía",
                style = tvText(22, FontWeight.Medium, white.copy(alpha = 0.85f)),
                maxLines = 1
            )
        }
    }
}

/** Mensajes del héroe: el primero es el principal y los siguientes, detalle pequeño. */
private fun tvNotice(usage: TvUsageView): List<String> = buildList {
    when {
        usage.status == TvDisplayStatus.NOT_CONFIGURED -> add("Configura la IP de la TV en Ajustes para iniciar el seguimiento.")
        usage.sourceError != null -> {
            val off = usage.status == TvDisplayStatus.OFF || usage.status == TvDisplayStatus.OFF_INFERRED
            add(if (off) "No se pudo contactar la TV. Se muestra como apagada." else "No se pudo contactar la TV.")
            add(usage.sourceError)
        }
        usage.status == TvDisplayStatus.OFF_INFERRED -> add("La TV no respondió; apagado inferido.")
        usage.status == TvDisplayStatus.UNKNOWN || usage.status == TvDisplayStatus.ERROR ->
            add("Sin una consulta reciente: el estado puede no estar actualizado.")
    }
    if (usage.inconsistentRows > 0) {
        add("Hay ${usage.inconsistentRows} registros con datos inconsistentes; se conservaron sin contar su duración.")
    }
}

@Composable
private fun HourStrip(hourly: Map<Int, Long>, currentHour: Int, modifier: Modifier) {
    var selected by remember { mutableStateOf<Int?>(null) }
    val busiest = tvBusiestHour(hourly)
    val detail = when {
        selected != null -> {
            val h = selected!!
            val ms = hourly[h] ?: 0L
            "%02d:00–%02d:00 · %s".format(h, (h + 1) % 24, if (ms > 0) formatTvDuration(ms) else "sin uso")
        }
        busiest != null -> "Mayor uso: %02d:00 · %s".format(busiest.first, formatTvDuration(busiest.second))
        else -> "Sin uso hoy todavía"
    }
    TvCard(modifier) {
        TvCardTitle("Hoy por hora") { Text(detail, style = tvText(24, FontWeight.SemiBold, Palette.InkSoft), maxLines = 1) }
        Row(Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (0..23).forEach { h ->
                val ms = hourly[h] ?: 0L
                val shape = RoundedCornerShape(8.dp)
                val base = Palette.TvHeat[tvHeatLevel(ms, HOUR_MILLIS)]
                Column(Modifier.weight(1f).fillMaxHeight().tap { selected = if (selected == h) null else h }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.weight(1f).fillMaxWidth().clip(shape)
                            .background(if (h > currentHour) base.copy(alpha = 0.5f) else base)
                            .then(
                                when {
                                    selected == h -> Modifier.border(4.dp, Palette.Ink, shape)
                                    h == currentHour -> Modifier.border(3.dp, Palette.TelevisionDeep, shape)
                                    else -> Modifier
                                }
                            )
                    )
                    Text(
                        if (h % 6 == 0) "%02d".format(h) else "",
                        modifier = Modifier.fillMaxWidth().height(28.dp).padding(top = 4.dp),
                        style = tvText(20, FontWeight.Medium, Palette.InkSoft),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapCard(usage: TvUsageView, today: LocalDate, weeks: Int, onWeeks: (Int) -> Unit, modifier: Modifier) {
    var selected by remember(weeks) { mutableStateOf<LocalDate?>(null) }
    val grid = remember(today, weeks, usage.dayTotals) { tvHeatmapWeeks(today, weeks, usage.dayTotals) }
    val peak = remember(today, weeks, usage.dayTotals) { tvUsagePeak(usage.dayTotals, grid.first().first().date, today) }
    val peakMillis = peak?.millis ?: 0L
    val cells = remember(grid) { grid.flatten() }
    val detail = selected?.let { d ->
        val ms = cells.firstOrNull { it.date == d }?.millis ?: 0L
        "${d.tvDayLabel()} · ${if (ms > 0) formatTvDuration(ms) else "sin uso"}"
    } ?: peak?.let { "Pico: ${it.date.tvDayLabel()} · ${formatTvDuration(it.millis)}" } ?: "Aún no hay tiempo registrado"

    TvCard(modifier) {
        TvCardTitle("Historial") { RangeSelector(weeks, onWeeks) }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
            val labelWidth = 32.dp
            val gap = 6.dp
            val monthHeight = 30.dp
            val cellWidth: Dp = ((maxWidth - labelWidth - gap * (weeks - 1) - 14.dp) / weeks).coerceIn(44.dp, 76.dp)
            val cellHeight: Dp = ((maxHeight - monthHeight - gap * 6) / 7).coerceIn(44.dp, 92.dp)
            val scroll = rememberScrollState()
            LaunchedEffect(weeks, scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
            Row(Modifier.fillMaxHeight()) {
                Column(Modifier.width(labelWidth).padding(top = monthHeight), verticalArrangement = Arrangement.spacedBy(gap)) {
                    listOf("L", "M", "X", "J", "V", "S", "D").forEach {
                        Box(Modifier.height(cellHeight).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            Text(it, style = tvText(20, FontWeight.Medium, Palette.InkSoft))
                        }
                    }
                }
                Row(Modifier.horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    grid.forEachIndexed { index, week ->
                        val monday = week.first().date
                        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            Box(Modifier.height(monthHeight).width(cellWidth), contentAlignment = Alignment.CenterStart) {
                                if (index == 0 || monday.dayOfMonth <= 7) {
                                    Text(
                                        monday.month.getDisplayName(TextStyle.SHORT, Spanish).trimEnd('.'),
                                        style = tvText(20, FontWeight.SemiBold, Palette.InkSoft),
                                        maxLines = 1, softWrap = false
                                    )
                                }
                            }
                            week.forEach { c ->
                                HeatCell(c, cellWidth, cellHeight, peakMillis, isPeak = peak?.date == c.date, isSelected = selected == c.date) {
                                    selected = if (selected == c.date) null else c.date
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(detail, modifier = Modifier.weight(1f), style = tvText(24, FontWeight.SemiBold, Palette.Ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Menos", style = tvText(20, FontWeight.Medium, Palette.InkSoft), maxLines = 1)
            Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Palette.TvHeat.forEach { Box(Modifier.size(20.dp).background(it, RoundedCornerShape(5.dp))) }
            }
            Text("Más", style = tvText(20, FontWeight.Medium, Palette.InkSoft), maxLines = 1)
        }
    }
}

@Composable
private fun HeatCell(cell: TvHeatCell, width: Dp, height: Dp, peakMillis: Long, isPeak: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val level = tvHeatLevel(cell.millis, peakMillis)
    val shape = RoundedCornerShape(12.dp)
    val fill = if (cell.future) Palette.TvHeat[0].copy(alpha = 0.4f) else Palette.TvHeat[level]
    Box(
        Modifier.size(width, height).clip(shape).background(fill)
            .then(if (isSelected) Modifier.border(4.dp, Palette.Ink, shape) else Modifier)
            .then(if (cell.future) Modifier else Modifier.tap(onClick)),
        contentAlignment = Alignment.Center
    ) {
        if (isPeak) Icon(Icons.Filled.Star, contentDescription = "Pico", tint = Color.White, modifier = Modifier.size(26.dp))
        if (cell.today) {
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).size(10.dp)
                    .background(if (level >= 3) Color.White else Palette.Ink, CircleShape)
            )
        }
    }
}

@Composable
private fun RangeSelector(weeks: Int, onWeeks: (Int) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(Radius.Pill)).background(Palette.TelevisionSoft).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        WEEK_RANGES.forEach { w ->
            val active = w == weeks
            Box(
                Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(Radius.Pill))
                    .background(if (active) Palette.Television else Color.Transparent)
                    .tap { onWeeks(w) }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("$w sem", style = tvText(22, FontWeight.SemiBold, if (active) Color.White else Palette.Television), maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun PeriodsCard(usage: TvUsageView, today: LocalDate, modifier: Modifier) {
    val week = remember(today, usage.dayTotals) { tvUsageWeekComparison(today, usage.dayTotals) }
    val month = remember(today, usage.dayTotals) { tvUsageMonthComparison(today, usage.dayTotals) }
    TvCard(modifier) {
        PeriodStat("ESTA SEMANA", usage.weekMillis, tvComparisonText(week, "la semana pasada"), Modifier.weight(1f).fillMaxWidth())
        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(Palette.Divider))
        PeriodStat("ESTE MES", usage.monthMillis, tvComparisonText(month, "el mes pasado"), Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun PeriodStat(label: String, millis: Long, comparison: Pair<Int, String>, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center) {
        Text(label, style = tvText(22, FontWeight.SemiBold, Palette.InkSoft).copy(letterSpacing = 1.5.sp), maxLines = 1)
        Text(
            buildAnnotatedString {
                tvDurationParts(millis).forEachIndexed { i, (value, unit) ->
                    if (i > 0) append("  ")
                    withStyle(SpanStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold)) { append(value) }
                    withStyle(SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Medium)) { append(" $unit") }
                }
            },
            style = tvText(56, FontWeight.Bold, Palette.Television).copy(lineHeight = 64.sp, letterSpacing = (-1).sp),
            maxLines = 1,
            softWrap = false
        )
        val (direction, text) = comparison
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (direction != 0) {
                Icon(
                    if (direction > 0) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = null, tint = Palette.Television, modifier = Modifier.size(24.dp)
                )
            }
            Text(text, style = tvText(22, FontWeight.Medium, if (direction != 0) Palette.Television else Palette.InkSoft), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MonthsCard(usage: TvUsageView, today: LocalDate, modifier: Modifier) {
    val current = YearMonth.from(today)
    val months = usage.monthTotals.entries.toList()
    val maxMillis = months.maxOfOrNull { it.value } ?: 0L
    TvCard(modifier) {
        TvCardTitle("Por mes")
        if (months.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Aún no hay meses con tiempo registrado", style = tvText(22, FontWeight.Medium, Palette.InkSoft), textAlign = TextAlign.Center)
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                months.forEach { (month, millis) ->
                    val isCurrent = month == current
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(month.tvMonthLabel(), modifier = Modifier.weight(1f), style = tvText(22, FontWeight.Medium, Palette.InkSoft), maxLines = 1, softWrap = false)
                            Text(
                                formatTvDuration(millis),
                                style = tvText(24, if (isCurrent) FontWeight.Bold else FontWeight.SemiBold, if (isCurrent) Palette.Television else Palette.Ink),
                                maxLines = 1, softWrap = false
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)).background(Palette.TelevisionSoft)) {
                            Box(
                                Modifier.fillMaxWidth(if (maxMillis > 0) (millis.toFloat() / maxMillis).coerceIn(0.03f, 1f) else 0.03f).fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) Palette.Television else Palette.TvHeat[3])
                            )
                        }
                    }
                }
            }
        }
    }
}
