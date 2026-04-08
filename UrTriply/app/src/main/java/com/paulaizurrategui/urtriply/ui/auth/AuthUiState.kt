package com.paulaizurrategui.urtriply.ui.auth

import androidx.annotation.StringRes


// Estado que consume la UI (Compose) para pintar loading, errores, éxitos y si hay sesión iniciada
data class AuthUiState(
    val isLoading: Boolean = false, // true mientras se está haciendo login/registro/reset en Firebase
    @StringRes val errorResId: Int? = null, // id de string de error para mostrar en un AlertDialog (null = sin error)
    @StringRes val successResId: Int? = null, // id de string de éxito para mostrar en un AlertDialog (null = sin éxito)
    val isLoggedIn: Boolean = false // true si el usuario está autenticado (FirebaseAuth.currentUser != null)
)

