package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.auth.AuthViewModel

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onGoToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }

    // Errores locales (validación) también como StringRes
    var localErrorResId by remember { mutableStateOf<Int?>(null) }

    // Dialog (prioriza error sobre éxito)
    val dialogResId = localErrorResId ?: uiState.errorResId ?: uiState.successResId
    val dialogTitleResId = when {
        localErrorResId != null -> R.string.dialog_error_title
        uiState.errorResId != null -> R.string.dialog_error_title
        uiState.successResId != null -> R.string.dialog_success_title
        else -> null
    }

    if (dialogResId != null && dialogTitleResId != null) {
        AlertDialog(
            onDismissRequest = {
                localErrorResId = null
                authViewModel.clearMessages()
            },
            title = { Text(stringResource(dialogTitleResId)) },
            text = { Text(stringResource(dialogResId)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        localErrorResId = null
                        authViewModel.clearMessages()
                    }
                ) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.register_title), style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                localErrorResId = null
                authViewModel.clearMessages()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.email_label)) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                localErrorResId = null
                authViewModel.clearMessages()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.password_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = repeatPassword,
            onValueChange = {
                repeatPassword = it
                localErrorResId = null
                authViewModel.clearMessages()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.repeat_password_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                localErrorResId = null

                if (password != repeatPassword) {
                    localErrorResId = R.string.error_passwords_not_match
                    return@Button
                }
                if (password.length < 6) {
                    localErrorResId = R.string.error_password_min
                    return@Button
                }

                authViewModel.register(email, password, onRegisterSuccess)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            Text(
                if (uiState.isLoading) stringResource(R.string.register_loading)
                else stringResource(R.string.register_button)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onGoToLogin) {
            Text(stringResource(R.string.already_have_account))
        }
    }
}