package com.paulaizurrategui.urtriply.data.likes

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class LikesRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun addLike(
        tripId: String,
        uid: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val likeData = mapOf(
            "uid" to uid,
            "timestamp" to Timestamp.now()
        )

        db.collection("trips")
            .document(tripId)
            .collection("likes")
            .document(uid)
            .set(likeData)
            .addOnSuccessListener {
                db.collection("trips")
                    .document(tripId)
                    .update("likes", FieldValue.increment(1))
                    .addOnSuccessListener {
                        Log.d("LikesRepository", "Like added")
                    }
                    .addOnFailureListener { e ->
                        Log.e("LikesRepository", "Failed to increment counter", e)
                    }
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("LikesRepository", "Failed to add like", e)
                onError(e)
            }
    }

    fun removeLike(
        tripId: String,
        uid: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("trips")
            .document(tripId)
            .collection("likes")
            .document(uid)
            .delete()
            .addOnSuccessListener {
                db.collection("trips")
                    .document(tripId)
                    .update("likes", FieldValue.increment(-1))
                    .addOnSuccessListener {
                        Log.d("LikesRepository", "Like removed")
                    }
                    .addOnFailureListener { e ->
                        Log.e("LikesRepository", "Failed to decrement counter", e)
                    }
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("LikesRepository", "Failed to remove like", e)
                onError(e)
            }
    }

    fun checkIfLiked(
        tripId: String,
        uid: String,
        onResult: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("trips")
            .document(tripId)
            .collection("likes")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                onResult(doc.exists())
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}
