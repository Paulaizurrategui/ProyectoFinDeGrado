package com.paulaizurrategui.urtriply.ui.auth

import androidx.annotation.StringRes
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.paulaizurrategui.urtriply.R

object FirebaseAuthErrorMapper {
    @StringRes
    fun toStringRes(e: Exception?): Int {
        return when (e) {
            is FirebaseNetworkException -> R.string.error_network
            is FirebaseAuthInvalidUserException -> R.string.error_invalid_user
            is FirebaseAuthInvalidCredentialsException -> R.string.error_invalid_credentials
            is FirebaseAuthUserCollisionException -> R.string.error_user_collision
            is FirebaseAuthWeakPasswordException -> R.string.error_weak_password
            else -> R.string.error_generic
        }
    }
}