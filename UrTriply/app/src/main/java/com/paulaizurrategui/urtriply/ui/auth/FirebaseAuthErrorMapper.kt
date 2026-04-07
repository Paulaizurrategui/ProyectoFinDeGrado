package com.paulaizurrategui.urtriply.ui.auth

import androidx.annotation.StringRes
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.paulaizurrategui.urtriply.R

// Mapper para traducir excepciones típicas de FirebaseAuth a mensajes de UI (strings.xml)
object FirebaseAuthErrorMapper {

    // Devuelve un @StringRes (id de recurso de string) para mostrar un mensaje entendible al usuario
    @StringRes
    fun toStringRes(e: Exception?): Int {
        return when (e) {
            is FirebaseNetworkException -> R.string.error_network // Fallo de red / sin conexión / timeout
            is FirebaseAuthInvalidUserException -> R.string.error_invalid_user // Usuario no existe o está deshabilitado
            is FirebaseAuthInvalidCredentialsException -> R.string.error_invalid_credentials // Email mal formado o contraseña incorrecta
            is FirebaseAuthUserCollisionException -> R.string.error_user_collision // Registro: el email ya está en uso
            is FirebaseAuthWeakPasswordException -> R.string.error_weak_password // Registro: contraseña demasiado débil
            else -> R.string.error_generic // Cualquier otro caso no contemplado
        }
    }
}