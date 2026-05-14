package com.paulaizurrategui.urtriply.data.remote.overpass

import android.util.Log
import com.paulaizurrategui.urtriply.BuildConfig
import com.paulaizurrategui.urtriply.domain.model.SuggestedActivity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import okhttp3.Request
import com.squareup.moshi.Types

class ActivitiesRepository {
    companion object {
        private const val TAG = "ActivitiesRepository"
        private const val TOTAL_TIMEOUT_MS = 12000L
        private const val PER_ENDPOINT_TIMEOUT_MS = 4500L

        // Mirrors públicos de Overpass para reducir timeouts intermitentes.
        private val OVERPASS_ENDPOINTS = listOf(
            "https://overpass-api.de/",
            "https://overpass.kumi.systems/",
            "https://overpass.openstreetmap.ru/"
        )
    }

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "urtriply/1.0 (android)")
                    .build()
                Log.d(TAG, "Request: ${req.url}")
                chain.proceed(req)
            }
            .addInterceptor(logger)
            // Timeouts cortos por intento para evitar bloqueos largos.
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val apisByEndpoint: Map<String, OverpassApi> by lazy {
        OVERPASS_ENDPOINTS.associateWith { endpoint ->
            Retrofit.Builder()
                .baseUrl(endpoint)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(OverpassApi::class.java)
        }
    }

    // --- Wikipedia geosearch DTOs ---
    private data class WikiGeoSearchResponse(val query: WikiQuery?)
    private data class WikiQuery(val geosearch: List<WikiGeoItem>?)
    private data class WikiGeoItem(val pageid: Long?, val title: String?, val lat: Double?, val lon: Double?)

    private suspend fun fetchWikipediaActivities(lat: Double, lon: Double, prefs: Set<String>, radiusKm: Float): List<SuggestedActivity> {
        try {
            val radiusMeters = (radiusKm * 1000).toInt()
            val lang = Locale.getDefault().language.takeIf { it.isNotBlank() } ?: "en"
            val host = "$lang.wikipedia.org"
            val url = "https://$host/w/api.php?action=query&list=geosearch&gscoord=$lat|$lon&gsradius=$radiusMeters&gslimit=12&format=json"
            val req = Request.Builder().url(url).build()
            val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()

            val adapter = moshi.adapter(WikiGeoSearchResponse::class.java)
            val parsed = adapter.fromJson(body) ?: return emptyList()
            val items = parsed.query?.geosearch.orEmpty()

            val preferredCategories = prefs.map { it.lowercase(Locale.getDefault()) }.toSet()

            val activities = items.mapNotNullIndexed { index, item ->
                val title = item.title ?: return@mapNotNullIndexed null
                val latItem = item.lat ?: lat
                val lonItem = item.lon ?: lon
                val category = guessCategoryFromTitle(title)
                if (preferredCategories.isNotEmpty() && !matchesPreferences(category, preferredCategories)) return@mapNotNullIndexed null

                SuggestedActivity(
                    id = "wiki_${item.pageid ?: index}",
                    name = title,
                    category = category,
                    lat = latItem,
                    lon = lonItem,
                    price = 0.0,
                    isFree = true,
                    bookingUrl = "https://en.wikipedia.org/wiki/${URLEncoder.encode(title, Charsets.UTF_8.name())}",
                    isReal = true
                )
            }.distinctBy { it.name }

            return activities.take(6)
        } catch (e: Throwable) {
            Log.w(TAG, "⚠️ Wikipedia geosearch failed: ${e.message}")
            return emptyList()
        }
    }

    // small helpers
    private fun guessCategoryFromTitle(title: String): String {
        val t = title.lowercase(Locale.getDefault())
        return when {
            t.contains("museum") || t.contains("museo") -> "cultura"
            t.contains("park") || t.contains("parco") -> "parque"
            t.contains("view") || t.contains("mirador") || t.contains("tower") -> "mirador"
            t.contains("cathedral") || t.contains("church") || t.contains("historic") -> "historico"
            t.contains("market") || t.contains("mercado") -> "gastronomia"
            else -> "general"
        }
    }

    // extension: mapNotNullIndexed
    private inline fun <T, R> Iterable<T>.mapNotNullIndexed(transform: (Int, T) -> R?): List<R> {
        val list = ArrayList<R>()
        var index = 0
        for (item in this) {
            val r = transform(index, item)
            if (r != null) list.add(r)
            index++
        }
        return list
    }

    suspend fun searchActivities(
        lat: Double,
        lon: Double,
        prefs: Set<String>,
        radiusKm: Float = 5f
    ): List<SuggestedActivity> {
        // Si hay clave de Google Places, consultamos primero para datos comerciales fiables
        if (BuildConfig.GOOGLE_PLACES_API_KEY.isNotBlank()) {
            try {
                val gp = com.paulaizurrategui.urtriply.data.remote.google.GooglePlacesRepository(BuildConfig.GOOGLE_PLACES_API_KEY)
                val places = gp.searchNearbyAttractions(lat, lon, (radiusKm * 1000).toInt())
                if (places.isNotEmpty()) {
                    Log.d(TAG, "✅ Google Places devolvió ${places.size} actividades; usándolas")
                    return places
                }
            } catch (e: Throwable) {
                Log.w(TAG, "⚠️ Error consultando Google Places: ${e.message}")
            }
        }
        val deltaLat = radiusKm / 111f
        val deltaLon = radiusKm / (111f * kotlin.math.cos(Math.toRadians(lat)).toFloat())

        val south = lat - deltaLat
        val north = lat + deltaLat
        val west = lon - deltaLon
        val east = lon + deltaLon

        // Query con timeout en Overpass; lo combinamos con timeout por mirror local.
        val query = buildString {
            append("[out:json][timeout:7];(")
            append("node[\"tourism\"=\"museum\"]($south,$west,$north,$east);")
            append("node[\"tourism\"=\"attraction\"]($south,$west,$north,$east);")
            append("node[\"tourism\"=\"viewpoint\"]($south,$west,$north,$east);")
            append("node[\"leisure\"=\"park\"]($south,$west,$north,$east);")
            append("node[\"historic\"]($south,$west,$north,$east);")
            append(");out body;")
        }

        val preferredCategories = prefs.map { it.lowercase(Locale.getDefault()) }.toSet()

        // Timeout total moderado: prioriza datos reales, sin comprometer la UX.
        val result = withTimeoutOrNull(TOTAL_TIMEOUT_MS) {
            for (endpoint in OVERPASS_ENDPOINTS) {
                try {
                    Log.d(TAG, "🔄 Buscando actividades alrededor de ($lat, $lon) en $endpoint")
                    val response = withTimeoutOrNull(PER_ENDPOINT_TIMEOUT_MS) {
                        withContext(Dispatchers.IO) {
                            apisByEndpoint.getValue(endpoint).queryHotels(query)
                        }
                    }

                    if (response == null) {
                        Log.w(TAG, "⏱️ Timeout en mirror: $endpoint")
                        continue
                    }

                    val elements = response.elements.orEmpty()
                    Log.d(TAG, "📋 API devolvió ${elements.size} candidatos en $endpoint")

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

                    // Si Overpass devolvió muchos candidatos pero el filtrado por preferencias dejó 0,
                    // intentamos una selección relajada (ignorar preferencias) para aprovechar datos reales.
                    if (activities.isEmpty() && elements.isNotEmpty()) {
                        Log.w(TAG, "⚠️ Ninguna actividad pasó el filtro de preferencias; aplicando selección relajada desde Overpass")
                        val relaxed = elements
                            .filter { (it.lat != null && it.lon != null) }
                            .mapIndexedNotNull { index, element ->
                                val name = element.getName()
                                val category = resolveCategory(element.tags)
                                SuggestedActivity(
                                    id = element.id?.toString() ?: "activity_relaxed_$index",
                                    name = name,
                                    category = category,
                                    lat = element.lat ?: lat,
                                    lon = element.lon ?: lon,
                                    price = calculatePrice(category, category in setOf("parque", "mirador", "historico")),
                                    isFree = category in setOf("parque", "mirador", "historico"),
                                    bookingUrl = buildSearchUrl(name),
                                    isReal = true
                                )
                            }
                            .distinctBy { it.name }
                            .take(6)

                        if (relaxed.isNotEmpty()) {
                            Log.d(TAG, "✅ Selección relajada: retornando ${relaxed.size} actividades reales desde Overpass")
                            return@withTimeoutOrNull relaxed
                        }
                    }

                    if (activities.isNotEmpty()) {
                        Log.d(TAG, "✅ SUCCESS: Encontradas ${activities.size} actividades reales en $endpoint")
                        return@withTimeoutOrNull activities
                    }

                    Log.w(TAG, "⚠️ Mirror respondió sin actividades útiles: $endpoint")
                } catch (e: Throwable) {
                    Log.w(TAG, "⚠️ Error en mirror $endpoint: ${e.message}")
                }
            }

            null
        }

        if (result == null) {
            Log.w(TAG, "⚠️ Usando fallback de actividades tras agotar mirrors/timeout")

            // Intentamos Wikipedia como último recurso para obtener POIs reales.
            try {
                val wiki = fetchWikipediaActivities(lat, lon, preferredCategories, radiusKm)
                if (wiki.isNotEmpty()) {
                    Log.d(TAG, "✅ Wikipedia geosearch devolvió ${wiki.size} actividades reales; usándolas como último recurso")
                    return wiki
                } else {
                    Log.w(TAG, "⚠️ Wikipedia geosearch no devolvió resultados útiles")
                }
            } catch (e: Throwable) {
                Log.w(TAG, "⚠️ Error al consultar Wikipedia: ${e.message}")
            }
        }

        return result ?: buildFallbackActivities(lat, lon, prefs)
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
