package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.data.trips.TripStatus
import com.paulaizurrategui.urtriply.ui.auth.AuthViewModel
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold

@Composable
fun ProfileTabScreen(
    authViewModel: AuthViewModel,
    onLoggedOut: () -> Unit,
    onEditProfile: () -> Unit,
    onEditTrip: (tripId: String) -> Unit,
    onNavigateToFindFriends: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email ?: "-"
    val displayName = user?.displayName
    val initial = (displayName?.firstOrNull() ?: email.firstOrNull() ?: 'U').uppercaseChar().toString()

    val tripsVm = remember { ProfileTripsViewModel() }
    val tripsState by tripsVm.uiState.collectAsState()

    // NUEVO: contador de amigos (siguiendo)
    val friendsCountVm: ProfileFriendsCountViewModel = viewModel()
    val friendsCount by friendsCountVm.followingCount.collectAsState()

    var confirmDeleteId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { tripsVm.loadMyTrips() }

    if (tripsState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { tripsVm.clearError() },
            title = { Text("Error") },
            text = { Text(tripsState.errorMessage ?: "") },
            confirmButton = { TextButton(onClick = { tripsVm.clearError() }) { Text("OK") } }
        )
    }

    if (confirmDeleteId != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Eliminar viaje") },
            text = { Text("¿Seguro que quieres eliminar este viaje? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = confirmDeleteId
                    confirmDeleteId = null
                    if (id != null) tripsVm.deleteTrip(id)
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteId = null }) { Text("Cancelar") } }
        )
    }

    UrTriplyGradientScaffold(title = stringResource(R.string.tab_profile)) {

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = email,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Mi cuenta",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(10.dp))

                            TextButton(
                                onClick = onEditProfile,
                                contentPadding = ButtonDefaults.TextButtonContentPadding
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    text = "Editar perfil",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // SECCIÓN AMIGOS (con contador)
            item {
                SectionTitle("Amigos", "Personas a las que sigues")

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Siguiendo: $friendsCount",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = onNavigateToFindFriends,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Gestionar amigos")
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = { tripsVm.loadMyTrips() },
                        enabled = !tripsState.isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (tripsState.isLoading) "Cargando..." else "Recargar")
                    }
                }
            }

            item { SectionTitle("Borradores", "Tus viajes guardados sin publicar") }

            if (tripsState.drafts.isEmpty()) {
                item { EmptyStateCard("Aún no tienes borradores. Crea un viaje y guárdalo para editarlo luego.") }
            } else {
                items(tripsState.drafts, key = { it.id }) { trip ->
                    TripCard(
                        title = trip.destino,
                        status = TripStatus.DRAFT,
                        onEdit = { onEditTrip(trip.id) },
                        onPublish = { tripsVm.publishTrip(trip.id) },
                        onDelete = { confirmDeleteId = trip.id },
                        actionsEnabled = !tripsState.isLoading
                    )
                }
            }

            item { SectionTitle("Publicaciones", "Tus viajes publicados en la comunidad") }

            if (tripsState.published.isEmpty()) {
                item { EmptyStateCard("Aún no tienes publicaciones. Publica un borrador para que aparezca aquí.") }
            } else {
                items(tripsState.published, key = { it.id }) { trip ->
                    TripCard(
                        title = trip.destino,
                        status = TripStatus.PUBLISHED,
                        onEdit = { onEditTrip(trip.id) },
                        onPublish = null,
                        onDelete = { confirmDeleteId = trip.id },
                        actionsEnabled = !tripsState.isLoading
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            authViewModel.logout()
                            onLoggedOut()
                        },
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.size(10.dp))
                            Column {
                                Text(
                                    text = "Cerrar sesión",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Saldrás de tu cuenta en este dispositivo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        TextButton(onClick = {
                            authViewModel.logout()
                            onLoggedOut()
                        }) {
                            Text(
                                text = "Salir",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyStateCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TripCard(
    title: String,
    status: TripStatus,
    onEdit: () -> Unit,
    onPublish: (() -> Unit)?,
    onDelete: () -> Unit,
    actionsEnabled: Boolean
) {
    val chipBg = when (status) {
        TripStatus.DRAFT -> MaterialTheme.colorScheme.secondaryContainer
        TripStatus.PUBLISHED -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val chipFg = when (status) {
        TripStatus.DRAFT -> MaterialTheme.colorScheme.onSecondaryContainer
        TripStatus.PUBLISHED -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val chipText = if (status == TripStatus.DRAFT) "Borrador" else "Publicado"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    StatusChip(text = chipText, bg = chipBg, fg = chipFg)
                }

                IconButton(onClick = onDelete, enabled = actionsEnabled) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    enabled = actionsEnabled,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Editar")
                }

                if (onPublish != null) {
                    Button(
                        onClick = onPublish,
                        enabled = actionsEnabled,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Publish, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = "Publicar",
                            fontSize = MaterialTheme.typography.labelMedium.fontSize
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}