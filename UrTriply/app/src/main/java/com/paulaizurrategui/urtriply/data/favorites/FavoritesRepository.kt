package com.paulaizurrategui.urtriply.data.favorites

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FavoritesRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun addFavorite(
        tripId: String,
        uid: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val favoriteData = mapOf(
            "uid" to uid,
            "timestamp" to Timestamp.now()
        )

        db.collection("trips")
            .document(tripId)
            .collection("favorites")
            .document(uid)
            .set(favoriteData)
            .addOnSuccessListener {
                db.collection("trips")
                    .document(tripId)
                    .update("favorites", FieldValue.increment(1))
                    .addOnSuccessListener {
                        Log.d("FavoritesRepository", "Favorite added")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FavoritesRepository", "Failed to increment counter", e)
                    }
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("FavoritesRepository", "Failed to add favorite", e)
                onError(e)
            }
    }

    fun removeFavorite(
        tripId: String,
        uid: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("trips")
            .document(tripId)
            .collection("favorites")
            .document(uid)
            .delete()
            .addOnSuccessListener {
                db.collection("trips")
                    .document(tripId)
                    .update("favorites", FieldValue.increment(-1))
                    .addOnSuccessListener {
                        Log.d("FavoritesRepository", "Favorite removed")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FavoritesRepository", "Failed to decrement counter", e)
                    }
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("FavoritesRepository", "Failed to remove favorite", e)
                onError(e)
            }
    }

    fun checkIfFavorited(
        tripId: String,
        uid: String,
        onResult: (Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("trips")
            .document(tripId)
            .collection("favorites")
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
