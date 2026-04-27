package com.paulaizurrategui.urtriply.ui.auth

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paulaizurrategui.urtriply.data.trips.TripStatus
import com.paulaizurrategui.urtriply.domain.model.CommunityFilters
import com.paulaizurrategui.urtriply.domain.model.TravelPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CommunityViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _posts = MutableStateFlow<List<TravelPost>>(emptyList())
    val posts: StateFlow<List<TravelPost>> = _posts.asStateFlow()

    private val _filters = mutableStateOf(CommunityFilters())
    val filters: State<CommunityFilters> = _filters

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _showFilters = mutableStateOf(false)
    val showFilters: State<Boolean> = _showFilters

    init {
        loadFeed()
    }

    fun loadFeed() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        _isLoading.value = true

        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDoc ->

                val following = userDoc.get("following") as? List<String> ?: emptyList()

                if (following.isEmpty()) {
                    _posts.value = emptyList()
                    _isLoading.value = false
                    return@addOnSuccessListener
                }

                loadPostsFromFollowing(following)
            }
            .addOnFailureListener {
                _isLoading.value = false
            }
    }

    private fun loadPostsFromFollowing(following: List<String>) {

        val allPosts = mutableListOf<TravelPost>()
        var completed = 0

        val chunks = following.chunked(10)

        for (chunk in chunks) {

            db.collection("trips")
                .whereIn("authorUid", chunk)
                .whereEqualTo("status", TripStatus.PUBLISHED.name)
                .get()
                .addOnSuccessListener { result ->

                    val posts = result.documents.map { doc ->

                        TravelPost(
                            id = doc.id,
                            destination = doc.getString("destino") ?: "",
                            days = (doc.getLong("days") ?: 0).toInt(),
                            budget = doc.getDouble("budget") ?: 0.0,
                            authorName = doc.getString("authorName") ?: "Usuario",
                            authorAvatar = doc.getString("authorAvatar"),
                            date = "Reciente",
                            description = doc.getString("description") ?: "",
                            imageUrl = doc.getString("imageUrl"),
                            likes = (doc.getLong("likes") ?: 0).toInt(),
                            comments = (doc.getLong("comments") ?: 0).toInt()
                        )
                    }

                    allPosts.addAll(posts)

                    completed++

                    if (completed == chunks.size) {
                        _posts.value = applyFilters(allPosts)
                        _isLoading.value = false
                    }
                }
                .addOnFailureListener {
                    completed++
                    if (completed == chunks.size) {
                        _isLoading.value = false
                    }
                }
        }
    }

    fun toggleFilters() {
        _showFilters.value = !_showFilters.value
    }

    fun updateFilters(newFilters: CommunityFilters) {
        _filters.value = newFilters
        loadFeed()
    }

    fun clearFilters() {
        _filters.value = CommunityFilters()
        loadFeed()
    }

    private fun applyFilters(list: List<TravelPost>): List<TravelPost> {
        val f = _filters.value

        return list.filter {
            val matchDest = f.destination.isEmpty() ||
                    it.destination.contains(f.destination, true)

            val matchBudget = f.maxBudget == null ||
                    it.budget <= f.maxBudget

            matchDest && matchBudget
        }
    }

    fun toggleLike(postId: String) {
        _posts.value = _posts.value.map {
            if (it.id == postId) {
                it.copy(
                    isLiked = !it.isLiked,
                    likes = if (it.isLiked) it.likes - 1 else it.likes + 1
                )
            } else it
        }
    }

    fun toggleFavorite(postId: String) {
        _posts.value = _posts.value.map {
            if (it.id == postId) it.copy(isFavorite = !it.isFavorite)
            else it
        }
    }
}