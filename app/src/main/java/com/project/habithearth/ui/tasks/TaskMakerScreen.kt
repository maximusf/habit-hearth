package com.project.habithearth.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.habithearth.HabitHearthApplication
import com.project.habithearth.model.TaskCategory
import com.project.habithearth.ui.map.defaultVillageBuildings
import com.project.habithearth.ui.state.GameStateViewModel
import com.project.habithearth.ui.theme.HearthPanelWarm

/**
 * Habit create/edit screen.
 *
 * Phase 5 of the DataStore refactor (see PLAN.md): persistence moved off
 * [GameStateViewModel] onto [TaskEditorViewModel] (DataStore-backed). Route
 * args (`taskId`, `buildingId`) are picked up by the editor VM via its
 * SavedStateHandle, so this composable no longer takes them as parameters.
 *
 * [gameStateViewModel] is still read for two pieces of progress data that
 * have not yet been split out (deferred to the second milestone):
 *   - `ownedBuildingIds` to filter the building dropdown.
 *   - reward-pool bookkeeping when a *completed* task's category changes
 *     (see the save handler). [TaskEditorViewModel.save] does not touch
 *     gems/coins, so the delta is applied here against the legacy VM.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskMakerScreen(
    onBack: () -> Unit,
    gameStateViewModel: GameStateViewModel,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as HabitHearthApplication
    val editorVm: TaskEditorViewModel = viewModel(
        factory = TaskEditorViewModelFactory(app.taskRepository),
    )

    val game by gameStateViewModel.uiState.collectAsState()
    val existingTask by editorVm.editingTask.collectAsState()
    val isEditMode = editorVm.isEditMode
    val initialBuildingId = editorVm.initialBuildingId

    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(TaskCategory.UNSORTED) }
    var buildingExpanded by remember { mutableStateOf(false) }
    var selectedBuildingId by remember { mutableStateOf<String?>(null) }
    // Track whether the form has been seeded from the persisted task at least
    // once. Without this, an in-flight DataStore re-emission (e.g. a save
    // round-trip) would clobber whatever the user just typed.
    var seeded by remember { mutableStateOf(false) }

    val villageBuildings = remember { defaultVillageBuildings() }
    val ownedBuildings = remember(game.ownedBuildingIds) {
        villageBuildings.filter { it.id in game.ownedBuildingIds }
    }
    val buildingsForDropdown = remember(ownedBuildings, existingTask?.buildingId) {
        // If the task is filed in a building the player no longer owns
        // (shouldn't happen today, but cheap to guard), keep that orphan in
        // the list so the user sees the current value rather than a silent
        // reset to Home.
        val orphanId = existingTask?.buildingId
        val orphan = orphanId?.let { id -> villageBuildings.find { it.id == id } }
        if (orphan != null && orphan.id !in game.ownedBuildingIds) {
            ownedBuildings + orphan
        } else {
            ownedBuildings
        }
    }

    // In edit mode, observeTask emits null if the task was deleted out from
    // under us; bail back to the previous screen so the editor doesn't sit on
    // stale state.
    LaunchedEffect(isEditMode, existingTask, seeded) {
        if (isEditMode && seeded && existingTask == null) {
            onBack()
        }
    }

    LaunchedEffect(existingTask, isEditMode) {
        if (!seeded) {
            val task = existingTask
            if (task != null) {
                title = task.title
                note = task.note
                selectedCategory = task.category
                selectedBuildingId = task.buildingId
                seeded = true
            } else if (!isEditMode) {
                selectedBuildingId =
                    initialBuildingId?.takeIf { it in game.ownedBuildingIds }
                seeded = true
            }
        }
    }

    LaunchedEffect(game.ownedBuildingIds, isEditMode) {
        if (!isEditMode) {
            val sid = selectedBuildingId
            if (sid != null && sid !in game.ownedBuildingIds) {
                selectedBuildingId = null
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit habit" else "New habit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(HearthPanelWarm)
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (isEditMode) "Update your habit" else "Create a habit",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What will you do?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            Text(
                text = "Category",
                style = MaterialTheme.typography.titleSmall,
            )
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedCategory.displayName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("Habit category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                ) {
                    TaskCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            onClick = {
                                selectedCategory = category
                                categoryExpanded = false
                            },
                        )
                    }
                }
            }

            Text(
                text = "File in building",
                style = MaterialTheme.typography.titleSmall,
            )
            ExposedDropdownMenuBox(
                expanded = buildingExpanded,
                onExpandedChange = { buildingExpanded = it },
            ) {
                val buildingLabel = selectedBuildingId?.let { id ->
                    villageBuildings.find { it.id == id }?.let { b -> "${b.shortLabel} — ${b.name}" }
                } ?: "Home (not on map)"
                OutlinedTextField(
                    value = buildingLabel,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text("Building") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = buildingExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                )
                ExposedDropdownMenu(
                    expanded = buildingExpanded,
                    onDismissRequest = { buildingExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Home (not filed to a building)") },
                        onClick = {
                            selectedBuildingId = null
                            buildingExpanded = false
                        },
                    )
                    buildingsForDropdown.forEach { building ->
                        DropdownMenuItem(
                            text = { Text("${building.shortLabel} — ${building.name}") },
                            onClick = {
                                selectedBuildingId = building.id
                                buildingExpanded = false
                            },
                        )
                    }
                }
            }

            Button(
                onClick = {
                    // Snapshot the persisted task before save so we can reward-
                    // rebalance if the user changed the category of a task that
                    // was already completed. TaskRepository.updateTask only
                    // touches the task fields - the gem/coin pools live on the
                    // legacy GameStateViewModel until ProgressRepository lands.
                    val before = existingTask
                    if (before != null && before.isCompleted && before.category != selectedCategory) {
                        gameStateViewModel.applyRewardDelta(before.category, -before.rewardAmount)
                        gameStateViewModel.applyRewardDelta(selectedCategory, before.rewardAmount)
                    }
                    editorVm.saveAsync(
                        title = title,
                        note = note,
                        category = selectedCategory,
                        buildingId = selectedBuildingId,
                    )
                    onBack()
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isEditMode) "Save changes" else "Save habit")
            }
        }
    }
}
