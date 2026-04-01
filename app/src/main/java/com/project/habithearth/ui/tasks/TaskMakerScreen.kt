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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.habithearth.data.UserProgressRepository
import com.project.habithearth.ui.map.defaultVillageBuildings
import com.project.habithearth.ui.model.TaskCategory
import com.project.habithearth.ui.state.GameStateViewModel
import com.project.habithearth.ui.theme.HearthPanelWarm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskMakerScreen(
    taskId: String?,
    initialBuildingId: String? = null,
    onBack: () -> Unit,
    gameStateViewModel: GameStateViewModel,
    modifier: Modifier = Modifier,
) {
    val game by gameStateViewModel.uiState.collectAsState()
    val existingTask = taskId?.let { id -> game.tasks.find { it.id == id } }
    val isEditMode = existingTask != null

    var title by remember(taskId) { mutableStateOf("") }
    var note by remember(taskId) { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember(taskId) { mutableStateOf(TaskCategory.UNSORTED) }
    var buildingExpanded by remember { mutableStateOf(false) }
    var selectedBuildingId by remember(taskId) { mutableStateOf<String?>(null) }
    val villageBuildings = remember { defaultVillageBuildings() }
    val ownedBuildings = remember(game.ownedBuildingIds) {
        villageBuildings.filter { it.id in game.ownedBuildingIds }
    }
    val buildingsForDropdown = remember(ownedBuildings, existingTask?.buildingId) {
        val orphanId = existingTask?.buildingId
        val orphan = orphanId?.let { id -> villageBuildings.find { it.id == id } }
        if (orphan != null && orphan.id !in game.ownedBuildingIds) {
            ownedBuildings + orphan
        } else {
            ownedBuildings
        }
    }

    LaunchedEffect(taskId, game.tasks) {
        if (taskId != null && game.tasks.none { it.id == taskId }) {
            onBack()
            return@LaunchedEffect
        }
    }

    LaunchedEffect(
        taskId,
        existingTask?.id,
        existingTask?.title,
        existingTask?.note,
        existingTask?.category,
        existingTask?.buildingId,
        initialBuildingId,
    ) {
        if (existingTask != null) {
            title = existingTask.title
            note = existingTask.note
            selectedCategory = existingTask.category
            selectedBuildingId = existingTask.buildingId
        } else if (taskId == null) {
            title = ""
            note = ""
            selectedCategory = TaskCategory.UNSORTED
            selectedBuildingId =
                initialBuildingId?.takeIf { it in game.ownedBuildingIds }
        }
    }

    LaunchedEffect(game.ownedBuildingIds, taskId) {
        if (taskId == null) {
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
                    if (isEditMode && taskId != null) {
                        gameStateViewModel.updateTask(
                            taskId = taskId,
                            title = title,
                            note = note,
                            category = selectedCategory,
                            buildingId = selectedBuildingId,
                        )
                    } else {
                        gameStateViewModel.addTask(
                            title = title,
                            note = note,
                            category = selectedCategory,
                            buildingId = selectedBuildingId,
                        )
                    }
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

//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//private fun TaskMakerScreenPreview() {
//    val context = LocalContext.current
//    val repo = remember { UserProgressRepository(context.applicationContext) }
//    val gameVm: GameStateViewModel = viewModel(factory = GameStateViewModelFactory(repo))
//    HabitHearthTheme {
//        TaskMakerScreen(
//            taskId = null,
//            initialBuildingId = "cottage",
//            onBack = {},
//            gameStateViewModel = gameVm,
//        )
//    }
//}
