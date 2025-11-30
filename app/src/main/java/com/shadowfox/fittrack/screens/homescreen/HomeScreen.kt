package com.shadowfox.fittrack.screens.homescreen

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp // For Logout Icon
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.shadowfox.fittrack.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    application: Application
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(application))
    val steps by viewModel.steps.collectAsState()
    val goal by viewModel.goal.collectAsState()
    var hasPermission by remember { mutableStateOf(false) }

    // Context for Logout
    val context = LocalContext.current

    // Drawer State
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (isGranted) viewModel.startStepCounterService()
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(application, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                hasPermission = true
                viewModel.startStepCounterService()
            } else {
                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        } else {
            hasPermission = true
            viewModel.startStepCounterService()
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(application, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Permission check
            }
        }
    }

    // Drawer Navigation Layout
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text("FitTrack Menu", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall)

                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("Profile") },
                    selected = false,
                    onClick = { navController.navigate(Screen.Profile.route) }
                )
                NavigationDrawerItem(
                    label = { Text("Achievements") },
                    selected = false,
                    onClick = { navController.navigate(Screen.Achievements.route) }
                )

                // LOGOUT BUTTON
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Log Out", color = MaterialTheme.colorScheme.error) },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Log Out", tint = MaterialTheme.colorScheme.error) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close() // Close drawer first
                            viewModel.logout(context) {
                                // Navigate back to Login Screen and clear history so user can't go back
                                navController.navigate("login_screen") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("FitTrack Dashboard") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GoalSetter(viewModel)
                Spacer(Modifier.height(32.dp))
                CircularProgressBar(steps = steps, goal = goal, hasPermission = hasPermission)
                Spacer(Modifier.height(32.dp))
                Button(onClick = { viewModel.resetSteps() }) { Text("Reset Daily Steps") }
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "Service Status: ${if (viewModel.isServiceRunning.value) "Running" else "Stopped"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (viewModel.isServiceRunning.value) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun CircularProgressBar(steps: Int, goal: Int, hasPermission: Boolean) {
    val sweepAngle = if (goal > 0) (steps.toFloat() / goal.toFloat()) * 360f else 0f
    val animatedSweepAngle by animateFloatAsState(targetValue = sweepAngle, label = "step_progress")
    val size = 250.dp
    val strokeWidth = 16.dp
    val progressColor = if (steps >= goal) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(strokeWidth.toPx() / 2, strokeWidth.toPx() / 2),
                size = Size(size.toPx() - strokeWidth.toPx(), size.toPx() - strokeWidth.toPx()),
                style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Canvas(Modifier.size(size)) {
            drawArc(
                color = progressColor,
                startAngle = 270f,
                sweepAngle = animatedSweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidth.toPx() / 2, strokeWidth.toPx() / 2),
                size = Size(size.toPx() - strokeWidth.toPx(), size.toPx() - strokeWidth.toPx()),
                style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (hasPermission) {
                Text(text = steps.toString(), fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
                Text(text = "of $goal steps", style = MaterialTheme.typography.bodyLarge)
            } else {
                Text("Permission Needed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun GoalSetter(viewModel: HomeViewModel) {
    var isDialogOpen by remember { mutableStateOf(false) }

    Button(onClick = { isDialogOpen = true }) {
        Text("Change Daily Goal")
    }

    if (isDialogOpen) {
        var newGoalText by remember { mutableStateOf(viewModel.goal.value.toString()) }
        AlertDialog(
            onDismissRequest = { isDialogOpen = false },
            title = { Text("Set New Step Goal") },
            text = {
                OutlinedTextField(
                    value = newGoalText,
                    onValueChange = { newGoalText = it.filter { char -> char.isDigit() } },
                    label = { Text("Steps") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newGoal = newGoalText.toIntOrNull() ?: viewModel.goal.value
                    if (newGoal > 0) viewModel.saveNewGoal(newGoal)
                    isDialogOpen = false
                }) { Text("Save") }
            },
            dismissButton = { Button(onClick = { isDialogOpen = false }) { Text("Cancel") } }
        )
    }
}