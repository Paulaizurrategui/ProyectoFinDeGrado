package com.paulaizurrategui.urtriply.domain.model

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val tripId: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorAvatar: String? = null,
    val text: String = "",
    val createdAt: Timestamp? = null,
    val likesCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false
)
