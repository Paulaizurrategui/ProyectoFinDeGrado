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

data class TravelPayoutsDatesResponse(
    @Json(name = "success")
    val success: Boolean? = false,
    @Json(name = "data")
    val data: List<TravelPayoutsDateFlightData>? = emptyList(),
    @Json(name = "error")
    val error: String? = null,
    @Json(name = "currency")
    val currency: String? = null
)

data class TravelPayoutsDateFlightData(
    @Json(name = "origin")
    val origin: String? = null,
    @Json(name = "destination")
    val destination: String? = null,
    @Json(name = "origin_airport")
    val originAirport: String? = null,
    @Json(name = "destination_airport")
    val destinationAirport: String? = null,
    @Json(name = "price")
    val price: Double? = null,
    @Json(name = "airline")
    val airline: String? = null,
    @Json(name = "flight_number")
    val flightNumber: String? = null,
    @Json(name = "departure_at")
    val departureAt: String? = null,
    @Json(name = "return_at")
    val returnAt: String? = null,
    @Json(name = "transfers")
    val transfers: Int? = null,
    @Json(name = "return_transfers")
    val returnTransfers: Int? = null,
    @Json(name = "duration")
    val duration: Int? = null,
    @Json(name = "duration_to")
    val durationTo: Int? = null,
    @Json(name = "duration_back")
    val durationBack: Int? = null,
    @Json(name = "link")
    val link: String? = null
)

data class TravelPayoutsSpecialOffersResponse(
    @Json(name = "success")
    val success: Boolean? = false,
    @Json(name = "data")
    val data: List<TravelPayoutsSpecialOfferFlightData>? = emptyList(),
    @Json(name = "error")
    val error: String? = null,
    @Json(name = "currency")
    val currency: String? = null
)

data class TravelPayoutsSpecialOfferFlightData(
    @Json(name = "airline")
    val airline: String? = null,
    @Json(name = "airline_title")
    val airlineTitle: String? = null,
    @Json(name = "departure_at")
    val departureAt: String? = null,
    @Json(name = "destination")
    val destination: String? = null,
    @Json(name = "destination_airport")
    val destinationAirport: String? = null,
    @Json(name = "flight_number")
    val flightNumber: String? = null,
    @Json(name = "duration")
    val duration: Int? = null,
    @Json(name = "link")
    val link: String? = null,
    @Json(name = "origin")
    val origin: String? = null,
    @Json(name = "origin_airport")
    val originAirport: String? = null,
    @Json(name = "price")
    val price: Double? = null,
    @Json(name = "return_at")
    val returnAt: String? = null,
    @Json(name = "title")
    val title: String? = null
)

data class TravelPayoutsMatrixResponse(
    @Json(name = "success")
    val success: Boolean? = false,
    @Json(name = "data")
    val data: List<TravelPayoutsMatrixFlightData>? = emptyList(),
    @Json(name = "error")
    val error: String? = null,
    @Json(name = "currency")
    val currency: String? = null
)

data class TravelPayoutsMatrixFlightData(
    @Json(name = "origin")
    val origin: String? = null,
    @Json(name = "destination")
    val destination: String? = null,
    @Json(name = "origin_airport")
    val originAirport: String? = null,
    @Json(name = "destination_airport")
    val destinationAirport: String? = null,
    @Json(name = "price")
    val price: Double? = null,
    @Json(name = "airline")
    val airline: String? = null,
    @Json(name = "flight_number")
    val flightNumber: String? = null,
    @Json(name = "departure_at")
    val departureAt: String? = null,
    @Json(name = "return_at")
    val returnAt: String? = null,
    @Json(name = "duration")
    val duration: Int? = null,
    @Json(name = "transfers")
    val transfers: Int? = null,
    @Json(name = "return_transfers")
    val returnTransfers: Int? = null,
    @Json(name = "link")
    val link: String? = null
)

data class TravelPayoutsNearestPlacesResponse(
    @Json(name = "prices")
    val prices: List<TravelPayoutsNearestPlaceFlightData>? = emptyList(),
    @Json(name = "errors")
    val errors: Map<String, Any>? = null,
    @Json(name = "origins")
    val origins: List<String>? = emptyList(),
    @Json(name = "destinations")
    val destinations: List<String>? = emptyList(),
    @Json(name = "currency")
    val currency: String? = null
)

data class TravelPayoutsNearestPlaceFlightData(
    @Json(name = "origin")
    val origin: String? = null,
    @Json(name = "destination")
    val destination: String? = null,
    @Json(name = "origin_airport")
    val originAirport: String? = null,
    @Json(name = "destination_airport")
    val destinationAirport: String? = null,
    @Json(name = "origin_code")
    val originCode: String? = null,
    @Json(name = "destination_code")
    val destinationCode: String? = null,
    @Json(name = "origin_name")
    val originName: String? = null,
    @Json(name = "destination_name")
    val destinationName: String? = null,
    @Json(name = "price")
    val price: Double? = null,
    @Json(name = "airline")
    val airline: String? = null,
    @Json(name = "flight_number")
    val flightNumber: String? = null,
    @Json(name = "departure_at")
    val departureAt: String? = null,
    @Json(name = "return_at")
    val returnAt: String? = null,
    @Json(name = "duration")
    val duration: Int? = null,
    @Json(name = "transfers")
    val transfers: Int? = null,
    @Json(name = "link")
    val link: String? = null
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
