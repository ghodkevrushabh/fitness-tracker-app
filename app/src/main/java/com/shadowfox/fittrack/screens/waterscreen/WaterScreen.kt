package com.shadowfox.fittrack.screens.waterscreen

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Refresh // Icon for Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.shadowfox.fittrack.screens.waterscreen.WaterViewModel
import com.shadowfox.fittrack.data.WaterDataStore

@Composable
fun WaterScreen(
    navController: NavController,
    application: Application
) {
    val viewModel: WaterViewModel = viewModel(
        factory = WaterViewModel.Factory(application)
    )

    val intake by viewModel.intake.collectAsState()
    val goal by viewModel.goal.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Water Progress Bar
        WaterProgressBar(intake = intake, goal = goal)

        Spacer(Modifier.height(32.dp))

        // Settings Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WaterGoalSetter(viewModel = viewModel)
            AlarmSettingDialog(viewModel = viewModel)
        }

        Spacer(Modifier.height(32.dp))

        // Quick Add Buttons
        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            AddWaterButton(amountMl = 250, viewModel = viewModel)
            AddWaterButton(amountMl = 500, viewModel = viewModel)
        }

        Spacer(Modifier.height(16.dp))

        // UNDO BUTTON
        Button(
            onClick = { viewModel.undoLastIntake() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Undo")
            Spacer(Modifier.width(8.dp))
            Text("Undo Last")
        }

        Spacer(Modifier.height(24.dp))

        StatusCard(intake = intake, goal = goal)

        Spacer(Modifier.height(24.dp))

        Button(onClick = { viewModel.resetWater() }) {
            Text("Reset Daily Intake")
        }
    }
}

// --- HELPER COMPOSABLES ---

@Composable
fun WaterProgressBar(intake: Int, goal: Int) {
    val progress = if (goal > 0) intake.toFloat() / goal.toFloat() else 0f
    val sweepAngle = progress.coerceAtMost(1f) * 360f
    val animatedSweepAngle by androidx.compose.animation.core.animateFloatAsState(
        targetValue = sweepAngle, label = "water_progress"
    )
    val size = 200.dp
    val strokeWidth = 20.dp
    val waterColor = Color(0xFF42A5F5)

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
                color = waterColor,
                startAngle = 270f,
                sweepAngle = animatedSweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidth.toPx() / 2, strokeWidth.toPx() / 2),
                size = Size(size.toPx() - strokeWidth.toPx(), size.toPx() - strokeWidth.toPx()),
                style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.LocalDrink,
                contentDescription = "Water",
                tint = waterColor,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(text = "${(progress * 100).toInt()}%", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AddWaterButton(amountMl: Int, viewModel: WaterViewModel) {
    Button(
        onClick = { viewModel.addWater(amountMl) },
        modifier = Modifier.width(120.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Add")
        Spacer(Modifier.width(8.dp))
        Text("${amountMl}ml")
    }
}

@Composable
fun StatusCard(intake: Int, goal: Int) {
    val remaining = (goal - intake).coerceAtLeast(0)
    val cardColor = if (remaining == 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(0.9f),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Today's Intake: ${intake}ml / ${goal}ml", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (remaining == 0) "GOAL COMPLETE!" else "${remaining}ml remaining",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (remaining == 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun WaterGoalSetter(viewModel: WaterViewModel) {
    var isDialogOpen by remember { mutableStateOf(false) }
    val goal by viewModel.goal.collectAsState()

    Button(onClick = { isDialogOpen = true }) {
        Text("Set Goal")
    }

    if (isDialogOpen) {
        var goalText by remember { mutableStateOf(goal.toString()) }
        AlertDialog(
            onDismissRequest = { isDialogOpen = false },
            title = { Text("Set Daily Goal (ml)") },
            text = {
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it.filter { char -> char.isDigit() } },
                    label = { Text("Goal (ml)") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newGoal = goalText.toIntOrNull() ?: goal
                    viewModel.saveGoal(newGoal)
                    isDialogOpen = false
                }) { Text("Save") }
            },
            dismissButton = {
                Button(onClick = { isDialogOpen = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AlarmSettingDialog(viewModel: WaterViewModel) {
    var isDialogOpen by remember { mutableStateOf(false) }
    val interval by viewModel.reminderInterval.collectAsState()

    Button(onClick = { isDialogOpen = true }) {
        Text(if (interval > 0) "Alarm: ${interval}h" else "Set Alarm")
    }

    if (isDialogOpen) {
        var intervalText by remember { mutableStateOf(if (interval > 0) interval.toString() else "2") }
        AlertDialog(
            onDismissRequest = { isDialogOpen = false },
            title = { Text("Set Reminder Interval") },
            text = {
                Column {
                    Text("Remind me every:")
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = { intervalText = it.filter { char -> char.isDigit() } },
                        label = { Text("Hours") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val hours = intervalText.toIntOrNull() ?: 0
                    viewModel.setReminderInterval(hours)
                    isDialogOpen = false
                }) { Text("Save") }
            },
            dismissButton = {
                Button(onClick = {
                    viewModel.setReminderInterval(0)
                    isDialogOpen = false
                }) { Text("Cancel") }
            }
        )
    }
}