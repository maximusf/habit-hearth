package com.project.habithearth.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.project.habithearth.data.UserProgressRepository
import com.project.habithearth.ui.map.unlockCost
import com.project.habithearth.ui.map.villageBuildingById
import com.project.habithearth.ui.map.canAfford
import com.project.habithearth.ui.map.MainHubBuildingIds
import com.project.habithearth.ui.map.withUnlockCostPaid
import com.project.habithearth.ui.model.HabitTask
import com.project.habithearth.ui.model.TaskCategory
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
    val tasks: List<HabitTask> = defaultSeedTasks(),
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

    fun addTask(
        title: String,
        note: String,
        category: TaskCategory,
        rewardAmount: Int = 1,
        buildingId: String? = null,
    ) {
        val normalizedBuilding = buildingId?.trim()?.takeIf { it.isNotEmpty() }
        _uiState.update { s ->
            val task = HabitTask(
                title = title.trim(),
                note = note.trim(),
                category = category,
                rewardAmount = rewardAmount.coerceAtLeast(1),
                buildingId = normalizedBuilding,
            )
            s.copy(tasks = s.tasks + task)
        }
        persist()
    }

    fun setTaskCompleted(taskId: String, completed: Boolean) {
        _uiState.update { s ->
            val task = s.tasks.find { it.id == taskId } ?: return@update s
            if (task.isCompleted == completed) return@update s

            val delta = if (completed) task.rewardAmount else -task.rewardAmount
            val newTasks = s.tasks.map { t ->
                if (t.id == taskId) t.copy(isCompleted = completed) else t
            }
            s.copy(tasks = newTasks).withResourceDelta(task.category, delta)
        }
        persist()
    }

    fun updateTask(
        taskId: String,
        title: String,
        note: String,
        category: TaskCategory,
        buildingId: String?,
    ) {
        val normalizedBuilding = buildingId?.trim()?.takeIf { it.isNotEmpty() }
        _uiState.update { s ->
            val task = s.tasks.find { it.id == taskId } ?: return@update s
            var next = s
            if (task.isCompleted && task.category != category) {
                next = next.withResourceDelta(task.category, -task.rewardAmount)
                next = next.withResourceDelta(category, task.rewardAmount)
            }
            next.copy(
                tasks = next.tasks.map { t ->
                    if (t.id == taskId) {
                        t.copy(
                            title = title.trim(),
                            note = note.trim(),
                            category = category,
                            buildingId = normalizedBuilding,
                        )
                    } else {
                        t
                    }
                },
            )
        }
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
        if (!current.canAfford(cost)) return false
        _uiState.update { s ->
            if (building.id in s.ownedBuildingIds) return@update s
            if (!s.canAfford(cost)) return@update s
            s.withUnlockCostPaid(cost).copy(ownedBuildingIds = s.ownedBuildingIds + building.id)
        }
        persist()
        return true
    }
}

@Suppress("UNCHECKED_CAST")
class GameStateViewModelFactory(
    private val repository: UserProgressRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameStateViewModel(repository) as T
    }
}

private fun defaultSeedTasks(): List<HabitTask> = listOf(
    HabitTask(
        title = "Morning stretch — 5 min",
        category = TaskCategory.VITALITY,
        rewardAmount = 1,
    ),
    HabitTask(
        title = "Drink a full glass of water",
        category = TaskCategory.VITALITY,
        rewardAmount = 1,
    ),
    HabitTask(
        title = "Read one chapter",
        category = TaskCategory.WISDOM,
        rewardAmount = 2,
    ),
)
