package com.paulaizurrategui.urtriply.domain.model

/**
 * Modelo de Hotel obtenido de Overpass API (OpenStreetMap)
 *
 * Se usa en la fase de alojamiento para cumplir RF-17 y RF-19:
 * datos reales cuando existan, fallback estimado cuando no.
 */
data class Hotel(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val stars: Int? = null,
    val rating: Double? = null,
    val pricePerNight: Double,
    val totalPrice: Double? = null,
    val bookingUrl: String? = null,
    val isReal: Boolean = false
) {
    override fun toString() = "$name (${stars ?: 0}⭐, €${String.format("%.0f", pricePerNight)}/noche)"
}
