@file:Suppress("DEPRECATION")
package com.shadowfox.fittrack.screens.homescreen

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.shadowfox.fittrack.data.FirestoreHelper // IMPORT ADDED
import com.shadowfox.fittrack.data.StepDataStore
import com.shadowfox.fittrack.service.StepCounterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(private val application: Application) : ViewModel() {

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps.asStateFlow()

    private val _goal = MutableStateFlow(10000)
    val goal: StateFlow<Int> = _goal.asStateFlow()

    val isServiceRunning = mutableStateOf(false)

    init {
        // Poll for steps
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val stepsFromService = StepCounterService.getSteps()
                withContext(Dispatchers.Main) {
                    _steps.value = stepsFromService
                }
                delay(1000L)
            }
        }

        // Load data
        viewModelScope.launch {
            _steps.value = StepDataStore.getSteps(application)
            _goal.value = StepDataStore.getGoal(application)
            isServiceRunning.value = StepCounterService.serviceInstance != null
        }
    }

    // --- LOGOUT LOGIC ---
    fun logout(context: Context, onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            stopStepCounterService()
            FirebaseAuth.getInstance().signOut()
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                googleSignInClient.signOut().addOnCompleteListener {
                    onLogoutComplete()
                }
            } catch (e: Exception) {
                onLogoutComplete()
            }
        }
    }

    // --- RESET LOGIC (UPDATED) ---
    fun resetSteps() {
        viewModelScope.launch {
            // 1. Save to Cloud BEFORE resetting (CRITICAL CHANGE)
            FirestoreHelper.saveSteps(_steps.value)

            // 2. Reset Local Service
            val intent = Intent(application, StepCounterService::class.java)
            intent.action = "STOP_AND_RESET"
            application.startForegroundService(intent)
            _steps.value = 0
        }
    }

    fun startStepCounterService() {
        val intent = Intent(application, StepCounterService::class.java)
        if (!isServiceRunning.value) {
            application.startForegroundService(intent)
            isServiceRunning.value = true
        }
    }

    fun stopStepCounterService() {
        val intent = Intent(application, StepCounterService::class.java)
        application.stopService(intent)
        isServiceRunning.value = false
    }

    fun saveNewGoal(newGoal: Int) {
        viewModelScope.launch {
            StepDataStore.saveGoal(application, newGoal)
            _goal.value = newGoal
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}