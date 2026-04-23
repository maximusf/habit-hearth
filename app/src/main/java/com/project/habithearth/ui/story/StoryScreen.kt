package com.project.habithearth.ui.story

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.habithearth.ui.state.GameUiState

@Composable
fun StoryScreen(
    gameState: GameUiState,
    modifier: Modifier = Modifier,
    storyViewModel: StoryViewModel = viewModel(),
) {
    val storyState by storyViewModel.uiState.collectAsState()
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        LandscapeStoryLayout(
            storyState = storyState,
            onGenerate = { storyViewModel.generateStory(gameState) },
            onChoice = { storyViewModel.makeChoice(it) },
            modifier = modifier,
        )
    } else {
        PortraitStoryLayout(
            storyState = storyState,
            onGenerate = { storyViewModel.generateStory(gameState) },
            onChoice = { storyViewModel.makeChoice(it) },
            modifier = modifier,
        )
    }
}

// ── Portrait: vertical stack, everything scrolls together ──

@Composable
private fun PortraitStoryLayout(
    storyState: StoryUiState,
    onGenerate: () -> Unit,
    onChoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Title
        Text(
            text = storyState.chapterTitle,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        // Placeholder image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Illustration",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))

        // Generate button
        Button(
            onClick = onGenerate,
            enabled = !storyState.isLoading,
        ) {
            Text(
                if (storyState.isLoading) "The tale unfolds..."
                else if (storyState.storyText.isEmpty()) "Begin the Story"
                else "Continue the Story"
            )
        }

        if (storyState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        storyState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // Story text card
        if (storyState.storyText.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text(
                    text = storyState.storyText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        // Choices
        if (storyState.choices.isNotEmpty()) {
            Text(
                text = "What do you do?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            storyState.choices.forEach { choice ->
                OutlinedButton(
                    onClick = { onChoice(choice) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(choice)
                }
            }
        }
    }
}

// ── Landscape: book-style split ──

@Composable
private fun LandscapeStoryLayout(
    storyState: StoryUiState,
    onGenerate: () -> Unit,
    onChoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val storyScroll = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Left page: Title + Image + Button
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = storyState.chapterTitle,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Illustration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onGenerate,
                enabled = !storyState.isLoading,
            ) {
                Text(
                    if (storyState.isLoading) "The tale unfolds..."
                    else if (storyState.storyText.isEmpty()) "Begin the Story"
                    else "Continue the Story"
                )
            }

            if (storyState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            storyState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        // Right page: Scrollable story text + choices
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(storyScroll)
                    .padding(16.dp),
            ) {
                if (storyState.storyText.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Your story will appear here...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Text(
                        text = storyState.storyText,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                if (storyState.choices.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "What do you do?",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    storyState.choices.forEach { choice ->
                        OutlinedButton(
                            onClick = { onChoice(choice) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(choice)
                        }
                    }
                }
            }
        }
    }
}