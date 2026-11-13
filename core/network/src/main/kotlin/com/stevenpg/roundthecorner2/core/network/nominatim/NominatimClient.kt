package com.stevenpg.roundthecorner2.core.network.nominatim

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Minimal client for the public OpenStreetMap Nominatim geocoder.
 *
 * Follows the usage policy (https://operations.osmfoundation.org/policies/nominatim/):
 * - at most **1 request per second** across the whole app (enforced here);
 * - an identifying User-Agent (added by the shared OkHttp client);
 * - no autocomplete-per-keystroke: callers debounce input (see the editor ViewModel).
 */
@Singleton
class NominatimClient @Inject constructor(
    private val httpClient: OkHttpClient,
    @Named(BASE_URL_NAME) private val baseUrl: String,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val rateLimitMutex = Mutex()
    private var lastRequestAtMillis = 0L

    /**
     * Free-text search.
     * @throws IOException on network failure or a non-2xx response.
     */
    suspend fun search(query: String, limit: Int = 8, locale: Locale = Locale.getDefault()): List<NominatimPlaceDto> {
        val url: HttpUrl = baseUrl.toHttpUrl().newBuilder()
            .addPathSegment("search")
            .addQueryParameter("q", query)
            .addQueryParameter("format", "jsonv2")
            .addQueryParameter("addressdetails", "1")
            .addQueryParameter("limit", limit.toString())
            .build()
        val request = Request.Builder()
            .url(url)
            // Results (street and city names) in the user's language where available.
            .header("Accept-Language", locale.toLanguageTag())
            .get()
            .build()

        return rateLimited {
            withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Nominatim HTTP ${response.code}")
                    json.decodeFromString<List<NominatimPlaceDto>>(response.body.string())
                }
            }
        }
    }

    /** Serialises calls and spaces them >= 1 s apart. */
    private suspend fun <T> rateLimited(block: suspend () -> T): T = rateLimitMutex.withLock {
        val wait = lastRequestAtMillis + MIN_INTERVAL_MILLIS - System.currentTimeMillis()
        if (wait > 0) delay(wait)
        try {
            block()
        } finally {
            lastRequestAtMillis = System.currentTimeMillis()
        }
    }

    companion object {
        const val BASE_URL_NAME = "nominatimBaseUrl"
        const val DEFAULT_BASE_URL = "https://nominatim.openstreetmap.org/"
        private const val MIN_INTERVAL_MILLIS = 1_000L
    }
}
