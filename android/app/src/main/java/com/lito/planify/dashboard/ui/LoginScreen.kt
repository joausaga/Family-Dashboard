package com.lito.planify.dashboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lito.planify.dashboard.R
import com.lito.planify.dashboard.UiState
import com.lito.planify.dashboard.ui.theme.Palette
import com.lito.planify.dashboard.ui.theme.Type

/**
 * Conexión a Google. Se solicitan permisos de solo lectura para Calendar y Tasks después de tocar el botón.
 * Sin Internet no se inicia OAuth: se espera a la red, con acceso a los ajustes de Wi-Fi.
 */
@Composable
fun LoginScreen(state: UiState, onLogin: () -> Unit, onWifi: () -> Unit) {
    if (!state.connected) {
        WaitingForNetwork(onWifi)
        return
    }
    Box(
        Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.width(760.dp), verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painterResource(R.drawable.planify_icon),
                contentDescription = "Family Dashboard",
                modifier = Modifier.size(144.dp).padding(bottom = 8.dp)
            )
            Text(
                "Conecta Google Calendar y Tasks",
                style = Type.Huge.copy(fontSize = Type.Title.fontSize * 1.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )
            Text(
                "Family Dashboard solo leerá tus calendarios, eventos recurrentes y tareas pendientes.",
                style = Type.Body,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            state.loginError?.let { Text(it, style = Type.Body.copy(color = Palette.Accent)) }
            PillButton(
                if (state.loggingIn) "Conectando…" else "Continuar con Google",
                Modifier.fillMaxWidth(),
                primary = true,
                enabled = !state.loggingIn
            ) {
                onLogin()
            }
        }
    }
}

/**
 * Arco que gira una vez por segundo incluso si las animaciones del sistema están desactivadas:
 * el giro va con el reloj de fotogramas.
 */
@Composable
private fun Spinner() {
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) withFrameMillis { angle = (it % 1000L) * 0.36f }
    }
    Canvas(Modifier.size(96.dp).graphicsLayer { rotationZ = angle }) {
        val stroke = 8.dp.toPx()
        val inset = stroke / 2
        val arc = Size(size.width - stroke, size.height - stroke)
        drawArc(Palette.Line, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
        drawArc(Palette.Ink, -90f, 110f, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun WaitingForNetwork(onWifi: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(40.dp)) {
            Spinner()
            Text("Esperando conexión a Internet…", style = Type.Title, textAlign = TextAlign.Center)
            PillButton("Ir a Ajustes", Modifier.width(520.dp), onClick = onWifi)
        }
    }
}
