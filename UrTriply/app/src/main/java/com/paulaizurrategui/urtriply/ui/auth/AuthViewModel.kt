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
    fun register(email: String, password: String, onSuccess: () -> Unit) {
        val cleaned = email.trim() // quito espacios por si pega el email con espacios

        // pongo loading y limpio mensajes
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

                    // si por lo que sea no hay uid, corto
                    if (uid == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorResId = R.string.generic_error
                        )
                        return@addOnCompleteListener
                    }

                    // displayname inicial: lo saco del email (antes del @)
                    val defaultDisplayName = cleaned.substringBefore("@").ifBlank { "usuario" }

                    // datos del perfil que guardo en firestore
                    val userMap = mapOf(
                        "uid" to uid,
                        "email" to cleaned,
                        "displayName" to defaultDisplayName,
                        "displayNameLower" to defaultDisplayName.lowercase() // para busquedas sin mayusculas
                    )

                    // merge para no pisar campos si ya existian
                    db.collection("users")
                        .document(uid)
                        .set(userMap, SetOptions.merge())
                        .addOnSuccessListener {
                            // dejo el estado limpio y marco login ok
                            _uiState.value = AuthUiState(isLoggedIn = true)
                            onSuccess()
                        }
                        .addOnFailureListener {
                            // la cuenta se creo, pero fallo firestore
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorResId = R.string.generic_error
                            )
                        }
                } else {
                    // error de firebase auth (email ya usado, password debil, etc)
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

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorResId = null,
            successResId = null
        )

        auth.signInWithEmailAndPassword(cleaned, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid

                    // si no hay uid, algo raro paso
                    if (uid == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorResId = R.string.generic_error
                        )
                        return@addOnCompleteListener
                    }

                    // me aseguro de tener doc en firestore (sin pisar displayname)
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
                            // aunque falle firestore, el login es valido igual
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
}