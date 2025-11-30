package com.shadowfox.fittrack.screens.profile

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowfox.fittrack.data.Achievement
import com.shadowfox.fittrack.data.AchievementRepository
import com.shadowfox.fittrack.data.FirestoreHelper // IMPORT ADDED
import com.shadowfox.fittrack.data.StepDataStore
import com.shadowfox.fittrack.data.UserDataStore
import com.shadowfox.fittrack.data.UserProfile
import com.shadowfox.fittrack.data.WaterDataStore
import com.shadowfox.fittrack.data.WeightDataStore
import com.shadowfox.fittrack.service.StepCounterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileViewModel(private val application: Application) : ViewModel() {

    private val _totalSteps = MutableStateFlow(0)
    val totalSteps: StateFlow<Int> = _totalSteps.asStateFlow()

    private val _currentWeight = MutableStateFlow(0f)
    val currentWeight: StateFlow<Float> = _currentWeight.asStateFlow()

    private val _waterIntake = MutableStateFlow(0)
    val waterIntake: StateFlow<Int> = _waterIntake.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile("Guest", "", "", "", "", ""))
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _userBMI = MutableStateFlow(0f)
    val userBMI: StateFlow<Float> = _userBMI.asStateFlow()
    private val _bmiCategory = MutableStateFlow("Unknown")
    val bmiCategory: StateFlow<String> = _bmiCategory.asStateFlow()
    private val _userBMR = MutableStateFlow(0)
    val userBMR: StateFlow<Int> = _userBMR.asStateFlow()

    private val _earnedBadges = MutableStateFlow<List<Achievement>>(emptyList())
    val earnedBadges: StateFlow<List<Achievement>> = _earnedBadges.asStateFlow()

    val userName = "ShadowFox Intern"
    val joinDate = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())

    init {
        refreshProfileData()
    }

    fun refreshProfileData() {
        viewModelScope.launch {
            val steps = StepCounterService.getSteps()
            val water = WaterDataStore.getIntake(application)
            val weights = WeightDataStore.getWeights(application)
            val profile = UserDataStore.getProfile(application)

            _totalSteps.value = steps
            _waterIntake.value = water
            val latestWeight = if (weights.isNotEmpty()) weights.last().weight else profile.startWeight.toFloatOrNull() ?: 0f
            _currentWeight.value = latestWeight
            _userProfile.value = profile

            calculateHealthMetrics(latestWeight, profile.height, profile.age)

            val (unlocked, _) = AchievementRepository.getAchievementStatus(
                steps, water, weights.size
            )
            _earnedBadges.value = unlocked
        }
    }

    private fun calculateHealthMetrics(weightKg: Float, heightCmStr: String, ageStr: String) {
        val heightCm = heightCmStr.toFloatOrNull() ?: 0f
        val age = ageStr.toIntOrNull() ?: 25

        if (weightKg > 0 && heightCm > 0) {
            val heightM = heightCm / 100
            val bmi = weightKg / (heightM * heightM)
            _userBMI.value = String.format(Locale.US, "%.1f", bmi).toFloat()

            _bmiCategory.value = when {
                bmi < 18.5 -> "Underweight"
                bmi < 25 -> "Normal Weight"
                bmi < 30 -> "Overweight"
                else -> "Obese"
            }
            val bmr = (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5
            _userBMR.value = bmr.toInt()
        }
    }

    fun saveUserProfile(name: String, age: String, height: String, startW: String, goalW: String, picUri: String) {
        viewModelScope.launch {
            // 1. Permission Logic
            if (picUri.isNotEmpty()) {
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    application.contentResolver.takePersistableUriPermission(Uri.parse(picUri), takeFlags)
                } catch (e: Exception) { }
            }

            val newProfile = UserProfile(name, age, height, startW, goalW, picUri)

            // 2. Save to Local Phone (Instant)
            UserDataStore.saveProfile(application, newProfile)

            // 3. Save to Cloud (Backup) - CRITICAL LINE ADDED
            FirestoreHelper.saveProfile(newProfile)

            _userProfile.value = newProfile
            refreshProfileData()
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}