package com.project.habithearth.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.habithearth.data.UserProgressRepository
import com.project.habithearth.ui.components.HabitTaskRowCard
import com.project.habithearth.ui.components.VerticalScrollIndicator
import com.project.habithearth.ui.state.GameStateViewModel
import com.project.habithearth.ui.state.GameStateViewModelFactory
import com.project.habithearth.ui.theme.HabitHearthTheme

@Composable
fun HomeScreen(
    welcomeDisplayName: String,
    onOpenTasks: () -> Unit,
    onEditTask: (String) -> Unit,
    gameStateViewModel: GameStateViewModel,
    modifier: Modifier = Modifier,
) {
    val game by gameStateViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A3323))
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Welcome, $welcomeDisplayName",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        Text(
            text = "Today's habits — all tasks, including those filed in map buildings",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

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
                game.tasks.forEach { task ->
                        HabitTaskRowCard(
                            task = task,
                            onCompletedChange = { checked ->
                                gameStateViewModel.setTaskCompleted(task.id, checked)
                            },
                            onOpenEdit = { onEditTask(task.id) },
                        )
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
            onClick = onOpenTasks,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
        ) {
            Text("Create habit")
        }
    }
}

//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//private fun HomeScreenPreview() {
//    val context = LocalContext.current
//    val repo = remember { UserProgressRepository(context.applicationContext) }
//    val gameVm: GameStateViewModel = viewModel(factory = GameStateViewModelFactory(repo))
//    HabitHearthTheme {
//        HomeScreen(
//            welcomeDisplayName = "Traveler",
//            onOpenTasks = {},
//            onEditTask = {},
//            gameStateViewModel = gameVm,
//        )
//    }
//}
