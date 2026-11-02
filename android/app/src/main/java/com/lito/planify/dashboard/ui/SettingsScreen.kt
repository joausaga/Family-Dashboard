package com.lito.planify.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.material.icons.outlined.ViewAgenda
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lito.planify.dashboard.UiState
import com.lito.planify.dashboard.data.GeoResult
import com.lito.planify.dashboard.data.Schedule
import com.lito.planify.dashboard.ui.theme.Palette
import com.lito.planify.dashboard.ui.theme.Radius

class SettingsCallbacks(
    val onSearch: (String) -> Unit,
    val onSelectLocation: (GeoResult) -> Unit,
    val onSchedule: (Schedule) -> Unit,
    val onBrightness: (Float?) -> Unit,
    val onLogout: () -> Unit,
    val onCalendarVisibilityChange: (String, Boolean) -> Unit,
    val onRefreshCalendarSettings: () -> Unit,
    val onTaskListVisibilityChange: (String, Boolean) -> Unit,
    val onRefreshTaskListSettings: () -> Unit,
    val onSaveTvMonitorSettings: (String, Int) -> Unit,
    val onCalendarTaskRefreshInterval: (Int) -> Unit
)

private enum class SettingsDestination(val title: String, val icon: ImageVector) {
    Content("Contenido", Icons.Outlined.ViewAgenda),
    Screen("Pantalla", Icons.Outlined.Brightness6),
    Services("Servicios", Icons.Outlined.Cloud),
    Device("Dispositivo", Icons.Outlined.TabletAndroid)
}

/** Índice contextual fijo y un único panel de detalle desplazable; «Cerrar sesión» pide confirmación. */
@Composable
fun SettingsScreen(state: UiState, actions: DashboardActions, callbacks: SettingsCallbacks) {
    var destination by remember { mutableStateOf(SettingsDestination.Content) }
    var confirmLogout by remember { mutableStateOf(false) }
    var timePick by remember { mutableStateOf<TimePickRequest?>(null) }
    Box(Modifier.fillMaxSize()) {
        ScreenPadding {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
                SettingsDestinationIndex(state, destination, { destination = it }, Modifier.width(320.dp).fillMaxHeight())
                SettingsPanel(Modifier.weight(1f).fillMaxHeight()) {
                    when (destination) {
                        SettingsDestination.Content -> ContentSection(state, callbacks)
                        SettingsDestination.Screen -> DisplaySection(state, callbacks) { timePick = it }
                        SettingsDestination.Services -> ServicesSection(state, callbacks)
                        SettingsDestination.Device -> DeviceSection(state, actions) { confirmLogout = true }
                    }
                }
            }
        }
        timePick?.let { request -> TimePickDialog(request, onDismiss = { timePick = null }) }
        if (confirmLogout) {
            LogoutConfirmation(onCancel = { confirmLogout = false }, onConfirm = { confirmLogout = false; callbacks.onLogout() })
        }
    }
}

@Composable
private fun SettingsDestinationIndex(
    state: UiState,
    selected: SettingsDestination,
    onSelect: (SettingsDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val contentSummary = "${state.calendars.count { it.id in state.visibleCalendarIds }}/${state.calendars.size} calendarios · " +
        "${state.taskLists.count { it.id in state.visibleTaskListIds }}/${state.taskLists.size} listas"
    val screenSummary = (if (state.schedule.enabled) "Horario activo" else "Horario inactivo") + " · " +
        if (state.brightness == null) "brillo automático" else "brillo manual"
    val servicesSummary = "Cada ${state.calendarTaskRefreshMinutes} min · " +
        if (state.tvMonitorSettings.isConfigured) "TV configurada" else "TV sin configurar"
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsDestination.entries.forEach { destination ->
            val summary = when (destination) {
                SettingsDestination.Content -> contentSummary
                SettingsDestination.Screen -> screenSummary
                SettingsDestination.Services -> servicesSummary
                SettingsDestination.Device -> "Cuenta, estado y red"
            }
            val isSelected = destination == selected
            val shape = RoundedCornerShape(Radius.Card)
            Row(
                Modifier.fillMaxWidth().clip(shape)
                    .background(if (isSelected) Palette.Card else Color.Transparent)
                    .then(if (isSelected) Modifier.border(1.dp, Palette.Border, shape) else Modifier)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${destination.title}. $summary"
                        stateDescription = if (isSelected) "Sección seleccionada" else ""
                    }
                    .clickable(role = Role.Button) { onSelect(destination) }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(destination.icon, contentDescription = null, tint = if (isSelected) Palette.Ink else Palette.InkSoft, modifier = Modifier.size(40.dp))
                Column(Modifier.padding(start = 16.dp)) {
                    Text(destination.title, style = settingsText(28, if (isSelected) FontWeight.Bold else FontWeight.Medium), maxLines = 1)
                    Text(summary, style = settingsText(20, FontWeight.Normal, Palette.InkSoft), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Confirmación propia (sin ventana del sistema, que usa otra densidad); no se cierra al tocar fuera. */
@Composable
private fun LogoutConfirmation(onCancel: () -> Unit, onConfirm: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        val shape = RoundedCornerShape(Radius.Card)
        Column(
            Modifier.width(780.dp).clip(shape).background(Palette.Card).border(1.dp, Palette.Border, shape).padding(36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = Palette.Accent, modifier = Modifier.size(52.dp))
                Text("¿Cerrar sesión?", style = settingsText(40, FontWeight.Bold))
            }
            Text(
                "Se revocará el acceso de este panel a tu cuenta de Google y se borrarán los datos guardados. Tendrás que iniciar sesión de nuevo.",
                style = settingsText(26, FontWeight.Normal, Palette.InkSoft)
            )
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingsButton("Cancelar", Modifier.weight(1f), onClick = onCancel)
                SettingsButton("Cerrar sesión", Modifier.weight(1f), style = SettingsButtonStyle.DestructiveSolid, onClick = onConfirm)
            }
        }
    }
}
