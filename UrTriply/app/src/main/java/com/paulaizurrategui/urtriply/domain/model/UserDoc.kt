package com.paulaizurrategui.urtriply.domain.model

// modelo simple de usuario para firestore (/users/{uid})
data class UserDoc(
    val uid: String = "",             // id del usuario (firebase auth)
    val email: String = "",           // email de la cuenta
    val displayName: String = "",      // nombre a mostrar en la app
    val photoUrl: String? = null,       // foto de perfil si existe
    val isOver13Confirmed: Boolean = false,
    val isDarkTheme: Boolean = false,
    val esAdmin: Boolean = false
)