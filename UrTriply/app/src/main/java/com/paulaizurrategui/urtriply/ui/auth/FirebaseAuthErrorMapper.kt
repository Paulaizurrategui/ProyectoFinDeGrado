package com.paulaizurrategui.urtriply.ui.auth

import androidx.annotation.StringRes
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.paulaizurrategui.urtriply.R

// mapper de errores de firebase auth a strings de la ui
object FirebaseAuthErrorMapper {

    // devuelve el id del string segun la excepcion
    @StringRes
    fun toStringRes(e: Exception?): Int {
        // Mapeo simple y explícito de excepciones Firebase a recursos de string
        // Esto permite mostrar mensajes localizables en la UI según el error.
        return when (e) {
            is FirebaseNetworkException -> R.string.error_network                // sin red / timeout
            is FirebaseAuthInvalidUserException -> R.string.error_invalid_user    // usuario no existe / deshabilitado
            is FirebaseAuthInvalidCredentialsException -> R.string.error_invalid_credentials // email mal / pass mal
            is FirebaseAuthUserCollisionException -> R.string.error_user_collision // email ya registrado
            is FirebaseAuthWeakPasswordException -> R.string.error_weak_password  // password debil
            else -> R.string.error_generic                                       // resto
        }
    }
}