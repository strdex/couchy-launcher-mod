package com.conreo.couchytv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToInt

/** Weather via Open-Meteo (free, no API key). */
object Weather {

    data class Place(val label: String, val lat: Double, val lon: Double)

    private val json = Json { ignoreUnknownKeys = true }

    private fun get(url: String): JsonObject {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        return try {
            json.parseToJsonElement(c.inputStream.bufferedReader().readText()).jsonObject
        } finally {
            c.disconnect()
        }
    }

    /** City search for the manual picker. */
    suspend fun search(query: String): List<Place> = withContext(Dispatchers.IO) {
        runCatching {
            val q = URLEncoder.encode(query.trim(), "UTF-8")
            val lang = Locale.getDefault().language.ifEmpty { "en" }
            val root = get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&language=$lang&format=json")
            root["results"]?.jsonArray.orEmpty().map { e ->
                val o = e.jsonObject
                val name = o["name"]!!.jsonPrimitive.content
                val extra = listOfNotNull(
                    o["admin1"]?.jsonPrimitive?.contentOrNull,
                    o["country"]?.jsonPrimitive?.contentOrNull,
                ).joinToString(", ")
                Place(
                    label = if (extra.isEmpty()) name else "$name, $extra",
                    lat = o["latitude"]!!.jsonPrimitive.double,
                    lon = o["longitude"]!!.jsonPrimitive.double,
                )
            }
        }.getOrDefault(emptyList())
    }

    /** Current conditions: rounded °C + WMO weather code. */
    data class Now(val temp: Int, val code: Int, val day: Boolean)

    suspend fun current(lat: Double, lon: Double): Now? = withContext(Dispatchers.IO) {
        runCatching {
            val root = get(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code,is_day"
            )
            val cur = root["current"]!!.jsonObject
            Now(
                temp = cur["temperature_2m"]!!.jsonPrimitive.double.roundToInt(),
                code = cur["weather_code"]!!.jsonPrimitive.int,
                day = cur["is_day"]?.jsonPrimitive?.int != 0,
            )
        }.getOrNull()
    }
}