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

    // estado observable por las pantallas (login/register)
    private val _uiState =
        MutableStateFlow(AuthUiState(isLoggedIn = auth.currentUser != null))

    val uiState: StateFlow<AuthUiState> = _uiState

    // limpio mensajes cuando el usuario toca inputs o cierra dialog
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorResId = null, successResId = null)
    }

    // registro con firebase auth + creo/actualizo el doc en /users
    fun register(email: String, password: String, isOver13: Boolean, onSuccess: () -> Unit) {
        val cleaned = email.trim() // quito espacios por si pega el email con espacios
        // Marco carga y limpio mensajes previos
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorResId = null,
            successResId = null
        )

        // Creo usuario con Firebase Auth
        auth.createUserWithEmailAndPassword(cleaned, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid

                    // Si por alguna razón no hay uid, abortamos y mostramos error
                    if (uid == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorResId = R.string.generic_error
                        )
                        return@addOnCompleteListener
                    }

                    // Construyo un displayName por defecto desde el email
                    val defaultDisplayName = cleaned.substringBefore("@").ifBlank { "usuario" }

                    // Map con datos mínimos que guardo en /users (merge para no sobrescribir)
                    val userMap = mapOf(
                        "uid" to uid,
                        "email" to cleaned,
                        "displayName" to defaultDisplayName,
                        "displayNameLower" to defaultDisplayName.lowercase(), // para búsquedas
                        "isOver13Confirmed" to isOver13
                    )

                    // Escribo/mergeo el doc del usuario en Firestore
                    db.collection("users")
                        .document(uid)
                        .set(userMap, SetOptions.merge())
                        .addOnSuccessListener {
                            // Registro completo: marco usuario logueado
                            _uiState.value = AuthUiState(isLoggedIn = true)
                            onSuccess()
                        }
                        .addOnFailureListener {
                            // Si falla Firestore tras crear la cuenta, muestro error
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorResId = R.string.generic_error
                            )
                        }
                } else {
                    // Error en Firebase Auth: mapeo excepción a recurso de string
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorResId = FirebaseAuthErrorMapper.toStringRes(task.exception),
                        successResId = null
                    )
                }
            }
    }

    // login con firebase auth + aseguro doc minimo en /users
    fun login(email: String, password: String, onSuccess: () -> Unit) {
        val cleaned = email.trim()
        // Marco carga y limpio mensajes
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorResId = null,
            successResId = null
        )

        // Intento autenticar con Firebase Auth
        auth.signInWithEmailAndPassword(cleaned, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid

                    // Si no hay uid, algo extraño ocurrió
                    if (uid == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorResId = R.string.generic_error
                        )
                        return@addOnCompleteListener
                    }

                    // Aseguro que existe un doc mínimo en /users (merge para no sobrescribir)
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
                            // Si falla Firestore no impide el login
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

    // envia email de recuperar contraseña
    fun sendPasswordReset(email: String) {
        val cleaned = email.trim()

        // valido que haya email
        if (cleaned.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorResId = R.string.error_enter_email_reset,
                successResId = null
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorResId = null,
            successResId = null
        )
        // Envío el email de recuperación y actualizo el estado según resultado
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

    // cierro sesion
    fun logout() {
        auth.signOut()
        _uiState.value = AuthUiState(isLoggedIn = false)
    }

    // verifico si el usuario actual tiene +13 confirmado
    fun isOver13Confirmed(onResult: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onResult(false)
            return
        }

        // Comprueba en Firestore el flag `isOver13Confirmed` en el doc de usuario
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val isConfirmed = doc.getBoolean("isOver13Confirmed") ?: false
                onResult(isConfirmed)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }
}