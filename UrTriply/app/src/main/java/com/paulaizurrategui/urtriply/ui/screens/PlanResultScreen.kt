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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold

@Composable
fun PlanResultScreen(
    isGuest: Boolean,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit
) {
    // 1) Recupero el resultado guardado en la memoria

    val r = PlanResultStore.lastResult


    UrTriplyGradientScaffold(title = "Propuesta") {

        // 2) Si no hay resultado saco un fallback de UI para no crashear.
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

        // 3) Si sí hay resultado pues lo muestro
        //    Lo hacemos scrollable porque puede ser largo
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {

            // --- Botón volver (arriba) ---
            TextButton(onClick = onBack) { Text("← Volver") }

            // --- Título principal ---
            Text(
                text = "Viaje a ${r.destino}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(8.dp))

            // --- Aviso de fallback (RF-19 / RNF-03) ---
            // Si usedFallback == true, significa que NO usamos APIs reales:
            // estamos mostrando estimaciones.
            if (r.usedFallback) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)) // tono “aviso”
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

            // --- Card: Resumen general ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Resumen", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Presupuesto total: €${"%.2f".format(r.presupuestoTotal)}")
                    Text("Viajeros: ${r.viajeros}")
                    Text("Duración recomendada: ${r.diasRecomendados} días")
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Card: Presupuesto por categorías (RF-13) ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Presupuesto por categorías", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))

                    // Iteramos el mapa (categoria -> cantidad)
                    r.presupuestoCategorias.forEach { (k, v) ->
                        Text("• $k: €${"%.2f".format(v)}")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Card: Itinerario por días (RF-13) ---
            // MVP: listado simple; luego se puede hacer expandible (acordeón).
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Itinerario por días", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))

                    r.itinerario.forEach { day ->
                        Text("• $day")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Card: Actividades (RF-13) ---
            // Separación de gratis y de pago.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Actividades recomendadas", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    Text("Gratis", fontWeight = FontWeight.SemiBold)
                    r.actividadesGratis.forEach { a -> Text("• $a") }

                    Spacer(Modifier.height(10.dp))

                    Text("De pago", fontWeight = FontWeight.SemiBold)
                    r.actividadesPago.forEach { a -> Text("• $a") }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Botones principales ---
            // Guardar/Publicar: requieren login (RF-20 / RF-22).
            // Si es invitado, llamamos al callback onRequireLogin.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // Guardar borrador
                OutlinedButton(
                    onClick = {
                        if (isGuest) {
                            onRequireLogin()
                        } else {
                            // TODO RF-20: Guardar borrador en Firestore
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar borrador")
                }

                // Publicar
                Button(
                    onClick = {
                        if (isGuest) {
                            onRequireLogin()
                        } else {
                            // TODO RF-22: Publicar en Firestore
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Publicar")
                }
            }

            Spacer(Modifier.height(10.dp))

            // Compartir (opcional)
            OutlinedButton(
                onClick = {
                    // TODO: Compartir (opcional)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Compartir")
            }
        }
    }
}