package com.paulaizurrategui.urtriply.ui.auth

import androidx.annotation.StringRes

// estado que consume la ui (compose)
// aqui guardo loading, errores, exitos y si hay sesion
data class AuthUiState(
    val isLoading: Boolean = false,            // true mientras firebase esta trabajando
    @StringRes val errorResId: Int? = null,    // string id para mostrar error (dialog)
    @StringRes val successResId: Int? = null,  // string id para mostrar aviso/exito (dialog)
    val isLoggedIn: Boolean = false            // true si hay usuario en firebaseauth
)