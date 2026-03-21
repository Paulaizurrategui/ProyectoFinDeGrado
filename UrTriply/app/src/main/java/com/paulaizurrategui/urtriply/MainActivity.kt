package com.paulaizurrategui.urtriply

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.paulaizurrategui.urtriply.ui.navigation.AppNavHost
import com.paulaizurrategui.urtriply.ui.theme.UrTriplyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UrTriplyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Esto es lo que debe verse al ejecutar la app
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        AppNavHost()
                    }
                }
            }
        }
    }
}