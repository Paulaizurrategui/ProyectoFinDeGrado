package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.auth.AuthViewModel

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel, // ViewModel que ejecuta el registro en Firebase y expone uiState (loading/errores)
    onGoToLogin: () -> Unit, // Navegación de vuelta a Login
    onRegisterSuccess: () -> Unit // Callback de navegación cuando Firebase confirma registro correcto
) {
    val uiState by authViewModel.uiState.collectAsState() // Observa el estado de auth (recompone al cambiar)

    var email by remember { mutableStateOf("") } // Estado local del campo email
    var password by remember { mutableStateOf("") } // Estado local del campo password
    var repeatPassword by remember { mutableStateOf("") } // Estado local del campo repetir password
    var showPassword by remember { mutableStateOf(false) } // Toggle de visibilidad password 1
    var showRepeatPassword by remember { mutableStateOf(false) } // Toggle de visibilidad password 2

    var localErrorResId by remember { mutableStateOf<Int?>(null) } // Errores de validación local (antes de llamar a Firebase)

    val dialogResId = localErrorResId ?: uiState.errorResId ?: uiState.successResId // Prioridad: error local > error Firebase > éxito
    val dialogTitleResId = when { // Título del diálogo según el tipo de mensaje
        localErrorResId != null -> R.string.dialog_error_title
        uiState.errorResId != null -> R.string.dialog_error_title
        uiState.successResId != null -> R.string.dialog_success_title
        else -> null
    }

    if (dialogResId != null && dialogTitleResId != null) { // Mostrar dialog si hay mensaje
        AlertDialog(
            onDismissRequest = {
                localErrorResId = null // Limpia el error local si existía
                authViewModel.clearMessages() // Limpia error/éxito del ViewModel
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

    val bg = Brush.verticalGradient( // Fondo degradado igual que Welcome/Login (consistencia visual)
        0f to Color(0xFF4FC3F7),
        0.55f to Color(0xFFB3E5FC),
        1f to Color(0xFFE3F2FD)
    )
    val orange = Color(0xFFFF8A00) // Color CTA

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg) // Degradado de fondo
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box( // Cabecera con "logo" textual (igual que Login)
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFFFF3E0))
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFEF6C00),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text( // Título "Crear cuenta"
                    text = stringResource(R.string.register_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text( // Subtítulo (explicación corta)
                    text = stringResource(R.string.register_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        localErrorResId = null // Si el usuario cambia campos, borramos errores de validación previos
                        authViewModel.clearMessages() // También limpiamos errores de Firebase previos
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.email_label)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
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
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) { // Toggle mostrar/ocultar contraseña
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
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
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showRepeatPassword = !showRepeatPassword }) { // Toggle mostrar/ocultar repetir contraseña
                            Icon(
                                imageVector = if (showRepeatPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (showRepeatPassword) VisualTransformation.None else PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        localErrorResId = null // Resetea error local antes de validar

                        if (password != repeatPassword) { // Validación 1: las contraseñas deben coincidir
                            localErrorResId = R.string.error_passwords_not_match
                            return@Button
                        }
                        if (password.length < 6) { // Validación 2: mínimo 6 caracteres (Firebase también lo exige)
                            localErrorResId = R.string.error_password_min
                            return@Button
                        }

                        authViewModel.register(email, password, onRegisterSuccess) // Si pasa validación, se llama a Firebase vía ViewModel
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !uiState.isLoading, // Evita doble click mientras se registra
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = orange)
                ) {
                    Text(
                        text = if (uiState.isLoading) stringResource(R.string.register_loading) else stringResource(R.string.register_button),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.already_have_account_question), // “¿Ya tienes cuenta?”
                        color = Color(0xFF6B7280)
                    )
                    TextButton(onClick = onGoToLogin) { // Vuelve a login
                        Text(stringResource(R.string.already_have_account_action), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}