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
import java.util.concurrent.TimeUnit

class FlightsRepository() {
    companion object {
        private const val TAG = "FlightsRepository"
        private const val TEQUILA_BASE = "https://api.tequila.kiwi.com/"
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

    private val tequila: TequilaApi by lazy {
        Retrofit.Builder()
            .baseUrl(TEQUILA_BASE)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TequilaApi::class.java)
    }

    // Amadeus clients
    private val amadeusAuth: AmadeusAuthApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.amadeus.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AmadeusAuthApi::class.java)
    }

    private val amadeusApi: AmadeusApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.amadeus.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AmadeusApi::class.java)
    }

    suspend fun searchFlights(
        origin: String,
        destination: String,
        dateFrom: String,
        dateTo: String,
        limit: Int = 6
    ): List<FlightOffer> {
        return withContext(Dispatchers.IO) {
            // 1) Try Amadeus first (preferred)
            try {
                val amadeusPref = tryAmadeus(origin, destination, dateFrom, dateTo, limit)
                if (amadeusPref.isNotEmpty()) return@withContext amadeusPref
            } catch (e: Throwable) {
                Log.w(TAG, "Amadeus primary attempt failed: ${e.message}")
            }

            // 2) Then try Tequila (if API key present)
            val tequilaApiKey = BuildConfig.TEQUILA_API_KEY.takeIf { it.isNotBlank() }
            if (!tequilaApiKey.isNullOrBlank()) {
                try {
                    // Resolve origin/destination to IATA codes if they look like city names
                    val originCode = origin.takeIf { it.length == 3 && it.all { ch -> ch.isLetterOrDigit() } }
                        ?: resolveLocationCode(tequilaApiKey, origin)
                    val destCode = destination.takeIf { it.length == 3 && it.all { ch -> ch.isLetterOrDigit() } }
                        ?: resolveLocationCode(tequilaApiKey, destination)

                    val resp = tequila.searchFlights(tequilaApiKey, originCode ?: origin, destCode ?: destination, dateFrom, dateTo, null, null, limit)
                    val items = resp.data.orEmpty()
                    val tequilaOffers = items.map { dto ->
                        val routeFirst = dto.route?.firstOrNull()
                        FlightOffer(
                            id = dto.id ?: "tequila_${dto.hashCode()}",
                            origin = routeFirst?.flyFrom ?: origin,
                            destination = routeFirst?.flyTo ?: destination,
                            departureDate = routeFirst?.localDeparture ?: dateFrom,
                            returnDate = dto.route?.lastOrNull()?.localArrival,
                            price = dto.price ?: 0.0,
                            currency = dto.currency ?: "EUR",
                            durationMinutes = dto.duration?.total ?: 0,
                            carrier = routeFirst?.airline ?: "",
                            bookingUrl = dto.deepLink,
                            isReal = true
                        )
                    }

                    if (tequilaOffers.isNotEmpty()) return@withContext tequilaOffers
                } catch (e: Throwable) {
                    Log.w(TAG, "Error querying Tequila API: ${e.message}")
                }
            } else {
                Log.w(TAG, "No Tequila API key provided — skipping Tequila and using Amadeus/stub")
            }

            // 3) fallback to stub
            buildStubOffers(origin, destination, dateFrom, dateTo, limit)
        }
    }

    private suspend fun tryAmadeus(origin: String, destination: String, dateFrom: String, dateTo: String?, limit: Int): List<FlightOffer> {
        val clientId = BuildConfig.AMADEUS_CLIENT_ID.takeIf { it.isNotBlank() }
        val clientSecret = BuildConfig.AMADEUS_CLIENT_SECRET.takeIf { it.isNotBlank() }
        if (clientId.isNullOrBlank() || clientSecret.isNullOrBlank()) return emptyList()

        return try {
            withContext(Dispatchers.IO) {
                val tokenResp = amadeusAuth.getToken("client_credentials", clientId, clientSecret)
                val token = tokenResp.accessToken ?: return@withContext emptyList<FlightOffer>()
                val bearer = "Bearer $token"

                val originCode = origin.takeIf { it.length == 3 } ?: origin
                val destCode = destination.takeIf { it.length == 3 } ?: destination

                val offersResp = amadeusApi.searchOffers(bearer, originCode, destCode, dateFrom, dateTo, 1, limit)
                val offers = offersResp.data.orEmpty()
                offers.mapNotNull { offer ->
                    val priceStr = offer.price?.total
                    val price = priceStr?.toDoubleOrNull() ?: 0.0
                    val currency = offer.price?.currency ?: "EUR"
                    val itinerary = offer.itineraries?.firstOrNull()
                    val carrier = itinerary?.segments?.firstOrNull()?.carrierCode ?: ""
                    val durationMinutes = itinerary?.duration?.let { parseIsoDurationMinutes(it) } ?: 0

                    FlightOffer(
                        id = offer.id ?: "amadeus_${offer.hashCode()}",
                        origin = origin,
                        destination = destination,
                        departureDate = dateFrom,
                        returnDate = dateTo,
                        price = price,
                        currency = currency,
                        durationMinutes = durationMinutes,
                        carrier = carrier,
                        bookingUrl = null,
                        isReal = true
                    )
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Amadeus search failed: ${e.message}")
            emptyList()
        }
    }

    private fun parseIsoDurationMinutes(iso: String): Int {
        // naive parser for formats like PT2H10M
        try {
            var hours = 0
            var minutes = 0
            var s = iso.removePrefix("PT")
            val hIndex = s.indexOf('H')
            if (hIndex >= 0) {
                hours = s.substring(0, hIndex).toIntOrNull() ?: 0
                s = s.substring(hIndex + 1)
            }
            val mIndex = s.indexOf('M')
            if (mIndex >= 0) {
                minutes = s.substring(0, mIndex).toIntOrNull() ?: 0
            }
            return hours * 60 + minutes
        } catch (e: Throwable) {
            return 0
        }
    }

    private suspend fun resolveLocationCode(apiKey: String, term: String): String? {
        return try {
            val resp = tequila.queryLocations(apiKey, term)
            val loc = resp.locations?.firstOrNull()
            loc?.code
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to resolve location code for '$term': ${e.message}")
            null
        }
    }

    private fun buildStubOffers(origin: String, destination: String, dateFrom: String, dateTo: String, limit: Int): List<FlightOffer> {
        val list = mutableListOf<FlightOffer>()
        for (i in 1..limit) {
            list.add(
                FlightOffer(
                    id = "stub_$i",
                    origin = origin,
                    destination = destination,
                    departureDate = dateFrom,
                    returnDate = dateTo,
                    price = 80.0 + i * 45,
                    currency = "EUR",
                    durationMinutes = 90 + i * 15,
                    carrier = listOf("Iberia","Vueling","Ryanair","Air Europa")[i % 4],
                    bookingUrl = null,
                    isReal = false
                )
            )
        }
        return list
    }
}
