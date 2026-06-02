package com.paulaizurrategui.urtriply.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.google.firebase.auth.FirebaseAuth
import com.paulaizurrategui.urtriply.ui.components.CommentSection
import com.paulaizurrategui.urtriply.ui.viewmodels.CommentViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paulaizurrategui.urtriply.data.trips.TripStatus
import com.paulaizurrategui.urtriply.domain.model.Hotel
import com.paulaizurrategui.urtriply.domain.model.SuggestedActivity
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold
import com.paulaizurrategui.urtriply.ui.theme.UrOrange
import java.text.NumberFormat
import com.paulaizurrategui.urtriply.R
import androidx.compose.runtime.remember as rememberRuntime

@Composable
fun PlanResultScreen(
    isGuest: Boolean,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    // Pantalla principal que muestra el `PlanResult` generado por el asistente.
    // - `isGuest`: si true, algunas acciones (guardar/publicar) pedirán login antes.
    // - `onBack`: callback para navegar atrás.
    // - `onRequireLogin`: callback cuando se necesita que el usuario inicie sesión.
    // - `onOpenUrl`: abre URLs externas (hoteles, vuelos, actividades).
    val r = PlanResultStore.lastResult
    // `r` contiene el último PlanResult calculado por el generator
    val context = LocalContext.current
    // ViewModel que maneja guardado/publicado del plan
    val vm: PlanResultViewModel = viewModel()
    // ViewModel para gestionar comentarios del trip (si existe tripId)
    val commentVm: CommentViewModel = viewModel()
    // Estado reactivo del viewmodel del plan
    val uiState by vm.uiState.collectAsState()
    // Lista de comentarios expuesta por el CommentViewModel
    val comments by commentVm.comments.collectAsState()
    // Usuario actual (null si invitado)
    val currentUser = FirebaseAuth.getInstance().currentUser
    // Flag rápido para saber si el plan ya está publicado
    val isPublished = uiState.currentStatus == TripStatus.PUBLISHED

    // Cargar comentarios solo cuando exista un `tripId` válido en el resultado
    // Esto evita consultas a Firestore para propuestas que aún son solo locales (no publicadas)
    LaunchedEffect(r?.tripId) {
        r?.tripId?.takeIf { it.isNotBlank() }?.let { validTripId ->
            commentVm.loadCommentsForTrip(validTripId)
        }
    }

    // Mensajes de éxito/error provenientes del ViewModel.
    // Si `errorMessage` o `successMessage` no son nulos, mostramos un AlertDialog.
    val dialogText = uiState.errorMessage ?: uiState.successMessage
    if (dialogText != null) {
        AlertDialog(
            onDismissRequest = { vm.clearMessages() },
            title = { Text(if (uiState.errorMessage != null) stringResource(R.string.dialog_error_title) else stringResource(R.string.plan_result_warning_title)) },
            text = { Text(dialogText) },
            confirmButton = { TextButton(onClick = { vm.clearMessages() }) { Text(stringResource(R.string.plan_result_ok)) } }
        )
    }

    // Layout contenedor principal con gradiente y sin header/título para ahorrar espacio arriba
    UrTriplyGradientScaffold(
        title = "",
        showHeader = false,
        showTitle = false
    ) {
        // Preparar textos para la acción de compartir (subject y chooser title)
        val shareSubject = stringResource(R.string.plan_result_share_subject, r?.destino ?: "")
        val shareChooser = stringResource(R.string.plan_result_share_chooser)
        if (r == null) {
            // Estado vacío: no hay PlanResult para mostrar (posible error o navegación directa)
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = stringResource(R.string.plan_result_empty_state),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                // Volver a la pantalla anterior
                Button(onClick = onBack) { Text(stringResource(R.string.plan_result_back)) }
            }
            return@UrTriplyGradientScaffold
        }

        val scroll = rememberScrollState()

        Scaffold(
            // Barra superior compacta con botón atrás
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        // menos padding para que quede mas arriba/compacto
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Text(
                            stringResource(R.string.plan_result_back_arrow),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            },
            // Barra inferior con acciones principales: Guardar, Publicar y Compartir
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Fila con botones Guardar borrador y Publicar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (isGuest) onRequireLogin()
                                else vm.saveDraft(r)
                            },
                            enabled = !uiState.isSaving && !isPublished,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = when {
                                    uiState.isSaving -> stringResource(R.string.plan_result_saving)
                                    isPublished -> stringResource(R.string.plan_result_saved)
                                    else -> stringResource(R.string.trip_save_draft)
                                },
                                maxLines = 1,
                                fontSize = 12.sp,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Botón de publicar: si es invitado se solicita login
                        Button(
                            onClick = {
                                if (isGuest) onRequireLogin()
                                else vm.publish(r)
                            },
                            enabled = !uiState.isSaving && !isPublished,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                        ) {
                            Text(
                                when {
                                    uiState.isSaving -> stringResource(R.string.plan_result_publishing)
                                    isPublished -> stringResource(R.string.plan_result_published)
                                    else -> stringResource(R.string.trip_publish)
                                },
                                fontWeight = FontWeight.Bold
                                ,maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Botón para compartir: genera el texto de la propuesta y lanza
                    // un Intent chooser para compartir por apps externas.
                    OutlinedButton(
                        onClick = {
                            val shareText = buildShareText(context, r)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, shareChooser))
                        },
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.plan_result_share))
                    }
                }
            }
        ) { inner ->
            Box(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxWidth()
            ) {
                // Contenedor scrollable principal con ancho limitado para pantallas grandes
                Column(
                    modifier = Modifier
                        .verticalScroll(scroll)
                        .fillMaxWidth()
                        // menos padding arriba para acercar el contenido al boton volver
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tarjeta principal que contiene los detalles completos del plan
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 18.dp)
                        ) {
                            // Título grande con el destino recomendado
                            Text(
                                text = stringResource(R.string.plan_result_trip_title, r.destino),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(6.dp))

                            // Pequeña sección de diagnóstico: si la API devolvió nombre y coordenadas,
                            // mostramos un texto informativo (útil en desarrollo).
                            if (r.destinoDisplayName != null && r.lat != null && r.lon != null) {
                                Text(
                                    text = stringResource(R.string.plan_result_api_ok, r.destinoDisplayName ?: "", r.lat ?: 0.0, r.lon ?: 0.0),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(6.dp))
                            } else {
                                Text(
                                    text = stringResource(R.string.plan_result_api_fallback),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(6.dp))
                            }

                            // Duración recomendada del viaje
                            Text(
                                text = stringResource(R.string.plan_result_duration, r.diasRecomendados),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(Modifier.height(12.dp))

                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.plan_result_pricing_note),
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Sección de itinerario (por días). Puede provenir de la API o del fallback local
                            SectionTitle(stringResource(R.string.plan_result_section_itinerary))
                            Spacer(Modifier.height(8.dp))
                            ItineraryCards(
                                itineraryByDay = r.itineraryByDay,
                                itinerario = r.itinerario,
                                onOpenUrl = onOpenUrl
                            )

                            Spacer(Modifier.height(14.dp))

                            // Sección de hoteles: mostramos solo hoteles reales (no sugerencias vacías)
                            SectionTitle(stringResource(R.string.plan_result_section_hotel))
                            Spacer(Modifier.height(8.dp))
                            HotelsBlock(
                                hoteles = r.hoteles,
                                apiHotelesOk = r.apiHotelesOk,
                                onOpenUrl = onOpenUrl
                            )

                            Spacer(Modifier.height(14.dp))

                            // Sección de vuelos: ofertas reales si la API las devolvió
                            SectionTitle(stringResource(R.string.plan_result_section_flights))
                            Spacer(Modifier.height(8.dp))
                            FlightsBlock(
                                flights = r.vuelos,
                                apiOk = r.apiVuelosOk,
                                onOpenUrl = onOpenUrl
                            )

                            Spacer(Modifier.height(14.dp))

                            // Sección de comentarios: solo aparece para propuestas publicadas
                            // porque requiere un `tripId` asociado en Firestore.
                            if (isPublished && r.tripId != null) {
                                SectionTitle(stringResource(R.string.plan_result_section_comments))
                                Spacer(Modifier.height(8.dp))
                                CommentSection(
                                    tripId = r.tripId ?: "",
                                    comments = comments,
                                    isLoading = commentVm.isLoading.value,
                                    onAddComment = { text ->
                                        if (currentUser == null) {
                                            onRequireLogin()
                                        } else {
                                            commentVm.addComment(text)
                                        }
                                    },
                                    onDeleteComment = { commentId ->
                                        commentVm.deleteComment(commentId)
                                    },
                                    currentUserId = currentUser?.uid,
                                    isAdmin = false  // TODO: Check if current user is admin
                                )
                                Spacer(Modifier.height(14.dp))
                            }

                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    Spacer(Modifier.height(90.dp))
                }
            }
        }
    }
}

// Componente pequeño que renderiza el título de una sección dentro del resultado.
// Usado para: Itinerario, Hoteles, Vuelos, Comentarios, etc.
@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold
    )
}

// Muestra un conjunto de tarjetas con el desglose presupuestario por categoría.
// Recibe un map `categorias` con etiqueta->importe y formatea según locale.
@Composable
private fun BudgetCards(categorias: Map<String, Double>) {
    if (categorias.isEmpty()) {
        Text(stringResource(R.string.plan_result_no_budget), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    val composeLocale = LocalLocale.current
    val javaLocale = rememberRuntime(composeLocale) {
        java.util.Locale.forLanguageTag(composeLocale.toLanguageTag())
    }
    val numberFormat = rememberRuntime(javaLocale) {
        NumberFormat.getNumberInstance(javaLocale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        categorias.entries.forEach { (label, value) ->
            val formatted = rememberRuntime(value, javaLocale) { numberFormat.format(value) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(label, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "€ $formatted",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(50),
                        colors = CardDefaults.cardColors(containerColor = UrOrange.copy(alpha = 0.14f))
                    ) {
                        Text(
                            text = "€ $formatted",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = UrOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Presenta el itinerario por días cuando existe (`itineraryByDay`) o
// muestra el itinerario fallback como lista de strings.
@Composable
private fun ItineraryCards(
    itineraryByDay: List<ItineraryDay>,
    itinerario: List<String>,
    onOpenUrl: (String) -> Unit
) {
    if (itineraryByDay.isEmpty() && itinerario.isEmpty()) {
        Text(stringResource(R.string.plan_result_no_itinerary), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (itineraryByDay.isNotEmpty()) {
            val context = LocalContext.current
            itineraryByDay.forEach { day ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(day.dayLabel.ifBlank { stringResource(R.string.plan_result_day_label, 1) }, fontWeight = FontWeight.Bold)
                        Text(day.summary, fontWeight = FontWeight.SemiBold)

                        if (day.activities.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.plan_result_activity_links_title),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    day.activities.forEach { activity ->
                                        val bookingUrl = activity.bookingUrl
                                        if (!bookingUrl.isNullOrBlank()) {
                                            TextButton(onClick = {
                                                onOpenUrl(bookingUrl)
                                            }) {
                                                Text(activity.name)
                                            }
                                        } else {
                                            Text(activity.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                            }
                        }
                    }
                }
            }
            return
        }

        itinerario.forEach { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(item, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// Bloque que lista actividades gratuitas y de pago en forma compacta.
@Composable
private fun ActivitiesBlock(
    gratis: List<String>,
    pago: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.plan_result_free_label), fontWeight = FontWeight.Bold)
            if (gratis.isEmpty()) {
                Text(stringResource(R.string.plan_result_empty_dash), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                gratis.forEach { Text(stringResource(R.string.plan_result_bullet, it)) }
            }

            Divider()

            Text(stringResource(R.string.plan_result_paid_label), fontWeight = FontWeight.Bold)
            if (pago.isEmpty()) {
                Text(stringResource(R.string.plan_result_empty_dash), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                pago.forEach { Text(stringResource(R.string.plan_result_bullet, it)) }
            }
        }
    }
}

// Bloque que muestra tarjetas de hoteles filtrando solo los reales
// (evita mostrar sugerencias vacías cuando la API no respondió).
@Composable
private fun HotelsBlock(
    hoteles: List<Hotel>,
    apiHotelesOk: Boolean,
    onOpenUrl: (String) -> Unit
) {
    val realHotels = hoteles.filter { it.isReal }

    if (realHotels.isEmpty()) {
        Text(
            text = stringResource(R.string.plan_result_no_hotels),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        realHotels.forEach { hotel ->
            HotelCard(hotel = hotel, onOpenUrl = onOpenUrl)
        }
    }
}

// Bloque para actividades que provienen de APIs reales (no fallback).
@Composable
private fun RealActivitiesBlock(
    activities: List<SuggestedActivity>,
    apiOk: Boolean,
    onOpenUrl: (String) -> Unit
) {
    val realActivities = activities.filter { it.isReal }

    if (realActivities.isEmpty()) {
        Text(
            text = stringResource(R.string.plan_result_no_activities),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        realActivities.forEach { activity ->
            ActivityCard(activity = activity, onOpenUrl = onOpenUrl)
        }
    }
}

// Tarjeta individual que representa una `SuggestedActivity`.
// Muestra nombre, categoría, precio y link de reserva cuando exista.
@Composable
private fun ActivityCard(activity: SuggestedActivity, onOpenUrl: (String) -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = activity.name,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = activity.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (activity.isFree) stringResource(R.string.plan_result_activity_price_free) else stringResource(R.string.plan_result_activity_price, String.format("%.0f", activity.price)),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.plan_result_real_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val bookingUrl = activity.bookingUrl
            if (!bookingUrl.isNullOrBlank()) {
                TextButton(onClick = {
                    onOpenUrl(bookingUrl)
                }) {
                    Text(stringResource(R.string.plan_result_link))
                }
            }
        }
    }
}

// Tarjeta individual para un `Hotel` con precio por noche y link de reserva.
@Composable
private fun HotelCard(hotel: Hotel, onOpenUrl: (String) -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hotel.name,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                        if (hotel.stars != null) {
                            Text(
                                text = stringResource(R.string.plan_result_stars, hotel.stars),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                }

                Text(
                    text = stringResource(R.string.plan_result_price_per_night, String.format("%.0f", hotel.pricePerNight)),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = stringResource(R.string.plan_result_real_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hotel.totalPrice != null) {
                Text(
                    text = stringResource(R.string.plan_result_total_estimated, hotel.totalPrice ?: 0.0),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val bookingUrl = hotel.bookingUrl
            if (!bookingUrl.isNullOrBlank()) {
                TextButton(onClick = {
                    onOpenUrl(bookingUrl)
                }) {
                    Text(stringResource(R.string.plan_result_reservation_link))
                }
            }
        }
    }
}

// Bloque que muestra ofertas de vuelos reales (si las hay).
@Composable
private fun FlightsBlock(
    flights: List<com.paulaizurrategui.urtriply.domain.model.FlightOffer>,
    apiOk: Boolean,
    onOpenUrl: (String) -> Unit
) {
    val realFlights = flights.filter { it.isReal }

    if (realFlights.isEmpty()) {
        Text(
            text = stringResource(R.string.plan_result_no_flights),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        realFlights.forEach { flight ->
            FlightCard(flight, onOpenUrl = onOpenUrl)
        }
    }
}

// Tarjeta que muestra detalles de una oferta de vuelo: origen/destino,
// fechas, duración, precio y enlace de reserva si existe.
@Composable
private fun FlightCard(flight: com.paulaizurrategui.urtriply.domain.model.FlightOffer, onOpenUrl: (String) -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "${flight.origin} → ${flight.destination}", fontWeight = FontWeight.Bold)

            Text(text = stringResource(R.string.plan_result_flight_departure, flight.departureDate, ""))
            if (flight.returnDate != null) {
                Text(text = stringResource(R.string.plan_result_flight_return, flight.returnDate))
            }

            Text(text = stringResource(R.string.plan_result_flight_duration, flight.durationMinutes, flight.carrier), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text(text = stringResource(R.string.plan_result_flight_price, flight.currency, String.format("%.2f", flight.price)), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

            Text(text = stringResource(R.string.plan_result_real_data), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            val bookingUrl = flight.bookingUrl
            if (!bookingUrl.isNullOrBlank()) {
                TextButton(onClick = {
                    onOpenUrl(bookingUrl)
                }) {
                    Text(stringResource(R.string.plan_result_flight_book))
                }
            }
        }
    }
}

/**
 * Construye un texto con los detalles de la propuesta para compartir
 */
private fun buildShareText(context: android.content.Context, proposal: PlanResult): String {
    val sb = StringBuilder()
    sb.append(context.getString(R.string.plan_result_share_intro, proposal.destino)).append("\n\n")

    sb.append(context.getString(R.string.plan_result_share_details)).append("\n")
    sb.append(context.getString(R.string.plan_result_share_destination, proposal.destino)).append("\n")
    sb.append(context.getString(R.string.plan_result_share_duration, proposal.diasRecomendados)).append("\n")
    sb.append(context.getString(R.string.plan_result_share_travelers, proposal.viajeros)).append("\n")
    sb.append(context.getString(R.string.plan_result_share_total_budget, proposal.presupuestoTotal)).append("\n\n")

    if (proposal.presupuestoCategorias.isNotEmpty()) {
        sb.append(context.getString(R.string.plan_result_share_budget_section)).append("\n")
        proposal.presupuestoCategorias.forEach { (categoria, cantidad) ->
            sb.append("• $categoria: €${String.format("%.2f", cantidad)}\n")
        }
        sb.append("\n")
    }
    
    if (proposal.itineraryByDay.isNotEmpty()) {
        sb.append(context.getString(R.string.plan_result_share_itinerary_section)).append("\n")
        proposal.itineraryByDay.forEach { day ->
            sb.append("• ${day.dayLabel} ${day.summary}\n")
            day.activities.forEach { activity ->
                sb.append("   - ${activity.name}")
                activity.bookingUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    sb.append(" ($url)")
                }
                sb.append("\n")
            }
        }
        sb.append("\n")
    } else if (proposal.itinerario.isNotEmpty()) {
        sb.append(context.getString(R.string.plan_result_share_itinerary_section)).append("\n")
        proposal.itinerario.forEach { dia ->
            sb.append("• $dia\n")
        }
        sb.append("\n")
    }
    
    sb.append(context.getString(R.string.plan_result_share_generated_with)).append("\n")
    
    return sb.toString()
}