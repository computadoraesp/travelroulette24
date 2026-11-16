package com.example.travelroulette24.utils

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ShortLinkHelper {
    /**
     * Uses TinyURL's zero-key plain-text endpoint to shorten a long URL.
     * Returns the shortened URL string, or the original [longUrl] if the request fails.
     */
    fun shorten(longUrl: String): String {
        if (longUrl.isBlank()) return longUrl
        return try {
            val encoded = URLEncoder.encode(longUrl, "UTF-8")
            val apiUrl = "https://tinyurl.com/api-create.php?url=$encoded"
            val connection = URL(apiUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val shortened = BufferedReader(InputStreamReader(connection.inputStream))
                    .use { it.readText() }
                    .trim()
                if (shortened.startsWith("http://") || shortened.startsWith("https://")) {
                    shortened
                } else {
                    longUrl
                }
            } else {
                longUrl
            }
        } catch (_: Exception) {
            longUrl
        }
    }
}
