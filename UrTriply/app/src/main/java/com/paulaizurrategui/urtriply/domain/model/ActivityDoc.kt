package com.paulaizurrategui.urtriply.domain.model

import com.google.firebase.Timestamp

data class ActivityDoc(
    val tripId: String = "",
    val name: String = "",
    val category: String = "",
    val priceEUR: Double = 0.0,
    val isGratis: Boolean = false,
    val enlace: String? = null,
    val isReal: Boolean = false,
    val createdAt: Timestamp? = null
)