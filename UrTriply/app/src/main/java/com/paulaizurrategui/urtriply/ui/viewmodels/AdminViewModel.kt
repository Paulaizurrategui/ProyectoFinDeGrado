package com.paulaizurrategui.urtriply.ui.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paulaizurrategui.urtriply.data.reports.ReportRepository
import com.paulaizurrategui.urtriply.domain.model.Report
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AdminViewModel : ViewModel() {

    private val reportRepo = ReportRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // State for reports
    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports

    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf("")
    val successMessage = mutableStateOf("")
    val isAdmin = mutableStateOf(false)

    init {
        checkAdminStatus()
    }

    // Check if current user is admin
    private fun checkAdminStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            isAdmin.value = false
            return
        }

        db.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                val admin = doc.getBoolean("esAdmin") ?: false
                isAdmin.value = admin
                if (admin) {
                    // only load reports if the user is admin
                    loadOpenReports()
                }
            }
            .addOnFailureListener {
                isAdmin.value = false
            }
    }

    // Load all open reports
    fun loadOpenReports() {
        isLoading.value = true

        reportRepo.getOpenReports(
            onSuccess = { reports ->
                _reports.value = reports
                isLoading.value = false
            },
            onError = { e ->
                errorMessage.value = e.message ?: "Error loading reports"
                isLoading.value = false
            }
        )
    }

    // Resolve a report
    fun resolveReport(reportId: String, resolution: String) {
        val adminUid = auth.currentUser?.uid
        if (adminUid == null) {
            errorMessage.value = "Not authenticated"
            return
        }

        isLoading.value = true

        reportRepo.resolveReport(
            reportId = reportId,
            adminUid = adminUid,
            resolution = resolution,
            onSuccess = {
                successMessage.value = "Report resolved"
                loadOpenReports()
                clearMessages()
            },
            onError = { e ->
                errorMessage.value = e.message ?: "Error resolving report"
                isLoading.value = false
            }
        )
    }

    // Dismiss a report
    fun dismissReport(reportId: String, reason: String) {
        val adminUid = auth.currentUser?.uid
        if (adminUid == null) {
            errorMessage.value = "Not authenticated"
            return
        }

        isLoading.value = true

        reportRepo.dismissReport(
            reportId = reportId,
            adminUid = adminUid,
            reason = reason,
            onSuccess = {
                successMessage.value = "Report dismissed"
                loadOpenReports()
                clearMessages()
            },
            onError = { e ->
                errorMessage.value = e.message ?: "Error dismissing report"
                isLoading.value = false
            }
        )
    }

    // Delete trip (after report resolution)
    fun deleteTripByAdmin(tripId: String) {
        val adminUid = auth.currentUser?.uid
        if (adminUid == null) {
            errorMessage.value = "Not authenticated"
            return
        }

        isLoading.value = true

        reportRepo.deleteTripByAdmin(
            tripId = tripId,
            adminUid = adminUid,
            onSuccess = {
                successMessage.value = "Trip deleted"
                isLoading.value = false
                clearMessages()
            },
            onError = { e ->
                errorMessage.value = e.message ?: "Error deleting trip"
                isLoading.value = false
            }
        )
    }

    // Delete comment (after report resolution)
    fun deleteCommentByAdmin(tripId: String, commentId: String) {
        val adminUid = auth.currentUser?.uid
        if (adminUid == null) {
            errorMessage.value = "Not authenticated"
            return
        }

        isLoading.value = true

        reportRepo.deleteCommentByAdmin(
            tripId = tripId,
            commentId = commentId,
            adminUid = adminUid,
            onSuccess = {
                successMessage.value = "Comment deleted"
                isLoading.value = false
                clearMessages()
            },
            onError = { e ->
                errorMessage.value = e.message ?: "Error deleting comment"
                isLoading.value = false
            }
        )
    }

    // Block user
    fun blockUser(userToBlockUid: String, reason: String) {
        val adminUid = auth.currentUser?.uid
        if (adminUid == null) {
            errorMessage.value = "Not authenticated"
            return
        }

        isLoading.value = true

        reportRepo.blockUser(
            adminUid = adminUid,
            userToBlockUid = userToBlockUid,
            reason = reason,
            onSuccess = {
                successMessage.value = "User blocked"
                isLoading.value = false
                clearMessages()
            },
            onError = { e ->
                errorMessage.value = e.message ?: "Error blocking user"
                isLoading.value = false
            }
        )
    }

    fun deleteReportedContentAndResolve(report: Report, resolution: String = "Contenido eliminado") {
        val adminUid = auth.currentUser?.uid
        if (adminUid == null) {
            errorMessage.value = "Not authenticated"
            return
        }

        isLoading.value = true
        errorMessage.value = ""

        val onDeleteSuccess = {
            reportRepo.resolveReport(
                reportId = report.id,
                adminUid = adminUid,
                resolution = resolution,
                onSuccess = {
                    successMessage.value = "Contenido eliminado y reporte resuelto"
                    loadOpenReports()
                    isLoading.value = false
                },
                onError = { e ->
                    errorMessage.value = e.message ?: "Contenido eliminado, pero no se pudo resolver el reporte"
                    isLoading.value = false
                }
            )
        }

        if (report.targetType == "TRIP") {
            val tripId = report.tripId ?: report.targetId
            if (tripId.isBlank()) {
                errorMessage.value = "No hay datos suficientes para borrar el viaje"
                isLoading.value = false
                return
            }

            reportRepo.deleteTripByAdmin(
                tripId = tripId,
                adminUid = adminUid,
                onSuccess = onDeleteSuccess,
                onError = { e ->
                    errorMessage.value = e.message ?: "Error deleting trip"
                    isLoading.value = false
                }
            )
        } else {
            val tripId = report.tripId ?: ""
            val commentId = report.commentId ?: report.targetId
            if (tripId.isBlank() || commentId.isBlank()) {
                errorMessage.value = "No hay datos suficientes para borrar el comentario"
                isLoading.value = false
                return
            }

            reportRepo.deleteCommentByAdmin(
                tripId = tripId,
                commentId = commentId,
                adminUid = adminUid,
                onSuccess = onDeleteSuccess,
                onError = { e ->
                    errorMessage.value = e.message ?: "Error deleting comment"
                    isLoading.value = false
                }
            )
        }
    }

    fun blockReportedUserAndResolve(report: Report) {
        val adminUid = auth.currentUser?.uid
        val targetUid = report.targetUserUid

        if (adminUid == null) {
            errorMessage.value = "Not authenticated"
            return
        }
        if (targetUid.isNullOrBlank()) {
            errorMessage.value = "No hay usuario asociado al reporte"
            return
        }

        isLoading.value = true
        errorMessage.value = ""

        reportRepo.blockUser(
            adminUid = adminUid,
            userToBlockUid = targetUid,
            reason = "Reporte: ${report.reason}",
            onSuccess = {
                reportRepo.resolveReport(
                    reportId = report.id,
                    adminUid = adminUid,
                    resolution = "Usuario bloqueado",
                    onSuccess = {
                        successMessage.value = "Usuario bloqueado y reporte resuelto"
                        loadOpenReports()
                        isLoading.value = false
                    },
                    onError = { e ->
                        errorMessage.value = e.message ?: "Usuario bloqueado, pero no se pudo resolver el reporte"
                        isLoading.value = false
                    }
                )
            },
            onError = { e ->
                errorMessage.value = e.message ?: "Error blocking user"
                isLoading.value = false
            }
        )
    }

    // Unblock user
    fun unblockUser(userToUnblockUid: String) {
        val adminUid = auth.currentUser?.uid
        if (adminUid == null) {
            errorMessage.value = "Not authenticated"
            return
        }

        isLoading.value = true

        reportRepo.unblockUser(
            adminUid = adminUid,
            userToUnblockUid = userToUnblockUid,
            onSuccess = {
                successMessage.value = "User unblocked"
                isLoading.value = false
                clearMessages()
            },
            onError = { e ->
                errorMessage.value = e.message ?: "Error unblocking user"
                isLoading.value = false
            }
        )
    }

    fun clearMessages() {
        if (successMessage.value.isNotEmpty()) {
            successMessage.value = ""
        }
        if (errorMessage.value.isNotEmpty()) {
            errorMessage.value = ""
        }
    }
}
