package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold

@Composable
fun CommunityTabScreen() {
    UrTriplyGradientScaffold(title = stringResource(R.string.tab_community)) { // Tab "Comunidad" (solo modo auth; en guest se bloquea en MainShell)
        Text(text = stringResource(R.string.community_placeholder_body)) // Placeholder: aquí irá el feed de viajes publicados
    }
}