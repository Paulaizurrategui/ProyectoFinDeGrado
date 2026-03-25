package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.layout.Spacer
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
    UrTriplyGradientScaffold(title = stringResource(R.string.login_required_title)) {
        Text(text = stringResource(R.string.login_required_body))
        Spacer(modifier = Modifier.height(14.dp))
        Button(onClick = onRequireLogin) {
            Text(stringResource(R.string.welcome_login))
        }
    }
}