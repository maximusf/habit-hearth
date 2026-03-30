package com.project.habithearth.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.habithearth.data.UserProgressRepository
import com.project.habithearth.ui.components.HabitTaskRowCard
import com.project.habithearth.ui.components.VerticalScrollIndicator
import com.project.habithearth.ui.state.GameStateViewModel
import com.project.habithearth.ui.state.GameStateViewModelFactory
import com.project.habithearth.ui.theme.HabitHearthTheme
import com.project.habithearth.ui.theme.HearthPanelWarm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingDetailScreen(
    buildingId: String,
    onBack: () -> Unit,
    onAddHabitInBuilding: (String) -> Unit,
    onEditTask: (String) -> Unit,
    gameStateViewModel: GameStateViewModel,
    modifier: Modifier = Modifier,
) {
    val game by gameStateViewModel.uiState.collectAsState()
    val building = villageBuildingById(buildingId)
    val tasksInBuilding = game.tasks.filter { it.buildingId == buildingId }
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = building?.name ?: "Building",
                        maxLines = 1,
                    )
                },
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
        if (building == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "This building isn’t on the map anymore.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Go back")
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(HearthPanelWarm)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            val context = LocalContext.current
            val headerAssetPath = remember(buildingId) {
                markerAssetPathForBuilding(buildingId, 0)
            }
            val headerBitmap: ImageBitmap? = remember(headerAssetPath) {
                decodeBuildingMarkerBitmap(
                    context = context,
                    assetPath = headerAssetPath,
                    maxEdgePx = 256,
                )
            }

            if (headerBitmap != null) {
                Image(
                    bitmap = headerBitmap,
                    contentDescription = "${building.name} header",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Spacer(modifier = Modifier.height(130.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = building.story,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text(
                text = "Habits filed here",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 14.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (tasksInBuilding.isEmpty()) {
                        Text(
                            text = "No habits in this building yet. Add one to file it here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        tasksInBuilding.forEach { task ->
                            HabitTaskRowCard(
                                task = task,
                                onCompletedChange = { checked ->
                                    gameStateViewModel.setTaskCompleted(task.id, checked)
                                },
                                onOpenEdit = { onEditTask(task.id) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                VerticalScrollIndicator(
                    scrollState = scrollState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                )
            }

            Button(
                onClick = { onAddHabitInBuilding(buildingId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
            ) {
                Text("Add habit to this building")
            }
        }
    }
}

private fun decodeBuildingMarkerBitmap(
    context: Context,
    assetPath: String,
    maxEdgePx: Int,
): ImageBitmap? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        val sample = sampleSizeForMaxEdge(bounds.outWidth, bounds.outHeight, maxEdgePx)
        val decode = BitmapFactory.Options().apply {
            inSampleSize = sample
            inScaled = false
        }
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream, null, decode)?.asImageBitmap()
        }
    }.getOrNull()
}

private fun sampleSizeForMaxEdge(width: Int, height: Int, maxEdgePx: Int): Int {
    val longest = maxOf(width, height)
    if (longest <= maxEdgePx) return 1
    var sample = 1
    while (longest / sample > maxEdgePx) {
        sample *= 2
    }
    return sample
}

//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//private fun BuildingDetailScreenPreview() {
//    val context = LocalContext.current
//    val repo = remember { UserProgressRepository(context.applicationContext) }
//    val gameVm: GameStateViewModel = viewModel(factory = GameStateViewModelFactory(repo))
//    HabitHearthTheme {
//        BuildingDetailScreen(
//            buildingId = "cottage",
//            onBack = {},
//            onAddHabitInBuilding = {},
//            onEditTask = {},
//            gameStateViewModel = gameVm,
//        )
//    }
//}
