package com.paulaizurrategui.urtriply.data.remote.overpass

import android.util.Log
import com.paulaizurrategui.urtriply.domain.model.Hotel
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit

class HotelsRepository {

    companion object {
        private const val TAG = "HotelsRepository"
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

    /**
     * Busca hoteles alrededor de una ubicación
     * 
     * @param lat Latitud del destino
     * @param lon Longitud del destino
     * @param radiusKm Radio de búsqueda en km (default: 5km)
     * @return Lista de hoteles encontrados
     */
    suspend fun searchHotels(
        lat: Double,
        lon: Double,
        checkInDate: Long? = null,
        checkOutDate: Long? = null,
        radiusKm: Float = 5f
    ): List<Hotel> {
        // Calcular bounding box desde coords center + radius
        // 1 grado ≈ 111 km, así que radiusKm/111 = grados
        val deltaLat = radiusKm / 111f
        val deltaLon = radiusKm / (111f * kotlin.math.cos(Math.toRadians(lat.toDouble())).toFloat())

        val south = lat - deltaLat
        val north = lat + deltaLat
        val west = lon - deltaLon
        val east = lon + deltaLon

        val query = buildString {
            append("[out:json][timeout:25];(")
            append("node[\"amenity\"=\"hotel\"]($south,$west,$north,$east);")
            append("node[\"tourism\"=\"hotel\"]($south,$west,$north,$east);")
            append(");out body;")
        }

        var lastError: Throwable? = null

        // Reintentar hasta 2 veces
        repeat(2) { attempt ->
            try {
                Log.d(TAG, "Searching hotels (attempt ${attempt + 1}/2) around ($lat, $lon)")
                Log.d(TAG, "Bbox: south=$south, west=$west, north=$north, east=$east")

                val response = api.queryHotels(query)
                Log.d(TAG, "API Response: ${response.elements?.size ?: 0} hotels found")

                val numNights = calculateNights(checkInDate, checkOutDate)

                val hotels = response.elements
                    ?.filter { it.type == "node" && it.lat != null && it.lon != null }
                    ?.take(5)  // Tomar solo los primeros 5
                    ?.mapIndexed { index, element ->
                        val pricePerNight = calculatePricePerNight(element.getRating())
                        Hotel(
                            id = element.id?.toString() ?: "hotel_$index",
                            name = element.getName(),
                            lat = element.lat ?: lat,
                            lon = element.lon ?: lon,
                            stars = element.tags?.get("stars")?.toIntOrNull(),
                            rating = element.getRating(),
                            pricePerNight = pricePerNight,
                            totalPrice = pricePerNight * numNights,
                            bookingUrl = buildBookingUrl(element.getName(), checkInDate, checkOutDate),
                            isReal = true
                        )
                    }
                    ?.sortedByDescending { it.rating }
                    ?: emptyList()

                if (hotels.isEmpty()) {
                    Log.w(TAG, "No hotels returned by API, using fallback list")
                    return buildFallbackHotels(lat, lon, checkInDate, checkOutDate)
                }

                Log.d(TAG, "✅ SUCCESS: Found ${hotels.size} hotels")
                hotels.forEach { Log.d(TAG, "  - ${it.name} (${it.rating?.toFloat() ?: "N/A"}⭐)") }

                return hotels
            } catch (e: Throwable) {
                lastError = e
                Log.e(TAG, "Error searching hotels (attempt ${attempt + 1}/2): ${e.message}")
                if (attempt < 1) {
                    delay(500)
                }
            }
        }

        Log.e(TAG, "❌ FAILED to search hotels", lastError)
        return buildFallbackHotels(lat, lon, checkInDate, checkOutDate)
    }

    /**
     * Calcula un precio estimado por noche basado en rating.
     */
    private fun calculatePricePerNight(rating: Double?): Double {
        return when {
            rating == null -> 80.0
            rating >= 4.5 -> 150.0  // Hotels de lujo
            rating >= 4.0 -> 120.0  // Hotels 4 estrellas
            rating >= 3.0 -> 90.0   // Hotels 3 estrellas
            else -> 60.0            // Hotels económicos
        }
    }

    private fun calculateNights(checkInDate: Long?, checkOutDate: Long?): Int {
        if (checkInDate == null || checkOutDate == null) return 1
        val diffMs = checkOutDate - checkInDate
        return maxOf(1, (diffMs / (1000 * 60 * 60 * 24)).toInt())
    }

    private fun buildBookingUrl(
        hotelName: String,
        checkInDate: Long?,
        checkOutDate: Long?
    ): String {
        val encodedName = java.net.URLEncoder.encode(hotelName, Charsets.UTF_8.name())
        val checkIn = formatDate(checkInDate)
        val checkOut = formatDate(checkOutDate)
        return "https://www.booking.com/searchresults.html?ss=$encodedName&checkin=$checkIn&checkout=$checkOut"
    }

    private fun formatDate(dateMillis: Long?): String {
        if (dateMillis == null) return ""
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(java.util.Date(dateMillis))
    }

    private fun buildFallbackHotels(
        lat: Double,
        lon: Double,
        checkInDate: Long?,
        checkOutDate: Long?
    ): List<Hotel> {
        val numNights = calculateNights(checkInDate, checkOutDate)
        val pricePerNight = 95.0

        return listOf(
            Hotel(
                id = "fallback_1",
                name = "Hotel central estimado",
                lat = lat,
                lon = lon,
                stars = 3,
                rating = 3.8,
                pricePerNight = pricePerNight,
                totalPrice = pricePerNight * numNights,
                bookingUrl = buildBookingUrl("Hotel central estimado", checkInDate, checkOutDate),
                isReal = false
            ),
            Hotel(
                id = "fallback_2",
                name = "Alojamiento recomendado",
                lat = lat + 0.01,
                lon = lon + 0.01,
                stars = 4,
                rating = 4.2,
                pricePerNight = 125.0,
                totalPrice = 125.0 * numNights,
                bookingUrl = buildBookingUrl("Alojamiento recomendado", checkInDate, checkOutDate),
                isReal = false
            )
        )
    }
}
