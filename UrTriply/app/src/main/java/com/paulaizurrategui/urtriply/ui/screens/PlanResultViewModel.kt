package com.paulaizurrategui.urtriply.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.paulaizurrategui.urtriply.data.trips.TripStatus
import com.paulaizurrategui.urtriply.data.trips.TripsRepository
import java.util.concurrent.atomic.AtomicBoolean
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
    application: Application
) : AndroidViewModel(application) {

    private val app = application
    private val auth = FirebaseAuth.getInstance()
    private val repo = TripsRepository()

    private val _uiState = MutableStateFlow(PlanResultUiState())
    val uiState: StateFlow<PlanResultUiState> = _uiState

    /**
     * Lock extra a prueba de recomposiciones/doble tap.
     * Evita que entren 2 llamadas a saveDraft() antes de que isSaving se propague a la UI.
     */
    private val draftLock = AtomicBoolean(false)

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun saveDraft(plan: PlanResult) {
        // 0) Lock duro: evita doble ejecución aunque el usuario pulse 2 veces muy rápido
        if (!draftLock.compareAndSet(false, true)) return

        // 1) También bloqueamos si ya está guardando por estado
        if (_uiState.value.isSaving) {
            draftLock.set(false)
            return
        }

        val user = auth.currentUser ?: run {
            _uiState.value = _uiState.value.copy(errorMessage = app.getString(com.paulaizurrategui.urtriply.R.string.login_required_body))
            draftLock.set(false)
            return
        }

        // 2) Si ya está publicado, no guardamos como borrador
        if (_uiState.value.currentStatus == TripStatus.PUBLISHED) {
            _uiState.value = _uiState.value.copy(successMessage = app.getString(com.paulaizurrategui.urtriply.R.string.plan_result_published))
            draftLock.set(false)
            return
        }

        // 3) Si ya hay un borrador guardado, NO creamos otro (evita duplicados)
        val existingId = _uiState.value.lastSavedTripId
        if (!existingId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(successMessage = app.getString(com.paulaizurrategui.urtriply.R.string.plan_result_draft_already_saved))
            draftLock.set(false)
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
                    successMessage = app.getString(com.paulaizurrategui.urtriply.R.string.plan_result_draft_saved),
                    lastSavedTripId = id,
                    currentStatus = TripStatus.DRAFT
                )
                draftLock.set(false)
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: app.getString(com.paulaizurrategui.urtriply.R.string.plan_result_error_save_draft)
                )
                draftLock.set(false)
            }
        )
    }

    fun publish(plan: PlanResult) {
        // Evitar doble click / doble request
        if (_uiState.value.isSaving) return

        val user = auth.currentUser ?: run {
            _uiState.value = _uiState.value.copy(errorMessage = app.getString(com.paulaizurrategui.urtriply.R.string.login_required_body))
            return
        }

        // Si ya está publicado, bloqueamos
        if (_uiState.value.currentStatus == TripStatus.PUBLISHED) {
            _uiState.value = _uiState.value.copy(successMessage = app.getString(com.paulaizurrategui.urtriply.R.string.plan_result_published))
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, successMessage = null)

        val existingId = _uiState.value.lastSavedTripId
        if (!existingId.isNullOrBlank()) {
            // Si ya hay borrador, lo actualizamos a PUBLISHED
            repo.publishExistingTrip(
                tripId = existingId,
                onSuccess = {
                    // Update tripId in PlanResultStore so comments can be loaded
                    PlanResultStore.lastResult = PlanResultStore.lastResult?.copy(tripId = existingId)
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        successMessage = app.getString(com.paulaizurrategui.urtriply.R.string.plan_result_published),
                        currentStatus = TripStatus.PUBLISHED
                    )
                },
                onError = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = e.message ?: app.getString(com.paulaizurrategui.urtriply.R.string.plan_result_error_publish)
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
                    // Update tripId in PlanResultStore so comments can be loaded
                    PlanResultStore.lastResult = PlanResultStore.lastResult?.copy(tripId = id)
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        successMessage = app.getString(com.paulaizurrategui.urtriply.R.string.plan_result_published),
                        lastSavedTripId = id,
                        currentStatus = TripStatus.PUBLISHED
                    )
                },
                onError = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = e.message ?: app.getString(com.paulaizurrategui.urtriply.R.string.plan_result_error_publish)
                    )
                }
            )
        }
    }
}