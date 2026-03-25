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
    UrTriplyGradientScaffold(title = stringResource(R.string.tab_plan)) {
        Text(text = stringResource(R.string.plan_placeholder_body))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isGuest) stringResource(R.string.plan_guest_note)
            else stringResource(R.string.plan_auth_note)
        )
    }
}