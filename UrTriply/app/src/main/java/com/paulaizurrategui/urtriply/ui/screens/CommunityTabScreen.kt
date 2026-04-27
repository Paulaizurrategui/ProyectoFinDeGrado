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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paulaizurrategui.urtriply.domain.model.CommunityFilters
import com.paulaizurrategui.urtriply.domain.model.TravelPost
import com.paulaizurrategui.urtriply.ui.auth.CommunityViewModel

@Composable
fun CommunityTabScreen() {
    UrTriplyGradientScaffold(title = stringResource(R.string.tab_community)) { // Tab "Comunidad" (solo modo auth; en guest se bloquea en MainShell)
        Text(text = stringResource(R.string.community_placeholder_body)) // Placeholder: aquí irá el feed de viajes publicados
    }
}


// Colores de la app
val OrangeUrTriply = Color(0xFFFF8C00)
val LightBlue = Color(0xFFE0F7FA)
val SkyBlue = Color(0xFF87CEEB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = viewModel(),
    onPostClick: (String) -> Unit = {}
) {
    val posts by viewModel.posts.collectAsState()
    val filters by viewModel.filters
    val isLoading by viewModel.isLoading
    val showFilters by viewModel.showFilters

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
            // Header
            CommunityHeader(
                onFilterClick = { viewModel.toggleFilters() },
                hasActiveFilters = filters.destination.isNotEmpty() || filters.maxBudget != null
            )

            // Panel de filtros
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

            // Lista de posts
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OrangeUrTriply)
                }
            } else if (posts.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        TravelPostCard(
                            post = post,
                            onLikeClick = { viewModel.toggleLike(post.id) },
                            onFavoriteClick = { viewModel.toggleFavorite(post.id) },
                            onCommentClick = { onPostClick(post.id) },
                            onCardClick = { onPostClick(post.id) }
                        )
                    }

                    // Espaciado para el bottom nav
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFFFF3E0))
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "UrTriply",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OrangeUrTriply
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Título y filtros
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Comunidad",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Descubre viajes de otros usuarios",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                // Botón filtros
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
                            contentDescription = "Filtros",
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
                text = "Filtrar publicaciones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Filtro por destino
            OutlinedTextField(
                value = destinationText,
                onValueChange = { destinationText = it },
                label = { Text("Destino") },
                placeholder = { Text("Ej: Barcelona, París...") },
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

            // Filtro por presupuesto
            OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it.filter { c -> c.isDigit() } },
                label = { Text("Presupuesto máximo (€)") },
                placeholder = { Text("Ej: 1000") },
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

            // Botones
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
                    Text("Limpiar")
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
                    Text("Aplicar")
                }
            }
        }
    }
}

@Composable
fun TravelPostCard(
    post: TravelPost,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCommentClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: autor y fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(OrangeUrTriply.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.authorName.first().toString(),
                        fontWeight = FontWeight.Bold,
                        color = OrangeUrTriply
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
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

                // Botón favorito
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (post.isFavorite) Icons.Filled.Bookmark
                        else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Guardar",
                        tint = if (post.isFavorite) OrangeUrTriply else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info del viaje: chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Destino
                AssistChip(
                    onClick = { },
                    label = { Text(post.destination) },
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

                // Días
                AssistChip(
                    onClick = { },
                    label = { Text("${post.days} días") },
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

                // Presupuesto
                AssistChip(
                    onClick = { },
                    label = { Text("${post.budget.toInt()}${post.currency}") },
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

            // Descripción
            Text(
                text = post.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = Color.LightGray.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(8.dp))

            // Acciones: likes y comentarios
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Likes
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLikeClick() }
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.Favorite
                        else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Me gusta",
                        tint = if (post.isLiked) Color.Red else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.likes}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                // Comentarios
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onCommentClick() }
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comentarios",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.comments}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                // Ver detalle
                TextButton(onClick = onCardClick) {
                    Text(
                        text = "Ver más",
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
                text = "No se encontraron publicaciones",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            Text(
                text = "Intenta con otros filtros",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )
        }
    }
}
