package com.project.habithearth.ui.story

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
            onBegin = { storyViewModel.beginStory(gameState) },
            onNext = { storyViewModel.goToNextPage(gameState) },
            onPrevious = { storyViewModel.goToPreviousPage() },
            onChoice = { storyViewModel.makeChoice(it) },
            modifier = modifier,
        )
    } else {
        PortraitStoryLayout(
            storyState = storyState,
            onBegin = { storyViewModel.beginStory(gameState) },
            onNext = { storyViewModel.goToNextPage(gameState) },
            onPrevious = { storyViewModel.goToPreviousPage() },
            onChoice = { storyViewModel.makeChoice(it) },
            modifier = modifier,
        )
    }
}

// ── Portrait: vertical stack, page text scrollable in a fixed card ──

@Composable
private fun PortraitStoryLayout(
    storyState: StoryUiState,
    onBegin: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onChoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = storyState.chapterTitle,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

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

        Spacer(Modifier.height(12.dp))

        // Page text — fixed weight, scrollable inside
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            if (storyState.pages.isEmpty() && !storyState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Your story awaits...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(16.dp),
                ) {
                    storyState.currentPage?.let { page ->
                        Text(
                            text = page.text,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    if (storyState.isOnChoicePage) {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "What do you do?",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        storyState.currentPage?.choices?.forEach { choice ->
                            OutlinedButton(
                                onClick = { onChoice(choice) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !storyState.isLoading,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                ),
                            ) {
                                Text(
                                    text = choice,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Status row: loading or error
        if (storyState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
        }
        storyState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
        }

        // Bottom controls: begin / navigation (choices live inside the card now)
        if (storyState.pages.isEmpty()) {
            Button(
                onClick = onBegin,
                enabled = !storyState.isLoading,
            ) {
                Text(if (storyState.isLoading) "The tale unfolds..." else "Begin the Story")
            }
        } else if (!storyState.isOnChoicePage) {
            PageNavigation(
                storyState = storyState,
                onPrevious = onPrevious,
                onNext = onNext,
            )
        }
    }
}

// ── Landscape: book-style split ──

@Composable
private fun LandscapeStoryLayout(
    storyState: StoryUiState,
    onBegin: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onChoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Left page: title + illustration + page indicator + prev arrow
        Column(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = storyState.chapterTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
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
            }

            // Page indicator + prev button at bottom of left page
            if (storyState.pages.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    IconButton(
                        onClick = onPrevious,
                        enabled = storyState.canGoBack && !storyState.isLoading,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous page",
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${storyState.currentPageIndex + 1} / ${storyState.pages.size}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            if (storyState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        }

        // Right page: story text (scrollable) + choices or next arrow
        Card(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                // Story text — scrollable, takes all space above controls
                Box(modifier = Modifier.weight(1f)) {
                    if (storyState.pages.isEmpty() && !storyState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Your story awaits...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        val scroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scroll),
                        ) {
                            storyState.currentPage?.let { page ->
                                Text(
                                    text = page.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            if (storyState.isOnChoicePage) {
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "What do you do?",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(8.dp))
                                storyState.currentPage?.choices?.forEach { choice ->
                                    OutlinedButton(
                                        onClick = { onChoice(choice) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        enabled = !storyState.isLoading,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                        ),
                                        border = BorderStroke(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colorScheme.outline,
                                        ),
                                    ) {
                                        Text(
                                            text = choice,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                storyState.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // Bottom controls on right page (choices live in the scroll area now)
                when {
                    storyState.pages.isEmpty() -> {
                        Button(
                            onClick = onBegin,
                            enabled = !storyState.isLoading,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(if (storyState.isLoading) "The tale unfolds..." else "Begin the Story")
                        }
                    }
                    !storyState.isOnChoicePage -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            IconButton(
                                onClick = onNext,
                                enabled = !storyState.isLoading && !storyState.hasShownEnding,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next page",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Shared navigation row for portrait ──

@Composable
private fun PageNavigation(
    storyState: StoryUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = storyState.canGoBack && !storyState.isLoading,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous page",
            )
        }

        Text(
            text = "${storyState.currentPageIndex + 1} / ${storyState.pages.size}",
            style = MaterialTheme.typography.labelLarge,
        )

        IconButton(
            onClick = onNext,
            enabled = !storyState.isLoading && !storyState.hasShownEnding,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next page",
            )
        }
    }
}
