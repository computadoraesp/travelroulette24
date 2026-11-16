package com.example.travelroulette24.network

import com.example.travelroulette24.PreferenceHelper
import com.example.travelroulette24.utils.ShortLinkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

/**
 * Client for collaborative Trip Hub synchronization (creation, RSVP polling, and participant replies).
 * Requires a user-configured serverless Trip Hub endpoint URL (no hardcoded placeholder domains).
 */
object TripHubHelper {

    private fun normalizeBaseUrl(rawUrl: String): String {
        val cleaned = rawUrl.trim().trimEnd('/')
        require(cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            "Trip Hub URL is not configured"
        }
        return cleaned
    }

    /**
     * Creates a new trip hub and returns a pair of (hubId, shareUrl).
     * Includes the persistent [PreferenceHelper.getOrganizerId].
     */
    suspend fun createHub(
        baseUrl: String,
        offerId: String,
        originCity: String,
        hubCodes: Map<String, List<String>>,
        ttlHours: Int = 24,
        centralized: Boolean = false,
        requiredCount: Int = 0
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val endpoint = normalizeBaseUrl(baseUrl)
        val hubId = UUID.randomUUID().toString().substring(0, 6)
        val expiresAt = Instant.now().plus(Duration.ofHours(ttlHours.toLong())).toString()
        val payload = JSONObject().apply {
            put("hubId", hubId)
            put("offerId", offerId)
            put("originCity", originCity)
            put("hubCodes", JSONObject(hubCodes))
            put("expiresAt", expiresAt)
            put("organizerId", PreferenceHelper.getOrganizerId())
            put("centralized", centralized)
            put("requiredCount", requiredCount)
            put("responses", JSONArray())
        }

        val url = URL("$endpoint/create")
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
            throw RuntimeException("Failed to create hub: HTTP $responseCode")
        }
        val resp = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        val json = JSONObject(resp)
        val resolvedId = json.optString("hubId", hubId).ifBlank { hubId }
        val hubUrl = json.optString("url", "$endpoint/trip/$resolvedId")
        val shortUrl = ShortLinkHelper.shorten(hubUrl)
        Pair(resolvedId, shortUrl)
    }

    /**
     * Periodically fetches the hub JSON and emits the list of participant responses.
     */
    fun pollResponses(
        baseUrl: String,
        hubId: String,
        intervalMs: Long = 10_000L
    ): Flow<List<HubResponse>> = flow {
        val endpoint = try {
            normalizeBaseUrl(baseUrl)
        } catch (_: Exception) {
            emit(emptyList())
            return@flow
        }

        while (true) {
            try {
                val list = withContext<List<HubResponse>?>(Dispatchers.IO) {
                    val url = URL("$endpoint/trip/$hubId")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 5000
                        readTimeout = 5000
                    }
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val resp = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                        val json = JSONObject(resp)
                        val respArray = json.optJSONArray("responses") ?: JSONArray()
                        buildList<HubResponse> {
                            for (i in 0 until respArray.length()) {
                                val obj = respArray.getJSONObject(i)
                                add(
                                    HubResponse(
                                        nickname = obj.optString("nickname", ""),
                                        answer = obj.optString("answer", "no"),
                                        name = if (obj.isNull("name")) null else obj.getString("name")
                                    )
                                )
                            }
                        }
                    } else {
                        null
                    }
                }
                emit(list ?: emptyList())
            } catch (_: Exception) {
                emit(emptyList())
            }
            delay(intervalMs)
        }
    }

    /**
     * Sends a participant RSVP reply ("yes" or "no") to the active hub.
     */
    suspend fun sendReply(
        baseUrl: String,
        hubId: String,
        nickname: String,
        answer: String,
        name: String? = null
    ) = withContext(Dispatchers.IO) {
        val endpoint = normalizeBaseUrl(baseUrl)
        val payload = JSONObject().apply {
            put("nickname", nickname)
            put("answer", answer)
            if (name != null) put("name", name)
        }
        val url = URL("$endpoint/trip/$hubId/reply")
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
            throw RuntimeException("Failed to send reply: HTTP $code")
        }
    }
}

data class HubResponse(
    val nickname: String,
    val answer: String,
    val name: String?
)
