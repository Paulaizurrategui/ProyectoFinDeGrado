package com.paulaizurrategui.urtriply.ui.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)

class AuthViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(isLoggedIn = auth.currentUser != null))
    val uiState: StateFlow<AuthUiState> = _uiState

    fun register(email: String, password: String, onSuccess: () -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.value = AuthUiState(isLoggedIn = true)
                    onSuccess()
                } else {
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        errorMessage = task.exception?.message ?: "Error al registrarse",
                        isLoggedIn = false
                    )
                }
            }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.value = AuthUiState(isLoggedIn = true)
                    onSuccess()
                } else {
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        errorMessage = task.exception?.message ?: "Error al iniciar sesión",
                        isLoggedIn = false
                    )
                }
            }
    }

    fun logout() {
        auth.signOut()
        _uiState.value = AuthUiState(isLoggedIn = false)
    }
}
