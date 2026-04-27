package com.paulaizurrategui.urtriply.ui.auth

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paulaizurrategui.urtriply.domain.model.CommunityFilters
import com.paulaizurrategui.urtriply.domain.model.TravelPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CommunityViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Lo que tu UI ya espera:
    private val _posts = MutableStateFlow<List<TravelPost>>(emptyList())
    val posts: StateFlow<List<TravelPost>> = _posts

    val filters: MutableState<CommunityFilters> = mutableStateOf(CommunityFilters())
    val isLoading: MutableState<Boolean> = mutableStateOf(false)
    val showFilters: MutableState<Boolean> = mutableStateOf(false)

    // Interno: posts sin filtrar (de amigos)
    private var rawPosts: List<TravelPost> = emptyList()

    init {
        observeFollowingAndLoadFeed()
    }

    fun toggleFilters() {
        showFilters.value = !showFilters.value
    }

    fun updateFilters(newFilters: CommunityFilters) {
        filters.value = newFilters
        applyFilters()
    }

    fun clearFilters() {
        filters.value = CommunityFilters()
        applyFilters()
    }

    // Si todavía no tienes likes/favs en Firestore, al menos no rompe la UI
    fun toggleLike(postId: String) {
        val updated = _posts.value.map { p ->
            if (p.id != postId) p
            else p.copy(isLiked = !p.isLiked, likes = if (!p.isLiked) p.likes + 1 else maxOf(0, p.likes - 1))
        }
        // Actualiza ambos para que al filtrar no se pierda el estado local
        rawPosts = rawPosts.map { p ->
            if (p.id != postId) p
            else p.copy(isLiked = !p.isLiked, likes = if (!p.isLiked) p.likes + 1 else maxOf(0, p.likes - 1))
        }
        _posts.value = updated
    }

    fun toggleFavorite(postId: String) {
        val updated = _posts.value.map { p ->
            if (p.id != postId) p else p.copy(isFavorite = !p.isFavorite)
        }
        rawPosts = rawPosts.map { p ->
            if (p.id != postId) p else p.copy(isFavorite = !p.isFavorite)
        }
        _posts.value = updated
    }

    private fun observeFollowingAndLoadFeed() {
        val myUid = auth.currentUser?.uid ?: run {
            rawPosts = emptyList()
            _posts.value = emptyList()
            return
        }

        db.collection("users")
            .document(myUid)
            .collection("following")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    rawPosts = emptyList()
                    _posts.value = emptyList()
                    return@addSnapshotListener
                }

                val followingIds = snap?.documents?.map { it.id } ?: emptyList()

                // Si quieres incluir tus posts también:
                // val authors = (followingIds + myUid).distinct()
                val authors = followingIds.distinct()

                loadPublishedTripsFromAuthors(authors)
            }
    }

    private fun loadPublishedTripsFromAuthors(authorUids: List<String>) {
        isLoading.value = true

        if (authorUids.isEmpty()) {
            rawPosts = emptyList()
            _posts.value = emptyList()
            isLoading.value = false
            return
        }

        val chunks = authorUids.chunked(10) // whereIn max 10
        val all = mutableListOf<TravelPost>()
        var pending = chunks.size

        chunks.forEach { chunk ->
            db.collection("trips")
                .whereEqualTo("status", "PUBLISHED")
                .whereIn("authorUid", chunk)
                .get()
                .addOnSuccessListener { snap ->
                    val mapped = snap.documents.mapNotNull { doc ->
                        // Campos mínimos para tu TravelPost (según tu data class)
                        val destination = doc.getString("destination")
                            ?: doc.getString("destino")
                            ?: return@mapNotNull null

                        val days = (doc.getLong("days")
                            ?: doc.getLong("diasRecomendados")
                            ?: 0L).toInt()

                        val budget = doc.getDouble("budget")
                            ?: doc.getDouble("presupuestoTotal")
                            ?: doc.getLong("budget")?.toDouble()
                            ?: doc.getLong("presupuestoTotal")?.toDouble()
                            ?: 0.0

                        val currency = doc.getString("currency") ?: "€"
                        val authorName = doc.getString("authorName") ?: "Usuario"
                        val authorAvatar = doc.getString("authorAvatar")
                        val date = doc.getString("date") ?: ""
                        val description = doc.getString("description") ?: ""
                        val imageUrl = doc.getString("imageUrl")

                        TravelPost(
                            id = doc.id,
                            destination = destination,
                            days = days,
                            budget = budget,
                            currency = currency,
                            authorName = authorName,
                            authorAvatar = authorAvatar,
                            date = date,
                            description = description,
                            imageUrl = imageUrl,
                            likes = (doc.getLong("likes") ?: 0L).toInt(),
                            comments = (doc.getLong("comments") ?: 0L).toInt(),
                            isLiked = doc.getBoolean("isLiked") ?: false,
                            isFavorite = doc.getBoolean("isFavorite") ?: false
                        )
                    }

                    all += mapped
                }
                .addOnFailureListener {
                    // si falla una chunk, seguimos con las demás, pero al final puede quedar vacío
                }
                .addOnCompleteListener {
                    pending--
                    if (pending == 0) {
                        rawPosts = all.distinctBy { it.id }
                        applyFilters()
                        isLoading.value = false
                    }
                }
        }
    }

    private fun applyFilters() {
        val f = filters.value

        val filtered = rawPosts.filter { post ->
            val okDestination =
                f.destination.isBlank() || post.destination.contains(f.destination, ignoreCase = true)

            val okBudget =
                f.maxBudget == null || post.budget <= f.maxBudget!!.toDouble()

            okDestination && okBudget
        }

        _posts.value = filtered
    }
}