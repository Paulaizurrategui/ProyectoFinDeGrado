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

    private val _favorites = MutableStateFlow<List<SimpleTripData>>(emptyList())
    val favorites: StateFlow<List<SimpleTripData>> = _favorites

    private val _likes = MutableStateFlow<List<SimpleTripData>>(emptyList())
    val likes: StateFlow<List<SimpleTripData>> = _likes

    private val _favoritesLoading = MutableStateFlow(false)
    val favoritesLoading: StateFlow<Boolean> = _favoritesLoading

    private val _likesLoading = MutableStateFlow(false)
    val likesLoading: StateFlow<Boolean> = _likesLoading

    fun clearListeners() {
        likesListener?.remove()
        favoritesListener?.remove()
        likesListener = null
        favoritesListener = null
        likesGeneration++
        favoritesGeneration++
        _likesLoading.value = false
        _favoritesLoading.value = false
    }

    fun loadFavorites() {
        val uid = auth.currentUser?.uid ?: return

        favoritesListener?.remove()
        favoritesListener = null
        favoritesGeneration++
        _favoritesLoading.value = true

        favoritesListener = db.collectionGroup("favorites")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _favorites.value = emptyList()
                    _favoritesLoading.value = false
                    return@addSnapshotListener
                }

                val tripIds = snap?.documents
                    ?.mapNotNull { it.reference.parent.parent?.id }
                    ?.distinct()
                    ?: emptySet()

                val generation = favoritesGeneration
                loadTripDetails(tripIds) { list ->
                    if (generation == favoritesGeneration) {
                        _favorites.value = list
                        _favoritesLoading.value = false
                    }
                }
            }
    }

    fun loadLikes() {
        val uid = auth.currentUser?.uid ?: return

        likesListener?.remove()
        likesListener = null
        likesGeneration++
        _likesLoading.value = true

        likesListener = db.collectionGroup("likes")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _likes.value = emptyList()
                    _likesLoading.value = false
                    return@addSnapshotListener
                }

                val tripIds = snap?.documents
                    ?.mapNotNull { it.reference.parent.parent?.id }
                    ?.distinct()
                    ?: emptySet()

                val generation = likesGeneration
                loadTripDetails(tripIds) { list ->
                    if (generation == likesGeneration) {
                        _likes.value = list
                        _likesLoading.value = false
                    }
                }
            }
    }

    fun refreshAll() {
        loadFavorites()
        loadLikes()
    }

    fun removeLike(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        val previousLikes = _likes.value
        _likes.value = previousLikes.filterNot { it.id == tripId }
        db.collection("trips").document(tripId).collection("likes").document(uid)
            .delete()
            .addOnFailureListener {
                _likes.value = previousLikes
                loadLikes()
            }
    }

    fun removeFavorite(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        val previousFavorites = _favorites.value
        _favorites.value = previousFavorites.filterNot { it.id == tripId }
        db.collection("trips").document(tripId).collection("favorites").document(uid)
            .delete()
            .addOnFailureListener {
                _favorites.value = previousFavorites
                loadFavorites()
            }
    }

    private fun loadTripDetails(
        tripIds: Collection<String>,
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

    override fun onCleared() {
        clearListeners()
        super.onCleared()
    }
}
