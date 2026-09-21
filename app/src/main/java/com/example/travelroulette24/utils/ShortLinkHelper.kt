package com.example.travelroulette24.utils

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ShortLinkHelper {
    /**
     * Uses the public shrtco.de API to shorten a long URL.
     * Returns the shortened URL string, or the original URL if the request fails.
     */
    fun shorten(longUrl: String): String {
        return try {
            val encoded = URLEncoder.encode(longUrl, "UTF-8")
            val apiUrl = "https://api.shrtco.de/v2/shorten?url=$encoded"
            val connection = URL(apiUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                // Simple parsing – the JSON looks like {"ok":true,"result":{"short_link":"https://shrtco.de/abcd"...}}
                val shortLinkKey = "\"short_link\":"
                val start = response.indexOf(shortLinkKey)
                if (start != -1) {
                    val afterKey = response.substring(start + shortLinkKey.length).trimStart()
                    // afterKey starts with "\"https://...\""
                    val quoteStart = afterKey.indexOf('"')
                    val quoteEnd = afterKey.indexOf('"', quoteStart + 1)
                    if (quoteStart != -1 && quoteEnd != -1) {
                        return afterKey.substring(quoteStart + 1, quoteEnd)
                    }
                }
                longUrl // fallback
            } else {
                longUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
            longUrl
        }
    }
}
