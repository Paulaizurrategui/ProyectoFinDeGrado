package com.paulaizurrategui.urtriply.data.remote.flights

import retrofit2.http.GET
import retrofit2.http.Query

interface TravelPayoutsApi {
    /**
     * Real flight tickets for specific dates.
     * Recommended replacement for legacy cheap/direct endpoints.
     */
    @GET("aviasales/v3/prices_for_dates")
    suspend fun searchFlightsForDates(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("departure_at") departureAt: String,
        @Query("return_at") returnAt: String? = null,
        @Query("unique") unique: Boolean = false,
        @Query("sorting") sorting: String = "price",
        @Query("direct") direct: Boolean = false,
        @Query("cy") currency: String = "EUR",
        @Query("limit") limit: Int = 30,
        @Query("page") page: Int = 1,
        @Query("one_way") oneWay: Boolean = false,
        @Query("token") token: String,
        @Query("market") market: String = "es"
    ): TravelPayoutsDatesResponse

    /**
     * Extra route suggestions from the selected destination.
     */
    @GET("aviasales/v3/get_special_offers")
    suspend fun getSpecialOffers(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("locale") locale: String = "es",
        @Query("currency") currency: String = "EUR",
        @Query("market") market: String = "es",
        @Query("token") token: String
    ): TravelPayoutsSpecialOffersResponse

    /**
     * Week matrix around the selected dates.
     */
    @GET("v2/prices/week-matrix")
    suspend fun getWeekMatrix(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("depart_date") departDate: String,
        @Query("return_date") returnDate: String,
        @Query("show_to_affiliates") showToAffiliates: Boolean = true,
        @Query("currency") currency: String = "EUR",
        @Query("market") market: String = "es",
        @Query("token") token: String
    ): TravelPayoutsMatrixResponse

    /**
     * Nearby airports / cities with real cached results.
     */
    @GET("v2/prices/nearest-places-matrix")
    suspend fun getNearestPlacesMatrix(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("limit") limit: Int = 5,
        @Query("distance") distance: Int = 1000,
        @Query("flexibility") flexibility: Int = 3,
        @Query("show_to_affiliates") showToAffiliates: Boolean = true,
        @Query("currency") currency: String = "EUR",
        @Query("market") market: String = "es",
        @Query("token") token: String
    ): TravelPayoutsNearestPlacesResponse
}
