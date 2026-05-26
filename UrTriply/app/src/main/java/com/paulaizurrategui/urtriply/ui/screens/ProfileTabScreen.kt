package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.paulaizurrategui.urtriply.ui.theme.ThemeViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
    onNavigateToFindFriends: () -> Unit,
    onNavigateToAdmin: () -> Unit = {}
) {
    val themeViewModel: com.paulaizurrategui.urtriply.ui.theme.ThemeViewModel = viewModel()
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    // Theme is loaded once in MainActivity.kt - no need to load it again here
    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email ?: "-"
    val displayName = user?.displayName
    val initial = (displayName?.firstOrNull() ?: email.firstOrNull() ?: 'U').uppercaseChar().toString()
    val profileAccountText = stringResource(R.string.profile_account)
    val profileEditProfileText = stringResource(R.string.profile_edit_profile)
    val profileFriendsTitle = stringResource(R.string.profile_friends_title)
    val profileFriendsSubtitle = stringResource(R.string.profile_friends_subtitle)
    val profileThemeLightDarkText = stringResource(R.string.profile_theme_light_dark)
    val profileThemeDarkLightText = stringResource(R.string.profile_theme_dark_light)

    val tripsVm = remember { ProfileTripsViewModel() }
    val tripsState by tripsVm.uiState.collectAsState()
    val adminVm: com.paulaizurrategui.urtriply.ui.viewmodels.AdminViewModel = viewModel()
    val isAdmin by adminVm.isAdmin

    // NUEVO: contador de amigos (siguiendo)
    val friendsCountVm: ProfileFriendsCountViewModel = viewModel()
    val friendsCount by friendsCountVm.followingCount.collectAsState()

    // NUEVO: Favoritos y Likes
    val favoritesVm: com.paulaizurrategui.urtriply.ui.viewmodels.ProfileFavoritesViewModel = viewModel()
    val favorites by favoritesVm.favorites.collectAsState()
    val likes by favoritesVm.likes.collectAsState()
    val favoritesLoading by favoritesVm.favoritesLoading.collectAsState()
    val likesLoading by favoritesVm.likesLoading.collectAsState()

    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    var confirmRemoveFavoriteId by remember { mutableStateOf<String?>(null) }
    var confirmRemoveLikeId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        tripsVm.loadMyTrips()
        favoritesVm.refreshAll()
    }

    if (tripsState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { tripsVm.clearError() },
            title = { Text(stringResource(R.string.profile_error_title)) },
            text = { Text(tripsState.errorMessage ?: "") },
            confirmButton = { TextButton(onClick = { tripsVm.clearError() }) { Text(stringResource(R.string.profile_ok)) } }
        )
    }

    if (confirmDeleteId != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text(stringResource(R.string.profile_delete_trip)) },
            text = { Text(stringResource(R.string.profile_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    val id = confirmDeleteId
                    confirmDeleteId = null
                    if (id != null) tripsVm.deleteTrip(id)
                }) { Text(stringResource(R.string.profile_delete_trip_confirm_button)) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteId = null }) { Text(stringResource(R.string.profile_cancel)) } }
        )
    }

    if (confirmRemoveFavoriteId != null) {
        AlertDialog(
            onDismissRequest = { confirmRemoveFavoriteId = null },
            title = { Text(stringResource(R.string.profile_remove_favorite_title)) },
            text = { Text(stringResource(R.string.profile_remove_favorite_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val id = confirmRemoveFavoriteId
                    confirmRemoveFavoriteId = null
                    if (id != null) favoritesVm.removeFavorite(id)
                }) { Text(stringResource(R.string.profile_remove_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmRemoveFavoriteId = null }) { Text(stringResource(R.string.profile_cancel)) } }
        )
    }

    if (confirmRemoveLikeId != null) {
        AlertDialog(
            onDismissRequest = { confirmRemoveLikeId = null },
            title = { Text(stringResource(R.string.profile_remove_like_title)) },
            text = { Text(stringResource(R.string.profile_remove_like_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val id = confirmRemoveLikeId
                    confirmRemoveLikeId = null
                    if (id != null) favoritesVm.removeLike(id)
                }) { Text(stringResource(R.string.profile_remove_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmRemoveLikeId = null }) { Text(stringResource(R.string.profile_cancel)) } }
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
                if (isAdmin) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAdmin() },
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.size(10.dp))
                            Column {
                                Text(text = stringResource(R.string.profile_panel_admin_title), fontWeight = FontWeight.Bold)
                                Text(text = stringResource(R.string.profile_panel_admin_subtitle), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
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
                                text = profileAccountText,
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
                                    text = profileEditProfileText,
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
                SectionTitle(profileFriendsTitle, profileFriendsSubtitle)

                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.profile_following_count, friendsCount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = onNavigateToFindFriends,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.profile_manage_friends))
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            tripsVm.loadMyTrips()
                            favoritesVm.refreshAll()
                        },
                        enabled = !tripsState.isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (tripsState.isLoading) stringResource(R.string.profile_refreshing) else stringResource(R.string.profile_refresh))
                    }
                }
            }

            // NUEVA SECCIÓN: Favoritos
            item { SectionTitle(stringResource(R.string.profile_favorites_title), stringResource(R.string.profile_favorites_subtitle)) }
            if (favorites.isEmpty() && !favoritesLoading) {
                item { EmptyStateCard(stringResource(R.string.profile_favorites_empty)) }
            } else if (favoritesLoading) {
                item { 
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(favorites.size, key = { index -> "favorite-${favorites[index].id}" }) { index ->
                    val trip = favorites[index]
                    TripCard(
                        title = trip.destino,
                        status = TripStatus.PUBLISHED,
                        onEdit = { },
                        onPublish = null,
                        onDelete = { confirmRemoveFavoriteId = trip.id },
                        actionsEnabled = true,
                        showEditAction = false,
                        showPublishAction = false
                    )
                }
            }

            // NUEVA SECCIÓN: Me Gusta
            item { SectionTitle(stringResource(R.string.profile_likes_title), stringResource(R.string.profile_likes_subtitle)) }
            if (likes.isEmpty() && !likesLoading) {
                item { EmptyStateCard(stringResource(R.string.profile_likes_empty)) }
            } else if (likesLoading) {
                item { 
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(likes.size, key = { index -> "like-${likes[index].id}" }) { index ->
                    val trip = likes[index]
                    TripCard(
                        title = trip.destino,
                        status = TripStatus.PUBLISHED,
                        onEdit = { },
                        onPublish = null,
                        onDelete = { confirmRemoveLikeId = trip.id },
                        actionsEnabled = true,
                        showEditAction = false,
                        showPublishAction = false
                    )
                }
            }

            item { SectionTitle(stringResource(R.string.profile_drafts_title), stringResource(R.string.profile_drafts_subtitle)) }

            if (tripsState.drafts.isEmpty()) {
                item { EmptyStateCard(stringResource(R.string.profile_drafts_empty)) }
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

            item { SectionTitle(stringResource(R.string.profile_published_title), stringResource(R.string.profile_published_subtitle)) }

            if (tripsState.published.isEmpty()) {
                item { EmptyStateCard(stringResource(R.string.profile_published_empty)) }
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
                            tripsVm.clearListeners()
                            favoritesVm.clearListeners()
                            friendsCountVm.clearListeners()
                            tripsVm.clearError()
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
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                modifier = Modifier.size(20.dp),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.size(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.profile_logout_title),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = stringResource(R.string.profile_logout_body),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                }
            }

            // Dark mode toggle card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { themeViewModel.toggleTheme() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.size(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.profile_theme_text),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isDarkTheme) profileThemeDarkLightText else profileThemeLightDarkText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { themeViewModel.toggleTheme() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
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
    actionsEnabled: Boolean,
    showEditAction: Boolean = true,
    showPublishAction: Boolean = true
) {
    val chipBg = when (status) {
        TripStatus.DRAFT -> MaterialTheme.colorScheme.secondaryContainer
        TripStatus.PUBLISHED -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val chipFg = when (status) {
        TripStatus.DRAFT -> MaterialTheme.colorScheme.onSecondaryContainer
        TripStatus.PUBLISHED -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val chipText = if (status == TripStatus.DRAFT) stringResource(R.string.profile_status_draft) else stringResource(R.string.profile_status_published)

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
                            contentDescription = stringResource(R.string.profile_delete_trip),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showEditAction) {
                    OutlinedButton(
                        onClick = onEdit,
                        enabled = actionsEnabled,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.profile_edit_button))
                    }
                }

                if (onPublish != null && showPublishAction) {
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
                            text = stringResource(R.string.trip_publish),
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