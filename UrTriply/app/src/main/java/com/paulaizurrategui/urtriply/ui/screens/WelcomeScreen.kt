package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold
import com.paulaizurrategui.urtriply.ui.theme.UrOrange
import com.paulaizurrategui.urtriply.ui.theme.UrTextMuted

@Composable
fun WelcomeScreen(
    isLoggedIn: Boolean,
    onGoHome: () -> Unit,
    onGoLogin: () -> Unit,
    onGoRegister: () -> Unit,
    onContinueGuest: () -> Unit
) {
    // Pantalla de bienvenida / entrada de la app.
    // - Muestra un slogan y botones de acción distintos según `isLoggedIn`.
    // - Callbacks: navegar a Home, Login, Register o continuar como invitado.
    UrTriplyGradientScaffold(
        title = "",
        showHeader = true,
        showTitle = false
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Texto con el eslogan/claim de la app (máx. 2 líneas)
            Text(
                text = stringResource(R.string.slogan),
                style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Si el usuario está autenticado: botón principal va a Home
            if (isLoggedIn) {
                Button(
                    onClick = onGoHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UrOrange)
                ) {
                    Text(
                        text = stringResource(R.string.welcome_go_home),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botón secundario para continuar como invitado
                OutlinedButton(
                    onClick = onContinueGuest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        stringResource(R.string.welcome_guest),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Button(
                    onClick = onGoLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UrOrange)
                ) {
                    Text(
                        text = stringResource(R.string.welcome_login),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onGoRegister,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        stringResource(R.string.welcome_register),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Enlace de texto alternativo para continuar como invitado
                TextButton(onClick = onContinueGuest) {
                    Text(
                        stringResource(R.string.welcome_guest),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Nota explicativa sobre limitaciones o comportamiento del modo invitado
            Text(
                text = stringResource(R.string.welcome_guest_note),
                style = MaterialTheme.typography.bodySmall,
                color = UrTextMuted
            )
        }
    }
}
