package com.project.habithearth.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.project.habithearth.data.UserProgressRepository
import com.project.habithearth.model.ResourceProgress
import com.project.habithearth.model.TaskCategory
import com.project.habithearth.model.canAfford
import com.project.habithearth.model.withUnlockCostPaid
import com.project.habithearth.ui.map.MainHubBuildingIds
import com.project.habithearth.ui.map.unlockCost
import com.project.habithearth.ui.map.villageBuildingById
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val strengthGems: Int = 12,
    val wisdomGems: Int = 9,
    val vitalityGems: Int = 15,
    val spiritGems: Int = 8,
    val coins: Int = 240,
    val xpProgress: Float = 0.62f,
    /** Map buildings the player has unlocked (starter hubs are merged in when loading a save). */
    val ownedBuildingIds: Set<String> = emptySet(),
)

class GameStateViewModel(
    private val userProgressRepository: UserProgressRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reloadFromRepositoryNow()
        }
    }

    /** Loads from disk, or default [GameUiState] if none. Safe to call after [UserProgressRepository.clearAllLocalData]. */
    suspend fun reloadFromRepositoryNow() {
        _uiState.value = userProgressRepository.loadGameState() ?: GameUiState()
    }

    private fun persist() {
        viewModelScope.launch {
            userProgressRepository.saveGameState(_uiState.value)
        }
    }

    /**
     * Applies a gem/coin delta for a task category without touching the task
     * list. Phase 5 of the DataStore refactor (see PLAN.md): tasks now live in
     * [com.project.habithearth.data.task.TaskRepository], but reward-pool
     * bookkeeping stays here until `ProgressRepository` lands. Screens call
     * this after [com.project.habithearth.ui.tasks.TaskListViewModel.setCompleted]
     * reports a real flip, so we don't double-credit on no-op writes.
     *
     * Pass a positive [delta] when crediting a fresh completion, negative when
     * un-completing or re-categorizing a completed task.
     */
    fun applyRewardDelta(category: TaskCategory, delta: Int) {
        if (delta == 0) return
        _uiState.update { it.withResourceDelta(category, delta) }
        persist()
    }

    private fun GameUiState.withResourceDelta(category: TaskCategory, delta: Int): GameUiState {
        return when (category) {
            TaskCategory.STRENGTH -> copy(strengthGems = (strengthGems + delta).coerceAtLeast(0))
            TaskCategory.WISDOM -> copy(wisdomGems = (wisdomGems + delta).coerceAtLeast(0))
            TaskCategory.VITALITY -> copy(vitalityGems = (vitalityGems + delta).coerceAtLeast(0))
            TaskCategory.SPIRIT -> copy(spiritGems = (spiritGems + delta).coerceAtLeast(0))
            TaskCategory.UNSORTED -> copy(coins = (coins + delta).coerceAtLeast(0))
        }
    }

    /**
     * Unlocks a map building if the player can pay its category cost (see `VillageBuildingUnlockCost`).
     * Starter hub buildings are always owned (no charge).
     */
    fun tryPurchaseBuilding(buildingId: String): Boolean {
        val building = villageBuildingById(buildingId) ?: return false
        val current = _uiState.value
        if (building.id in current.ownedBuildingIds) return true
        val cost = building.unlockCost()
        if (!current.resources.canAfford(cost)) return false
        _uiState.update { s ->
            if (building.id in s.ownedBuildingIds) return@update s
            // Recheck affordability inside the atomic update: resource pools
            // can shift between the read above and the write here (e.g. a
            // concurrent task completion), and canAfford on a stale snapshot
            // is not enough to guarantee non-negative balances.
            if (!s.resources.canAfford(cost)) return@update s
            val paid = s.resources.withUnlockCostPaid(cost)
            s.copyResources(paid).copy(ownedBuildingIds = s.ownedBuildingIds + building.id)
        }
        persist()
        return true
    }
}

/**
 * Bridge view of the player's wallet/XP/owned buildings as a [ResourceProgress].
 *
 * Phase 2 of the DataStore refactor moves unlock-cost math onto
 * [ResourceProgress] so it no longer depends on the UI state class. Until
 * Phase 4 collapses these duplicated fields, [GameUiState] continues to own
 * the actual storage and exposes this read-only view for the math.
 */
val GameUiState.resources: ResourceProgress
    get() = ResourceProgress(
        strengthGems = strengthGems,
        wisdomGems = wisdomGems,
        vitalityGems = vitalityGems,
        spiritGems = spiritGems,
        coins = coins,
        xpProgress = xpProgress,
        ownedBuildingIds = ownedBuildingIds,
    )

/** Inverse of [resources]: write a [ResourceProgress] back into a [GameUiState]. */
fun GameUiState.copyResources(rp: ResourceProgress): GameUiState =
    copy(
        strengthGems = rp.strengthGems,
        wisdomGems = rp.wisdomGems,
        vitalityGems = rp.vitalityGems,
        spiritGems = rp.spiritGems,
        coins = rp.coins,
        xpProgress = rp.xpProgress,
        ownedBuildingIds = rp.ownedBuildingIds,
    )

@Suppress("UNCHECKED_CAST")
class GameStateViewModelFactory(
    private val repository: UserProgressRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameStateViewModel(repository) as T
    }
}

