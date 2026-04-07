package com.paulaizurrategui.urtriply.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Definición de tipografías para Material3.
// Ahora mismo solo estás sobreescribiendo bodyLarge; el resto usa los valores por defecto del sistema.
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default, // Fuente por defecto del sistema (Roboto normalmente)
        fontWeight = FontWeight.Normal, // Peso normal
        fontSize = 16.sp, // Tamaño de texto estándar para cuerpo
        lineHeight = 24.sp, // Alto de línea (mejora legibilidad)
        letterSpacing = 0.5.sp // Separación entre letras (ligera)
    )
)