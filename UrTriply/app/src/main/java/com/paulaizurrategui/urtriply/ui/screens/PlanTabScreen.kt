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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.util.Log
import com.paulaizurrategui.urtriply.data.remote.overpass.HotelsRepository
import com.paulaizurrategui.urtriply.data.remote.nominatim.GeocodingRepository
import com.paulaizurrategui.urtriply.domain.model.Hotel
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Preference(val label: String) {
    CULTURA("Cultura"),
    OCIO("Ocio nocturno"),
    NATURALEZA("Naturaleza"),
    GASTRONOMIA("Gastronomía")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTabScreen(
    isGuest: Boolean,
    onNavigateToResult: () -> Unit
) {
    UrTriplyGradientScaffold(title = "Planificar") {
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
                            text = "Planifica tu viaje",
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
                SectionTitle(title = "Destino", subtitle = "Elige la capital europea que quieres visitar")
                Spacer(Modifier.height(8.dp))
                DestinationDropdown(
                    destinos = destinos,
                    selected = destino,
                    onSelected = { destino = it }
                )

                Spacer(Modifier.height(14.dp))

                // presupuesto y viajeros
                SectionTitle(title = "Presupuesto y viajeros", subtitle = "Ajusta la propuesta al tamaño del grupo")
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
                            label = { Text("Presupuesto total") },
                            trailingIcon = { Text("EUR", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text(
                            text = "Presupuesto aproximado para todo el viaje",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = viajerosText,
                            onValueChange = { viajerosText = it.filter(Char::isDigit) },
                            label = { Text("Número de viajeros") },
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
                        Text("Fechas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                        DateRowField(
                            label = "Fecha de inicio",
                            value = formatDate(fechaInicioMillis),
                            onClick = { showStartPicker = true }
                        )
                        DateRowField(
                            label = "Fecha de fin",
                            value = formatDate(fechaFinMillis),
                            onClick = { showEndPicker = true }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // preferencias
                SectionTitle(title = "Preferencias", subtitle = "Selecciona al menos una")
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
                            // delay solo para que se vea el loader
                            delay(900)

                            // 1) genero local (fallback)
                            val base = generateLocalProposal(
                                destino = destino,
                                presupuestoTotal = presupuesto,
                                viajeros = viajeros,
                                fechaInicioMillis = fechaInicioMillis,
                                fechaFinMillis = fechaFinMillis,
                                prefs = prefs
                            )

                            // 2) geocoding real (b: solo parte antes del parentesis)
                            val queryCity = destino.substringBefore("(").trim()
                            val geo = geocodingRepo.geocode(queryCity)

                            val hoteles: List<Hotel> = if (geo != null) {
                                try {
                                    hotelsRepo.searchHotels(
                                        lat = geo.lat,
                                        lon = geo.lon,
                                        checkInDate = fechaInicioMillis,
                                        checkOutDate = fechaFinMillis
                                    )
                                } catch (e: Throwable) {
                                    Log.e("PlanTabScreen", "Error loading hotels for $queryCity", e)
                                    emptyList()
                                }
                            } else {
                                emptyList()
                            }

                            // 3) si sale bien, lo guardo en el resultado
                            val finalPlan = if (geo != null) {
                                base.copy(
                                    destinoDisplayName = geo.displayName,
                                    lat = geo.lat,
                                    lon = geo.lon,
                                    hoteles = hoteles,
                                    hotelMesSeleccionado = hoteles.firstOrNull(),
                                    apiHotelesOk = hoteles.any { it.isReal }
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
                        Text("Generando...")
                    } else {
                        Text("Generar propuesta", fontWeight = FontWeight.Bold)
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
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Cancelar") } }
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
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Cancelar") } }
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
        TextButton(onClick = onClick) { Text("Seleccionar") }
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
            label = { Text("Destino (capital europea)") },
            trailingIcon = { Text("▼") },
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
            title = { Text("Elige destino") },
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
                TextButton(onClick = { showDialog = false }) { Text("Cerrar") }
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

    // estimacion simple de dias recomendados
    val costeDiarioEstimado = (alojamiento + comidas + actividades) / 3.0
    val diasRecomendados = (presupuestoTotal / maxOf(1.0, costeDiarioEstimado))
        .toInt()
        .coerceIn(2, 7)

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