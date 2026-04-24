package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paulaizurrategui.urtriply.data.trips.TripStatus
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold

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

    // Dialog de mensajes (éxito/error)
    val dialogText = uiState.errorMessage ?: uiState.successMessage
    if (dialogText != null) {
        AlertDialog(
            onDismissRequest = { vm.clearMessages() },
            title = { Text(if (uiState.errorMessage != null) "Error" else "Aviso") },
            text = { Text(dialogText) },
            confirmButton = {
                TextButton(onClick = { vm.clearMessages() }) { Text("OK") }
            }
        )
    }

    UrTriplyGradientScaffold(title = "Propuesta") {

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

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {

            TextButton(onClick = onBack) { Text("← Volver") }

            Text(
                text = "Viaje a ${r.destino}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(8.dp))

            if (r.usedFallback) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = "Aviso: se han usado estimaciones (fallback).",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cuando conecte las APIs, se verá precios reales y enlaces.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ... (tus cards Resumen / Categorías / Itinerario / Actividades igual)

            Spacer(Modifier.height(16.dp))

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
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        when {
                            uiState.isSaving -> "Guardando..."
                            isPublished -> "Guardado"
                            else -> "Guardar borrador"
                        }
                    )
                }

                Button(
                    onClick = {
                        if (isGuest) onRequireLogin()
                        else vm.publish(r)
                    },
                    enabled = !uiState.isSaving && !isPublished,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        when {
                            uiState.isSaving -> "Publicando..."
                            isPublished -> "Ya publicado"
                            else -> "Publicar"
                        }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    // TODO: Compartir (opcional)
                },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Compartir")
            }
        }
    }
}