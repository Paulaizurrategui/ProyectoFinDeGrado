package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold

@Composable
fun InicioTabScreen(
    isGuest: Boolean, // true si el usuario entró con "Continuar sin cuenta" (modo invitado)
    onRequireLogin: () -> Unit // Callback para enviar al usuario a Login cuando quiera desbloquear funciones
) {
    UrTriplyGradientScaffold(title = stringResource(R.string.tab_home)) { // Scaffold común con el estilo UrTriply y título del tab
        Text(text = stringResource(R.string.home_subtitle)) // Texto introductorio del home
        Spacer(modifier = Modifier.height(16.dp)) // Separación visual

        if (isGuest) { // Si es invitado, mostramos un "mensaje" de que hay funciones bloqueadas
            Card {
                Column(
                    modifier = Modifier.padding(14.dp) // Padding interior de la card para que respire el contenido
                ) {
                    Text(text = stringResource(R.string.guest_hint_title), fontWeight = FontWeight.Bold) // Título: modo invitado
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = stringResource(R.string.guest_hint_body)) // Explicación: comunidad/guardado requieren login
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onRequireLogin) { // CTA para ir a login (decidido por el NavHost)
                        Text(stringResource(R.string.welcome_login))
                    }
                }
            }
        } else { // Si está autenticado, mostramos mensaje de estado OK (y en el futuro botones: Planificar, Borradores, Comunidad)
            Text(text = stringResource(R.string.home_logged_in_ok))
        }
    }
}