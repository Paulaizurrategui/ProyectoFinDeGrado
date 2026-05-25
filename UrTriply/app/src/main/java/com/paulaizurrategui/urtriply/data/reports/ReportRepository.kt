package com.paulaizurrategui.urtriply.data.reports

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.paulaizurrategui.urtriply.domain.model.Report
import com.paulaizurrategui.urtriply.domain.model.ReportStatus

class ReportRepository {
    private val db = FirebaseFirestore.getInstance()

    // Submit a report (trip or comment)
    fun submitReport(
        targetType: String,  // "TRIP" or "COMMENT"
        targetId: String,    // tripId or commentId
        targetUserUid: String? = null,
        tripId: String? = null,
        commentId: String? = null,
        reporterUid: String,
        reporterName: String,
        reason: String,      // abuse, spam, inappropriate
        description: String,
        onSuccess: (reportId: String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val report = mapOf(
            "targetType" to targetType,
            "targetId" to targetId,
            "targetUserUid" to targetUserUid,
            "tripId" to tripId,
            "commentId" to commentId,
            "reporterUid" to reporterUid,
            "reporterName" to reporterName,
            "reason" to reason,
            "description" to description,
            "createdAt" to Timestamp.now(),
            "status" to ReportStatus.OPEN.name
        )

        db.collection("reports")
            .add(report)
            .addOnSuccessListener { ref ->
                onSuccess(ref.id)
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    // Get all open reports (admin only)
    fun getOpenReports(
        onSuccess: (List<Report>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("reports")
            .whereEqualTo("status", ReportStatus.OPEN.name)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    onError(e)
                    return@addSnapshotListener
                }

                val reports = snap?.documents?.mapNotNull { doc ->
                    Report(
                        id = doc.id,
                        targetType = doc.getString("targetType") ?: "TRIP",
                        targetId = doc.getString("targetId") ?: "",
                        targetUserUid = doc.getString("targetUserUid"),
                        tripId = doc.getString("tripId"),
                        commentId = doc.getString("commentId"),
                        reporterUid = doc.getString("reporterUid") ?: "",
                        reporterName = doc.getString("reporterName") ?: "",
                        reason = doc.getString("reason") ?: "",
                        description = doc.getString("description") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        status = doc.getString("status") ?: ReportStatus.OPEN.name
                    )
                }?.sortedByDescending { it.createdAt?.seconds ?: 0L } ?: emptyList()

                onSuccess(reports)
            }
    }

    // Resolve report (admin only)
    fun resolveReport(
        reportId: String,
        adminUid: String,
        resolution: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("reports")
            .document(reportId)
            .update(mapOf(
                "status" to ReportStatus.RESOLVED.name,
                "resolvedBy" to adminUid,
                "resolution" to resolution
            ))
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    // Dismiss report (admin only)
    fun dismissReport(
        reportId: String,
        adminUid: String,
        reason: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("reports")
            .document(reportId)
            .update(mapOf(
                "status" to ReportStatus.DISMISSED.name,
                "resolvedBy" to adminUid,
                "resolution" to reason
            ))
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    // Delete trip (after report resolution)
    fun deleteTripByAdmin(
        tripId: String,
        adminUid: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("trips")
            .document(tripId)
            .update(mapOf(
                "deleted" to true,
                "deletedBy" to adminUid,
                "deletedAt" to Timestamp.now()
            ))
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    // Delete comment (after report resolution)
    fun deleteCommentByAdmin(
        tripId: String,
        commentId: String,
        adminUid: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("trips")
            .document(tripId)
            .collection("comments")
            .document(commentId)
            .update(mapOf(
                "deleted" to true,
                "deletedBy" to adminUid,
                "deletedAt" to Timestamp.now()
            ))
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    // Block user (admin only)
    fun blockUser(
        adminUid: String,
        userToBlockUid: String,
        reason: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // 1. Add to /users/{adminUid}/blocks/{userToBlockUid}
        val blockData = mapOf(
            "reason" to reason,
            "createdAt" to Timestamp.now()
        )

        db.collection("users").document(adminUid).collection("blocks")
            .document(userToBlockUid)
            .set(blockData)
            .addOnSuccessListener {
                // 2. Add admin to blockedByUserIds array in /users/{userToBlockUid}
                db.collection("users").document(userToBlockUid)
                    .update(mapOf(
                        "blockedByUserIds" to com.google.firebase.firestore.FieldValue.arrayUnion(adminUid)
                    ))
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onError(e)
                    }
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    // Unblock user
    fun unblockUser(
        adminUid: String,
        userToUnblockUid: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // 1. Remove from /users/{adminUid}/blocks/{userToUnblockUid}
        db.collection("users").document(adminUid).collection("blocks")
            .document(userToUnblockUid)
            .delete()
            .addOnSuccessListener {
                // 2. Remove admin from blockedByUserIds array in /users/{userToUnblockUid}
                db.collection("users").document(userToUnblockUid)
                    .update(mapOf(
                        "blockedByUserIds" to com.google.firebase.firestore.FieldValue.arrayRemove(adminUid)
                    ))
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onError(e)
                    }
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    // Check if user is blocked
    fun isUserBlocked(
        userUid: String,
        onSuccess: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("users")
            .document(userUid)
            .get()
            .addOnSuccessListener { doc ->
                val blockedByUserIds = doc.get("blockedByUserIds") as? List<*>
                onSuccess(!blockedByUserIds.isNullOrEmpty())
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}
