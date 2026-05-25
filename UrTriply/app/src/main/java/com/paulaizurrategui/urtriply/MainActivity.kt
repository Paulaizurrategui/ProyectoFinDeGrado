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

        enableEdgeToEdge() // Permite dibujar bajo status/navigation bar (look moderno); Compose lo gestiona con paddings/insets

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val isDarkTheme = themeViewModel.isDarkTheme.collectAsState().value
            
            // Load theme preference ONLY ONCE on app startup
            LaunchedEffect(Unit) {
                themeViewModel.loadThemePreference()
            }

            UrTriplyTheme(darkTheme = isDarkTheme) { // Aplica el theme Material3 de la app (colores, tipografías, etc.)
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                    AppNavHost() // Punto de entrada real: navegación (Welcome/Login/Register/Main)
                }
            }
        }
    }
}