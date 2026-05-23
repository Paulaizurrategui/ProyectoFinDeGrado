package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold

@Composable
fun RequireLoginScreen(onRequireLogin: () -> Unit) {
    UrTriplyGradientScaffold(title = stringResource(R.string.login_required_title)) { // Scaffold común con el look&feel (degradado + título)
        Text(text = stringResource(R.string.login_required_body)) // Explica que esa sección es solo para usuarios registrados
        Spacer(modifier = Modifier.height(14.dp)) // Separación visual entre texto y botón
        Button(
            onClick = onRequireLogin,
            modifier = Modifier.fillMaxWidth()
        ) { // Acción principal: llevar a Login (callback lo decide el NavHost)
            Text(stringResource(R.string.welcome_login)) // Reutiliza el mismo string de "Iniciar sesión"
        }
    }
}