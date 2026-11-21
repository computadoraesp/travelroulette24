package com.example.travelroulette24.data.byok

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Shared HTTP client, JSON parser, and URL encoding helper for all BYOK travel services.
 */
internal object ByokHttpClient {

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val json: Json = Json { ignoreUnknownKeys = true }

    fun encodeParam(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
