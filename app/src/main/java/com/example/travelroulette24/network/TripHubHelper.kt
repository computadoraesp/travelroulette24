package com.example.travelroulette24.network

import com.example.travelroulette24.PreferenceHelper
import com.example.travelroulette24.utils.ShortLinkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@Suppress("unused")
object TripHubHelper {
    // Replace with the actual endpoint of your deployed server‑less function
    private const val BASE_URL = "https://YOUR_TRIP_HUB_ENDPOINT"

    /**
     * Creates a new trip‑hub and returns a pair of (hubId, shortUrl).
     * The payload includes offer details, TTL, payment mode, required participant count,
     * and the persistent organizerId.
     */
    suspend fun createHub(
        offerId: String,
        originCity: String,
        hubCodes: Map<String, List<String>>, // e.g. mapOf("airports" to listOf("CDG"))
        ttlHours: Int = 24,
        centralized: Boolean = false,
        requiredCount: Int = 0
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val hubId = UUID.randomUUID().toString().substring(0, 6)
        val expiresAt = Instant.now().plus(Duration.ofHours(ttlHours.toLong())).toString()
        val payload = JSONObject().apply {
            put("offerId", offerId)
            put("originCity", originCity)
            put("hubCodes", JSONObject(hubCodes))
            put("expiresAt", expiresAt)
            put("organizerId", PreferenceHelper.getOrganizerId())
            put("centralized", centralized)
            put("requiredCount", requiredCount)
            put("responses", JSONArray())
        }
        val url = URL("$BASE_URL/create")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 5000
            readTimeout = 5000
        }
        OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
        val responseCode = conn.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
            throw RuntimeException("Failed to create hub: $responseCode")
        }
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val resp = reader.use { it.readText() }
        val json = JSONObject(resp)
        val hubUrl = json.getString("url")
        val shortUrl = ShortLinkHelper.shorten(hubUrl)
        Pair(hubId, shortUrl)
    }

    /**
     * Periodically fetches the hub JSON and emits the list of participant responses.
     * Emits an empty list on error.
     */
    fun pollResponses(hubId: String, intervalMs: Long = 10_000L): Flow<List<HubResponse>> = flow {
        while (true) {
            val url = URL("$BASE_URL/trip/$hubId")
            try {
                val list = withContext(Dispatchers.IO) {
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 5000
                        readTimeout = 5000
                    }
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(conn.inputStream))
                        val resp = reader.use { it.readText() }
                        val json = JSONObject(resp)
                        val respArray = json.optJSONArray("responses") ?: JSONArray()
                        val resultList = mutableListOf<HubResponse>()
                        for (i in 0 until respArray.length()) {
                            val obj = respArray.getJSONObject(i)
                            resultList.add(
                                HubResponse(
                                    nickname = obj.optString("nickname", ""),
                                    answer = obj.optString("answer", "no"),
                                    name = if (obj.isNull("name")) null else obj.getString("name")
                                )
                            )
                        }
                        resultList
                    } else {
                        null
                    }
                }
                emit(list ?: emptyList())
            } catch (_: Exception) {
                emit(emptyList())
            }
            kotlinx.coroutines.delay(intervalMs.milliseconds)
        }
    }

    /**
     * Sends a reply from a participant to the hub.
     */
    @Suppress("unused")
    suspend fun sendReply(
        hubId: String,
        nickname: String,
        answer: String, // "yes" or "no"
        name: String? = null
    ) = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("nickname", nickname)
            put("answer", answer)
            if (name != null) put("name", name)
        }
        val url = URL("$BASE_URL/trip/$hubId/reply")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 5000
            readTimeout = 5000
        }
        OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
        val code = conn.responseCode
        if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_CREATED) {
            throw RuntimeException("Failed to send reply: $code")
        }
    }
}

data class HubResponse(
    val nickname: String,
    val answer: String,
    val name: String?
)
