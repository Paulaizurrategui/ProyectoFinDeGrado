package com.paulaizurrategui.urtriply.data.remote.overpass

import android.util.Log
import com.paulaizurrategui.urtriply.domain.model.SuggestedActivity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

class ActivitiesRepository {

    companion object {
        private const val TAG = "ActivitiesRepository"
    }

    private val api: OverpassApi by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "urtriply/1.0 (android)")
                    .build()
                Log.d(TAG, "Request: ${req.url}")
                chain.proceed(req)
            }
            .addInterceptor(logger)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://overpass-api.de/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OverpassApi::class.java)
    }

    suspend fun searchActivities(
        lat: Double,
        lon: Double,
        prefs: Set<String>,
        radiusKm: Float = 5f
    ): List<SuggestedActivity> {
        val deltaLat = radiusKm / 111f
        val deltaLon = radiusKm / (111f * kotlin.math.cos(Math.toRadians(lat)).toFloat())

        val south = lat - deltaLat
        val north = lat + deltaLat
        val west = lon - deltaLon
        val east = lon + deltaLon

        val query = buildString {
            append("[out:json][timeout:25];(")
            append("node[\"tourism\"=\"museum\"]($south,$west,$north,$east);")
            append("node[\"tourism\"=\"attraction\"]($south,$west,$north,$east);")
            append("node[\"tourism\"=\"viewpoint\"]($south,$west,$north,$east);")
            append("node[\"leisure\"=\"park\"]($south,$west,$north,$east);")
            append("node[\"historic\"]($south,$west,$north,$east);")
            append(");out body;")
        }

        var lastError: Throwable? = null

        repeat(2) { attempt ->
            try {
                Log.d(TAG, "Searching activities (attempt ${attempt + 1}/2) around ($lat, $lon)")
                val response = api.queryHotels(query)
                val elements = response.elements.orEmpty()
                Log.d(TAG, "API Response: ${elements.size} activity candidates found")

                val preferredCategories = prefs.map { it.lowercase(Locale.getDefault()) }.toSet()

                val activities = elements
                    .filter { it.type == "node" && it.lat != null && it.lon != null }
                    .mapIndexedNotNull { index, element ->
                        val name = element.getName()
                        val category = resolveCategory(element.tags)
                        if (preferredCategories.isNotEmpty() && !matchesPreferences(category, preferredCategories)) {
                            null
                        } else {
                            val isFree = category in setOf("parque", "mirador", "historico")
                            val price = calculatePrice(category, isFree)
                            SuggestedActivity(
                                id = element.id?.toString() ?: "activity_$index",
                                name = name,
                                category = category,
                                lat = element.lat ?: lat,
                                lon = element.lon ?: lon,
                                price = price,
                                isFree = isFree,
                                bookingUrl = buildSearchUrl(name),
                                isReal = true
                            )
                        }
                    }
                    .distinctBy { it.name }
                    .take(6)

                if (activities.isNotEmpty()) {
                    Log.d(TAG, "✅ SUCCESS: Found ${activities.size} activities")
                    return activities
                }

                Log.w(TAG, "No activities returned by API, using fallback list")
                return buildFallbackActivities(lat, lon, prefs)
            } catch (e: Throwable) {
                lastError = e
                Log.e(TAG, "Error searching activities (attempt ${attempt + 1}/2): ${e.message}")
                if (attempt < 1) delay(500)
            }
        }

        Log.e(TAG, "❌ FAILED to search activities", lastError)
        return buildFallbackActivities(lat, lon, prefs)
    }

    private fun resolveCategory(tags: Map<String, String>?): String {
        val tourism = tags?.get("tourism")
        val leisure = tags?.get("leisure")
        val historic = tags?.containsKey("historic") == true
        return when {
            tourism == "museum" -> "cultura"
            tourism == "attraction" -> "atraccion"
            tourism == "viewpoint" -> "mirador"
            leisure == "park" -> "parque"
            historic -> "historico"
            else -> "general"
        }
    }

    private fun matchesPreferences(category: String, prefs: Set<String>): Boolean {
        if (prefs.isEmpty()) return true
        return when (category) {
            "cultura", "historico", "atraccion" -> prefs.contains("cultura")
            "mirador", "parque" -> prefs.contains("naturaleza")
            else -> true
        }
    }

    private fun calculatePrice(category: String, isFree: Boolean): Double {
        return when {
            isFree -> 0.0
            category == "cultura" -> 18.0
            category == "atraccion" -> 22.0
            category == "historico" -> 12.0
            else -> 15.0
        }
    }

    private fun buildSearchUrl(name: String): String {
        val encoded = URLEncoder.encode(name, Charsets.UTF_8.name())
        return "https://www.google.com/search?q=$encoded"
    }

    private fun buildFallbackActivities(
        lat: Double,
        lon: Double,
        prefs: Set<String>
    ): List<SuggestedActivity> {
        val wantsCulture = prefs.contains("cultura")
        val wantsNature = prefs.contains("naturaleza")
        val wantsNight = prefs.contains("ocio nocturno")
        val wantsFood = prefs.contains("gastronomía") || prefs.contains("gastronomia")

        return buildList {
            if (wantsCulture || prefs.isEmpty()) {
                add(SuggestedActivity("fallback_cultura_1", "Museo principal de la ciudad", "cultura", lat, lon, 18.0, false, buildSearchUrl("Museo principal de la ciudad"), false))
            }
            if (wantsNature || prefs.isEmpty()) {
                add(SuggestedActivity("fallback_naturaleza_1", "Paseo por parque y miradores", "naturaleza", lat + 0.01, lon + 0.01, 0.0, true, buildSearchUrl("Paseo por parque y miradores"), false))
            }
            if (wantsNight || prefs.isEmpty()) {
                add(SuggestedActivity("fallback_ocio_1", "Ruta por zona de ocio nocturno", "ocio nocturno", lat - 0.01, lon - 0.01, 20.0, false, buildSearchUrl("Ruta por zona de ocio nocturno"), false))
            }
            if (wantsFood || prefs.isEmpty()) {
                add(SuggestedActivity("fallback_gastro_1", "Mercado local y comida tipica", "gastronomia", lat + 0.005, lon - 0.005, 14.0, false, buildSearchUrl("Mercado local y comida tipica"), false))
            }
        }.take(6)
    }
}
