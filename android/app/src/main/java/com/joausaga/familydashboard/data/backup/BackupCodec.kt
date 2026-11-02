package com.joausaga.familydashboard.data.backup

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.joausaga.familydashboard.data.CalendarTaskRefreshInterval
import com.joausaga.familydashboard.data.TvUsageCalculator
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.time.ZoneId

/** JSON portable, acotado y verificado. SHA-256 detecta corrupción, no acredita autenticidad. */
class BackupCodec(private val now: () -> Long = System::currentTimeMillis) {
    private val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

    fun encode(document: DashboardBackup): ByteArray {
        val root = gson.toJsonTree(document).asJsonObject
        validateShape(root)
        validate(document)
        root.addProperty("sha256", digest(root.toString().toByteArray(Charsets.UTF_8)))
        return gson.toJson(root).toByteArray(Charsets.UTF_8).also {
            require(it.size <= MAX_BYTES) { "La copia supera el límite de 32 MiB; no se ha truncado." }
        }
    }

    fun decode(bytes: ByteArray): DashboardBackup {
        require(bytes.size <= MAX_BYTES) { "El archivo supera el límite de 32 MiB." }
        try {
            val root = JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject
            val checksum = string(root, "sha256")
            root.remove("sha256")
            require(checksum == digest(root.toString().toByteArray(Charsets.UTF_8))) { "La copia está alterada o incompleta (SHA-256 no coincide)." }
            validateShape(root)
            return gson.fromJson(root, DashboardBackup::class.java).also(::validate)
        } catch (failure: IllegalArgumentException) {
            throw failure
        } catch (failure: Exception) {
            throw IllegalArgumentException("No es una copia válida de Family Dashboard.", failure)
        }
    }

    fun read(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size().toLong() + count <= MAX_BYTES) { "El archivo supera el límite de 32 MiB." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun validate(document: DashboardBackup) {
        require(document.format == "family-dashboard-backup") { "El archivo no es una copia de Family Dashboard." }
        require(document.version == 1) { "Versión de copia no compatible: ${document.version}." }
        val maxTime = now() + 86_400_000L
        require(document.createdAtMillis in 0..maxTime) { "La fecha de la copia no es válida." }
        val config = document.configuration
        config.location?.let { place ->
            require(place.latitude.isFinite() && place.latitude in -90.0..90.0 && place.longitude.isFinite() && place.longitude in -180.0..180.0) { "Coordenadas no válidas en la copia." }
            place.timezone?.let { require(runCatching { ZoneId.of(it) }.isSuccess) { "Zona horaria no válida en la copia." } }
        }
        val schedule = config.schedule
        listOf(schedule.weekdayOn to schedule.weekdayOff, schedule.weekendOn to schedule.weekendOff).forEach { (on, off) ->
            require(on in 0..1439 && off in 0..1440 && off - on >= 15) { "Horario no válido en la copia." }
        }
        require(config.brightness == null || (config.brightness.isFinite() && config.brightness in 0.05f..1f)) { "Brillo no válido en la copia." }
        require(config.tvMonitorSettings.validationError() == null) { "Configuración TV no válida en la copia." }
        require(CalendarTaskRefreshInterval.isValid(config.calendarTaskRefreshMinutes)) { "Frecuencia Google no válida en la copia." }
        config.calendarSelections.forEach { (key, selection) ->
            require(CALENDAR_KEY.matches(key) && selection.knownIds.containsAll(selection.visibleIds)) { "Selección de calendarios no válida." }
        }
        config.taskListSelections.forEach { (key, selection) ->
            require(TASK_KEY.matches(key) && selection.knownIds.containsAll(selection.visibleIds)) { "Selección de listas no válida." }
        }
        document.tvHistory?.let { history ->
            fun time(value: Long) = require(value in 0..minOf(maxTime, document.createdAtMillis + 86_400_000L)) { "Fecha de historial TV no válida en la copia." }
            history.transitions.forEach { event -> time(event.atMillis); require(event.order >= 0) { "Orden de historial TV no válido." } }
            history.lastSyncMillis?.let(::time)
            history.localMonitoringStartedAtMillis?.let(::time)
            require(history.sourceRowCounts.values.all { it >= 0 } && history.sourceOrders.values.all { it >= 0 }) { "Metadatos TV no válidos." }
            val rebuilt = TvUsageCalculator.history(history.transitions, null, history.sourceAvailable)
            require(rebuilt.intervals == history.intervals && rebuilt.inconsistentRows == history.inconsistentRows) { "Los intervalos TV no coinciden con sus transiciones." }
        }
    }

    /** Exigir campos/tipos antes de Gson: evita valores primitivos implícitos o nulos de JSON incompleto. */
    private fun validateShape(root: JsonObject) {
        require(string(root, "format") == "family-dashboard-backup") { "El archivo no es una copia de Family Dashboard." }
        require(number(root, "version") == 1L) { "Versión de copia no compatible." }
        number(root, "createdAtMillis")
        val c = objectField(root, "configuration")
        field(c, "location").takeUnless { it.isJsonNull }?.asJsonObject?.let { p ->
            string(p, "name"); string(p, "detail"); decimal(p, "latitude"); decimal(p, "longitude"); nullableString(p, "timezone")
        }
        val s = objectField(c, "schedule")
        boolean(s, "enabled"); boolean(s, "screenOff")
        listOf("weekdayOn", "weekdayOff", "weekendOn", "weekendOff").forEach { int(s, it) }
        if (!field(c, "brightness").isJsonNull) decimal(c, "brightness")
        val tv = objectField(c, "tvMonitorSettings")
        string(tv, "ipAddress"); int(tv, "pollMinutes"); int(c, "calendarTaskRefreshMinutes")
        listOf("calendarSelections", "taskListSelections").forEach { name ->
            objectField(c, name).entrySet().forEach { (_, value) ->
                val selection = value.asJsonObject
                listOf("knownIds", "visibleIds").forEach { ids ->
                    field(selection, ids).asJsonArray.forEach { id -> require(id.isJsonPrimitive && id.asJsonPrimitive.isString && id.asString.isNotBlank()) { "ID de selección no válido." } }
                }
            }
        }
        field(root, "tvHistory").takeUnless { it.isJsonNull }?.asJsonObject?.let { h ->
            field(h, "transitions").asJsonArray.forEach { element ->
                val e = element.asJsonObject
                number(e, "atMillis"); number(e, "order"); string(e, "device"); boolean(e, "inferred")
                require(string(e, "state") in setOf("ON", "OFF")) { "Estado TV no válido." }
            }
            field(h, "intervals").asJsonArray.forEach { element ->
                val i = element.asJsonObject
                number(i, "startMillis"); nullableNumber(i, "endMillis")
            }
            objectField(h, "sourceRowCounts").entrySet().forEach { (key, _) -> int(objectField(h, "sourceRowCounts"), key) }
            objectField(h, "sourceOrders").entrySet().forEach { (key, _) -> number(objectField(h, "sourceOrders"), key) }
            int(h, "inconsistentRows"); boolean(h, "sourceAvailable"); nullableString(h, "sourceError")
            nullableNumber(h, "lastSyncMillis"); nullableNumber(h, "localMonitoringStartedAtMillis")
        }
    }

    private fun field(o: JsonObject, key: String): JsonElement = o.get(key) ?: throw IllegalArgumentException("Falta un campo obligatorio: $key.")
    private fun objectField(o: JsonObject, key: String) = field(o, key).asJsonObject
    private fun string(o: JsonObject, key: String): String = field(o, key).let { require(it.isJsonPrimitive && it.asJsonPrimitive.isString) { "Texto no válido: $key." }; it.asString }
    private fun boolean(o: JsonObject, key: String) { val v = field(o, key); require(v.isJsonPrimitive && v.asJsonPrimitive.isBoolean) { "Valor lógico no válido: $key." } }
    private fun number(o: JsonObject, key: String): Long = field(o, key).let { require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber) { "Número no válido: $key." }; it.asBigDecimal.longValueExact() }
    private fun int(o: JsonObject, key: String) { require(number(o, key) in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "Número fuera de rango: $key." } }
    private fun decimal(o: JsonObject, key: String) { val v = field(o, key); require(v.isJsonPrimitive && v.asJsonPrimitive.isNumber && v.asDouble.isFinite()) { "Número no válido: $key." } }
    private fun nullableString(o: JsonObject, key: String) { if (!field(o, key).isJsonNull) string(o, key) }
    private fun nullableNumber(o: JsonObject, key: String) { if (!field(o, key).isJsonNull) number(o, key) }

    private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }

    companion object {
        const val MAX_BYTES = 32 * 1024 * 1024
        val CALENDAR_KEY = Regex("calendar_visibility_[A-Za-z0-9_-]+")
        val TASK_KEY = Regex("task_list_visibility_[A-Za-z0-9_-]+")
    }
}
