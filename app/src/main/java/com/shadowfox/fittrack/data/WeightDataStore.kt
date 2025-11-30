package com.shadowfox.fittrack.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.weightDataStore by preferencesDataStore("weight_tracker_prefs")

data class WeightEntry(val date: String, val weight: Float)

object WeightDataStore {
    private val WEIGHT_HISTORY_KEY = stringPreferencesKey("weight_history_json")

    // Save a new weight entry
    suspend fun addWeight(context: Context, weight: Float) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentList = getWeights(context).toMutableList()

        // Remove existing entry for today if it exists (overwrite)
        currentList.removeAll { it.date == today }
        currentList.add(WeightEntry(today, weight))

        // Convert to JSON string to save
        val jsonArray = JSONArray()
        currentList.forEach { entry ->
            val obj = JSONObject()
            obj.put("date", entry.date)
            obj.put("weight", entry.weight.toDouble())
            jsonArray.put(obj)
        }

        context.weightDataStore.edit { preferences ->
            preferences[WEIGHT_HISTORY_KEY] = jsonArray.toString()
        }
    }

    // Get all weight entries
    suspend fun getWeights(context: Context): List<WeightEntry> {
        val jsonString = context.weightDataStore.data.map { preferences ->
            preferences[WEIGHT_HISTORY_KEY] ?: "[]"
        }.first()

        val list = mutableListOf<WeightEntry>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(WeightEntry(obj.getString("date"), obj.getDouble("weight").toFloat()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Sort by date
        return list.sortedBy { it.date }
    }
}