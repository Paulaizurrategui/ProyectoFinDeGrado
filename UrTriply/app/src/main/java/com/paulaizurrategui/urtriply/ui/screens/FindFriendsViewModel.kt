package com.paulaizurrategui.urtriply.ui.screens

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paulaizurrategui.urtriply.domain.model.UserDoc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// estado de la pantalla encontrar amigos
data class FindFriendsUiState(
    val isLoading: Boolean = false,
    val users: List<UserDoc> = emptyList(),
    val followingIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

class FindFriendsViewModel : ViewModel() {
    // firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // estado observable por compose
    private val _uiState = MutableStateFlow(FindFriendsUiState())
    val uiState: StateFlow<FindFriendsUiState> = _uiState

    init {
        // cargo la lista de following al iniciar
        loadFollowingList()
    }

    private fun loadFollowingList() {
        val currentUid = auth.currentUser?.uid ?: return

        // escucho en tiempo real los ids que sigo
        db.collection("users")
            .document(currentUid)
            .collection("following")
            .addSnapshotListener { snapshot, _ ->
                val ids = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                _uiState.value = _uiState.value.copy(followingIds = ids)
            }
    }

    fun searchUsers(query: String) {
        val currentUid = auth.currentUser?.uid ?: run {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                users = emptyList(),
                errorMessage = "Inicia sesión para buscar amigos"
            )
            return
        }
        val q = query.trim()

        // si esta vacio, limpio la lista
        if (q.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                users = emptyList(),
                errorMessage = null
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        // busqueda simple: traigo users y filtro en cliente
        db.collection("users")
            .limit(50)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { doc ->
                    val uid = doc.id

                    // no me incluyo a mi
                    if (uid == currentUid) return@mapNotNull null

                    val displayName = doc.getString("displayName")?.trim().orEmpty()
                    val email = doc.getString("email")?.trim().orEmpty()

                    // match por nombre o email
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

        // miro si ya sigo al usuario
        val isFollowing = _uiState.value.followingIds.contains(targetUser.uid)

        val docRef = db.collection("users")
            .document(currentUid)
            .collection("following")
            .document(targetUser.uid)

        // update optimista para que cambie el boton al instante
        val newFollowingIds =
            if (isFollowing) _uiState.value.followingIds - targetUser.uid
            else _uiState.value.followingIds + targetUser.uid

        _uiState.value = _uiState.value.copy(followingIds = newFollowingIds)

        if (isFollowing) {
            // dejar de seguir
            docRef.delete()
                .addOnFailureListener { e ->
                    // revertir si falla
                    _uiState.value = _uiState.value.copy(
                        followingIds = _uiState.value.followingIds + targetUser.uid,
                        errorMessage = e.message ?: "No se pudo dejar de seguir"
                    )
                }
        } else {
            // seguir (guardo algo de info + timestamp)
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