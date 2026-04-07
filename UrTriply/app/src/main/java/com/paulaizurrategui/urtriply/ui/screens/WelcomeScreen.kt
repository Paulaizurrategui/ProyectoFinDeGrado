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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paulaizurrategui.urtriply.R

@Composable
fun WelcomeScreen(
    isLoggedIn: Boolean, // true si FirebaseAuth detecta un usuario actual (sesión iniciada)
    onGoHome: () -> Unit, // Acción cuando el usuario ya está logueado y quiere "Entrar" a la app (modo auth)
    onGoLogin: () -> Unit, // Navegar a Login
    onGoRegister: () -> Unit, // Navegar a Registro
    onContinueGuest: () -> Unit // Entrar sin cuenta (modo invitado) -> MainShell guest
) {
    val bg = Brush.verticalGradient( // Fondo degradado (identidad visual estilo “viaje”)
        0f to Color(0xFF4FC3F7),
        0.55f to Color(0xFFB3E5FC),
        1f to Color(0xFFE3F2FD)
    )
    val orange = Color(0xFFFF8A00) // Color principal para CTA (botones destacados)

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg) // Aplica el degradado al fondo completo
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White), // Tarjeta blanca sobre el fondo
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box( // “Logo” textual dentro de una cápsula (se puede cambiar por Image más adelante)
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

                    Text( // Eslogan de UrTriply (del Hito 1)
                        text = stringResource(R.string.slogan),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF374151)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (isLoggedIn) { // Si ya hay sesión activa, priorizamos el botón "Entrar"
                        Button(
                            onClick = onGoHome, // Ir al Main autenticado
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = orange)
                        ) {
                            Text(
                                text = stringResource(R.string.welcome_go_home),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton( // Permite entrar como invitado aunque haya sesión (útil para demo / pruebas)
                            onClick = onContinueGuest,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(stringResource(R.string.welcome_guest), fontWeight = FontWeight.Bold)
                        }
                    } else { // Si no hay sesión, mostramos login/registro + opción de invitado
                        Button(
                            onClick = onGoLogin,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = orange)
                        ) {
                            Text(
                                text = stringResource(R.string.welcome_login),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onGoRegister,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(stringResource(R.string.welcome_register), fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        TextButton(onClick = onContinueGuest) { // CTA secundario para modo invitado
                            Text(stringResource(R.string.welcome_guest))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text( // Aviso: en invitado no hay comunidad/guardado (alineado con Hito 1)
                        text = stringResource(R.string.welcome_guest_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}