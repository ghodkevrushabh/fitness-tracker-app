package com.shadowfox.fittrack.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

// DataStore for daily water intake persistence
private val Context.waterDataStore by preferencesDataStore("water_tracker_prefs")

object WaterDataStore {
    private val INTAKE_ML_KEY = intPreferencesKey("current_water_intake_ml")
    private val GOAL_ML_KEY = intPreferencesKey("water_goal_ml")
    private val LAST_RESET_DAY_KEY = longPreferencesKey("water_last_reset_day_ms")

    private val INTERVAL_HOURS_KEY = intPreferencesKey("reminder_interval_hours") // NEW KEY
    // Default goal (2000 ml)
    const val DEFAULT_GOAL = 2000

    suspend fun getIntake(context: Context): Int {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        return context.waterDataStore.data.map { preferences ->
            val lastResetMs = preferences[LAST_RESET_DAY_KEY] ?: 0L
            val lastResetCal = Calendar.getInstance().apply { timeInMillis = lastResetMs }

            // Check if it's a new day (simple check)
            if (lastResetCal.get(Calendar.DAY_OF_YEAR) != today || lastResetMs == 0L) {
                // New day: reset intake to 0 and update reset day
                context.waterDataStore.edit { it[LAST_RESET_DAY_KEY] = System.currentTimeMillis() }
                return@map 0
            } else {
                return@map preferences[INTAKE_ML_KEY] ?: 0
            }
        }.first()
    }

    suspend fun updateIntake(context: Context, amountMl: Int) {
        // First check for reset, then update
        getIntake(context)
        context.waterDataStore.edit { preferences ->
            preferences[INTAKE_ML_KEY] = amountMl
        }
    }

    suspend fun getGoal(context: Context): Int {
        return context.waterDataStore.data.map { preferences ->
            preferences[GOAL_ML_KEY] ?: DEFAULT_GOAL
        }.first()
    }

    suspend fun saveGoal(context: Context, goal: Int) {
        context.waterDataStore.edit { preferences ->
            preferences[GOAL_ML_KEY] = goal
        }
    }

    suspend fun getInterval(context: Context): Int {
        return context.waterDataStore.data.map { preferences ->
            preferences[INTERVAL_HOURS_KEY] ?: 0
        }.first()
    }

    suspend fun saveInterval(context: Context, hours: Int) {
        context.waterDataStore.edit { preferences ->
            preferences[INTERVAL_HOURS_KEY] = hours
        }
    }
}