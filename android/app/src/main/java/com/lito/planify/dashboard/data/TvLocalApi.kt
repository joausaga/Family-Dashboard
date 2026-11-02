package com.lito.planify.dashboard.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.Inet6Address
import java.net.InetAddress
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

fun interface TvStatusProbe {
    suspend fun isOn(ipAddress: String): Boolean
}

/** Lectura de solo lectura del endpoint local Samsung Smart View/Tizen. */
class TvLocalApi(private val timeoutMillis: Int = 5_000) : TvStatusProbe {
    override suspend fun isOn(ipAddress: String): Boolean = withContext(Dispatchers.IO) {
        require(isIpAddress(ipAddress)) { "La dirección IP de la TV no es válida." }
        val host = if (ipAddress.contains(':')) "[$ipAddress]" else ipAddress
        val connection = URL("http://$host:8001/api/v2/").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("La TV respondió HTTP $code")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseIsOn(body)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        /** Un estado de potencia explícito prevalece; sin él, una respuesta válida del API confirma que la TV responde. */
        internal fun parseIsOn(body: String): Boolean {
            val root = runCatching { JsonParser.parseString(body).asJsonObject }
                .getOrElse { throw IOException("Respuesta no válida del API de TV") }
            val device = root.get("device")?.takeIf { it.isJsonObject }?.asJsonObject
                ?: throw IOException("La respuesta no contiene información de dispositivo Samsung")
            val explicit = findPowerState(device) ?: findPowerState(root)
            if (explicit != null) {
                return when (explicit.trim().lowercase(Locale.ROOT)) {
                    "on", "true", "1", "active" -> true
                    "off", "false", "0", "standby" -> false
                    else -> throw IOException("El API devolvió un estado de potencia no reconocido")
                }
            }
            return true
        }

        private fun findPowerState(value: JsonObject): String? {
            for ((key, element) in value.entrySet()) {
                if ((key.equals("powerstate", ignoreCase = true) || key.equals("power_state", ignoreCase = true)) && !element.isJsonNull) {
                    return element.asString
                }
            }
            return null
        }

        internal fun isIpAddress(value: String): Boolean {
            val ip = value.trim()
            if (ip.isEmpty()) return false
            if (!ip.contains(':')) {
                val parts = ip.split('.')
                return parts.size == 4 && parts.all { part ->
                    part.isNotEmpty() && part.all(Char::isDigit) && part.toIntOrNull()?.let { it in 0..255 } == true
                }
            }
            if (!ip.matches(Regex("[0-9a-fA-F:.]+"))) return false
            return runCatching { InetAddress.getByName(ip) is Inet6Address }.getOrDefault(false)
        }
    }
}
