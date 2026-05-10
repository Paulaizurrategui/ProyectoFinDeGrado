package com.paulaizurrategui.urtriply.ui.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.paulaizurrategui.urtriply.data.comments.CommentRepository
import com.paulaizurrategui.urtriply.domain.model.Comment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CommentViewModel : ViewModel() {

    private val commentRepo = CommentRepository()
    private val auth = FirebaseAuth.getInstance()

    // State for displaying comments
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf("")
    val successMessage = mutableStateOf("")

    // Current trip being viewed
    private var currentTripId = ""

    // Load comments for a trip
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

    // Add a comment
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

    // Delete a comment (author or admin only - validated by security rules)
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

    // Update a comment (author only - validated by security rules)
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

    fun clearMessages() {
        if (successMessage.value.isNotEmpty()) {
            successMessage.value = ""
        }
        if (errorMessage.value.isNotEmpty()) {
            errorMessage.value = ""
        }
    }
}
