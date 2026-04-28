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

    // The editor VM publishes a separate "loaded" signal so we can tell apart
    // "first emission with a real task", "task vanished mid-edit", and the
    // initial null before the StateFlow has flushed its first value. Without
    // this, edit mode keyed off an invalid taskId would keep `seeded = false`
    // forever and the deleted-task fallback below would never fire.
    val isEditingTaskLoaded by editorVm.isEditingTaskLoaded.collectAsState()

    // In edit mode, observeTask emits null if the task was deleted out from
    // under us (or the route arg pointed at an invalid id to begin with);
    // either way, bail back so the editor doesn't sit on stale or empty state.
    LaunchedEffect(isEditMode, isEditingTaskLoaded, existingTask) {
        if (isEditMode && isEditingTaskLoaded && existingTask == null) {
            onBack()
        }
    }

    LaunchedEffect(existingTask, isEditMode, isEditingTaskLoaded) {
        if (!seeded) {
            val task = existingTask
            if (isEditMode) {
                if (task != null) {
                    title = task.title
                    note = task.note
                    selectedCategory = task.category
                    selectedBuildingId = task.buildingId
                    seeded = true
                }
                // If isEditingTaskLoaded is true but task is null, the
                // LaunchedEffect above will navigate back; nothing to seed.
            } else {
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
            // In edit mode the form is disabled until the editor VM has
            // observed at least one emission for the requested taskId.
            // Without this, a user typing fast could race the seeding
            // LaunchedEffect: the late DataStore emission would clobber
            // anything they had already entered. New-task mode has nothing
            // to wait on, so the form is always live.
            val formEnabled = !isEditMode || isEditingTaskLoaded
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("What will you do?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = formEnabled,
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = formEnabled,
            )

            Text(
                text = "Category",
                style = MaterialTheme.typography.titleSmall,
            )
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { if (formEnabled) categoryExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedCategory.displayName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    enabled = formEnabled,
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
                onExpandedChange = { if (formEnabled) buildingExpanded = it },
            ) {
                val buildingLabel = selectedBuildingId?.let { id ->
                    villageBuildings.find { it.id == id }?.let { b -> "${b.shortLabel} — ${b.name}" }
                } ?: "Home (not on map)"
                OutlinedTextField(
                    value = buildingLabel,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    enabled = formEnabled,
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
                    // Snapshot the persisted task and target category at click
                    // time so the reward-rebalance below sees the values as
                    // they were when the user pressed Save, even if the
                    // editing flow re-emits during the save round-trip.
                    val before = existingTask
                    val targetCategory = selectedCategory
                    // Pop only after the save has actually completed. Calling
                    // onBack() synchronously after saveAsync used to cancel
                    // the editor's viewModelScope mid-write (the back stack
                    // entry, and therefore the VM, gets cleared on pop), so
                    // slow DataStore writes lost edits.
                    editorVm.saveAsync(
                        title = title,
                        note = note,
                        category = targetCategory,
                        buildingId = selectedBuildingId,
                        onSaved = {
                            // Apply the reward rebalance only after the task
                            // category change has actually persisted. Doing it
                            // before saveAsync left the gem/coin pools mutated
                            // even when the save threw or was cancelled,
                            // silently desyncing rewards from tasks.
                            if (before != null && before.isCompleted && before.category != targetCategory) {
                                gameStateViewModel.applyRewardDelta(before.category, -before.rewardAmount)
                                gameStateViewModel.applyRewardDelta(targetCategory, before.rewardAmount)
                            }
                            onBack()
                        },
                    )
                },
                enabled = title.isNotBlank() && (!isEditMode || isEditingTaskLoaded),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isEditMode) "Save changes" else "Save habit")
            }
        }
    }
}
