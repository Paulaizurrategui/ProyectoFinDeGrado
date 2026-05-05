package com.paulaizurrategui.urtriply.domain.model

/**
 * Actividad sugerida para el viaje.
 * Se usa para mostrar actividades reales o fallback estimado en la propuesta.
 */
data class SuggestedActivity(
    val id: String,
    val name: String,
    val category: String,
    val lat: Double,
    val lon: Double,
    val price: Double,
    val isFree: Boolean,
    val bookingUrl: String? = null,
    val isReal: Boolean = false
)
