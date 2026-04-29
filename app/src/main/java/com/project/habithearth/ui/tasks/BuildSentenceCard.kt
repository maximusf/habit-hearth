package com.project.habithearth.ui.tasks

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BuildSentenceCard(
    modifier: Modifier = Modifier,
    viewModel: TaskSentenceViewModel = viewModel(),
    startExpanded: Boolean = true,
) {
    LaunchedEffect(startExpanded) {
        viewModel.isBuildCardExpanded = startExpanded
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        onClick = {
            if (!viewModel.isBuildCardExpanded) viewModel.isBuildCardExpanded = true
        },
    ) {
        if (viewModel.isBuildCardExpanded) {
            Column {
                FlowRow(
                    modifier = Modifier.padding(15.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    viewModel.sentenceSegments.forEachIndexed { index, segment ->
                        when (segment) {
                            is SentenceSegment.FixedText -> {
                                Text(
                                    text = segment.text,
                                    modifier = Modifier.padding(top = 15.dp),
                                    fontSize = 18.sp,
                                )
                            }

                            is SentenceSegment.EditableVerb -> {
                                OutlinedTextField(
                                    value = segment.text,
                                    onValueChange = { newValue ->
                                        viewModel.sentenceSegments[index] =
                                            segment.copy(text = newValue)
                                    },
                                    modifier = Modifier.width(IntrinsicSize.Min),
                                    label = { Text("Verb") },
                                )
                            }

                            is SentenceSegment.PrepositionChip -> {
                                Box {
                                    InputChip(
                                        selected = true,
                                        onClick = {
                                            viewModel.sentenceSegments[index] =
                                                segment.copy(isMenuExpanded = true)
                                        },
                                        label = { Text("${segment.preposition.label} ${segment.option}") },
                                    )
                                    DropdownMenu(
                                        expanded = segment.isMenuExpanded,
                                        onDismissRequest = {
                                            viewModel.sentenceSegments[index] =
                                                segment.copy(isMenuExpanded = false)
                                        },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Add before") },
                                            onClick = {
                                                viewModel.sentenceSegments[index] =
                                                    segment.copy(isMenuExpanded = false)
                                                viewModel.editingPrepositionIndex = index + 1
                                                viewModel.isInsertingByMenu = true
                                                viewModel.isPrepositionMenuVisible = true
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Change preposition") },
                                            onClick = {
                                                viewModel.sentenceSegments[index] =
                                                    segment.copy(isMenuExpanded = false)
                                                viewModel.editingPrepositionIndex = index
                                                viewModel.isInsertingByMenu = false
                                                viewModel.isPrepositionMenuVisible = true
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete preposition") },
                                            onClick = { viewModel.deletePrepositionBlock(index) },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete from here") },
                                            onClick = { viewModel.deleteRemainder(index) },
                                        )
                                    }
                                }
                            }

                            is SentenceSegment.InputField -> {
                                OutlinedTextField(
                                    value = segment.value,
                                    onValueChange = { newValue ->
                                        viewModel.sentenceSegments[index] =
                                            segment.copy(value = newValue)
                                    },
                                    modifier = Modifier.width(IntrinsicSize.Min),
                                    label = { Text(segment.hint) },
                                )
                            }

                            is SentenceSegment.DropdownField -> {
                                Box {
                                    TextButton(onClick = {
                                        viewModel.sentenceSegments[index] =
                                            segment.copy(isExpanded = true)
                                    }) {
                                        Text(segment.options[segment.selectedIndex])
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = segment.isExpanded,
                                        onDismissRequest = {
                                            viewModel.sentenceSegments[index] =
                                                segment.copy(isExpanded = false)
                                        },
                                    ) {
                                        segment.options.forEachIndexed { optIndex, label ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = {
                                                    viewModel.sentenceSegments[index] =
                                                        segment.copy(
                                                            selectedIndex = optIndex,
                                                            isExpanded = false,
                                                        )
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // "+" button for adding prepositions
                    Box {
                        FilledIconButton(onClick = { viewModel.isPrepositionMenuVisible = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = viewModel.isPrepositionMenuVisible,
                            onDismissRequest = { viewModel.isPrepositionMenuVisible = false },
                            modifier = Modifier.width(200.dp),
                        ) {
                            TaskSentenceViewModel.Preposition.entries.forEach { prep ->
                                val isExpanded = viewModel.expandedPrepositionInMenu == prep
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(prep.label, modifier = Modifier.weight(1f))
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                                                              else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.expandedPrepositionInMenu =
                                            if (isExpanded) null else prep
                                    },
                                )
                                if (isExpanded) {
                                    viewModel.prepositionOptions[prep]?.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option,
                                                    modifier = Modifier.padding(start = 16.dp),
                                                    fontSize = 14.sp,
                                                )
                                            },
                                            onClick = {
                                                viewModel.selectedPreposition = prep
                                                viewModel.selectedOption = option
                                                viewModel.isPrepositionMenuVisible = false
                                                if (viewModel.isInsertingByMenu) {
                                                    viewModel.onPrepositionSelected(prep, option)
                                                } else {
                                                    viewModel.addPreposition(prep, option)
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    modifier = Modifier.padding(15.dp),
                    onClick = {
                        viewModel.taskSentence = viewModel.getFinalSentenceString()
                        viewModel.isBuildCardExpanded = false
                    },
                ) {
                    Text("Done")
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = viewModel.getFinalSentenceString(),
                    style = MaterialTheme.typography.bodyLarge,
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
