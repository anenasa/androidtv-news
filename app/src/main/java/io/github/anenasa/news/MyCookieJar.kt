package io.github.anenasa.news

import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.io.File

class TxtCookieJar : CookieJar {
    private var cookies = mutableListOf<Cookie>()

    fun loadFile(cookiesFile: File) {
        if (!cookiesFile.exists()) return
        val lines: List<String>
        try {
            lines = cookiesFile.readLines()
        } catch (e: Exception) {
            Log.e(TAG, "Cookie not loaded", e)
            return
        }
        cookies.clear()
        lines.mapIndexedNotNullTo(cookies) { index, line ->
            if (line.startsWith("#") || line.isBlank()) return@mapIndexedNotNullTo null
            val parts = line.split("\t")
            if (parts.size < 7) {
                Log.e(TAG, "cookies.txt line ${index + 1} size too small")
                return@mapIndexedNotNullTo null
            }
            try {
                return@mapIndexedNotNullTo Cookie.Builder()
                    .domain(parts[0].removePrefix("."))
                    .path(parts[2])
                    .apply { if (parts[3].equals("TRUE", true)) secure() }
                    .expiresAt(parts[4].toLong() * 1000)
                    .name(parts[5])
                    .value(parts[6])
                    .build()
            } catch (e: Exception) {
                Log.e(TAG, "cookies.txt line ${index + 1} error", e)
                return@mapIndexedNotNullTo null
            }
        }
    }

    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>
    ) {
        // TODO: Save cookies
        // https://github.com/anenasa/androidtv-news/issues/8
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookies.filter { it.matches(url) }
    }

    companion object {
        const val TAG = "TxtCookieJar"
    }
}
