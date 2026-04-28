package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold

@Composable
fun FindFriendsScreen(onBack: () -> Unit) {
    // vm de la pantalla (busqueda + follow)
    val vm: FindFriendsViewModel = viewModel()

    // ui state observable
    val state by vm.uiState.collectAsState()

    // texto de busqueda
    var query by remember { mutableStateOf("") }

    // scaffold con estilo comun (card + degradado)
    UrTriplyGradientScaffold(title = "Encontrar Amigos") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // volver al perfil
            TextButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 2.dp))
                Text("Volver al perfil", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // input de busqueda
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Busca por nombre o email...") },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                trailingIcon = {
                    // boton buscar
                    IconButton(onClick = { vm.searchUsers(query) }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // estados: cargando / vacio
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (state.users.isEmpty() && query.isNotEmpty()) {
                Text(
                    "No se han encontrado aventureros.",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // lista de usuarios encontrados
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.users, key = { it.uid }) { user ->
                    val isFollowing = state.followingIds.contains(user.uid)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // nombre + email
                            Column(Modifier.weight(1f)) {
                                Text(user.displayName.ifBlank { "Sin nombre" }, fontWeight = FontWeight.Bold)
                                if (user.email.isNotBlank()) {
                                    Text(user.email, style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            // boton seguir / siguiendo
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