package com.paulaizurrategui.urtriply.ui.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.paulaizurrategui.urtriply.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(AuthUiState(isLoggedIn = auth.currentUser != null))

    val uiState: StateFlow<AuthUiState> = _uiState

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorResId = null, successResId = null)
    }

    fun register(email: String, password: String, onSuccess: () -> Unit) {
        val cleaned = email.trim()

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorResId = null,
            successResId = null
        )

        auth.createUserWithEmailAndPassword(cleaned, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid

                    if (uid == null) {
                        _uiState.value = _uiState.value.copy(isLoading = false, errorResId = R.string.generic_error)
                        return@addOnCompleteListener
                    }

                    // displayName inicial: parte antes de @ (si luego lo cambias en perfil, se actualizará allí)
                    val defaultDisplayName = cleaned.substringBefore("@").ifBlank { "Usuario" }

                    val userMap = mapOf(
                        "uid" to uid,
                        "email" to cleaned,
                        "displayName" to defaultDisplayName,
                        "displayNameLower" to defaultDisplayName.lowercase()
                    )

                    db.collection("users")
                        .document(uid)
                        .set(userMap, SetOptions.merge())
                        .addOnSuccessListener {
                            _uiState.value = AuthUiState(isLoggedIn = true)
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            // La cuenta existe, pero no se pudo crear perfil en Firestore
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorResId = R.string.generic_error
                            )
                        }
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

        _uiState.value =
            _uiState.value.copy(isLoading = true, errorResId = null, successResId = null)

        auth.signInWithEmailAndPassword(cleaned, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid

                    if (uid == null) {
                        _uiState.value = _uiState.value.copy(isLoading = false, errorResId = R.string.generic_error)
                        return@addOnCompleteListener
                    }

                    // Asegura que el doc existe y al menos tenga email.
                    // No machaca displayName si ya lo has puesto desde perfil.
                    val ensureMap = mapOf(
                        "uid" to uid,
                        "email" to cleaned
                    )

                    db.collection("users")
                        .document(uid)
                        .set(ensureMap, SetOptions.merge())
                        .addOnSuccessListener {
                            _uiState.value = AuthUiState(isLoggedIn = true)
                            onSuccess()
                        }
                        .addOnFailureListener {
                            // Aunque falle Firestore, el login existe. Te dejo entrar igualmente.
                            _uiState.value = AuthUiState(isLoggedIn = true)
                            onSuccess()
                        }
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

        _uiState.value =
            _uiState.value.copy(isLoading = true, errorResId = null, successResId = null)

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