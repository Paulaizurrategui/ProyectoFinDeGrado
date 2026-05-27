package com.paulaizurrategui.urtriply

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.paulaizurrategui.urtriply.ui.navigation.AppNavHost
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paulaizurrategui.urtriply.ui.theme.ThemeViewModel
import com.paulaizurrategui.urtriply.ui.theme.UrTriplyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Habilita que la UI se dibuje edge-to-edge (bajo status/navigation bars)
        // Esto permite un aspecto más moderno; Compose debe gestionar insets/padding.
        enableEdgeToEdge()

        // Define el contenido Compose de la Activity.
        setContent {
            // Obtengo el ViewModel que controla el tema (oscuro/claro).
            val themeViewModel: ThemeViewModel = viewModel()

            // Convierto el StateFlow `isDarkTheme` a estado observable por Compose
            // y tomo su valor actual para decidir el modo de la UI.
            val isDarkTheme = themeViewModel.isDarkTheme.collectAsState().value

            // Cargo la preferencia de tema una sola vez al arrancar la app.
            // `LaunchedEffect(Unit)` asegura que la acción se ejecute únicamente
            // en la primera composición.
            LaunchedEffect(Unit) {
                themeViewModel.loadThemePreference()
            }

            // Aplico el Theme global de la app, pasando si debe usar modo oscuro.
            // El Theme configura colores, tipografías y shapes (Material3).
            UrTriplyTheme(darkTheme = isDarkTheme) {
                // Contenedor principal que ocupa todo el tamaño disponible.
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                    // Inserta el NavHost de la app: gestiona rutas y pantallas.
                    AppNavHost()
                }
            }
        }
    }
}