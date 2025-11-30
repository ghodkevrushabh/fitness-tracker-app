package com.shadowfox.fittrack.screens.achievements

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowfox.fittrack.data.Achievement
import com.shadowfox.fittrack.data.AchievementRepository
import com.shadowfox.fittrack.data.StepDataStore
import com.shadowfox.fittrack.data.WaterDataStore
import com.shadowfox.fittrack.data.WeightDataStore
import com.shadowfox.fittrack.service.StepCounterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AchievementsViewModel(private val application: Application) : ViewModel() {

    private val _unlockedBadges = MutableStateFlow<List<Achievement>>(emptyList())
    val unlockedBadges: StateFlow<List<Achievement>> = _unlockedBadges.asStateFlow()

    private val _nextBadges = MutableStateFlow<List<Achievement>>(emptyList())
    val nextBadges: StateFlow<List<Achievement>> = _nextBadges.asStateFlow()

    init {
        checkAchievements()
    }

    fun checkAchievements() {
        viewModelScope.launch {
            val currentSteps = StepCounterService.getSteps()
            val currentWater = WaterDataStore.getIntake(application)
            val weightEntries = WeightDataStore.getWeights(application).size

            // Destructuring the Pair explicitly
            val resultPair = AchievementRepository.getAchievementStatus(
                currentSteps,
                currentWater,
                weightEntries
            )

            _unlockedBadges.value = resultPair.first
            _nextBadges.value = resultPair.second
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AchievementsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AchievementsViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}