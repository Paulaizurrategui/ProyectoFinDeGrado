package com.paulaizurrategui.urtriply.data.remote.overpass

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API para Overpass (OpenStreetMap)
 * 
 * Documentación: https://overpass-api.de/
 * 
 * Query structure:
 * [bbox=south,west,north,east];(node[amenity=hotel];);out;
 */
interface OverpassApi {
    /**
     * Busca hoteles en un bounding box
     * 
     * @param south Latitud sur
     * @param west Longitud oeste
     * @param north Latitud norte
     * @param east Longitud este
     */
    @GET("api/interpreter")
    suspend fun queryHotels(
        @Query("data") query: String
    ): OverpassResponseDto
}
