package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paulaizurrategui.urtriply.data.trips.TripDoc
import com.paulaizurrategui.urtriply.ui.auth.CommunityViewModel
import com.paulaizurrategui.urtriply.ui.components.CommentSection
import com.paulaizurrategui.urtriply.ui.viewmodels.CommentViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    
    var tripData by remember { mutableStateOf<TripDoc?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val commentVm = remember { CommentViewModel() }
    val comments by commentVm.comments.collectAsState()
    val currentUser = auth.currentUser
    
    // Load trip data from Firestore
    LaunchedEffect(postId) {
        db.collection("trips").document(postId).get()
            .addOnSuccessListener { doc ->
                try {
                    tripData = doc.toObject(TripDoc::class.java)
                    isLoading = false
                    commentVm.loadCommentsForTrip(postId)
                } catch (e: Exception) {
                    errorMessage = "Error loading post: ${e.message}"
                    isLoading = false
                }
            }
            .addOnFailureListener { e ->
                errorMessage = "Error: ${e.message}"
                isLoading = false
            }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(errorMessage!!, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onBack) { Text("Go Back") }
                    }
                }
            }
            tripData != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Trip title and basic info
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Trip to ${tripData?.destino}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Author: ${tripData?.authorEmail}")
                                Text("Duration: ${tripData?.diasRecomendados} days")
                                Text("Budget: €${tripData?.presupuestoTotal}")
                            }
                        }
                    }
                    
                    item {
                        // Itinerary
                        if (tripData?.itinerario?.isNotEmpty() == true) {
                            Text(
                                text = "Itinerary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                tripData?.itinerario?.forEach { day ->
                                    Text(day, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    
                    item {
                        // Comments section
                        CommentSection(
                            tripId = postId,
                            comments = comments,
                            isLoading = commentVm.isLoading.value,
                            onAddComment = { text ->
                                if (currentUser != null) {
                                    commentVm.addComment(text)
                                }
                            },
                            onDeleteComment = { commentId ->
                                commentVm.deleteComment(commentId)
                            },
                            currentUserId = currentUser?.uid,
                            isAdmin = false
                        )
                    }
                    
                    item {
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
