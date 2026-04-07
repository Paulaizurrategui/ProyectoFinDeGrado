package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.auth.AuthViewModel

@Composable
fun HomeScreen(
    authViewModel: AuthViewModel, // ViewModel de auth para poder hacer logout desde esta pantalla
    onLogout: () -> Unit // Callback de navegación cuando se cierra sesión (volver a Welcome, etc.)
) {
    val email = FirebaseAuth.getInstance().currentUser?.email ?: "usuario" // Lee el email del usuario logueado (fallback "usuario")

    val bg = Brush.verticalGradient( // Fondo degradado consistente con el resto de pantallas
        0f to Color(0xFF4FC3F7),
        0.55f to Color(0xFFB3E5FC),
        1f to Color(0xFFE3F2FD)
    )
    val orange = Color(0xFFFF8A00) // Color principal de botones

    var showSoonDialog by remember { mutableStateOf(false) } // Controla si se muestra el diálogo "Próximamente"

    if (showSoonDialog) { // Dialog placeholder para botones que aún no tienen funcionalidad
        AlertDialog(
            onDismissRequest = { showSoonDialog = false }, // Cierra dialog tocando fuera o con back
            title = { Text(stringResource(R.string.soon_title)) }, // Título del dialog
            text = { Text(stringResource(R.string.soon_body)) }, // Body "próximamente"
            confirmButton = {
                TextButton(onClick = { showSoonDialog = false }) { // Botón OK para cerrar
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { // Surface pantalla completa
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg) // Fondo degradado
                .padding(20.dp),
            contentAlignment = Alignment.Center // Centra la Card
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)), // Bordes redondeados
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White), // Fondo blanco para legibilidad
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box( // Cabecera con nombre de la app (look consistente)
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Text( // Título "Bienvenida"
                        text = stringResource(R.string.home_welcome),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text( // Subtítulo (descripción corta)
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text( // Muestra el usuario con el que estás logueado
                        text = stringResource(R.string.home_logged_as, email),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Button( // Placeholder: planificar viaje (por ahora abre "Próximamente")
                        onClick = { showSoonDialog = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = orange)
                    ) {
                        Text(
                            text = stringResource(R.string.home_plan_trip),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton( // Placeholder: borradores
                        onClick = { showSoonDialog = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.home_drafts), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton( // Placeholder: comunidad
                        onClick = { showSoonDialog = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.home_community), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button( // Logout: cierra sesión y vuelve a Welcome
                        onClick = {
                            authViewModel.logout() // SignOut + actualiza uiState
                            onLogout() // Navega fuera del Home
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)) // Rojo para acción destructiva
                    ) {
                        Text(
                            text = stringResource(R.string.logout_button),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}