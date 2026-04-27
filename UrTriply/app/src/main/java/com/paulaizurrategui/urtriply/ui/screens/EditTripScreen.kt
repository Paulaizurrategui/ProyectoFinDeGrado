package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.paulaizurrategui.urtriply.ui.theme.UrCream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// OJO: Se llama TripPreference para no chocar con el enum Preference de PlanTabScreen.kt
private enum class TripPreference(val label: String) {
    CULTURA("Cultura"),
    OCIO("Ocio nocturno"),
    NATURALEZA("Naturaleza"),
    GASTRONOMIA("Gastronomía")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTripScreen(
    tripId: String,
    onBack: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val trips = remember { db.collection("trips") }

    // Mismos destinos que PlanTabScreen
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

    // --- Estado formulario ---
    var destino by remember { mutableStateOf(destinos.first()) }
    var presupuestoText by remember { mutableStateOf("") }
    var viajerosText by remember { mutableStateOf("1") }
    var fechaInicioMillis by remember { mutableStateOf<Long?>(null) }
    var fechaFinMillis by remember { mutableStateOf<Long?>(null) }
    var prefs by remember { mutableStateOf(setOf<TripPreference>()) }

    // --- UI state ---
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    fun formatDate(ms: Long?): String = ms?.let { dateFormat.format(Date(it)) } ?: "-"

    val scrollState = rememberScrollState()

    // 1) Cargar viaje
    LaunchedEffect(tripId) {
        loading = true
        error = null
        localError = null

        trips.document(tripId).get()
            .addOnSuccessListener { doc ->
                destino = doc.getString("destino") ?: destino

                // presupuesto: intenta presupuestoTotal y si no existe, presupuesto
                val presupuesto = doc.getDouble("presupuestoTotal")
                    ?: doc.getDouble("presupuesto")
                presupuestoText = presupuesto?.toString() ?: ""

                // viajeros
                val viajeros = doc.getLong("viajeros") ?: 1L
                viajerosText = viajeros.toString()

                // fechas en millis
                fechaInicioMillis = doc.getLong("fechaInicioMillis")
                fechaFinMillis = doc.getLong("fechaFinMillis")

                // preferencias: List<String> con nombres del enum
                val prefStrings = (doc.get("prefs") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                prefs = prefStrings.mapNotNull { key ->
                    runCatching { TripPreference.valueOf(key) }.getOrNull()
                }.toSet()

                loading = false
            }
            .addOnFailureListener { e ->
                error = e.message ?: "No se pudo cargar el viaje."
                loading = false
            }
    }

    // 2) UI (misma estética que PlanResultScreen)
    Scaffold { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                ) {
                    // Header
                    TextButton(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) { Text("← Volver") }

                    Text(
                        text = "Editar viaje",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(10.dp))

                    // Card info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = UrCream),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                text = "Actualiza los datos del viaje",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Puedes editar destino, presupuesto, fechas y preferencias.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Loader
                    if (loading) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(strokeWidth = 2.dp)
                                Spacer(Modifier.size(12.dp))
                                Text("Cargando viaje...")
                            }
                        }
                        Spacer(Modifier.height(90.dp))
                        return@Column
                    }

                    // Error de carga
                    error?.let {
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

                    // Form container
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SectionLabel("Destino")
                            DestinationDropdown(
                                destinos = destinos,
                                selected = destino,
                                onSelected = { destino = it }
                            )

                            SectionLabel("Presupuesto total")
                            OutlinedTextField(
                                value = presupuestoText,
                                onValueChange = { presupuestoText = it.replace(",", ".") },
                                label = { Text("EUR") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            SectionLabel("Viajeros")
                            OutlinedTextField(
                                value = viajerosText,
                                onValueChange = { viajerosText = it.filter(Char::isDigit) },
                                label = { Text("Número de viajeros") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Fechas
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Fechas",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )

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

                            // Preferencias
                            SectionLabel("Preferencias")
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    TripPreference.entries.take(2).forEach { pref ->
                                        FilterChip(
                                            selected = prefs.contains(pref),
                                            onClick = { prefs = if (prefs.contains(pref)) prefs - pref else prefs + pref },
                                            label = { Text(pref.label) }
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    TripPreference.entries.drop(2).forEach { pref ->
                                        FilterChip(
                                            selected = prefs.contains(pref),
                                            onClick = { prefs = if (prefs.contains(pref)) prefs - pref else prefs + pref },
                                            label = { Text(pref.label) }
                                        )
                                    }
                                }
                            }

                            // Error local validación
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
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Botón guardar cambios (estilo pro)
                    Button(
                        onClick = {
                            localError = null
                            error = null

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

                            saving = true

                            val updateMap = mapOf(
                                "destino" to destino,
                                "presupuestoTotal" to presupuesto,
                                "viajeros" to viajeros,
                                "fechaInicioMillis" to fechaInicioMillis,
                                "fechaFinMillis" to fechaFinMillis,
                                "prefs" to prefs.map { it.name } // List<String>
                            )

                            trips.document(tripId)
                                .update(updateMap)
                                .addOnSuccessListener {
                                    saving = false
                                    onBack()
                                }
                                .addOnFailureListener { e ->
                                    saving = false
                                    error = e.message ?: "No se pudieron guardar los cambios."
                                }
                        },
                        enabled = !saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) {
                        if (saving) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(12.dp))
                            Text("Guardando…", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Guardar cambios", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(90.dp)) // aire + por si hay bottom bar en el futuro
                }
            }

            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 2.dp),
                scrollState = scrollState
            )
        }

        // Date pickers
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
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
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun VerticalScrollbar(
    modifier: Modifier,
    scrollState: ScrollState
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