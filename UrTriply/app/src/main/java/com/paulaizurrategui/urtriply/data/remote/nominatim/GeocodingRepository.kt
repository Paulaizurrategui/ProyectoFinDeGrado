package com.paulaizurrategui.urtriply.data.remote.nominatim

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

data class GeocodingResult(
    val displayName: String,
    val lat: Double,
    val lon: Double
)

class GeocodingRepository {

    companion object {
        private const val TAG = "GeocodingRepository"
    }

    private val api: NominatimApi by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            // Nominatim requiere identificador en User-Agent; lo incluyo para obedecer política
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "urtriply/1.0 (android)")
                    .build()
                Log.d(TAG, "Request: ${req.url}")
                chain.proceed(req)
            }
            .addInterceptor(logger)
            .build()

        // Crear Moshi con KotlinJsonAdapterFactory para soporte de nulabilidad
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NominatimApi::class.java)
    }

    suspend fun geocode(query: String): GeocodingResult? {
        var lastError: Throwable? = null

        // Intento geocodificar con hasta 3 reintentos para mejorar resiliencia
        // - Log geo attempts para debugging
        // - Si no hay resultados o parsing fallido, reintento
        repeat(3) { attempt ->
            try {
                Log.d(TAG, "Geocoding attempt ${attempt + 1}/3: $query")
                val results = api.search(query = query)
                Log.d(TAG, "API Response: ${results.size} results")
                
                // Tomo el primer resultado (Nominatim ordena por relevancia)
                val first = results.firstOrNull()
                if (first == null) {
                    Log.w(TAG, "No results for: $query")
                    return@repeat
                }
                
                Log.d(TAG, "First result: displayName=${first.displayName}, lat=${first.lat}, lon=${first.lon}")
                
                // Parseo de coordenadas defensivo (puede venir como string)
                val lat = first.lat?.toDoubleOrNull()
                val lon = first.lon?.toDoubleOrNull()

                if (lat == null || lon == null) {
                    Log.w(TAG, "Failed to parse coords - lat: $lat, lon: $lon")
                    return@repeat
                }
                
                // Resultado final: displayName (fallback al query) + coords
                val result = GeocodingResult(
                    displayName = first.displayName ?: query,
                    lat = lat,
                    lon = lon
                )
                Log.d(TAG, " SUCCESS: ${result.displayName} (${result.lat}, ${result.lon})")
                return result
            } catch (e: Throwable) {
                lastError = e
                Log.e(TAG, "Error geocoding '$query' (attempt ${attempt + 1}/3): ${e.message}")
                if (attempt < 2) {
                    delay(150) // Espera antes de reintentar (reducido)
                }
            }
        }
        
        // Si agoté intentos, retorno null para que el caller use fallback
        Log.e(TAG, " FAILED after 3 attempts for '$query'", lastError)
        return null
    }
}