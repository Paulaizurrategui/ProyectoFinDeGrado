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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.theme.UrCream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ojo: se llama trippreference para no chocar con preference de plantabscreen
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
    val context = LocalContext.current

    // firestore
    val db = remember { FirebaseFirestore.getInstance() }
    val trips = remember { db.collection("trips") }

    // mismos destinos que planificar
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

    // estado del formulario
    var destino by remember { mutableStateOf(destinos.first()) }
    var presupuestoText by remember { mutableStateOf("") }
    var viajerosText by remember { mutableStateOf("1") }
    var fechaInicioMillis by remember { mutableStateOf<Long?>(null) }
    var fechaFinMillis by remember { mutableStateOf<Long?>(null) }
    var prefs by remember { mutableStateOf(setOf<TripPreference>()) }

    // estado de ui
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }

    // formateador de fechas para mostrar en pantalla
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    fun formatDate(ms: Long?): String = ms?.let { dateFormat.format(Date(it)) } ?: "-"

    // scroll para pantallas pequeñas
    val scrollState = rememberScrollState()

    // 1) cargar viaje al abrir
    LaunchedEffect(tripId) {
        loading = true
        error = null
        localError = null

        trips.document(tripId).get()
            .addOnSuccessListener { doc ->
                // destino guardado
                destino = doc.getString("destino") ?: destino

                // presupuesto (compatibilidad: presupuestoTotal o presupuesto)
                val presupuesto = doc.getDouble("presupuestoTotal")
                    ?: doc.getDouble("presupuesto")
                presupuestoText = presupuesto?.toString() ?: ""

                // viajeros
                val viajeros = doc.getLong("viajeros") ?: 1L
                viajerosText = viajeros.toString()

                // fechas en millis
                fechaInicioMillis = doc.getLong("fechaInicioMillis")
                fechaFinMillis = doc.getLong("fechaFinMillis")

                // preferencias guardadas como list<string> (name del enum)
                val prefStrings =
                    (doc.get("prefs") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                prefs = prefStrings.mapNotNull { key ->
                    runCatching { TripPreference.valueOf(key) }.getOrNull()
                }.toSet()

                loading = false
            }
            .addOnFailureListener { e ->
                error = e.message ?: context.getString(R.string.edit_trip_loading_error)
                loading = false
            }
    }

    // 2) ui (misma estetica que otras pantallas)
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
                    // header: volver
                    TextButton(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) { Text(stringResource(R.string.edit_trip_back)) }

                    // titulo
                    Text(
                        text = stringResource(R.string.edit_trip_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(Modifier.height(10.dp))

                    // card informativa
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = UrCream),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                text = stringResource(R.string.edit_trip_intro_title),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.edit_trip_intro_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // loader de carga
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
                                Text(stringResource(R.string.edit_trip_loading))
                            }
                        }

                        // dejo aire y corto el render del resto
                        Spacer(Modifier.height(90.dp))
                        return@Column
                    }

                    // error al cargar
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

                    // contenedor del formulario
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
                            // destino
                            SectionLabel("Destino")
                            DestinationDropdown(
                                destinos = destinos,
                                selected = destino,
                                onSelected = { destino = it }
                            )

                            // presupuesto
                            SectionLabel("Presupuesto total")
                            OutlinedTextField(
                                value = presupuestoText,
                                onValueChange = { presupuestoText = it.replace(",", ".") }, // permito coma y la convierto
                                label = { Text(stringResource(R.string.edit_trip_currency)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // viajeros
                            SectionLabel("Viajeros")
                            OutlinedTextField(
                                value = viajerosText,
                                onValueChange = { viajerosText = it.filter(Char::isDigit) }, // solo numeros
                                label = { Text(stringResource(R.string.edit_trip_travelers)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // fechas (inicio/fin)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                            ) {
                                Column(
                                    Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
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

                            // preferencias (chips)
                            SectionLabel("Preferencias")
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    TripPreference.entries.take(2).forEach { pref ->
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
                                    TripPreference.entries.drop(2).forEach { pref ->
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

                            // error local de validacion
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

                    // boton guardar cambios
                    Button(
                        onClick = {
                            // limpio errores previos
                            localError = null
                            error = null

                            // parseo datos
                            val presupuesto = presupuestoText.toDoubleOrNull()
                            val viajeros = viajerosText.toIntOrNull()

                            // validaciones
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

                            // mapa de update (solo campos editables)
                            val updateMap = mapOf(
                                "destino" to destino,
                                "presupuestoTotal" to presupuesto,
                                "viajeros" to viajeros,
                                "fechaInicioMillis" to fechaInicioMillis,
                                "fechaFinMillis" to fechaFinMillis,
                                "prefs" to prefs.map { it.name } // list<string>
                            )

                            // update en firestore
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
                        // feedback mientras guardo
                        if (saving) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(12.dp))
                            Text(stringResource(R.string.edit_trip_saving), fontWeight = FontWeight.Bold)
                        } else {
                            Text(stringResource(R.string.edit_trip_save_changes), fontWeight = FontWeight.Bold)
                        }
                    }

                    // aire por si hay bottom bar
                    Spacer(Modifier.height(90.dp))
                }
            }

            // scrollbar visual (opcional)
            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 2.dp),
                scrollState = scrollState
            )
        }

        // datepicker inicio
        if (showStartPicker) {
            val state = rememberDatePickerState(initialSelectedDateMillis = fechaInicioMillis)
            DatePickerDialog(
                onDismissRequest = { showStartPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        fechaInicioMillis = state.selectedDateMillis
                        showStartPicker = false
                    }) { Text(stringResource(R.string.edit_trip_ok)) }
                },
                dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text(stringResource(R.string.edit_trip_cancel)) } }
            ) { DatePicker(state = state) }
        }

        // datepicker fin
        if (showEndPicker) {
            val state = rememberDatePickerState(initialSelectedDateMillis = fechaFinMillis)
            DatePickerDialog(
                onDismissRequest = { showEndPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        fechaFinMillis = state.selectedDateMillis
                        showEndPicker = false
                    }) { Text(stringResource(R.string.edit_trip_ok)) }
                },
                dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text(stringResource(R.string.edit_trip_cancel)) } }
            ) { DatePicker(state = state) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    // label para secciones del formulario
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
    // fila clicable para elegir fecha
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

        // boton para abrir el datepicker
        TextButton(onClick = onClick) { Text(stringResource(R.string.edit_trip_select)) }
    }
}

@Composable
private fun DestinationDropdown(
    destinos: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    // dialog simple para elegir destino
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        // campo readOnly que parece dropdown
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.edit_trip_destination_label)) },
            trailingIcon = { Text(stringResource(R.string.edit_trip_dropdown)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // capa clickable encima para abrir dialog
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }

    // dialog con lista de destinos
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.edit_trip_choose_destination)) },
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
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.edit_trip_close)) } }
        )
    }
}

@Composable
private fun VerticalScrollbar(
    modifier: Modifier,
    scrollState: ScrollState
) {
    // scrollbar basico calculado a mano (track + thumb)
    val max = scrollState.maxValue
    if (max <= 0) return

    // progreso 0..1
    val progress = scrollState.value.toFloat() / max.toFloat()

    // colores suaves
    val trackColor = Color(0x33000000)
    val thumbColor = Color(0x99000000)

    // tamaño del thumb
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