package com.paulaizurrategui.urtriply.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paulaizurrategui.urtriply.R

@Composable
fun UrTriplyGradientScaffold(
    title: String, // Título de pantalla/tab (ej. "Perfil", "Comunidad", "Planificar"...)
    modifier: Modifier = Modifier, // Permite que la pantalla que lo use añada modificadores si lo necesita
    content: @Composable () -> Unit // Slot: contenido específico de cada pantalla
) {
    val bg = Brush.verticalGradient( // Fondo degradado común a toda la app (identidad visual consistente)
        0f to Color(0xFF4FC3F7),
        0.55f to Color(0xFFB3E5FC),
        1f to Color(0xFFE3F2FD)
    )

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { // Surface base que ocupa toda la pantalla
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg) // Aplica el degradado al fondo
                .padding(20.dp),
            contentAlignment = Alignment.Center // Centra la tarjeta principal
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)), // Estilo común: bordes redondeados
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White), // Tarjeta blanca para legibilidad
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) { // Padding interno de la tarjeta
                    Box( // Cabecera con el nombre de la app (reutiliza look de Login/Register/Welcome)
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFFFF3E0))
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.app_name), // "UrTriply"
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEF6C00),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Spacer(modifier = Modifier.padding(top = 12.dp)) // Espacio entre cabecera y título

                    Text(
                        text = title, // Título de la pantalla actual (lo pasa cada pantalla)
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )

                    Spacer(modifier = Modifier.padding(top = 14.dp)) // Espacio antes del contenido

                    content() // Aquí se inserta el contenido real de cada pantalla/tab
                }
            }
        }
    }
}