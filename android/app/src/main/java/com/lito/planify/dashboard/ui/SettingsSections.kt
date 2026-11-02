package com.lito.planify.dashboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lito.planify.dashboard.UiState
import com.lito.planify.dashboard.data.CalendarTaskRefreshInterval
import com.lito.planify.dashboard.data.GoogleCalendar
import com.lito.planify.dashboard.data.GoogleTaskList
import com.lito.planify.dashboard.data.TvLocalApi
import com.lito.planify.dashboard.data.TvMonitorIntervals
import com.lito.planify.dashboard.data.TvMonitorSettings
import com.lito.planify.dashboard.data.WifiStatus
import com.lito.planify.dashboard.data.readWifiStatus
import com.lito.planify.dashboard.ui.theme.Palette
import com.lito.planify.dashboard.ui.theme.Radius
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------- Contenido ----------

private val ListWidth = 780.dp

@Composable
internal fun ContentSection(state: UiState, callbacks: SettingsCallbacks) {
    val visibleCalendars = state.calendars.count { it.id in state.visibleCalendarIds }
    SettingsGroup("Calendarios", trailing = { if (state.calendars.isNotEmpty()) SettingsCountPill("$visibleCalendars de ${state.calendars.size} visibles") }) {
        CalendarList(state.calendars, state.visibleCalendarIds, state.calendarSettingsLoading, state.calendarSettingsError, callbacks)
    }
    SettingsDivider()
    val visibleLists = state.taskLists.count { it.id in state.visibleTaskListIds }
    SettingsGroup("Listas de tareas", trailing = { if (state.taskLists.isNotEmpty()) SettingsCountPill("$visibleLists de ${state.taskLists.size} visibles") }) {
        TaskListList(state.taskLists, state.visibleTaskListIds, state.taskListSettingsLoading, state.taskListSettingsError, callbacks)
    }
}

@Composable
private fun CalendarList(calendars: List<GoogleCalendar>, visible: Set<String>, loading: Boolean, error: String?, callbacks: SettingsCallbacks) {
    when {
        loading && calendars.isEmpty() -> Text("Cargando calendarios…", style = settingsText(26, FontWeight.Normal, Palette.InkSoft))
        error != null -> RetryBlock(error, callbacks.onRefreshCalendarSettings)
        calendars.isEmpty() -> Text("No hay calendarios asociados a esta cuenta.", style = settingsText(24, FontWeight.Normal, Palette.InkSoft))
        else -> Column(Modifier.widthIn(max = ListWidth)) {
            val ordered = activeFirst(calendars) { it.id in visible }
            ordered.forEachIndexed { index, calendar ->
                val title = if (calendar.primary == true) "Calendario principal" else calendar.summary?.takeIf { it.isNotBlank() } ?: "Calendario sin nombre"
                val detail = if (calendar.hidden == true) "Oculto en Google Calendar" else null
                SettingsSwitchRow(
                    label = title, checked = calendar.id in visible, description = detail,
                    stateOn = "Visible en Family Dashboard", stateOff = "Oculto en Family Dashboard", dimWhenOff = true,
                    leading = {
                        val active = calendar.id in visible
                        Box(Modifier.size(24.dp).background(if (active) parseColor(calendar.backgroundColor, Palette.InkSoft) else Palette.InkMuted, CircleShape))
                    },
                    onChange = { callbacks.onCalendarVisibilityChange(calendar.id, it) }
                )
                if (index < ordered.lastIndex) SettingsDivider()
            }
        }
    }
}

@Composable
private fun TaskListList(lists: List<GoogleTaskList>, visible: Set<String>, loading: Boolean, error: String?, callbacks: SettingsCallbacks) {
    when {
        loading && lists.isEmpty() -> Text("Cargando listas de tareas…", style = settingsText(26, FontWeight.Normal, Palette.InkSoft))
        error != null && lists.isEmpty() -> RetryBlock(error, callbacks.onRefreshTaskListSettings)
        lists.isEmpty() -> Text("No hay listas de tareas asociadas a esta cuenta.", style = settingsText(24, FontWeight.Normal, Palette.InkSoft))
        else -> Column(Modifier.widthIn(max = ListWidth)) {
            if (error != null) RetryBlock(error, callbacks.onRefreshTaskListSettings)
            lists.forEachIndexed { index, list ->
                SettingsSwitchRow(
                    label = list.title?.takeIf { it.isNotBlank() } ?: "Lista sin nombre", checked = list.id in visible,
                    stateOn = "Visible en Family Dashboard", stateOff = "Oculta en Family Dashboard", dimWhenOff = true,
                    leading = { Icon(Icons.Outlined.Checklist, contentDescription = null, tint = Palette.InkSoft, modifier = Modifier.size(28.dp)) },
                    onChange = { callbacks.onTaskListVisibilityChange(list.id, it) }
                )
                if (index < lists.lastIndex) SettingsDivider()
            }
        }
    }
}

@Composable
private fun RetryBlock(error: String, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(error, style = settingsText(22, FontWeight.Medium, Palette.Accent), maxLines = 3, overflow = TextOverflow.Ellipsis)
        SettingsButton("Volver a cargar", Modifier.width(300.dp), onClick = onRetry)
    }
}

// ---------- Pantalla ----------

@Composable
internal fun DisplaySection(state: UiState, callbacks: SettingsCallbacks, onPickTime: (TimePickRequest) -> Unit) {
    val schedule = state.schedule
    SettingsGroup("Horario de pantalla") {
        SettingsSwitchRow(
            "Horario activo", schedule.enabled,
            description = "La pantalla se enciende y se apaga a las horas que elijas."
        ) { callbacks.onSchedule(schedule.copy(enabled = it)) }
        if (schedule.enabled) {
            SettingsDivider()
            SettingsSwitchRow(
                "Reloj nocturno", !schedule.screenOff,
                description = "Fuera de horario muestra un reloj tenue en lugar de apagar la pantalla."
            ) { callbacks.onSchedule(schedule.copy(screenOff = !it)) }
            SettingsDivider()
            ScheduleDayRow("Lunes a viernes", schedule.weekdayOn, schedule.weekdayOff, onPickTime) { on, off ->
                callbacks.onSchedule(schedule.copy(weekdayOn = on, weekdayOff = off))
            }
            ScheduleDayRow("Sábado y domingo", schedule.weekendOn, schedule.weekendOff, onPickTime) { on, off ->
                callbacks.onSchedule(schedule.copy(weekendOn = on, weekendOff = off))
            }
        }
    }
    SettingsDivider()
    BrightnessGroup(state.brightness, callbacks.onBrightness)
}

/**
 * Horario de un tipo de día: dos botones de hora (enciende y apaga) que abren el selector de reloj.
 */
@Composable
private fun ScheduleDayRow(label: String, on: Int, off: Int, onPickTime: (TimePickRequest) -> Unit, onCommit: (Int, Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = settingsText(28, FontWeight.SemiBold), maxLines = 1)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TimeButton("Enciende", Icons.Outlined.WbSunny, Palette.AmberInk, on) {
                onPickTime(TimePickRequest("Enciende · $label", on, { scheduleTimeError(true, it, on, off) }) { onCommit(it, off) })
            }
            TimeButton("Apaga", Icons.Outlined.Bedtime, Palette.ScheduleNight, off) {
                onPickTime(TimePickRequest("Apaga · $label", off, { scheduleTimeError(false, it, on, off) }) { onCommit(on, it) })
            }
        }
    }
}

@Composable
private fun TimeButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, minutes: Int, onClick: () -> Unit) {
    Row(
        Modifier.width(340.dp).height(84.dp).clip(RoundedCornerShape(24.dp)).background(SettingsControl)
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(34.dp))
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(label, style = settingsText(20, FontWeight.Medium, Palette.InkSoft), maxLines = 1)
            Text(scheduleTimeLabel(minutes), style = settingsText(36, FontWeight.Bold), maxLines = 1)
        }
    }
}

/** Brillo mínimo del deslizador: más bajo, la pantalla apenas se ve de día. */
private const val MIN_BRIGHTNESS = 0.05f

/** Salto del deslizador: de 5 en 5 %. */
private const val BRIGHTNESS_STEP = 0.05f

@Composable
private fun BrightnessGroup(brightness: Float?, onChange: (Float?) -> Unit) {
    // Último brillo manual, para volver a él al quitar el automático
    var manual by remember { mutableFloatStateOf(brightness ?: 0.8f) }
    val auto = brightness == null
    SettingsGroup(
        "Brillo",
        trailing = { Text(if (auto) "Automático" else "${(manual * 100).roundToInt()} %", style = settingsText(32, FontWeight.Bold), maxLines = 1) }
    ) {
        SettingsSwitchRow(
            "Automático", auto,
            description = "El sistema ajusta el brillo según la luz del entorno."
        ) { onChange(if (it) null else manual) }
        LevelSlider(manual, enabled = !auto, modifier = Modifier.fillMaxWidth()) {
            manual = it
            onChange(it)
        }
        if (auto) Text("Desactiva «Automático» para ajustar el brillo a mano.", style = settingsText(22, FontWeight.Normal, Palette.InkSoft))
    }
}

/** Deslizador a tamaño de este panel: se toca o se arrastra; desactivado, se ve translúcido y no responde. */
@Composable
private fun LevelSlider(value: Float, enabled: Boolean, modifier: Modifier = Modifier, onChange: (Float) -> Unit) {
    val change by rememberUpdatedState(onChange)
    val thumb = 26f
    fun valueAt(x: Float, width: Int): Float {
        val raw = MIN_BRIGHTNESS + (1f - MIN_BRIGHTNESS) * ((x - thumb) / (width - thumb * 2))
        return ((raw / BRIGHTNESS_STEP).roundToInt() * BRIGHTNESS_STEP).coerceIn(MIN_BRIGHTNESS, 1f)
    }
    Canvas(
        modifier
            .height(84.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .pointerInput(enabled) { if (enabled) detectTapGestures { change(valueAt(it.x, size.width)) } }
            .pointerInput(enabled) { if (enabled) detectHorizontalDragGestures { pointer, _ -> change(valueAt(pointer.position.x, size.width)) } }
    ) {
        val y = size.height / 2
        val x = thumb + (size.width - thumb * 2) * ((value - MIN_BRIGHTNESS) / (1f - MIN_BRIGHTNESS))
        drawLine(Palette.Line, Offset(thumb, y), Offset(size.width - thumb, y), strokeWidth = 16f, cap = StrokeCap.Round)
        drawLine(Palette.Ink, Offset(thumb, y), Offset(x, y), strokeWidth = 16f, cap = StrokeCap.Round)
        drawCircle(Palette.Ink, thumb, Offset(x, y))
        drawCircle(Palette.Card, thumb - 8f, Offset(x, y))
    }
}

// ---------- Servicios ----------

@Composable
internal fun ServicesSection(state: UiState, callbacks: SettingsCallbacks) {
    LocationGroup(state, callbacks)
    SettingsDivider()
    TvMonitorGroup(state.tvMonitorSettings, state.tvSettingsError, callbacks.onSaveTvMonitorSettings)
    SettingsDivider()
    RefreshGroup(state.calendarTaskRefreshMinutes, callbacks.onCalendarTaskRefreshInterval)
}

@Composable
private fun LocationGroup(state: UiState, callbacks: SettingsCallbacks) {
    var query by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val search = {
        keyboard?.hide()
        callbacks.onSearch(query)
    }
    SettingsGroup("Ubicación del tiempo") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Place, contentDescription = null, tint = Palette.InkSoft, modifier = Modifier.size(30.dp))
            Text(state.location?.let { "${it.name} · ${it.detail}" } ?: "Sin ubicación", style = settingsText(28, FontWeight.Medium), maxLines = 2)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Field(query, { query = it }, "Buscar ciudad", Modifier.weight(1f), background = SettingsControl, imeAction = ImeAction.Search, onAction = search)
            SettingsButton(
                if (state.geoSearching) "Buscando…" else "Buscar", Modifier.width(240.dp),
                style = SettingsButtonStyle.Primary, enabled = query.isNotBlank() && !state.geoSearching, onClick = search
            )
        }
        state.geoResults.take(5).forEach { result ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .clickable { query = ""; callbacks.onSelectLocation(result) }.padding(vertical = 16.dp)
            ) {
                Text(result.name, style = settingsText(28, FontWeight.Medium), maxLines = 1)
                Text(
                    listOfNotNull(result.admin1, result.country).joinToString(", "),
                    style = settingsText(24, FontWeight.Normal, Palette.InkSoft), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 18.dp)
                )
            }
            SettingsDivider()
        }
    }
}

private sealed interface TvTest {
    object Idle : TvTest
    object Running : TvTest
    data class Responds(val on: Boolean) : TvTest
    data class Fails(val message: String) : TvTest
}

@Composable
private fun TvMonitorGroup(settings: TvMonitorSettings, error: String?, onSave: (String, Int) -> Unit) {
    var ip by remember(settings.ipAddress) { mutableStateOf(settings.ipAddress) }
    var interval by remember(settings.pollMinutes) { mutableIntStateOf(settings.pollMinutes) }
    var saves by remember { mutableIntStateOf(0) }
    var showSaved by remember { mutableStateOf(false) }
    var test by remember(ip) { mutableStateOf<TvTest>(TvTest.Idle) }
    val scope = rememberCoroutineScope()
    val ipValid = ip.isBlank() || TvLocalApi.isIpAddress(ip.trim())
    val intervalValid = interval in TvMonitorIntervals.options
    LaunchedEffect(saves) {
        if (saves > 0) {
            showSaved = true
            delay(3_000)
            showSaved = false
        }
    }
    SettingsGroup("Monitorización de TV", description = "Consulta la TV por la red local para saber cuánto tiempo está encendida.") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Dirección IP de la TV", style = settingsText(22, FontWeight.Medium, Palette.InkSoft), maxLines = 1)
                Field(ip, { ip = it }, "Ej. 192.168.1.50", Modifier.fillMaxWidth(), background = SettingsControl, keyboardType = KeyboardType.Uri)
            }
            Column(Modifier.weight(1f)) {
                Text("Sondeo", style = settingsText(22, FontWeight.Medium, Palette.InkSoft), maxLines = 1)
                MinuteDropdown(interval, TvMonitorIntervals.options, { interval = it }, Modifier.fillMaxWidth())
            }
            SettingsButton(
                "Probar conexión", Modifier.width(320.dp), icon = Icons.Outlined.Tv,
                enabled = ip.isNotBlank() && ipValid && test != TvTest.Running
            ) {
                test = TvTest.Running
                val target = ip.trim()
                scope.launch {
                    test = try {
                        TvTest.Responds(TvLocalApi().isOn(target))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        TvTest.Fails(tvTestFailureMessage(failure))
                    }
                }
            }
            SettingsButton("Guardar", Modifier.width(220.dp), style = SettingsButtonStyle.Primary, enabled = ipValid && intervalValid) {
                onSave(ip.trim(), interval)
                saves++
            }
        }
        val message = error ?: when {
            !ipValid -> "Escribe una dirección IP válida."
            !intervalValid -> "Elige una frecuencia disponible."
            else -> null
        }
        when (val t = test) {
            TvTest.Running -> Text("Probando…", style = settingsText(24, FontWeight.SemiBold, Palette.InkSoft))
            is TvTest.Responds -> Text(
                if (t.on) "La TV responde: encendida" else "La TV responde: en reposo",
                style = settingsText(24, FontWeight.SemiBold, Palette.Success)
            )
            is TvTest.Fails -> Text(t.message, style = settingsText(24, FontWeight.SemiBold, Palette.Accent), maxLines = 2)
            TvTest.Idle -> when {
                message != null -> Text(message, style = settingsText(22, FontWeight.Medium, Palette.Accent))
                showSaved -> SavedBadge()
                else -> Text("Si la TV no responde se cuenta como apagada (inferida).", style = settingsText(22, FontWeight.Normal, Palette.InkSoft))
            }
        }
    }
}

@Composable
private fun RefreshGroup(minutes: Int, onChange: (Int) -> Unit) {
    var saves by remember { mutableIntStateOf(0) }
    var showSaved by remember { mutableStateOf(false) }
    LaunchedEffect(saves) {
        if (saves > 0) {
            showSaved = true
            delay(3_000)
            showSaved = false
        }
    }
    SettingsGroup(
        "Actualización de calendario y tareas", description = "Frecuencia de sincronización automática.",
        trailing = { if (showSaved) SavedBadge() }
    ) {
        MinuteDropdown(minutes, CalendarTaskRefreshInterval.options, { onChange(it); saves++ }, Modifier.width(360.dp))
    }
}

// ---------- Dispositivo ----------

@Composable
internal fun DeviceSection(state: UiState, actions: DashboardActions, onRequestLogout: () -> Unit) {
    SettingsGroup("Cuenta") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.size(64.dp).background(Palette.Highlight, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = Palette.InkSoft, modifier = Modifier.size(36.dp))
            }
            Column {
                Text(if (state.token != null) "Cuenta de Google conectada" else "Sin sesión", style = settingsText(28, FontWeight.SemiBold))
                Text(
                    state.calendarAccountId ?: if (state.calendarSettingsLoading) "Obteniendo la cuenta…" else "Correo no disponible",
                    style = settingsText(24, FontWeight.Medium, Palette.InkSoft), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    SettingsDivider()
    ServiceStatusGroup(state)
    SettingsDivider()
    SettingsGroup("Conexión") { WifiCard() }
    SettingsDivider()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        SettingsButton("Wi-Fi", Modifier.weight(1f), icon = Icons.Outlined.Wifi, onClick = actions.openWifi)
        SettingsButton("Ajustes de Android", Modifier.weight(1.35f), icon = Icons.Outlined.Settings, onClick = actions.openSettings)
        SettingsButton("Cerrar sesión", Modifier.weight(1f), style = SettingsButtonStyle.Destructive, onClick = onRequestLogout)
    }
}

@Composable
private fun ServiceStatusGroup(state: UiState) {
    val now = state.now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    SettingsGroup("Estado de los servicios") {
        ServiceRow("Calendario y tareas", state.contentFetchedAt?.let { now - it }, state.calendarTaskRefreshMinutes, "Aún no se ha sincronizado")
        ServiceRow("Tiempo", state.forecastFetchedAt?.let { now - it }, 30, "Aún no se ha sincronizado")
        if (state.tvMonitorSettings.isConfigured) {
            ServiceRow("TV", state.tvUsage.lastSyncMillis?.let { now - it }, state.tvMonitorSettings.pollMinutes, "Aún no se ha consultado")
        } else {
            ServiceRow("TV", null, 0, "Sin configurar")
        }
    }
}

@Composable
private fun ServiceRow(name: String, ageMillis: Long?, expectedMinutes: Int, missing: String) {
    val fresh = serviceIsFresh(ageMillis, expectedMinutes)
    val color = when {
        ageMillis == null -> Palette.InkSoft
        fresh -> Palette.Online
        else -> Palette.Warm
    }
    Row(Modifier.fillMaxWidth().widthIn(max = ListWidth), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(16.dp).background(color, CircleShape))
        Text(name, style = settingsText(28, FontWeight.Medium), modifier = Modifier.padding(start = 16.dp).weight(1f), maxLines = 1)
        Text(
            when {
                ageMillis == null -> missing
                fresh -> "Sincronizado ${tvAgoLabel(ageMillis)}"
                else -> "Atrasado: ${tvAgoLabel(ageMillis)}"
            },
            style = settingsText(24, FontWeight.Medium, if (ageMillis != null && !fresh) Palette.AmberInk else Palette.InkSoft), maxLines = 1
        )
    }
}

/** Red Wi-Fi conectada, con una barra de calidad según la señal. Se actualiza cada 5 s mientras se ve. */
@Composable
private fun WifiCard() {
    val context = LocalContext.current
    var status by remember { mutableStateOf<WifiStatus?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            status = withContext(Dispatchers.IO) { runCatching { readWifiStatus(context) }.getOrNull() }
            loaded = true
            delay(5_000)
        }
    }
    val current = status
    val rssi = current?.rssi
    val (quality, color) = when {
        rssi == null -> "" to Palette.InkSoft
        rssi >= -60 -> "Excelente" to Palette.Success
        rssi >= -67 -> "Buena" to Palette.Success
        rssi >= -75 -> "Regular" to Palette.AmberInk
        else -> "Débil" to Palette.Accent
    }
    val barColor = when {
        rssi == null -> Palette.InkSoft
        rssi >= -67 -> Palette.Online
        rssi >= -75 -> Palette.Warm
        else -> Palette.Accent
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().background(SettingsControl, RoundedCornerShape(Radius.Chip + 8.dp)).padding(horizontal = 24.dp, vertical = 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (current == null) Icons.Outlined.WifiOff else Icons.Outlined.Wifi, contentDescription = null,
                tint = if (current == null) Palette.InkSoft else barColor, modifier = Modifier.size(40.dp)
            )
            Text(
                when {
                    current != null -> current.ssid ?: "Conectado"
                    loaded -> "Sin conexión Wi-Fi"
                    else -> "Wi-Fi…"
                },
                style = settingsText(30, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 14.dp, end = 10.dp).weight(1f)
            )
            Text(quality, style = settingsText(26, FontWeight.Bold, color), maxLines = 1)
        }
        if (current != null) {
            // De -90 dBm (sin señal útil) a -50 dBm (llena)
            val level = rssi?.let { ((it + 90) / 40f).coerceIn(0.04f, 1f) } ?: 0f
            Box(Modifier.fillMaxWidth().height(14.dp).background(Palette.Line, CircleShape)) {
                if (level > 0f) Box(Modifier.fillMaxWidth(level).height(14.dp).background(barColor, CircleShape))
            }
            Text(
                listOfNotNull(rssi?.let { "$it dBm" } ?: "Señal desconocida", current.ip).joinToString("   ·   "),
                style = settingsText(24, FontWeight.Medium, Palette.InkSoft), maxLines = 1
            )
        }
    }
}
