package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paulaizurrategui.urtriply.R

@Composable
fun GuestHomeScreen(
    onGoLogin: () -> Unit, // Acción principal: llevar al usuario a Login para desbloquear funciones (modo auth)
    onBack: () -> Unit // Acción secundaria: volver atrás (normalmente a Welcome)
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mainTextColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF6B7280)
    val panelColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color.White
    val headerColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color(0xFFFFF3E0)
    val headerTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else Color(0xFFEF6C00)

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
    val orange = Color(0xFFFF8A00) // Color para el botón CTA principal

    Surface(modifier = Modifier.fillMaxSize()) { // Surface ocupa toda la pantalla
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg) // Aplica degradado de fondo
                .padding(20.dp),
            contentAlignment = Alignment.Center // Centra la Card principal
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp), // Bordes redondeados (misma estética)
                colors = CardDefaults.cardColors(containerColor = panelColor), // Card adaptada al tema
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box( // Cabecera con nombre de la app (misma “capsula” que en otras pantallas)
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.guest_title), // Título explicando que estás en modo invitado
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = mainTextColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.guest_subtitle), // Subtítulo: qué se puede / qué no se puede sin cuenta
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onGoLogin, // CTA: ir a Login
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = orange)
                    ) {
                        Text(
                            text = stringResource(R.string.guest_login_cta), // Texto del CTA (ej. "Inicia sesión para guardar...")
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onBack, // Botón secundario: volver atrás (ej. a Welcome)
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.back), fontWeight = FontWeight.Bold, color = mainTextColor) // Texto "Volver"
                    }
                }
            }
        }
    }
}