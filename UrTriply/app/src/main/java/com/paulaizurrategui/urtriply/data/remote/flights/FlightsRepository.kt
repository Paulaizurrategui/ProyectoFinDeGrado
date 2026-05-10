package com.paulaizurrategui.urtriply.data.remote.flights

import android.util.Log
import com.paulaizurrategui.urtriply.BuildConfig
import com.paulaizurrategui.urtriply.domain.model.FlightOffer
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class FlightsRepository() {
    companion object {
        private const val TAG = "FlightsRepository"
        private const val TRAVELPAYOUTS_BASE = "https://api.travelpayouts.com/"
    }

    private val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logger)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }

    // Travelpayouts API (primary)
    private val travelpayouts: TravelPayoutsApi by lazy {
        Retrofit.Builder()
            .baseUrl(TRAVELPAYOUTS_BASE)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TravelPayoutsApi::class.java)
    }

    suspend fun searchFlights(
        origin: String,
        destination: String,
        dateFrom: String,
        dateTo: String,
        limit: Int = 6
    ): List<FlightOffer> {
        return withContext(Dispatchers.IO) {
            val originCode = normalizeLocationCode(origin, defaultCode = "MAD")
            val destinationCode = normalizeLocationCode(destination, defaultCode = "MAD")
            val departureDate = normalizeApiDate(dateFrom)
            val returnDate = normalizeApiDate(dateTo)

            // 1) Try Travelpayouts real-date route prices first
            try {
                val tpOffers = tryTravelPayouts(originCode, destinationCode, departureDate, returnDate, limit)
                if (tpOffers.isNotEmpty()) {
                    Log.i(TAG, "Travelpayouts dates: found ${tpOffers.size} offers")
                    return@withContext tpOffers
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Travelpayouts dates attempt failed: ${e.message}")
            }

            // 2) Try Travelpayouts special offers for that destination
            try {
                val specialOffers = trySpecialOffers(originCode, destinationCode, limit)
                if (specialOffers.isNotEmpty()) {
                    Log.i(TAG, "Travelpayouts special offers: found ${specialOffers.size} offers")
                    return@withContext specialOffers
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Travelpayouts special offers failed: ${e.message}")
            }

            // 3) Try nearby real fares around the selected dates and airports
            try {
                val matrixOffers = tryWeekMatrix(originCode, destinationCode, departureDate, returnDate, limit)
                if (matrixOffers.isNotEmpty()) {
                    Log.i(TAG, "Travelpayouts week matrix: found ${matrixOffers.size} offers")
                    return@withContext matrixOffers
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Travelpayouts week matrix failed: ${e.message}")
            }

            try {
                val nearbyOffers = tryNearestPlaces(originCode, destinationCode, departureDate, returnDate, limit)
                if (nearbyOffers.isNotEmpty()) {
                    Log.i(TAG, "Travelpayouts nearby places: found ${nearbyOffers.size} offers")
                    return@withContext nearbyOffers
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Travelpayouts nearby places failed: ${e.message}")
            }

            // 4) No inventamos precios: devolvemos vacío si no hay datos reales.
            Log.w(TAG, "No real flight offers found for $originCode -> $destinationCode")
            emptyList()
        }
    }

    private suspend fun tryTravelPayouts(origin: String, destination: String, dateFrom: String, dateTo: String, limit: Int): List<FlightOffer> {
        val token = BuildConfig.TRAVELPAYOUTS_API_TOKEN.takeIf { it.isNotBlank() }
        if (token.isNullOrBlank()) {
            Log.w(TAG, "No Travelpayouts API token provided")
            return emptyList()
        }

        return try {
            withContext(Dispatchers.IO) {
                val resp = travelpayouts.searchFlightsForDates(
                    origin = origin,
                    destination = destination,
                    departureAt = dateFrom,
                    returnAt = dateTo,
                    unique = false,
                    sorting = "price",
                    direct = false,
                    currency = "EUR",
                    limit = limit,
                    page = 1,
                    oneWay = false,
                    token = token,
                    market = "es"
                )

                if (resp.success != true) {
                    Log.w(TAG, "Travelpayouts error: ${resp.error}")
                    return@withContext emptyList()
                }

                val data = resp.data.orEmpty()
                if (data.isEmpty()) {
                    Log.w(TAG, "Travelpayouts returned empty results")
                    return@withContext emptyList()
                }

                data.take(limit).mapNotNullIndexed { index, flight ->
                    val price = flight.price ?: return@mapNotNullIndexed null
                    val airline = flight.airline ?: "Unknown"
                    val link = flight.link?.let { "https://www.aviasales.com$it" }
                    FlightOffer(
                        id = "tp_date_$index",
                        origin = flight.originAirport ?: flight.origin ?: origin,
                        destination = flight.destinationAirport ?: flight.destination ?: destination,
                        departureDate = flight.departureAt ?: dateFrom,
                        returnDate = flight.returnAt ?: dateTo,
                        price = price,
                        currency = "EUR",
                        durationMinutes = flight.duration ?: 0,
                        carrier = airline,
                        bookingUrl = link,
                        isReal = true
                    )
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Travelpayouts API call failed: ${e.message}")
            emptyList()
        }
    }

    private suspend fun trySpecialOffers(origin: String, destination: String, limit: Int): List<FlightOffer> {
        val token = BuildConfig.TRAVELPAYOUTS_API_TOKEN.takeIf { it.isNotBlank() } ?: return emptyList()

        return try {
            withContext(Dispatchers.IO) {
                val resp = travelpayouts.getSpecialOffers(
                    origin = origin,
                    destination = destination,
                    locale = "es",
                    currency = "EUR",
                    market = "es",
                    token = token
                )

                if (resp.success != true) {
                    Log.w(TAG, "Travelpayouts special offers error: ${resp.error}")
                    return@withContext emptyList()
                }

                resp.data.orEmpty().take(limit).mapIndexedNotNull { index, flight ->
                    val price = flight.price ?: return@mapIndexedNotNull null
                    val link = flight.link?.let { "https://www.aviasales.com$it" }
                    FlightOffer(
                        id = "tp_special_$index",
                        origin = flight.originAirport ?: flight.origin ?: origin,
                        destination = flight.destinationAirport ?: flight.destination ?: destination,
                        departureDate = flight.departureAt ?: "",
                        returnDate = flight.returnAt,
                        price = price,
                        currency = resp.currency?.uppercase(Locale.getDefault()) ?: "EUR",
                        durationMinutes = flight.duration ?: 0,
                        carrier = flight.airlineTitle ?: flight.airline ?: "",
                        bookingUrl = link,
                        isReal = true
                    )
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Travelpayouts special offers API call failed: ${e.message}")
            emptyList()
        }
    }

    private suspend fun tryWeekMatrix(origin: String, destination: String, departureDate: String, returnDate: String, limit: Int): List<FlightOffer> {
        val token = BuildConfig.TRAVELPAYOUTS_API_TOKEN.takeIf { it.isNotBlank() } ?: return emptyList()

        return try {
            withContext(Dispatchers.IO) {
                val resp = travelpayouts.getWeekMatrix(
                    origin = origin,
                    destination = destination,
                    departDate = departureDate,
                    returnDate = returnDate,
                    showToAffiliates = true,
                    currency = "EUR",
                    market = "es",
                    token = token
                )

                if (resp.success != true) {
                    Log.w(TAG, "Travelpayouts week matrix error: ${resp.error}")
                    return@withContext emptyList()
                }

                resp.data.orEmpty().take(limit).mapIndexedNotNull { index, flight ->
                    val price = flight.price ?: return@mapIndexedNotNull null
                    val link = flight.link?.let { "https://www.aviasales.com$it" }
                    FlightOffer(
                        id = "tp_week_$index",
                        origin = flight.originAirport ?: flight.origin ?: origin,
                        destination = flight.destinationAirport ?: flight.destination ?: destination,
                        departureDate = flight.departureAt ?: departureDate,
                        returnDate = flight.returnAt ?: returnDate,
                        price = price,
                        currency = resp.currency?.uppercase(Locale.getDefault()) ?: "EUR",
                        durationMinutes = flight.duration ?: 0,
                        carrier = flight.airline ?: "",
                        bookingUrl = link,
                        isReal = true
                    )
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Travelpayouts week matrix API call failed: ${e.message}")
            emptyList()
        }
    }

    private suspend fun tryNearestPlaces(origin: String, destination: String, departureDate: String, returnDate: String, limit: Int): List<FlightOffer> {
        val token = BuildConfig.TRAVELPAYOUTS_API_TOKEN.takeIf { it.isNotBlank() } ?: return emptyList()

        return try {
            withContext(Dispatchers.IO) {
                val resp = travelpayouts.getNearestPlacesMatrix(
                    origin = origin,
                    destination = destination,
                    limit = limit,
                    distance = 1000,
                    flexibility = 3,
                    showToAffiliates = true,
                    currency = "EUR",
                    market = "es",
                    token = token
                )

                val prices = resp.prices.orEmpty()
                if (prices.isEmpty()) {
                    return@withContext emptyList()
                }

                prices.take(limit).mapIndexedNotNull { index, flight ->
                    val price = flight.price ?: return@mapIndexedNotNull null
                    val link = flight.link?.let { "https://www.aviasales.com$it" }
                    FlightOffer(
                        id = "tp_near_$index",
                        origin = flight.originAirport ?: flight.originCode ?: flight.origin ?: origin,
                        destination = flight.destinationAirport ?: flight.destinationCode ?: flight.destination ?: destination,
                        departureDate = flight.departureAt ?: departureDate,
                        returnDate = flight.returnAt ?: returnDate,
                        price = price,
                        currency = resp.currency?.uppercase(Locale.getDefault()) ?: "EUR",
                        durationMinutes = flight.duration ?: 0,
                        carrier = flight.airline ?: "",
                        bookingUrl = link,
                        isReal = true
                    )
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Travelpayouts nearest places API call failed: ${e.message}")
            emptyList()
        }
    }

    private fun normalizeLocationCode(value: String, defaultCode: String): String {
        val raw = value.trim().uppercase(Locale.getDefault())
        if (raw.length == 3 && raw.all { it.isLetter() }) return raw

        return when {
            raw.contains("PAR") || raw.contains("PARÍ") || raw.contains("PARIS") -> "PAR"
            raw.contains("LON") || raw.contains("LOND") -> "LON"
            raw.contains("ROM") || raw.contains("ROME") -> "ROM"
            raw.contains("AMS") || raw.contains("AMSTER") -> "AMS"
            raw.contains("ATH") || raw.contains("ATEN") -> "ATH"
            raw.contains("LIS") || raw.contains("LISBO") -> "LIS"
            raw.contains("BER") || raw.contains("BERL") -> "BER"
            raw.contains("PRG") || raw.contains("PRAG") -> "PRG"
            raw.contains("VIE") || raw.contains("VIEN") -> "VIE"
            raw.contains("DUB") || raw.contains("DUBL") -> "DUB"
            raw.isBlank() -> defaultCode
            else -> raw.take(3).ifBlank { defaultCode }
        }
    }

    private fun normalizeApiDate(value: String): String {
        val candidates = listOf("dd/MM/yyyy", "yyyy-MM-dd")
        for (pattern in candidates) {
            try {
                val input = SimpleDateFormat(pattern, Locale.getDefault())
                input.isLenient = false
                val parsed = input.parse(value)
                if (parsed != null) {
                    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(parsed)
                }
            } catch (_: Throwable) {
            }
        }
        return value
    }

    private inline fun <T, R> List<T>.mapNotNullIndexed(transform: (index: Int, T) -> R?): List<R> {
        val destination = ArrayList<R>(size)
        for (index in indices) {
            val mapped = transform(index, this[index]) ?: continue
            destination.add(mapped)
        }
        return destination
    }
}
