package com.paulaizurrategui.urtriply.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Esquema de colores para modo oscuro (adaptado a UrTriply) - MEJORADO
private val DarkColorScheme = darkColorScheme(
    primary = UrOrange,
    onPrimary = Color.White,

    secondary = UrSky,
    onSecondary = Color.White,

    tertiary = UrCream,
    onTertiary = Color(0xFF1A1A1A),

    background = Color(0xFF121212),  // Más claro para mejor contraste
    onBackground = Color(0xFFE8E8E8),  // Texto muy claro

    surface = Color(0xFF1E1E1E),     // Más visible
    onSurface = Color(0xFFF5F5F5),   // Blanco casi puro

    surfaceVariant = Color(0xFF2A2A2A),   // Más claro
    onSurfaceVariant = Color(0xFFD0D0D0),  // Gris claro

    outline = Color(0xFF505050),  // Líneas más visibles

    error = Color(0xFFFF6B6B),   // Rojo más vívido
    onError = Color.White,
    
    // Removed duplicate tertiary parameter
    // onTertiary = Color(0xFF1A1A1A), // This line is removed
    
    errorContainer = Color(0xFF8B0000),  // Rojo más oscuro para fondo
    onErrorContainer = Color(0xFFFFB3B3)
)

// Esquema de colores para modo claro (UrTriply)
private val LightColorScheme = lightColorScheme(
    primary = UrOrange,
    onPrimary = Color.White,

    secondary = UrSky,
    onSecondary = UrText,

    tertiary = UrCream,
    onTertiary = UrText,

    background = UrSkySoft,
    onBackground = UrText,

    surface = UrSurface,
    onSurface = UrText,

    surfaceVariant = UrSurface2,
    onSurfaceVariant = UrTextMuted,

    outline = UrOutline,

    error = UrError,
    onError = Color.White
)

@Composable
fun UrTriplyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // IMPORTANTE: false para que no te cambie los colores en Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}