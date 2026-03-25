package com.paulaizurrategui.urtriply.ui.screens

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
    isGuest: Boolean,
    onRequireLogin: () -> Unit
) {
    UrTriplyGradientScaffold(title = stringResource(R.string.tab_home)) {
        Text(text = stringResource(R.string.home_subtitle))
        Spacer(modifier = Modifier.height(16.dp))

        if (isGuest) {
            Card {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(text = stringResource(R.string.guest_hint_title), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = stringResource(R.string.guest_hint_body))
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onRequireLogin) {
                        Text(stringResource(R.string.welcome_login))
                    }
                }
            }
        } else {
            Text(text = stringResource(R.string.home_logged_in_ok))
        }
    }
}