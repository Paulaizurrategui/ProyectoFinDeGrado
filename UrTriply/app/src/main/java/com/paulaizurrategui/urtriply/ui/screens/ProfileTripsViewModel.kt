package com.paulaizurrategui.urtriply.ui.screens

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.paulaizurrategui.urtriply.data.trips.TripStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class TripListItem(
    val id: String,
    val destino: String,
    val status: TripStatus,
    val createdAt: Timestamp? = null,
    val publishedAt: Timestamp? = null
)

data class ProfileTripsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val drafts: List<TripListItem> = emptyList(),
    val published: List<TripListItem> = emptyList()
)

class ProfileTripsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val trips = db.collection("trips")

    private val _uiState = MutableStateFlow(ProfileTripsUiState(isLoading = true))
    val uiState: StateFlow<ProfileTripsUiState> = _uiState

    fun loadMyTrips() {
        val uid = auth.currentUser?.uid ?: run {
            _uiState.value = ProfileTripsUiState(
                isLoading = false,
                errorMessage = "Necesitas iniciar sesión."
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        // Borradores
        trips.whereEqualTo("authorUid", uid)
            .whereEqualTo("status", TripStatus.DRAFT.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapDrafts ->
                val drafts = snapDrafts.documents.map { d ->
                    TripListItem(
                        id = d.id,
                        destino = d.getString("destino") ?: "(sin destino)",
                        status = TripStatus.DRAFT,
                        createdAt = d.getTimestamp("createdAt"),
                        publishedAt = d.getTimestamp("publishedAt")
                    )
                }

                // Publicados
                trips.whereEqualTo("authorUid", uid)
                    .whereEqualTo("status", TripStatus.PUBLISHED.name)
                    .orderBy("publishedAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { snapPublished ->
                        val published = snapPublished.documents.map { d ->
                            TripListItem(
                                id = d.id,
                                destino = d.getString("destino") ?: "(sin destino)",
                                status = TripStatus.PUBLISHED,
                                createdAt = d.getTimestamp("createdAt"),
                                publishedAt = d.getTimestamp("publishedAt")
                            )
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            drafts = drafts,
                            published = published
                        )
                    }
                    .addOnFailureListener { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Error cargando publicaciones."
                        )
                    }
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Error cargando borradores."
                )
            }
    }

    fun publishTrip(tripId: String) {
        // Solo para asegurarnos de que hay sesión (las reglas validan autor)
        auth.currentUser?.uid ?: run {
            _uiState.value = _uiState.value.copy(errorMessage = "Necesitas iniciar sesión.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        trips.document(tripId)
            .update(
                mapOf(
                    "status" to TripStatus.PUBLISHED.name,
                    "publishedAt" to Timestamp.now()
                )
            )
            .addOnSuccessListener {
                loadMyTrips()
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "No se pudo publicar."
                )
            }
    }

    fun deleteTrip(tripId: String) {
        auth.currentUser?.uid ?: run {
            _uiState.value = _uiState.value.copy(errorMessage = "Necesitas iniciar sesión.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        // Reglas ya se encargan de validar que sea el autor.
        trips.document(tripId)
            .delete()
            .addOnSuccessListener {
                loadMyTrips()
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "No se pudo borrar el viaje."
                )
            }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}