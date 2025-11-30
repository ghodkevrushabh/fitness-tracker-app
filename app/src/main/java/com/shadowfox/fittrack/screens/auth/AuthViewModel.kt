package com.shadowfox.fittrack.screens.auth

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.shadowfox.fittrack.data.UserDataStore
import com.shadowfox.fittrack.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val application: Application) : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun validateInputs(username: String, email: String, phone: String, pass: String, confirmPass: String): String? {
        if (username.isBlank()) return "Please enter a username"
        if (email.isBlank()) return "Please enter an email"
        if (phone.isBlank()) return "Please enter a phone number"
        if (pass.length < 6) return "Password must be at least 6 characters"
        if (pass != confirmPass) return "Passwords do not match"
        return null
    }

    fun signIn(email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Please enter email and password"
            return
        }

        _isLoading.value = true
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _currentUser.value = auth.currentUser
                    onSuccess()
                } else {
                    _errorMessage.value = task.exception?.localizedMessage ?: "Login failed"
                }
            }
    }

    fun signInWithGoogle(credential: AuthCredential, onSuccess: () -> Unit) {
        _isLoading.value = true
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _currentUser.value = auth.currentUser
                    onSuccess()
                } else {
                    _errorMessage.value = task.exception?.localizedMessage ?: "Google Sign-In failed"
                }
            }
    }

    fun signUp(username: String, email: String, phone: String, pass: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    viewModelScope.launch {
                        val profile = UserProfile(
                            name = username,
                            age = "25",
                            height = "170",
                            startWeight = "70",
                            goalWeight = "65",
                            profilePicUri = ""
                        )
                        UserDataStore.saveProfile(application, profile)

                        _isLoading.value = false
                        _currentUser.value = auth.currentUser
                        onSuccess()
                    }
                } else {
                    _isLoading.value = false
                    _errorMessage.value = task.exception?.localizedMessage ?: "Sign up failed"
                }
            }
    }

    fun clearError() { _errorMessage.value = null }

    // FIX: This updated Factory block removes the unchecked cast warning
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}