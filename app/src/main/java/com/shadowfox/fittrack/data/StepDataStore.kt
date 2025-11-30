package com.shadowfox.fittrack.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

// DataStore for daily step count persistence
private val Context.dataStore by preferencesDataStore("step_counter_prefs")

object StepDataStore {
    private val STEP_COUNT_KEY = intPreferencesKey("current_step_count")
    private val LAST_RESET_DAY_KEY = intPreferencesKey("last_reset_day")
    private val TODAY_STEP_GOAL_KEY = intPreferencesKey("today_step_goal")

    // --- Persistence Functions ---

    suspend fun saveSteps(context: Context, steps: Int) {
        context.dataStore.edit { preferences ->
            preferences[STEP_COUNT_KEY] = steps
        }
    }

    suspend fun getSteps(context: Context): Int {
        return context.dataStore.data.map { preferences ->
            preferences[STEP_COUNT_KEY] ?: 0
        }.first()
    }

    // --- Goal Functions ---

    suspend fun saveGoal(context: Context, goal: Int) {
        context.dataStore.edit { preferences ->
            preferences[TODAY_STEP_GOAL_KEY] = goal
        }
    }

    suspend fun getGoal(context: Context): Int {
        return context.dataStore.data.map { preferences ->
            preferences[TODAY_STEP_GOAL_KEY] ?: 10000 // Default goal is 10,000 steps
        }.first()
    }

    // --- Daily Reset Logic ---

    /**
     * Checks if a new day has started since the last stored day.
     * If so, resets the step count and updates the last reset day.
     * Returns the adjusted initial offset based on the reset.
     */
    suspend fun checkAndResetDaily(context: Context, currentSensorValue: Int): Int {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        return context.dataStore.data.map { preferences ->
            val lastResetDay = preferences[LAST_RESET_DAY_KEY] ?: today
            val storedSteps = preferences[STEP_COUNT_KEY] ?: 0

            if (lastResetDay != today) {
                // New day detected: Reset steps and update date
                context.dataStore.edit { editPreferences ->
                    editPreferences[LAST_RESET_DAY_KEY] = today
                    editPreferences[STEP_COUNT_KEY] = 0 // Clear today's display count
                }
                // Since the sensor keeps counting, the starting point for TODAY'S tracking
                // is the current raw sensor value.
                return@map currentSensorValue
            } else {
                // Same day: Calculate the necessary initial offset
                // This is the raw sensor value minus the steps we already recorded today.
                return@map currentSensorValue - storedSteps
            }
        }.first()
    }
}