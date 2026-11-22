/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Reads which channels a Dispatcharr instance is currently streaming.
 *
 * ## Why this exists
 *
 * When OpenTV points at a Dispatcharr that fronts a provider, the provider's connection limit is
 * enforced by Dispatcharr, per *channel*, not per device. Two people on the same channel cost one
 * line; a third channel costs another. Knowing that from inside the guide turns "this channel just
 * failed" into "two lines are busy — join one of them", which is the difference between an error and
 * an explanation.
 *
 * ## What it deliberately does not touch
 *
 * The status payload includes each channel's *upstream* URL, and for an Xtream provider that URL
 * contains the account's username and password. Those fields are never declared in the DTO below,
 * so they are never parsed, stored or logged. Only the id, the display name and the client count
 * leave this class.
 *
 * The endpoint needs an admin API key; the caller supplies it. A failure returns null and the
 * feature silently disappears — a status probe must never break the guide.
 */
class DispatcharrApi(
    private val http: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) {

    /** One channel that is being streamed right now. */
    data class LiveChannel(
        val channelId: String,
        val name: String,
        val clients: Int,
    )

    /**
     * Active live channels, or null when the instance is unreachable or the key is rejected.
     *
     * Tries the API-key header first and falls back to a bearer token, because Dispatcharr has been
     * configured with both across versions and the cost of being wrong is a silently dead feature.
     */
    suspend fun liveChannels(baseUrl: String, apiKey: String): List<LiveChannel>? =
        withContext(Dispatchers.IO) {
            val root = baseUrl.trim().trimEnd('/')
            if (root.isEmpty() || apiKey.isBlank()) return@withContext null
            val url = "$root/proxy/ts/status"

            fetch(url, "X-API-Key", apiKey)
                ?: fetch(url, "Authorization", "Bearer ${apiKey.trim()}")
        }

    private fun fetch(url: String, headerName: String, headerValue: String): List<LiveChannel>? {
        val request = Request.Builder()
            .url(url)
            .header(headerName, headerValue)
            .header("Accept", "application/json")
            .build()
        return runCatching {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "status HTTP ${response.code}")
                    return null
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) {
                    Log.w(TAG, "status empty body")
                    return null
                }
                val parsed = json.decodeFromString<Status>(body)
                    .channels
                    .filter { it.channelId.isNotBlank() }
                    .map { LiveChannel(it.channelId, it.channelName.ifBlank { "Channel" }, it.clientCount) }
                Log.d(TAG, "status ok: ${parsed.size} channel(s) live")
                parsed
            }
        }.onFailure {
            // Never log the request: the URL is fine, but the response body is not something to
            // splatter into logcat even by accident.
            Log.w(TAG, "Live status request failed (${it.javaClass.simpleName})")
        }.getOrNull()
    }

    @Serializable
    private data class Status(
        val count: Int = 0,
        val channels: List<Entry> = emptyList(),
    )

    /**
     * Only these three fields. `url` (which carries the provider's credentials), `clients`,
     * `buffer_index` and the codec metadata are intentionally absent.
     */
    @Serializable
    private data class Entry(
        @SerialName("channel_id") val channelId: String = "",
        @SerialName("channel_name") val channelName: String = "",
        @SerialName("client_count") val clientCount: Int = 0,
    )

    private companion object {
        const val TAG = "DispatcharrApi"
    }
}
