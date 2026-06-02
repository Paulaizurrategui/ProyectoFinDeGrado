package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paulaizurrategui.urtriply.domain.model.CommunityFilters
import com.paulaizurrategui.urtriply.domain.model.TravelPost
import com.paulaizurrategui.urtriply.ui.auth.CommunityViewModel

@Composable
fun CommunityTabScreen(
    onPostClick: (String) -> Unit = {},
    onNavigateToFindFriends: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {}
) {
    // Pantalla contenedor para la pestaña Comunidad
    // - Verifica la edad (+13) antes de mostrar el feed
    // - Muestra un loader, un mensaje de restricción o el `CommunityScreen`
    val isCompactWidth = LocalConfiguration.current.screenWidthDp < 360
    val authViewModel = viewModel<com.paulaizurrategui.urtriply.ui.auth.AuthViewModel>()
    var isOver13 by remember { mutableStateOf<Boolean?>(null) }
    
    LaunchedEffect(Unit) {
        authViewModel.isOver13Confirmed { confirmed ->
            isOver13 = confirmed
        }
    }

    when {
        isOver13 == null -> {
            // Verificando... muestro loader mientras comprobamos la edad
            UrTriplyGradientScaffold(title = stringResource(R.string.tab_community)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        isOver13 == false -> {
            // Usuario no confirmó +13 años: muestro mensaje explicativo
            UrTriplyGradientScaffold(title = stringResource(R.string.tab_community)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.community_access_restricted),
                            style = if (isCompactWidth) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = stringResource(R.string.community_access_age_required),
                            style = if (isCompactWidth) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        else -> {
            // Usuario confirmó +13 años -> renderizo el feed comunitario
            CommunityScreen(
                onPostClick = onPostClick,
                onNavigateToFindFriends = onNavigateToFindFriends,
                onNavigateToUserProfile = onNavigateToUserProfile
            )
        }
    }
}

// colores del feed
val OrangeUrTriply = Color(0xFFFF8C00)
val LightBlue = Color(0xFFE0F7FA)
val SkyBlue = Color(0xFF87CEEB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = viewModel(),
    onPostClick: (String) -> Unit = {},
    onNavigateToFindFriends: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {}
) {
    // Composable que dibuja todo el feed: cabecera, filtros, lista de posts, estados vacíos
    // Consume el `CommunityViewModel` para obtener posts, loading, errores y filtros
    val isCompactWidth = LocalConfiguration.current.screenWidthDp < 360

    // estado del viewmodel
    val posts by viewModel.posts.collectAsState()
    val errorMessage by remember { derivedStateOf { viewModel.errorMessage.value } }
    val filters by viewModel.filters
    val isLoading by viewModel.isLoading
    val showFilters by viewModel.showFilters

    val followingIds by viewModel.followingIdsFlow.collectAsState()

    // fondo suave para comunidad
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SkyBlue.copy(alpha = 0.3f), LightBlue)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // cabecera con logo + botón para abrir panel de filtros
            CommunityHeader(
                onFilterClick = { viewModel.toggleFilters() },
                hasActiveFilters = filters.destination.isNotEmpty() || filters.maxBudget != null
            )

            // panel de filtros desplegable (AnimatedVisibility para animar apertura)
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                FiltersPanel(
                    filters = filters,
                    onFiltersChange = { viewModel.updateFilters(it) },
                    onClearFilters = { viewModel.clearFilters() }
                )
            }

            // Mensaje de error (visible temporalmente) para depuración y feedback al usuario
            if (!errorMessage.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.errorMessage.value = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            // Contenido principal según estado: loader, vacío o lista de posts
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OrangeUrTriply)
                }
            } else if (posts.isEmpty()) {
                if (followingIds.isEmpty()) {
                    EmptyFollowingState(onNavigateToFindFriends = onNavigateToFindFriends)
                } else {
                    EmptyState()
                }
            } else {
                // lista de posts
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Item por cada TravelPost -> tarjeta reutilizable
                    items(posts, key = { it.id }) { post ->
                        TravelPostCard(
                            post = post,
                            isCompactWidth = isCompactWidth,
                            onLikeClick = { viewModel.toggleLike(post.id) },
                            onFavoriteClick = { viewModel.toggleFavorite(post.id) },
                            onCommentClick = { onPostClick(post.id) },
                            onCardClick = { onPostClick(post.id) },
                            onAuthorClick = { if (post.authorUid.isNotBlank()) onNavigateToUserProfile(post.authorUid) }
                        )
                    }

                    // espacio para que no se tape con el bottom nav
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityHeader(
    onFilterClick: () -> Unit,
    hasActiveFilters: Boolean
) {
    val isCompactWidth = LocalConfiguration.current.screenWidthDp < 360

    // Header: appbar custom con logo y botón de filtros
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // logo y nombre de la app (estilizado en un contenedor)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFFFF3E0))
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = if (isCompactWidth) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OrangeUrTriply,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // título de la sección y botón de filtros (con badge si hay filtros activos)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.community_title),
                        style = if (isCompactWidth) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.community_subtitle),
                        style = if (isCompactWidth) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                // Botón de filtros con badge si hay filtros activos (visual cue)
                FilledTonalIconButton(
                    onClick = onFilterClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (hasActiveFilters) OrangeUrTriply.copy(alpha = 0.2f)
                        else Color.LightGray.copy(alpha = 0.3f)
                    )
                ) {
                    BadgedBox(
                        badge = {
                            if (hasActiveFilters) {
                                Badge(containerColor = OrangeUrTriply)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.community_filters),
                            tint = if (hasActiveFilters) OrangeUrTriply else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FiltersPanel(
    filters: CommunityFilters,
    onFiltersChange: (CommunityFilters) -> Unit,
    onClearFilters: () -> Unit
) {
    // estado local para los inputs (mejor experiencia al escribir)
    var destinationText by remember(filters.destination) {
        mutableStateOf(filters.destination)
    }
    var budgetText by remember(filters.maxBudget) {
        mutableStateOf(filters.maxBudget?.toInt()?.toString() ?: "")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.community_filters_panel),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // filtro por destino
            OutlinedTextField(
                value = destinationText,
                onValueChange = { destinationText = it },
                label = { Text(stringResource(R.string.community_destination_label)) },
                placeholder = { Text(stringResource(R.string.community_destination_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Default.Place, contentDescription = null, tint = OrangeUrTriply)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeUrTriply,
                    focusedLabelColor = OrangeUrTriply
                )
            )

            // filtro por presupuesto (solo digitos)
            OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.community_max_budget_label)) },
                placeholder = { Text(stringResource(R.string.community_max_budget_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Default.Euro, contentDescription = null, tint = OrangeUrTriply)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeUrTriply,
                    focusedLabelColor = OrangeUrTriply
                )
            )

            // botones de aplicar/limpiar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        destinationText = ""
                        budgetText = ""
                        onClearFilters()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Gray
                    )
                ) {
                    Text(stringResource(R.string.community_clear))
                }

                Button(
                    onClick = {
                        onFiltersChange(
                            CommunityFilters(
                                destination = destinationText,
                                maxBudget = budgetText.toFloatOrNull()
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeUrTriply
                    )
                ) {
                    Text(stringResource(R.string.community_apply))
                }
            }
        }
    }
}

@Composable
fun TravelPostCard(
    post: TravelPost,
    isCompactWidth: Boolean,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCommentClick: () -> Unit,
    onCardClick: () -> Unit,
    onAuthorClick: () -> Unit = {}
) {
    val authorInitial = post.authorName.firstOrNull()?.toString() ?: "?"

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header de la tarjeta: avatar (inicial), nombre del autor, fecha y favorito
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar: inicial del autor dentro de un círculo
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(OrangeUrTriply.copy(alpha = 0.2f))
                        .size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = authorInitial,
                        fontWeight = FontWeight.Bold,
                        color = OrangeUrTriply,
                        style = if (isCompactWidth) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = post.authorUid.isNotBlank(), onClick = onAuthorClick)) {
                    Text(
                        text = post.authorName,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = post.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Botón para marcar como favorito (estado visual según `post.isFavorite`)
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (post.isFavorite) Icons.Filled.Bookmark
                        else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(R.string.community_save),
                        tint = if (post.isFavorite) OrangeUrTriply else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chips del viaje: destino, días y presupuesto (información rápida)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            post.destination,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = OrangeUrTriply.copy(alpha = 0.1f),
                        labelColor = OrangeUrTriply,
                        leadingIconContentColor = OrangeUrTriply
                    )
                )

                AssistChip(
                    onClick = { },
                    label = { Text(stringResource(R.string.community_days_format, post.days), maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = SkyBlue.copy(alpha = 0.3f),
                        labelColor = Color(0xFF0277BD)
                    )
                )

                AssistChip(
                    onClick = { },
                    label = { Text("${post.budget.toInt()}${post.currency}", maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Euro,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFFE8F5E9),
                        labelColor = Color(0xFF2E7D32)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Descripción del post; limitada en líneas para mantener la tarjeta compacta
            Text(
                text = post.description,
                style = if (isCompactWidth) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                maxLines = if (isCompactWidth) 4 else 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = Color.LightGray.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(8.dp))

            // Acciones: like, abrir comentarios y ver más (navegar al detalle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLikeClick() }
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.Favorite
                        else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.community_like),
                        tint = if (post.isLiked) Color.Red else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onCommentClick() }
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = stringResource(R.string.community_comments),
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                TextButton(onClick = onCardClick) {
                    Text(
                        text = stringResource(R.string.community_view_more),
                        color = OrangeUrTriply,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = OrangeUrTriply,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    // estado vacio cuando no hay posts
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.community_no_posts_found),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            Text(
                text = stringResource(R.string.community_try_other_filters),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )
        }
    }
}

@Composable
fun EmptyFollowingState(onNavigateToFindFriends: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = OrangeUrTriply
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.community_empty_feed_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.community_empty_feed_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateToFindFriends, colors = ButtonDefaults.buttonColors(containerColor = OrangeUrTriply)) {
                Text(stringResource(R.string.community_search_friends))
            }
        }
    }
}