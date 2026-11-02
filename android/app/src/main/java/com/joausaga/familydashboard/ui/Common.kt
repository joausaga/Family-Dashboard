package com.joausaga.familydashboard.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.joausaga.familydashboard.data.Person
import com.joausaga.familydashboard.ui.theme.Palette
import com.joausaga.familydashboard.ui.theme.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val Spanish: Locale = Locale.forLanguageTag("es-ES")
val HourMinute: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun parseColor(hex: String?, fallback: Color = Palette.InkMuted): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)

fun String.capitalized() = replaceFirstChar { it.titlecase(Spanish) }

fun LocalDate.weekdayName(style: TextStyle = TextStyle.FULL) = dayOfWeek.getDisplayName(style, Spanish).capitalized()

/** Los 7 días desde [from]: "13 – 19 de octubre" o "29 de septiembre – 5 de octubre" (con el año si no es el actual). */
fun weekRange(from: LocalDate): String {
    val to = from.plusDays(6)
    fun month(date: LocalDate) = date.month.getDisplayName(TextStyle.FULL, Spanish)
    val year = if (to.year != LocalDate.now().year) " de ${to.year}" else ""
    return if (from.month == to.month) "${from.dayOfMonth} – ${to.dayOfMonth} de ${month(to)}$year"
    else "${from.dayOfMonth} de ${month(from)} – ${to.dayOfMonth} de ${month(to)}$year"
}

/** "Hoy", "Mañana" o "Jue 16". */
fun relativeDay(date: LocalDate, today: LocalDate) = when (date) {
    today -> "Hoy"
    today.plusDays(1) -> "Mañana"
    else -> "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Spanish).capitalized().trimEnd('.')} ${date.dayOfMonth}"
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(Spanish), style = Type.Label, maxLines = 1, modifier = modifier)
}

/**
 * Desplazamiento vertical con los bordes desvanecidos: abajo mientras queda contenido por ver y arriba en
 * cuanto se ha desplazado, para que el texto no se corte en seco contra el borde de la tarjeta.
 */
fun Modifier.fadingScroll(state: ScrollState, edge: Dp = 56.dp): Modifier = this
    // La capa propia permite que DstIn recorte solo el contenido de la lista
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fade = edge.toPx().coerceAtMost(size.height / 3)
        if (state.canScrollForward) {
            drawRect(
                Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = size.height - fade, endY = size.height),
                topLeft = Offset(0f, size.height - fade),
                size = Size(size.width, fade),
                blendMode = BlendMode.DstIn
            )
        }
        if (state.canScrollBackward) {
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = fade),
                size = Size(size.width, fade),
                blendMode = BlendMode.DstIn
            )
        }
    }
    .verticalScroll(state)

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(2.dp).background(Palette.Line))
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .background(Palette.Card, RoundedCornerShape(36.dp))
            .padding(horizontal = 40.dp, vertical = 34.dp),
        content = content
    )
}

@Composable
fun Dot(color: Color, size: Int = 14) {
    Box(Modifier.size(size.dp).background(color, CircleShape))
}

/** Iniciales en círculos del color de cada persona: para filas donde no caben los nombres. */
@Composable
fun PersonInitials(persons: List<Person>?, modifier: Modifier = Modifier, size: Int = 36) {
    if (persons.isNullOrEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp), modifier = modifier) {
        persons.forEach { person ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size.dp)
                    .border(3.dp, Palette.Background, CircleShape)
                    .background(parseColor(person.color), CircleShape)
            ) {
                Text(person.name.take(1).uppercase(Spanish), style = Type.Tiny.copy(color = Color.White, fontSize = Type.Tiny.fontSize * (size / 40f)))
            }
        }
    }
}
