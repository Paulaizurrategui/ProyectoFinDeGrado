package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
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

    val bg = Brush.verticalGradient(
        0f to Color(0xFFD98A3D), // coral suave
        1f to Color(0xFF6FA9C9)  // celeste grisáceo
    )
    val orange = Color(0xFFFF8A00) // Color principal de botones (se mantiene para compatibilidad)

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(horizontal = 16.dp)
        ) {
            // Header compacto ~220dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(bg)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    // Logo (si existe drawable/ic_launcher_foreground se puede mostrar)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.home_emoji_plane), style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Saludo compacto: nombre + email pequeño
                    Text(
                        text = stringResource(R.string.home_greeting, email.substringBefore('@').replaceFirstChar { it.uppercaseChar() }),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xCCFFFFFF)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.home_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xCCFFFFFF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Accesos rápidos
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.home_quick_access),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                )

                // Helper to build a compact card
                @Composable
                fun QuickCard(title: String, subtitle: String, primary: Boolean = false, onClick: () -> Unit) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(if (primary) 74.dp else 64.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (primary) Color(0xFFE8F8FF) else Color(0xFFF3FAFD)),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (primary) 12.dp else 4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Placeholder icon circle
                            Card(
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("✈️")
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = title, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                            }

                            // Arrow
                            Text(text = "→", color = Color(0xFF0F172A))
                        }
                    }
                }

                QuickCard(
                    title = stringResource(R.string.home_plan_trip),
                    subtitle = stringResource(R.string.home_plan_description),
                    primary = true,
                    onClick = { showSoonDialog = true }
                )

                QuickCard(
                    title = stringResource(R.string.home_drafts),
                    subtitle = stringResource(R.string.home_plan_description),
                    onClick = { showSoonDialog = true }
                )

                QuickCard(
                    title = stringResource(R.string.home_community),
                    subtitle = stringResource(R.string.home_community_description),
                    onClick = { showSoonDialog = true }
                )

                QuickCard(
                    title = stringResource(R.string.home_profile_title),
                    subtitle = stringResource(R.string.home_profile_description),
                    onClick = { /* navigate to profile */ }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Logout button at bottom
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        authViewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text(text = stringResource(R.string.logout_button), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}