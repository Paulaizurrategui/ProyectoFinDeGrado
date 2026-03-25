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
    authViewModel: AuthViewModel,
    onLoggedOut: () -> Unit
) {
    val email = FirebaseAuth.getInstance().currentUser?.email ?: "-"

    UrTriplyGradientScaffold(title = stringResource(R.string.tab_profile)) {
        Text(text = stringResource(R.string.profile_email, email))
        Spacer(modifier = Modifier.height(18.dp))
        Button(onClick = {
            authViewModel.logout()
            onLoggedOut()
        }) {
            Text(stringResource(R.string.logout_button))
        }
    }
}