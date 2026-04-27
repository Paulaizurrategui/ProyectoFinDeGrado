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
    val followingIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

class FindFriendsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(FindFriendsUiState())
    val uiState: StateFlow<FindFriendsUiState> = _uiState

    init {
        loadFollowingList()
    }

    private fun loadFollowingList() {
        val currentUid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(currentUid)
            .collection("following")
            .addSnapshotListener { snapshot, _ ->
                val ids = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                _uiState.value = _uiState.value.copy(followingIds = ids)
            }
    }

    fun searchUsers(query: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val q = query.trim()

        if (q.isBlank()) {
            _uiState.value = _uiState.value.copy(isLoading = false, users = emptyList(), errorMessage = null)
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        db.collection("users")
            .limit(50)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { doc ->
                    val uid = doc.id
                    if (uid == currentUid) return@mapNotNull null

                    val displayName = doc.getString("displayName")?.trim().orEmpty()
                    val email = doc.getString("email")?.trim().orEmpty()

                    val matches = displayName.contains(q, ignoreCase = true) ||
                            email.contains(q, ignoreCase = true)

                    if (!matches) return@mapNotNull null

                    UserDoc(
                        uid = uid,
                        email = email,
                        displayName = displayName
                    )
                }

                _uiState.value = _uiState.value.copy(users = list, isLoading = false)
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Error en la búsqueda"
                )
            }
    }

    fun toggleFollow(targetUser: UserDoc) {
        val currentUid = auth.currentUser?.uid ?: return
        val isFollowing = _uiState.value.followingIds.contains(targetUser.uid)

        val docRef = db.collection("users")
            .document(currentUid)
            .collection("following")
            .document(targetUser.uid)

        // Update optimista para que el botón cambie al instante
        val newFollowingIds =
            if (isFollowing) _uiState.value.followingIds - targetUser.uid
            else _uiState.value.followingIds + targetUser.uid

        _uiState.value = _uiState.value.copy(followingIds = newFollowingIds)

        if (isFollowing) {
            docRef.delete()
                .addOnFailureListener { e ->
                    // revertir si falla
                    _uiState.value = _uiState.value.copy(
                        followingIds = _uiState.value.followingIds + targetUser.uid,
                        errorMessage = e.message ?: "No se pudo dejar de seguir"
                    )
                }
        } else {
            docRef.set(
                mapOf(
                    "uid" to targetUser.uid,
                    "displayName" to targetUser.displayName,
                    "email" to targetUser.email,
                    "followedAt" to Timestamp.now()
                )
            ).addOnFailureListener { e ->
                // revertir si falla
                _uiState.value = _uiState.value.copy(
                    followingIds = _uiState.value.followingIds - targetUser.uid,
                    errorMessage = e.message ?: "No se pudo seguir"
                )
            }
        }
    }
}