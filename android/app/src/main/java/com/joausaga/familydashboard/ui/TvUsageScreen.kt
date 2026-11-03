package com.joausaga.familydashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joausaga.familydashboard.data.TvDisplayStatus
import com.joausaga.familydashboard.data.TvHeatCell
import com.joausaga.familydashboard.data.TvUsageView
import com.joausaga.familydashboard.data.tvBusiestHour
import com.joausaga.familydashboard.data.tvHeatLevel
import com.joausaga.familydashboard.data.tvHeatmapWeeks
import com.joausaga.familydashboard.data.tvUsageAverageLastFiveCompletedDays
import com.joausaga.familydashboard.data.tvUsageMonthComparison
import com.joausaga.familydashboard.data.tvUsagePeak
import com.joausaga.familydashboard.data.tvUsageWeekComparison
import com.joausaga.familydashboard.ui.theme.Palette
import com.joausaga.familydashboard.ui.theme.Radius
import com.joausaga.familydashboard.ui.theme.Type
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.YearMonth
import java.time.format.TextStyle

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
                HourStrip(usage.hourlyMillis, now.hour, Modifier.fillMaxWidth().weight(0.29f))
                HeatmapCard(usage, today, weeks, { weeks = it }, Modifier.fillMaxWidth().weight(0.545f))
                Row(Modifier.fillMaxWidth().weight(0.165f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    PeriodsCard(usage, today, Modifier.weight(0.56f).fillMaxHeight())
                    MonthsCard(usage, today, Modifier.weight(0.44f).fillMaxHeight())
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
private fun TvCard(modifier: Modifier, contentPadding: Dp = 24.dp, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(Radius.Card)
    Column(modifier.background(Palette.Card, shape).border(1.dp, Palette.Border, shape).padding(contentPadding), content = content)
}

@Composable
private fun TvCardTitle(title: String, minHeight: Dp = 48.dp, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min = minHeight), verticalAlignment = Alignment.CenterVertically) {
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
    val hourlyValues = remember(hourly) { (0..23).map { hourly[it]?.coerceAtLeast(0L) ?: 0L } }
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
        Box(Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)) {
            HourlyUsageBars(hourlyValues, currentHour, selected, Modifier.fillMaxSize())
            Row(Modifier.fillMaxSize()) {
                (0..23).forEach { hour ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .semantics { contentDescription = hourlyAccessibleLabel(hour, hourlyValues[hour]) }
                            .tap { selected = if (selected == hour) null else hour }
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, 6, 12, 18).forEach { hour ->
                Text("%02d".format(hour), style = tvText(20, FontWeight.Medium, Palette.InkSoft), maxLines = 1)
            }
            Text("24", style = tvText(20, FontWeight.Medium, Palette.InkSoft), maxLines = 1)
        }
    }
}

private fun hourlyAccessibleLabel(hour: Int, millis: Long): String =
    "%02d:00 a %02d:00, %s".format(hour, (hour + 1) % 24, if (millis > 0L) formatTvDuration(millis) else "sin uso")

@Composable
private fun HourlyUsageBars(values: List<Long>, currentHour: Int, selectedHour: Int?, modifier: Modifier) {
    val maxValue = (values.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
    val labelHeight = 30.dp
    BoxWithConstraints(modifier) {
        val chartHeight = maxHeight
        val plotHeight = (chartHeight - labelHeight).coerceAtLeast(1.dp)
        val currentX = maxWidth * ((currentHour.coerceIn(0, values.lastIndex) + 0.5f) / values.size)
        Canvas(Modifier.fillMaxSize()) {
            drawLine(
                Palette.Divider,
                Offset(0f, size.height - 1.dp.toPx()),
                Offset(size.width, size.height - 1.dp.toPx()),
                1.dp.toPx()
            )
            drawLine(
                Palette.Television.copy(alpha = 0.28f),
                Offset(currentX.toPx(), labelHeight.toPx()),
                Offset(currentX.toPx(), size.height),
                2.dp.toPx()
            )
        }
        Row(Modifier.fillMaxSize()) {
            values.forEachIndexed { hour, millis ->
                val fraction = (millis.coerceAtLeast(0L) / maxValue).coerceIn(0f, 1f)
                val barHeight = plotHeight * fraction
                val labelOffset = (chartHeight - barHeight - labelHeight).coerceAtLeast(0.dp)
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                        .then(if (selectedHour == hour) Modifier.background(Palette.Television.copy(alpha = 0.08f)) else Modifier),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        Box(
                            Modifier.align(Alignment.BottomCenter)
                                .fillMaxWidth(0.62f)
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                .background(if (hour > currentHour) Palette.Television.copy(alpha = 0.45f) else Palette.Television)
                        )
                        compactHourDuration(millis).takeIf { it.isNotEmpty() }?.let { label ->
                            Text(
                                label,
                                modifier = Modifier.align(Alignment.TopCenter).offset(y = labelOffset).fillMaxWidth(),
                                style = tvText(18, FontWeight.SemiBold, if (hour > currentHour) Palette.InkSoft else Palette.Television),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun compactHourDuration(milliseconds: Long): String =
    if (milliseconds <= 0L) "" else compactHeatDuration(milliseconds)

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

    TvCard(modifier, contentPadding = 14.dp) {
        TvCardTitle("Historial", minHeight = 36.dp) { RangeSelector(weeks, onWeeks) }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp)) {
            val labelWidth = 24.dp
            val gap = 3.dp
            val monthHeight = 28.dp
            val cellWidth: Dp = ((maxWidth - labelWidth - gap * (weeks - 1) - 14.dp) / weeks).coerceAtLeast(1.dp)
            val cellHeight: Dp = ((maxHeight - monthHeight - gap * 6) / 7).coerceAtLeast(1.dp)
            val scroll = rememberScrollState()
            LaunchedEffect(weeks, scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
            Row(Modifier.fillMaxHeight()) {
                Column(Modifier.width(labelWidth).padding(top = monthHeight), verticalArrangement = Arrangement.spacedBy(gap)) {
                    listOf("L", "M", "X", "J", "V", "S", "D").forEach {
                        Box(Modifier.height(cellHeight).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            Text(it, style = tvText(if (cellHeight < 25.dp) 12 else 20, FontWeight.Medium, Palette.InkSoft))
                        }
                    }
                }
                Row(Modifier.horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    grid.forEachIndexed { index, week ->
                        val monday = week.first().date
                        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            Box(Modifier.height(monthHeight).width(cellWidth), contentAlignment = Alignment.CenterStart) {
                                val monthStart = week.firstOrNull { it.date.dayOfMonth == 1 }
                                if (index == 0 || monthStart != null) {
                                    val labelDate = monthStart?.date ?: monday
                                    Text(
                                        labelDate.month.getDisplayName(TextStyle.SHORT, Spanish).trimEnd('.'),
                                        modifier = Modifier.width(cellWidth),
                                        style = tvText(if (cellHeight < 25.dp) 10 else 15, FontWeight.SemiBold, Palette.InkSoft),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
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
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(detail, modifier = Modifier.weight(1f), style = tvText(19, FontWeight.SemiBold, Palette.Ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Menos", style = tvText(16, FontWeight.Medium, Palette.InkSoft), maxLines = 1)
            Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Palette.TvHeat.forEach { Box(Modifier.size(14.dp).background(it, RoundedCornerShape(4.dp))) }
            }
            Text("Más", style = tvText(16, FontWeight.Medium, Palette.InkSoft), maxLines = 1)
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
            .then(
                when {
                    isSelected -> Modifier.border(2.dp, Palette.Ink, shape)
                    cell.today -> Modifier.border(1.dp, Palette.Television, shape)
                    else -> Modifier
                }
            )
            .then(if (cell.future) Modifier else Modifier.tap(onClick))
            .semantics { contentDescription = heatCellAccessibleLabel(cell) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            cell.date.dayOfMonth.toString(),
            modifier = Modifier.align(Alignment.TopStart).padding(start = 3.dp, top = 1.dp),
            style = tvText(
                when {
                    width < 32.dp -> 8
                    width < 50.dp -> 10
                    else -> 12
                },
                FontWeight.Medium,
                Palette.InkSoft
            ),
            maxLines = 1,
            softWrap = false
        )
        if (!cell.future && cell.millis > 0L) {
            val consumptionFontSize = when {
                width < 32.dp -> 15
                width < 50.dp -> 21
                height < 25.dp -> 18
                else -> 22
            }
            val duration = if (width < 32.dp && cell.millis >= 60 * 60 * 1000L) {
                "${cell.millis / (60 * 60 * 1000L)}h"
            } else {
                compactHeatDuration(cell.millis)
            }
            Text(
                duration,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 1.dp),
                style = tvText(consumptionFontSize, FontWeight.Bold, if (level >= 3) Color.White else Palette.Ink),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
        if (isPeak) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = if (level >= 3) Color.White else Palette.Television, modifier = Modifier.align(Alignment.TopEnd).padding(1.dp).size(9.dp))
        }
    }
}

internal fun compactHeatDuration(milliseconds: Long): String {
    val minutes = milliseconds.coerceAtLeast(0L) / 60_000L
    if (milliseconds <= 0L) return ""
    if (minutes == 0L) return "<1m"
    val hours = minutes / 60L
    val remainingMinutes = minutes % 60L
    return if (hours > 0L) "${hours}h${if (remainingMinutes > 0L) remainingMinutes else ""}" else "${minutes}m"
}

private fun heatCellAccessibleLabel(cell: TvHeatCell): String {
    val date = cell.date.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM uuuu", Spanish))
    return buildString {
        append(date)
        append(" · ")
        append(when {
            cell.future -> "fecha futura"
            cell.millis > 0L -> formatTvDuration(cell.millis)
            else -> "sin uso"
        })
        if (cell.today) append(" · hoy")
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
    TvCard(modifier, contentPadding = 16.dp) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            PeriodStat("ESTA SEMANA", usage.weekMillis, tvComparisonText(week, "la semana pasada"), Modifier.weight(1f).fillMaxHeight())
            Box(Modifier.padding(horizontal = 10.dp).width(1.dp).fillMaxHeight(0.8f).background(Palette.Divider))
            PeriodStat("ESTE MES", usage.monthMillis, tvComparisonText(month, "el mes pasado"), Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun PeriodStat(label: String, millis: Long, comparison: Pair<Int, String>, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = tvText(23, FontWeight.SemiBold, Palette.InkSoft).copy(letterSpacing = 0.5.sp), maxLines = 1)
        Text(
            buildAnnotatedString {
                tvDurationParts(millis).forEachIndexed { i, (value, unit) ->
                    if (i > 0) append("  ")
                    withStyle(SpanStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold)) { append(value) }
                    withStyle(SpanStyle(fontSize = 50.sp, fontWeight = FontWeight.Medium)) { append(" $unit") }
                }
            },
            style = tvText(40, FontWeight.Bold, Palette.Television).copy(lineHeight = 46.sp, letterSpacing = (-1).sp),
            maxLines = 1,
            softWrap = false
        )
        val (direction, text) = comparison
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (direction != 0) {
                Icon(
                    if (direction > 0) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = null, tint = Palette.Television, modifier = Modifier.size(16.dp)
                )
            }
            Text(text, style = tvText(14, FontWeight.Medium, if (direction != 0) Palette.Television else Palette.InkSoft), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MonthsCard(usage: TvUsageView, today: LocalDate, modifier: Modifier) {
    val current = YearMonth.from(today)
    val months = usage.monthTotals.entries.toList()
    val maxMillis = months.maxOfOrNull { it.value } ?: 0L
    TvCard(modifier, contentPadding = 16.dp) {
        TvCardTitle("Por mes", minHeight = 34.dp)
        if (months.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Aún no hay meses con tiempo registrado", style = tvText(22, FontWeight.Medium, Palette.InkSoft), textAlign = TextAlign.Center)
            }
        } else {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val rowHeight = maxOf(48.dp, maxHeight / months.size)
                Column(
                    Modifier.fillMaxWidth().height(rowHeight * months.size).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    months.forEach { (month, millis) ->
                        val isCurrent = month == current
                        Column(
                            Modifier.fillMaxWidth().height(rowHeight).padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(month.tvMonthLabel(), modifier = Modifier.weight(1f), style = tvText(17, FontWeight.Medium, Palette.InkSoft), maxLines = 1, softWrap = false)
                                Text(
                                    formatTvDuration(millis),
                                    style = tvText(17, if (isCurrent) FontWeight.Bold else FontWeight.SemiBold, if (isCurrent) Palette.Television else Palette.Ink),
                                    maxLines = 1, softWrap = false
                                )
                            }
                            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(8.dp)).background(Palette.TelevisionSoft)) {
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
}
