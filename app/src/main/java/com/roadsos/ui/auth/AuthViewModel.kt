package com.roadsos.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.roadsos.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val userRepository = UserRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapAuthException(e))
            }
        }
    }

    fun register(name: String, email: String, pass: String, confirmPass: String) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("All fields must be filled")
            return
        }

        if (pass != confirmPass) {
            _authState.value = AuthState.Error("Passwords do not match")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                result.user?.let { user ->
                    // Create user document in Firestore
                    userRepository.createUserDocument(user.uid, name, email)
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(mapAuthException(e))
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    private fun mapAuthException(e: Exception): String {
        return when (e) {
            is FirebaseAuthException -> {
                when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "Invalid email address."
                    "ERROR_WRONG_PASSWORD" -> "Wrong password."
                    "ERROR_USER_NOT_FOUND" -> "No account found with this email."
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Email is already registered."
                    "ERROR_WEAK_PASSWORD" -> "Password is too weak."
                    "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your connection."
                    else -> e.localizedMessage ?: "Authentication failed."
                }
            }
            else -> e.localizedMessage ?: "An unknown error occurred."
        }
    }
}
