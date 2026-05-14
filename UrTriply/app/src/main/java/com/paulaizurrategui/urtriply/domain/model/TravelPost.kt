package com.paulaizurrategui.urtriply.domain.model

// modelo que usa la ui para pintar posts de comunidad
data class TravelPost(
    val id: String,
    val authorUid: String = "",
    val destination: String,
    val days: Int,
    val budget: Double,
    val currency: String = "€",

    // datos del autor (para cabecera del post)
    val authorName: String,
    val authorAvatar: String? = null,

    // info extra del post
    val date: String,
    val description: String,
    val imageUrl: String? = null,

    // interacciones
    val likes: Int = 0,
    val comments: Int = 0,
    val isLiked: Boolean = false,
    val isFavorite: Boolean = false
)

// filtros de la pantalla comunidad (busqueda simple)
data class CommunityFilters(
    val destination: String = "",
    val maxBudget: Float? = null
)