package com.paulaizurrategui.urtriply.ui.screens

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProfileFriendsCountViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var followingListener: ListenerRegistration? = null

    private val _followingCount = MutableStateFlow(0)
    val followingCount: StateFlow<Int> = _followingCount

    init {
        val uid = auth.currentUser?.uid

        if (uid != null) {
            followingListener = db.collection("users")
                .document(uid)
                .collection("following")
                .addSnapshotListener { snap, _ ->
                    _followingCount.value = snap?.size() ?: 0
                }
        } else {
            _followingCount.value = 0
        }
    }

    fun clearListeners() {
        followingListener?.remove()
        followingListener = null
    }

    override fun onCleared() {
        clearListeners()
        super.onCleared()
    }
}