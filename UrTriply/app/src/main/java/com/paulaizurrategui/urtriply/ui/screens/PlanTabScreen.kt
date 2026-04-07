package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold

@Composable
fun PlanTabScreen(isGuest: Boolean) {
    UrTriplyGradientScaffold(title = stringResource(R.string.tab_plan)) { // Tab "Planificar" dentro del MainShell
        Text(text = stringResource(R.string.plan_placeholder_body)) // Placeholder: aquí irá el formulario de planificación del Hito 1
        Spacer(modifier = Modifier.height(16.dp)) // Separación visual
        Text(
            text = if (isGuest) stringResource(R.string.plan_guest_note) // Invitado: puede generar, pero se avisará de límites (guardar/publicar)
            else stringResource(R.string.plan_auth_note) // Auth: tendrá guardado de borradores/publicación disponible
        )
    }
}