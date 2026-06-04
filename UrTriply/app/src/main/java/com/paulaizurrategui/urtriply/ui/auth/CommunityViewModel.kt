package com.paulaizurrategui.urtriply.ui.auth

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.paulaizurrategui.urtriply.data.favorites.FavoritesRepository
import com.paulaizurrategui.urtriply.data.likes.LikesRepository
import com.paulaizurrategui.urtriply.domain.model.CommunityFilters
import com.paulaizurrategui.urtriply.domain.model.TravelPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CommunityViewModel : ViewModel() {
    // ViewModel que gestiona el feed de la comunidad (posts)
    // - Escucha a quién sigo y carga los viajes publicados por esos autores
    // - Gestiona likes y favoritos con actualizaciones optimistas y rollback
    // - Maneja bloqueos de usuarios para filtrar el feed localmente
    // Instancias y repositorios Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val likesRepo = LikesRepository()
    private val favoritesRepo = FavoritesRepository()

    // Estado expuesto a la UI: posts ya filtrados (aplicando filtros y bloqueos)
    private val _posts = MutableStateFlow<List<TravelPost>>(emptyList())
    val posts: StateFlow<List<TravelPost>> = _posts

    // Estados de UI locales: filtros, loader, panel de filtros y mensajes de error
    val filters: MutableState<CommunityFilters> = mutableStateOf(CommunityFilters())
    val isLoading: MutableState<Boolean> = mutableStateOf(false)
    val showFilters: MutableState<Boolean> = mutableStateOf(false)
    val errorMessage: MutableState<String?> = mutableStateOf(null)

    // Datos internos: `rawPosts` contiene el feed sin filtrar; liked/favorite ids
    private var rawPosts: List<TravelPost> = emptyList()
    private var likedTripIds: Set<String> = emptySet()
    private var favoriteTripIds: Set<String> = emptySet()

    // Bloqueos: usuarios que yo bloqueé y usuarios que me bloquearon
    private var blockedUserIds: Set<String> = emptySet()
    private var usersWhoBlockedMe: Set<String> = emptySet()

    // Seguimiento: flow con ids de usuarios que sigo y caché local para cambios
    val followingIdsFlow = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())
    private var lastFollowingIds: Set<String> = emptySet()
    private var followingListener: ListenerRegistration? = null
    private var authStateListener: AuthStateListener? = null

    // Al iniciar, subscribo un listener de auth y cargo bloqueos para filtrar el feed
    init {
        authStateListener = AuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                clearCommunityState()
                return@AuthStateListener
            }

            loadBlockedUsers()
        }
        auth.addAuthStateListener(authStateListener!!)

        // Si ya hay usuario, arranco inmediatamente la carga de bloqueos
        auth.currentUser?.let {
            loadBlockedUsers()
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearFollowingListener()
        authStateListener?.let { auth.removeAuthStateListener(it) }
        authStateListener = null
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

    // Block a user (add to /users/{myUid}/blocks/{userToBlockUid} and update user's blockedBy list)
    fun blockUser(userToBlockUid: String, onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {}) {
        val myUid = auth.currentUser?.uid ?: run {
            onError(Exception("No session"))
            return
        }

        val blockData = mapOf(
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        // Guardo el bloqueo en /users/{myUid}/blocks/{userToBlockUid}
        db.collection("users").document(myUid).collection("blocks")
            .document(userToBlockUid)
            .set(blockData)
                .addOnSuccessListener {
                // También actualizo el array `blockedByUserIds` del usuario objetivo
                db.collection("users").document(userToBlockUid)
                    .update(mapOf(
                        "blockedByUserIds" to com.google.firebase.firestore.FieldValue.arrayUnion(myUid)
                    ))
                    .addOnSuccessListener {
                        // Actualizo cache local y aplico filtros para quitar posts bloqueados
                        blockedUserIds = blockedUserIds + userToBlockUid
                        applyFilters()
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onError(e)
                    }
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    // Unblock a user
    fun unblockUser(userToUnblockUid: String, onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {}) {
        val myUid = auth.currentUser?.uid ?: run {
            onError(Exception("No session"))
            return
        }

        // Elimino el doc de bloqueo local y retiro la referencia en el usuario objetivo
        db.collection("users").document(myUid).collection("blocks")
            .document(userToUnblockUid)
            .delete()
            .addOnSuccessListener {
                db.collection("users").document(userToUnblockUid)
                    .update(mapOf(
                        "blockedByUserIds" to com.google.firebase.firestore.FieldValue.arrayRemove(myUid)
                    ))
                    .addOnSuccessListener {
                        blockedUserIds = blockedUserIds - userToUnblockUid
                        applyFilters()
                        onSuccess()
                    }
                    .addOnFailureListener { e -> onError(e) }
            }
            .addOnFailureListener { e -> onError(e) }
    }

    // like local + persistir en firestore
    fun toggleLike(postId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            errorMessage.value = "Inicia sesión para dar like"
            return
        }

        // Obtengo el post y su estado actual
        val post = _posts.value.find { it.id == postId }
        val isCurrentlyLiked = post?.isLiked ?: false

        // Guardo estado previo para rollback en caso de error
        val previousPosts = _posts.value
        val previousRawPosts = rawPosts

        // Optimistic update: actualizo UI inmediatamente para mejor UX.
        // Si la persistencia falla, hago rollback a los estados previos.
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

        // Persistencia en Firestore con manejo de errores: si falla, hago rollback
        if (isCurrentlyLiked) {
            // Remover like
            likesRepo.removeLike(postId, currentUser.uid,
                onSuccess = {
                    errorMessage.value = null
                    likedTripIds = likedTripIds - postId
                },
                onError = { e ->
                    // Rollback UI on error
                    _posts.value = previousPosts
                    rawPosts = previousRawPosts
                    errorMessage.value = "Error: ${e.message}"
                }
            )
        } else {
            // Añadir like
            likesRepo.addLike(postId, currentUser.uid,
                onSuccess = {
                    errorMessage.value = null
                    likedTripIds = likedTripIds + postId
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

        // Estado actual del favorito
        val post = _posts.value.find { it.id == postId }
        val isCurrentlyFavorite = post?.isFavorite ?: false

        // Guardar estado previo para rollback
        val previousPosts = _posts.value
        val previousRawPosts = rawPosts

        // Optimistic UI update para favoritos (igual que likes)
        val updated = _posts.value.map { p ->
            if (p.id != postId) p else p.copy(isFavorite = !p.isFavorite)
        }

        rawPosts = rawPosts.map { p ->
            if (p.id != postId) p else p.copy(isFavorite = !p.isFavorite)
        }

        _posts.value = updated

        // Persistencia y rollback en caso de error
        if (isCurrentlyFavorite) {
            // Eliminar favorito
            favoritesRepo.removeFavorite(postId, currentUser.uid,
                onSuccess = {
                    errorMessage.value = null
                    favoriteTripIds = favoriteTripIds - postId
                },
                onError = { e ->
                    // Rollback UI on error
                    _posts.value = previousPosts
                    rawPosts = previousRawPosts
                    errorMessage.value = "Error: ${e.message}"
                }
            )
        } else {
            // Añadir favorito
            favoritesRepo.addFavorite(postId, currentUser.uid,
                onSuccess = {
                    errorMessage.value = null
                    favoriteTripIds = favoriteTripIds + postId
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

    // Carga todos los bloqueos del usuario actual (los que yo hice)
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
                // Guardo los ids de usuarios a los que he bloqueado
                blockedUserIds = snap.documents.mapNotNull { it.id }.toSet()
                // Luego cargo quienes me bloquearon (si existe ese campo)
                loadUsersWhoBlockedMe()
            }
            .addOnFailureListener {
                // si falla, continuar sin bloqueos
                blockedUserIds = emptySet()
                loadUsersWhoBlockedMe()
            }
    }

    // Carga usuarios que me han bloqueado (intento leer campo `blockedByUserIds`)
    private fun loadUsersWhoBlockedMe() {
        val myUid = auth.currentUser?.uid ?: run {
            observeFollowingAndLoadFeed()
            return
        }

        // Nota: escanear todos los usuarios sería ineficiente. Prefiero leer
        // un campo `blockedByUserIds` dentro del propio documento del usuario.

        // Intento leer el campo `blockedByUserIds` del usuario actual. Es más eficiente
        // que escanear todos los usuarios para ver quién me tiene en su lista de bloques.
        db.collection("users").document(myUid)
            .get()
            .addOnSuccessListener { doc ->
                val blockedByList = (doc.get("blockedByUserIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?: emptyList()
                usersWhoBlockedMe = blockedByList.toSet()
                observeFollowingAndLoadFeed()
            }
            .addOnFailureListener {
                usersWhoBlockedMe = emptySet()
                observeFollowingAndLoadFeed()
            }
    }

    // Observa la colección `following` del usuario para saber a quién sigo,
    // y carga los viajes publicados por esos autores.
    private fun observeFollowingAndLoadFeed() {
        val myUid = auth.currentUser?.uid ?: run {
            rawPosts = emptyList()
            _posts.value = emptyList()
            return
        }

        clearFollowingListener()

        // En lugar de escuchar todos los viajes, escuchamos la lista de usuarios que sigo
        // Escucho cambios en la colección /users/{myUid}/following para saber a quién sigo
        followingListener = db.collection("users").document(myUid).collection("following")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    rawPosts = emptyList()
                    _posts.value = emptyList()
                    followingIdsFlow.value = emptySet()
                    return@addSnapshotListener
                }

                val followingIds = snap?.documents?.mapNotNull { it.id } ?: emptyList()
                val followingSet = followingIds.toSet()
                // Si no hay cambios en la lista de seguimiento, no recargo el feed
                if (followingSet == lastFollowingIds) {
                    // No hay cambios en la lista de seguimiento -> no recargo feed
                    followingIdsFlow.value = followingSet
                    return@addSnapshotListener
                }
                lastFollowingIds = followingSet
                // Actualizo el flow para que la UI pueda reaccionar (ej. mostrar CTA)
                followingIdsFlow.value = followingSet
                // Cargo los viajes publicados por los autores que sigo
                loadPublishedTripsFromAuthors(followingIds)
            }
    }

    private fun clearFollowingListener() {
        followingListener?.remove()
        followingListener = null
    }

    private fun clearCommunityState() {
        clearFollowingListener()
        rawPosts = emptyList()
        likedTripIds = emptySet()
        favoriteTripIds = emptySet()
        blockedUserIds = emptySet()
        usersWhoBlockedMe = emptySet()
        lastFollowingIds = emptySet()
        followingIdsFlow.value = emptySet()
        _posts.value = emptyList()
        isLoading.value = false
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

        // Firestore `whereIn` limita a 10 ids por consulta, por eso troceo la lista
        val chunks = authorUids.chunked(10)
        val all = mutableListOf<TravelPost>()
        var pending = chunks.size
        val completeChunk: () -> Unit = {
            pending--
            if (pending == 0) {
                // Elimino duplicados y cargo estados de interacción del usuario
                rawPosts = all.distinctBy { it.id }
                loadUserInteractionStates()
            }
        }

        chunks.forEach { chunk ->
                db.collection("trips")
                .whereEqualTo("status", "PUBLISHED")
                .whereIn("authorUid", chunk)
                // Limito resultados iniciales para mejorar latencia
                .limit(20)
                .get()
                .addOnSuccessListener { snap ->
                    val docs = snap.documents
                    val missingAuthorUids = docs.mapNotNull { doc ->
                        val uid = doc.getString("authorUid") ?: return@mapNotNull null
                        val storedName = doc.getString("authorName").orEmpty().trim()
                        if (storedName.isBlank() || storedName.equals("usuario", true)) uid else null
                    }.toSet()

                    resolveAuthorNames(missingAuthorUids) { namesByUid ->
                        val mapped = docs.mapNotNull { doc ->
                            val isDeleted = doc.getBoolean("deleted") ?: false
                            if (isDeleted) {
                                return@mapNotNull null
                            }

                            // Filtrado por bloqueos: ignoro posts de autores bloqueados
                            val authorUid = doc.getString("authorUid") ?: return@mapNotNull null
                            if (authorUid in blockedUserIds) return@mapNotNull null
                            if (authorUid in usersWhoBlockedMe) return@mapNotNull null

                            // Campos principales con compatibilidad de nombres
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

                            // Campos restantes con valores por defecto para robustez
                            val currency = doc.getString("currency") ?: "€"
                            val storedName = doc.getString("authorName").orEmpty().trim()
                            val authorName = when {
                                storedName.isNotBlank() && !storedName.equals("usuario", true) -> storedName
                                namesByUid[authorUid].isNullOrBlank().not() -> namesByUid[authorUid] ?: "usuario"
                                else -> "usuario"
                            }
                            val authorAvatar = doc.getString("authorAvatar")
                            val date = doc.getString("date") ?: ""
                            val description = doc.getString("description") ?: ""
                            val imageUrl = doc.getString("imageUrl")

                            TravelPost(
                                id = doc.id,
                                authorUid = authorUid,
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
                        completeChunk()
                    }
                }
                .addOnFailureListener {
                    errorMessage.value = it.message ?: "Error cargando publicaciones de la comunidad"
                    completeChunk()
                }
        }
    }

    private fun resolveAuthorNames(
        authorUids: Set<String>,
        onResult: (Map<String, String>) -> Unit
    ) {
        if (authorUids.isEmpty()) {
            onResult(emptyMap())
            return
        }

        val chunks = authorUids.toList().chunked(10)
        val resolved = mutableMapOf<String, String>()
        var pending = chunks.size

        chunks.forEach { chunk ->
            db.collection("users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .addOnSuccessListener { snap ->
                    snap.documents.forEach { userDoc ->
                        val uid = userDoc.id
                        val displayName = userDoc.getString("displayName").orEmpty().trim()
                        if (displayName.isNotBlank()) {
                            resolved[uid] = displayName
                        }
                    }
                }
                .addOnCompleteListener {
                    pending--
                    if (pending == 0) {
                        onResult(resolved)
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

        // Cargo en paralelo likes y favorites del usuario para marcar estados
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
        // Marco cada post con los flags de interacción del usuario
        rawPosts = rawPosts.map { post ->
            post.copy(
                isLiked = post.id in likedTripIds,
                isFavorite = post.id in favoriteTripIds
            )
        }
        // Aplicar filtros locales y actualizar UI
        applyFilters()
    }

    // aplica filtros locales sobre rawposts y actualiza lo que ve la ui
    private fun applyFilters() {
        val f = filters.value
        // Filtro por destino y presupuesto máximo (si están presentes)
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