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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.firestore.SetOptions
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.theme.UrCream

@Composable
fun EditProfileScreen(
    onBack: () -> Unit
) {
    // instancias de firebase
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    // usuario actual
    val user = auth.currentUser

    // estado local de campos
    var name by remember { mutableStateOf(user?.displayName ?: "") }
    var photoUrl by remember { mutableStateOf(user?.photoUrl?.toString() ?: "") }
    var bio by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var instagram by remember { mutableStateOf("") }

    // estado local de ui
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var uploadingPhoto by remember { mutableStateOf(false) }

    // scroll para pantallas pequeñas
    val scroll = rememberScrollState()
    val context = LocalContext.current

    // cargo el doc de users/{uid} al abrir
    LaunchedEffect(user?.uid) {
        val uid = user?.uid
        if (uid == null) {
            // si no hay sesion, no puedo editar
            error = context.getString(R.string.edit_profile_no_session)
            loading = false
            return@LaunchedEffect
        }

        loading = true
        error = null

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                // campos opcionales (si no existen, dejo vacio)
                bio = doc.getString("bio") ?: ""
                city = doc.getString("city") ?: ""
                instagram = doc.getString("instagram") ?: ""
                photoUrl = doc.getString("photoUrl") ?: user?.photoUrl?.toString() ?: ""
                loading = false
            }
            .addOnFailureListener { e ->
                // si falla, dejo editar igualmente
                error = e.message ?: context.getString(R.string.edit_profile_load_error)
                loading = false
            }
    }

    Scaffold(
        bottomBar = {
            // barra inferior sticky con boton guardar
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
                            error = context.getString(R.string.edit_profile_no_session)
                            return@Button
                        }

                        // validacion simple del nombre
                        val trimmedName = name.trim()
                        if (trimmedName.isBlank()) {
                            error = context.getString(R.string.edit_profile_empty_name)
                            return@Button
                        }

                        saving = true
                        error = null

                        // 1) guardo displayname en firebase auth
                        u.updateProfile(
                            UserProfileChangeRequest.Builder()
                                .setDisplayName(trimmedName)
                                .build()
                        ).addOnFailureListener { e ->
                            saving = false
                            error = e.message ?: context.getString(R.string.edit_profile_name_error)
                        }.addOnSuccessListener {
                            // 2) guardo extras en firestore (merge para no pisar otros campos)
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
                                    error = e.message ?: context.getString(R.string.edit_profile_save_error)
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
                    // feedback visual mientras guardo
                    if (saving) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(12.dp))
                        Text(stringResource(R.string.edit_profile_saving), fontWeight = FontWeight.Bold)
                    } else {
                        Text(stringResource(R.string.edit_profile_save), fontWeight = FontWeight.Bold)
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
                // cabecera: volver + titulo
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
                        Text(stringResource(R.string.edit_profile_back))
                    }
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.edit_profile_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(Modifier.height(12.dp))

                // card de ayuda (tip)
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
                            text = stringResource(R.string.edit_profile_tip_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.edit_profile_tip_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // contenedor del formulario
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
                        // foto de perfil
                        FieldLabel(stringResource(R.string.edit_profile_photo_label))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val imageSize = 84.dp
                            if (photoUrl.isNotBlank()) {
                                // usa Coil si está disponible
                                androidx.compose.foundation.Image(
                                    painter = coil.compose.rememberAsyncImagePainter(photoUrl),
                                    contentDescription = null,
                                    modifier = Modifier.size(imageSize).clickable { /* abrir picker más abajo */ },
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(imageSize)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stringResource(R.string.edit_profile_no_photo), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(Modifier.widthIn(8.dp))

                            Column {
                                val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                                    if (uri != null && user != null) {
                                        uploadingPhoto = true
                                        // upload to Firebase Storage
                                        val storage = FirebaseStorage.getInstance()
                                        val ref = storage.reference.child("profile_photos/${user.uid}.jpg")
                                        val uploadTask = ref.putFile(uri)
                                        uploadTask.addOnSuccessListener {
                                            ref.downloadUrl.addOnSuccessListener { dl ->
                                                val url = dl.toString()
                                                // update Auth profile + users doc
                                                user.updateProfile(UserProfileChangeRequest.Builder().setPhotoUri(android.net.Uri.parse(url)).build())
                                                db.collection("users").document(user.uid)
                                                    .set(mapOf("photoUrl" to url), SetOptions.merge())
                                                    .addOnSuccessListener {
                                                        photoUrl = url
                                                        uploadingPhoto = false
                                                    }
                                                    .addOnFailureListener { e ->
                                                        uploadingPhoto = false
                                                        error = e.message ?: context.getString(R.string.edit_profile_photo_error)
                                                    }
                                            }.addOnFailureListener { e ->
                                                uploadingPhoto = false
                                                error = e.message ?: context.getString(R.string.edit_profile_photo_error)
                                            }
                                        }.addOnFailureListener { e ->
                                            uploadingPhoto = false
                                            error = e.message ?: context.getString(R.string.edit_profile_photo_error)
                                        }
                                    }
                                }

                                Button(onClick = { pickLauncher.launch("image/*") }, enabled = !uploadingPhoto) {
                                    Text(if (uploadingPhoto) stringResource(R.string.edit_profile_photo_uploading) else stringResource(R.string.edit_profile_change_photo))
                                }

                                if (photoUrl.isNotBlank()) {
                                    TextButton(onClick = {
                                        // remove photo
                                        val u = user
                                        if (u != null) {
                                            uploadingPhoto = true
                                            // remove storage file (best-effort)
                                            val storage = FirebaseStorage.getInstance()
                                            val ref = storage.reference.child("profile_photos/${u.uid}.jpg")
                                            ref.delete().addOnCompleteListener {
                                                val request = UserProfileChangeRequest.Builder().setPhotoUri(null).build()
                                                u.updateProfile(request).addOnCompleteListener {
                                                    db.collection("users").document(u.uid).set(mapOf("photoUrl" to null), SetOptions.merge())
                                                    photoUrl = ""
                                                    uploadingPhoto = false
                                                }
                                            }
                                        }
                                    }) {
                                        Text(stringResource(R.string.edit_profile_remove_photo))
                                    }
                                }
                            }
                        }

                        // texto mientras carga
                        if (loading) {
                            Text(stringResource(R.string.edit_profile_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // campo nombre
                        FieldLabel(stringResource(R.string.edit_profile_name))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !saving,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // campo bio
                        FieldLabel(stringResource(R.string.edit_profile_bio))
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            enabled = !saving,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // campo ciudad
                        FieldLabel(stringResource(R.string.edit_profile_city))
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !saving,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // campo instagram
                        FieldLabel(stringResource(R.string.edit_profile_instagram))
                        OutlinedTextField(
                            value = instagram,
                            onValueChange = { instagram = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !saving,
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text(stringResource(R.string.edit_profile_instagram_placeholder)) }
                        )

                        // bloque de error
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

                // espacio extra para que no se tape con la bottombar
                Spacer(Modifier.height(90.dp))
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    // label para separar visualmente los campos
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
}