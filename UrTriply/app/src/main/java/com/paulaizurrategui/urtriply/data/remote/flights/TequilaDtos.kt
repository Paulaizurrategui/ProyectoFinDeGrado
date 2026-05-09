package com.paulaizurrategui.urtriply.data.remote.flights

import com.squareup.moshi.Json

data class TequilaSearchResponseDto(
    @Json(name = "data") val data: List<TequilaFlightDto>?
)

data class TequilaFlightDto(
    @Json(name = "id") val id: String?,
    @Json(name = "price") val price: Double?,
    @Json(name = "currency") val currency: String?,
    @Json(name = "duration") val duration: TequilaDurationDto?,
    @Json(name = "route") val route: List<TequilaRouteDto>?,
    @Json(name = "deep_link") val deepLink: String?
)

data class TequilaDurationDto(
    @Json(name = "total") val total: Int?
)

data class TequilaRouteDto(
    @Json(name = "cityFrom") val cityFrom: String?,
    @Json(name = "cityTo") val cityTo: String?,
    @Json(name = "flyFrom") val flyFrom: String?,
    @Json(name = "flyTo") val flyTo: String?,
    @Json(name = "local_departure") val localDeparture: String?,
    @Json(name = "local_arrival") val localArrival: String?,
    @Json(name = "airline") val airline: String?
)

data class TequilaLocationsResponseDto(
    @Json(name = "locations") val locations: List<TequilaLocationDto>?
)

data class TequilaLocationDto(
    @Json(name = "id") val id: String?,
    @Json(name = "code") val code: String?,
    @Json(name = "name") val name: String?
)
