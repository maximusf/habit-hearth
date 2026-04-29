package com.project.habithearth.model

import java.util.UUID

/**
 * Persisted habit task. Lives in `model/` rather than `ui/model/` because this
 * shape is read and written by the data layer (TaskRepository in Phase 3) and
 * must not depend on Compose.
 */
data class HabitTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val category: TaskCategory,
    /** Gems (or coins for [TaskCategory.UNSORTED]) granted when marked complete. */
    val rewardAmount: Int = 1,
    val isCompleted: Boolean = false,
    /**
     * [com.project.habithearth.ui.map.VillageBuilding.id] this habit is filed under,
     * or null / blank for **Home** (unfiled).
     */
    val buildingId: String? = null,
    val completionCount: Int = 0,
    /** Multiplier applied to [rewardAmount] on each completion; grows with consecutive-day streaks. */
    val streakMultiplier: Int = 1,
    val collectedCurrency: Int = 0,
    val collectionLog: List<CollectionLogEntry> = emptyList(),
)
