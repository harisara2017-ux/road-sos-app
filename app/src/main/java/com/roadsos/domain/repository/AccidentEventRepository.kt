package com.roadsos.domain.repository

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.roadsos.domain.model.AccidentEvent
import com.roadsos.domain.model.AccidentReport
import kotlinx.coroutines.tasks.await

class AccidentEventRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private fun getUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    suspend fun saveAccidentEvent(event: AccidentEvent): AccidentEvent {
        val uid = getUserId()
        val ref = db.collection("accidents").document()
        val eventWithId = event.copy(eventId = ref.id, userId = uid)
        ref.set(eventWithId).await()
        return eventWithId
    }

    suspend fun saveAccidentReport(report: AccidentReport): AccidentReport {
        val uid = getUserId()
        val ref = db.collection("accidentReports").document()
        val userEmail = auth.currentUser?.email ?: ""
        val reportWithId = report.copy(
            reportId = ref.id,
            userId = uid,
            userEmail = userEmail
        )
        ref.set(reportWithId).await()
        return reportWithId
    }

    suspend fun getUserAccidentReports(): List<AccidentReport> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = db.collection("accidentReports")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            snapshot.toObjects(AccidentReport::class.java).sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateNotificationStatus(
        eventId: String, 
        contactStatus: String? = null, 
        hospitalStatus: String? = null
    ) {
        val updates = mutableMapOf<String, Any>()
        if (contactStatus != null) {
            updates["emergencyContactNotificationStatus"] = contactStatus
        }
        if (hospitalStatus != null) {
            updates["hospitalNotificationStatus"] = hospitalStatus
        }
        if (updates.isNotEmpty()) {
            try {
                db.collection("accidents").document(eventId).update(updates).await()
            } catch (_: Exception) {}
        }
    }

    suspend fun updateReportStatus(
        reportId: String,
        status: String
    ) {
        try {
            db.collection("accidentReports").document(reportId).update("reportStatus", status).await()
        } catch (_: Exception) {}
    }
}
