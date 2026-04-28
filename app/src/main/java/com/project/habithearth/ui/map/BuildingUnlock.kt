package com.project.habithearth.ui.map

import com.project.habithearth.model.TaskCategory

/** Gems (or coins for unsorted / general buildings) required to unlock one map building. */
const val VillageBuildingUnlockCost: Int = 50

sealed class BuildingUnlockCost {
    data class Gems(val category: TaskCategory, val amount: Int = VillageBuildingUnlockCost) : BuildingUnlockCost()
    data class Coins(val amount: Int = VillageBuildingUnlockCost) : BuildingUnlockCost()
}

fun VillageBuilding.unlockCost(): BuildingUnlockCost =
    when (category) {
        TaskCategory.STRENGTH -> BuildingUnlockCost.Gems(TaskCategory.STRENGTH)
        TaskCategory.WISDOM -> BuildingUnlockCost.Gems(TaskCategory.WISDOM)
        TaskCategory.VITALITY -> BuildingUnlockCost.Gems(TaskCategory.VITALITY)
        TaskCategory.SPIRIT -> BuildingUnlockCost.Gems(TaskCategory.SPIRIT)
        TaskCategory.UNSORTED -> BuildingUnlockCost.Coins()
    }

fun BuildingUnlockCost.displayLabel(): String =
    when (this) {
        is BuildingUnlockCost.Gems -> "$amount ${category.displayName.lowercase()} gems"
        is BuildingUnlockCost.Coins -> "$amount coins"
    }

// canAfford / withUnlockCostPaid moved to model/ResourceProgress.kt in Phase 2.
// Receiver type changed from GameUiState to ResourceProgress so unlock-cost
// math no longer depends on the UI-state class.
