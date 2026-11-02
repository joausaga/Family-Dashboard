package com.lito.planify.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lito.planify.dashboard.UiState
import com.lito.planify.dashboard.data.Event
import com.lito.planify.dashboard.data.TvDisplayStatus
import com.lito.planify.dashboard.data.homeCalendarDates
import com.lito.planify.dashboard.data.homeCalendarWeekStarts
import com.lito.planify.dashboard.data.homeUpcomingTasks
import com.lito.planify.dashboard.data.tvUsageAverageLastFiveCompletedDays
import com.lito.planify.dashboard.data.tvUsageLastFiveCompletedDays
import com.lito.planify.dashboard.ui.theme.Palette
import com.lito.planify.dashboard.ui.theme.Radius
import com.lito.planify.dashboard.ui.theme.Type
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle

private val Gap = 18.dp

@Composable
fun TodayScreen(state: UiState, onLoadWeek: (LocalDate) -> Unit, onGoToSettings: () -> Unit) {
    val today = state.now.toLocalDate()
    val dates = homeCalendarDates(today)
    LaunchedEffect(state.clockValid, today) {
        if (state.clockValid) homeCalendarWeekStarts(today).forEach(onLoadWeek)
    }
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 22.dp)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Gap)) {
            Row(Modifier.fillMaxWidth().weight(0.245f), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                OverviewCard(state, Modifier.weight(0.665f).fillMaxHeight())
                UpcomingEventCard(state, Modifier.weight(0.335f).fillMaxHeight())
            }
            Row(Modifier.fillMaxWidth().weight(0.505f), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                dates.forEach { date ->
                    val week = state.weeks[date.with(DayOfWeek.MONDAY)]
                    val events = week?.events ?: state.content?.events.orEmpty().filter { it.at.toLocalDate() == date }
                    val routines = week?.sessions ?: state.content?.sessions.orEmpty().filter { it.at.toLocalDate() == date }
                    HomeDayCard(date, today, state.now, events, routines, Modifier.weight(1f).fillMaxHeight())
                }
            }
            Row(Modifier.fillMaxWidth().weight(0.25f).padding(horizontal = 5.dp), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                UpcomingTasksCard(state, Modifier.weight(0.40f).fillMaxHeight())
                TvUsageCard(state, Modifier.weight(0.60f).fillMaxHeight(), onGoToSettings)
            }
        }
    }
}

@Composable
private fun Card(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .background(Palette.Card, RoundedCornerShape(Radius.Card))
            .border(1.dp, Palette.Border, RoundedCornerShape(Radius.Card))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        content = content
    )
}

// --- Resumen superior: hora, tiempo y siguiente actividad ---

@Composable
private fun OverviewCard(state: UiState, modifier: Modifier) {
    val today = state.now.toLocalDate()
    val colors = overviewColors(state.now.toLocalTime())
    val contentColor = colors.content
    Row(
        modifier.background(
            Brush.horizontalGradient(listOf(colors.start, colors.end)),
            RoundedCornerShape(Radius.Card)
        ).padding(horizontal = 26.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(0.40f).fillMaxHeight().padding(bottom = 12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (state.clockValid) state.now.format(HourMinute) else "--:--",
                style = Type.Clock.copy(fontSize = 164.sp, lineHeight = 164.sp, letterSpacing = (-7).sp, fontWeight = FontWeight.Bold, color = contentColor),
                maxLines = 1
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(today.weekdayName()) }
                    append(" ${today.dayOfMonth} de ${today.month.getDisplayName(TextStyle.FULL, Spanish)}")
                },
                style = Type.Small.copy(fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = 0.sp, color = contentColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        val forecast = state.forecast
        if (forecast == null) {
            Text("Cargando previsión…", style = Type.Small.copy(color = contentColor), modifier = Modifier.weight(0.55f))
        } else {
            CurrentWeather(forecast, Modifier.weight(0.18f).fillMaxHeight(), today, contentColor)
            Spacer(Modifier.width(14.dp))
            Row(Modifier.weight(0.42f).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                (1L..5L).forEach { offset ->
                    HomeForecastDay(forecast, today.plusDays(offset), Modifier.weight(1f).fillMaxHeight(0.85f), colors)
                }
            }
        }
    }
}

@Composable
private fun CurrentWeather(forecast: com.lito.planify.dashboard.data.Forecast, modifier: Modifier, today: LocalDate, contentColor: Color) {
    val current = forecast.current
    val day = forecast.dayIndex(today)
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WeatherIcon(current.weather_code, current.is_day == 1, size = 64, colorFilter = ColorFilter.tint(contentColor))
        Text(current.temperature_2m.degrees(), style = Type.Large.copy(fontSize = 62.sp, lineHeight = 64.sp, color = contentColor, fontWeight = FontWeight.SemiBold), maxLines = 1)
        if (day >= 0) {
            Text(
                "${forecast.daily.temperature_2m_min[day].degrees()} / ${forecast.daily.temperature_2m_max[day].degrees()}",
                style = Type.Tiny.copy(fontSize = 22.sp, lineHeight = 26.sp, color = contentColor),
                maxLines = 1,
                softWrap = false
            )
            val rain = forecast.daily.precipitation_probability_max[day] ?: 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                Text("$rain%", style = Type.Tiny.copy(fontSize = 19.sp, lineHeight = 22.sp, color = contentColor), maxLines = 1)
            }
        }
    }
}

@Composable
private fun HomeForecastDay(forecast: com.lito.planify.dashboard.data.Forecast, date: LocalDate, modifier: Modifier, colors: OverviewColors) {
    val day = forecast.dayIndex(date)
    val contentColor = colors.forecastContent
    Column(
        modifier.background(colors.forecastSurface, RoundedCornerShape(Radius.Small)).padding(vertical = 3.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Spanish).trimEnd('.'), style = Type.Tiny.copy(color = contentColor, fontSize = 21.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold), maxLines = 1)
        if (day >= 0) {
            WeatherIcon(forecast.daily.weather_code[day], size = 48, colorFilter = ColorFilter.tint(contentColor))
            Text(forecast.daily.temperature_2m_max[day].degrees(), style = Type.Tiny.copy(color = contentColor, fontSize = 24.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Text(forecast.daily.temperature_2m_min[day].degrees(), style = Type.Tiny.copy(color = contentColor, fontSize = 20.sp, lineHeight = 23.sp), maxLines = 1)
            val rain = forecast.daily.precipitation_probability_max[day] ?: 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
                Text("$rain%", style = Type.Tiny.copy(color = contentColor, fontSize = 19.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold), maxLines = 1)
            }
        } else Text("—", style = Type.Tiny.copy(color = contentColor))
    }
}

/** Regla de color única: recurrente = verde; resto = color de su calendario (barra + fondo tintado). */
private fun eventColors(routine: Boolean, calendarColor: String?): Pair<Color, Color> =
    if (routine) Palette.Success to Palette.SuccessSoft
    else parseColor(calendarColor).let { it to it.copy(alpha = 0.16f).compositeOver(Palette.Card) }

/** Texto legible sobre un color de calendario arbitrario: blanco si contrasta ≥4,5:1; si no, tinta oscura. */
private fun onColor(bg: Color): Color =
    if (1.05f / (bg.luminance() + 0.05f) >= 4.5f) Color.White else Palette.Ink

@Composable
private fun UpcomingEventCard(state: UiState, modifier: Modifier) {
    val next = nextUp(state)
    val event = next?.first
    val routine = next?.second == true
    val (accent, tint) = eventColors(routine, event?.calendar_color)
    val shape = RoundedCornerShape(Radius.Card)
    Row(
        modifier.clip(shape).background(tint).height(IntrinsicSize.Min),
    ) {
        Box(Modifier.width(10.dp).fillMaxHeight().background(accent))
        Column(
            Modifier.weight(1f).fillMaxHeight().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (event == null) {
                Text("LO PRÓXIMO", style = Type.Label.copy(color = Palette.InkSoft, fontSize = 21.sp))
                Text("No hay próximos eventos", style = Type.Large.copy(fontSize = 36.sp, lineHeight = 40.sp), maxLines = 2)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("LO PRÓXIMO", style = Type.Label.copy(color = Palette.InkSoft, fontSize = 21.sp), maxLines = 1, modifier = Modifier.weight(1f))
                    Text(
                        naturalCountdown(state.now, event.at),
                        style = Type.Small.copy(color = onColor(accent), fontSize = 23.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        modifier = Modifier.background(accent, RoundedCornerShape(Radius.Column)).padding(horizontal = 13.dp, vertical = 6.dp)
                    )
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (routine) {
                        Icon(Icons.Outlined.Repeat, contentDescription = "Evento recurrente", tint = Palette.InkSoft, modifier = Modifier.size(25.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(event.title, style = Type.Large.copy(color = Palette.Ink, fontSize = 37.sp, lineHeight = 41.sp, fontWeight = FontWeight.SemiBold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                val end = event.until?.takeIf { it.toLocalDate() == event.at.toLocalDate() }
                val timeLabel = if (end != null) "${event.at.format(HourMinute)}–${end.format(HourMinute)}" else event.at.format(HourMinute)
                Text("${relativeDay(event.at.toLocalDate(), state.now.toLocalDate())} · $timeLabel", style = Type.Small.copy(color = Palette.InkSoft, fontSize = 25.sp, lineHeight = 29.sp), maxLines = 1, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

internal fun naturalCountdown(now: LocalDateTime, at: LocalDateTime): String {
    val minutes = java.time.Duration.between(now, at).toMinutes().coerceAtLeast(0)
    if (minutes == 0L) return "ahora"
    val days = minutes / (24 * 60)
    val hours = (minutes / 60) % 24
    val remainder = minutes % 60
    return when {
        days > 0 && hours > 0 -> "en $days d $hours h"
        days > 0 -> "en $days d"
        hours > 0 && remainder > 0 -> "en $hours h $remainder min"
        hours > 0 -> "en $hours h"
        else -> "en $minutes min"
    }
}

/** Próximo evento y si es recurrente (routine_id o sesión de rutina). */
private fun nextUp(state: UiState): Pair<Event, Boolean>? {
    val candidates = buildList {
        state.content?.events.orEmpty().forEach { add(it to false) }
        state.content?.sessions.orEmpty().forEach { add(it to true) }
        state.weeks.values.forEach { week ->
            week.events.forEach { add(it to false) }
            week.sessions.forEach { add(it to true) }
        }
    }
    return candidates.map { (e, r) -> e to (r || e.routine_id != null) }
        .distinctBy { it.first.id to it.first.at }
        .filter { !it.first.at.isBefore(state.now) }
        .minByOrNull { it.first.at }
}

// --- Fila central: cinco días y tareas ---

@Composable
private fun HomeDayCard(
    date: LocalDate,
    today: LocalDate,
    now: LocalDateTime,
    events: List<Event>,
    routines: List<Event>,
    modifier: Modifier
) {
    val items = (events.map { it to false } + routines.map { it to true })
        .filter { it.first.at.toLocalDate() == date }
        .distinctBy { it.first.id to it.first.at }
        .sortedBy { it.first.at }
    val todayCard = date == today
    val shape = RoundedCornerShape(Radius.Column)
    Box(
        modifier.clip(shape)
            .background(if (todayCard) Palette.TodayTint else Palette.Card.copy(alpha = 0.72f))
            .border(1.dp, if (todayCard) Palette.TodayBorder else Palette.Border, shape)
    ) {
        if (todayCard) {
            Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(8.dp).background(Palette.Accent))
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (todayCard) "HOY" else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Spanish).trimEnd('.'),
                    style = Type.Small.copy(color = if (todayCard) Palette.Accent else Palette.InkSoft, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (todayCard) {
                    Box(Modifier.size(48.dp).background(Palette.Accent, CircleShape), contentAlignment = Alignment.Center) {
                        Text("${date.dayOfMonth}", style = Type.Small.copy(color = Color.White, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                    }
                } else {
                    Text("${date.dayOfMonth}", style = Type.Large.copy(color = Palette.Ink, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold), maxLines = 1)
                }
            }
            Box(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp).height(1.dp).background(Palette.Divider))
            if (items.isEmpty()) {
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.EventAvailable, contentDescription = null, tint = Palette.EmptyIcon, modifier = Modifier.size(56.dp))
                    Text("Libre", modifier = Modifier.padding(top = 10.dp), style = Type.Small.copy(color = Palette.InkSoft, fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                }
            } else {
                val scroll = rememberScrollState()
                val density = androidx.compose.ui.platform.LocalDensity.current
                val gap = with(density) { 7.dp.roundToPx() }
                var viewport by remember { mutableStateOf(0) }
                val heights = remember(items.size) { mutableStateListOf<Int>().apply { repeat(items.size) { add(0) } } }
                // Chips cuyo borde inferior queda por debajo de la zona visible
                val hidden by remember {
                    derivedStateOf {
                        var top = 0
                        var count = 0
                        heights.forEach { h -> top += h; if (top - gap / 2 > scroll.value + viewport) count++; top += gap }
                        count
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth().onSizeChanged { viewport = it.height }) {
                Column(Modifier.fillMaxSize().fadingScroll(scroll), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items.forEachIndexed { index, (event, isRoutine) ->
                        val routine = isRoutine || event.routine_id != null
                        val past = event.at.isBefore(now)
                        val (accent, tint) = eventColors(routine, event.calendar_color)
                        val end = event.until?.takeIf { it.isAfter(event.at) }
                        val timeText = event.at.format(HourMinute) + (end?.let { "–" + it.format(HourMinute) } ?: "")
                        Row(
                            Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                                .onSizeChanged { if (index < heights.size) heights[index] = it.height }
                                .clip(RoundedCornerShape(Radius.Chip))
                                .background(tint)
                                .alpha(if (past) 0.5f else 1f)
                        ) {
                            Box(Modifier.width(7.dp).fillMaxHeight().background(accent))
                            Column(
                                Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        timeText,
                                        style = Type.Tiny.copy(color = Palette.InkSoft, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (routine) {
                                        Icon(Icons.Outlined.Repeat, contentDescription = "Evento recurrente", tint = Palette.InkSoft, modifier = Modifier.padding(start = 6.dp).size(20.dp))
                                    }
                                }
                                Text(
                                    event.title,
                                    style = Type.Small.copy(color = Palette.Ink, fontSize = 30.sp, lineHeight = 35.sp, fontWeight = FontWeight.Medium),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                if (hidden > 0) {
                    Text(
                        "+$hidden más",
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                            .background(Palette.Ink, RoundedCornerShape(Radius.Pill)).padding(horizontal = 14.dp, vertical = 5.dp),
                        style = Type.Tiny.copy(color = Palette.Card, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun CardTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, title: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(tint.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        }
        Text(
            title,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
            style = Type.Small.copy(color = Palette.Ink, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
            maxLines = 1
        )
        trailing()
    }
}

@Composable
private fun UpcomingTasksCard(state: UiState, modifier: Modifier) {
    val allTasks = remember(state.content?.tasks) { homeUpcomingTasks(state.content?.tasks.orEmpty(), limit = Int.MAX_VALUE) }
    val today = state.now.toLocalDate()
    Card(modifier) {
        if (allTasks.isEmpty()) {
            val title: String
            val subtitle: String
            val emptyIcon: androidx.compose.ui.graphics.vector.ImageVector
            val emptyTint: Color
            when {
                !state.taskListSelectionReady && state.taskListSettingsLoading -> { title = "Cargando listas"; subtitle = "Espera mientras se actualizan"; emptyIcon = Icons.Outlined.HourglassEmpty; emptyTint = Palette.InkSoft }
                !state.taskListSelectionReady && state.taskListSettingsError != null -> { title = "No se pudieron cargar las listas"; subtitle = "Vuelve a intentarlo desde Ajustes"; emptyIcon = Icons.Outlined.ErrorOutline; emptyTint = Palette.Accent }
                state.taskListSelectionReady && state.visibleTaskListIds.isEmpty() -> { title = "No hay listas seleccionadas"; subtitle = "Activa una lista en Ajustes"; emptyIcon = Icons.Outlined.Checklist; emptyTint = Palette.InkSoft }
                else -> { title = "Todo al día"; subtitle = "No quedan tareas pendientes"; emptyIcon = Icons.Outlined.TaskAlt; emptyTint = Palette.Success }
            }
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Box(Modifier.size(84.dp).background(emptyTint.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(emptyIcon, contentDescription = null, tint = emptyTint, modifier = Modifier.size(48.dp))
                }
                Column(Modifier.padding(start = 20.dp)) {
                    Text(title, style = Type.Large.copy(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold), maxLines = 2)
                    Text(subtitle, style = Type.Small.copy(fontSize = 22.sp, lineHeight = 26.sp), maxLines = 2)
                }
            }
            return@Card
        }
        val lead = allTasks.first()
        val rest = allTasks.drop(1)
        val strips = rest.take(2)
        val extra = rest.size - strips.size
        val (leadInk, leadSoft) = dueColors(dueTone(lead.due, today))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Protagonista
            Row(
                Modifier.weight(if (rest.isEmpty()) 1f else 0.56f).fillMaxHeight()
                    .clip(RoundedCornerShape(Radius.Chip)).background(leadSoft)
            ) {
                Box(Modifier.width(8.dp).fillMaxHeight().background(leadInk))
                Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "TAREAS · ${allTasks.size}",
                        style = Type.Tiny.copy(color = Palette.InkSoft, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp),
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).background(leadInk.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(taskIcon(lead.title), contentDescription = null, tint = leadInk, modifier = Modifier.size(32.dp))
                        }
                        Text(
                            taskCountdown(lead.due, today),
                            modifier = Modifier.padding(start = 14.dp),
                            style = Type.Large.copy(color = leadInk, fontSize = 46.sp, lineHeight = 50.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
                            maxLines = 1
                        )
                    }
                    Column {
                        Text(
                            lead.title,
                            style = Type.Small.copy(color = Palette.Ink, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            lead.task_list_name.orEmpty().ifBlank { "Lista" },
                            style = Type.Tiny.copy(color = Palette.InkSoft, fontSize = 20.sp, lineHeight = 24.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            // Siguientes
            if (rest.isNotEmpty()) {
                Column(Modifier.weight(0.44f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    strips.forEach { task ->
                        val (ink, soft) = dueColors(dueTone(task.due, today))
                        Row(
                            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(Radius.Chip)).background(soft)
                        ) {
                            Box(Modifier.width(6.dp).fillMaxHeight().background(ink))
                            Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.Center) {
                                Text(
                                    task.title,
                                    style = Type.Small.copy(color = Palette.Ink, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    taskDueLabel(task.due, today),
                                    style = Type.Tiny.copy(color = ink, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    if (extra > 0) {
                        Text(
                            "+$extra más",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = Type.Tiny.copy(color = Palette.InkSoft, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private val Tabular = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")

private fun shortTv(ms: Long): String {
    val m = ms.coerceAtLeast(0) / 60_000
    return when {
        m == 0L -> "–"
        m < 60 -> "${m}m"
        m % 60 == 0L -> "${m / 60}h"
        else -> "${m / 60}h ${m % 60}"
    }
}

@Composable
private fun TvUsageCard(state: UiState, modifier: Modifier, onGoToSettings: () -> Unit) {
    val tv = state.tvUsage
    val today = state.now.toLocalDate()
    val days = remember(today, tv.dayTotals) { tvUsageLastFiveCompletedDays(today, tv.dayTotals).reversed() }
    val average = remember(today, tv.dayTotals) { tvUsageAverageLastFiveCompletedDays(today, tv.dayTotals) }
    val statusColor = when (tv.status) {
        TvDisplayStatus.ON -> Palette.Online
        TvDisplayStatus.NOT_CONFIGURED -> Palette.Television
        else -> Palette.InkSoft
    }
    val statusLabel = when (tv.status) {
        TvDisplayStatus.ON -> "Encendida"
        TvDisplayStatus.OFF -> "Apagada"
        TvDisplayStatus.OFF_INFERRED -> "Apagada"
        TvDisplayStatus.NOT_CONFIGURED -> "Configurar TV"
        TvDisplayStatus.UNKNOWN, TvDisplayStatus.ERROR -> "Sin confirmación"
        TvDisplayStatus.LOADING -> "Consultando…"
    }
    val diffMs = tv.todayMillis - average
    Card(modifier) {
        CardTitle(Icons.Outlined.Tv, Palette.Television, "Monitor de TV") {
            Row(
                Modifier
                    .clip(RoundedCornerShape(Radius.Pill))
                    .background(statusColor.copy(alpha = 0.14f))
                    .heightIn(min = 52.dp)
                    .then(if (tv.status == TvDisplayStatus.NOT_CONFIGURED) Modifier.clickable(onClick = onGoToSettings) else Modifier)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.size(14.dp).background(statusColor, CircleShape))
                Text(statusLabel, style = Type.Tiny.copy(color = statusColor, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold), maxLines = 1, softWrap = false)
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp).height(1.dp).background(Palette.Divider))
        Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
            // Hoy
            Column(Modifier.weight(0.34f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text("HOY", style = Type.Tiny.copy(color = Palette.InkSoft, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp), maxLines = 1)
                Text(
                    formatTvDuration(tv.todayMillis),
                    style = Type.Large.merge(Tabular).copy(color = Palette.Television, fontSize = 60.sp, lineHeight = 66.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
                val label = when {
                    average <= 0L -> "Sin historial"
                    kotlin.math.abs(diffMs) < 60_000 -> "Igual que el promedio"
                    diffMs > 0 -> "${formatTvDuration(diffMs)} sobre el promedio"
                    else -> "${formatTvDuration(-diffMs)} bajo el promedio"
                }
                Text(
                    label,
                    style = Type.Tiny.copy(color = if (diffMs > 0 && average > 0L) Palette.Television else Palette.InkSoft, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(Modifier.padding(horizontal = 14.dp).width(1.dp).fillMaxHeight(0.8f).background(Palette.Divider))
            // Tendencia de los últimos 5 días completos
            Column(Modifier.weight(0.66f).fillMaxHeight()) {
                TvTrendChart(days.map { it.millis }, Modifier.weight(1f).fillMaxWidth())
                val peak = days.maxOfOrNull { it.millis } ?: 0L
                Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
                    days.forEach { point ->
                        val isPeak = point.millis == peak && peak > 0L
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                shortTv(point.millis),
                                style = Type.Tiny.merge(Tabular).copy(
                                    color = if (isPeak) Palette.Television else Palette.InkSoft,
                                    fontSize = 22.sp, lineHeight = 26.sp,
                                    fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium
                                ),
                                maxLines = 1, softWrap = false
                            )
                            Text(
                                point.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Spanish).trimEnd('.'),
                                style = Type.Tiny.copy(color = Palette.InkSoft, fontSize = 20.sp, lineHeight = 24.sp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvTrendChart(values: List<Long>, modifier: Modifier) {
    val line = Palette.Television
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val topPad = 16.dp.toPx()
        val bottomPad = 14.dp.toPx()
        val h = size.height - topPad - bottomPad
        val scale = maxOf(values.max(), 1L).toFloat()
        val cell = size.width / values.size
        fun yOf(v: Long) = topPad + h * (1f - v / scale)
        val pts = values.mapIndexed { i, v -> Offset(cell * (i + 0.5f), yOf(v)) }

        val area = Path().apply {
            moveTo(pts.first().x, size.height - bottomPad)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, size.height - bottomPad)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(line.copy(alpha = 0.22f), line.copy(alpha = 0.02f)), topPad, size.height - bottomPad))

        val path = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
        drawPath(path, line, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val peak = values.max()
        pts.forEachIndexed { i, p ->
            val isPeak = values[i] == peak && peak > 0L
            val empty = values[i] == 0L
            if (isPeak) drawCircle(line.copy(alpha = 0.18f), 16.dp.toPx(), p)
            drawCircle(Palette.Card, 9.dp.toPx(), p)
            drawCircle(if (empty) line.copy(alpha = 0.4f) else line, if (isPeak) 7.dp.toPx() else 6.dp.toPx(), p)
        }
    }
}

internal fun formatHourlyMinutes(minutes: Int): String = if (minutes <= 0) "" else "${minutes.coerceAtMost(60)}m"

internal fun formatTvDuration(milliseconds: Long): String {
    val minutes = milliseconds.coerceAtLeast(0) / 60_000
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (hours == 0L) "$remainder min" else if (remainder == 0L) "$hours h" else "$hours h $remainder min"
}
