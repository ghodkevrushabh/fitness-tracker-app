package com.shadowfox.fittrack.screens.weightscreen

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.shadowfox.fittrack.data.WeightEntry

@Composable
fun WeightScreen(
    navController: NavController,
    application: Application
) {
    val viewModel: WeightViewModel = viewModel(
        factory = WeightViewModel.Factory(application)
    )
    val weightList by viewModel.weights.collectAsState()
    var weightInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Weight Tracker", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(24.dp))

        // Input Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = weightInput,
                onValueChange = { weightInput = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Enter Weight (kg)") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(16.dp))
            Button(onClick = {
                val w = weightInput.toFloatOrNull()
                if (w != null && w > 0) {
                    viewModel.saveWeight(w)
                    weightInput = ""
                }
            }) {
                Text("Save")
            }
        }

        Spacer(Modifier.height(32.dp))

        // The Graph
        if (weightList.isNotEmpty()) {
            Text("Progress Graph", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            WeightGraph(weightList = weightList)

            Spacer(Modifier.height(16.dp))

            // Current Weight Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current Weight", style = MaterialTheme.typography.labelMedium)
                    Text("${weightList.last().weight} kg", style = MaterialTheme.typography.headlineLarge)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("No data yet. Add your weight to see the graph!")
            }
        }
    }
}

@Composable
fun WeightGraph(weightList: List<WeightEntry>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.secondary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // FIX: Handle single entry gracefully
        if (weightList.size == 1) {
            drawCircle(
                color = pointColor,
                radius = 8.dp.toPx(),
                center = Offset(size.width / 2, size.height / 2)
            )
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    weightList[0].weight.toString(),
                    size.width / 2,
                    (size.height / 2) - 30f,
                    android.graphics.Paint().apply {
                        textSize = 40f
                        color = android.graphics.Color.BLACK
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
            return@Canvas
        }

        val maxWeight = weightList.maxOf { it.weight } + 5f
        val minWeight = (weightList.minOf { it.weight } - 5f).coerceAtLeast(0f)
        // Ensure we don't divide by zero if all weights are the same
        val heightRange = (maxWeight - minWeight).coerceAtLeast(1f)
        val widthPerPoint = size.width / (weightList.size - 1)

        val path = Path()

        weightList.forEachIndexed { index, entry ->
            val x = index * widthPerPoint
            val y = size.height - ((entry.weight - minWeight) / heightRange * size.height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            // Draw Point
            drawCircle(
                color = pointColor,
                radius = 6.dp.toPx(),
                center = Offset(x, y)
            )
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 5.dp.toPx())
        )
    }
}