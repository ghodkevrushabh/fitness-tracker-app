package com.shadowfox.fittrack.screens.achievements

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.shadowfox.fittrack.data.Achievement
import com.shadowfox.fittrack.data.AchievementRepository

@Composable
fun AchievementsScreen(
    navController: NavController,
    application: Application
) {
    val viewModel: AchievementsViewModel = viewModel(
        factory = AchievementsViewModel.Factory(application)
    )
    val unlocked by viewModel.unlockedBadges.collectAsState()
    val nextBadges by viewModel.nextBadges.collectAsState()

    // Calculate Hidden Badges Logic (Done directly here for simplicity)
    val totalBadgesCount = AchievementRepository.allAchievements.size
    val visibleBadgesCount = unlocked.size + nextBadges.size
    val hiddenCount = (totalBadgesCount - visibleBadgesCount).coerceAtLeast(0)

    var selectedBadge by remember { mutableStateOf<Achievement?>(null) }

    // Badge Details Dialog
    if (selectedBadge != null) {
        AlertDialog(
            onDismissRequest = { selectedBadge = null },
            icon = { Icon(selectedBadge!!.icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) },
            title = { Text(selectedBadge!!.title) },
            text = {
                Column {
                    Text(selectedBadge!!.description)
                    Spacer(Modifier.height(8.dp))
                    val status = if (selectedBadge!!.isUnlocked) "Unlocked! 🎉" else "Goal: ${selectedBadge!!.threshold}"
                    Text(status, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                }
            },
            confirmButton = {
                Button(onClick = { selectedBadge = null }) { Text("Close") }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.checkAchievements()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Next Milestones Section
        if (nextBadges.isNotEmpty()) {
            item {
                Text("Next Milestones 🎯", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            items(nextBadges) { badge ->
                AchievementCard(badge, isLocked = true, onClick = { selectedBadge = badge })
            }
        }

        // 2. Unlocked Badges Section
        item {
            Text("Your Collection 🏆", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        if (unlocked.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Start moving to fill your trophy case!", color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(unlocked) { badge ->
                AchievementCard(badge, isLocked = false, onClick = { selectedBadge = badge })
            }
        }

        // 3. Motivational "Hidden Badges" Section
        item {
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (hiddenCount > 0)
                            "✨ $hiddenCount secret badges are waiting for you!"
                        else
                            "🎉 You are a legend! All badges revealed!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    if (hiddenCount > 0) {
                        Text(
                            text = "Stay consistent and crush your goals to reveal them.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement, isLocked: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLocked) 0.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = achievement.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isLocked) Color.Gray else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    achievement.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (isLocked) "Locked - Tap to see details" else achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isLocked) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}