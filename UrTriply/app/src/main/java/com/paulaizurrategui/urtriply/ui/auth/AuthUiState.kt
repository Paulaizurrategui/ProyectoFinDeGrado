package com.paulaizurrategui.urtriply.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.paulaizurrategui.urtriply.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AuthUiState(
    val isLoading: Boolean = false,
    @StringRes val errorResId: Int? = null,
    @StringRes val successResId: Int? = null,
    val isLoggedIn: Boolean = false
)

class AuthViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(isLoggedIn = auth.currentUser != null))
    val uiState: StateFlow<AuthUiState> = _uiState

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorResId = null, successResId = null)
    }

    fun register(email: String, password: String, onSuccess: () -> Unit) {
        val cleaned = email.trim()
        _uiState.value = _uiState.value.copy(isLoading = true, errorResId = null, successResId = null)

        auth.createUserWithEmailAndPassword(cleaned, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.value = AuthUiState(isLoggedIn = true)
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

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        val cleaned = email.trim()
        _uiState.value = _uiState.value.copy(isLoading = true, errorResId = null, successResId = null)

        auth.signInWithEmailAndPassword(cleaned, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.value = AuthUiState(isLoggedIn = true)
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

    fun sendPasswordReset(email: String) {
        val cleaned = email.trim()

        if (cleaned.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorResId = R.string.error_enter_email_reset,
                successResId = null
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorResId = null, successResId = null)

        auth.sendPasswordResetEmail(cleaned)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successResId = R.string.reset_email_sent,
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

    fun logout() {
        auth.signOut()
        _uiState.value = AuthUiState(isLoggedIn = false)
    }
}