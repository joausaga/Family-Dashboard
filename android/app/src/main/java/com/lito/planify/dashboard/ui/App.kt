package com.lito.planify.dashboard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.lito.planify.dashboard.ui.theme.Radius
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lito.planify.dashboard.UiState
import com.lito.planify.dashboard.ui.theme.Palette
import com.lito.planify.dashboard.ui.theme.Type
import java.time.LocalDate
import kotlinx.coroutines.delay

enum class Page(val label: String, val icon: ImageVector) {
    Today("Hoy", Icons.Outlined.Home),
    Calendar("Calendario", Icons.Outlined.CalendarMonth),
    Weather("Tiempo", Icons.Outlined.Thermostat),
    Tasks("Tareas", Icons.Outlined.Checklist),
    Tv("TV", Icons.Outlined.Tv),
    Settings("Ajustes", Icons.Outlined.Settings)
}

/** Acciones que dependen de la actividad (pantalla, quiosco, ajustes de Android). */
class DashboardActions(
    val sleep: () -> Unit,
    val openWifi: () -> Unit,
    val openSettings: () -> Unit,
    val powerMenu: () -> Unit,
)

private const val IDLE_MILLIS = 120_000L

@Composable
fun DashboardApp(
    state: UiState,
    actions: DashboardActions,
    onLogin: () -> Unit,
    onLoadWeek: (LocalDate) -> Unit,
    onSync: () -> Unit,
    settings: SettingsCallbacks,
    onPeek: () -> Unit
) {
    var page by remember { mutableStateOf(Page.Today) }
    var lastTouch by remember { mutableLongStateOf(0L) }

    // Tras 2 minutos sin tocar se vuelve a Hoy.
    LaunchedEffect(page, lastTouch) {
        if (page == Page.Today) return@LaunchedEffect
        delay(IDLE_MILLIS)
        page = Page.Today
    }
    // Es el launcher: "atrás" vuelve a Hoy y nunca cierra la actividad.
    BackHandler { page = Page.Today }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        lastTouch = System.currentTimeMillis()
                    }
                }
            }
    ) {
        if (state.token == null) {
            LoginScreen(state, onLogin, actions.openWifi)
            return@Box
        }
        if (state.night) {
            NightScreen(state, onWake = {
                page = Page.Today
                onPeek()
            })
            return@Box
        }
        Row(Modifier.fillMaxSize()) {
            Rail(page, state, onSelect = { page = it }, onSync = onSync, onSleep = actions.sleep)
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when (page) {
                    Page.Today -> TodayScreen(state, onLoadWeek) { page = Page.Settings }
                    Page.Calendar -> CalendarScreen(state, onLoadWeek)
                    Page.Weather -> WeatherScreen(state)
                    Page.Tasks -> TasksScreen(state)
                    Page.Tv -> TvUsageScreen(state.tvUsage, state.tvTime) { page = Page.Settings }
                    Page.Settings -> SettingsScreen(state, actions, settings)
                }
            }
        }
    }
}

@Composable
private fun Rail(current: Page, state: UiState, onSelect: (Page) -> Unit, onSync: () -> Unit, onSleep: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(136.dp)
            .fillMaxHeight()
            .background(Palette.Rail)
            .padding(vertical = 32.dp)
    ) {
        // Botones arriba; estado de conexión y sincronización abajo, en todas las pantallas.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Page.entries.filter { it != Page.Settings }.forEach { page -> RailItem(page.label, page.icon, page == current) { onSelect(page) } }
            SyncItem(state.syncing, onSync)
            RailItem(Page.Settings.label, Page.Settings.icon, current == Page.Settings) { onSelect(Page.Settings) }
            RailItem("Apagar pantalla", Icons.Outlined.Bedtime, false, caption = "Apagar", onClick = onSleep)
        }
        Spacer(Modifier.weight(1f))
        RailStatus(state)
    }
}

/** Estado de conexión y sincronización: punto de color y texto corto. */
@Composable
private fun RailStatus(state: UiState) {
    val (color, text) = when {
        !state.connected -> Palette.Accent to "Sin conexión"
        state.syncError -> Palette.Accent to "Error de sync"
        state.syncing -> Palette.Warm to "Sincroniza"
        else -> Palette.Online to "Conectado"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
        Box(Modifier.size(16.dp).background(color, CircleShape))
        Text(
            text,
            modifier = Modifier.padding(top = 8.dp),
            style = Type.Tiny.copy(color = Palette.InkSoft, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/**
 * Sincronizar ahora. Gira mientras dura; el giro va con el reloj de fotogramas porque el marco tiene las animaciones
 * del sistema a 0 y Compose las respeta.
 */
@Composable
private fun SyncItem(syncing: Boolean, onSync: () -> Unit) {
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(syncing) {
        angle = 0f
        if (!syncing) return@LaunchedEffect
        val start = withFrameMillis { it }
        // Hacia la izquierda, como indica la flecha del icono
        while (true) withFrameMillis { angle = -((it - start) % 1000L) * 0.36f }
    }
    RailItem("Sincronizar", Icons.Outlined.Sync, syncing, rotation = angle, caption = "Sincronizar") { onSync() }
}

/** Botón de la barra lateral: icono con etiqueta corta, relleno al seleccionar y respuesta visual al pulsar. */
@Composable
private fun RailItem(label: String, icon: ImageVector, selected: Boolean, rotation: Float = 0f, caption: String = label, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape(Radius.Card)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(120.dp)
            .height(112.dp)
            .scale(if (pressed) 0.95f else 1f)
            .background(
                when {
                    pressed -> Palette.Card.copy(alpha = 0.7f)
                    selected -> Palette.Card
                    else -> Palette.Rail
                },
                shape
            )
            .clickable(interactionSource = source, indication = null, onClick = onClick)
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) Palette.Ink else Palette.InkSoft, modifier = Modifier.size(48.dp).rotate(rotation))
        Text(
            caption,
            modifier = Modifier.padding(top = 4.dp),
            style = Type.Tiny.copy(
                color = if (selected) Palette.Ink else Palette.InkSoft,
                fontSize = 20.sp, lineHeight = 24.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            maxLines = 1
        )
    }
}

/** Botón en píldora. */
@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    background: Color = Palette.Card,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(84.dp)
            // Recortado a su forma, para que el efecto al pulsar no salga cuadrado
            .clip(RoundedCornerShape(42.dp))
            .background(
                when {
                    !enabled -> Palette.Line
                    primary -> Palette.Ink
                    else -> background
                },
                RoundedCornerShape(42.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 40.dp)
    ) {
        Text(text, style = Type.Body.copy(color = if (!enabled) Palette.InkSoft else if (primary) Palette.Card else Palette.Ink), maxLines = 1)
    }
}

@Composable
fun ScreenPadding(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(start = 64.dp, end = 64.dp, top = 60.dp, bottom = 52.dp)) { content() }
}
