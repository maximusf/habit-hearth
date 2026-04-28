package com.project.habithearth.model

import com.project.habithearth.ui.map.BuildingUnlockCost

/**
 * Pure-data view of the player's wallet, XP, and owned buildings.
 *
 * Carved out of `GameUiState` so unlock-cost math can run without dragging the
 * full UI state class along. During Phase 3+ this becomes the canonical type
 * persisted by `ProgressRepository`; for now `GameUiState` mirrors these fields
 * and bridges via [com.project.habithearth.ui.state.resources] /
 * [com.project.habithearth.ui.state.copyResources].
 */
data class ResourceProgress(
    val strengthGems: Int = 0,
    val wisdomGems: Int = 0,
    val vitalityGems: Int = 0,
    val spiritGems: Int = 0,
    val coins: Int = 0,
    val xpProgress: Float = 0f,
    val ownedBuildingIds: Set<String> = emptySet(),
)

/**
 * True iff the player can pay [cost] right now. UNSORTED gems are intentionally
 * unaffordable: the unsorted pool is "coins", not gems, and a `Gems(UNSORTED)`
 * cost should never be constructed in the first place
 * (see [com.project.habithearth.ui.map.unlockCost]).
 */
fun ResourceProgress.canAfford(cost: BuildingUnlockCost): Boolean =
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

/**
 * Returns a new [ResourceProgress] with [cost] subtracted from the matching
 * pool. Floors at zero defensively even though [canAfford] should be checked
 * first; cheaper to be wrong about a phantom call site than to ship negative
 * gem balances.
 */
fun ResourceProgress.withUnlockCostPaid(cost: BuildingUnlockCost): ResourceProgress =
    when (cost) {
        is BuildingUnlockCost.Gems ->
            when (cost.category) {
                TaskCategory.STRENGTH -> copy(strengthGems = (strengthGems - cost.amount).coerceAtLeast(0))
                TaskCategory.WISDOM -> copy(wisdomGems = (wisdomGems - cost.amount).coerceAtLeast(0))
                TaskCategory.VITALITY -> copy(vitalityGems = (vitalityGems - cost.amount).coerceAtLeast(0))
                TaskCategory.SPIRIT -> copy(spiritGems = (spiritGems - cost.amount).coerceAtLeast(0))
                // Defensive: see canAfford note. UNSORTED is unreachable for Gems.
                TaskCategory.UNSORTED -> this
            }
        is BuildingUnlockCost.Coins -> copy(coins = (coins - cost.amount).coerceAtLeast(0))
    }
