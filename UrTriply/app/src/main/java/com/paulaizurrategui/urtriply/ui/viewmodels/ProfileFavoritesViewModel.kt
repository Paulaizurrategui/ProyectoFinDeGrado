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
    // ViewModel que expone listas sencillas de trips para Favoritos y Likes
    // - Usa collectionGroup para escuchar las subcolecciones `favorites` y `likes`
    // - Mantiene listeners para actualizaciones en tiempo real y permite limpiarlos
    // - Provee métodos para eliminar favoritos/likes con actualización optimista
    // Instancias de Firebase para autenticación y Firestore
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Listener registrations para poder quitar los listeners cuando no sean necesarios
    private var likesListener: ListenerRegistration? = null
    private var favoritesListener: ListenerRegistration? = null

    // Generaciones para invalidar resultados antiguos cuando se re-suscribe
    private var likesGeneration = 0
    private var favoritesGeneration = 0

    // StateFlow expuestos hacia la UI con listas simples de viajes
    private val _favorites = MutableStateFlow<List<SimpleTripData>>(emptyList())
    val favorites: StateFlow<List<SimpleTripData>> = _favorites

    private val _likes = MutableStateFlow<List<SimpleTripData>>(emptyList())
    val likes: StateFlow<List<SimpleTripData>> = _likes

    // Flags de carga para indicar estado (mostrar progress spinners en UI)
    private val _favoritesLoading = MutableStateFlow(false)
    val favoritesLoading: StateFlow<Boolean> = _favoritesLoading

    private val _likesLoading = MutableStateFlow(false)
    val likesLoading: StateFlow<Boolean> = _likesLoading

    // Elimina listeners activos y resetea estados/generaciones
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

    // Carga los favoritos del usuario mediante collectionGroup("favorites")
    // Extrae los IDs de los viajes y luego carga detalles con loadTripDetails
    fun loadFavorites() {
        val uid = auth.currentUser?.uid ?: return

        // Reinicio listener previo y marco que estamos cargando
        favoritesListener?.remove()
        favoritesListener = null
        favoritesGeneration++
        _favoritesLoading.value = true

        favoritesListener = db.collectionGroup("favorites")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    // En caso de error, limpio la lista y desactivo loader
                    _favorites.value = emptyList()
                    _favoritesLoading.value = false
                    return@addSnapshotListener
                }

                // Mapear los documentos a los IDs de los viajes (parent.parent)
                val tripIds = snap?.documents
                    ?.mapNotNull { it.reference.parent.parent?.id }
                    ?.distinct()
                    ?: emptySet()

                val generation = favoritesGeneration
                // Cargo detalles de los viajes y solo aplico si la generación coincide
                loadTripDetails(tripIds) { list ->
                    if (generation == favoritesGeneration) {
                        _favorites.value = list
                        _favoritesLoading.value = false
                    }
                }
            }
    }

    // Análogo a loadFavorites pero para "likes"
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

    // Refresca ambos conjuntos
    fun refreshAll() {
        loadFavorites()
        loadLikes()
    }

    // Remove optimistamente un like y realiza la petición a Firestore. Si falla,
    // se restaura el estado previo y se recarga desde Firestore.
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

    // Igual que removeLike pero para favoritos
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

    // Dado un conjunto de tripIds, carga en paralelo cada documento de "trips"
    // y construye una lista de SimpleTripData. Invoca onResult cuando todos
    // los requests han completado.
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
                                destino = tripDoc.getString("destino") ?: tripDoc.getString("destination") ?: "Destino",
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
