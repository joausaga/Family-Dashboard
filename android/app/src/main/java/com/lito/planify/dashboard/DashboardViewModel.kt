package com.lito.planify.dashboard

import androidx.core.content.ContextCompat
import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lito.planify.dashboard.data.Api
import com.lito.planify.dashboard.data.Content
import com.lito.planify.dashboard.data.visibleInCalendars
import com.lito.planify.dashboard.data.visibleInTaskLists
import com.lito.planify.dashboard.data.Forecast
import com.lito.planify.dashboard.data.GoogleWorkspaceApis
import com.lito.planify.dashboard.data.GeoResult
import com.lito.planify.dashboard.data.GoogleCalendar
import com.lito.planify.dashboard.data.GoogleTaskList
import com.lito.planify.dashboard.data.Location
import com.lito.planify.dashboard.data.ReferenceClock
import com.lito.planify.dashboard.data.Schedule
import com.lito.planify.dashboard.data.Store
import com.lito.planify.dashboard.data.TvUsageCalculator
import com.lito.planify.dashboard.data.TvUsageView
import com.lito.planify.dashboard.data.TvDisplayStatus
import com.lito.planify.dashboard.data.TvMonitorSettings
import com.lito.planify.dashboard.data.TvLocalApi
import com.lito.planify.dashboard.data.CalendarTaskRefreshInterval
import com.lito.planify.dashboard.data.Week
import com.lito.planify.dashboard.data.cache.DiskCache
import com.lito.planify.dashboard.data.isUnauthorized
import com.lito.planify.dashboard.data.repository.CalendarRepository
import com.lito.planify.dashboard.data.repository.ContentRepository
import com.lito.planify.dashboard.data.repository.GoogleWorkspaceRepository
import com.lito.planify.dashboard.data.repository.WeatherRepository
import com.lito.planify.dashboard.data.repository.TvLocalUsageRepository
import com.lito.planify.dashboard.power.ScreenSchedule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

data class UiState(
    val token: String? = null,
    val email: String? = null,
    val now: LocalDateTime = LocalDateTime.now(),
    /** false mientras el reloj no se haya sincronizado (el RTC vuelve a la fecha de fábrica). */
    val clockValid: Boolean = false,
    val content: Content? = null,
    val forecast: Forecast? = null,
    val location: Location? = null,
    val schedule: Schedule = Schedule(),
    val brightness: Float? = null,
    /** true si el último resultado de sincronización aceptado falló; no representa el estado del Wi-Fi. */
    val syncError: Boolean = false,
    /** Android detecta una red con salida a Internet (sin ella, el login muestra la espera y "Ir a Ajustes"). */
    val connected: Boolean = false,
    /** Sincronización manual en curso (botón de la barra lateral). */
    val syncing: Boolean = false,
    /** Semanas del calendario descargadas bajo demanda, por su lunes. */
    val weeks: Map<LocalDate, Week> = emptyMap(),
    val loginError: String? = null,
    val loggingIn: Boolean = false,
    val geoResults: List<GeoResult> = emptyList(),
    val geoSearching: Boolean = false,
    /** Tocar el reloj nocturno enseña el dashboard hasta esta hora. */
    val peekUntil: LocalDateTime? = null,
    /** Reloj nocturno pedido con la luna de la barra lateral: hasta que se toque o hasta el siguiente encendido. */
    val nightUntil: LocalDateTime? = null,
    val calendars: List<GoogleCalendar> = emptyList(),
    val visibleCalendarIds: Set<String> = emptySet(),
    val calendarAccountId: String? = null,
    val calendarSettingsLoading: Boolean = false,
    val calendarSettingsError: String? = null,
    val calendarSelectionReady: Boolean = false,
    val taskLists: List<GoogleTaskList> = emptyList(),
    val visibleTaskListIds: Set<String> = emptySet(),
    val taskListAccountId: String? = null,
    val taskListSettingsLoading: Boolean = false,
    val taskListSettingsError: String? = null,
    val taskListSelectionReady: Boolean = false,
    val tvUsage: TvUsageView = TvUsageView(),
    val tvMonitorSettings: TvMonitorSettings = TvMonitorSettings(),
    val calendarTaskRefreshMinutes: Int = CalendarTaskRefreshInterval.DEFAULT_MINUTES,
    val tvSettingsError: String? = null,
    /** Hora (ms) de la última descarga correcta de calendario y tareas, y del tiempo; null si nunca. */
    val contentFetchedAt: Long? = null,
    val forecastFetchedAt: Long? = null
) {
    /** Reloj nocturno: fuera de horario (salvo mientras se mira el dashboard un momento) o pedido con la luna. */
    val night: Boolean
        get() = clockValid && token != null && (
            (nightUntil != null && now.isBefore(nightUntil)) ||
                (ScreenSchedule.isNight(schedule, now) && (peekUntil == null || !now.isBefore(peekUntil)))
            )
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val store = Store(application)
    private val api = Api.client
    private val google = GoogleWorkspaceRepository(GoogleWorkspaceApis.calendar, GoogleWorkspaceApis.tasks)
    private val cache = DiskCache(File(application.filesDir, "cache"))
    private val contentRepository = ContentRepository(google, cache)
    private val weatherRepository = WeatherRepository(api, cache)
    private val tvUsageRepository = TvLocalUsageRepository(TvLocalApi(), cache)
    private val calendarRepository = CalendarRepository(google, cache)
    private val clock = ReferenceClock(BuildConfig.BUILD_TIME, { store.lastValidTime }, { store.lastValidTime = it })

    private val _state = MutableStateFlow(
        clock.current().let { time ->
            UiState(
                // Los access tokens de Google duran una hora: solo viven en memoria y se renuevan con Play Services.
                token = null,
                email = null,
                now = time.now,
                clockValid = time.valid,
                content = contentRepository.content.value?.value,
                forecast = weatherRepository.forecast.value?.value,
                weeks = calendarRepository.weeks.value,
                location = store.location,
                schedule = store.schedule,
                brightness = store.brightness,
                tvMonitorSettings = store.tvMonitorSettings,
                calendarTaskRefreshMinutes = store.calendarTaskRefreshMinutes,
                tvUsage = TvUsageView(status = if (store.tvMonitorSettings.isConfigured) TvDisplayStatus.LOADING else TvDisplayStatus.NOT_CONFIGURED)
            )
        }
    )
    val state: StateFlow<UiState> = _state

    private var rawContent = contentRepository.content.value?.value
    private var rawWeeks = calendarRepository.weeks.value
    private var activeCalendarWeek: LocalDate? = null
    private val syncStatusTracker = SyncStatusTracker()
    private var tvPollInFlight = false

    private val connectivity = ContextCompat.getSystemService(application, ConnectivityManager::class.java)!!
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
            setConnected(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))

        override fun onLost(network: Network) = setConnected(false)
    }

    @Suppress("DEPRECATION")
    private fun hasInternetConnection(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        connectivity.getNetworkCapabilities(connectivity.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    } else {
        connectivity.activeNetworkInfo?.isConnected == true
    }

    init {
        // Los repositorios publican la copia en disco y cada descarga buena: la pantalla siempre tiene lo último.
        viewModelScope.launch {
            contentRepository.content.collect { c ->
                rawContent = c?.value
                _state.update { it.copy(contentFetchedAt = c?.fetchedAt) }
                applyFilters()
            }
        }
        viewModelScope.launch { weatherRepository.forecast.collect { f -> _state.update { it.copy(forecast = f?.value, forecastFetchedAt = f?.fetchedAt) } } }
        viewModelScope.launch {
            tvUsageRepository.history.collect { entry ->
                val current = _state.value.now.atZone(ZoneId.systemDefault()).toInstant()
                val configured = _state.value.tvMonitorSettings.isConfigured
                val view = entry?.value?.let { history -> TvUsageCalculator.summarize(history, current, ZoneId.of("America/Asuncion")) }
                    ?: TvUsageView()
                _state.update { it.copy(tvUsage = if (configured) view else view.copy(status = TvDisplayStatus.NOT_CONFIGURED, currentSessionMillis = null)) }
            }
        }
        viewModelScope.launch {
            calendarRepository.weeks.collect { w ->
                rawWeeks = w
                applyFilters()
            }
        }
        setConnected(hasInternetConnection())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivity.registerDefaultNetworkCallback(networkCallback)
        } else {
            connectivity.registerNetworkCallback(
                NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                networkCallback
            )
        }
    }

    override fun onCleared() {
        connectivity.unregisterNetworkCallback(networkCallback)
    }

    /** Al recuperar la red se sincroniza enseguida, sin esperar al siguiente ciclo. */
    private fun setConnected(connected: Boolean) {
        val was = _state.value.connected
        _state.update { it.copy(connected = connected) }
        if (connected && !was) refreshNow()
    }

    /** Bucle activo: reloj y vencimientos de contenido/semana; clima y TV conservan intervalos propios. */
    suspend fun run() {
        var routineWeek = _state.value.now.toLocalDate().with(java.time.DayOfWeek.MONDAY)
        while (true) {
            tick()
            if (_state.value.clockValid) {
                val currentWeek = _state.value.now.toLocalDate().with(java.time.DayOfWeek.MONDAY)
                if (currentWeek != routineWeek) {
                    routineWeek = currentWeek
                    refreshContent()
                } else if (contentRepository.isStale(CalendarTaskRefreshInterval.maxAgeMillis(_state.value.calendarTaskRefreshMinutes))) {
                    refreshContent()
                }
                val intervalAge = CalendarTaskRefreshInterval.maxAgeMillis(_state.value.calendarTaskRefreshMinutes)
                activeCalendarWeek?.let { monday ->
                    if (calendarRepository.isStale(monday, intervalAge)) refreshCalendarWeek(monday)
                }
                if (weatherRepository.isStale()) refreshWeather()
            }
            val settings = _state.value.tvMonitorSettings
            if (_state.value.clockValid && settings.isConfigured) {
                val history = tvUsageRepository.history.value?.value
                val lastPoll = history?.lastSyncMillis
                val nowMillis = _state.value.now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val firstLocalPoll = history?.localMonitoringStartedAtMillis == null
                if (firstLocalPoll || lastPoll == null || nowMillis - lastPoll >= settings.pollMinutes * 60_000L) {
                    pollTvUsage()
                }
            }
            delay(60_000 - System.currentTimeMillis() % 60_000 + 50)
        }
    }

    private fun tick() {
        val time = clock.current()
        val current = time.now.atZone(ZoneId.systemDefault()).toInstant()
        val view = tvUsageRepository.history.value?.value?.let {
            TvUsageCalculator.summarize(it, current, ZoneId.of("America/Asuncion"))
        } ?: TvUsageView()
        val tv = if (_state.value.tvMonitorSettings.isConfigured) view
            else view.copy(status = TvDisplayStatus.NOT_CONFIGURED, currentSessionMillis = null)
        _state.update { it.copy(now = time.now, clockValid = time.valid, tvUsage = tv) }
    }

    /** Fuerza la sincronización (al recuperar la red, tras iniciar sesión...). */
    fun refreshNow() {
        viewModelScope.launch {
            tick()
            if (!_state.value.clockValid) return@launch
            refreshContent()
            activeCalendarWeek?.let { refreshCalendarWeek(it) }
            refreshWeather()
            pollTvUsage()
        }
    }

    /** Sin hora válida no se pide nada: las fechas de la petición serían las de fábrica. */
    private suspend fun refreshContent() {
        val token = _state.value.token ?: return
        val requestId = syncStatusTracker.begin()
        report(requestId, contentRepository.refresh(token, _state.value.now.toLocalDate()))
    }

    private suspend fun refreshWeather() {
        val location = _state.value.location ?: return
        val requestId = syncStatusTracker.begin()
        report(requestId, weatherRepository.refresh(location))
    }

    private suspend fun pollTvUsage() {
        val settings = _state.value.tvMonitorSettings
        if (!settings.isConfigured || !_state.value.clockValid) return
        if (tvPollInFlight) return
        tvPollInFlight = true
        try {
            val time = clock.current()
            if (!time.valid) return
            _state.update { it.copy(now = time.now, clockValid = time.valid) }
            val observedAt = time.now.atZone(ZoneId.systemDefault()).toInstant()
            tvUsageRepository.observe(settings.ipAddress, observedAt)
        } finally {
            tvPollInFlight = false
        }
    }

    /** Guarda la IP y el intervalo local; borrar la IP desactiva el sondeo sin borrar el historial. */
    fun saveTvMonitorSettings(ipAddress: String, pollMinutes: Int) {
        val ip = ipAddress.trim()
        val error = TvMonitorSettings(ip, pollMinutes).validationError()
        if (error != null) {
            _state.update { it.copy(tvSettingsError = error) }
            return
        }
        val settings = TvMonitorSettings(ip, pollMinutes)
        store.tvMonitorSettings = settings
        _state.update { current -> current.copy(
            tvMonitorSettings = settings,
            tvSettingsError = null,
            tvUsage = if (settings.isConfigured) current.tvUsage.copy(status = TvDisplayStatus.LOADING)
                else current.tvUsage.copy(status = TvDisplayStatus.NOT_CONFIGURED, currentSessionMillis = null)
        ) }
        if (settings.isConfigured) viewModelScope.launch { pollTvUsage() }
    }

    /** Carga la semana del calendario que empieza en [monday] si no está en caché o ha caducado. */
    fun loadWeek(monday: LocalDate) {
        activeCalendarWeek = monday
        val token = _state.value.token ?: return
        if (!calendarRepository.isStale(monday, CalendarTaskRefreshInterval.maxAgeMillis(_state.value.calendarTaskRefreshMinutes))) return
        viewModelScope.launch { refreshCalendarWeek(monday) }
    }

    private suspend fun refreshCalendarWeek(monday: LocalDate) {
        val token = _state.value.token ?: return
        val requestId = syncStatusTracker.begin()
        report(requestId, calendarRepository.refresh(token, monday))
    }

    fun setCalendarTaskRefreshMinutes(minutes: Int) {
        if (!CalendarTaskRefreshInterval.isValid(minutes)) return
        store.calendarTaskRefreshMinutes = minutes
        _state.update { it.copy(calendarTaskRefreshMinutes = minutes) }
    }

    /**
     * Botón de sincronizar: vuelve a pedir el contenido de Hoy, las semanas del calendario ya cargadas y el
     * tiempo, aunque no hayan caducado. Dura al menos un segundo para que se vea el giro.
     */
    fun syncAll() {
        if (_state.value.syncing) return
        viewModelScope.launch {
            _state.update { it.copy(syncing = true) }
            val started = System.currentTimeMillis()
            tick()
            if (_state.value.clockValid) {
                refreshContent()
                _state.value.token?.let { token ->
                    calendarRepository.loaded().forEach { monday ->
                        val requestId = syncStatusTracker.begin()
                        report(requestId, calendarRepository.refresh(token, monday))
                    }
                }
                refreshWeather()
            }
            pollTvUsage()
            delay((1000 - (System.currentTimeMillis() - started)).coerceAtLeast(0))
            _state.update { it.copy(syncing = false) }
        }
    }

    /** Registra el resultado más reciente; las peticiones antiguas no pisan una respuesta nueva. */
    private fun report(requestId: Long, result: Result<*>) {
        val error = result.exceptionOrNull()
        val syncError = syncStatusTracker.complete(requestId, succeeded = error == null) ?: return
        when {
            error == null -> _state.update { it.copy(connected = true, syncError = false) }
            error.isUnauthorized -> expireSession()
            else -> _state.update { it.copy(syncError = syncError) }
        }
    }

    fun beginGoogleLogin() = _state.update { it.copy(loggingIn = true, loginError = null) }

    fun onGoogleAuthorized(accessToken: String?) {
        if (accessToken.isNullOrBlank()) {
            googleAuthorizationFailed("Google no devolvió un token de acceso")
            return
        }
        store.googleConnected = true
        store.token = null
        store.email = null
        _state.update {
            it.copy(
                token = accessToken,
                email = "Google",
                syncError = false,
                loggingIn = false,
                loginError = null,
                calendarSettingsLoading = true,
                calendarSettingsError = null,
                calendarSelectionReady = false,
                taskListSettingsLoading = true,
                taskListSettingsError = null,
                taskListSelectionReady = false,
                content = null,
                weeks = emptyMap()
            )
        }
        viewModelScope.launch {
            refreshCalendarSettings(accessToken)
            refreshTaskListSettings(accessToken)
            refreshNow()
            calendarRepository.loaded().forEach { monday ->
                viewModelScope.launch {
                    val requestId = syncStatusTracker.begin()
                    report(requestId, calendarRepository.refresh(accessToken, monday))
                }
            }
        }
    }

    fun googleAuthorizationFailed(message: String) {
        _state.update { it.copy(loggingIn = false, loginError = message) }
    }

    fun shouldRenewGoogleSession(): Boolean = store.googleConnected &&
        _state.value.token == null && !_state.value.loggingIn && _state.value.loginError == null

    fun logout() {
        _state.value.token?.let { token ->
            viewModelScope.launch { runCatching { GoogleWorkspaceApis.oauth.revoke(token) } }
        }
        store.logout()
        activeCalendarWeek = null
        contentRepository.clear()
        calendarRepository.clear()
        rawContent = null
        rawWeeks = emptyMap()
        _state.update {
            it.copy(
                token = null,
                calendarSelectionReady = false,
                calendars = emptyList(),
                visibleCalendarIds = emptySet(),
                calendarAccountId = null,
                taskLists = emptyList(),
                visibleTaskListIds = emptySet(),
                taskListAccountId = null,
                taskListSelectionReady = false,
                taskListSettingsLoading = false,
                taskListSettingsError = null
            )
        }
    }

    fun refreshCalendarSettings() {
        val token = _state.value.token ?: return
        viewModelScope.launch { refreshCalendarSettings(token) }
    }

    fun refreshTaskListSettings() {
        val token = _state.value.token ?: return
        viewModelScope.launch { refreshTaskListSettings(token) }
    }

    fun setCalendarVisible(calendarId: String, visible: Boolean) {
        val current = _state.value
        val accountId = current.calendarAccountId ?: return
        val ids = current.calendars.mapTo(mutableSetOf()) { it.id }
        if (calendarId !in ids) return
        val next = current.visibleCalendarIds.toMutableSet().apply {
            if (visible) add(calendarId) else remove(calendarId)
        }
        store.saveCalendarVisibility(accountId, ids, next)
        _state.update { it.copy(visibleCalendarIds = next) }
        applyFilters()
    }

    private suspend fun refreshCalendarSettings(token: String) {
        _state.update { it.copy(calendarSettingsLoading = true, calendarSettingsError = null) }
        val calendars = runCatching { google.calendars(token) }.getOrElse {
            _state.update {
                it.copy(
                    calendarSettingsLoading = false,
                    calendarSettingsError = "No se pudieron cargar los calendarios. Comprueba la conexión y vuelve a intentarlo.",
                    calendarSelectionReady = it.calendarSelectionReady
                )
            }
            return
        }
        val accountId = calendars.singleOrNull { it.primary == true }?.id
        if (accountId == null) {
            _state.update {
                it.copy(
                    calendarSettingsLoading = false,
                    calendarSettingsError = "Google no indicó el calendario principal de esta cuenta. Vuelve a cargar la lista.",
                    calendarSelectionReady = false,
                    content = null,
                    weeks = emptyMap()
                )
            }
            return
        }
        val visibleIds = store.reconcileVisibleCalendars(accountId, calendars.mapTo(mutableSetOf()) { it.id })
        _state.update {
            val accountChanged = it.calendarAccountId != null && it.calendarAccountId != accountId
            it.copy(
                calendars = calendars.sortedWith(compareByDescending<GoogleCalendar> { c -> c.primary == true }.thenBy { c -> c.summary.orEmpty().lowercase() }),
                visibleCalendarIds = visibleIds,
                calendarAccountId = accountId,
                calendarSettingsLoading = false,
                calendarSettingsError = null,
                calendarSelectionReady = true,
                taskLists = if (accountChanged) emptyList() else it.taskLists,
                visibleTaskListIds = if (accountChanged) emptySet() else it.visibleTaskListIds,
                taskListAccountId = if (accountChanged) null else it.taskListAccountId,
                taskListSelectionReady = if (accountChanged) false else it.taskListSelectionReady
            )
        }
        applyFilters()
    }

    fun setTaskListVisible(taskListId: String, visible: Boolean) {
        val current = _state.value
        val accountId = current.taskListAccountId ?: return
        if (accountId != current.calendarAccountId) return
        val ids = current.taskLists.mapTo(mutableSetOf()) { it.id }
        if (taskListId !in ids) return
        val next = current.visibleTaskListIds.toMutableSet().apply {
            if (visible) add(taskListId) else remove(taskListId)
        }
        store.saveTaskListVisibility(accountId, ids, next)
        _state.update { it.copy(visibleTaskListIds = next) }
        applyFilters()
    }

    private suspend fun refreshTaskListSettings(token: String) {
        val currentState = _state.value
        val accountId = currentState.calendarAccountId
        if (!currentState.calendarSelectionReady || accountId == null) {
            _state.update {
                it.copy(
                    taskListSettingsLoading = false,
                    taskListSettingsError = "No se pudo identificar la cuenta de Google para cargar las listas."
                )
            }
            return
        }
        _state.update { it.copy(taskListSettingsLoading = true, taskListSettingsError = null) }
        val taskLists = runCatching { google.taskLists(token) }.getOrElse {
            _state.update {
                it.copy(
                    taskListSettingsLoading = false,
                    taskListSettingsError = "No se pudieron cargar las listas de tareas. Comprueba la conexión y vuelve a intentarlo."
                )
            }
            return
        }
        if (_state.value.token != token || _state.value.calendarAccountId != accountId) return
        val ids = taskLists.mapTo(mutableSetOf()) { it.id }
        val visibleIds = store.reconcileVisibleTaskLists(accountId, ids)
        _state.update {
            it.copy(
                taskLists = taskLists.sortedBy { list -> list.title.orEmpty().lowercase() },
                visibleTaskListIds = visibleIds,
                taskListAccountId = accountId,
                taskListSettingsLoading = false,
                taskListSettingsError = null,
                taskListSelectionReady = true
            )
        }
        applyFilters()
    }

    private fun applyFilters() {
        val current = _state.value
        val content = rawContent?.let { data ->
            if (!current.calendarSelectionReady) null else {
                val calendarFiltered = data.visibleInCalendars(current.visibleCalendarIds)
                if (current.taskListSelectionReady && current.taskListAccountId == current.calendarAccountId) {
                    calendarFiltered.visibleInTaskLists(current.visibleTaskListIds)
                } else {
                    calendarFiltered.copy(tasks = emptyList())
                }
            }
        }
        val weeks = if (!current.calendarSelectionReady) emptyMap() else rawWeeks.mapValues { (_, week) ->
            week.visibleInCalendars(current.visibleCalendarIds)
        }
        _state.update { it.copy(content = content, weeks = weeks) }
    }

    private fun expireSession() {
        if (_state.value.token == null) return
        store.token = null
        // Conserva la caché local mientras Play Services renueva el access token.
        // MainActivity observa token=null y solicita una autorización nueva sin esperar a otro onResume.
        _state.update { it.copy(token = null, loginError = null) }
    }

    fun searchLocation(query: String) {
        if (query.isBlank()) return
        _state.update { it.copy(geoSearching = true) }
        viewModelScope.launch {
            val results = runCatching { api.geocode(Api.geocodeUrl(query.trim())).body()?.results }.getOrNull().orEmpty()
            _state.update { it.copy(geoResults = results, geoSearching = false) }
        }
    }

    fun selectLocation(result: GeoResult) {
        val location = Location(
            name = result.name,
            detail = listOfNotNull(result.admin1, result.country).joinToString(", "),
            latitude = result.latitude,
            longitude = result.longitude
        )
        store.location = location
        weatherRepository.clear()
        _state.update { it.copy(location = location, geoResults = emptyList()) }
        viewModelScope.launch { refreshWeather() }
    }

    fun setSchedule(schedule: Schedule) {
        store.schedule = schedule
        _state.update { it.copy(schedule = schedule) }
        ScreenSchedule.reschedule(getApplication())
    }

    /**
     * Desde el reloj nocturno: vuelve el dashboard. Fuera de horario, solo durante [ScreenSchedule.GRACE_MINUTES];
     * el reloj pedido con la luna se quita del todo.
     */
    fun peek() {
        _state.update { it.copy(peekUntil = LocalDateTime.now().plusMinutes(ScreenSchedule.GRACE_MINUTES), nightUntil = null) }
    }

    /**
     * La luna de la barra lateral. Con el horario y el reloj nocturno activos, pasa al reloj (hasta que se toque o
     * hasta el siguiente encendido) y devuelve true; si no, false y la pantalla se apaga de verdad.
     */
    fun nightNow(): Boolean {
        val schedule = _state.value.schedule
        if (!schedule.enabled || schedule.screenOff || !_state.value.clockValid) return false
        _state.update { it.copy(nightUntil = ScreenSchedule.nextOn(schedule, it.now), peekUntil = null) }
        return true
    }

    fun setBrightness(value: Float?) {
        store.brightness = value
        _state.update { it.copy(brightness = value) }
    }
}
