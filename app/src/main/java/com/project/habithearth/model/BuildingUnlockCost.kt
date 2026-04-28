package com.project.habithearth.model

/** Gems (or coins for unsorted / general buildings) required to unlock one map building. */
const val VillageBuildingUnlockCost: Int = 50

/**
 * Pure-data unlock-cost descriptor.
 *
 * Lives under `model/` (not `ui/map/`) so domain types like [ResourceProgress]
 * can reference it without the model layer depending on `ui/`. The UI-side
 * helpers ([com.project.habithearth.ui.map.unlockCost],
 * [com.project.habithearth.ui.map.displayLabel]) stay in `ui/map/` because
 * they bridge to `VillageBuilding`, which is a UI concern.
 */
sealed class BuildingUnlockCost {
    data class Gems(val category: TaskCategory, val amount: Int = VillageBuildingUnlockCost) : BuildingUnlockCost()
    data class Coins(val amount: Int = VillageBuildingUnlockCost) : BuildingUnlockCost()
}
