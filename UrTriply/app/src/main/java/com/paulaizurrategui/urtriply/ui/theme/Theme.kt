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
    primary = UrOrangeSoft,
    onPrimary = Color(0xFF1C1206),
    primaryContainer = Color(0xFF5B2B00),
    onPrimaryContainer = Color(0xFFFFDCC4),

    secondary = UrSkySoftDark,
    onSecondary = Color(0xFF062033),
    secondaryContainer = Color(0xFF12304A),
    onSecondaryContainer = Color(0xFFD7F0FF),

    tertiary = UrCreamDark,
    onTertiary = Color(0xFF2C2208),
    tertiaryContainer = Color(0xFF4A3A12),
    onTertiaryContainer = Color(0xFFFFEFC8),

    background = UrNightBackground,
    onBackground = UrNightText,

    surface = UrNightSurface,
    onSurface = UrNightText,
    surfaceVariant = UrNightSurface2,
    onSurfaceVariant = UrNightTextMuted,

    surfaceContainer = UrNightSurface2,
    surfaceContainerHigh = UrNightSurface3,
    surfaceContainerHighest = UrNightSurface3,

    outline = UrNightOutline,
    outlineVariant = Color(0xFF263246),

    inverseSurface = Color(0xFFEAF0FF),
    inverseOnSurface = Color(0xFF111827),
    inversePrimary = UrOrange,

    error = Color(0xFFFF8A80),
    onError = Color.White,

    errorContainer = Color(0xFF6B1B1B),
    onErrorContainer = Color(0xFFFFD7D7),

    scrim = Color(0xFF000000),
    surfaceTint = UrOrangeSoft
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