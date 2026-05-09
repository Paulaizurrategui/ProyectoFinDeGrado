package com.paulaizurrategui.urtriply.data.remote.flights

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AmadeusAuthApi {
    @FormUrlEncoded
    @POST("/v1/security/oauth2/token")
    suspend fun getToken(
        @Field("grant_type") grantType: String = "client_credentials",
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): AmadeusTokenDto
}

interface AmadeusApi {
    @GET("/v2/shopping/flight-offers")
    suspend fun searchOffers(
        @Header("Authorization") bearerToken: String,
        @Query("originLocationCode") origin: String,
        @Query("destinationLocationCode") destination: String,
        @Query("departureDate") departureDate: String,
        @Query("returnDate") returnDate: String? = null,
        @Query("adults") adults: Int = 1,
        @Query("max") max: Int = 6
    ): AmadeusOffersResponseDto
}
