package com.paulaizurrategui.urtriply.domain.model

data class FlightOffer(
    val id: String,
    val origin: String,
    val destination: String,
    val departureDate: String,
    val returnDate: String?,
    val price: Double,
    val currency: String,
    val durationMinutes: Int,
    val carrier: String,
    val bookingUrl: String?,
    val isReal: Boolean = true
)
