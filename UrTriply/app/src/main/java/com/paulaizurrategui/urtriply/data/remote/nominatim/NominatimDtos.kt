package com.paulaizurrategui.urtriply.data.remote.nominatim

import com.squareup.moshi.Json

data class NominatimSearchItemDto(
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "lat") val lat: String?,
    @Json(name = "lon") val lon: String?
)
