package com.paulaizurrategui.urtriply.ui.screens

// ---------- IMPORTS ----------
// Layout / UI base
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

// Para lista de destinos dentro del diálogo
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

// Material3
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState

// State / Compose runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Utilidades UI
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Scaffold propio con degradado y tarjeta
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold

// Fechas
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Enum con las preferencias del usuario para el viaje (multi-selección con chips)
private enum class Preference(val label: String) {
    CULTURA("Cultura"),
    OCIO("Ocio nocturno"),
    NATURALEZA("Naturaleza"),
    GASTRONOMIA("Gastronomía")
}

@OptIn(ExperimentalMaterial3Api::class) // Necesario por DatePicker / DatePickerDialog (APIs experimentales)
@Composable
fun PlanTabScreen(isGuest: Boolean) {
    // Scaffold con el estilo común de la app (degradado + tarjeta centrada + header)
    UrTriplyGradientScaffold(title = "Planificar") {

        // Catálogo MVP de destinos (10 capitales europeas)
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

        // ---------------------------
        // ESTADO DEL FORMULARIO (inputs)
        // ---------------------------

        // Destino seleccionado (por defecto el primero del catálogo)
        var destino by remember { mutableStateOf(destinos.first()) }

        // Presupuesto total (texto para poder escribir) -> luego lo parsearemos a Double cuando generemos
        var presupuestoText by remember { mutableStateOf("") }

        // Nº viajeros como texto (más fácil validar/limitar)
        var viajerosText by remember { mutableStateOf("1") }

        // Fechas en milisegundos (null si no se ha elegido)
        var fechaInicioMillis by remember { mutableStateOf<Long?>(null) }
        var fechaFinMillis by remember { mutableStateOf<Long?>(null) }

        // Preferencias seleccionadas (multi-select)
        var prefs by remember { mutableStateOf(setOf<Preference>()) }

        // ---------------------------
        // ESTADO UI auxiliar
        // ---------------------------

        // Controla si se muestran los diálogos de fecha
        var showStartPicker by remember { mutableStateOf(false) }
        var showEndPicker by remember { mutableStateOf(false) }

        // Formateo de fechas (dd/MM/yyyy) para mostrar en el UI
        val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
        fun formatDate(ms: Long?): String = ms?.let { dateFormat.format(Date(it)) } ?: "-"

        // Scroll: permite bajar/subir dentro de la tarjeta (y nos sirve para pintar la barra a la derecha)
        val scrollState = rememberScrollState()

        // Caja principal para:
        // - pintar el contenido scrollable
        // - superponer la “barra de scroll” a la derecha
        Box(modifier = Modifier.fillMaxWidth()) {

            // ---------------------------
            // CONTENIDO SCROLLABLE
            // ---------------------------
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState) // habilita scroll vertical
                    .fillMaxWidth()
                    .padding(end = 14.dp) // deja espacio para que el scrollbar no pise el contenido
            ) {
                // Texto informativo distinto según modo guest/auth
                Text(
                    text = if (isGuest)
                        "Modo invitado: puedes generar propuestas, pero para guardar/publicar necesitarás iniciar sesión."
                    else
                        "Completa el formulario para generar una propuesta ajustada a tu presupuesto.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(14.dp))

                // Selector de destino (en diálogo para evitar problemas de DropdownMenu dentro del Scaffold/Card)
                DestinationDropdown(
                    destinos = destinos,
                    selected = destino,
                    onSelected = { destino = it }
                )

                Spacer(Modifier.height(14.dp))

                // Presupuesto total
                OutlinedTextField(
                    value = presupuestoText,
                    onValueChange = { presupuestoText = it.replace(",", ".") }, // permite coma o punto decimal
                    label = { Text("Presupuesto total (EUR)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                // Número de viajeros (solo dígitos)
                OutlinedTextField(
                    value = viajerosText,
                    onValueChange = { viajerosText = it.filter(Char::isDigit) },
                    label = { Text("Número de viajeros") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                // Tarjeta del rango de fechas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Rango de fechas", style = MaterialTheme.typography.titleSmall)

                        Spacer(Modifier.height(8.dp))

                        // Fila: inicio + botón elegir
                        RowLine(
                            left = "Inicio: ${formatDate(fechaInicioMillis)}",
                            actionText = "Elegir",
                            onAction = { showStartPicker = true }
                        )

                        // Fila: fin + botón elegir
                        RowLine(
                            left = "Fin: ${formatDate(fechaFinMillis)}",
                            actionText = "Elegir",
                            onAction = { showEndPicker = true }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Preferencias (chips)
                Text("Preferencias", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                // Chips en 2 filas para que no se “aplasten”
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Preference.entries.take(2).forEach { pref ->
                            FilterChip(
                                selected = prefs.contains(pref),
                                onClick = {
                                    // toggle (si estaba, lo quita; si no estaba, lo añade)
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

                // Placeholder (luego aquí irá el botón “Generar propuesta” y el resultado)
                Text(
                    text = "Siguiente: botón “Generar propuesta” + resultado (Pantalla 5).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )

                Spacer(Modifier.height(10.dp))
            }

            // ---------------------------
            // SCROLLBAR VISUAL (derecha)
            // ---------------------------
            // Compose por defecto NO muestra scrollbar en Column + verticalScroll,
            // así que lo dibujamos manualmente para que se vea que se puede bajar/subir.
            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 2.dp),
                scrollState = scrollState
            )
        }

        // ---------------------------
        // DATE PICKERS (DIALOGS)
        // ---------------------------
        // Se abren al pulsar “Elegir” en inicio/fin.

        if (showStartPicker) {
            val state = rememberDatePickerState(initialSelectedDateMillis = fechaInicioMillis)
            DatePickerDialog(
                onDismissRequest = { showStartPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        fechaInicioMillis = state.selectedDateMillis // guarda fecha elegida
                        showStartPicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showStartPicker = false }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = state)
            }
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
                dismissButton = {
                    TextButton(onClick = { showEndPicker = false }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = state)
            }
        }
    }
}

@Composable
private fun DestinationDropdown(
    destinos: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    // Controla si el diálogo de destinos está visible
    var showDialog by remember { mutableStateOf(false) }

    // Campo tipo “dropdown” (pero no usamos DropdownMenu porque a veces falla dentro de Cards/Scaffold)
    Box(modifier = Modifier.fillMaxWidth()) {

        // Campo visible (solo lectura) que muestra el destino actual
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Destino (capital europea)") },
            trailingIcon = { Text("▼") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Capa transparente encima del campo:
        // así capturamos el click en TODO el área del TextField (siempre funciona)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }

    // Diálogo con la lista completa de destinos (scrollable)
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
                                    // cuando el usuario elige una opción:
                                    onSelected(option)
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            DividerDefaults.color
                        )
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
    // Fila genérica: texto a la izquierda + botón a la derecha (para Inicio/Fin)
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
 * Objetivo: que se vea “una barra” a la derecha indicando que hay scroll.
 */
@Composable
private fun VerticalScrollbar(
    modifier: Modifier,
    scrollState: androidx.compose.foundation.ScrollState
) {
    // maxValue: cuanto “scroll total” hay.
    // Si es 0, significa que el contenido no excede la pantalla => no hace falta scrollbar.
    val max = scrollState.maxValue
    if (max <= 0) return

    // progress (0..1): posición actual del scroll dentro del total
    val progress = scrollState.value.toFloat() / max.toFloat()

    // Colores semitransparentes para track (fondo) y thumb (la pieza que se mueve)
    val trackColor = Color(0x33000000)
    val thumbColor = Color(0x99000000)

    // Tamaño del thumb (fijo para MVP visual)
    val thumbHeight = 72.dp

    // Posición aproximada vertical del thumb según el progreso del scroll
    val topPadding = (progress * 140).dp

    // Track (barra de fondo)
    Box(
        modifier = modifier
            .width(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(trackColor)
    ) {
        // Thumb (la parte “oscura” que indica dónde estás)
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