package com.paulaizurrategui.urtriply.ui.screensimport

import androidx.compose.foundation.Image
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.theme.UrCream
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    onBack: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val user = auth.currentUser

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados de los campos
    var name by remember { mutableStateOf(user?.displayName ?: "") }
    var photoUrl by remember { mutableStateOf(user?.photoUrl?.toString() ?: "") }
    var bio by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var instagram by remember { mutableStateOf("") }

    // Estados de UI
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Carga inicial de datos desde Firestore
    LaunchedEffect(user?.uid) {
        if (user == null) {
            loading = false
            return@LaunchedEffect
        }
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                bio = doc.getString("bio") ?: ""
                city = doc.getString("city") ?: ""
                instagram = doc.getString("instagram") ?: ""
                photoUrl = doc.getString("photoUrl") ?: user.photoUrl?.toString() ?: ""
                loading = false
            }
            .addOnFailureListener { loading = false }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Button(
                onClick = {
                    val u = auth.currentUser ?: return@Button
                    saving = true
                    // Actualizar DisplayName en Firebase Auth
                    u.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
                        .addOnSuccessListener {
                            // Actualizar info extra en Firestore
                            val data = mapOf(
                                "displayName" to name,
                                "bio" to bio,
                                "city" to city,
                                "instagram" to instagram
                            )
                            db.collection("users").document(u.uid).set(data, SetOptions.merge())
                                .addOnSuccessListener {
                                    saving = false
                                    onBack()
                                }
                                .addOnFailureListener { e ->
                                    saving = false
                                    error = e.message
                                }
                        }
                        .addOnFailureListener { e ->
                            saving = false
                            error = e.message
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                enabled = !saving
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Guardar cambios", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Volver") }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Editar Perfil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(20.dp))

            // Card de Visualización de Foto (Solo lectura)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUrl.isNotBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(photoUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("Sin foto", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "La foto de perfil no se puede cambiar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Formulario de datos de texto
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("Ciudad") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = instagram,
                onValueChange = { instagram = it },
                label = { Text("Instagram") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("@usuario") }
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(100.dp))
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