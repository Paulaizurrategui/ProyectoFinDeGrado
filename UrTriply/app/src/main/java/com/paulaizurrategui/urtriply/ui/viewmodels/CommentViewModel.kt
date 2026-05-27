package com.paulaizurrategui.urtriply.ui.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.paulaizurrategui.urtriply.data.comments.CommentRepository
import com.paulaizurrategui.urtriply.domain.model.Comment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CommentViewModel : ViewModel() {
    // Repositorio de comentarios y autenticación
    private val commentRepo = CommentRepository()
    private val auth = FirebaseAuth.getInstance()

    // StateFlow que expone la lista de comentarios hacia la UI
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    // Flags y mensajes observables para la UI (cargando, error, éxito)
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf("")
    val successMessage = mutableStateOf("")

    // ID del viaje actualmente visualizado (usado por las operaciones de add/delete)
    private var currentTripId = ""

    // Carga los comentarios para un `tripId` y actualiza el estado.
    fun loadCommentsForTrip(tripId: String) {
        currentTripId = tripId
        isLoading.value = true
        errorMessage.value = ""

        commentRepo.getCommentsForTrip(
            tripId = tripId,
            onSuccess = { comments ->
                _comments.value = comments
                isLoading.value = false
            },
            onError = { e ->
                errorMessage.value = e.message ?: "Error loading comments"
                isLoading.value = false
            }
        )
    }

    // Añade un comentario para el `currentTripId`. Realiza validaciones locales
    // (texto vacío, usuario no autenticado) antes de delegar al repositorio.
    fun addComment(text: String) {
        if (text.isBlank()) {
            errorMessage.value = "Comment cannot be empty"
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            errorMessage.value = "Must be logged in to comment"
            return
        }

        isLoading.value = true
        errorMessage.value = ""

        commentRepo.addComment(
            tripId = currentTripId,
            authorUid = currentUser.uid,
            authorName = currentUser.displayName ?: "usuario",
            authorAvatar = currentUser.photoUrl?.toString(),
            text = text,
            onSuccess = {
                successMessage.value = "Comment added!"
                isLoading.value = false
                clearMessages()
            },
            onError = { e ->
                errorMessage.value = e.message ?: "Error adding comment"
                isLoading.value = false
            }
        )
    }

    // Elimina un comentario. Las reglas de seguridad en Firestore deben garantizar
    // que solo el autor o admin puedan borrarlo; aquí solo se llama al repo.
    fun deleteComment(commentId: String) {
        isLoading.value = true

        commentRepo.deleteComment(
            tripId = currentTripId,
            commentId = commentId,
            onSuccess = {
                successMessage.value = "Comment deleted"
                isLoading.value = false
                clearMessages()
            },
            onError = { e ->
                errorMessage.value = e.message ?: "Error deleting comment"
                isLoading.value = false
            }
        )
    }

    // Actualiza el texto de un comentario existente (validación local del texto)
    fun updateComment(commentId: String, newText: String) {
        if (newText.isBlank()) {
            errorMessage.value = "Comment cannot be empty"
            return
        }

        isLoading.value = true

        commentRepo.updateComment(
            tripId = currentTripId,
            commentId = commentId,
            newText = newText,
            onSuccess = {
                successMessage.value = "Comment updated"
                isLoading.value = false
                clearMessages()
            },
            onError = { e ->
                errorMessage.value = e.message ?: "Error updating comment"
                isLoading.value = false
            }
        )
    }

    // Limpia mensajes de éxito/ error si existen (utilizado tras operaciones)
    fun clearMessages() {
        if (successMessage.value.isNotEmpty()) {
            successMessage.value = ""
        }
        if (errorMessage.value.isNotEmpty()) {
            errorMessage.value = ""
        }
    }
}
