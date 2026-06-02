package com.paulaizurrategui.urtriply.data.remote.google

import android.util.Log
import com.paulaizurrategui.urtriply.domain.model.SuggestedActivity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.net.URLEncoder
import java.util.Locale

// Cliente ligero para usar Google Places (Nearby Search + Place Details).
// Lo uso cuando tengo `BuildConfig.GOOGLE_PLACES_API_KEY` para obtener
// POIs comerciales más fiables que Overpass/Wikipedia.
class GooglePlacesRepository(
    private val apiKey: String
) {
    companion object {
        private const val TAG = "GooglePlacesRepo"
        private const val BASE_URL = "https://maps.googleapis.com/maps/api/place/"
    }

    private val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()
    }

    private val moshi: Moshi by lazy { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }

    private data class PlacesSearchResponse(val results: List<PlaceResult>?)
    private data class PlaceResult(val place_id: String?, val name: String?, val geometry: Geometry?, val types: List<String>?)
    private data class Geometry(val location: Location?)
    private data class Location(val lat: Double?, val lng: Double?)

    private data class PlaceDetailsResponse(val result: PlaceDetails?)
    private data class PlaceDetails(val website: String?, val url: String?, val name: String?, val international_phone_number: String?, val geometry: Geometry?)

    suspend fun searchNearbyAttractions(lat: Double, lon: Double, radiusMeters: Int = 5000, keyword: String? = null): List<SuggestedActivity> {
        // Si no hay apiKey, no intento la búsqueda (caller puede usar fallback)
        if (apiKey.isBlank()) return emptyList()
        try {
            val encodedKeyword = keyword?.let { URLEncoder.encode(it, Charsets.UTF_8.name()) }
            val url = StringBuilder().apply {
                append("${BASE_URL}nearbysearch/json?key=$apiKey&location=$lat,$lon&radius=$radiusMeters")
                append("&type=tourist_attraction|museum|tourist_attraction")
                if (!encodedKeyword.isNullOrBlank()) append("&keyword=$encodedKeyword")
            }.toString()

            val req = Request.Builder().url(url).build()
            val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            val adapter = moshi.adapter(PlacesSearchResponse::class.java)
            val parsed = adapter.fromJson(body) ?: return emptyList()

            val results = parsed.results.orEmpty()
            // Mapear resultados de Places a SuggestedActivity (precio desconocido)
            val list = results.mapNotNull { r ->
                val pid = r.place_id ?: return@mapNotNull null
                val name = r.name ?: return@mapNotNull null
                val latR = r.geometry?.location?.lat ?: lat
                val lonR = r.geometry?.location?.lng ?: lon
                SuggestedActivity(
                    id = "gp_$pid",
                    name = name,
                    category = r.types?.firstOrNull() ?: "atraccion",
                    lat = latR,
                    lon = lonR,
                    price = 0.0,
                    isFree = false,
                    bookingUrl = "https://www.google.com/maps/place/?q=place_id:$pid",
                    isReal = true
                )
            }

            return list.distinctBy { it.name }.take(8)
        } catch (e: Throwable) {
            Log.w(TAG, "Google Places search failed: ${e.message}")
            return emptyList()
        }
    }

    private suspend fun getPlaceDetails(placeId: String): PlaceDetailsResponse? {
        // Request a Place Details para obtener website/url/phone si lo necesito
        if (apiKey.isBlank()) return null
        try {
            val url = "${BASE_URL}details/json?key=$apiKey&place_id=$placeId&fields=name,website,url,international_phone_number,geometry"
            val req = Request.Builder().url(url).build()
            val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val adapter = moshi.adapter(PlaceDetailsResponse::class.java)
            return adapter.fromJson(body)
        } catch (e: Throwable) {
            Log.w(TAG, "Google Places details failed: ${e.message}")
            return null
        }
    }
}
