package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.domain.model.Comment
import com.paulaizurrategui.urtriply.data.trips.TripDoc
import com.paulaizurrategui.urtriply.ui.components.CommentSection
import com.paulaizurrategui.urtriply.ui.auth.CommunityViewModel
import com.paulaizurrategui.urtriply.ui.theme.UrOrange
import com.paulaizurrategui.urtriply.data.reports.ReportRepository
import com.paulaizurrategui.urtriply.ui.theme.UrSky
import com.paulaizurrategui.urtriply.ui.theme.UrSkySoft
import com.paulaizurrategui.urtriply.ui.viewmodels.CommentViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onBack: () -> Unit
) {
    val isCompactWidth = LocalConfiguration.current.screenWidthDp < 360
    val context = LocalContext.current
    val reportReasonSpam = stringResource(R.string.post_detail_report_reason_spam)
    val reportReasonInappropriate = stringResource(R.string.post_detail_report_reason_inappropriate)
    val reportReasonScam = stringResource(R.string.post_detail_report_reason_scam)
    val reportReasonOffensive = stringResource(R.string.post_detail_report_reason_offensive)
    val reportReasonOther = stringResource(R.string.post_detail_report_reason_other)
    val reportSuccessText = stringResource(R.string.post_detail_report_success)
    val reportErrorTemplate = stringResource(R.string.post_detail_report_error)
    val reportReasonRequiredText = stringResource(R.string.post_detail_report_reason_required)
    val blockSuccessText = stringResource(R.string.post_detail_block_success)
    val blockLoginRequiredText = stringResource(R.string.post_detail_block_login_required)
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var tripData by remember { mutableStateOf<TripDoc?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val commentVm = remember { CommentViewModel() }
    val comments by commentVm.comments.collectAsState()
    val currentUser = auth.currentUser
    val communityVm: CommunityViewModel = viewModel()
    
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var reportDescription by remember { mutableStateOf("") }
    var reportMessage by remember { mutableStateOf<String?>(null) }
    var showReportCommentDialog by remember { mutableStateOf(false) }
    var selectedCommentToReport by remember { mutableStateOf<Comment?>(null) }
    var commentReportReason by remember { mutableStateOf("") }
    var commentReportDescription by remember { mutableStateOf("") }
    var commentReportMessage by remember { mutableStateOf<String?>(null) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var blockTargetUid by remember { mutableStateOf<String?>(null) }
    var blockReason by remember { mutableStateOf("") }
    var blockMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(postId) {
        db.collection("trips").document(postId).get()
            .addOnSuccessListener { doc ->
                try {
                    val isDeleted = doc.getBoolean("deleted") ?: false
                    if (isDeleted) {
                        errorMessage = context.getString(R.string.community_post_unavailable)
                        tripData = null
                        isLoading = false
                    } else {
                        tripData = doc.toObject(TripDoc::class.java)
                        isLoading = false
                        commentVm.loadCommentsForTrip(postId)
                    }
                } catch (e: Exception) {
                    errorMessage = context.getString(R.string.community_load_trip_error, e.message ?: "")
                    isLoading = false
                }
            }
            .addOnFailureListener { e ->
                errorMessage = context.getString(R.string.community_generic_error, e.message ?: "")
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.post_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.post_detail_action_options))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.post_detail_report_trip)) },
                                onClick = {
                                    menuExpanded = false
                                    showReportDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.block_user)) },
                                onClick = {
                                    menuExpanded = false
                                    // target author UID
                                    blockTargetUid = tripData?.authorUid
                                    showBlockDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                    CircularProgressIndicator(color = UrOrange)
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedCard(
                        modifier = Modifier.padding(16.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                Text(text = errorMessage!!, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(16.dp))
                            androidx.compose.material3.Button(onClick = onBack) { Text(stringResource(R.string.back)) }
                        }
                    }
                }
            }

            tripData != null -> {
                val trip = tripData!!
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Brush.linearGradient(listOf(UrOrange, UrSky)))
                                    .padding(20.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.White.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.post_detail_published_friend),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Text(
                                        text = trip.destino.ifBlank { stringResource(R.string.post_detail_no_destination) },
                                        style = if (isCompactWidth) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )

                                    Text(
                                        text = trip.authorEmail?.takeIf { it.isNotBlank() }
                                            ?.let { stringResource(R.string.post_detail_published_by, it) }
                                            ?: stringResource(R.string.post_detail_published_community),
                                        color = Color.White.copy(alpha = 0.9f),
                                        style = if (isCompactWidth) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                                        maxLines = if (isCompactWidth) 3 else 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        DetailChip(Icons.Default.Euro, "€${trip.presupuestoTotal.toInt()}", isCompactWidth)
                                        DetailChip(Icons.Default.People, stringResource(R.string.community_travelers_format, trip.viajeros), isCompactWidth)
                                        DetailChip(Icons.Default.AccessTime, stringResource(R.string.community_days_format, trip.diasRecomendados), isCompactWidth)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = stringResource(R.string.post_detail_summary),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                SummaryRow(stringResource(R.string.post_detail_departure), trip.fechaInicioMillis?.let { dateFormat.format(Date(it)) } ?: stringResource(R.string.post_detail_no_date), Icons.Default.CalendarToday)
                                SummaryRow(stringResource(R.string.post_detail_return), trip.fechaFinMillis?.let { dateFormat.format(Date(it)) } ?: stringResource(R.string.post_detail_no_date), Icons.Default.CalendarToday)
                                SummaryRow(stringResource(R.string.post_detail_origin), stringResource(R.string.post_detail_origin_madrid), Icons.Default.LocationOn)
                                SummaryRow(stringResource(R.string.post_detail_status), trip.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, Icons.Default.AccessTime)
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = UrSkySoft),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = stringResource(R.string.post_detail_itinerary),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                if (trip.itineraryByDay != null && trip.itineraryByDay.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        trip.itineraryByDay.forEachIndexed { index, day ->
                                            OutlinedCard(
                                                shape = RoundedCornerShape(18.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = UrOrange.copy(alpha = 0.12f)
                                                        ) {
                                                            Text(
                                                                text = day.dayLabel ?: "${index + 1}",
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                                color = UrOrange,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }

                                                        Text(
                                                            text = day.summary ?: "",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }

                                                    if (day.activities != null && day.activities.isNotEmpty()) {
                                                        val ctx = LocalContext.current
                                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            day.activities.forEach { act ->
                                                                if (!act.bookingUrl.isNullOrBlank()) {
                                                                    TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(act.bookingUrl))) }) {
                                                                        Text(act.name ?: "")
                                                                    }
                                                                } else {
                                                                    Text(text = act.name ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (trip.itinerario.isNotEmpty()) {
                                    Text(
                                        text = stringResource(R.string.post_detail_no_itinerary),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = stringResource(R.string.post_detail_comments),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

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
                                    isAdmin = false,
                                    onReportComment = { comment ->
                                        selectedCommentToReport = comment
                                        showReportCommentDialog = true
                                    }
                                    ,onBlockUser = { authorUid ->
                                        blockTargetUid = authorUid
                                        showBlockDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showReportDialog = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Text(stringResource(R.string.post_detail_report_trip))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (reportMessage != null) {
                        Surface(
                            color = UrOrange.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = reportMessage!!,
                                modifier = Modifier.padding(12.dp),
                                color = UrOrange
                            )
                        }
                    }

                    Text(stringResource(R.string.post_detail_select_reason))
                    val reasons = listOf(reportReasonSpam, reportReasonInappropriate, reportReasonScam, reportReasonOther)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        reasons.forEach { reason ->
                            val isSelected = reportReason == reason
                            val borderColor by animateColorAsState(targetValue = if (isSelected) UrOrange else Color.Transparent, animationSpec = tween(220))
                            ElevatedButton(
                                onClick = { reportReason = reason },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (isSelected) Modifier.shadow(6.dp, RoundedCornerShape(12.dp)) else Modifier),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(2.dp, borderColor),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) UrOrange else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(reason)
                            }
                        }
                    }

                    androidx.compose.material3.OutlinedTextField(
                        value = reportDescription,
                        onValueChange = { reportDescription = it },
                        label = { Text(stringResource(R.string.post_detail_optional_details)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        if (reportReason.isNotEmpty() && currentUser != null && tripData != null) {
                            ReportRepository().submitReport(
                                targetType = "TRIP",
                                targetId = postId,
                                targetUserUid = tripData?.authorUid,
                                tripId = postId,
                                reporterUid = currentUser.uid,
                                    reporterName = currentUser.displayName ?: context.getString(R.string.post_detail_anonymous_user),
                                reason = reportReason,
                                description = reportDescription,
                                onSuccess = {
                                    reportMessage = reportSuccessText
                                    reportReason = ""
                                    reportDescription = ""
                                },
                                onError = { e ->
                                    reportMessage = context.getString(R.string.post_detail_report_error, e.message ?: "")
                                }
                            )
                        } else {
                            reportMessage = reportReasonRequiredText
                        }
                    }
                ) {
                    Text(stringResource(R.string.post_detail_report_submit))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showReportDialog = false }) {
                    Text(stringResource(R.string.post_detail_report_cancel))
                }
            }
        )
    }

    if (showReportCommentDialog && selectedCommentToReport != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showReportCommentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showReportCommentDialog = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Text(stringResource(R.string.post_detail_report_comment))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (commentReportMessage != null) {
                        Surface(
                            color = UrOrange.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = commentReportMessage!!,
                                modifier = Modifier.padding(12.dp),
                                color = UrOrange
                            )
                        }
                    }

                    Text(stringResource(R.string.post_detail_select_reason))
                    val reasons = listOf(reportReasonSpam, reportReasonInappropriate, reportReasonOffensive, reportReasonOther)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        reasons.forEach { reason ->
                            val isSelected = commentReportReason == reason
                            val borderColor by animateColorAsState(targetValue = if (isSelected) UrOrange else Color.Transparent, animationSpec = tween(220))
                            ElevatedButton(
                                onClick = { commentReportReason = reason },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (isSelected) Modifier.shadow(6.dp, RoundedCornerShape(12.dp)) else Modifier),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(2.dp, borderColor),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) UrOrange else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(reason)
                            }
                        }
                    }

                    androidx.compose.material3.OutlinedTextField(
                        value = commentReportDescription,
                        onValueChange = { commentReportDescription = it },
                        label = { Text(stringResource(R.string.post_detail_optional_details)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        val comment = selectedCommentToReport
                        if (commentReportReason.isNotEmpty() && currentUser != null && comment != null) {
                            ReportRepository().submitReport(
                                targetType = "COMMENT",
                                targetId = comment.id,
                                targetUserUid = comment.authorUid,
                                tripId = postId,
                                commentId = comment.id,
                                reporterUid = currentUser.uid,
                                    reporterName = currentUser.displayName ?: context.getString(R.string.post_detail_anonymous_user),
                                reason = commentReportReason,
                                description = commentReportDescription,
                                onSuccess = {
                                    commentReportMessage = reportSuccessText
                                    commentReportReason = ""
                                    commentReportDescription = ""
                                },
                                onError = { e ->
                                    commentReportMessage = context.getString(R.string.post_detail_report_error, e.message ?: "")
                                }
                            )
                        } else {
                            commentReportMessage = reportReasonRequiredText
                        }
                    }
                ) {
                    Text(stringResource(R.string.post_detail_report_submit))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showReportCommentDialog = false }) {
                    Text(stringResource(R.string.post_detail_report_cancel))
                }
            }
        )
    }

    // Block user dialog (used for post author or comment author)
    if (showBlockDialog && blockTargetUid != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showBlockDialog = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Text(stringResource(R.string.block_user))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (blockMessage != null) {
                        Surface(
                            color = UrOrange.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = blockMessage!!,
                                modifier = Modifier.padding(12.dp),
                                color = UrOrange
                            )
                        }
                    }

                    Text(stringResource(R.string.block_reason))

                    androidx.compose.material3.OutlinedTextField(
                        value = blockReason,
                        onValueChange = { blockReason = it },
                        label = { Text(stringResource(R.string.block_reason)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        val target = blockTargetUid
                        if (target != null && currentUser != null) {
                            communityVm.blockUser(target,
                                onSuccess = {
                                    blockMessage = blockSuccessText
                                    blockReason = ""
                                },
                                onError = { e -> blockMessage = context.getString(R.string.post_detail_report_error, e.message ?: "") }
                            )
                        } else {
                            blockMessage = blockLoginRequiredText
                        }
                    }
                ) {
                    Text(stringResource(R.string.block_user))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showBlockDialog = false }) {
                    Text(stringResource(R.string.post_detail_report_cancel))
                }
            }
        )
    }
}

@Composable
private fun DetailChip(icon: ImageVector, text: String, isCompactWidth: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.18f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text(
                text = text,
                color = Color.White,
                style = if (isCompactWidth) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = UrSkySoft
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = UrOrange.copy(alpha = 0.12f)
            ) {
                Icon(icon, contentDescription = null, tint = UrOrange, modifier = Modifier.padding(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
