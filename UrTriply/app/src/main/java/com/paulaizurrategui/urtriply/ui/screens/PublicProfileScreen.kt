package com.paulaizurrategui.urtriply.ui.screens

// Pantalla pública de usuario: muestra información básica del `UserDoc`
// y una lista de viajes publicados por ese usuario.
// - `PublicProfileViewModel` consulta Firestore para obtener el user y sus viajes.
// - La UI muestra estados: carga, error y contenido.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.domain.model.TravelPost
import com.paulaizurrategui.urtriply.domain.model.UserDoc
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold
import com.paulaizurrategui.urtriply.ui.theme.UrOrange
import com.paulaizurrategui.urtriply.ui.theme.UrSky
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

// Estado expuesto por `PublicProfileViewModel` a la composable.
// Contiene flags de carga/errores, datos del usuario y viajes publicados.
data class PublicProfileUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val user: UserDoc? = null,
    val publishedTrips: List<TravelPost> = emptyList()
)

// ViewModel que carga el perfil público y sus viajes publicados desde Firestore.
// - `loadProfile()` obtiene el documento del usuario y desencadena la carga de viajes.
// - `loadPublishedTrips()` obtiene hasta 20 viajes publicados por el autor ordenados por fecha.
class PublicProfileViewModel(
    private val userId: String,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {
    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState

    init {
        // Al crear el ViewModel, iniciamos la carga del perfil
        loadProfile()
    }

    fun loadProfile() {
        // Indicamos inicio de carga y limpiamos errores previos
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        // Petición a Firestore para leer el documento del usuario
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                // Mapear campos del documento a `UserDoc` con fallback
                val user = UserDoc(
                    uid = doc.id,
                    email = doc.getString("email") ?: "",
                    displayName = doc.getString("displayName") ?: "",
                    photoUrl = doc.getString("photoUrl") ?: doc.getString("avatarUrl"),
                    isOver13Confirmed = doc.getBoolean("isOver13Confirmed") ?: false,
                    isDarkTheme = doc.getBoolean("isDarkTheme") ?: false,
                    esAdmin = doc.getBoolean("esAdmin") ?: false
                )

                // Cargar los viajes publicados del usuario y actualizar el estado
                loadPublishedTrips(user)
                _uiState.value = _uiState.value.copy(user = user)
            }
            .addOnFailureListener { e ->
                // En fallo, dejamos el estado en error y cancelamos la carga
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: localizedFallback("No se pudo cargar el perfil público", "Could not load the public profile")
                )
            }
    }

    private fun loadPublishedTrips(user: UserDoc) {
        // Consulta Firestore para trips publicados por este autor
        db.collection("trips")
            .whereEqualTo("authorUid", user.uid)
            .whereEqualTo("status", "PUBLISHED")
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { snap ->
                // Mapear documentos a `TravelPost` con los campos relevantes
                val trips = snap.documents.map { doc ->
                    TravelPost(
                        id = doc.id,
                        authorUid = doc.getString("authorUid") ?: user.uid,
                        destination = doc.getString("destination") ?: doc.getString("destino") ?: localizedFallback("(sin destino)", "(no destination)"),
                        days = (doc.getLong("days") ?: doc.getLong("diasRecomendados") ?: 0L).toInt(),
                        budget = doc.getDouble("budget") ?: doc.getDouble("presupuestoTotal") ?: 0.0,
                        currency = doc.getString("currency") ?: "€",
                        authorName = doc.getString("authorName") ?: user.displayName.ifBlank { localizedFallback("usuario", "user") },
                        authorAvatar = doc.getString("authorAvatar") ?: user.photoUrl,
                        date = doc.getString("date") ?: "",
                        description = doc.getString("description") ?: "",
                        imageUrl = doc.getString("imageUrl"),
                        likes = (doc.getLong("likes") ?: 0L).toInt(),
                        comments = (doc.getLong("comments") ?: 0L).toInt(),
                        isLiked = false,
                        isFavorite = false
                    )
                }

                // Actualizar estado con la lista de viajes y marcar carga finalizada
                _uiState.value = _uiState.value.copy(
                    publishedTrips = trips,
                    isLoading = false,
                    errorMessage = null
                )
            }
            .addOnFailureListener { e ->
                // En fallo, informar en el estado
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: localizedFallback("No se pudieron cargar los viajes", "Could not load trips")
                )
            }
    }

    // Devuelve mensaje localizado según el idioma del dispositivo
    private fun localizedFallback(spanish: String, english: String): String {
        return if (Locale.getDefault().language.startsWith("es")) spanish else english
    }
}

@Composable
fun PublicProfileScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: PublicProfileViewModel = remember(userId) { PublicProfileViewModel(userId) }
) {
    // Estado reactivo del ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Scaffold con gradiente propio de la app
    UrTriplyGradientScaffold(title = stringResource(R.string.public_profile_title), onBack = onBack) {
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = UrOrange)
                }
            }
            uiState.errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.errorMessage ?: stringResource(R.string.public_profile_error))
                        Spacer(Modifier.height(12.dp))
                        androidx.compose.material3.Button(onClick = onBack) { Text(stringResource(R.string.public_profile_back)) }
                    }
                }
            }
            else -> {
                val user = uiState.user ?: return@UrTriplyGradientScaffold
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(UrOrange, UrSky))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!user.photoUrl.isNullOrBlank()) {
                                            val ctx = LocalContext.current
                                            AsyncImage(
                                                model = ImageRequest.Builder(ctx)
                                                    .data(user.photoUrl)
                                                    .crossfade(true)
                                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                                    .diskCachePolicy(CachePolicy.ENABLED)
                                                    .build(),
                                                contentDescription = user.displayName,
                                                modifier = Modifier
                                                    .size(76.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = user.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.headlineMedium
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.displayName.ifBlank { stringResource(R.string.public_profile_no_name) },
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = user.email.ifBlank { stringResource(R.string.public_profile_no_email) },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Chips informativos: verificado, rol admin y tema
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(if (user.isOver13Confirmed) stringResource(R.string.public_profile_verified_yes) else stringResource(R.string.public_profile_verified_no)) },
                                        leadingIcon = { Icon(Icons.Default.Verified, contentDescription = null) },
                                        colors = AssistChipDefaults.assistChipColors()
                                    )
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(if (user.esAdmin) stringResource(R.string.public_profile_admin) else stringResource(R.string.public_profile_user)) },
                                        leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null) },
                                        colors = AssistChipDefaults.assistChipColors()
                                    )
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(if (user.isDarkTheme) stringResource(R.string.public_profile_dark_mode) else stringResource(R.string.public_profile_light_mode)) },
                                        leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                                        colors = AssistChipDefaults.assistChipColors()
                                    )
                                }

                                // Panel con campos del perfil (nombre, email, foto, edad)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(stringResource(R.string.public_profile_fields_title), fontWeight = FontWeight.SemiBold)
                                        ProfileFieldRow(stringResource(R.string.public_profile_field_name), user.displayName)
                                        ProfileFieldRow(stringResource(R.string.public_profile_field_email), user.email)
                                        ProfileFieldRow(stringResource(R.string.public_profile_field_photo), if (user.photoUrl.isNullOrBlank()) stringResource(R.string.public_profile_field_no) else stringResource(R.string.public_profile_field_yes))
                                        ProfileFieldRow(stringResource(R.string.public_profile_field_age), if (user.isOver13Confirmed) stringResource(R.string.public_profile_field_yes) else stringResource(R.string.public_profile_field_no))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.public_profile_trips_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.public_profile_trips_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Sección de viajes publicados: lista o estado vacío
                    if (uiState.publishedTrips.isEmpty()) {
                        item {
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(stringResource(R.string.public_profile_no_trips))
                                }
                            }
                        }
                    } else {
                        items(uiState.publishedTrips, key = { it.id }) { trip ->
                            PublicProfileTripCard(trip)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileFieldRow(label: String, value: String) {
    // Fila simple que muestra una etiqueta y su valor correspondiente
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "-" }, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PublicProfileTripCard(trip: TravelPost) {
    // Tarjeta compacta que muestra la información básica de un `TravelPost`
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(trip.destination, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.public_profile_trip_summary, trip.days, trip.budget.toInt(), trip.currency), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(trip.description.ifBlank { stringResource(R.string.comments_empty) }, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}
