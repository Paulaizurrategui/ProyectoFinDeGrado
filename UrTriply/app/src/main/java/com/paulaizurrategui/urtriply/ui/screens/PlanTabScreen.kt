package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.util.Log

import com.paulaizurrategui.urtriply.data.remote.overpass.ActivitiesRepository
import com.paulaizurrategui.urtriply.data.remote.overpass.HotelsRepository
import com.paulaizurrategui.urtriply.data.remote.nominatim.GeocodingRepository
import com.paulaizurrategui.urtriply.domain.model.Hotel
import com.paulaizurrategui.urtriply.domain.model.SuggestedActivity
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import com.paulaizurrategui.urtriply.R

private enum class Preference(val label: String) {
    CULTURA("Cultura"),
    OCIO("Ocio nocturno"),
    NATURALEZA("Naturaleza"),
    GASTRONOMIA("Gastronomía")
}

private fun destinationToIata(destination: String): String {
    val value = destination.substringBefore("(").trim().lowercase(Locale.getDefault())
    return when {
        value.contains("parís") || value.contains("paris") -> "PAR"
        value.contains("londres") -> "LON"
        value.contains("roma") -> "ROM"
        value.contains("ámsterdam") || value.contains("amsterdam") -> "AMS"
        value.contains("atenas") -> "ATH"
        value.contains("lisboa") -> "LIS"
        value.contains("berlín") || value.contains("berlin") -> "BER"
        value.contains("praga") -> "PRG"
        value.contains("viena") -> "VIE"
        value.contains("dublín") || value.contains("dublin") -> "DUB"
        else -> "MAD"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTabScreen(
    isGuest: Boolean,
    onNavigateToResult: () -> Unit
) {
    UrTriplyGradientScaffold(title = stringResource(R.string.plan_title)) {
        val destinos = listOf(
            "París (Francia)",
            "Londres (Reino Unido)",
            "Roma (Italia)",
            "Ámsterdam (Países Bajos)",
            "Atenas (Grecia)",
            "Lisboa (Portugal)",
            "Berlín (Alemania)",
            "Praga (República Checa)",
            "Viena (Austria)",
            "Dublín (Irlanda)"
        )

        // --- estado formulario ---
        var destino by remember { mutableStateOf(destinos.first()) }
        var presupuestoText by remember { mutableStateOf("") }
        var viajerosText by remember { mutableStateOf("1") }
        var fechaInicioMillis by remember { mutableStateOf<Long?>(null) }
        var fechaFinMillis by remember { mutableStateOf<Long?>(null) }
        var prefs by remember { mutableStateOf(setOf<Preference>()) }

        // --- ui state ---
        var showStartPicker by remember { mutableStateOf(false) }
        var showEndPicker by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var localError by remember { mutableStateOf<String?>(null) }

        val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
        fun formatDate(ms: Long?): String = ms?.let { dateFormat.format(Date(it)) } ?: "-"

        val scrollState = rememberScrollState()

        // coroutines (sin crear scope manual)
        val scope = rememberCoroutineScope()

        // repo geocoding (nominatim)
        val geocodingRepo = remember { GeocodingRepository() }
        val hotelsRepo = remember { HotelsRepository() }
        val activitiesRepo = remember { ActivitiesRepository() }
        val flightsRepo = remember { com.paulaizurrategui.urtriply.data.remote.flights.FlightsRepository() }
        val queryCity = destino.substringBefore("(").trim()
        val destinationIata = destinationToIata(destino)
        val effectiveOriginIata = "MAD"

        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 18.dp)
            ) {
                // intro
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.plan_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isGuest)
                                "Modo invitado: puedes generar propuestas, pero para guardar/publicar necesitarás iniciar sesión."
                            else
                                "Completa el formulario para generar una propuesta ajustada a tu presupuesto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // destino
                    SectionTitle(title = stringResource(R.string.plan_destination_label), subtitle = stringResource(R.string.plan_choose_destination))
                Spacer(Modifier.height(8.dp))
                DestinationDropdown(
                    destinos = destinos,
                    selected = destino,
                    onSelected = { destino = it }
                )

                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.plan_origin_fixed),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.plan_origin_madrid),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // presupuesto y viajeros
                SectionTitle(title = stringResource(R.string.plan_budget_travelers_title), subtitle = stringResource(R.string.plan_budget_travelers_subtitle))
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = presupuestoText,
                            onValueChange = { presupuestoText = it.replace(",", ".") },
                            label = { Text(stringResource(R.string.plan_budget_total)) },
                            trailingIcon = { Text(stringResource(R.string.plan_currency), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text(
                            text = stringResource(R.string.plan_budget_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = viajerosText,
                            onValueChange = { viajerosText = it.filter(Char::isDigit) },
                            label = { Text(stringResource(R.string.plan_travelers)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // fechas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.plan_dates), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                        DateRowField(
                            label = stringResource(R.string.plan_start_date),
                            value = formatDate(fechaInicioMillis),
                            onClick = { showStartPicker = true }
                        )
                        DateRowField(
                            label = stringResource(R.string.plan_end_date),
                            value = formatDate(fechaFinMillis),
                            onClick = { showEndPicker = true }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // preferencias
                SectionTitle(title = stringResource(R.string.plan_preferences), subtitle = stringResource(R.string.plan_preferences_subtitle))
                Spacer(Modifier.height(8.dp))

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
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Preference.entries.take(2).forEach { pref ->
                                    FilterChip(
                                        selected = prefs.contains(pref),
                                        onClick = {
                                            prefs = if (prefs.contains(pref)) prefs - pref else prefs + pref
                                        },
                                        label = { Text(pref.label) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Preference.entries.drop(2).forEach { pref ->
                                    FilterChip(
                                        selected = prefs.contains(pref),
                                        onClick = {
                                            prefs = if (prefs.contains(pref)) prefs - pref else prefs + pref
                                        },
                                        label = { Text(pref.label) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.plan_flight_route),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.plan_route_outbound, queryCity, destinationIata),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.plan_route_return, queryCity, destinationIata),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // error local validacion
                localError?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // boton generar
                Button(
                    onClick = {
                        localError = null

                        val presupuesto = presupuestoText.toDoubleOrNull()
                        val viajeros = viajerosText.toIntOrNull()

                        if (presupuesto == null || presupuesto <= 0) {
                            localError = "Introduce un presupuesto válido (> 0)."
                            return@Button
                        }
                        if (viajeros == null || viajeros <= 0) {
                            localError = "Introduce un número de viajeros válido (> 0)."
                            return@Button
                        }
                        if (fechaInicioMillis == null || fechaFinMillis == null) {
                            localError = "Selecciona fecha de inicio y fin."
                            return@Button
                        }
                        if (fechaFinMillis!! < fechaInicioMillis!!) {
                            localError = "La fecha fin no puede ser anterior a la fecha inicio."
                            return@Button
                        }
                        if (prefs.isEmpty()) {
                            localError = "Selecciona al menos una preferencia."
                            return@Button
                        }

                        isLoading = true

                        scope.launch {
                            // 1) genero local (fallback)
                            val base = generateLocalProposal(
                                destino = destino,
                                presupuestoTotal = presupuesto,
                                viajeros = viajeros,
                                fechaInicioMillis = fechaInicioMillis,
                                fechaFinMillis = fechaFinMillis,
                                prefs = prefs
                            )

                            // 2) geocoding real (se necesita para hoteles/actividades)
                            val geo = geocodingRepo.geocode(queryCity)

                            // 3) Parallelizar búsquedas remotas para reducir tiempo total
                            var hoteles: List<Hotel>
                            var actividadesReales: List<SuggestedActivity>
                            var vuelosOfertas: List<com.paulaizurrategui.urtriply.domain.model.FlightOffer>

                            try {
                                val resultTriple = withTimeoutOrNull(6000L) {
                                    coroutineScope {
                                        val hotelesDeferred = async {
                                            if (geo == null) return@async emptyList<Hotel>()
                                            try {
                                                hotelsRepo.searchHotels(
                                                    lat = geo.lat,
                                                    lon = geo.lon,
                                                    checkInDate = fechaInicioMillis,
                                                    checkOutDate = fechaFinMillis
                                                )
                                            } catch (e: Throwable) {
                                                emptyList()
                                            }
                                        }

                                        val actividadesDeferred = async {
                                            if (geo == null) return@async emptyList<SuggestedActivity>()
                                            try {
                                                activitiesRepo.searchActivities(
                                                    lat = geo.lat,
                                                    lon = geo.lon,
                                                    prefs = prefs.map { it.label.lowercase(Locale.getDefault()) }.toSet()
                                                )
                                            } catch (e: Throwable) {
                                                emptyList()
                                            }
                                        }

                                        val vuelosDeferred = async {
                                            try {
                                                flightsRepo.searchFlights(
                                                    origin = effectiveOriginIata,
                                                    destination = destinationIata,
                                                    dateFrom = formatDate(fechaInicioMillis),
                                                    dateTo = formatDate(fechaFinMillis)
                                                )
                                            } catch (e: Throwable) {
                                                emptyList()
                                            }
                                        }

                                        Triple(hotelesDeferred.await(), actividadesDeferred.await(), vuelosDeferred.await())
                                    }
                                } ?: Triple(emptyList<Hotel>(), emptyList<SuggestedActivity>(), emptyList())

                                hoteles = resultTriple.first
                                actividadesReales = resultTriple.second
                                vuelosOfertas = resultTriple.third
                            } catch (e: Throwable) {
                                Log.e("PlanTabScreen", "Error parallel searches", e)
                                hoteles = emptyList()
                                actividadesReales = emptyList()
                                vuelosOfertas = emptyList()
                            }



                            // 3) si sale bien, lo guardo en el resultado
                            val finalPlan = if (geo != null) {
                                val itineraryFromActivities = buildItineraryFromActivities(
                                    destino = destino,
                                    diasRecomendados = base.diasRecomendados,
                                    prefs = prefs,
                                    actividades = actividadesReales,
                                    fallbackItinerary = base.itinerario
                                )

                                base.copy(
                                    destinoDisplayName = geo.displayName,
                                    lat = geo.lat,
                                    lon = geo.lon,
                                    hoteles = hoteles,
                                    hotelMesSeleccionado = hoteles.firstOrNull(),
                                    apiHotelesOk = hoteles.any { it.isReal },
                                    actividadesReales = actividadesReales,
                                    apiActividadesOk = actividadesReales.any { it.isReal },
                                    itinerario = itineraryFromActivities,
                                    vuelos = vuelosOfertas,
                                    apiVuelosOk = vuelosOfertas.any { it.isReal }
                                )
                            } else {
                                base
                            }

                            PlanResultStore.lastResult = finalPlan

                            isLoading = false
                            onNavigateToResult()
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(end = 12.dp))
                        Text(stringResource(R.string.plan_generating))
                    } else {
                        Text(stringResource(R.string.plan_generate), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(10.dp))
            }

            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 2.dp),
                scrollState = scrollState
            )
        }

        // date pickers
        if (showStartPicker) {
            val state = rememberDatePickerState(initialSelectedDateMillis = fechaInicioMillis)
            DatePickerDialog(
                onDismissRequest = { showStartPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        fechaInicioMillis = state.selectedDateMillis
                        showStartPicker = false
                    }) { Text(stringResource(R.string.plan_ok)) }
                },
                dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text(stringResource(R.string.plan_cancel)) } }
            ) { DatePicker(state = state) }
        }

        if (showEndPicker) {
            val state = rememberDatePickerState(initialSelectedDateMillis = fechaFinMillis)
            DatePickerDialog(
                onDismissRequest = { showEndPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        fechaFinMillis = state.selectedDateMillis
                        showEndPicker = false
                    }) { Text(stringResource(R.string.plan_ok)) }
                },
                dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text(stringResource(R.string.plan_cancel)) } }
            ) { DatePicker(state = state) }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DateRowField(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(text = value, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onClick) { Text(stringResource(R.string.plan_select)) }
    }
}

@Composable
private fun DestinationDropdown(
    destinos: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.plan_destination_input_label)) },
            trailingIcon = { Text(stringResource(R.string.edit_trip_dropdown)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.plan_choose_destination)) },
            text = {
                LazyColumn {
                    items(destinos) { option ->
                        Text(
                            text = option,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelected(option)
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Divider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.plan_close)) }
            }
        )
    }
}

@Composable
private fun VerticalScrollbar(
    modifier: Modifier,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val max = scrollState.maxValue
    if (max <= 0) return

    val progress = scrollState.value.toFloat() / max.toFloat()

    val trackColor = Color(0x33000000)
    val thumbColor = Color(0x99000000)

    val thumbHeight = 72.dp
    val topPadding = (progress * 140).dp

    Box(
        modifier = modifier
            .width(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .padding(top = topPadding)
                .width(6.dp)
                .height(thumbHeight)
                .clip(RoundedCornerShape(3.dp))
                .background(thumbColor)
        )
    }
}

/**
 * Generación local (MVP). Todavía no usamos APIs reales, por eso usedFallback=true.
 * Cumple RF-13 / RF-14 de forma estimada.
 */
private fun generateLocalProposal(
    destino: String,
    presupuestoTotal: Double,
    viajeros: Int,
    fechaInicioMillis: Long?,
    fechaFinMillis: Long?,
    prefs: Set<Preference>
): PlanResult {
    val transporte = presupuestoTotal * 0.35
    val alojamiento = presupuestoTotal * 0.35
    val comidas = presupuestoTotal * 0.20
    val actividades = presupuestoTotal * 0.10

    // estimación basada en presupuesto por persona y rango de fechas
    val presupuestoPorPersona = presupuestoTotal / maxOf(1, viajeros)
    val diasPorPresupuesto = when {
        presupuestoPorPersona < 250 -> 2
        presupuestoPorPersona < 450 -> 3
        presupuestoPorPersona < 700 -> 4
        presupuestoPorPersona < 950 -> 5
        presupuestoPorPersona < 1300 -> 6
        presupuestoPorPersona < 1700 -> 7
        else -> 8
    }

    val diasDisponibles = if (fechaInicioMillis != null && fechaFinMillis != null) {
        val millisPerDay = 24L * 60L * 60L * 1000L
        (((fechaFinMillis - fechaInicioMillis).coerceAtLeast(0L) / millisPerDay) + 1L)
            .toInt()
            .coerceAtLeast(1)
    } else {
        8
    }

    val bonusPorPreferencias = when {
        prefs.size >= 3 -> 1
        prefs.contains(Preference.CULTURA) && prefs.contains(Preference.GASTRONOMIA) -> 1
        prefs.contains(Preference.NATURALEZA) && prefs.contains(Preference.OCIO) -> 1
        else -> 0
    }

    val diasRecomendados = (diasPorPresupuesto + bonusPorPreferencias)
        .coerceAtMost(diasDisponibles)
        .coerceIn(2, 10)

    val itinerario = (1..diasRecomendados).map { day ->
        val focus = when {
            prefs.contains(Preference.CULTURA) && day % 2 == 1 -> "museos y casco histórico"
            prefs.contains(Preference.NATURALEZA) && day % 3 == 0 -> "parques y miradores"
            prefs.contains(Preference.GASTRONOMIA) -> "ruta gastronómica"
            prefs.contains(Preference.OCIO) -> "zona de ocio nocturno"
            else -> "paseo libre"
        }
        "Día $day: $focus en $destino"
    }

    val actividadesGratis = buildList {
        if (prefs.contains(Preference.CULTURA)) add("Free walking tour (propina) + plazas principales")
        if (prefs.contains(Preference.NATURALEZA)) add("Parque urbano principal + miradores")
        if (prefs.contains(Preference.GASTRONOMIA)) add("Mercado local (ambiente y degustación barata)")
        if (prefs.contains(Preference.OCIO)) add("Barrio con ambiente nocturno")
    }.distinct()

    val actividadesPago = buildList {
        if (prefs.contains(Preference.CULTURA)) add("Entrada a museo emblemático")
        if (prefs.contains(Preference.NATURALEZA)) add("Excursión de medio día fuera de la ciudad")
        if (prefs.contains(Preference.GASTRONOMIA)) add("Tour gastronómico o cena típica")
        if (prefs.contains(Preference.OCIO)) add("Club / espectáculo local")
    }.distinct()

    return PlanResult(
        destino = destino,
        presupuestoTotal = presupuestoTotal,
        viajeros = viajeros,
        fechaInicioMillis = fechaInicioMillis,
        fechaFinMillis = fechaFinMillis,
        diasRecomendados = diasRecomendados,
        presupuestoCategorias = linkedMapOf(
            "Transporte (vuelos MAD)" to transporte,
            "Alojamiento" to alojamiento,
            "Comidas" to comidas,
            "Actividades" to actividades
        ),
        itinerario = itinerario,
        actividadesGratis = actividadesGratis,
        actividadesPago = actividadesPago,
        usedFallback = true
        // los nuevos campos (destinodisplayname/lat/lon) quedan null por defecto
    )
}

private fun buildItineraryFromActivities(
    destino: String,
    diasRecomendados: Int,
    prefs: Set<Preference>,
    actividades: List<SuggestedActivity>,
    fallbackItinerary: List<String>
): List<String> {
    if (diasRecomendados <= 0) return fallbackItinerary
    if (actividades.isEmpty()) return fallbackItinerary

    val grouped = actividades
        .sortedWith(
            compareByDescending<SuggestedActivity> { it.isReal }
                .thenBy { it.isFree }
                .thenBy { it.category }
                .thenBy { it.name }
        )

    val chunks = grouped.chunked(((grouped.size + diasRecomendados - 1) / diasRecomendados).coerceAtLeast(1))

    return (1..diasRecomendados).map { day ->
        val dayActivities = chunks.getOrNull(day - 1).orEmpty()
        val focus = when {
            prefs.contains(Preference.CULTURA) && dayActivities.any { it.category.contains("museum", true) || it.category.contains("culture", true) || it.category.contains("historic", true) } -> "cultura y patrimonio"
            prefs.contains(Preference.NATURALEZA) && dayActivities.any { it.category.contains("park", true) || it.category.contains("nature", true) || it.category.contains("view", true) } -> "naturaleza y miradores"
            prefs.contains(Preference.GASTRONOMIA) && dayActivities.any { it.category.contains("food", true) || it.category.contains("gastr", true) || it.category.contains("market", true) } -> "gastronomía"
            prefs.contains(Preference.OCIO) && dayActivities.any { it.category.contains("night", true) || it.category.contains("entertain", true) || it.category.contains("club", true) } -> "ocio"
            else -> "recorrido libre"
        }

        val activitiesText = if (dayActivities.isNotEmpty()) {
            dayActivities.joinToString(separator = " • ") { it.name }
        } else {
            "Actividad libre sugerida"
        }

        "Día $day: $focus en $destino. Actividades: $activitiesText"
    }
}