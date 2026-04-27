package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paulaizurrategui.urtriply.ui.auth.UserViewModel

@Composable
fun FindFriendsScreen(viewModel: UserViewModel = viewModel(),onBack: () -> Unit) {

    var searchText by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    Column(modifier = Modifier.padding(16.dp)) {
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("← Volver al perfil")
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                viewModel.searchUsers(it) { result ->
                    users = result
                }
            },
            label = { Text("Buscar usuarios") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(users) { (uid, name) ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name)

                    Button(onClick = {
                        viewModel.followUser(uid)
                    }) {
                        Text("Seguir")
                    }
                }
            }
        }
    }
}