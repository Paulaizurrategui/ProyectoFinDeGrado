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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Preference(val label: String) {
    CULTURA("Cultura"),
    OCIO("Ocio nocturno"),
    NATURALEZA("Naturaleza"),
    GASTRONOMIA("Gastronomía")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTabScreen(isGuest: Boolean) {
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

        // --- Estados del formulario ---
        var destino by remember { mutableStateOf(destinos.first()) }
        var presupuestoText by remember { mutableStateOf("") }
        var viajerosText by remember { mutableStateOf("1") }
        var fechaInicioMillis by remember { mutableStateOf<Long?>(null) }
        var fechaFinMillis by remember { mutableStateOf<Long?>(null) }
        var prefs by remember { mutableStateOf(setOf<Preference>()) }

        // --- Date pickers (inicio/fin separados por ahora) ---
        var showStartPicker by remember { mutableStateOf(false) }
        var showEndPicker by remember { mutableStateOf(false) }

        val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
        fun formatDate(ms: Long?): String = ms?.let { dateFormat.format(Date(it)) } ?: "-"

        // --- Scroll ---
        val scrollState = rememberScrollState()

        Box(modifier = Modifier.fillMaxWidth()) {

            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
                    .padding(end = 14.dp) // deja sitio para el scrollbar visual
            ) {
                Text(
                    text = if (isGuest)
                        "Modo invitado: puedes generar propuestas, pero para guardar/publicar necesitarás iniciar sesión."
                    else
                        "Completa el formulario para generar una propuesta ajustada a tu presupuesto.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(14.dp))

                DestinationDropdown(
                    destinos = destinos,
                    selected = destino,
                    onSelected = { destino = it }
                )

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = presupuestoText,
                    onValueChange = { presupuestoText = it.replace(",", ".") },
                    label = { Text("Presupuesto total (EUR)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = viajerosText,
                    onValueChange = { viajerosText = it.filter(Char::isDigit) },
                    label = { Text("Número de viajeros") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Rango de fechas", style = MaterialTheme.typography.titleSmall)

                        Spacer(Modifier.height(8.dp))

                        RowLine(
                            left = "Inicio: ${formatDate(fechaInicioMillis)}",
                            actionText = "Elegir",
                            onAction = { showStartPicker = true }
                        )

                        RowLine(
                            left = "Fin: ${formatDate(fechaFinMillis)}",
                            actionText = "Elegir",
                            onAction = { showEndPicker = true }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Preferencias", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                Spacer(Modifier.height(26.dp))

                Text(
                    text = "Siguiente: botón “Generar propuesta” + resultado (Pantalla 5).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )

                Spacer(Modifier.height(10.dp))
            }

            // Scrollbar (solo visual)
            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 2.dp),
                scrollState = scrollState
            )
        }

        // --- Dialogs DatePicker ---
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
private fun DestinationDropdown(
    destinos: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    // Campo tipo “dropdown” pero abre un diálogo (siempre funciona)
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Destino (capital europea)") },
            trailingIcon = { Text("▼") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Capa transparente encima para capturar el click en TODO el campo
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
                TextButton(onClick = { showDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
private fun RowLine(left: String, actionText: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(left)
        TextButton(onClick = onAction) { Text(actionText) }
    }
}

/**
 * Scrollbar vertical simple (VISUAL). No es arrastrable.
 * Compose no muestra scrollbar por defecto para Column + verticalScroll.
 */
@Composable
private fun VerticalScrollbar(
    modifier: Modifier,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val max = scrollState.maxValue
    if (max <= 0) return

    val progress = scrollState.value.toFloat() / max.toFloat() // 0..1

    val trackColor = Color(0x33000000)
    val thumbColor = Color(0x99000000)

    val thumbHeight = 72.dp
    val topPadding = (progress * 140).dp // aproximado (visual)

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