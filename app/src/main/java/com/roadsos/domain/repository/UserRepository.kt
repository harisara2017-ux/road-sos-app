package com.roadsos.domain.repository

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

class UserRepository {
    private val db = Firebase.firestore

    suspend fun createUserDocument(uid: String, name: String, email: String) {
        val userProfile = UserProfile(uid = uid, name = name, email = email)
        db.collection("users").document(uid).set(userProfile).await()
    }
}
