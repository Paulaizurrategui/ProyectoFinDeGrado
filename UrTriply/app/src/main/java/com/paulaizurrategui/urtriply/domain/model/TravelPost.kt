package com.paulaizurrategui.urtriply.domain.model

data class TravelPost(
    val id: String,
    val destination: String,
    val days: Int,
    val budget: Double,
    val currency: String = "€",
    val authorName: String,
    val authorAvatar: String? = null,
    val date: String,
    val description: String,
    val imageUrl: String? = null,
    val likes: Int = 0,
    val comments: Int = 0,
    val isLiked: Boolean = false,
    val isFavorite: Boolean = false
)
data class CommunityFilters(
    val destination: String = "",
    val maxBudget: Float? = null
)