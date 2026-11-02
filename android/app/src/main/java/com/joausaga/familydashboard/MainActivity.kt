package com.joausaga.familydashboard

import android.content.Intent
import android.content.ActivityNotFoundException
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.joausaga.familydashboard.admin.Kiosk
import com.joausaga.familydashboard.admin.PowerMenuService
import com.joausaga.familydashboard.admin.ReturnButton
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.joausaga.familydashboard.power.ScreenSchedule
import com.joausaga.familydashboard.ui.DashboardActions
import com.joausaga.familydashboard.ui.DashboardApp
import com.joausaga.familydashboard.ui.ProvideIconClock
import com.joausaga.familydashboard.ui.SettingsCallbacks
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: DashboardViewModel by viewModels()
    private var authorizationInFlight = false
    private val backupExportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        viewModel.backupOperations.exportTo(uri?.toString())
    }
    private val backupImportLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.backupOperations.importFrom(uri?.toString())
    }
    private val authorizationLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        authorizationInFlight = false
        if (result.resultCode != RESULT_OK || result.data == null) {
            viewModel.googleAuthorizationFailed("No se completó la autorización de Google")
            return@registerForActivityResult
        }
        runCatching {
            Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(result.data!!).accessToken
        }.onSuccess(viewModel::onGoogleAuthorized)
            .onFailure { viewModel.googleAuthorizationFailed("No se pudo completar la autorización de Google") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La alarma de encendido trae la actividad al frente con la pantalla apagada.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        val actions = DashboardActions(
            // La luna: con el reloj nocturno activo pasa a él; si no, apaga la pantalla
            sleep = {
                if (!viewModel.nightNow() && !Kiosk.sleep(this)) {
                    Toast.makeText(this, "Apagar requiere el modo quiosco (Device Owner)", Toast.LENGTH_LONG).show()
                }
            },
            openWifi = { openSystemSettings(Settings.ACTION_WIFI_SETTINGS) },
            openSettings = { openSystemSettings(Settings.ACTION_SETTINGS) },
            powerMenu = { if (!PowerMenuService.open()) Toast.makeText(this, "Activa el servicio de apagado en los ajustes de accesibilidad de Android", Toast.LENGTH_LONG).show() }
        )
        val settings = SettingsCallbacks(
            onSearch = viewModel::searchLocation,
            onSelectLocation = viewModel::selectLocation,
            onSchedule = viewModel::setSchedule,
            onBrightness = viewModel::setBrightness,
            onLogout = viewModel::logout,
            onCalendarVisibilityChange = viewModel::setCalendarVisible,
            onRefreshCalendarSettings = viewModel::refreshCalendarSettings,
            onTaskListVisibilityChange = viewModel::setTaskListVisible,
            onRefreshTaskListSettings = viewModel::refreshTaskListSettings,
            onSaveTvMonitorSettings = viewModel::saveTvMonitorSettings,
            onCalendarTaskRefreshInterval = viewModel::setCalendarTaskRefreshMinutes,
            onRequestBackupExport = viewModel.backupOperations::requestExport,
            onConfirmBackupExport = {
                viewModel.backupOperations.prepareExport { filename ->
                    try { backupExportLauncher.launch(filename) }
                    catch (unavailable: ActivityNotFoundException) { viewModel.backupOperations.pickerFailed() }
                    catch (denied: SecurityException) { viewModel.backupOperations.pickerFailed() }
                }
            },
            onImportBackup = {
                if (viewModel.backupOperations.beginImport()) {
                    try { backupImportLauncher.launch(arrayOf("*/*")) }
                    catch (unavailable: ActivityNotFoundException) { viewModel.backupOperations.pickerFailed() }
                    catch (denied: SecurityException) { viewModel.backupOperations.pickerFailed() }
                }
            },
            onDismissBackup = viewModel.backupOperations::dismiss,
            onRestoreBackup = viewModel.backupOperations::restoreSelected,
            onRetryBackupRecovery = viewModel.backupOperations::retryRecovery
        )

        val metrics = resources.displayMetrics
        val uiScale = dashboardDesignScale(metrics.widthPixels, metrics.heightPixels)
        val systemFontScale = resources.configuration.fontScale
        // La escala física de texto en la Fire queda cerca del 90 % del tamaño de diseño.
        val readableFontScale = maxOf(systemFontScale, if (uiScale < 1f) 1.35f else 1f)
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            // Escalar la geometría al panel sin aplicar esa misma reducción agresiva al texto.
            CompositionLocalProvider(LocalDensity provides Density(density = uiScale, fontScale = readableFontScale)) {
                ProvideIconClock {
                DashboardApp(state, actions, ::startGoogleAuthorization, viewModel::loadWeek, viewModel::syncAll, settings, viewModel::peek)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state
                    .map { viewModel.shouldRenewGoogleSession() }
                    .distinctUntilChanged()
                    .collect { shouldRenew -> if (shouldRenew) startGoogleAuthorization() }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.run() }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // El reloj nocturno va con el brillo al mínimo; el resto, con el elegido en Ajustes (o el del sistema)
                viewModel.state.map { if (it.night) NIGHT_BRIGHTNESS else it.brightness }.distinctUntilChanged().collect { value ->
                    window.attributes = window.attributes.apply {
                        screenBrightness = value ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ReturnButton.hide(this)
        Kiosk.enter(this)
        hideSystemBars()
        // Si se ha encendido con el botón fuera de horario, programa el apagado de cortesía.
        if (!viewModel.state.value.backup.recoveryRequired && !viewModel.state.value.backup.busy) ScreenSchedule.reschedule(this)
        // Después del primer consentimiento, Play Services puede emitir un access token nuevo sin volver a pedir
        // permisos. El token anterior no se persiste porque caduca en aproximadamente una hora.
        if (viewModel.shouldRenewGoogleSession()) startGoogleAuthorization()
    }

    private fun startGoogleAuthorization() {
        if (authorizationInFlight) return
        authorizationInFlight = true
        viewModel.beginGoogleLogin()
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope("https://www.googleapis.com/auth/calendar.readonly"),
                    Scope("https://www.googleapis.com/auth/tasks.readonly")
                )
            )
            .build()
        Identity.getAuthorizationClient(this).authorize(request)
            .addOnSuccessListener { result ->
                val pendingIntent = result.pendingIntent
                if (pendingIntent != null) {
                    authorizationLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                } else {
                    authorizationInFlight = false
                    viewModel.onGoogleAuthorized(result.accessToken)
                }
            }
            .addOnFailureListener {
                authorizationInFlight = false
                viewModel.googleAuthorizationFailed(
                    "No se pudo autorizar Google. Verifica que Play Services esté actualizado y las APIs estén habilitadas."
                )
            }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /** Ajustes de Android, con el botón flotante para volver (el marco no tiene barra de navegación). */
    private fun openSystemSettings(action: String) {
        startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        ReturnButton.show(this)
    }

    private companion object {
        /** El paso más bajo de la retroiluminación (1 de 255): lo justo para leer el reloj nocturno a oscuras. */
        const val NIGHT_BRIGHTNESS = 1f / 255f
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsControllerCompat(window, window.decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            // En Fire OS 5 / Android 5.1 la ruta legacy debe incluir los flags
            // FULLSCREEN y HIDE_NAVIGATION además de IMMERSIVE_STICKY.
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        }
    }
}


/** Escala proporcional al diseño base y normaliza el orden de los ejes para ambas orientaciones. */
private fun dashboardDesignScale(widthPx: Int, heightPx: Int): Float {
    val longEdge = maxOf(widthPx, heightPx)
    val shortEdge = minOf(widthPx, heightPx)
    return minOf(longEdge / 1920f, shortEdge / 1200f).coerceAtMost(1f)
}
