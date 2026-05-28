package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.theme.UrOrange
import com.paulaizurrategui.urtriply.ui.theme.UrSky
import com.paulaizurrategui.urtriply.ui.theme.UrSkySoft

@Composable
fun InicioTabScreen(
    isGuest: Boolean,
    onRequireLogin: () -> Unit,
    onGoPlan: () -> Unit,
    onGoManualTrip: () -> Unit,
    onGoCommunity: () -> Unit,
    onGoProfile: () -> Unit
) {
    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = colorScheme.background.luminance() < 0.5f
    val email = FirebaseAuth.getInstance().currentUser?.email ?: "usuario"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // ==================== HERO SECTION ====================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isDarkTheme) {
                                listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF1E293B)
                                )
                            } else {
                                listOf(
                                    UrOrange,
                                    UrSky
                                )
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Overlay oscuro
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = if (isDarkTheme) 0.28f else 0.15f)
                        )
                )

                // Contenido hero
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Logo
                    Text(
                        text = stringResource(R.string.home_emoji_plane),
                        fontSize = 44.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Título
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 38.sp
                        ),
                        color = if (isDarkTheme) Color(0xFFF3F7FF) else Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mensaje personalizado
                    if (!isGuest) {
                        Text(
                            text = stringResource(R.string.home_greeting, email),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isDarkTheme) Color(0xFFE3EAF7) else Color.White,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.home_guest_explore),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isDarkTheme) Color(0xFFE3EAF7) else Color.White,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Eslogan
                    Text(
                        text = stringResource(R.string.home_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDarkTheme) Color(0xFFB4C0D4) else Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }



            // ==================== VALUE CARDS SECTION ====================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_quick_access),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    color = colorScheme.onBackground
                )

                // Planificar viaje
                HomeActionCard(
                    icon = "🗺️",
                    title = stringResource(R.string.home_plan_trip),
                    description = stringResource(R.string.home_plan_description),
                    onClick = onGoPlan
                )

                // Borrador manual
                HomeActionCard(
                    icon = "✍️",
                    title = stringResource(R.string.home_manual_trip),
                    description = stringResource(R.string.home_manual_trip_description),
                    onClick = if (isGuest) onRequireLogin else onGoManualTrip
                )

                // Comunidad
                HomeActionCard(
                    icon = "👥",
                    title = stringResource(R.string.home_community),
                    description = if (isGuest) stringResource(R.string.home_guest_login_required) else stringResource(R.string.home_community_description),
                    onClick = if (isGuest) onRequireLogin else onGoCommunity
                )

                // Mi perfil
                HomeActionCard(
                    icon = "👤",
                    title = stringResource(R.string.home_profile_title),
                    description = if (isGuest) stringResource(R.string.home_guest_login_required) else stringResource(R.string.home_profile_description),
                    onClick = if (isGuest) onRequireLogin else onGoProfile
                )
            }
        }
    }
}

enum class HomeActionVariant {
    Full,
    Compact,
    IconOnly
}

@Composable
fun HomeActionCard(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HomeActionVariant = HomeActionVariant.Full
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = colorScheme.background.luminance() < 0.5f
    when (variant) {
        HomeActionVariant.IconOnly -> {
            ElevatedCard(
                modifier = modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isDarkTheme) colorScheme.surfaceVariant else UrSkySoft
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                onClick = onClick
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDarkTheme) colorScheme.surface else Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = icon, fontSize = 26.sp)
                    }
                }
            }
        }

        HomeActionVariant.Compact -> {
            ElevatedCard(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isDarkTheme) colorScheme.surfaceVariant else UrSkySoft
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                onClick = onClick
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDarkTheme) colorScheme.surface else Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = icon, fontSize = 22.sp)
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface
                    )
                }
            }
        }

        HomeActionVariant.Full -> {
            ElevatedCard(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isDarkTheme) colorScheme.surfaceVariant else UrSkySoft
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                onClick = onClick
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isDarkTheme) colorScheme.surface else Color(0xFFE2E8F0)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = icon, fontSize = 28.sp)
                    }

                    // Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    // Arrow
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colorScheme.primary
                    )
                }
            }
        }
    }
}
