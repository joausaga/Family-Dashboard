package com.joausaga.familydashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joausaga.familydashboard.UiState
import com.joausaga.familydashboard.data.Task
import com.joausaga.familydashboard.data.TaskDayFilter
import com.joausaga.familydashboard.data.TaskSection
import com.joausaga.familydashboard.data.TaskTimeline
import com.joausaga.familydashboard.data.TasksSummary
import com.joausaga.familydashboard.data.filterTasks
import com.joausaga.familydashboard.data.tasksBySection
import com.joausaga.familydashboard.data.tasksByDue
import com.joausaga.familydashboard.data.tasksGroupedByList
import com.joausaga.familydashboard.data.tasksSummary
import com.joausaga.familydashboard.data.tasksTimeline
import com.joausaga.familydashboard.data.tasksWithTitles
import com.joausaga.familydashboard.ui.theme.Palette
import com.joausaga.familydashboard.ui.theme.Radius
import com.joausaga.familydashboard.ui.theme.Type
import java.time.LocalDate

private enum class TasksMode(val label: String) { ByDate("Por fecha"), ByList("Por lista") }

private data class ListInfo(val id: Int, val name: String, val count: Int)

private fun taskText(size: Int, weight: FontWeight = FontWeight.Medium, color: Color = Palette.Ink) =
    Type.Tiny.copy(fontSize = size.sp, lineHeight = (size * 1.2f).sp, fontWeight = weight, color = color, fontFeatureSettings = "tnum")

private fun Modifier.tapNoRipple(onClick: () -> Unit): Modifier =
    clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClick)

/** Pantalla Tareas: resumen, línea de tiempo y tareas por fecha o por lista, con filtros y expansión táctiles. */
@Composable
fun TasksScreen(state: UiState) {
    val today = state.now.toLocalDate()
    val all = remember(state.content?.tasks) { tasksWithTitles(state.content?.tasks.orEmpty()) }
    var mode by remember { mutableStateOf(TasksMode.ByDate) }
    var listFilter by remember { mutableStateOf<Int?>(null) }
    var dayFilter by remember { mutableStateOf<TaskDayFilter>(TaskDayFilter.None) }
    var expanded by remember { mutableStateOf<Int?>(null) }

    ScreenPadding {
        if (all.isEmpty()) {
            TasksEmpty(state)
            return@ScreenPadding
        }
        val lists = remember(all) {
            tasksGroupedByList(all).map { ListInfo(it.first().task_list_id, it.first().task_list_name.orEmpty().ifBlank { "Lista" }, it.size) }
        }
        val activeList = listFilter?.takeIf { id -> lists.any { it.id == id } }
        val filtersActive = activeList != null || dayFilter != TaskDayFilter.None
        val summary = remember(all, today) { tasksSummary(all, today) }
        val timeline = remember(all, activeList, today) { tasksTimeline(filterTasks(all, activeList, TaskDayFilter.None, today), today) }
        val visible = remember(all, activeList, dayFilter, today) { filterTasks(all, activeList, dayFilter, today) }
        val clear = { listFilter = null; dayFilter = TaskDayFilter.None; expanded = null }

        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            SummaryStrip(summary, mode) { mode = it }
            if (lists.size > 1 || filtersActive) {
                FilterRow(lists, all.size, activeList, filtersActive, onList = { listFilter = it; expanded = null }, onClear = clear)
            }
            TimelineCard(timeline, dayFilter, today) { picked ->
                dayFilter = if (dayFilter == picked) TaskDayFilter.None else picked
                expanded = null
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    visible.isEmpty() -> NoResults(clear)
                    mode == TasksMode.ByDate -> ByDateColumns(visible, today, expanded) { expanded = if (expanded == it) null else it }
                    else -> ByListColumns(visible, today, expanded) { expanded = if (expanded == it) null else it }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(Radius.Card)
    Column(modifier.background(Palette.Card, shape).border(1.dp, Palette.Border, shape).padding(20.dp), content = content)
}

@Composable
private fun SummaryStrip(summary: TasksSummary, mode: TasksMode, onMode: (TasksMode) -> Unit) {
    TaskCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                SummaryStat("Pendientes", summary.total, Palette.Ink, Modifier.weight(1f))
                SummaryStat("Vencidas", summary.overdue, if (summary.overdue > 0) Palette.Accent else Palette.InkSoft, Modifier.weight(1f))
                SummaryStat("Hoy", summary.today, Palette.Ink, Modifier.weight(1f))
                SummaryStat("Esta semana", summary.week, Palette.Ink, Modifier.weight(1f))
            }
            SegmentedPills(TasksMode.entries.map { it.label }, mode.ordinal) { onMode(TasksMode.entries[it]) }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("$value", style = taskText(68, FontWeight.Bold, color).copy(lineHeight = 72.sp, letterSpacing = (-1).sp), maxLines = 1)
        Text(label, style = taskText(22, FontWeight.Medium, Palette.InkSoft), maxLines = 1)
    }
}

@Composable
private fun SegmentedPills(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(Radius.Pill)).background(Palette.Highlight).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selected
            Box(
                Modifier.heightIn(min = 52.dp).clip(RoundedCornerShape(Radius.Pill))
                    .background(if (active) Palette.Ink else Color.Transparent)
                    .tapNoRipple { onSelect(index) }.padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = taskText(24, FontWeight.SemiBold, if (active) Palette.Card else Palette.InkSoft), maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun FilterRow(lists: List<ListInfo>, total: Int, active: Int?, filtersActive: Boolean, onList: (Int?) -> Unit, onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        FilterChip("Todas · $total", active == null) { onList(null) }
        lists.forEach { FilterChip("${it.name} · ${it.count}", active == it.id) { onList(if (active == it.id) null else it.id) } }
        if (filtersActive) {
            Row(
                Modifier.heightIn(min = 52.dp).clip(RoundedCornerShape(Radius.Pill)).border(2.dp, Palette.Accent, RoundedCornerShape(Radius.Pill))
                    .tapNoRipple(onClear).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Close, contentDescription = null, tint = Palette.Accent, modifier = Modifier.size(24.dp))
                Text("Limpiar filtros", style = taskText(22, FontWeight.SemiBold, Palette.Accent), maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 52.dp).clip(RoundedCornerShape(Radius.Pill))
            .background(if (selected) Palette.Ink else Palette.Highlight)
            .tapNoRipple(onClick).padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = taskText(24, FontWeight.SemiBold, if (selected) Palette.Card else Palette.InkSoft), maxLines = 1, softWrap = false)
    }
}

@Composable
private fun TimelineCard(timeline: TaskTimeline, selected: TaskDayFilter, today: LocalDate, onPick: (TaskDayFilter) -> Unit) {
    TaskCard(Modifier.fillMaxWidth().height(172.dp)) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (timeline.overdue > 0) {
                TimelineCell(
                    label = "Venc.", number = null, count = timeline.overdue, ink = Palette.Accent,
                    selected = selected == TaskDayFilter.Overdue, isToday = false, modifier = Modifier.weight(1f)
                ) { onPick(TaskDayFilter.Overdue) }
            }
            timeline.days.forEachIndexed { index, day ->
                val tone = dueTone(day.date.toString(), today)
                TimelineCell(
                    label = if (index == 0) "Hoy" else WeekdayInitials[day.date.dayOfWeek.value - 1],
                    number = day.date.dayOfMonth.toString(), count = day.count, ink = dueColors(tone).first,
                    selected = selected == TaskDayFilter.Day(day.date), isToday = index == 0, modifier = Modifier.weight(1f)
                ) { onPick(TaskDayFilter.Day(day.date)) }
            }
        }
    }
}

@Composable
private fun TimelineCell(label: String, number: String?, count: Int, ink: Color, selected: Boolean, isToday: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.Chip)
    val fg = if (selected) Palette.Card else Palette.Ink
    val sub = if (selected) Palette.Card else Palette.InkSoft
    Column(
        modifier.fillMaxHeight().clip(shape)
            .background(if (selected) Palette.Ink else if (isToday) Palette.Highlight else Color.Transparent)
            .tapNoRipple(onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(label, style = taskText(20, FontWeight.SemiBold, sub), maxLines = 1, softWrap = false)
        if (number != null) {
            Text(number, style = taskText(32, FontWeight.Bold, fg), maxLines = 1)
        } else {
            Icon(Icons.Outlined.WarningAmber, contentDescription = "Vencidas", tint = if (selected) Palette.Card else Palette.Accent, modifier = Modifier.size(34.dp))
        }
        if (count > 0) {
            Box(Modifier.size(34.dp).background(ink, CircleShape), contentAlignment = Alignment.Center) {
                Text("$count", style = taskText(20, FontWeight.Bold, Color.White), maxLines = 1)
            }
        } else {
            Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) { Box(Modifier.size(8.dp).background(Palette.Line, CircleShape)) }
        }
    }
}

private val WeekdayInitials = listOf("L", "M", "X", "J", "V", "S", "D")

private val SectionTitles = mapOf(
    TaskSection.Overdue to "Vencidas", TaskSection.Today to "Hoy", TaskSection.Tomorrow to "Mañana",
    TaskSection.ThisWeek to "Esta semana", TaskSection.Later to "Más adelante", TaskSection.NoDate to "Sin fecha"
)

private val ColumnEmptyText = listOf("Nada vencido ni para hoy", "Nada para mañana ni esta semana", "Nada más adelante ni sin fecha")

@Composable
private fun ByDateColumns(tasks: List<Task>, today: LocalDate, expanded: Int?, onToggle: (Int) -> Unit) {
    val grouped = remember(tasks, today) { tasksBySection(tasks, today) }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        (0..2).forEach { column ->
            val sections = grouped.filterKeys { it.column == column }
            TaskCard(Modifier.weight(1f).fillMaxHeight()) {
                if (sections.isEmpty()) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Outlined.TaskAlt, contentDescription = null, tint = Palette.EmptyIcon, modifier = Modifier.size(44.dp))
                        Text(ColumnEmptyText[column], modifier = Modifier.padding(top = 10.dp), style = taskText(24, FontWeight.Medium, Palette.InkSoft), textAlign = TextAlign.Center)
                    }
                } else {
                    Column(Modifier.fillMaxSize().fadingScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        sections.entries.forEachIndexed { index, (section, list) ->
                            SectionHeader(SectionTitles.getValue(section), list.size, section == TaskSection.Overdue, Modifier.padding(top = if (index == 0) 0.dp else 10.dp))
                            list.forEach { TaskRowCard(it, today, showList = true, expanded = expanded == it.id) { onToggle(it.id) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ByListColumns(tasks: List<Task>, today: LocalDate, expanded: Int?, onToggle: (Int) -> Unit) {
    val lists = remember(tasks) { tasksGroupedByList(tasks) }
    val scrolls = lists.size > 3
    Row(
        Modifier.fillMaxSize().then(if (scrolls) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        lists.forEach { list ->
            TaskCard((if (scrolls) Modifier.width(520.dp) else Modifier.weight(1f)).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(list.first().task_list_name.orEmpty().ifBlank { "Lista" }, modifier = Modifier.weight(1f), style = taskText(30, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    CountPill(list.size)
                }
                Column(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp).fadingScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    tasksByDue(list).forEach { TaskRowCard(it, today, showList = false, expanded = expanded == it.id) { onToggle(it.id) } }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, alert: Boolean, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title.uppercase(Spanish), modifier = Modifier.weight(1f),
            style = taskText(22, FontWeight.Bold, if (alert) Palette.Accent else Palette.InkSoft).copy(letterSpacing = 1.5.sp), maxLines = 1
        )
        CountPill(count)
    }
}

@Composable
private fun CountPill(count: Int) {
    Text(
        "$count", style = taskText(22, FontWeight.Bold, Palette.InkSoft), maxLines = 1,
        modifier = Modifier.background(Palette.Highlight, RoundedCornerShape(Radius.Pill)).padding(horizontal = 14.dp, vertical = 4.dp)
    )
}

@Composable
private fun TaskRowCard(task: Task, today: LocalDate, showList: Boolean, expanded: Boolean, onToggle: () -> Unit) {
    val ink = dueColors(dueTone(task.due, today)).first
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(Radius.Chip)).background(Palette.Background).tapNoRipple(onToggle)
    ) {
        Box(Modifier.width(8.dp).fillMaxHeight().background(ink))
        Row(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(44.dp).background(ink.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(taskIcon(task.title), contentDescription = null, tint = ink, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    task.title, style = taskText(26, FontWeight.SemiBold),
                    maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis
                )
                if (showList || expanded) {
                    Text(
                        task.task_list_name.orEmpty().ifBlank { "Lista" },
                        modifier = Modifier.background(Palette.Highlight, RoundedCornerShape(Radius.Small)).padding(horizontal = 10.dp, vertical = 2.dp),
                        style = taskText(20, FontWeight.Medium, Palette.InkSoft), maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (expanded) Text(taskDueSentence(task.due, today), style = taskText(22, FontWeight.Medium, Palette.InkSoft))
            }
            Text(
                taskCountdown(task.due, today), style = taskText(22, FontWeight.Bold, ink), maxLines = 1, softWrap = false,
                modifier = Modifier.background(Palette.Card, RoundedCornerShape(Radius.Pill)).padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun NoResults(onClear: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("No hay tareas con estos filtros", style = taskText(36, FontWeight.SemiBold), textAlign = TextAlign.Center)
        PillButton("Limpiar filtros", Modifier.padding(top = 24.dp).width(340.dp), primary = true, onClick = onClear)
    }
}

@Composable
private fun TasksEmpty(state: UiState) {
    val title: String
    val subtitle: String
    val icon: ImageVector
    val tint: Color
    when {
        !state.taskListSelectionReady && state.taskListSettingsLoading -> { title = "Cargando listas de tareas"; subtitle = "Espera mientras se actualizan las listas"; icon = Icons.Outlined.HourglassEmpty; tint = Palette.InkSoft }
        !state.taskListSelectionReady && state.taskListSettingsError != null -> { title = "No se pudieron cargar las listas"; subtitle = "Vuelve a intentarlo desde Ajustes"; icon = Icons.Outlined.ErrorOutline; tint = Palette.Accent }
        state.taskListSelectionReady && state.visibleTaskListIds.isEmpty() -> { title = "No hay listas seleccionadas"; subtitle = "Activa al menos una lista en Ajustes"; icon = Icons.Outlined.Checklist; tint = Palette.InkSoft }
        else -> { title = "Todo al día"; subtitle = "No quedan tareas pendientes en las listas visibles"; icon = Icons.Outlined.TaskAlt; tint = Palette.Success }
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(168.dp).background(tint.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(88.dp))
        }
        Text(title, modifier = Modifier.padding(top = 28.dp), style = taskText(44, FontWeight.SemiBold), textAlign = TextAlign.Center)
        Text(subtitle, modifier = Modifier.padding(top = 8.dp), style = taskText(28, FontWeight.Normal, Palette.InkSoft), textAlign = TextAlign.Center)
    }
}
