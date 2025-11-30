package com.shadowfox.fittrack

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // This state will be used to keep the splash screen on
    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    init {
        // Simulate loading data (e.g., checking auth, loading user prefs)
        viewModelScope.launch {
            delay(1500) // Keep splash screen for 1.5 seconds
            _isLoading.value = false
        }
    }
}