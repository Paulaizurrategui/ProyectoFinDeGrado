package com.paulaizurrategui.urtriply.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.paulaizurrategui.urtriply.data.trips.TripStatus
import com.paulaizurrategui.urtriply.data.trips.TripsRepository
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Estado que expone el ViewModel a la UI de `PlanResultScreen`.
// - `isSaving`: indica operaciones en curso (save/publish)
// - `errorMessage`/`successMessage`: mensajes para mostrar en diálogos
// - `lastSavedTripId`: id del viaje guardado (si existe)
// - `currentStatus`: estado actual del viaje (DRAFT/PUBLISHED)
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

    // Referencia a Application para acceder a recursos (strings)
    private val app = application

    // FirebaseAuth para comprobar usuario actual y autoría al guardar
    private val auth = FirebaseAuth.getInstance()

    // Repositorio que persiste la entidad Trip en Firestore
    private val repo = TripsRepository()

    // StateFlow expuesto a la UI
    private val _uiState = MutableStateFlow(PlanResultUiState())
    val uiState: StateFlow<PlanResultUiState> = _uiState

    /**
     * Lock extra a prueba de recomposiciones/doble tap.
     * Evita que entren 2 llamadas a saveDraft() antes de que isSaving se propague a la UI.
     */
    // Lock para prevenir doble envío cuando el usuario pulsa varias veces
    // AtomicBoolean funciona bien frente a recomposiciones y hilos.
    private val draftLock = AtomicBoolean(false)

    // Comentarios generales:
    // - `saveDraft(plan)` guarda el plan como borrador si no existe ya uno y el usuario está logueado.
    // - Usa `draftLock` para evitar doble envío por recomposiciones/rapid taps.
    // - `publish(plan)` publica el trip: si existe un borrador, lo marca como PUBLISHED;
    //   si no existe, crea el doc directamente con status PUBLISHED.

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

        // Comprobamos que el usuario esté logueado; si no, mostramos error
        val user = auth.currentUser ?: run {
            _uiState.value = _uiState.value.copy(errorMessage = app.getString(com.paulaizurrategui.urtriply.R.string.login_required_body))
            draftLock.set(false)
            return
        }

        // 2) Si ya está publicado, no guardamos como borrador (operación no permitida)
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

        // Inicio guardado: marco isSaving y limpio mensajes previos
        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, successMessage = null)

        // Llamada asíncrona al repositorio: guardamos como DRAFT y manejamos callbacks
        repo.saveTripFromPlan(
            plan = plan,
            authorUid = user.uid,
            authorEmail = user.email,
            status = TripStatus.DRAFT,
            onSuccess = { id ->
                // Guardado correcto: actualizo el estado con el id devuelto
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    successMessage = app.getString(com.paulaizurrategui.urtriply.R.string.plan_result_draft_saved),
                    lastSavedTripId = id,
                    currentStatus = TripStatus.DRAFT
                )
                draftLock.set(false)
            },
            onError = { e ->
                // Fallo en persistencia: muestro mensaje de error
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: app.getString(com.paulaizurrategui.urtriply.R.string.plan_result_error_save_draft)
                )
                draftLock.set(false)
            }
        )
    }

    fun publish(plan: PlanResult) {
        // Evitar doble click / doble request comprobando isSaving
        if (_uiState.value.isSaving) return

        // Requiere usuario logueado
        val user = auth.currentUser ?: run {
            _uiState.value = _uiState.value.copy(errorMessage = app.getString(com.paulaizurrategui.urtriply.R.string.login_required_body))
            return
        }

        // Si ya está publicado, no hacemos nada extra
        if (_uiState.value.currentStatus == TripStatus.PUBLISHED) {
            _uiState.value = _uiState.value.copy(successMessage = app.getString(com.paulaizurrategui.urtriply.R.string.plan_result_published))
            return
        }

        // Marco el inicio de publicación
        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, successMessage = null)

        val existingId = _uiState.value.lastSavedTripId
        if (!existingId.isNullOrBlank()) {
            // Si existe un borrador previo, lo actualizamos y lo marcamos como PUBLISHED
            repo.publishExistingTrip(
                tripId = existingId,
                onSuccess = {
                    // También actualizamos el PlanResultStore para que la pantalla de
                    // resultados pueda cargar comentarios sobre el tripId
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
            // Si no hay borrador, creamos y lo guardamos como publicado de inicio
            repo.saveTripFromPlan(
                plan = plan,
                authorUid = user.uid,
                authorEmail = user.email,
                status = TripStatus.PUBLISHED,
                onSuccess = { id ->
                    // Guardado correcto: actualizo PlanResultStore y estado
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