package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextOverflow
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
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mainTextColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF6B7280)
    val panelColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color.White
    val headerColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color(0xFFFFF3E0)
    val headerTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else Color(0xFFEF6C00)
    val errorEmailDomainText = stringResource(R.string.error_email_domain_not_allowed)

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
    val bg = if (isDarkTheme) {
        Brush.verticalGradient(
            0f to MaterialTheme.colorScheme.background,
            0.55f to MaterialTheme.colorScheme.surface,
            1f to MaterialTheme.colorScheme.surfaceVariant
        )
    } else {
        Brush.verticalGradient(
            0f to Color(0xFF4FC3F7),
            0.55f to Color(0xFFB3E5FC),
            1f to Color(0xFFE3F2FD)
        )
    }

    Box( // contenedor principal
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card( // tarjeta principal del login
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = panelColor),
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
                            .background(headerColor)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.app_name), // nombre app
                        fontWeight = FontWeight.ExtraBold,
                            color = headerTextColor,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.height(12.dp)) // espacio

                Text( // titulo login
                    text = stringResource(R.string.login_button),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = mainTextColor
                )
                Text( // subtitulo
                    text = stringResource(R.string.login_welcome_back),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = secondaryTextColor.copy(alpha = 0.7f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = secondaryTextColor,
                        focusedTextColor = mainTextColor,
                        unfocusedTextColor = mainTextColor,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLeadingIconColor = secondaryTextColor,
                        unfocusedLeadingIconColor = secondaryTextColor
                    )
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
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(), // ocultar/mostrar
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = secondaryTextColor.copy(alpha = 0.7f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = secondaryTextColor,
                        focusedTextColor = mainTextColor,
                        unfocusedTextColor = mainTextColor,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLeadingIconColor = secondaryTextColor,
                        unfocusedLeadingIconColor = secondaryTextColor,
                        focusedTrailingIconColor = secondaryTextColor,
                        unfocusedTrailingIconColor = secondaryTextColor
                    )
                )

                Row( // fila para forgot password
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            // valido email antes
                            if (!EmailValidator.isAllowed(email)) {
                                localErrorText = errorEmailDomainText
                                return@TextButton
                            }
                            authViewModel.sendPasswordReset(email.trim()) // envio reset
                        },
                        enabled = !uiState.isLoading // desactivo si carga
                    ) {
                        Text(stringResource(R.string.forgot_password), color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val orange = Color(0xFFFF8A00) // color boton
                Button( // boton login
                    onClick = {
                        localErrorText = null // limpio error

                        // valido email
                        if (!EmailValidator.isAllowed(email)) {
                            localErrorText = errorEmailDomainText
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
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(R.string.login_no_account), color = secondaryTextColor)
                    TextButton(onClick = onGoToRegister) { // ir a registro
                        Text(
                            stringResource(R.string.create_account),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}