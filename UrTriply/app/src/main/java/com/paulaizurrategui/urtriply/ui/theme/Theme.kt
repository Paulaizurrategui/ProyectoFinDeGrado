package com.paulaizurrategui.urtriply.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Esquema de colores para modo oscuro (ahora mismo: colores del template morado)
private val DarkColorScheme = darkColorScheme(
    primary = Purple80, // Color principal (botones, highlights) en dark
    secondary = PurpleGrey80, // Color secundario en dark
    tertiary = Pink80 // Color terciario (acentos) en dark
)

// Esquema de colores para modo claro (ahora mismo: colores del template morado)
private val LightColorScheme = lightColorScheme(
    primary = Purple40, // Color principal en light
    secondary = PurpleGrey40, // Color secundario en light
    tertiary = Pink40 // Color terciario en light

)

@Composable
fun UrTriplyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Usa el tema del sistema (dark/light) por defecto
    dynamicColor: Boolean = true, // Si es true y Android 12+, usa colores dinámicos (Material You)
    content: @Composable () -> Unit // UI de tu app que se renderiza con este theme
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { // Android 12+ puede usar Material You
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) // Colores dinámicos según wallpaper
        }

        darkTheme -> DarkColorScheme // Si no hay dynamicColor o no soporta, usa esquema dark fijo
        else -> LightColorScheme // Si no, usa esquema light fijo
    }

    MaterialTheme(
        colorScheme = colorScheme, // Colores de la app (usados por Material components)
        typography = Typography, // Tipografía definida en Type.kt
        content = content // Contenido de la app
    )
}