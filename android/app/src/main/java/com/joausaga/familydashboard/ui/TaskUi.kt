package com.joausaga.familydashboard.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.ui.graphics.Color
import com.joausaga.familydashboard.data.taskDueDate
import com.joausaga.familydashboard.ui.theme.Palette
import java.time.LocalDate
import java.time.format.TextStyle

// Reglas de urgencia, iconos y etiquetas de vencimiento compartidas por la tarjeta de Tareas de Inicio y la pantalla Tareas.

internal enum class DueTone { Overdue, Soon, Later, None }

internal fun dueTone(due: String?, today: LocalDate): DueTone {
    val date = taskDueDate(due) ?: return DueTone.None
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
    return when {
        days <= 0 -> DueTone.Overdue
        days <= 2 -> DueTone.Soon
        else -> DueTone.Later
    }
}

/** Icono de mejor esfuerzo según palabras clave del título; si nada coincide, un icono neutro. */
internal fun taskIcon(title: String): androidx.compose.ui.graphics.vector.ImageVector {
    val t = java.text.Normalizer.normalize(title.lowercase(Spanish), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
    fun has(vararg k: String) = k.any { t.contains(it) }
    return when {
        has("medic", "dentist", "doctor", "pediatr", "vacuna", "farmacia") -> Icons.Outlined.MedicalServices
        has("llamar", "telefon", "whatsapp") -> Icons.Outlined.Call
        has("pagar", "factura", "banco", "impuesto", "cuota", "recibo") -> Icons.Outlined.CreditCard
        has("comprar", "super", "mercado", "compra") -> Icons.Outlined.ShoppingCart
        has("consulado", "tramite", "document", "pasaporte", "renovar", "formulario", "firmar") -> Icons.Outlined.Description
        has("cita", "turno", "reserv") -> Icons.Outlined.CalendarMonth
        has("reunion", "clase", "colegio", "escuela") -> Icons.Outlined.Groups
        has("enviar", "email", "correo", "mail") -> Icons.Outlined.Mail
        else -> Icons.Outlined.TaskAlt
    }
}

internal fun dueColors(tone: DueTone): Pair<Color, Color> = when (tone) {
    DueTone.Overdue -> Palette.Accent to Palette.AccentSoft
    DueTone.Soon -> Palette.AmberInk to Palette.AmberSoft
    DueTone.Later -> Palette.Success to Palette.SuccessSoft
    DueTone.None -> Palette.InkSoft to Palette.Highlight
}

/** Cuenta atrás corta para la tarea protagonista. */
internal fun taskCountdown(due: String?, today: LocalDate): String {
    val date = taskDueDate(due) ?: return "Sin fecha"
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
    return when {
        days < 0 -> "Vencida"
        days == 0L -> "Hoy"
        days == 1L -> "Mañana"
        days <= 30 -> "$days días"
        else -> taskDueLabel(due, today)
    }
}

internal fun taskDueLabel(due: String?, today: LocalDate): String {
    val date = taskDueDate(due) ?: return "Sin fecha"
    return when {
        date.isBefore(today) -> "Vencida"
        date == today -> "Hoy"
        date == today.plusDays(1) -> "Mañana"
        else -> {
            val month = date.month.getDisplayName(TextStyle.SHORT, Spanish).trimEnd('.').replaceFirstChar { it.titlecase(Spanish) }
            "${date.dayOfMonth} $month"
        }
    }
}

/** Frase del vencimiento para la tarea expandida: «Vence el lunes 2 de noviembre, en 3 días». */
internal fun taskDueSentence(due: String?, today: LocalDate): String {
    val date = taskDueDate(due) ?: return "Sin fecha de vencimiento"
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
    val label = "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Spanish)} ${date.dayOfMonth} de ${date.month.getDisplayName(TextStyle.FULL, Spanish)}".lowercase(Spanish)
    return when {
        days < -1 -> "Venció el $label, hace ${-days} días"
        days == -1L -> "Venció ayer, el $label"
        days == 0L -> "Vence hoy, $label"
        days == 1L -> "Vence mañana, $label"
        else -> "Vence el $label, en $days días"
    }
}
