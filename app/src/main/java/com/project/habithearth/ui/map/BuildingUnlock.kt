package com.project.habithearth.ui.map

import com.project.habithearth.ui.model.TaskCategory
import com.project.habithearth.ui.state.GameUiState

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

fun GameUiState.canAfford(cost: BuildingUnlockCost): Boolean =
    when (cost) {
        is BuildingUnlockCost.Gems ->
            when (cost.category) {
                TaskCategory.STRENGTH -> strengthGems >= cost.amount
                TaskCategory.WISDOM -> wisdomGems >= cost.amount
                TaskCategory.VITALITY -> vitalityGems >= cost.amount
                TaskCategory.SPIRIT -> spiritGems >= cost.amount
                TaskCategory.UNSORTED -> false
            }
        is BuildingUnlockCost.Coins -> coins >= cost.amount
    }

fun GameUiState.withUnlockCostPaid(cost: BuildingUnlockCost): GameUiState =
    when (cost) {
        is BuildingUnlockCost.Gems ->
            when (cost.category) {
                TaskCategory.STRENGTH -> copy(strengthGems = (strengthGems - cost.amount).coerceAtLeast(0))
                TaskCategory.WISDOM -> copy(wisdomGems = (wisdomGems - cost.amount).coerceAtLeast(0))
                TaskCategory.VITALITY -> copy(vitalityGems = (vitalityGems - cost.amount).coerceAtLeast(0))
                TaskCategory.SPIRIT -> copy(spiritGems = (spiritGems - cost.amount).coerceAtLeast(0))
                TaskCategory.UNSORTED -> this
            }
        is BuildingUnlockCost.Coins -> copy(coins = (coins - cost.amount).coerceAtLeast(0))
    }
