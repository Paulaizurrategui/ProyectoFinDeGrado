package com.paulaizurrategui.urtriply.ui.auth

import androidx.annotation.StringRes

// estado que consume la ui (compose)
// aqui guardo loading, errores, exitos y si hay sesion
data class AuthUiState(
    // Flag para mostrar indicador de carga en UI mientras se realizan llamadas a Firebase
    val isLoading: Boolean = false,

    // Recurso de string para errores (nullable): permite mostrar diálogos localizables
    @StringRes val errorResId: Int? = null,

    // Recurso de string para mensajes de éxito (nullable)
    @StringRes val successResId: Int? = null,

    // Indica si hay una sesión iniciada (se inicializa desde FirebaseAuth)
    val isLoggedIn: Boolean = false
)