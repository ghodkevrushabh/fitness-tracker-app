package com.shadowfox.fittrack.screens.weightscreen

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowfox.fittrack.data.WeightDataStore
import com.shadowfox.fittrack.data.WeightEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeightViewModel(private val application: Application) : ViewModel() {

    private val _weights = MutableStateFlow<List<WeightEntry>>(emptyList())
    val weights: StateFlow<List<WeightEntry>> = _weights.asStateFlow()

    init {
        loadWeights()
    }

    private fun loadWeights() {
        viewModelScope.launch {
            _weights.value = WeightDataStore.getWeights(application)
        }
    }

    fun saveWeight(weight: Float) {
        viewModelScope.launch {
            WeightDataStore.addWeight(application, weight)
            loadWeights() // Refresh list
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WeightViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WeightViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}