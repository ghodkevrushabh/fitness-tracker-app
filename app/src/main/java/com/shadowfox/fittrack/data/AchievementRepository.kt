package com.shadowfox.fittrack.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk // <--- Use this one
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop

object AchievementRepository {

    val allAchievements = listOf(
        // --- STEP MILESTONES (Fun & Dedicated) ---
        // FIX: Updated to use the correct AutoMirrored icon
        Achievement("step_1k", "Wake Up Call", "First 1,000 steps! You're awake!", Icons.AutoMirrored.Filled.DirectionsWalk, 1000, AchievementType.STEPS),
        Achievement("step_3k", "Momentum Builder", "3,000 steps. Hitting your stride.", Icons.AutoMirrored.Filled.DirectionsRun, 3000, AchievementType.STEPS),
        Achievement("step_5k", "Halfway Hero", "5,000 steps. Halfway to greatness!", Icons.Default.Star, 5000, AchievementType.STEPS),
        Achievement("step_7k", "Sweat Equity", "7,500 steps. Putting in the work!", Icons.Default.LocalFireDepartment, 7500, AchievementType.STEPS),
        Achievement("step_10k", "The Daily Legend", "10,000 steps. You crushed the standard!", Icons.Default.EmojiEvents, 10000, AchievementType.STEPS),
        Achievement("step_12k", "Overdrive", "12,500 steps. Going above and beyond!", Icons.Default.Bolt, 12500, AchievementType.STEPS),
        Achievement("step_15k", "Concrete Conqueror", "15,000 steps. The city is yours.", Icons.AutoMirrored.Filled.DirectionsRun, 15000, AchievementType.STEPS),
        Achievement("step_20k", "Unstoppable Force", "20,000 steps. Simply incredible performance.", Icons.Default.FitnessCenter, 20000, AchievementType.STEPS),

        // --- HYDRATION MILESTONES ---
        Achievement("water_1l", "Thirsty", "Drank 1,000ml. Staying fluid.", Icons.Default.WaterDrop, 1000, AchievementType.WATER),
        Achievement("water_2l", "Hydrated", "Reached 2,000ml daily goal.", Icons.Default.LocalDrink, 2000, AchievementType.WATER),
        Achievement("water_3l", "Hydro Homie", "3,000ml. Super hydrated!", Icons.Default.LocalDrink, 3000, AchievementType.WATER),
        Achievement("water_4l", "Aqua King", "Max hydration: 4,000ml.", Icons.Default.EmojiEvents, 4000, AchievementType.WATER),

        // --- WEIGHT MILESTONES ---
        Achievement("weight_start", "First Weigh-In", "Logged starting weight.", Icons.AutoMirrored.Filled.TrendingUp, 1, AchievementType.WEIGHT),
        Achievement("weight_5", "Consistent", "Logged weight 5 times.", Icons.Default.Star, 5, AchievementType.WEIGHT),
        Achievement("weight_10", "Dedicated", "Logged weight 10 times.", Icons.Default.FitnessCenter, 10, AchievementType.WEIGHT)
    )

    // Returns: Pair(Unlocked Badges, Next Goal Badges)
    fun getAchievementStatus(currentSteps: Int, currentWater: Int, totalWeightEntries: Int): Pair<List<Achievement>, List<Achievement>> {
        val unlocked = mutableListOf<Achievement>()
        val nextBadges = mutableListOf<Achievement>()

        fun checkType(type: AchievementType, currentValue: Int) {
            val badges = allAchievements.filter { it.type == type }.sortedBy { it.threshold }

            var nextFound = false
            for (badge in badges) {
                if (currentValue >= badge.threshold) {
                    unlocked.add(badge.copy(isUnlocked = true))
                } else {
                    if (!nextFound) {
                        nextBadges.add(badge.copy(isUnlocked = false))
                        nextFound = true
                    }
                }
            }
        }

        checkType(AchievementType.STEPS, currentSteps)
        checkType(AchievementType.WATER, currentWater)
        checkType(AchievementType.WEIGHT, totalWeightEntries)

        return Pair(unlocked, nextBadges)
    }

    // NEW FUNCTION: Returns a motivating message string based on how many badges are left to discover
    fun getHiddenPotentialMessage(currentSteps: Int, currentWater: Int, totalWeightEntries: Int): String {
        // 1. Get lists of what is unlocked and what is the immediate next goal
        val (unlocked, next) = getAchievementStatus(currentSteps, currentWater, totalWeightEntries)

        // 2. Calculate total visible badges (Unlocked + Next)
        val visibleCount = unlocked.size + next.size

        // 3. Calculate "Hidden" badges (Total - Visible)
        val hiddenCount = allAchievements.size - visibleCount

        return if (hiddenCount > 0) {
            "✨ There are $hiddenCount more secret badges waiting for you! Stay consistent and crush your goals to reveal them!"
        } else {
            "🎉 Incredible! You've revealed every badge! Keep pushing to maintain your legendary status!"
        }
    }
}