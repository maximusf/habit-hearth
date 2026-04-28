package com.project.habithearth.ui.map

import com.project.habithearth.model.BuildingUnlockCost
import com.project.habithearth.model.TaskCategory

// BuildingUnlockCost (sealed) and VillageBuildingUnlockCost (constant) live in
// model/ so domain types can reference them without the model layer depending
// on ui/. The bridge-to-VillageBuilding helpers stay here because
// VillageBuilding is a UI concept.

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
