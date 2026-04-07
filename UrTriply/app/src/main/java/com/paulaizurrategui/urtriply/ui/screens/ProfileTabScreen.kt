package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.auth.AuthViewModel
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold

@Composable
fun ProfileTabScreen(
    authViewModel: AuthViewModel, // ViewModel para ejecutar logout y actualizar el estado de autenticación
    onLoggedOut: () -> Unit // Callback que decide qué hacer tras cerrar sesión (normalmente navegar a Welcome y limpiar backstack)
) {
    val email = FirebaseAuth.getInstance().currentUser?.email ?: "-" // Muestra el email del usuario logueado (si no existe, "-")

    UrTriplyGradientScaffold(title = stringResource(R.string.tab_profile)) { // Contenedor con título y fondo de marca
        Text(text = stringResource(R.string.profile_email, email)) // Texto tipo "Email: {email}" usando string con placeholder
        Spacer(modifier = Modifier.height(18.dp)) // Espacio antes del botón
        Button(onClick = {
            authViewModel.logout() // 1) Cierra sesión en Firebase (signOut) y actualiza uiState
            onLoggedOut() // 2) Notifica a la navegación para volver a Welcome y resetear historial
        }) {
            Text(stringResource(R.string.logout_button)) // Texto del botón "Cerrar sesión"
        }
    }
}