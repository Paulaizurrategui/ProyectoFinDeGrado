package com.paulaizurrategui.urtriply.ui.screens

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paulaizurrategui.urtriply.domain.model.UserDoc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class FindFriendsUiState(
    val isLoading: Boolean = false,
    val users: List<UserDoc> = emptyList(),
    val followingIds: Set<String> = emptySet(), // Para saber a quién seguimos ya
    val errorMessage: String? = null
)

class FindFriendsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow(FindFriendsUiState())
    val uiState: StateFlow<FindFriendsUiState> = _uiState

    init {
        loadFollowingList() // Cargamos a quién seguimos al empezar
    }

    private fun loadFollowingList() {
        val currentUid = auth.currentUser?.uid ?: return
        db.collection("users").document(currentUid).collection("following")
            .addSnapshotListener { snapshot, _ ->
                val ids = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                _uiState.value = _uiState.value.copy(followingIds = ids)
            }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) return
        val currentUid = auth.currentUser?.uid ?: return

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        db.collection("users")
            .whereEqualTo("displayName", query)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { it.toObject(UserDoc::class.java) }
                    .filter { it.uid != currentUid } // No te buscas a ti mismo
                _uiState.value = _uiState.value.copy(users = list, isLoading = false)
            }
            .addOnFailureListener {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Error en la búsqueda")
            }
    }

    fun toggleFollow(targetUser: UserDoc) {
        val currentUid = auth.currentUser?.uid ?: return
        val isFollowing = _uiState.value.followingIds.contains(targetUser.uid)
        val docRef = db.collection("users").document(currentUid)
            .collection("following").document(targetUser.uid)

        if (isFollowing) {
            docRef.delete() // Dejar de seguir
        } else {
            docRef.set(mapOf(
                "uid" to targetUser.uid,
                "displayName" to targetUser.displayName,
                "followedAt" to Timestamp.now()
            ))
        }
    }
}