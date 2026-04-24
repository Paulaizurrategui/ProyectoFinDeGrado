package com.paulaizurrategui.urtriply.ui.screens

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.paulaizurrategui.urtriply.data.trips.TripStatus
import com.paulaizurrategui.urtriply.data.trips.TripsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PlanResultUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lastSavedTripId: String? = null,
    val currentStatus: TripStatus = TripStatus.DRAFT
)

class PlanResultViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val repo: TripsRepository = TripsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanResultUiState())
    val uiState: StateFlow<PlanResultUiState> = _uiState

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun saveDraft(plan: PlanResult) {
        val user = auth.currentUser ?: run {
            _uiState.value = _uiState.value.copy(errorMessage = "Necesitas iniciar sesión.")
            return
        }

        // Si ya está publicado, no guardamos como borrador
        if (_uiState.value.currentStatus == TripStatus.PUBLISHED) {
            _uiState.value = _uiState.value.copy(successMessage = "Este viaje ya está publicado.")
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, successMessage = null)

        repo.saveTripFromPlan(
            plan = plan,
            authorUid = user.uid,
            authorEmail = user.email,
            status = TripStatus.DRAFT,
            onSuccess = { id ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    successMessage = "Borrador guardado.",
                    lastSavedTripId = id,
                    currentStatus = TripStatus.DRAFT
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Error al guardar borrador."
                )
            }
        )
    }

    fun publish(plan: PlanResult) {
        val user = auth.currentUser ?: run {
            _uiState.value = _uiState.value.copy(errorMessage = "Necesitas iniciar sesión.")
            return
        }

        // B1: si ya está publicado, bloqueamos
        if (_uiState.value.currentStatus == TripStatus.PUBLISHED) {
            _uiState.value = _uiState.value.copy(successMessage = "Ya está publicado.")
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, successMessage = null)

        val existingId = _uiState.value.lastSavedTripId
        if (!existingId.isNullOrBlank()) {
            // B: si ya hay borrador, lo actualizamos a PUBLISHED
            repo.publishExistingTrip(
                tripId = existingId,
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        successMessage = "Viaje publicado.",
                        currentStatus = TripStatus.PUBLISHED
                    )
                },
                onError = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Error al publicar."
                    )
                }
            )
        } else {
            // Si no hay borrador previo, creamos directamente publicado
            repo.saveTripFromPlan(
                plan = plan,
                authorUid = user.uid,
                authorEmail = user.email,
                status = TripStatus.PUBLISHED,
                onSuccess = { id ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        successMessage = "Viaje publicado.",
                        lastSavedTripId = id,
                        currentStatus = TripStatus.PUBLISHED
                    )
                },
                onError = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Error al publicar."
                    )
                }
            )
        }
    }
}