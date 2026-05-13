package com.paulaizurrategui.urtriply.ui.auth

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paulaizurrategui.urtriply.data.favorites.FavoritesRepository
import com.paulaizurrategui.urtriply.data.likes.LikesRepository
import com.paulaizurrategui.urtriply.domain.model.CommunityFilters
import com.paulaizurrategui.urtriply.domain.model.TravelPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CommunityViewModel : ViewModel() {

    // firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val likesRepo = LikesRepository()
    private val favoritesRepo = FavoritesRepository()

    // lista que consume la ui (ya filtrada)
    private val _posts = MutableStateFlow<List<TravelPost>>(emptyList())
    val posts: StateFlow<List<TravelPost>> = _posts

    // estados de ui (filtros + loading)
    val filters: MutableState<CommunityFilters> = mutableStateOf(CommunityFilters())
    val isLoading: MutableState<Boolean> = mutableStateOf(false)
    val showFilters: MutableState<Boolean> = mutableStateOf(false)
    val errorMessage: MutableState<String?> = mutableStateOf(null)

    // interno: posts sin filtrar (feed base)
    private var rawPosts: List<TravelPost> = emptyList()
    private var likedTripIds: Set<String> = emptySet()
    private var favoriteTripIds: Set<String> = emptySet()

    // bloqueos del usuario actual
    private var blockedUserIds: Set<String> = emptySet()
    private var usersWhoBlockedMe: Set<String> = emptySet()

    init {
        // al crear el vm, cargo los bloqueos y luego escucho a quien sigo
        loadBlockedUsers()
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

    // like local + persistir en firestore
    fun toggleLike(postId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            errorMessage.value = "Inicia sesión para dar like"
            return
        }

        // Get current state
        val post = _posts.value.find { it.id == postId }
        val isCurrentlyLiked = post?.isLiked ?: false

        // Save previous state for rollback
        val previousPosts = _posts.value
        val previousRawPosts = rawPosts

        // Update UI optimistically
        val updated = _posts.value.map { p ->
            if (p.id != postId) p
            else p.copy(
                isLiked = !p.isLiked,
                likes = if (!p.isLiked) p.likes + 1 else maxOf(0, p.likes - 1)
            )
        }

        rawPosts = rawPosts.map { p ->
            if (p.id != postId) p
            else p.copy(
                isLiked = !p.isLiked,
                likes = if (!p.isLiked) p.likes + 1 else maxOf(0, p.likes - 1)
            )
        }

        _posts.value = updated

        // Persist to Firestore WITH ERROR HANDLING
        if (isCurrentlyLiked) {
            // Remove like
            likesRepo.removeLike(postId, currentUser.uid,
                onSuccess = {
                    errorMessage.value = null
                },
                onError = { e ->
                    // Rollback UI on error
                    _posts.value = previousPosts
                    rawPosts = previousRawPosts
                    errorMessage.value = "Error: ${e.message}"
                }
            )
        } else {
            // Add like
            likesRepo.addLike(postId, currentUser.uid,
                onSuccess = {
                    errorMessage.value = null
                },
                onError = { e ->
                    // Rollback UI on error
                    _posts.value = previousPosts
                    rawPosts = previousRawPosts
                    errorMessage.value = "Error: ${e.message}"
                }
            )
        }
    }

    // favorito local + persistir en firestore
    fun toggleFavorite(postId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            errorMessage.value = "Inicia sesión para guardar favoritos"
            return
        }

        // Get current state
        val post = _posts.value.find { it.id == postId }
        val isCurrentlyFavorite = post?.isFavorite ?: false

        // Save previous state for rollback
        val previousPosts = _posts.value
        val previousRawPosts = rawPosts

        // Update UI optimistically
        val updated = _posts.value.map { p ->
            if (p.id != postId) p else p.copy(isFavorite = !p.isFavorite)
        }

        rawPosts = rawPosts.map { p ->
            if (p.id != postId) p else p.copy(isFavorite = !p.isFavorite)
        }

        _posts.value = updated

        // Persist to Firestore WITH ERROR HANDLING
        if (isCurrentlyFavorite) {
            // Remove favorite
            favoritesRepo.removeFavorite(postId, currentUser.uid,
                onSuccess = {
                    errorMessage.value = null
                },
                onError = { e ->
                    // Rollback UI on error
                    _posts.value = previousPosts
                    rawPosts = previousRawPosts
                    errorMessage.value = "Error: ${e.message}"
                }
            )
        } else {
            // Add favorite
            favoritesRepo.addFavorite(postId, currentUser.uid,
                onSuccess = {
                    errorMessage.value = null
                },
                onError = { e ->
                    // Rollback UI on error
                    _posts.value = previousPosts
                    rawPosts = previousRawPosts
                    errorMessage.value = "Error: ${e.message}"
                }
            )
        }
    }

    // carga todos los bloqueos del usuario actual
    private fun loadBlockedUsers() {
        val myUid = auth.currentUser?.uid ?: run {
            // si no hay usuario, no hay bloqueos
            observeFollowingAndLoadFeed()
            return
        }

        // 1. Cargar usuarios que yo he bloqueado (en /users/{myUid}/blocks/)
        db.collection("users").document(myUid).collection("blocks")
            .get()
            .addOnSuccessListener { snap ->
                blockedUserIds = snap.documents.mapNotNull { it.id }.toSet()
                loadUsersWhoBlockedMe()
            }
            .addOnFailureListener {
                // si falla, continuar sin bloqueos
                blockedUserIds = emptySet()
                loadUsersWhoBlockedMe()
            }
    }

    // carga usuarios que me han bloqueado (query en /users/{otherUid}/blocks/{myUid})
    private fun loadUsersWhoBlockedMe() {
        val myUid = auth.currentUser?.uid ?: run {
            observeFollowingAndLoadFeed()
            return
        }

        // Para saber quién me bloqueó, debo buscar todos los usuarios y revisar si me tienen en su blocks
        // Sin embargo, esto es ineficiente. Alternativa: guardar en /users/{myUid}/blockedBy
        // Por ahora, voy a asumir que ese campo existe y lo consulto

        db.collection("users").document(myUid)
            .get()
            .addOnSuccessListener { doc ->
                val blockedByList = doc.get("blockedByUserIds") as? List<String> ?: emptyList()
                usersWhoBlockedMe = blockedByList.toSet()
                observeFollowingAndLoadFeed()
            }
            .addOnFailureListener {
                usersWhoBlockedMe = emptySet()
                observeFollowingAndLoadFeed()
            }
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
                // limit initial query size to reduce startup latency and reads
                .limit(50)
                .get()
                .addOnSuccessListener { snap ->
                    val mapped = snap.documents.mapNotNull { doc ->
                        val isDeleted = doc.getBoolean("deleted") ?: false
                        if (isDeleted) {
                            return@mapNotNull null
                        }

                        // verificar si está bloqueado
                        val authorUid = doc.getString("authorUid") ?: return@mapNotNull null
                        
                        // si yo bloqueé al autor, ignorar post
                        if (authorUid in blockedUserIds) {
                            return@mapNotNull null
                        }
                        
                        // si el autor me bloqueó, ignorar post
                        if (authorUid in usersWhoBlockedMe) {
                            return@mapNotNull null
                        }

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
                        loadUserInteractionStates()
                    }
                }
        }
    }

    private fun loadUserInteractionStates() {
        val myUid = auth.currentUser?.uid
        if (myUid == null) {
            likedTripIds = emptySet()
            favoriteTripIds = emptySet()
            applyInteractionFlagsAndFilters()
            isLoading.value = false
            return
        }

        var loadedParts = 0
        val markLoaded: () -> Unit = {
            loadedParts++
            if (loadedParts == 2) {
                applyInteractionFlagsAndFilters()
                isLoading.value = false
            }
        }

        db.collectionGroup("likes")
            .whereEqualTo("uid", myUid)
            .get()
            .addOnSuccessListener { snap ->
                likedTripIds = snap.documents
                    .mapNotNull { it.reference.parent.parent?.id }
                    .toSet()
            }
            .addOnFailureListener {
                likedTripIds = emptySet()
            }
            .addOnCompleteListener {
                markLoaded()
            }

        db.collectionGroup("favorites")
            .whereEqualTo("uid", myUid)
            .get()
            .addOnSuccessListener { snap ->
                favoriteTripIds = snap.documents
                    .mapNotNull { it.reference.parent.parent?.id }
                    .toSet()
            }
            .addOnFailureListener {
                favoriteTripIds = emptySet()
            }
            .addOnCompleteListener {
                markLoaded()
            }
    }

    private fun applyInteractionFlagsAndFilters() {
        rawPosts = rawPosts.map { post ->
            post.copy(
                isLiked = post.id in likedTripIds,
                isFavorite = post.id in favoriteTripIds
            )
        }
        applyFilters()
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