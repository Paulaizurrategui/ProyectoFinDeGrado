package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold

@Composable
fun FindFriendsScreen(onBack: () -> Unit) {
    val vm: FindFriendsViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    var query by remember { mutableStateOf("") }

    UrTriplyGradientScaffold(title = "Encontrar Amigos") {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nombre de usuario exacto...") },
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    IconButton(onClick = { vm.searchUsers(query) }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (state.users.isEmpty() && query.isNotEmpty()) {
                Text("No se han encontrado aventureros.", modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.users) { user ->
                    val isFollowing = state.followingIds.contains(user.uid)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(user.displayName, fontWeight = FontWeight.Bold)
                                Text(user.email, style = MaterialTheme.typography.bodySmall)
                            }

                            Button(
                                onClick = { vm.toggleFollow(user) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color.Gray else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(if (isFollowing) "Siguiendo" else "Seguir")
                            }
                        }
                    }
                }
            }
        }
    }
}