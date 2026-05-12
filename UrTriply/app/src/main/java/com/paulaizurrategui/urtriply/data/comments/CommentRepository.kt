package com.paulaizurrategui.urtriply.data.comments

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.paulaizurrategui.urtriply.domain.model.Comment

class CommentRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun validateTripId(tripId: String, onError: (Exception) -> Unit): Boolean {
        if (tripId.isBlank()) {
            onError(IllegalArgumentException("Trip ID cannot be blank"))
            return false
        }
        return true
    }

    // Add comment to trip
    fun addComment(
        tripId: String,
        authorUid: String,
        authorName: String,
        authorAvatar: String?,
        text: String,
        onSuccess: (commentId: String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (!validateTripId(tripId, onError)) return

        val comment = mapOf(
            "tripId" to tripId,
            "authorUid" to authorUid,
            "authorName" to authorName,
            "authorAvatar" to authorAvatar,
            "text" to text,
            "createdAt" to Timestamp.now(),
            "likesCount" to 0
        )

        db.collection("trips")
            .document(tripId)
            .collection("comments")
            .add(comment)
            .addOnSuccessListener { ref ->
                db.collection("trips")
                    .document(tripId)
                    .update("comments", FieldValue.increment(1))
                    .addOnSuccessListener {
                        onSuccess(ref.id)
                    }
                    .addOnFailureListener { e ->
                        onError(e)
                    }
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    // Get all comments for a trip
    fun getCommentsForTrip(
        tripId: String,
        onSuccess: (List<Comment>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (tripId.isBlank()) {
            onSuccess(emptyList())
            return
        }

        db.collection("trips")
            .document(tripId)
            .collection("comments")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    onError(e)
                    return@addSnapshotListener
                }

                val comments = snap?.documents?.mapNotNull { doc ->
                    Comment(
                        id = doc.id,
                        tripId = doc.getString("tripId") ?: tripId,
                        authorUid = doc.getString("authorUid") ?: "",
                        authorName = doc.getString("authorName") ?: "usuario",
                        authorAvatar = doc.getString("authorAvatar"),
                        text = doc.getString("text") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        likesCount = (doc.getLong("likesCount") ?: 0L).toInt()
                    )
                } ?: emptyList()

                onSuccess(comments)
            }
    }

    // Delete comment (admin or comment author only)
    fun deleteComment(
        tripId: String,
        commentId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (!validateTripId(tripId, onError)) return

        db.collection("trips")
            .document(tripId)
            .collection("comments")
            .document(commentId)
            .delete()
            .addOnSuccessListener {
                db.collection("trips")
                    .document(tripId)
                    .update("comments", FieldValue.increment(-1))
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

    // Update comment
    fun updateComment(
        tripId: String,
        commentId: String,
        newText: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (!validateTripId(tripId, onError)) return

        db.collection("trips")
            .document(tripId)
            .collection("comments")
            .document(commentId)
            .update("text", newText)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}
