package com.paulaizurrategui.urtriply.ui.screens

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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import com.paulaizurrategui.urtriply.data.trips.TripStatus
import com.paulaizurrategui.urtriply.domain.model.Hotel
import com.paulaizurrategui.urtriply.domain.model.SuggestedActivity
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold
import com.paulaizurrategui.urtriply.ui.theme.UrCream
import com.paulaizurrategui.urtriply.ui.theme.UrOrange
import java.text.NumberFormat
import androidx.compose.runtime.remember as rememberRuntime

@Composable
fun PlanResultScreen(
    isGuest: Boolean,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit
) {
    val r = PlanResultStore.lastResult
    val vm = remember { PlanResultViewModel() }
    val uiState by vm.uiState.collectAsState()

    val isPublished = uiState.currentStatus == TripStatus.PUBLISHED

    val dialogText = uiState.errorMessage ?: uiState.successMessage
    if (dialogText != null) {
        AlertDialog(
            onDismissRequest = { vm.clearMessages() },
            title = { Text(if (uiState.errorMessage != null) "Error" else "Aviso") },
            text = { Text(dialogText) },
            confirmButton = { TextButton(onClick = { vm.clearMessages() }) { Text("OK") } }
        )
    }

    // clave: ocultamos header y titulo para que no haya espacio extra arriba
    UrTriplyGradientScaffold(
        title = "",
        showHeader = false,
        showTitle = false
    ) {
        if (r == null) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = "No hay ninguna propuesta generada. Vuelve al formulario y crea una nueva.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Volver") }
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
                        Text("← Volver", fontWeight = FontWeight.SemiBold)
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
                                    uiState.isSaving -> "Guardando..."
                                    isPublished -> "Guardado"
                                    else -> "Guardar borrador"
                                },
                                maxLines = 1,
                                fontSize = 12.sp
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
                                    uiState.isSaving -> "Publicando..."
                                    isPublished -> "Ya publicado"
                                    else -> "Publicar"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { /* TODO: Compartir */ },
                        enabled = !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Compartir")
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
                                text = "Viaje a ${r.destino}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(6.dp))

                            // debug para comprobar api (si no quieres esto luego, lo quitas)
                            if (r.destinoDisplayName != null && r.lat != null && r.lon != null) {
                                Text(
                                    text = "api ok: ${r.destinoDisplayName} (${r.lat}, ${r.lon})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(6.dp))
                            } else {
                                Text(
                                    text = "api: sin datos (fallback)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(6.dp))
                            }

                            Text(
                                text = "Duración recomendada: ${r.diasRecomendados} días",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(Modifier.height(12.dp))

                            if (r.usedFallback) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = UrCream),
                                    shape = RoundedCornerShape(18.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                                ) {
                                    Column(Modifier.padding(14.dp)) {
                                        Text(
                                            text = "Aviso: se han usado estimaciones (fallback).",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Cuando conecte las APIs, verás precios reales y enlaces.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            SectionTitle("Presupuesto por categorías")
                            Spacer(Modifier.height(8.dp))
                            BudgetCards(r.presupuestoCategorias)

                            Spacer(Modifier.height(14.dp))

                            SectionTitle("Itinerario por días")
                            Spacer(Modifier.height(8.dp))
                            ItineraryCards(r.itinerario)

                            Spacer(Modifier.height(14.dp))

                            SectionTitle("Actividades recomendadas")
                            Spacer(Modifier.height(8.dp))
                            ActivitiesBlock(
                                gratis = r.actividadesGratis,
                                pago = r.actividadesPago
                            )

                            Spacer(Modifier.height(14.dp))

                            SectionTitle("Actividades recomendadas reales")
                            Spacer(Modifier.height(8.dp))
                            RealActivitiesBlock(
                                activities = r.actividadesReales,
                                apiOk = r.apiActividadesOk
                            )

                            Spacer(Modifier.height(14.dp))

                            SectionTitle("Alojamiento recomendado")
                            Spacer(Modifier.height(8.dp))
                            HotelsBlock(
                                hoteles = r.hoteles,
                                apiHotelesOk = r.apiHotelesOk
                            )

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
        Text("No hay datos de presupuesto.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun ItineraryCards(itinerario: List<String>) {
    if (itinerario.isEmpty()) {
        Text("No hay itinerario.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            Text("Gratis", fontWeight = FontWeight.Bold)
            if (gratis.isEmpty()) {
                Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                gratis.forEach { Text("• $it") }
            }

            Divider()

            Text("De pago", fontWeight = FontWeight.Bold)
            if (pago.isEmpty()) {
                Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                pago.forEach { Text("• $it") }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "No incluyen enlaces ni precios reales.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HotelsBlock(
    hoteles: List<Hotel>,
    apiHotelesOk: Boolean
) {
    if (hoteles.isEmpty()) {
        Text(
            text = "No hay hoteles disponibles para este destino.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!apiHotelesOk) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = UrCream),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "Aviso: el alojamiento mostrado es fallback estimado.",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        hoteles.forEach { hotel ->
            HotelCard(hotel = hotel)
        }
    }
}

@Composable
private fun RealActivitiesBlock(
    activities: List<SuggestedActivity>,
    apiOk: Boolean
) {
    if (activities.isEmpty()) {
        Text(
            text = "No hay actividades reales disponibles para este destino.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!apiOk) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = UrCream),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "Aviso: las actividades mostradas son fallback estimado (API lenta).",
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        activities.forEach { activity ->
            ActivityCard(activity = activity)
        }
    }
}

@Composable
private fun ActivityCard(activity: SuggestedActivity) {
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
                text = if (activity.isFree) "Gratis" else "€${String.format("%.0f", activity.price)}",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (activity.isReal) "Datos reales de OpenStreetMap" else "Fallback estimado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (activity.bookingUrl != null) {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activity.bookingUrl))
                    context.startActivity(intent)
                }) {
                    Text("Ver enlace")
                }
            }
        }
    }
}

@Composable
private fun HotelCard(hotel: Hotel) {
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
                            text = "${hotel.stars} estrellas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "€${String.format("%.0f", hotel.pricePerNight)}/noche",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = if (hotel.isReal) "Datos reales de OpenStreetMap" else "Fallback estimado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hotel.totalPrice != null) {
                Text(
                    text = "Total estimado: €${String.format("%.0f", hotel.totalPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (hotel.bookingUrl != null) {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(hotel.bookingUrl))
                    context.startActivity(intent)
                }) {
                    Text("Ver enlace de reserva")
                }
            }
        }
    }
}