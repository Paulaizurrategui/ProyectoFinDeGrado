package com.paulaizurrategui.urtriply.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.util.Log

import com.paulaizurrategui.urtriply.data.remote.overpass.ActivitiesRepository
import com.paulaizurrategui.urtriply.data.remote.overpass.HotelsRepository
import com.paulaizurrategui.urtriply.data.remote.nominatim.GeocodingRepository
import com.paulaizurrategui.urtriply.domain.model.FlightOffer
import com.paulaizurrategui.urtriply.domain.model.Hotel
import com.paulaizurrategui.urtriply.domain.model.countTripNights
import com.paulaizurrategui.urtriply.domain.model.SuggestedActivity
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.supervisorScope
import com.paulaizurrategui.urtriply.R

// Preferencias que el usuario puede seleccionar en el formulario.
// Cada entrada contiene la clave usada internamente y el recurso de texto para la UI.
private enum class Preference(val labelRes: Int, val key: String) {
    CULTURA(R.string.plan_pref_culture, "cultura"),
    OCIO(R.string.plan_pref_night, "ocio nocturno"),
    NATURALEZA(R.string.plan_pref_nature, "naturaleza"),
    GASTRONOMIA(R.string.plan_pref_food, "gastronomia")
}

private fun destinationToIata(destination: String): String {
    // Normalizo el nombre de la ciudad y devuelvo un código IATA simple usado
    // como origen/destino para búsquedas de vuelos. Es un mapeo manual limitado
    // (solo capitales incluidas en la app); si no coincide, se usa MAD por defecto.
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
    // hide scaffold header and render a compact custom header to match design polish
    UrTriplyGradientScaffold(title = stringResource(R.string.plan_title), showHeader = false) {
        val context = LocalContext.current
        val destinos = listOf(
            stringResource(R.string.plan_dest_paris),
            stringResource(R.string.plan_dest_london),
            stringResource(R.string.plan_dest_rome),
            stringResource(R.string.plan_dest_amsterdam),
            stringResource(R.string.plan_dest_athens),
            stringResource(R.string.plan_dest_lisbon),
            stringResource(R.string.plan_dest_berlin),
            stringResource(R.string.plan_dest_prague),
            stringResource(R.string.plan_dest_vienna),
            stringResource(R.string.plan_dest_dublin)
        )

        // --- estado formulario ---
        var destino by remember { mutableStateOf(destinos.first()) }
        var presupuestoText by remember { mutableStateOf("") }
        var viajerosText by remember { mutableStateOf("1") }
        var fechaInicioMillis by remember { mutableStateOf<Long?>(null) }
        var fechaFinMillis by remember { mutableStateOf<Long?>(null) }
        var prefs by remember { mutableStateOf(setOf<Preference>()) }

        // Notas de diseño: usamos Strings para inputs y luego parseamos (Double/Int)
        // para validar; las fechas se guardan como millis para compatibilidad con
        // DatePicker y para enviarlas a los repositorios que requieren formatos.

        // --- ui state ---
        var showStartPicker by remember { mutableStateOf(false) }
        var showEndPicker by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var localError by remember { mutableStateOf<String?>(null) }

        // `localError` se usa para mostrar errores de validación antes de disparar
        // las búsquedas/llamadas a APIs externas.

        val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
        fun formatDate(ms: Long?): String = ms?.let { dateFormat.format(Date(it)) } ?: "-"

        val scrollState = rememberScrollState()

        // Coroutines: usamos `rememberCoroutineScope()` para lanzar la generación
        // de la propuesta sin bloquear la UI. Las llamadas de red se realizan en
        // Dispatchers.IO y se usan timeouts y reintentos.
        val scope = rememberCoroutineScope()

        // repo geocoding (nominatim)
        // Repositorios que consultan servicios externos (Nominatim, Overpass, Flights)
        // Se inicializan dentro de la composición con `remember` para mantener
        // la misma instancia mientras el composable esté vivo.
        val geocodingRepo = remember { GeocodingRepository() }
        val hotelsRepo = remember { HotelsRepository() }
        val activitiesRepo = remember { ActivitiesRepository() }
        val flightsRepo = remember { com.paulaizurrategui.urtriply.data.remote.flights.FlightsRepository() }
        val queryCity = destino.substringBefore("(").trim()
        val destinationIata = destinationToIata(destino)
        val effectiveOriginIata = "MAD"
        val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val mainTextColor = if (isDarkTheme) Color(0xFFD7DFEB) else MaterialTheme.colorScheme.onSurface
        val secondaryTextColor = if (isDarkTheme) Color(0xFF9AA7B8) else MaterialTheme.colorScheme.onSurfaceVariant
        val panelColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f) else MaterialTheme.colorScheme.surface
        val fieldColor = if (isDarkTheme) MaterialTheme.colorScheme.surface.copy(alpha = 0.9f) else Color.White

        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 18.dp)
            ) {
                // compact header (60-70dp) with subtle background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(panelColor)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.home_emoji_plane), color = mainTextColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold, color = mainTextColor)
                            Text(text = stringResource(R.string.plan_header_subtitle), style = MaterialTheme.typography.bodySmall, color = secondaryTextColor)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // intro (lighter)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = panelColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.plan_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = mainTextColor
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (isGuest) stringResource(R.string.plan_guest_notice)
                            else stringResource(R.string.plan_form_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // destino
                    SectionTitle(
                        title = stringResource(R.string.plan_destination_label),
                        subtitle = stringResource(R.string.plan_choose_destination)
                    )
                Spacer(Modifier.height(8.dp))
                DestinationDropdown(
                    destinos = destinos,
                    selected = destino,
                    onSelected = { destino = it }
                )

                Spacer(Modifier.height(8.dp))
                // origen fijo: mostrar como info box más suave
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = panelColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📍", modifier = Modifier.size(28.dp), color = mainTextColor)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(text = stringResource(R.string.plan_origin_fixed), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = mainTextColor)
                            Text(text = stringResource(R.string.plan_origin_madrid), style = MaterialTheme.typography.bodySmall, color = secondaryTextColor)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // presupuesto y viajeros
                SectionTitle(title = stringResource(R.string.plan_budget_travelers_title), subtitle = stringResource(R.string.plan_budget_travelers_subtitle))
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = panelColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // presupuesto: prefijo € y borde más fino
                        OutlinedTextField(
                            value = presupuestoText,
                            onValueChange = { presupuestoText = it.replace(",", ".") },
                            label = { Text(stringResource(R.string.plan_budget_total)) },
                            leadingIcon = { Text("€", style = MaterialTheme.typography.bodyLarge, color = mainTextColor) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = mainTextColor),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = secondaryTextColor.copy(alpha = 0.7f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = secondaryTextColor,
                                focusedTextColor = mainTextColor,
                                unfocusedTextColor = mainTextColor,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedLeadingIconColor = mainTextColor,
                                unfocusedLeadingIconColor = mainTextColor
                            )
                        )

                        Text(
                            text = stringResource(R.string.plan_budget_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor
                        )

                        // viajeros: compact stepper
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = stringResource(R.string.plan_travelers), modifier = Modifier.weight(1f), color = mainTextColor)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { val n = viajerosText.toIntOrNull() ?: 1; if (n>1) viajerosText = (n-1).toString() }, modifier = Modifier.size(36.dp), shape = RoundedCornerShape(8.dp)) { Text("-") }
                                Spacer(Modifier.width(8.dp))
                                Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = fieldColor)) { Text(viajerosText, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = mainTextColor) }
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = { val n = viajerosText.toIntOrNull() ?: 1; viajerosText = (n+1).toString() }, modifier = Modifier.size(36.dp), shape = RoundedCornerShape(8.dp)) { Text("+") }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // fechas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = panelColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.plan_dates), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = mainTextColor)

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
                    colors = CardDefaults.cardColors(containerColor = panelColor),
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
                                        label = { Text(stringResource(pref.labelRes)) },
                                        shape = RoundedCornerShape(24.dp),
                                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFFFF3E0),
                                            selectedLabelColor = Color(0xFF1F2937)
                                        )
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
                                        label = { Text(stringResource(pref.labelRes)) },
                                        shape = RoundedCornerShape(24.dp),
                                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFFFF3E0),
                                            selectedLabelColor = Color(0xFF1F2937)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.04f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "✈️", modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.plan_route_outbound_short, "MAD", destinationIata),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
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

                val errorBudgetText = stringResource(R.string.plan_error_budget)
                val errorTravelersText = stringResource(R.string.plan_error_travelers)
                val errorDatesText = stringResource(R.string.plan_error_dates)
                val errorDateOrderText = stringResource(R.string.plan_error_date_order)
                val errorPreferencesText = stringResource(R.string.plan_error_preferences)
                val preferenceNames = prefs.map { it.key }.toSet()

                // boton generar (más delgado, sombra suave, feedback)
                Button(
                    onClick = {
                        localError = null

                        val presupuesto = presupuestoText.toDoubleOrNull()
                        val viajeros = viajerosText.toIntOrNull()

                        if (presupuesto == null || presupuesto <= 0) {
                            localError = errorBudgetText
                            return@Button
                        }
                        if (viajeros == null || viajeros <= 0) {
                            localError = errorTravelersText
                            return@Button
                        }
                        if (fechaInicioMillis == null || fechaFinMillis == null) {
                            localError = errorDatesText
                            return@Button
                        }
                        if (fechaFinMillis!! < fechaInicioMillis!!) {
                            localError = errorDateOrderText
                            return@Button
                        }
                        if (prefs.isEmpty()) {
                            localError = errorPreferencesText
                            return@Button
                        }

                        isLoading = true

                            scope.launch {
                                val fallback = generateLocalProposal(
                                    context = context,
                                    destino = destino,
                                    presupuestoTotal = presupuesto,
                                    viajeros = viajeros,
                                    fechaInicioMillis = fechaInicioMillis,
                                    fechaFinMillis = fechaFinMillis,
                                    prefs = prefs
                                )

                                try {

                                    // Si no hay conexión usamos directamente el fallback
                                    if (!hasInternetConnection(context)) {
                                        Log.i(
                                            "PlanTabScreen",
                                            "Sin conexión a Internet. Usando fallback local."
                                        )

                                        PlanResultStore.lastResult = fallback
                                        onNavigateToResult()
                                        return@launch
                                    }

                                    // 2) Geocodificación
                                    val geo = withContext(Dispatchers.IO) {
                                        geocodingRepo.geocode(queryCity)
                                    }

                                // 3) Búsquedas paralelas (hoteles, actividades, vuelos).
                                // Cada llamada se ejecuta con `retryWithBackoff` y `withTimeoutOrNull`
                                // para controlar latencias y realizar reintentos automáticos.
                                val (hoteles, actividadesReales, vuelosOfertas) = supervisorScope {
                                    val hotelesDeferred = async(Dispatchers.IO) {
                                        if (geo == null) return@async emptyList<Hotel>()
                                        val result = retryWithBackoff(times = 3, initialDelay = 500) {
                                            // treat timeout as failure so retryWithBackoff can retry
                                            withTimeoutOrNull(10000L) {
                                                hotelsRepo.searchHotels(
                                                    lat = geo.lat,
                                                    lon = geo.lon,
                                                    checkInDate = fechaInicioMillis,
                                                    checkOutDate = fechaFinMillis
                                                )
                                            } ?: throw RuntimeException("timeout")
                                        }
                                        result ?: emptyList()
                                    }

                                    val actividadesDeferred = async(Dispatchers.IO) {
                                        if (geo == null) return@async emptyList<SuggestedActivity>()
                                        val result = retryWithBackoff(times = 3, initialDelay = 600) {
                                            withTimeoutOrNull(15000L) {
                                                activitiesRepo.searchActivities(
                                                    lat = geo.lat,
                                                    lon = geo.lon,
                                                    prefs = preferenceNames
                                                )
                                            } ?: throw RuntimeException("timeout")
                                        }
                                        result ?: emptyList()
                                    }

                                    val vuelosDeferred = async(Dispatchers.IO) {
                                        val result = retryWithBackoff(times = 2, initialDelay = 400) {
                                            withTimeoutOrNull(8000L) {
                                                flightsRepo.searchFlights(
                                                    origin = effectiveOriginIata,
                                                    destination = destinationIata,
                                                    dateFrom = formatDate(fechaInicioMillis),
                                                    dateTo = formatDate(fechaFinMillis)
                                                )
                                            } ?: throw RuntimeException("timeout")
                                        }
                                        result ?: emptyList()
                                    }

                                    Triple(hotelesDeferred.await(), actividadesDeferred.await(), vuelosDeferred.await())
                                }

                                // 4) Reconstruyo la propuesta final mezclando datos reales con
                                // el fallback. Si `geo` es nulo usamos el `fallback` directo.
                                val finalPlan = if (geo != null) {
                                    val itineraryByDay = withContext(Dispatchers.Default) {
                                        buildItineraryFromActivities(
                                            context = context,
                                            destino = destino,
                                            diasRecomendados = fallback.diasRecomendados,
                                            prefs = prefs,
                                            actividades = actividadesReales.filter { it.isReal },
                                            fallbackItinerary = fallback.itinerario
                                        )
                                    }
                                    buildRealProposal(
                                        context = context,
                                        fallback = fallback,
                                        presupuestoTotal = presupuesto,
                                        destinoDisplayName = geo.displayName,
                                        lat = geo.lat,
                                        lon = geo.lon,
                                        hoteles = hoteles,
                                        actividades = actividadesReales,
                                        vuelos = vuelosOfertas,
                                        itineraryByDay = itineraryByDay
                                    )
                                } else {
                                    fallback
                                }

                                // Guardamos el resultado en el store temporal para la pantalla
                                // de resultados y navegamos a ella.
                                PlanResultStore.lastResult = finalPlan
                                onNavigateToResult()
                                } catch (e: Throwable) {
                                    Log.e("PlanTabScreen", "Error generating proposal", e)

                                    // Si cualquier API falla de forma inesperada,
                                    // mostramos igualmente la propuesta local.
                                    PlanResultStore.lastResult = fallback
                                    onNavigateToResult()

                                } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    if (isLoading) {
                        // simple contextual loading
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp).padding(end = 8.dp))
                        Text(stringResource(R.string.plan_generating))
                    } else {
                        Text(stringResource(R.string.plan_generate), fontWeight = FontWeight.SemiBold)
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
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mainTextColor = if (isDarkTheme) Color(0xFFD7DFEB) else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isDarkTheme) Color(0xFF9AA7B8) else MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = mainTextColor
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = secondaryTextColor
        )
    }
}

@Composable
private fun DateRowField(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mainTextColor = if (isDarkTheme) Color(0xFFD7DFEB) else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isDarkTheme) Color(0xFF9AA7B8) else MaterialTheme.colorScheme.onSurfaceVariant
    val fieldColor = if (isDarkTheme) MaterialTheme.colorScheme.surface.copy(alpha = 0.9f) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(fieldColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = secondaryTextColor
            )
            Spacer(Modifier.height(2.dp))
            Text(text = value, fontWeight = FontWeight.SemiBold, color = mainTextColor)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClick) { Text(stringResource(R.string.plan_select), color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun DestinationDropdown(
    destinos: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mainTextColor = if (isDarkTheme) Color(0xFFD7DFEB) else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isDarkTheme) Color(0xFF9AA7B8) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.plan_destination_input_label), color = secondaryTextColor) },
            trailingIcon = { Text("▾", color = secondaryTextColor) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = mainTextColor),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = secondaryTextColor.copy(alpha = 0.7f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = secondaryTextColor,
                focusedTextColor = mainTextColor,
                unfocusedTextColor = mainTextColor,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTrailingIconColor = secondaryTextColor,
                unfocusedTrailingIconColor = secondaryTextColor
            )
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
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
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
 * Generación local de respaldo.
 *
 * Nota: esta función crea una propuesta estimada cuando las APIs externas
 * no devuelven suficientes datos. Lo dejo como fallback:
 * - Calcula reparto de presupuesto (transporte, alojamiento, comidas, actividades)
 * - Estima días recomendados según presupuesto por persona y preferencias
 * - Construye un `itineraryByDay` simple con resúmenes y actividades sugeridas
 *
 * Uso: llamada desde la UI cuando `generate` no obtuvo resultados reales.
 */
private fun generateLocalProposal(
    context: android.content.Context,
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

    val itineraryByDay = (1..diasRecomendados).map { day ->
        val focus = fallbackFocusForDay(context, prefs, day)
        val fallbackActivities = buildFallbackItineraryActivities(context, prefs, day)
        val summary = appendFallbackActivitiesToSummary(
            summary = context.getString(R.string.plan_local_itinerary_day, day, focus, destino),
            activities = fallbackActivities
        )
        ItineraryDay(
            dayLabel = context.getString(R.string.plan_result_day_label, day),
            summary = summary,
            activities = fallbackActivities
        )
    }

    val actividadesGratis = buildList {
        if (prefs.contains(Preference.CULTURA)) add(context.getString(R.string.plan_local_free_walking_tour))
        if (prefs.contains(Preference.NATURALEZA)) add(context.getString(R.string.plan_local_urban_park))
        if (prefs.contains(Preference.GASTRONOMIA)) add(context.getString(R.string.plan_local_market))
        if (prefs.contains(Preference.OCIO)) add(context.getString(R.string.plan_local_nightlife_neighborhood))
    }.distinct()

    val actividadesPago = buildList {
        if (prefs.contains(Preference.CULTURA)) add(context.getString(R.string.plan_local_museum_entry))
        if (prefs.contains(Preference.NATURALEZA)) add(context.getString(R.string.plan_local_day_trip))
        if (prefs.contains(Preference.GASTRONOMIA)) add(context.getString(R.string.plan_local_food_tour))
        if (prefs.contains(Preference.OCIO)) add(context.getString(R.string.plan_local_club))
    }.distinct()

    return PlanResult(
        destino = destino,
        presupuestoTotal = presupuestoTotal,
        viajeros = viajeros,
        fechaInicioMillis = fechaInicioMillis,
        fechaFinMillis = fechaFinMillis,
        diasRecomendados = diasRecomendados,
        presupuestoCategorias = linkedMapOf(
            context.getString(R.string.plan_local_budget_transport) to transporte,
            context.getString(R.string.plan_local_budget_lodging) to alojamiento,
            context.getString(R.string.plan_local_budget_meals) to comidas,
            context.getString(R.string.plan_local_budget_activities) to actividades
        ),
        itinerario = itineraryByDay.map { it.summary },
        itineraryByDay = itineraryByDay,
        actividadesGratis = actividadesGratis,
        actividadesPago = actividadesPago,
        hoteles = buildFallbackHotels(countTripNights(fechaInicioMillis, fechaFinMillis)),
        vuelos = buildFallbackFlights(
            origin = "MAD",
            destination = destinationToIata(destino),
            departureDate = formatIsoDate(fechaInicioMillis),
            returnDate = formatIsoDate(fechaFinMillis)
        ),
        usedFallback = true
    )
}

private fun fallbackFocusForDay(
    context: Context,
    prefs: Set<Preference>,
    day: Int
): String {
    val rotation = preferredFallbackRotation(prefs)
    val primary = rotation[(day - 1) % rotation.size]
    return when (primary) {
        Preference.CULTURA -> context.getString(R.string.plan_local_focus_culture)
        Preference.NATURALEZA -> context.getString(R.string.plan_local_focus_nature)
        Preference.GASTRONOMIA -> context.getString(R.string.plan_local_focus_food)
        Preference.OCIO -> context.getString(R.string.plan_local_focus_night)
    }
}

private fun buildFallbackItineraryActivities(
    context: Context,
    prefs: Set<Preference>,
    day: Int
): List<ItineraryActivityLink> {
    val rotation = preferredFallbackRotation(prefs)
    val primary = rotation[(day - 1) % rotation.size]
    val secondary = rotation[day % rotation.size]

    fun namesFor(pref: Preference): List<String> = when (pref) {
        Preference.CULTURA -> listOf(
            context.getString(R.string.plan_local_free_walking_tour),
            context.getString(R.string.plan_local_museum_entry)
        )
        Preference.NATURALEZA -> listOf(
            context.getString(R.string.plan_local_urban_park),
            context.getString(R.string.plan_local_day_trip)
        )
        Preference.GASTRONOMIA -> listOf(
            context.getString(R.string.plan_local_market),
            context.getString(R.string.plan_local_food_tour)
        )
        Preference.OCIO -> listOf(
            context.getString(R.string.plan_local_nightlife_neighborhood),
            context.getString(R.string.plan_local_club)
        )
    }

    val activityNames = (namesFor(primary) + namesFor(secondary)).distinct().take(3)

    return activityNames.map { name ->
        ItineraryActivityLink(
            name = name,
            bookingUrl = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(name, Charsets.UTF_8.name())
        )
    }
}

private fun preferredFallbackRotation(prefs: Set<Preference>): List<Preference> {
    // Prioridad visual: cultura > gastronomia > naturaleza > ocio.
    // Si no hay preferencias marcadas, rotamos todas para mantener variedad.
    val ordered = listOf(
        Preference.CULTURA,
        Preference.GASTRONOMIA,
        Preference.NATURALEZA,
        Preference.OCIO
    )
    val selected = ordered.filter { prefs.contains(it) }
    return if (selected.isNotEmpty()) selected else ordered
}

private fun appendFallbackActivitiesToSummary(
    summary: String,
    activities: List<ItineraryActivityLink>
): String {
    val names = activities.map { it.name }.distinct().take(2)
    if (names.isEmpty()) return summary
    return "$summary · ${names.joinToString(separator = " • ")}"
}

private fun formatIsoDate(dateMillis: Long?): String {
    return dateMillis?.let {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
    } ?: ""
}

private fun buildFallbackHotels(numNights: Int): List<Hotel> {
    return listOf(
        Hotel(
            id = "fallback_hotel_1",
            name = "Hotel céntrico estimado",
            lat = 0.0,
            lon = 0.0,
            stars = 3,
            rating = 3.8,
            pricePerNight = 95.0,
            totalPrice = 95.0 * numNights,
            bookingUrl = null,
            isReal = false
        ),
        Hotel(
            id = "fallback_hotel_2",
            name = "Alojamiento recomendado",
            lat = 0.0,
            lon = 0.0,
            stars = 4,
            rating = 4.2,
            pricePerNight = 125.0,
            totalPrice = 125.0 * numNights,
            bookingUrl = null,
            isReal = false
        )
    )
}

private fun buildFallbackFlights(
    origin: String,
    destination: String,
    departureDate: String,
    returnDate: String?
): List<FlightOffer> {
    return listOf(
        FlightOffer(
            id = "fallback_flight_1",
            origin = origin,
            destination = destination,
            departureDate = departureDate,
            returnDate = returnDate,
            price = 120.0,
            currency = "EUR",
            durationMinutes = 120,
            carrier = "Fallback Air",
            bookingUrl = null,
            isReal = false
        ),
        FlightOffer(
            id = "fallback_flight_2",
            origin = origin,
            destination = destination,
            departureDate = departureDate,
            returnDate = returnDate,
            price = 150.0,
            currency = "EUR",
            durationMinutes = 140,
            carrier = "Fallback Connect",
            bookingUrl = null,
            isReal = false
        )
    )
}
private fun buildRealProposal(
    context: android.content.Context,
    fallback: PlanResult,
    presupuestoTotal: Double,
    destinoDisplayName: String,
    lat: Double,
    lon: Double,
    hoteles: List<Hotel>,
    actividades: List<SuggestedActivity>,
    vuelos: List<FlightOffer>,
    itineraryByDay: List<ItineraryDay>
): PlanResult {
    // Ensamblo la propuesta final a partir del fallback y los datos reales
    // - prefiero elementos "reales" cuando existen, si no, mantengo lo recibido
    // - calculo presupuestos finales basados en selección óptima (hotel/flight)
    val realHotels = hoteles.filter { it.isReal }
    val realActivities = actividades.filter { it.isReal }
    val realFlights = vuelos.filter { it.isReal }

    val hotelSource = if (realHotels.isNotEmpty()) realHotels else hoteles
    val activitySource = if (realActivities.isNotEmpty()) realActivities else actividades
    val flightSource = if (realFlights.isNotEmpty()) realFlights else vuelos

    val tripNights = countTripNights(fallback.fechaInicioMillis, fallback.fechaFinMillis)

    val selectedHotel = hotelSource.minByOrNull {
        it.totalPrice ?: (it.pricePerNight * tripNights.toDouble())
    }
    val selectedFlight = flightSource.minByOrNull { it.price }
    val paidActivities = activitySource
        .filterNot { it.isFree }
        .sortedWith(compareBy<SuggestedActivity> { it.price }.thenBy { it.name })
        .take(maxOf(1, itineraryByDay.size))

    // Garantiza contenido en UI incluso si alguna fuente llega vacía.
    val safeHotels = hotelSource.ifEmpty { fallback.hoteles }
    val safeFlights = flightSource.ifEmpty { fallback.vuelos }
    val safeItineraryByDay = itineraryByDay.ifEmpty { fallback.itineraryByDay }
    val safeItinerario = if (safeItineraryByDay.isNotEmpty()) {
        safeItineraryByDay.map { it.summary }
    } else {
        fallback.itinerario
    }
    val safeFreeActivities = activitySource
        .filter { it.isFree }
        .map { it.name }
        .distinct()
        .ifEmpty { fallback.actividadesGratis }
    val safePaidActivities = paidActivities
        .map { it.name }
        .distinct()
        .ifEmpty { fallback.actividadesPago }

    val transportBudget = selectedFlight?.price ?: fallback.presupuestoCategorias[
        context.getString(R.string.plan_local_budget_transport)
    ] ?: 0.0
    val lodgingBudget = selectedHotel?.totalPrice
        ?: selectedHotel?.pricePerNight?.times(tripNights.toDouble())
        ?: fallback.presupuestoCategorias[context.getString(R.string.plan_local_budget_lodging)]
        ?: 0.0
    val activitiesBudget = if (paidActivities.isNotEmpty()) {
        paidActivities.sumOf { it.price }
    } else {
        fallback.presupuestoCategorias[context.getString(R.string.plan_local_budget_activities)] ?: 0.0
    }
    val mealsBudget = (presupuestoTotal - transportBudget - lodgingBudget - activitiesBudget)
        .coerceAtLeast(0.0)

    return fallback.copy(
        destinoDisplayName = destinoDisplayName,
        lat = lat,
        lon = lon,
        hoteles = safeHotels,
        hotelMesSeleccionado = selectedHotel ?: safeHotels.minByOrNull {
            it.totalPrice ?: (it.pricePerNight * tripNights.toDouble())
        },
        apiHotelesOk = realHotels.isNotEmpty(),
        actividadesReales = activitySource,
        apiActividadesOk = realActivities.isNotEmpty(),
        itinerario = safeItinerario,
        itineraryByDay = safeItineraryByDay,
        actividadesGratis = safeFreeActivities,
        actividadesPago = safePaidActivities,
        presupuestoCategorias = linkedMapOf(
            context.getString(R.string.plan_local_budget_transport) to transportBudget,
            context.getString(R.string.plan_local_budget_lodging) to lodgingBudget,
            context.getString(R.string.plan_local_budget_meals) to mealsBudget,
            context.getString(R.string.plan_local_budget_activities) to activitiesBudget
        ),
        vuelos = safeFlights,
        apiVuelosOk = realFlights.isNotEmpty(),
        usedFallback = !(realHotels.isNotEmpty() && realActivities.isNotEmpty() && realFlights.isNotEmpty())
    )
}

private fun buildItineraryFromActivities(
    context: android.content.Context,
    destino: String,
    diasRecomendados: Int,
    prefs: Set<Preference>,
    actividades: List<SuggestedActivity>,
    fallbackItinerary: List<String>
): List<ItineraryDay> {
    // Construye `ItineraryDay` a partir de actividades reales.
    // - Agrupa y distribuye actividades por días
    // - Intenta respetar las preferencias para asignar un "focus" por día
    fun fallbackDays(): List<ItineraryDay> {
        val baseSummaries = if (fallbackItinerary.isNotEmpty()) {
            fallbackItinerary
        } else {
            (1..maxOf(1, diasRecomendados)).map { day ->
                val focus = fallbackFocusForDay(context, prefs, day)
                context.getString(R.string.plan_local_itinerary_day, day, focus, destino)
            }
        }

        return baseSummaries.mapIndexed { index, summary ->
            val day = index + 1
            val fallbackActivities = buildFallbackItineraryActivities(context, prefs, day)
            ItineraryDay(
                dayLabel = context.getString(R.string.plan_result_day_label, day),
                summary = appendFallbackActivitiesToSummary(summary, fallbackActivities),
                activities = fallbackActivities
            )
        }
    }

    if (diasRecomendados <= 0) return fallbackDays()
    if (actividades.isEmpty()) return fallbackDays()

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
        val fallbackActivities = buildFallbackItineraryActivities(context, prefs, day)
        val realActivities = dayActivities.map {
            ItineraryActivityLink(
                name = it.name,
                bookingUrl = it.bookingUrl
            )
        }
        val resolvedActivities = (realActivities + fallbackActivities)
            .distinctBy { it.name }
            .take(3)
            .ifEmpty { fallbackActivities }

        val focus = when {
            dayActivities.isNotEmpty() && prefs.contains(Preference.CULTURA) && dayActivities.any { it.category.contains("museum", true) || it.category.contains("culture", true) || it.category.contains("historic", true) } -> context.getString(R.string.plan_local_focus_culture)
            dayActivities.isNotEmpty() && prefs.contains(Preference.NATURALEZA) && dayActivities.any { it.category.contains("park", true) || it.category.contains("nature", true) || it.category.contains("view", true) } -> context.getString(R.string.plan_local_focus_nature)
            dayActivities.isNotEmpty() && prefs.contains(Preference.GASTRONOMIA) && dayActivities.any { it.category.contains("food", true) || it.category.contains("gastr", true) || it.category.contains("market", true) } -> context.getString(R.string.plan_local_focus_food)
            dayActivities.isNotEmpty() && prefs.contains(Preference.OCIO) && dayActivities.any { it.category.contains("night", true) || it.category.contains("entertain", true) || it.category.contains("club", true) } -> context.getString(R.string.plan_local_focus_night)
            else -> fallbackFocusForDay(context, prefs, day)
        }

        val activitiesText = resolvedActivities
            .map { it.name }
            .ifEmpty { listOf(context.getString(R.string.plan_local_activity_free_suggestion)) }
            .joinToString(separator = " • ")

        ItineraryDay(
            dayLabel = context.getString(R.string.plan_result_day_label, day),
            summary = context.getString(R.string.plan_local_itinerary_day_activities, day, focus, destino, activitiesText),
            activities = resolvedActivities
        )
    }
}
private fun hasInternetConnection(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val network = connectivityManager.activeNetwork ?: return false

    val capabilities =
        connectivityManager.getNetworkCapabilities(network) ?: return false

    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
/**
 * Retry with exponential backoff. Returns null if all attempts fail.
 */
private suspend fun <T> retryWithBackoff(
    times: Int = 3,
    initialDelay: Long = 400,
    factor: Double = 2.0,
    maxDelay: Long = 5000,
    block: suspend () -> T?
): T? {
    // Retry helper: intenta ejecutar `block` varias veces con backoff exponencial
    // - `times`: número de intentos
    // - `initialDelay` y `factor` controlan la espera entre intentos
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            Log.w("PlanTabScreen", "Call failed, retrying in ${currentDelay}ms", e)
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    return try {
        block()
    } catch (e: Exception) {
        Log.e("PlanTabScreen", "Final attempt failed", e)
        null
    }
}