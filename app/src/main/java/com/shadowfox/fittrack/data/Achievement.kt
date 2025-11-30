package com.shadowfox.fittrack.data

import androidx.compose.ui.graphics.vector.ImageVector

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val threshold: Int,
    val type: AchievementType,
    val isUnlocked: Boolean = false
)

enum class AchievementType {
    STEPS, WATER, WEIGHT
}