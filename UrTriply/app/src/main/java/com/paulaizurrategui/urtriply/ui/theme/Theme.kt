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

// Esquema de colores para modo oscuro (adaptado a UrTriply)
private val DarkColorScheme = darkColorScheme(
    primary = UrOrange,
    onPrimary = Color.White,

    secondary = UrSky,
    onSecondary = UrText,

    tertiary = UrCream,
    onTertiary = UrText,

    background = Color(0xFF0B1220),
    onBackground = Color.White,

    surface = Color(0xFF101827),
    onSurface = Color.White,

    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color.White.copy(alpha = 0.75f),

    outline = Color(0xFF334155),

    error = UrError,
    onError = Color.White
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