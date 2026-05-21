package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.domain.model.Report
import com.paulaizurrategui.urtriply.ui.theme.UrOrange
import com.paulaizurrategui.urtriply.ui.viewmodels.AdminViewModel

@Composable
fun AdminModerateScreen(
    viewModel: AdminViewModel = viewModel()
) {
    val reports by viewModel.reports.collectAsState()
    val isLoading = viewModel.isLoading.value
    val isAdmin = viewModel.isAdmin.value
    val errorMessage = viewModel.errorMessage.value
    val successMessage = viewModel.successMessage.value

    if (!isAdmin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.admin_access_message),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = stringResource(R.string.admin_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = stringResource(R.string.admin_pending_reports, reports.size),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Error message
        if (errorMessage.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp
                )
            }
        }

        // Success message
        if (successMessage.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = successMessage,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp
                )
            }
        }

        // Reports list
        if (reports.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.admin_no_reports),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reports) { report ->
                    ReportCard(
                        report = report,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun ReportCard(
    report: Report,
    viewModel: AdminViewModel
) {
    var showResolutionDialog by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf<String?>(null) }
    var resolutionText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Report header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.admin_report_label, report.id.take(8)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = stringResource(
                            R.string.admin_report_type,
                            if (report.targetType == "TRIP") stringResource(R.string.admin_type_trip) else stringResource(R.string.admin_type_comment)
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = UrOrange,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.admin_status_open),
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Report details
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.admin_reported_by, report.reporterName),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = stringResource(R.string.admin_reason, report.reason),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (report.description.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.admin_description, report.description),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.admin_target_id, report.targetId.take(12)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        selectedAction = "delete"
                        showResolutionDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    Text(stringResource(R.string.admin_delete), fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        selectedAction = "resolve"
                        showResolutionDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UrOrange
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    Text(stringResource(R.string.admin_resolve), fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        selectedAction = "block"
                        showResolutionDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    Text(stringResource(R.string.admin_block), fontSize = 12.sp)
                }
            }
        }
    }

    // Resolution dialog (customized per action)
    if (showResolutionDialog) {
        when (selectedAction) {
            "delete" -> {
                AlertDialog(
                    onDismissRequest = { showResolutionDialog = false },
                    title = { Text(stringResource(R.string.admin_delete_confirm_title)) },
                    text = {
                        Column {
                            Text(
                                text = stringResource(R.string.admin_delete_confirm_body),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteReportedContentAndResolve(report)
                                showResolutionDialog = false
                                resolutionText = ""
                                selectedAction = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.admin_delete_confirm_button))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResolutionDialog = false }) {
                            Text(stringResource(R.string.admin_cancel))
                        }
                    }
                )
            }
            else -> {
                AlertDialog(
                    onDismissRequest = { showResolutionDialog = false },
                    title = { Text(stringResource(R.string.admin_resolve_report_title)) },
                    text = {
                        Column {
                            Text(
                                text = stringResource(R.string.admin_action_question),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            OutlinedTextField(
                                value = resolutionText,
                                onValueChange = { resolutionText = it },
                                label = { Text(stringResource(R.string.admin_notes_optional)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp),
                                maxLines = 3
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                when (selectedAction) {
                                    "resolve" -> {
                                        viewModel.resolveReport(report.id, resolutionText.ifEmpty { "No action needed" })
                                    }
                                    "block" -> {
                                        viewModel.blockReportedUserAndResolve(report)
                                    }
                                }
                                showResolutionDialog = false
                                resolutionText = ""
                                selectedAction = null
                            }
                        ) {
                            Text(stringResource(R.string.admin_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResolutionDialog = false }) {
                            Text(stringResource(R.string.admin_cancel))
                        }
                    }
                )
            }
        }
    }
}
