package com.joausaga.familydashboard.admin

import androidx.core.content.ContextCompat
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.joausaga.familydashboard.MainActivity
import com.joausaga.familydashboard.R

/**
 * Botón flotante con el icono de inicio encima de Ajustes de Android, para volver al dashboard.
 * El marco no tiene barra de navegación y, en el lock task, Ajustes es la única otra app permitida:
 * sin este botón, desde la portada de Ajustes (que no tiene flecha atrás) no hay forma de volver.
 * Vuelve solo tras [IDLE_MS] sin tocar la pantalla: la superposición recibe un aviso con cada toque
 * fuera de ella (FLAG_WATCH_OUTSIDE_TOUCH), también en el teclado, y reinicia la cuenta.
 * Necesita el permiso de superposición (`SYSTEM_ALERT_WINDOW`) concedido en el dispositivo;
 * sin él no se muestra nada.
 */
// El botón se crea con el contexto de la aplicación y se suelta en hide(): no retiene ninguna actividad.
@SuppressLint("StaticFieldLeak")
object ReturnButton {
    private const val IDLE_MS = 60_000L
    private const val SIZE = 120

    private var view: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timeout: Runnable? = null

    @SuppressLint("ClickableViewAccessibility")
    fun show(context: Context) {
        val app = context.applicationContext
        val canDrawOverlays = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(app)
        if (view != null || !canDrawOverlays) return
        // Medidas en píxeles del panel, como el resto de la app
        val button = ImageView(app).apply {
            setImageResource(R.drawable.ic_home)
            contentDescription = "Volver al dashboard"
            setPadding(30, 30, 30, 30)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF221D18.toInt())
            }
            elevation = 12f
            setOnClickListener { backToDashboard(app) }
            setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) restartIdle(app)
                false
            }
        }
        val params = WindowManager.LayoutParams(
            SIZE,
            SIZE,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 40
            y = 40
        }
        runCatching { ContextCompat.getSystemService(app, WindowManager::class.java)!!.addView(button, params) }
            .onSuccess {
                view = button
                restartIdle(app)
            }
    }

    /** Lo quita (se llama cada vez que el dashboard vuelve al frente, se vuelva como se vuelva). */
    fun hide(context: Context) {
        timeout?.let(handler::removeCallbacks)
        timeout = null
        val current = view ?: return
        view = null
        runCatching { ContextCompat.getSystemService(context.applicationContext, WindowManager::class.java)!!.removeView(current) }
    }

    private fun restartIdle(context: Context) {
        timeout?.let(handler::removeCallbacks)
        timeout = Runnable { backToDashboard(context) }.also { handler.postDelayed(it, IDLE_MS) }
    }

    private fun backToDashboard(context: Context) {
        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
        hide(context)
    }
}
