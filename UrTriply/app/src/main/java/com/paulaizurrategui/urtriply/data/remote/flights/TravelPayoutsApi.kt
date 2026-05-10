package com.paulaizurrategui.urtriply.data.remote.flights

import retrofit2.http.GET
import retrofit2.http.Query

interface TravelPayoutsApi {
    /**
     * Search for cheap flights
     * API: https://api.travelpayouts.com/v1/prices/cheap
     * 
     * @param origin IATA code of departure city
     * @param destination IATA code of arrival city
     * @param departureAt Date in format YYYY-MM-DD
     * @param returnAt Optional return date in format YYYY-MM-DD (for round trips)
     * @param token Your Travelpayouts API token
     * @param currency Currency code (USD, EUR, etc.)
     * @param limit Max results to return
     */
    @GET("v1/prices/cheap")
    suspend fun searchCheapFlights(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("departure_at") departureAt: String,
        @Query("return_at") returnAt: String? = null,
        @Query("token") token: String,
        @Query("currency") currency: String = "EUR",
        @Query("limit") limit: Int = 10
    ): TravelPayoutsResponse
}
