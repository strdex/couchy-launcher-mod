package com.conreo.couchytv.data

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * City photo wallpapers via Unsplash API (with Wikimedia fallback).
 * Downloads landscape photos once a day into filesDir/citywall/
 * and rotates them periodically.
 */
object CityWallpapers {

    private const val EMBEDDED_UNSPLASH_KEY = "cAOHVMsvAFU3q-SuuzyoAbkORyjuOFkl5JBhIYELobw"
    private const val COUNT = 6
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) CouchyLauncher/1.0"
    private val json = Json { ignoreUnknownKeys = true }

    private fun dir(ctx: Context) = File(ctx.filesDir, "citywall").apply { mkdirs() }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", UA)
        }

    fun pick(ctx: Context, city: String, unsplashKey: String = "", avoid: File? = null): File? {
        runCatching { refreshIfStale(ctx, city, unsplashKey) }
        val files = dir(ctx).listFiles { f -> f.name.endsWith(".jpg") }.orEmpty().toList()
        if (files.isEmpty()) return null
        return files.filter { it != avoid }.randomOrNull() ?: files.random()
    }

    private fun refreshIfStale(ctx: Context, city: String, unsplashKey: String) {
        val d = dir(ctx)
        val stamp = File(d, "stamp")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val key = "$today|$city"
        if (stamp.exists() && stamp.readText() == key) return

        val urls = search(city, unsplashKey)
        if (urls.isEmpty()) return

        val tmp = File(d, "new").apply { deleteRecursively(); mkdirs() }
        urls.shuffled().take(COUNT).forEachIndexed { i, u ->
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
        stamp.writeText(key)
    }

    private fun search(city: String, unsplashKey: String): List<String> {
        val key = unsplashKey.ifBlank { EMBEDDED_UNSPLASH_KEY }
        if (key.isNotBlank()) {
            val unsplashUrls = searchUnsplash(city, key)
            if (unsplashUrls.isNotEmpty()) return unsplashUrls
        }
        return searchWikimedia(city)
    }

    private fun searchUnsplash(city: String, key: String): List<String> = runCatching {
        val q = URLEncoder.encode("$city city skyline", "UTF-8")
        val url = "https://api.unsplash.com/search/photos?query=$q&orientation=landscape&per_page=10&client_id=${key.trim()}"
        val c = open(url)
        val body = try { c.inputStream.bufferedReader().readText() } finally { c.disconnect() }
        val root = json.parseToJsonElement(body).jsonObject
        val results = root["results"]?.jsonArray ?: return emptyList()
        results.mapNotNull { e ->
            val urls = e.jsonObject["urls"]?.jsonObject
            urls?.get("regular")?.jsonPrimitive?.content ?: urls?.get("full")?.jsonPrimitive?.content
        }
    }.getOrDefault(emptyList())

    private fun searchWikimedia(city: String): List<String> = runCatching {
        val q = URLEncoder.encode("$city city skyline filetype:bitmap", "UTF-8")
        val url = "https://commons.wikimedia.org/w/api.php?action=query&format=json" +
            "&generator=search&gsrnamespace=6&gsrlimit=30&gsrsearch=$q" +
            "&prop=imageinfo&iiprop=url|size|mime&iiurlwidth=1920"
        val c = open(url)
        val body = try { c.inputStream.bufferedReader().readText() } finally { c.disconnect() }
        val pages = json.parseToJsonElement(body).jsonObject["query"]
            ?.jsonObject?.get("pages")?.jsonObject ?: return emptyList()
        pages.values.mapNotNull { p ->
            val info = p.jsonObject["imageinfo"]?.jsonArray?.firstOrNull()?.jsonObject ?: return@mapNotNull null
            val w = info["width"]?.jsonPrimitive?.int ?: 0
            val h = info["height"]?.jsonPrimitive?.int ?: 0
            val mime = info["mime"]?.jsonPrimitive?.contentOrNull
            if (mime != "image/jpeg" || w < 1600 || w < h * 1.3) null
            else info["thumburl"]?.jsonPrimitive?.contentOrNull
        }
    }.getOrDefault(emptyList())
}
