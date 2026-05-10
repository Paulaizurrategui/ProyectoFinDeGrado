package com.paulaizurrategui.urtriply.domain.model

import com.google.firebase.Timestamp

enum class ReportType {
    TRIP, COMMENT
}

enum class ReportStatus {
    OPEN, RESOLVED, DISMISSED
}

data class Report(
    val id: String = "",
    val targetType: String = ReportType.TRIP.name,  // TRIP or COMMENT
    val targetId: String = "",  // tripId or commentId
    val reporterUid: String = "",
    val reporterName: String = "",
    val reason: String = "",  // abuse, spam, inappropriate, etc.
    val description: String = "",
    val createdAt: Timestamp? = null,
    val status: String = ReportStatus.OPEN.name,  // OPEN, RESOLVED, DISMISSED
    val resolvedBy: String? = null,  // admin uid
    val resolution: String? = null  // what action was taken
)
