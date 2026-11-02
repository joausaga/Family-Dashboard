package com.lito.planify.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lito.planify.dashboard.ui.theme.Palette
import com.lito.planify.dashboard.ui.theme.Radius
import com.lito.planify.dashboard.ui.theme.Type

internal val SettingsControl = Palette.Background

internal fun settingsText(size: Int, weight: FontWeight = FontWeight.Medium, color: Color = Palette.Ink) =
    Type.Tiny.copy(fontSize = size.sp, lineHeight = (size * 1.25f).sp, fontWeight = weight, color = color, fontFeatureSettings = "tnum")

internal enum class SettingsButtonStyle { Primary, Secondary, Destructive, DestructiveSolid }

/** Botón de Ajustes: primario (oscuro), secundario (neutro) o destructivo (contorno rojo, o relleno rojo en el diálogo). */
@Composable
internal fun SettingsButton(
    label: String,
    modifier: Modifier = Modifier,
    style: SettingsButtonStyle = SettingsButtonStyle.Secondary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(Radius.Pill + 18.dp)
    val (bg, fg) = when {
        !enabled -> Palette.Line to Palette.InkSoft
        style == SettingsButtonStyle.Primary -> Palette.Ink to Palette.Card
        style == SettingsButtonStyle.Secondary -> SettingsControl to Palette.Ink
        style == SettingsButtonStyle.Destructive -> Palette.Card to Palette.Accent
        else -> Palette.Accent to Color.White
    }
    Row(
        modifier.height(84.dp).clip(shape).background(bg)
            .then(if (enabled && style == SettingsButtonStyle.Destructive) Modifier.border(2.dp, Palette.Accent, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(28.dp))
        Text(label, style = settingsText(26, FontWeight.SemiBold, fg), maxLines = 1, modifier = Modifier.padding(start = if (icon != null) 10.dp else 0.dp))
    }
}

/** Panel de detalle: una sola hoja con desplazamiento propio y grupos separados por líneas finas. */
@Composable
internal fun SettingsPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(Radius.Card)
    Box(modifier.clip(shape).background(Palette.Card).border(1.dp, Palette.Border, shape)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 36.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
            content = content
        )
    }
}

@Composable
internal fun SettingsDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Divider))
}

@Composable
internal fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = settingsText(32, FontWeight.SemiBold), maxLines = 1)
                if (description != null) Text(description, style = settingsText(22, FontWeight.Normal, Palette.InkSoft))
            }
            trailing()
        }
        content()
    }
}

@Composable
internal fun SettingsCountPill(text: String) {
    Text(
        text, style = settingsText(22, FontWeight.Bold, Palette.InkSoft), maxLines = 1,
        modifier = Modifier.background(Palette.Highlight, RoundedCornerShape(Radius.Pill)).padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

/** «Guardado» con una marca, en verde; el llamador decide cuánto tiempo se muestra. */
@Composable
internal fun SavedBadge() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = Palette.Success, modifier = Modifier.size(26.dp))
        Text("Guardado", style = settingsText(24, FontWeight.Bold, Palette.Success), maxLines = 1)
    }
}

/** Interruptor de 80 × 44 dp: verde si está activo, neutro con suficiente contraste si no. */
@Composable
internal fun SwitchVisual(checked: Boolean, enabled: Boolean = true) {
    Box(
        Modifier.size(width = 80.dp, height = 44.dp).alpha(if (enabled) 1f else 0.4f)
            .background(if (checked) Palette.Success else Palette.SwitchOff, CircleShape)
            .padding(5.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(Modifier.size(34.dp).background(Palette.Card, CircleShape))
    }
}

/** Fila con interruptor: toda la fila es tocable; admite un elemento inicial y una descripción. */
@Composable
internal fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    description: String? = null,
    stateOn: String = "Activado",
    stateOff: String = "Desactivado",
    dimWhenOff: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier.fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = label
                stateDescription = if (checked) stateOn else stateOff
            }
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Box(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                label,
                style = settingsText(28, FontWeight.Medium, if (dimWhenOff && !checked) Palette.InkSoft else Palette.Ink),
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (description != null) Text(description, style = settingsText(22, FontWeight.Normal, Palette.InkSoft))
        }
        SwitchVisual(checked)
    }
}

@Composable
internal fun MinuteDropdown(
    selected: Int,
    options: List<Int>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val contentDensity = LocalDensity.current
    Box(modifier) {
        Row(
            Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(24.dp)).background(SettingsControl)
                .clickable(role = Role.Button) { expanded = true }.padding(start = 24.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(selected.minuteLabel(), style = settingsText(28, FontWeight.Medium), modifier = Modifier.weight(1f), maxLines = 1)
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Palette.InkSoft, modifier = Modifier.size(40.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CompositionLocalProvider(LocalDensity provides contentDensity) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.minuteLabel(), style = Type.Small.copy(color = Palette.Ink)) },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                        modifier = Modifier.height(56.dp)
                    )
                }
            }
        }
    }
}

internal fun Int.minuteLabel(): String = "$this ${if (this == 1) "minuto" else "minutos"}"

/** Campo de texto a tamaño de este panel (los de Material están pensados para dp de móvil). */
@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    background: Color = Palette.Card,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onAction: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = Type.Body,
        cursorBrush = SolidColor(Palette.Ink),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType, imeAction = imeAction),
        // Solo las acciones que usan los campos: "Siguiente" pasa al campo de abajo; el resto ejecuta onAction.
        // (KeyboardActions { … } sustituiría también "Siguiente" y el foco se quedaría atascado.)
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
            onDone = {
                focusManager.clearFocus()
                onAction()
            },
            onSearch = { onAction() }
        ),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .height(84.dp)
                    .background(background, RoundedCornerShape(24.dp))
                    .border(2.dp, Palette.CardBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 30.dp)
            ) {
                if (value.isEmpty()) Text(placeholder, style = Type.Body.copy(color = Palette.InkSoft))
                inner()
            }
        }
    )
}
