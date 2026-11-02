package com.joausaga.familydashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.Typography
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.joausaga.familydashboard.ui.theme.Geist
import com.joausaga.familydashboard.ui.theme.Palette
import com.joausaga.familydashboard.ui.theme.Radius

/** Petición de elegir una hora: título, hora inicial, validación (mensaje de error o null) y qué hacer al aceptar. */
internal class TimePickRequest(
    val title: String,
    val initialMinutes: Int,
    val validate: (Int) -> String?,
    val onConfirm: (Int) -> Unit
)

/** Escala del selector respecto al tamaño de Material, para un panel táctil de este tamaño. */
private const val DIAL_SCALE = 1.7f

/**
 * Selector de hora de reloj de esfera (como el de Google Calendar), en 12 horas con AM/PM. Se muestra como un panel dentro de
 * la composición con un velo, igual que la confirmación de cierre de sesión; no se cierra al tocar fuera.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickDialog(request: TimePickRequest, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(request.initialMinutes / 60, request.initialMinutes % 60, is24Hour = false)
    val minutes = state.hour * 60 + state.minute
    val error = request.validate(minutes)
    val base = LocalDensity.current
    val shape = RoundedCornerShape(Radius.Card)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.width(660.dp).clip(shape).background(Palette.Card).border(1.dp, Palette.Border, shape).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(request.title, style = settingsText(34, FontWeight.Bold), modifier = Modifier.fillMaxWidth())
            MaterialTheme(typography = geistTypography()) {
                // Material dimensiona en dp de móvil: se amplía solo el selector, no el resto del panel
                CompositionLocalProvider(LocalDensity provides Density(base.density * DIAL_SCALE, base.fontScale)) {
                    TimePicker(
                        state = state,
                        layoutType = TimePickerLayoutType.Vertical,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = Palette.Highlight,
                            clockDialSelectedContentColor = Palette.Card,
                            clockDialUnselectedContentColor = Palette.Ink,
                            selectorColor = Palette.Ink,
                            timeSelectorSelectedContainerColor = Palette.Ink,
                            // El campo activo destaca; el otro queda sin recuadro y atenuado, para que no compitan
                            timeSelectorUnselectedContainerColor = Color.Transparent,
                            timeSelectorSelectedContentColor = Palette.Card,
                            timeSelectorUnselectedContentColor = Palette.SwitchOff,
                            periodSelectorBorderColor = Palette.Line,
                            periodSelectorSelectedContainerColor = Palette.Ink,
                            periodSelectorUnselectedContainerColor = Color.Transparent,
                            periodSelectorSelectedContentColor = Palette.Card,
                            periodSelectorUnselectedContentColor = Palette.InkSoft,
                            containerColor = Palette.Card
                        )
                    )
                }
            }
            Text(
                error ?: "Toca la hora y los minutos, o arrastra la aguja del reloj.",
                style = settingsText(22, if (error != null) FontWeight.SemiBold else FontWeight.Normal, if (error != null) Palette.Accent else Palette.InkSoft),
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingsButton("Cancelar", Modifier.weight(1f), onClick = onDismiss)
                SettingsButton("Aceptar", Modifier.weight(1f), style = SettingsButtonStyle.Primary, enabled = error == null) {
                    request.onConfirm(minutes)
                    onDismiss()
                }
            }
        }
    }
}

/** Tipografía de Material con Geist (el panel no usa `MaterialTheme`, así que sus textos saldrían en Roboto). */
private fun geistTypography(): Typography {
    val base = Typography()
    fun androidx.compose.ui.text.TextStyle.geist() = copy(fontFamily = Geist, fontFeatureSettings = "tnum")
    return base.copy(
        displayLarge = base.displayLarge.geist(), displayMedium = base.displayMedium.geist(), displaySmall = base.displaySmall.geist(),
        headlineLarge = base.headlineLarge.geist(), headlineMedium = base.headlineMedium.geist(), headlineSmall = base.headlineSmall.geist(),
        titleLarge = base.titleLarge.geist(), titleMedium = base.titleMedium.geist(), titleSmall = base.titleSmall.geist(),
        bodyLarge = base.bodyLarge.geist(), bodyMedium = base.bodyMedium.geist(), bodySmall = base.bodySmall.geist(),
        labelLarge = base.labelLarge.geist(), labelMedium = base.labelMedium.geist(), labelSmall = base.labelSmall.geist()
    )
}
