package com.paulaizurrategui.urtriply.data.remote.flights

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// Minimal subset of Tequila (Kiwi) API for search
interface TequilaApi {
    @GET("/v2/search")
    suspend fun searchFlights(
        @Header("apikey") apiKey: String,
        @Query("fly_from") origin: String,
        @Query("fly_to") destination: String,
        @Query("date_from") dateFrom: String,
        @Query("date_to") dateTo: String,
        @Query("return_from") returnFrom: String? = null,
        @Query("return_to") returnTo: String? = null,
        @Query("limit") limit: Int = 6
    ): TequilaSearchResponseDto

    @GET("/locations/query")
    suspend fun queryLocations(
        @Header("apikey") apiKey: String,
        @Query("term") term: String,
        @Query("location_types") locationTypes: String = "city",
        @Query("limit") limit: Int = 1
    ): TequilaLocationsResponseDto
}
