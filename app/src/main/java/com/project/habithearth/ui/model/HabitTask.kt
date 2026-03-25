package com.project.habithearth.ui.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class TaskCategory(
    val displayName: String,
    val outlineColor: Color,
) {
    UNSORTED("Unsorted", Color(0xFF6B7280)),
    WISDOM("Wisdom", Color(0xFF5C6BB8)),
    STRENGTH("Strength", Color(0xFFB85C5C)),
    VITALITY("Vitality", Color(0xFF5CB86B)),
    SPIRIT("Spirit", Color(0xFF8B5CB8)),
}

data class HabitTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val category: TaskCategory,
    /** Gems (or coins for [TaskCategory.UNSORTED]) granted when marked complete. */
    val rewardAmount: Int = 1,
    val isCompleted: Boolean = false,
    /**
     * [com.project.habithearth.ui.map.VillageBuilding.id] this habit is filed under, or null / blank for **Home** (unfiled).
     */
    val buildingId: String? = null,
)
