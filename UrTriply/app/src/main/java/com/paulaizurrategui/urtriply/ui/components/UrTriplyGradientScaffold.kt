package com.paulaizurrategui.urtriply.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paulaizurrategui.urtriply.R

@Composable
fun UrTriplyGradientScaffold(
    title: String,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,                // si false, quito el bloque "urtriply"
    showTitle: Boolean = title.isNotBlank(),   // si title viene vacio, no muestro titulo
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    // Determino si estamos en tema oscuro usando la luminancia del background.
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Color de fondo del contenedor principal (card). En oscuro uso surface del theme,
    // en claro uso blanco puro para mayor contraste.
    val cardContainer = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color.White

    // Color del header (el bloque superior). En oscuro uso surface con algo de alpha,
    // en claro se usa un tono naranja pastel definido por literal ARGB.
    val headerContainer = if (isDarkTheme) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else Color(0xFFFFF3E0)

    // Color del texto del header: onSurface en oscuro, naranja fuerte en claro.
    val headerTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else Color(0xFFEF6C00)

    // Degradado de fondo compartido por toda la pantalla. Emplea diferentes stops
    // según si estamos en modo oscuro o claro para mantener legibilidad.
    val bg = if (isDarkTheme) {
        Brush.verticalGradient(
            0f to MaterialTheme.colorScheme.background,
            0.55f to MaterialTheme.colorScheme.surface,
            1f to MaterialTheme.colorScheme.surfaceVariant
        )
    } else {
        Brush.verticalGradient(
            0f to Color(0xFF4FC3F7),
            0.55f to Color(0xFFB3E5FC),
            1f to Color(0xFFE3F2FD)
        )
    }

    // Superficie raíz: ocupa todo el espacio y usa el background del theme.
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // Caja principal con el degradado de fondo y padding exterior que deja
        // espacio alrededor de la Card central.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(20.dp), // padding exterior de la card
            contentAlignment = Alignment.Center // centro la card dentro de la caja
        ) {
            // Card principal con esquinas grandes y elevación.
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                // Columna interna con padding donde se colocan header, título y contenido.
                Column(modifier = Modifier.padding(20.dp)) {

                    // Header opcional: bloque con el nombre de la app centrado.
                    if (showHeader) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(headerContainer)
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Nombre de la aplicación mostrado en el header.
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.ExtraBold,
                                color = headerTextColor,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        // Espacio entre header y resto del contenido.
                        Spacer(modifier = Modifier.padding(top = 12.dp))
                    }

                    // Título de la pantalla (opcional). Incluye botón de retroceso si se
                    // pasó `onBack` para permitir navegación hacia atrás.
                    if (showTitle) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onBack != null) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                                }
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Separador visual entre título y contenido principal.
                        Spacer(modifier = Modifier.padding(top = 14.dp))
                    }

                    // Lugar donde se inyecta el contenido específico de cada pantalla.
                    content()
                }
            }
        }
    }
}