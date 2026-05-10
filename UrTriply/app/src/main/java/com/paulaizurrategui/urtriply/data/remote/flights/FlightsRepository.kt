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
            // 1) Try Travelpayouts first (primary, no auth required)
            try {
                val tpOffers = tryTravelPayouts(origin, destination, dateFrom, dateTo, limit)
                if (tpOffers.isNotEmpty()) {
                    Log.i(TAG, "Travelpayouts: found ${tpOffers.size} offers")
                    return@withContext tpOffers
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Travelpayouts attempt failed: ${e.message}")
            }

            // 2) Fallback to stub
            Log.w(TAG, "Travelpayouts failed; returning stub offers")
            buildStubOffers(origin, destination, dateFrom, dateTo, limit)
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
                val resp = travelpayouts.searchCheapFlights(
                    origin = origin,
                    destination = destination,
                    departureAt = dateFrom,
                    returnAt = dateTo,
                    token = token,
                    currency = "EUR",
                    limit = limit
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

                data.entries.take(limit).mapNotNull { (key, flight) ->
                    val price = flight.price ?: return@mapNotNull null
                    val airline = flight.airline ?: "Unknown"
                    FlightOffer(
                        id = "tp_$key",
                        origin = flight.origin ?: origin,
                        destination = flight.destination ?: destination,
                        departureDate = flight.departureAt ?: dateFrom,
                        returnDate = flight.returnAt ?: dateTo,
                        price = price,
                        currency = "EUR",
                        durationMinutes = 0,  // Travelpayouts doesn't provide this
                        carrier = airline,
                        bookingUrl = null,
                        isReal = true
                    )
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Travelpayouts API call failed: ${e.message}")
            emptyList()
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
