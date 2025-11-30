package com.shadowfox.fittrack.screens.runscreen

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// This class will hold the state and logic for our Run screen
class RunViewModel(application: Application) : ViewModel() {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    // State Variables for UI
    val isTracking = mutableStateOf(false)
    val totalDistanceKm = mutableDoubleStateOf(0.0)
    val currentSpeedKmh = mutableDoubleStateOf(0.0)
    val timeElapsedMs = mutableStateOf(0L)
    val locationHistory = mutableStateListOf<LatLng>()
    val lastLocation = mutableStateOf<Location?>(null)

    // Tracking variables
    private var job: Job? = null
    private var startTime: Long = 0L
    private var lastLocationPoint: Location? = null

    // Default Camera (San Francisco as a safe default)
    val defaultCameraPosition = LatLng(37.7749, -122.4194)

    // Location Update Callback
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let { newLocation ->
                // 1. Update UI location
                lastLocation.value = newLocation

                if (isTracking.value) {
                    // 2. Add to route history
                    locationHistory.add(LatLng(newLocation.latitude, newLocation.longitude))

                    // 3. Calculate distance and speed
                    calculateStats(newLocation)
                }
            }
        }
    }

    private fun calculateStats(newLocation: Location) {
        if (lastLocationPoint != null) {
            // Calculate distance added since last point (in meters)
            val distanceMeters = lastLocationPoint!!.distanceTo(newLocation)

            // Convert to kilometers and add to total
            totalDistanceKm.doubleValue += distanceMeters / 1000.0

            // Calculate current speed (m/s to km/h)
            currentSpeedKmh.doubleValue = newLocation.speed * 3.6
        }
        lastLocationPoint = newLocation
    }

    // Timer Job
    private fun startTimer() {
        startTime = System.currentTimeMillis() - timeElapsedMs.value
        job = viewModelScope.launch {
            while (isTracking.value) {
                delay(100) // Update every 100ms for smooth timer
                timeElapsedMs.value = System.currentTimeMillis() - startTime
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (!isTracking.value) {
            isTracking.value = true
            locationHistory.clear()
            totalDistanceKm.doubleValue = 0.0
            timeElapsedMs.value = 0L
            lastLocationPoint = null
            startLocationUpdates() // Public function call
            startTimer()
        }
    }

    fun stopTracking() {
        isTracking.value = false
        job?.cancel()
        stopLocationUpdates() // Public function call
        // Here you would save the final run data to Firebase/Room DB
    }

    fun pauseTracking() {
        isTracking.value = false
        job?.cancel()
        stopLocationUpdates() // Public function call
    }

    fun formatTime(timeMs: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs) % 60
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs) % 60
        val hours = TimeUnit.MILLISECONDS.toHours(timeMs)
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // --- Core Location Functions ---

    // REMOVED 'private' KEYWORD. THIS IS THE FIX.
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    // REMOVED 'private' KEYWORD. THIS IS THE FIX.
    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // ViewModel Lifecycle Management
    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        job?.cancel()
    }

    // Factory
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RunViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RunViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}