package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.google.firebase.firestore.SetOptions
import com.paulaizurrategui.urtriply.ui.theme.UrCream

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

    // Cargar datos desde Firestore: users/{uid}
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
                // Mostramos el error pero dejamos editar igualmente
                error = e.message ?: "No se pudo cargar el perfil."
                loading = false
            }
    }

    Scaffold(
        bottomBar = {
            // CTA sticky (queda bien con el nuevo theme naranja)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        val u = auth.currentUser
                        if (u == null) {
                            error = "No hay sesión."
                            return@Button
                        }

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
                            saving = false
                            error = e.message ?: "No se pudo guardar el nombre."
                        }.addOnSuccessListener {
                            // 2) Guardar extras en Firestore (merge)
                            val uid = u.uid
                            val data = mapOf(
                                "displayName" to trimmedName,
                                "bio" to bio.trim(),
                                "city" to city.trim(),
                                "instagram" to instagram.trim()
                            )

                            db.collection("users").document(uid)
                                .set(data, SetOptions.merge())
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    if (saving) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(12.dp))
                        Text("Guardando…", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scroll)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("← Volver")
                    }
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Editar perfil",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(Modifier.height(12.dp))

                // “Tip” / cabecera suave
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = UrCream),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = "Personaliza tu perfil",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Estos datos se usarán para mostrar tu perfil en la app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Form container
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (loading) {
                            Text("Cargando...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        FieldLabel("Nombre")
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !saving,
                            shape = RoundedCornerShape(12.dp)
                        )

                        FieldLabel("Bio")
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            enabled = !saving,
                            shape = RoundedCornerShape(12.dp)
                        )

                        FieldLabel("Ciudad")
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !saving,
                            shape = RoundedCornerShape(12.dp)
                        )

                        FieldLabel("Instagram (opcional)")
                        OutlinedTextField(
                            value = instagram,
                            onValueChange = { instagram = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !saving,
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("@usuario o url") }
                        )

                        // Error (bonito)
                        if (error != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Text(
                                    text = error!!,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // espacio para el sticky CTA
                Spacer(Modifier.height(90.dp))
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
}