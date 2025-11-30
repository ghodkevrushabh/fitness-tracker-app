package com.shadowfox.fittrack.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userDataStore by preferencesDataStore("user_profile_prefs")

object UserDataStore {
    private val NAME_KEY = stringPreferencesKey("user_name")
    private val AGE_KEY = stringPreferencesKey("user_age")
    private val HEIGHT_KEY = stringPreferencesKey("user_height")
    private val START_WEIGHT_KEY = stringPreferencesKey("user_start_weight")
    private val GOAL_WEIGHT_KEY = stringPreferencesKey("user_goal_weight")
    private val PROFILE_PIC_KEY = stringPreferencesKey("user_profile_pic_uri")

    suspend fun saveProfile(context: Context, profile: UserProfile) {
        context.userDataStore.edit { prefs ->
            prefs[NAME_KEY] = profile.name
            prefs[AGE_KEY] = profile.age
            prefs[HEIGHT_KEY] = profile.height
            prefs[START_WEIGHT_KEY] = profile.startWeight
            prefs[GOAL_WEIGHT_KEY] = profile.goalWeight
            prefs[PROFILE_PIC_KEY] = profile.profilePicUri
        }
    }

    suspend fun getProfile(context: Context): UserProfile {
        return context.userDataStore.data.map { prefs ->
            UserProfile(
                name = prefs[NAME_KEY] ?: "Guest User",
                age = prefs[AGE_KEY] ?: "25",
                height = prefs[HEIGHT_KEY] ?: "170",
                startWeight = prefs[START_WEIGHT_KEY] ?: "70",
                goalWeight = prefs[GOAL_WEIGHT_KEY] ?: "65",
                profilePicUri = prefs[PROFILE_PIC_KEY] ?: ""
            )
        }.first()
    }
}