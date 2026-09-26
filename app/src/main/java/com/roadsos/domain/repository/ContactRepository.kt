package com.roadsos.domain.repository

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.roadsos.domain.model.EmergencyContact
import com.roadsos.domain.model.HospitalContact
import kotlinx.coroutines.tasks.await

class ContactRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private fun getUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun getContacts(): List<EmergencyContact> {
        val userId = getUserId() ?: return emptyList()
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("emergencyContacts")
                .get()
                .await()
            snapshot.toObjects(EmergencyContact::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addContact(contact: EmergencyContact) {
        val userId = getUserId() ?: return
        val ref = db.collection("users")
            .document(userId)
            .collection("emergencyContacts")
            .document()
        
        val newContact = contact.copy(contactId = ref.id)
        ref.set(newContact).await()
    }

    suspend fun updateContact(contact: EmergencyContact) {
        val userId = getUserId() ?: return
        db.collection("users")
            .document(userId)
            .collection("emergencyContacts")
            .document(contact.contactId)
            .set(contact)
            .await()
    }

    suspend fun deleteContact(contactId: String) {
        val userId = getUserId() ?: return
        db.collection("users")
            .document(userId)
            .collection("emergencyContacts")
            .document(contactId)
            .delete()
            .await()
    }
    
    suspend fun getVerifiedHospitalContacts(): List<HospitalContact> {
        val userId = getUserId() ?: return emptyList()
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("hospitalEmergencyContacts")
                .whereEqualTo("enabled", true)
                .whereEqualTo("verified", true)
                .get()
                .await()
            snapshot.toObjects(com.roadsos.domain.model.HospitalContact::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
