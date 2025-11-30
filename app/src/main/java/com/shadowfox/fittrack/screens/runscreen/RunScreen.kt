package com.shadowfox.fittrack.screens.runscreen

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun RunScreen(
    navController: NavController,
    application: Application
) {
    // Initialize the ViewModel
    val viewModel: RunViewModel = viewModel(
        factory = RunViewModel.Factory(application)
    )
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(false) }

    // Map camera state management
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(viewModel.defaultCameraPosition, 10f)
    }

    // Permission Launcher Setup
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasLocationPermission = isGranted
        }
    )

    // --- Permission Check and Request ---
    LaunchedEffect(Unit) {
        when {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                hasLocationPermission = true
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // --- Location Update Lifecycle Management ---
    DisposableEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            viewModel.startLocationUpdates()
        }
        onDispose {
            viewModel.stopLocationUpdates()
            viewModel.stopTracking() // Ensure tracking is stopped when leaving the screen
        }
    }

    // --- UI Layout ---
    Box(modifier = Modifier.fillMaxSize()) {
        if (hasLocationPermission) {
            // 1. Google Map
            MapContent(viewModel, cameraPositionState)

            // 2. Statistics Overlay
            StatsOverlay(viewModel, Modifier.align(Alignment.TopCenter))

            // 3. Control Buttons
            RunControls(viewModel, Modifier.align(Alignment.BottomCenter))

        } else {
            // 4. Permission Denied Message
            PermissionDeniedContent(permissionLauncher)
        }
    }

    // --- Update Camera on Location Change ---
    val currentLocation = viewModel.lastLocation.value
    LaunchedEffect(currentLocation) {
        currentLocation?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(it.latitude, it.longitude),
                15f // Zoom level for street view
            )
        }
    }
}

@Composable
private fun MapContent(
    viewModel: RunViewModel,
    cameraPositionState: CameraPositionState
) {
    val currentLocation = viewModel.lastLocation.value
    val locationHistory = viewModel.locationHistory

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // Draw the path taken
        if (locationHistory.isNotEmpty()) {
            Polyline(
                points = locationHistory,
                color = MaterialTheme.colorScheme.primary,
                width = 10f
            )
        }

        // Marker at current location
        currentLocation?.let {
            Marker(
                state = MarkerState(
                    position = LatLng(it.latitude, it.longitude)
                ),
                title = "Your Location",
                snippet = "Live GPS"
            )
        }
    }
}

@Composable
private fun StatsOverlay(viewModel: RunViewModel, modifier: Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Distance
                StatItem(
                    label = "Distance (km)",
                    value = String.format("%.2f", viewModel.totalDistanceKm.doubleValue)
                )
                // Time
                StatItem(
                    label = "Time",
                    value = viewModel.formatTime(viewModel.timeElapsedMs.value)
                )
                // Speed
                StatItem(
                    label = "Speed (km/h)",
                    value = String.format("%.1f", viewModel.currentSpeedKmh.doubleValue)
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RunControls(viewModel: RunViewModel, modifier: Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .background(Color.Transparent),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        if (!viewModel.isTracking.value) {
            // START Button
            Button(
                onClick = { viewModel.startTracking() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Start")
                Spacer(Modifier.padding(4.dp))
                Text("START")
            }
        } else {
            // STOP Button 
            Button(
                onClick = { viewModel.stopTracking() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.Stop, contentDescription = "Stop")
                Spacer(Modifier.padding(4.dp))
                Text("STOP")
            }
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    permissionLauncher: ActivityResultLauncher<String>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Location access is required to track your run.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
            Text("Request Location Permission")
        }
    }
}