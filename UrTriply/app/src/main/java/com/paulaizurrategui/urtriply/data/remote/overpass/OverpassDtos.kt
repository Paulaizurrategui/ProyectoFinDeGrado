package com.paulaizurrategui.urtriply.data.remote.overpass

import com.squareup.moshi.Json

/**
 * DTOs para parsear respuesta de Overpass API
 * 
 * Overpass devuelve un JSON con estructura:
 * {
 *   "elements": [
 *     {
 *       "type": "node",
 *       "id": 123456,
 *       "lat": 38.7077507,
 *       "lon": -9.1365919,
 *       "tags": {
 *         "name": "Hotel Lisboa",
 *         "amenity": "hotel",
 *         "stars": "4"
 *       }
 *     }
 *   ]
 * }
 */

data class OverpassResponseDto(
    @Json(name = "elements") val elements: List<OverpassElementDto>?
)

data class OverpassElementDto(
    @Json(name = "type") val type: String?,
    @Json(name = "id") val id: Long?,
    @Json(name = "lat") val lat: Double?,
    @Json(name = "lon") val lon: Double?,
    @Json(name = "tags") val tags: Map<String, String>?
) {
    fun getName(): String = tags?.get("name") ?: "Hotel"
    fun getRating(): Double? {
        val stars = tags?.get("stars")?.toDoubleOrNull()
        val rating = tags?.get("rating")?.toDoubleOrNull()
        return stars ?: rating
    }
}
