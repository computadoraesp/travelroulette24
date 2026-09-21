package com.example.travelroulette24.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Builds the Retrofit [BackendApi] instance.
 * Base URL points to the TravelRoulette24 backend services.
 */
@Suppress("unused")
object NetworkModule {

    const val BASE_URL = "https://api.travelroulette24.example/"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun createApi(baseUrl: String = BASE_URL, client: OkHttpClient = OkHttpClient()): BackendApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BackendApi::class.java)
}