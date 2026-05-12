package com.paulaizurrategui.urtriply.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SimpleTripData(
    val id: String,
    val destino: String,
    val presupuestoTotal: Double
)

class ProfileFavoritesViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var likesListener: ListenerRegistration? = null
    private var favoritesListener: ListenerRegistration? = null
    private var likesGeneration = 0
    private var favoritesGeneration = 0
    private var likesReady = false
    private var favoritesReady = false

    private val _favorites = MutableStateFlow<List<SimpleTripData>>(emptyList())
    val favorites: StateFlow<List<SimpleTripData>> = _favorites

    private val _likes = MutableStateFlow<List<SimpleTripData>>(emptyList())
    val likes: StateFlow<List<SimpleTripData>> = _likes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadFavoritesAndLikes() {
        val uid = auth.currentUser?.uid ?: return

        likesListener?.remove()
        favoritesListener?.remove()
        likesListener = null
        favoritesListener = null
        likesReady = false
        favoritesReady = false
        likesGeneration++
        favoritesGeneration++
        _isLoading.value = true

        likesListener = db.collectionGroup("likes")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snap, e ->
                likesReady = true
                if (e != null) {
                    _likes.value = emptyList()
                    finishLoadingIfReady()
                    return@addSnapshotListener
                }

                val tripIds = snap?.documents
                    ?.mapNotNull { it.reference.parent.parent?.id }
                    ?.toSet()
                    ?: emptySet()

                val generation = likesGeneration
                loadTripDetails(tripIds) { list ->
                    if (generation == likesGeneration) {
                        _likes.value = list
                        finishLoadingIfReady()
                    }
                }
            }

        favoritesListener = db.collectionGroup("favorites")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snap, e ->
                favoritesReady = true
                if (e != null) {
                    _favorites.value = emptyList()
                    finishLoadingIfReady()
                    return@addSnapshotListener
                }

                val tripIds = snap?.documents
                    ?.mapNotNull { it.reference.parent.parent?.id }
                    ?.toSet()
                    ?: emptySet()

                val generation = favoritesGeneration
                loadTripDetails(tripIds) { list ->
                    if (generation == favoritesGeneration) {
                        _favorites.value = list
                        finishLoadingIfReady()
                    }
                }
            }
    }

    fun removeLike(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("trips").document(tripId).collection("likes").document(uid)
            .delete()
    }

    fun removeFavorite(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("trips").document(tripId).collection("favorites").document(uid)
            .delete()
    }

    private fun loadTripDetails(
        tripIds: Set<String>,
        onResult: (List<SimpleTripData>) -> Unit
    ) {
        if (tripIds.isEmpty()) {
            onResult(emptyList())
            return
        }

        val results = mutableListOf<SimpleTripData>()
        var pending = tripIds.size

        tripIds.forEach { tripId ->
            db.collection("trips").document(tripId).get()
                .addOnSuccessListener { tripDoc ->
                    if (tripDoc.exists()) {
                        results.add(
                            SimpleTripData(
                                id = tripDoc.id,
                                destino = tripDoc.getString("destino") ?: "Destino",
                                presupuestoTotal = tripDoc.getDouble("presupuestoTotal") ?: 0.0
                            )
                        )
                    }
                }
                .addOnCompleteListener {
                    pending--
                    if (pending == 0) {
                        onResult(results)
                    }
                }
        }
    }

    private fun finishLoadingIfReady() {
        if (likesReady && favoritesReady) {
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        likesListener?.remove()
        favoritesListener?.remove()
        super.onCleared()
    }
}
