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
    val lastSavedTripId: String? = null
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

    fun save(plan: PlanResult, status: TripStatus) {
        val user = auth.currentUser
        if (user == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Necesitas iniciar sesión.")
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, successMessage = null)

        repo.saveTripFromPlan(
            plan = plan,
            authorUid = user.uid,
            authorEmail = user.email,
            status = status,
            onSuccess = { id ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    successMessage = if (status == TripStatus.DRAFT) "Borrador guardado." else "Viaje publicado.",
                    lastSavedTripId = id
                )
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Error al guardar en Firestore."
                )
            }
        )
    }
}