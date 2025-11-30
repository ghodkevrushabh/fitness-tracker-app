package com.shadowfox.fittrack.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirestoreHelper {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val userId: String?
        get() = auth.currentUser?.uid

    // --- SAVE functions (Call on Events) ---

    suspend fun saveProfile(profile: UserProfile) {
        userId?.let { uid ->
            try {
                db.collection("users").document(uid)
                    .set(profile, SetOptions.merge())
            } catch (e: Exception) {
                Log.e("Firestore", "Error saving profile", e)
            }
        }
    }

    // THIS IS THE FUNCTION THAT WAS MISSING OR NAMED WRONG
    suspend fun saveSteps(steps: Int) {
        userId?.let { uid ->
            val data = hashMapOf(
                "dailySteps" to steps,
                "timestamp" to System.currentTimeMillis(),
                "date" to java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            )
            try {
                // Save to a history collection so we don't overwrite previous days
                db.collection("users").document(uid)
                    .collection("step_history").document(data["date"].toString())
                    .set(data, SetOptions.merge())
            } catch (e: Exception) {
                Log.e("Firestore", "Error saving steps", e)
            }
        }
    }

    // --- LOAD function (Call on Login/Start) ---
    // Downloads cloud data and saves it to Local DataStore
    suspend fun restoreDataFromCloud(context: Context) {
        userId?.let { uid ->
            try {
                val document = db.collection("users").document(uid).get().await()
                // Use the class safely
                val profile = document.toObject(UserProfile::class.java)

                if (profile != null) {
                    // Save cloud data to local phone storage
                    UserDataStore.saveProfile(context, profile)
                }
            } catch (e: Exception) {
                Log.e("Firestore", "Error restoring data", e)
            }
        }
    }
}