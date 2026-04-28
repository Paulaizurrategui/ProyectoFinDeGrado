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

    // firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // lista que consume la ui (ya filtrada)
    private val _posts = MutableStateFlow<List<TravelPost>>(emptyList())
    val posts: StateFlow<List<TravelPost>> = _posts

    // estados de ui (filtros + loading)
    val filters: MutableState<CommunityFilters> = mutableStateOf(CommunityFilters())
    val isLoading: MutableState<Boolean> = mutableStateOf(false)
    val showFilters: MutableState<Boolean> = mutableStateOf(false)

    // interno: posts sin filtrar (feed base)
    private var rawPosts: List<TravelPost> = emptyList()

    init {
        // al crear el vm, escucho a quien sigo y cargo el feed
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

    // like local (si aun no hay likes en firestore, esto al menos anima la ui)
    fun toggleLike(postId: String) {
        val updated = _posts.value.map { p ->
            if (p.id != postId) p
            else p.copy(
                isLiked = !p.isLiked,
                likes = if (!p.isLiked) p.likes + 1 else maxOf(0, p.likes - 1)
            )
        }

        // actualizo tambien rawposts para no perder estado al filtrar
        rawPosts = rawPosts.map { p ->
            if (p.id != postId) p
            else p.copy(
                isLiked = !p.isLiked,
                likes = if (!p.isLiked) p.likes + 1 else maxOf(0, p.likes - 1)
            )
        }

        _posts.value = updated
    }

    // favorito local
    fun toggleFavorite(postId: String) {
        val updated = _posts.value.map { p ->
            if (p.id != postId) p else p.copy(isFavorite = !p.isFavorite)
        }

        rawPosts = rawPosts.map { p ->
            if (p.id != postId) p else p.copy(isFavorite = !p.isFavorite)
        }

        _posts.value = updated
    }

    // escucha cambios en /users/{uid}/following para recargar el feed
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

                // ids de los usuarios a los que sigo (cada doc id = uid seguido)
                val followingIds = snap?.documents?.map { it.id } ?: emptyList()

                // si quieres incluir mis posts tambien, suma myuid aqui
                // val authors = (followingIds + myUid).distinct()
                val authors = followingIds.distinct()

                loadPublishedTripsFromAuthors(authors)
            }
    }

    // carga los viajes publicados de una lista de autores (wherein max 10)
    private fun loadPublishedTripsFromAuthors(authorUids: List<String>) {
        isLoading.value = true

        if (authorUids.isEmpty()) {
            rawPosts = emptyList()
            _posts.value = emptyList()
            isLoading.value = false
            return
        }

        // firestore: wherein solo permite 10 ids
        val chunks = authorUids.chunked(10)
        val all = mutableListOf<TravelPost>()
        var pending = chunks.size

        chunks.forEach { chunk ->
            db.collection("trips")
                .whereEqualTo("status", "PUBLISHED")
                .whereIn("authorUid", chunk)
                .get()
                .addOnSuccessListener { snap ->
                    val mapped = snap.documents.mapNotNull { doc ->
                        // destino (acepto ambos nombres por compatibilidad)
                        val destination = doc.getString("destination")
                            ?: doc.getString("destino")
                            ?: return@mapNotNull null

                        // dias (acepto ambos nombres)
                        val days = (doc.getLong("days")
                            ?: doc.getLong("diasRecomendados")
                            ?: 0L).toInt()

                        // presupuesto (acepto double o long y ambos nombres)
                        val budget = doc.getDouble("budget")
                            ?: doc.getDouble("presupuestoTotal")
                            ?: doc.getLong("budget")?.toDouble()
                            ?: doc.getLong("presupuestoTotal")?.toDouble()
                            ?: 0.0

                        // resto de campos (si faltan, pongo default para que no rompa)
                        val currency = doc.getString("currency") ?: "€"
                        val authorName = doc.getString("authorName") ?: "usuario"
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
                    // si falla una chunk, sigo con las demas (puede quedar el feed medio vacio)
                }
                .addOnCompleteListener {
                    pending--
                    if (pending == 0) {
                        // quito duplicados por si un post entra dos veces
                        rawPosts = all.distinctBy { it.id }
                        applyFilters()
                        isLoading.value = false
                    }
                }
        }
    }

    // aplica filtros locales sobre rawposts y actualiza lo que ve la ui
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