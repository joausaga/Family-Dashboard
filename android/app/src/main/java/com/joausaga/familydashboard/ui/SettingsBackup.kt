package com.joausaga.familydashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joausaga.familydashboard.data.backup.BackupSelection
import com.joausaga.familydashboard.data.backup.BackupUiState
import com.joausaga.familydashboard.data.backup.selectionLabel
import com.joausaga.familydashboard.ui.theme.Palette
import com.joausaga.familydashboard.ui.theme.Radius
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun BackupSettingsGroup(state: BackupUiState, callbacks: SettingsCallbacks) {
    SettingsGroup("Copia de seguridad", description = "Historial TV y configuración local, sin credenciales de Google.") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsButton("Exportar copia", Modifier.weight(1f), icon = Icons.Outlined.FileUpload,
                enabled = !state.active, onClick = callbacks.onRequestBackupExport)
            SettingsButton("Importar copia", Modifier.weight(1f), icon = Icons.Outlined.FileDownload,
                enabled = !state.active, onClick = callbacks.onImportBackup)
        }
        Text("Archivo JSON sin contraseña. Al importar podrás elegir qué restaurar y confirmar el reemplazo.", style = settingsText(22, color = Palette.InkSoft))
        state.message?.let { Text(it, style = settingsText(24, FontWeight.Medium, if (state.error) Palette.Accent else Palette.Success)) }
    }
}

@Composable
internal fun BackupDialogs(state: BackupUiState, callbacks: SettingsCallbacks) {
    when {
        state.busy -> BackupModal("Copia de seguridad") {
            Text(state.message ?: "Procesando…", style = settingsText(28))
            Text("Espera a que termine. No se cerrará la sesión Google.", style = settingsText(22, color = Palette.InkSoft))
        }
        state.recoveryRequired -> BackupModal("Recuperación pendiente") {
            Text(state.message ?: "Hay una restauración interrumpida.", style = settingsText(26, color = Palette.Accent))
            Text("El sondeo está detenido para proteger los datos. Se intentará recuperar la configuración y el historial anteriores.", style = settingsText(24, color = Palette.InkSoft))
            SettingsButton("Reintentar recuperación", Modifier.fillMaxWidth(), style = SettingsButtonStyle.Primary, onClick = callbacks.onRetryBackupRecovery)
        }
        state.confirmExport -> BackupModal("Exportar copia de seguridad") {
            Text("El archivo JSON no está cifrado ni protegido con contraseña. Contiene tu historial TV, ubicación, IP de la TV y selecciones por cuenta. Guárdalo en un lugar seguro y no lo publiques.", style = settingsText(26))
            Text("No incluye tokens, contraseñas, credenciales OAuth ni contenido de eventos/tareas. El archivo se comprobará después de guardarlo.", style = settingsText(24, color = Palette.InkSoft))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingsButton("Cancelar", Modifier.weight(1f), onClick = callbacks.onDismissBackup)
                SettingsButton("Elegir destino", Modifier.weight(1f), style = SettingsButtonStyle.Primary, onClick = callbacks.onConfirmBackupExport)
            }
        }
        state.preview != null -> {
            val preview = state.preview
            var history by remember(preview) { mutableStateOf(false) }
            var configuration by remember(preview) { mutableStateOf(false) }
            var confirm by remember(preview) { mutableStateOf(false) }
            val selection = BackupSelection(history, configuration)
            BackupModal(if (confirm) "Confirmar reemplazo" else "Contenido de la copia") {
                Text("Copia del ${DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Spanish).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(preview.createdAtMillis))}", style = settingsText(26, FontWeight.SemiBold))
                if (!confirm) {
                    SettingsSwitchRow("Historial TV", history, description = "${preview.transitions} transiciones. Se reemplazará todo el historial local.", onChange = { history = it })
                    SettingsSwitchRow("Configuración", configuration,
                        description = "Ubicación: ${preview.locationName ?: "sin configurar"}. Preferencias de ${preview.accounts} cuenta(s), horario, brillo, TV y frecuencias.", onChange = { configuration = it })
                    Text("Elige uno o ambos bloques. La sesión Google no se importa ni se borra. Importa solo copias de confianza: SHA-256 detecta corrupción, no autenticidad.", style = settingsText(22, color = Palette.InkSoft))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        SettingsButton("Cancelar", Modifier.weight(1f), onClick = callbacks.onDismissBackup)
                        SettingsButton("Continuar", Modifier.weight(1f), style = SettingsButtonStyle.Primary, enabled = selection.any) { confirm = true }
                    }
                } else {
                    Text("Se reemplazará ${selectionLabel(selection)} con los datos de esta copia. No se mezclarán registros. Los bloques no seleccionados se conservarán.", style = settingsText(28))
                    Text("Esta acción sustituye datos locales. Exporta antes una copia actual si quieres conservarlos. Las preferencias de cuenta solo se aplican a la cuenta correspondiente.", style = settingsText(24, color = Palette.InkSoft))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        SettingsButton("Volver", Modifier.weight(1f)) { confirm = false }
                        SettingsButton("Restaurar selección", Modifier.weight(1f), style = SettingsButtonStyle.DestructiveSolid) { callbacks.onRestoreBackup(selection) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupModal(title: String, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).pointerInput(Unit) { detectTapGestures { } }, contentAlignment = Alignment.Center) {
        val shape = RoundedCornerShape(Radius.Card)
        Column(Modifier.width(980.dp).heightIn(max = 1080.dp).clip(shape).background(Palette.Card)
            .border(1.dp, Palette.Border, shape).verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Text(title, style = settingsText(38, FontWeight.Bold))
            content()
        }
    }
}
