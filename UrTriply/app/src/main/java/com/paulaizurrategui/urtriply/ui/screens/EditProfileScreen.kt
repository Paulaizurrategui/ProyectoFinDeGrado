package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun EditProfileScreen(
    onBack: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    val user = auth.currentUser

    var name by remember { mutableStateOf(user?.displayName ?: "") }
    var bio by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var instagram by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scroll = rememberScrollState()

    // Cargar datos extra desde Firestore: users/{uid}
    LaunchedEffect(user?.uid) {
        val uid = user?.uid
        if (uid == null) {
            error = "No hay sesión."
            loading = false
            return@LaunchedEffect
        }

        loading = true
        error = null

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                bio = doc.getString("bio") ?: ""
                city = doc.getString("city") ?: ""
                instagram = doc.getString("instagram") ?: ""
                loading = false
            }
            .addOnFailureListener { e ->
                // No bloqueamos la pantalla si falla cargar; solo mostramos error
                error = e.message ?: "No se pudo cargar el perfil."
                loading = false
            }
    }

    Scaffold { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.Top
        ) {
            // Header custom (compatible con cualquier versión de Material3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("← Volver") }
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Editar perfil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (loading) {
                Text("Cargando...", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
            }

            Text("Nombre", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !saving
            )

            Spacer(Modifier.height(12.dp))

            Text("Bio", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !saving
            )

            Spacer(Modifier.height(12.dp))

            Text("Ciudad", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !saving
            )

            Spacer(Modifier.height(12.dp))

            Text("Instagram (opcional)", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = instagram,
                onValueChange = { instagram = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !saving,
                placeholder = { Text("@usuario o url") }
            )

            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val u = auth.currentUser
                    if (u == null) {
                        error = "No hay sesión."
                        return@Button
                    }

                    val uid = u.uid
                    val trimmedName = name.trim()

                    if (trimmedName.isBlank()) {
                        error = "El nombre no puede estar vacío."
                        return@Button
                    }

                    saving = true
                    error = null

                    // 1) Guardar nombre en FirebaseAuth
                    u.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(trimmedName)
                            .build()
                    ).addOnFailureListener { e ->
                        // Si falla, paramos aquí
                        saving = false
                        error = e.message ?: "No se pudo guardar el nombre."
                    }.addOnSuccessListener {
                        // 2) Guardar campos extra en Firestore (merge para no machacar)
                        val data = mapOf(
                            "displayName" to trimmedName,
                            "bio" to bio.trim(),
                            "city" to city.trim(),
                            "instagram" to instagram.trim()
                        )

                        db.collection("users").document(uid)
                            .set(data, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener {
                                saving = false
                                onBack()
                            }
                            .addOnFailureListener { e ->
                                saving = false
                                error = e.message ?: "No se pudo guardar el perfil."
                            }
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (saving) "Guardando..." else "Guardar")
            }
        }
    }
}