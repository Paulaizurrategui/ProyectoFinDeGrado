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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.auth.AuthViewModel
import com.paulaizurrategui.urtriply.ui.validation.EmailValidator

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onGoToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mainTextColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF6B7280)
    val panelColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color.White
    val headerColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color(0xFFFFF3E0)
    val headerTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else Color(0xFFEF6C00)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showRepeatPassword by remember { mutableStateOf(false) }
    var isOver13 by remember { mutableStateOf(false) }

    var localErrorResId by remember { mutableStateOf<Int?>(null) }

    // NUEVO: error local con texto (para no tocar strings.xml)
    var localErrorText by remember { mutableStateOf<String?>(null) }

    // Dialog con strings (los que ya tenías)
    val dialogResId = localErrorResId ?: uiState.errorResId ?: uiState.successResId
    val dialogTitleResId = when {
        localErrorResId != null -> R.string.dialog_error_title
        uiState.errorResId != null -> R.string.dialog_error_title
        uiState.successResId != null -> R.string.dialog_success_title
        else -> null
    }

    // NUEVO: Dialog para errores con texto
    if (localErrorText != null) {
        AlertDialog(
            onDismissRequest = {
                localErrorText = null
                authViewModel.clearMessages()
            },
            title = { Text(stringResource(R.string.dialog_error_title)) },
            text = { Text(localErrorText ?: "") },
            confirmButton = {
                TextButton(onClick = { localErrorText = null }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        )
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
                ) { Text(stringResource(R.string.dialog_ok)) }
            }
        )
    }

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
    val orange = Color(0xFFFF8A00)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = panelColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(headerColor)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.ExtraBold,
                        color = headerTextColor,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.register_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = mainTextColor
                )
                Text(
                    text = stringResource(R.string.register_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        localErrorResId = null
                        localErrorText = null
                        authViewModel.clearMessages()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.email_label)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
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

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        localErrorResId = null
                        localErrorText = null
                        authViewModel.clearMessages()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.password_label)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = repeatPassword,
                    onValueChange = {
                        repeatPassword = it
                        localErrorResId = null
                        localErrorText = null
                        authViewModel.clearMessages()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.repeat_password_label)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showRepeatPassword = !showRepeatPassword }) {
                            Icon(
                                imageVector = if (showRepeatPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (showRepeatPassword) VisualTransformation.None else PasswordVisualTransformation(),
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

                Spacer(modifier = Modifier.height(16.dp))

                // Age verification checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isOver13,
                        onCheckedChange = { isOver13 = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = orange,
                            uncheckedColor = Color.Gray
                        )
                    )
                    Text(
                        text = "Tengo más de 13 años / I\'m over 13",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        localErrorResId = null
                        localErrorText = null

                        // Validate age
                        if (!isOver13) {
                            localErrorText = "Debes confirmar que tienes más de 13 años para continuar / You must confirm you are over 13 to continue."
                            return@Button
                        }

                        // NUEVO: validar dominio permitido
                        if (!EmailValidator.isAllowed(email)) {
                            localErrorText =
                                "El correo debe ser de un dominio válido (por ejemplo: @gmail.com, @hotmail.com, @outlook.com)."
                            return@Button
                        }

                        if (password != repeatPassword) {
                            localErrorResId = R.string.error_passwords_not_match
                            return@Button
                        }
                        if (password.length < 6) {
                            localErrorResId = R.string.error_password_min
                            return@Button
                        }

                        authViewModel.register(email.trim(), password, isOver13, onRegisterSuccess)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = orange)
                ) {
                    Text(
                        text = if (uiState.isLoading) stringResource(R.string.register_loading) else stringResource(R.string.register_button),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.already_have_account_question),
                        color = secondaryTextColor
                    )
                    TextButton(onClick = onGoToLogin) {
                        Text(
                            stringResource(R.string.already_have_account_action),
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