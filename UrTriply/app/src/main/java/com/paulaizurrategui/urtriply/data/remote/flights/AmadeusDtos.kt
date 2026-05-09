package com.paulaizurrategui.urtriply.data.remote.flights

import com.squareup.moshi.Json

data class AmadeusTokenDto(
    @Json(name = "access_token") val accessToken: String?,
    @Json(name = "token_type") val tokenType: String?,
    @Json(name = "expires_in") val expiresIn: Int?
)

data class AmadeusOffersResponseDto(
    @Json(name = "data") val data: List<AmadeusOfferDto>?,
    @Json(name = "meta") val meta: Any?
)

data class AmadeusOfferDto(
    @Json(name = "id") val id: String?,
    @Json(name = "price") val price: AmadeusPriceDto?,
    @Json(name = "itineraries") val itineraries: List<AmadeusItineraryDto>?
)

data class AmadeusPriceDto(
    @Json(name = "total") val total: String?,
    @Json(name = "currency") val currency: String?
)

data class AmadeusItineraryDto(
    @Json(name = "duration") val duration: String?,
    @Json(name = "segments") val segments: List<AmadeusSegmentDto>?
)

data class AmadeusSegmentDto(
    @Json(name = "carrierCode") val carrierCode: String?,
    @Json(name = "departure") val departure: Any?,
    @Json(name = "arrival") val arrival: Any?
)
