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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
    val r = PlanResultStore.lastResult
    val context = LocalContext.current
    val vm: PlanResultViewModel = viewModel()
    val commentVm: CommentViewModel = viewModel()
    val uiState by vm.uiState.collectAsState()
    val comments by commentVm.comments.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val isPublished = uiState.currentStatus == TripStatus.PUBLISHED

    // Load comments only when we have a valid Firestore trip id
    LaunchedEffect(r?.tripId) {
        r?.tripId?.takeIf { it.isNotBlank() }?.let { validTripId ->
            commentVm.loadCommentsForTrip(validTripId)
        }
    }

    val dialogText = uiState.errorMessage ?: uiState.successMessage
    if (dialogText != null) {
        AlertDialog(
            onDismissRequest = { vm.clearMessages() },
            title = { Text(if (uiState.errorMessage != null) stringResource(R.string.dialog_error_title) else stringResource(R.string.plan_result_warning_title)) },
            text = { Text(dialogText) },
            confirmButton = { TextButton(onClick = { vm.clearMessages() }) { Text(stringResource(R.string.plan_result_ok)) } }
        )
    }

    // clave: ocultamos header y titulo para que no haya espacio extra arriba
    UrTriplyGradientScaffold(
        title = "",
        showHeader = false,
        showTitle = false
    ) {
        val shareSubject = stringResource(R.string.plan_result_share_subject, r?.destino ?: "")
        val shareChooser = stringResource(R.string.plan_result_share_chooser)
        if (r == null) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = stringResource(R.string.plan_result_empty_state),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) { Text(stringResource(R.string.plan_result_back)) }
            }
            return@UrTriplyGradientScaffold
        }

        val scroll = rememberScrollState()

        Scaffold(
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
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
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
                Column(
                    modifier = Modifier
                        .verticalScroll(scroll)
                        .fillMaxWidth()
                        // menos padding arriba para acercar el contenido al boton volver
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                            Text(
                                text = stringResource(R.string.plan_result_trip_title, r.destino),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(6.dp))

                            if (r.usedFallback || !r.apiHotelesOk || !r.apiActividadesOk || !r.apiVuelosOk) {
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = stringResource(R.string.plan_result_warning_title),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(R.string.plan_result_api_fallback),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            } else if (r.destinoDisplayName != null && r.lat != null && r.lon != null) {
                                Text(
                                    text = stringResource(R.string.plan_result_api_ok, r.destinoDisplayName ?: "", r.lat ?: 0.0, r.lon ?: 0.0),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(6.dp))
                            }

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
                                    text = if (r.usedFallback || !r.apiHotelesOk || !r.apiActividadesOk || !r.apiVuelosOk) {
                                        stringResource(R.string.plan_result_pricing_note)
                                    } else {
                                        stringResource(R.string.plan_result_real_data)
                                    },
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text(
                                        text = stringResource(R.string.plan_result_legal_notice),
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(Modifier.height(10.dp))

                            SectionTitle(stringResource(R.string.plan_result_section_itinerary))
                            Spacer(Modifier.height(8.dp))
                            ItineraryCards(
                                itineraryByDay = r.itineraryByDay,
                                itinerario = r.itinerario,
                                onOpenUrl = onOpenUrl
                            )

                            Spacer(Modifier.height(14.dp))

                            SectionTitle(stringResource(R.string.plan_result_section_hotel))
                            Spacer(Modifier.height(8.dp))
                            HotelsBlock(
                                hoteles = r.hoteles,
                                apiHotelesOk = r.apiHotelesOk,
                                onOpenUrl = onOpenUrl
                            )

                            Spacer(Modifier.height(14.dp))

                            SectionTitle(stringResource(R.string.plan_result_section_flights))
                            Spacer(Modifier.height(8.dp))
                            FlightsBlock(
                                flights = r.vuelos,
                                apiOk = r.apiVuelosOk,
                                onOpenUrl = onOpenUrl
                            )

                            Spacer(Modifier.height(14.dp))

                            // Comments section (only if trip is published)
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

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold
    )
}

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

@Composable
private fun HotelsBlock(
    hoteles: List<Hotel>,
    apiHotelesOk: Boolean,
    onOpenUrl: (String) -> Unit
) {
    if (hoteles.isEmpty()) {
        Text(
            text = stringResource(R.string.plan_result_no_hotels),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        hoteles.forEach { hotel ->
            HotelCard(hotel = hotel, onOpenUrl = onOpenUrl)
        }
    }
}

@Composable
private fun RealActivitiesBlock(
    activities: List<SuggestedActivity>,
    apiOk: Boolean,
    onOpenUrl: (String) -> Unit
) {
    if (activities.isEmpty()) {
        Text(
            text = stringResource(R.string.plan_result_no_activities),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        activities.forEach { activity ->
            ActivityCard(activity = activity, onOpenUrl = onOpenUrl)
        }
    }
}

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

            SourceLabel(isReal = activity.isReal)

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

            SourceLabel(isReal = hotel.isReal)

            if (hotel.totalPrice != null) {
                Text(
                    text = if (hotel.isReal) {
                        stringResource(R.string.plan_result_total_real, hotel.totalPrice ?: 0.0)
                    } else {
                        stringResource(R.string.plan_result_total_estimated, hotel.totalPrice ?: 0.0)
                    },
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

            SourceLabel(isReal = flight.isReal)

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

@Composable
private fun SourceLabel(isReal: Boolean) {
    AssistChip(
        onClick = { },
        enabled = false,
        label = {
            Text(
                text = if (isReal) {
                    stringResource(R.string.plan_result_real_data)
                } else {
                    stringResource(R.string.plan_result_fallback_data)
                },
                style = MaterialTheme.typography.labelMedium
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = if (isReal) {
                UrOrange.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
            },
            disabledLabelColor = if (isReal) {
                UrOrange
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
            disabledLeadingIconContentColor = if (isReal) {
                UrOrange
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            }
        )
    )
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