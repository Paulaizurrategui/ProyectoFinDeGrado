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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.components.UrTriplyGradientScaffold

@Composable
fun FindFriendsScreen(onBack: () -> Unit) {
    val vm: FindFriendsViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    var query by remember { mutableStateOf("") }

    UrTriplyGradientScaffold(title = stringResource(R.string.friends_title)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                Spacer(Modifier.padding(horizontal = 2.dp))
                Text(stringResource(R.string.friends_back_profile), fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.friends_search_placeholder)) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { vm.searchUsers(query) }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.friends_search_icon))
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
                    // mostrar errores (ej. no autenticado o fallo en la consulta)
                    if (!state.errorMessage.isNullOrBlank()) {
                        Text(
                            text = state.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (state.users.isEmpty() && query.isNotEmpty()) {
                Text(
                    stringResource(R.string.friends_empty),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

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
                            Column(Modifier.weight(1f)) {
                                Text(
                                    user.displayName.ifBlank { stringResource(R.string.friends_no_name) },
                                    fontWeight = FontWeight.Bold
                                )
                                if (user.email.isNotBlank()) {
                                    Text(user.email, style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Button(
                                onClick = { vm.toggleFollow(user) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color.Gray else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(if (isFollowing) stringResource(R.string.friends_following) else stringResource(R.string.friends_follow))
                            }
                        }
                    }
                }
            }
        }
    }
}