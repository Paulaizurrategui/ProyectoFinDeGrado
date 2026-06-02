package com.paulaizurrategui.urtriply.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.domain.model.Comment
import com.paulaizurrategui.urtriply.ui.theme.UrOrange

@Composable
fun CommentSection(
    tripId: String,
    comments: List<Comment>,
    isLoading: Boolean,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    currentUserId: String?,
    isAdmin: Boolean = false,
    onReportComment: ((Comment) -> Unit)? = null,
    onBlockUser: ((String) -> Unit)? = null
) {
    // Sección de comentarios reutilizable
    // - Muestra el contador de comentarios
    // - Permite añadir un comentario si el usuario está logueado
    // - Lista comentarios existentes con opciones para borrar/reportar/bloquear
    // Estado local para el texto del nuevo comentario
    var newCommentText by remember { mutableStateOf("") }

    // Contenedor vertical principal con padding lateral
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Título de la sección con el número de comentarios
        Text(
            text = stringResource(R.string.comments_title, comments.size),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Input para añadir un comentario: solo visible si hay usuario logueado
        if (currentUserId != null) {
            OutlinedTextField(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = { Text(stringResource(R.string.comments_placeholder)) },
                maxLines = 4,
                shape = RoundedCornerShape(8.dp),
                enabled = true,
                singleLine = false
            )

            // Botones de cancelar y publicar alineados a la derecha
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                // Botón cancelar: limpia el campo
                TextButton(
                    onClick = { newCommentText = "" }
                ) {
                    Text(stringResource(R.string.comments_cancel))
                }

                // Botón publicar: llama a onAddComment y limpia el campo
                Button(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            onAddComment(newCommentText)
                            newCommentText = ""
                        }
                    },
                    enabled = newCommentText.isNotBlank() && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UrOrange
                    )
                ) {
                    Text(stringResource(R.string.comments_button))
                }
            }
        } else {
            // Mensaje para usuarios no logueados invitando a iniciar sesión
            Text(
                text = stringResource(R.string.comments_login),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(8.dp)
            )
        }

        // Lista de comentarios o estado vacío/cargando
        Spacer(modifier = Modifier.height(16.dp))

        if (comments.isEmpty() && !isLoading) {
            // Mensaje cuando no hay comentarios
            Text(
                text = stringResource(R.string.comments_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else if (isLoading) {
            // Indicador de carga mientras se obtienen los comentarios
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            // Lista perezosa de comentarios con separación vertical
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(comments) { comment ->
                    CommentCard(
                        comment = comment,
                        onDelete = { onDeleteComment(comment.id) },
                        canDelete = currentUserId == comment.authorUid || isAdmin,
                        onReport = { onReportComment?.invoke(comment) },
                        onBlock = { authorUid -> onBlockUser?.invoke(authorUid) }
                    )
                }
            }
        }
    }
}

@Composable
fun CommentCard(
    comment: Comment,
    onDelete: () -> Unit,
    canDelete: Boolean,
    onReport: (() -> Unit)? = null,
    onBlock: ((String) -> Unit)? = null
) {
    // Card individual que renderiza un comentario y sus acciones
    // Estado local para mostrar el diálogo de confirmación de borrado
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Card que contiene todo el comentario (cabecera + texto)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: avatar, nombre y tiempo, y acciones (borrar/reportar/bloquear)
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Parte izquierda: avatar + nombre + tiempo
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar: si existe URL, se carga con Coil; si no, se muestra
                    // una surface circular con la inicial del autor.
                    if (comment.authorAvatar != null) {
                        val ctx = LocalContext.current
                        AsyncImage(
                            model = ImageRequest.Builder(ctx)
                                .data(comment.authorAvatar)
                                .crossfade(true)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = comment.authorName,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            placeholder = null
                        )
                    } else {
                        // Avatar fallback con inicial
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            color = UrOrange
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = comment.authorName.firstOrNull()?.uppercase() ?: "U",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Nombre del autor y tiempo relativo del comentario
                    Column {
                        Text(
                            text = comment.authorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = comment.createdAt?.toDate()?.let {
                                val now = System.currentTimeMillis()
                                val diff = now - it.seconds * 1000
                                when {
                                    diff < 60000 -> stringResource(R.string.time_just_now)
                                    diff < 3600000 -> stringResource(R.string.time_minutes_ago, diff / 60000)
                                    diff < 86400000 -> stringResource(R.string.time_hours_ago, diff / 3600000)
                                    else -> stringResource(R.string.time_days_ago, diff / 86400000)
                                }
                            } ?: stringResource(R.string.time_soon),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Acciones a la derecha: borrar (si puede), reportar, bloquear
                if (canDelete) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.comment_delete_icon),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onReport != null) {
                    IconButton(
                        onClick = onReport,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = stringResource(R.string.comment_report_icon),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onBlock != null) {
                    IconButton(
                        onClick = { onBlock(comment.authorUid) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = stringResource(R.string.block_user),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Texto del comentario
            Text(
                text = comment.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                fontSize = 14.sp
            )
        }
    }

    // Diálogo de confirmación para borrar comentario. Se muestra cuando se pulsa
    // el icono de borrar y evita borrados accidentales.
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.comment_delete_title)) },
            text = { Text(stringResource(R.string.comment_delete_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.comment_delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.comment_cancel_action))
                }
            }
        )
    }
}
