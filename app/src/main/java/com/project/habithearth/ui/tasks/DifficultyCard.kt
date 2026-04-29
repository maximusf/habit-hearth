package com.project.habithearth.ui.tasks

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DifficultyCard(
    modifier: Modifier = Modifier,
    viewModel: TaskSentenceViewModel = viewModel(),
    startExpanded: Boolean = true,
) {
    LaunchedEffect(startExpanded) {
        viewModel.isDifficultyCardExpanded = startExpanded
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        onClick = {
            if (!viewModel.isDifficultyCardExpanded) viewModel.isDifficultyCardExpanded = true
        },
    ) {
        if (viewModel.isDifficultyCardExpanded) {
            Row(
                modifier = Modifier.padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Difficulty (1-5):   ", style = MaterialTheme.typography.bodyLarge)
                Box {
                    Button(onClick = { viewModel.isDifficultyMenuVisible = true }) {
                        Text(
                            if (viewModel.selectedDifficulty == 0) "--Select--"
                            else viewModel.selectedDifficulty.toString()
                        )
                    }
                    DropdownMenu(
                        expanded = viewModel.isDifficultyMenuVisible,
                        onDismissRequest = { viewModel.isDifficultyMenuVisible = false },
                        modifier = Modifier
                            .width(40.dp)
                            .height(20.dp)
                            .align(Alignment.Center),
                    ) {
                        TaskSentenceViewModel.Difficulty.entries.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.label) },
                                onClick = {
                                    viewModel.selectedDifficulty = level.label.toInt()
                                    viewModel.isDifficultyMenuVisible = false
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (viewModel.selectedDifficulty != 0) {
                    IconButton(
                        modifier = Modifier.padding(15.dp),
                        onClick = { viewModel.isDifficultyCardExpanded = false },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm Difficulty",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
            ) {
                Text(
                    text = "Difficulty: ${viewModel.selectedDifficulty}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
