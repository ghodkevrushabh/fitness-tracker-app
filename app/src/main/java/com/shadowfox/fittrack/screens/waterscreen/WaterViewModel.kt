package com.shadowfox.fittrack.screens.waterscreen

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowfox.fittrack.data.WaterDataStore
import com.shadowfox.fittrack.utility.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Stack

class WaterViewModel(private val application: Application) : ViewModel() {

    private val _intake = MutableStateFlow(0)
    val intake: StateFlow<Int> = _intake.asStateFlow()

    private val _goal = MutableStateFlow(WaterDataStore.DEFAULT_GOAL)
    val goal: StateFlow<Int> = _goal.asStateFlow()

    private val _reminderInterval = MutableStateFlow(0)
    val reminderInterval: StateFlow<Int> = _reminderInterval.asStateFlow()

    // History stack for Undo functionality
    private val intakeHistory = Stack<Int>()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _intake.value = WaterDataStore.getIntake(application)
            _goal.value = WaterDataStore.getGoal(application)
            _reminderInterval.value = WaterDataStore.getInterval(application)
        }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            val newIntake = _intake.value + amountMl
            WaterDataStore.updateIntake(application, newIntake)
            _intake.value = newIntake

            // Push this action to history for undo
            intakeHistory.push(amountMl)
        }
    }

    // New Undo Function
    fun undoLastIntake() {
        if (intakeHistory.isNotEmpty()) {
            val lastAmount = intakeHistory.pop()
            viewModelScope.launch {
                // Prevent going below 0
                val newIntake = (_intake.value - lastAmount).coerceAtLeast(0)
                WaterDataStore.updateIntake(application, newIntake)
                _intake.value = newIntake
            }
        }
    }

    fun saveGoal(newGoal: Int) {
        viewModelScope.launch {
            WaterDataStore.saveGoal(application, newGoal)
            _goal.value = newGoal
        }
    }

    fun resetWater() {
        viewModelScope.launch {
            WaterDataStore.updateIntake(application, 0)
            _intake.value = 0
            intakeHistory.clear() // Clear history on reset
        }
    }

    fun setReminderInterval(intervalHours: Int) {
        viewModelScope.launch {
            WaterDataStore.saveInterval(application, intervalHours)
            _reminderInterval.value = intervalHours

            if (intervalHours > 0) {
                NotificationScheduler.scheduleWaterReminder(application, intervalHours)
            } else {
                NotificationScheduler.cancelWaterReminder(application)
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WaterViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WaterViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}