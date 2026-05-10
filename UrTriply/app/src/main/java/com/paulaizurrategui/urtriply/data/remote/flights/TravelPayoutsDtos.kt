package com.paulaizurrategui.urtriply.data.remote.flights

import com.squareup.moshi.Json

data class TravelPayoutsResponse(
    @Json(name = "success")
    val success: Boolean? = false,
    @Json(name = "data")
    val data: Map<String, TravelPayoutsFlightData>? = emptyMap(),
    @Json(name = "error")
    val error: String? = null
)

data class TravelPayoutsFlightData(
    @Json(name = "origin")
    val origin: String? = null,
    @Json(name = "destination")
    val destination: String? = null,
    @Json(name = "departure_at")
    val departureAt: String? = null,
    @Json(name = "return_at")
    val returnAt: String? = null,
    @Json(name = "value")
    val price: Double? = null,
    @Json(name = "airline")
    val airline: String? = null,
    @Json(name = "flight_number")
    val flightNumber: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null
)
