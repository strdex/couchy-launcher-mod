package com.conreo.couchytv.data

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Daily wallpapers from Microsoft Bing (Free, no API key needed).
 * Downloads today's Bing images once a day into filesDir/bingwall/
 * and rotates them periodically.
 */
object BingWallpapers {

    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) CouchyLauncher/1.0"
    private val json = Json { ignoreUnknownKeys = true }

    private fun dir(ctx: Context) = File(ctx.filesDir, "bingwall").apply { mkdirs() }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", UA)
        }

    fun pick(ctx: Context, avoid: File? = null): File? {
        runCatching { refreshIfStale(ctx) }
        val files = dir(ctx).listFiles { f -> f.name.endsWith(".jpg") }.orEmpty().toList()
        if (files.isEmpty()) return null
        return files.filter { it != avoid }.randomOrNull() ?: files.random()
    }

    private fun refreshIfStale(ctx: Context) {
        val d = dir(ctx)
        val stamp = File(d, "stamp")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (stamp.exists() && stamp.readText() == today) return

        val urls = fetchBingUrls()
        if (urls.isEmpty()) return

        val tmp = File(d, "new").apply { deleteRecursively(); mkdirs() }
        urls.forEachIndexed { i, u ->
            runCatching {
                val c = open(u)
                try {
                    c.inputStream.use { inp -> File(tmp, "$i.jpg").outputStream().use { inp.copyTo(it) } }
                } finally { c.disconnect() }
            }
        }
        val got = tmp.listFiles().orEmpty()
        if (got.isEmpty()) { tmp.deleteRecursively(); return }

        d.listFiles { f -> f.name.endsWith(".jpg") }?.forEach { it.delete() }
        got.forEach { it.renameTo(File(d, it.name)) }
        tmp.deleteRecursively()
        stamp.writeText(today)
    }

    private fun fetchBingUrls(): List<String> = runCatching {
        val url = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=8&mkt=en-US"
        val c = open(url)
        val body = try { c.inputStream.bufferedReader().readText() } finally { c.disconnect() }
        val images = json.parseToJsonElement(body).jsonObject["images"]?.jsonArray ?: return emptyList()
        images.mapNotNull { elem ->
            val relUrl = elem.jsonObject["url"]?.jsonPrimitive?.content ?: return@mapNotNull null
            if (relUrl.startsWith("http")) relUrl else "https://www.bing.com$relUrl"
        }
    }.getOrDefault(emptyList())
}
