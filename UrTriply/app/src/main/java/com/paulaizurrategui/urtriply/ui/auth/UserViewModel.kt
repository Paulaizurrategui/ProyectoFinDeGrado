package com.paulaizurrategui.urtriply.ui.auth


import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class UserViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    fun followUser(targetUserId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users")
            .document(currentUserId)
            .update("following", FieldValue.arrayUnion(targetUserId))
    }

    fun unfollowUser(targetUserId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users")
            .document(currentUserId)
            .update("following", FieldValue.arrayRemove(targetUserId))
    }
    fun searchUsers(query: String, onResult: (List<Pair<String, String>>) -> Unit) {

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        db.collection("users")
            .get()
            .addOnSuccessListener { result ->

                val users = result.documents.mapNotNull { doc ->

                    val name = doc.getString("name") ?: return@mapNotNull null
                    val uid = doc.id

                    if (uid == currentUserId) return@mapNotNull null

                    if (query.isEmpty()) return@mapNotNull null

                    if (name.contains(query, ignoreCase = true)) {
                        uid to name
                    } else null
                }

                onResult(users)
            }
    }
}