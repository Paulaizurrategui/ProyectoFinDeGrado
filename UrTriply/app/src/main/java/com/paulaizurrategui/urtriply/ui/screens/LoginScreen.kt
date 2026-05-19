package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.auth.AuthViewModel
import com.paulaizurrategui.urtriply.ui.validation.EmailValidator

@Composable
fun LoginScreen( // pantalla de login
    authViewModel: AuthViewModel, // viewmodel para manejar login
    onGoToRegister: () -> Unit, // navegar a registro
    onLoginSuccess: () -> Unit // cuando login va bien
) {
    val uiState by authViewModel.uiState.collectAsState() // recojo estado del viewmodel

    var email by remember { mutableStateOf("") } // estado email
    var password by remember { mutableStateOf("") } // estado password
    var showPassword by remember { mutableStateOf(false) } // para mostrar/ocultar pass

    var localErrorText by remember { mutableStateOf<String?>(null) } // error local

    // si hay error local muestro dialog
    if (localErrorText != null) {
        AlertDialog(
            onDismissRequest = { localErrorText = null }, // cerrar dialog
            title = { Text(stringResource(R.string.dialog_error_title)) },
            text = { Text(localErrorText ?: "") }, // mensaje error
            confirmButton = {
                TextButton(onClick = { localErrorText = null }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        )
    }

    // cojo error o success del viewmodel
    val dialogResId = uiState.errorResId ?: uiState.successResId
    val dialogTitleResId = when {
        uiState.errorResId != null -> R.string.dialog_error_title // titulo error
        uiState.successResId != null -> R.string.dialog_success_title // titulo ok
        else -> null
    }

    // dialog del viewmodel
    if (dialogResId != null && dialogTitleResId != null) {
        AlertDialog(
            onDismissRequest = { authViewModel.clearMessages() }, // limpiar mensajes
            title = { Text(stringResource(dialogTitleResId)) },
            text = { Text(stringResource(dialogResId)) },
            confirmButton = {
                TextButton(onClick = { authViewModel.clearMessages() }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        )
    }

    // fondo degradado azul
    val bg = Brush.verticalGradient(
        0f to Color(0xFF4FC3F7),
        0.55f to Color(0xFFB3E5FC),
        1f to Color(0xFFE3F2FD)
    )

    Box( // contenedor principal
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card( // tarjeta blanca del login
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column( // contenido en columna
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box( // cabecera con nombre app
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFFFF3E0))
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.app_name), // nombre app
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFEF6C00),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.height(12.dp)) // espacio

                Text( // titulo login
                    text = stringResource(R.string.login_button),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text( // subtitulo
                    text = "¡bienvenid@ de vuelta!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // campo email
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it // actualizo email
                        localErrorText = null // limpio error
                        authViewModel.clearMessages() // limpio mensajes vm
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.email_label)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // campo password
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it // actualizo pass
                        localErrorText = null
                        authViewModel.clearMessages()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.password_label)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) { // toggle ver pass
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation() // ocultar/mostrar
                )

                Row( // fila para forgot password
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            // valido email antes
                            if (!EmailValidator.isAllowed(email)) {
                                localErrorText = "introduce un correo válido (ej: @gmail.com)"
                                return@TextButton
                            }
                            authViewModel.sendPasswordReset(email.trim()) // envio reset
                        },
                        enabled = !uiState.isLoading // desactivo si carga
                    ) {
                        Text(stringResource(R.string.forgot_password))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val orange = Color(0xFFFF8A00) // color boton
                val ctx = LocalContext.current

                Button( // boton login
                    onClick = {
                        Log.d("LoginScreen", "Login button clicked: email=$email")
                        Toast.makeText(ctx, "Login pressed", Toast.LENGTH_SHORT).show()

                        localErrorText = null // limpio error

                        // valido email
                        if (!EmailValidator.isAllowed(email)) {
                            localErrorText = "correo no válido"
                            return@Button
                        }

                        authViewModel.login(email.trim(), password, onLoginSuccess) // login
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = orange)
                ) {
                    Text(
                        text = if (uiState.isLoading)
                            stringResource(R.string.login_loading) // texto cargando
                        else
                            stringResource(R.string.login_button),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "¿no tienes cuenta?", color = Color(0xFF6B7280))
                    TextButton(onClick = onGoToRegister) { // ir a registro
                        Text(stringResource(R.string.create_account), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}