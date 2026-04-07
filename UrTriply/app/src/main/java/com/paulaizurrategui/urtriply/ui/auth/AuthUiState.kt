package com.paulaizurrategui.urtriply.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.paulaizurrategui.urtriply.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Estado que consume la UI (Compose) para pintar loading, errores, éxitos y si hay sesión iniciada
data class AuthUiState(
    val isLoading: Boolean = false, // true mientras se está haciendo login/registro/reset en Firebase
    @StringRes val errorResId: Int? = null, // id de string de error para mostrar en un AlertDialog (null = sin error)
    @StringRes val successResId: Int? = null, // id de string de éxito para mostrar en un AlertDialog (null = sin éxito)
    val isLoggedIn: Boolean = false // true si el usuario está autenticado (FirebaseAuth.currentUser != null)
)

// ViewModel encargado de encapsular FirebaseAuth y exponer un StateFlow consumible por la UI
class AuthViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance() // Inyección simple; permite testear o mockear si hiciera falta
) : ViewModel() {

    // StateFlow interno mutable; solo el ViewModel puede modificarlo
    private val _uiState = MutableStateFlow(AuthUiState(isLoggedIn = auth.currentUser != null)) // Inicializa según sesión actual
    // StateFlow público inmutable; las pantallas lo observan con collectAsState()
    val uiState: StateFlow<AuthUiState> = _uiState

    // Limpia mensajes para que no se quede el AlertDialog abierto al recomponer o navegar
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorResId = null, successResId = null)
    }

    // Registro con email/contraseña; onSuccess se llama para navegar cuando Firebase confirma que se creó la cuenta
    fun register(email: String, password: String, onSuccess: () -> Unit) {
        val cleaned = email.trim() // Evita errores típicos por espacios al copiar/pegar
        _uiState.value = _uiState.value.copy(isLoading = true, errorResId = null, successResId = null) // Arranca loading y limpia mensajes

        auth.createUserWithEmailAndPassword(cleaned, password) // Llamada asíncrona a Firebase
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.value = AuthUiState(isLoggedIn = true) // Estado “limpio” con sesión iniciada
                    onSuccess() // Navegación a la pantalla principal
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, // Se termina el loading
                        errorResId = FirebaseAuthErrorMapper.toStringRes(task.exception), // Mapea la excepción a un string amigable
                        successResId = null
                    )
                }
            }
    }

    // Login con email/contraseña; onSuccess se llama para navegar cuando Firebase confirma el login
    fun login(email: String, password: String, onSuccess: () -> Unit) {
        val cleaned = email.trim()
        _uiState.value = _uiState.value.copy(isLoading = true, errorResId = null, successResId = null)

        auth.signInWithEmailAndPassword(cleaned, password) // Llamada asíncrona a Firebase
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.value = AuthUiState(isLoggedIn = true) // Marca sesión activa
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorResId = FirebaseAuthErrorMapper.toStringRes(task.exception),
                        successResId = null
                    )
                }
            }
    }

    // Envía email de recuperación de contraseña; si el email está vacío se muestra un error local sin llamar a Firebase
    fun sendPasswordReset(email: String) {
        val cleaned = email.trim()

        if (cleaned.isBlank()) { // Validación rápida en cliente
            _uiState.value = _uiState.value.copy(
                errorResId = R.string.error_enter_email_reset, // Mensaje de “introduce email”
                successResId = null
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorResId = null, successResId = null)

        auth.sendPasswordResetEmail(cleaned) // Firebase envía un email al usuario si existe
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successResId = R.string.reset_email_sent, // Mensaje de “email enviado”
                        errorResId = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorResId = FirebaseAuthErrorMapper.toStringRes(task.exception),
                        successResId = null
                    )
                }
            }
    }

    // Cierra sesión en Firebase y actualiza estado para que la UI y navegación vuelvan a modo no autenticado
    fun logout() {
        auth.signOut()
        _uiState.value = AuthUiState(isLoggedIn = false)
    }
}