package com.project.habithearth.ui.story

import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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

    // Auto-retry advancement when a level lock is active and the player has
    // since leveled up. Without this, the forward arrow stays disabled until
    // the user manually fiddles with nav, which earlier feedback flagged as
    // confusing. Effect re-runs whenever totalXp or the gating threshold
    // change so it doesn't fire spuriously when the player isn't gated.
    LaunchedEffect(storyState.lockedRequiredLevel, gameState.totalXp) {
        val gate = storyState.lockedRequiredLevel
        if (gate != null
            && com.project.habithearth.ui.state.levelFor(gameState.totalXp) >= gate
        ) {
            storyViewModel.goToNextPage(gameState)
        }
    }

    if (storyState.isHydrated && storyState.viewMode == StoryViewMode.Select) {
        ChapterSelectScreen(
            storyState = storyState,
            onBegin = { storyViewModel.beginStory(gameState) },
            onResume = { storyViewModel.resumeStory() },
            onReplay = { storyViewModel.restartChapter(gameState) },
            modifier = modifier,
        )
        return
    }

    if (isLandscape) {
        LandscapeStoryLayout(
            storyState = storyState,
            gameState = gameState,
            onNext = { storyViewModel.goToNextPage(gameState) },
            onPrevious = { storyViewModel.goToPreviousPage() },
            onChoice = { storyViewModel.makeChoice(it, gameState) },
            onRestart = { storyViewModel.restartChapter(gameState) },
            onChaptersClick = { storyViewModel.goToChapterSelect() },
            modifier = modifier,
        )
    } else {
        PortraitStoryLayout(
            storyState = storyState,
            gameState = gameState,
            onNext = { storyViewModel.goToNextPage(gameState) },
            onPrevious = { storyViewModel.goToPreviousPage() },
            onChoice = { storyViewModel.makeChoice(it, gameState) },
            onRestart = { storyViewModel.restartChapter(gameState) },
            onChaptersClick = { storyViewModel.goToChapterSelect() },
            modifier = modifier,
        )
    }
}

// Returns the gem balance the player holds for the given category, or null
// for the "free" choice path. Drives choice-button enabled state and the
// inline cost label.
private fun gemsFor(category: String?, gameState: GameUiState): Int? = when (category) {
    Chapter1.CATEGORY_STRENGTH -> gameState.strengthGems
    Chapter1.CATEGORY_WISDOM -> gameState.wisdomGems
    Chapter1.CATEGORY_VITALITY -> gameState.vitalityGems
    Chapter1.CATEGORY_SPIRIT -> gameState.spiritGems
    else -> null
}

// ── Portrait: vertical stack, page text scrollable in a fixed card ──

@Composable
private fun PortraitStoryLayout(
    storyState: StoryUiState,
    gameState: GameUiState,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onChoice: (String) -> Unit,
    onRestart: () -> Unit,
    onChaptersClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Title row with a leading "Chapters" back link so the player can
        // pop back to the chapter cover at any time. Title stays centered;
        // the link sits in the leading slot of a SpaceBetween row, with a
        // matching invisible spacer on the trailing side to keep the title
        // visually centered in the row.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChaptersBackButton(onClick = onChaptersClick)
            Text(
                text = storyState.chapterTitle,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(72.dp))
        }

        Spacer(Modifier.height(12.dp))

        StoryIllustration(
            backgroundAsset = storyState.currentPage?.backgroundAsset,
            characterAssets = storyState.currentPage?.characterAssets.orEmpty(),
        )

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
                // Reset scroll to top whenever the page changes so a reader
                // mid-paragraph on page N starts fresh at the top of page N+1
                // (or N-1) after using the nav arrows.
                LaunchedEffect(storyState.currentPageIndex) {
                    scroll.scrollTo(0)
                }
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
                        ChoiceList(
                            storyState = storyState,
                            gameState = gameState,
                            onChoice = onChoice,
                            compact = false,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    if (storyState.currentPage?.nodeId == "s_tbc") {
                        Spacer(Modifier.height(20.dp))
                        TbcCardFooter(onRestart = onRestart, compact = false)
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
        storyState.lockedReason?.let { reason ->
            LockedBanner(reason)
            Spacer(Modifier.height(8.dp))
        }
        // Restart + Chapter 2 teaser live inside the page card on the TBC
        // page so they share the scroll, matching the look of the choice list
        // and saving vertical space on landscape.

        // Reading mode is reached only after a chapter is selected, so pages
        // is always non-empty here; the legacy begin button has been folded
        // into ChapterSelectScreen.
        if (storyState.pages.isNotEmpty()) {
            // Always show navigation once the story has started so the player
            // can read prior pages mid-decision. Forward stays disabled on a
            // fresh decision page (no advance available); Back works as long
            // as there's prior history.
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
    gameState: GameUiState,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onChoice: (String) -> Unit,
    onRestart: () -> Unit,
    onChaptersClick: () -> Unit,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChaptersBackButton(onClick = onChaptersClick)
                    Text(
                        text = storyState.chapterTitle,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(72.dp))
                }

                Spacer(Modifier.height(8.dp))

                StoryIllustration(
                    backgroundAsset = storyState.currentPage?.backgroundAsset,
                    characterAssets = storyState.currentPage?.characterAssets.orEmpty(),
                )
            }

            // Page indicator + prev/next live on the right card's bottom row
            // (PageNavigation) in landscape, so both arrows are reachable in
            // one place. Left column keeps only the title and illustration.

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
                        // Same reset rule as portrait: snap to the top of the
                        // page whenever currentPageIndex changes.
                        LaunchedEffect(storyState.currentPageIndex) {
                            scroll.scrollTo(0)
                        }
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
                                ChoiceList(
                                    storyState = storyState,
                                    gameState = gameState,
                                    onChoice = onChoice,
                                    compact = true,
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            if (storyState.currentPage?.nodeId == "s_tbc") {
                                Spacer(Modifier.height(16.dp))
                                TbcCardFooter(onRestart = onRestart, compact = true)
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
                storyState.lockedReason?.let { reason ->
                    LockedBanner(reason)
                    Spacer(Modifier.height(4.dp))
                }
                // Restart + teaser live inside the page card on the TBC page
                // (see TbcCardFooter) to share the scroll area with the prose.

                // Reading mode always has at least one page (Begin/Resume add
                // it before flipping the mode), so the navigation row is the
                // only bottom control needed here.
                if (storyState.pages.isNotEmpty()) {
                    PageNavigation(
                        storyState = storyState,
                        onPrevious = onPrevious,
                        onNext = onNext,
                    )
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
            // Forward is allowed whenever there's history to scrub through. Fresh
// advancement is gated: past the ending, undecided choice pages, and
// level-locked pages. The level-lock auto-clears via a LaunchedEffect that
// retries advancement once the player's level catches up, so the button
// stays visually disabled while the gate is active.
enabled = !storyState.isLoading
    && !storyState.isLocked
    && (storyState.canGoForward
        || (!storyState.hasShownEnding && !storyState.isOnChoicePage)),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next page",
            )
        }
    }
}

// Square art slot, fixed dp so portrait and landscape render the same physical
// box. Background is drawn full-bleed; up to four character sprites are laid
// out across the bottom edge, scaled to a fraction of the slot height so they
// read as figures in front of the scene rather than overpowering the art.
private val StoryIllustrationSize = 280.dp

// Backgrounds ship at 1024x1024; downsampling to ~768 px keeps GPU memory low
// without visible quality loss at the 280 dp slot size on hdpi/xhdpi devices.
private const val BackgroundMaxEdgePx = 768
private const val CharacterMaxEdgePx = 512

@Composable
private fun StoryIllustration(
    backgroundAsset: String?,
    characterAssets: List<String>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val background = remember(backgroundAsset) {
        backgroundAsset?.let {
            decodeStoryAsset(context, it, BackgroundMaxEdgePx)
        }
    }
    // Cap character row at 4 sprites; spec sections never list more than that
    // and the illustration box runs out of horizontal room beyond it.
    val characters = remember(characterAssets) {
        characterAssets.take(4).mapNotNull { path ->
            decodeStoryAsset(context, path, CharacterMaxEdgePx)
        }
    }

    Box(
        modifier = modifier
            .size(StoryIllustrationSize)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (background != null) {
            Image(
                bitmap = background,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = "Illustration",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (characters.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Bottom,
            ) {
                characters.forEach { sprite ->
                    Image(
                        bitmap = sprite,
                        contentDescription = null,
                        modifier = Modifier
                            .height(StoryIllustrationSize * 0.55f)
                            .width(StoryIllustrationSize * 0.32f),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }
}

private fun decodeStoryAsset(
    context: android.content.Context,
    assetPath: String,
    maxEdgePx: Int,
): ImageBitmap? = runCatching {
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

private fun sampleSizeForMaxEdge(width: Int, height: Int, maxEdgePx: Int): Int {
    val longest = maxOf(width, height)
    if (longest <= maxEdgePx) return 1
    var sample = 1
    while (longest / sample > maxEdgePx) {
        sample *= 2
    }
    return sample
}

// Renders the choice block for the current page. Each choice's enabled state
// folds together: chapter-loading flag, whether *some* choice has already been
// locked in for this decision, and whether the player can afford this option.
// Once a decision has been made, the picked option is highlighted and the rest
// are visibly muted but kept on screen so the player remembers their pick.
@Composable
private fun ChoiceList(
    storyState: StoryUiState,
    gameState: GameUiState,
    onChoice: (String) -> Unit,
    compact: Boolean,
) {
    val page = storyState.currentPage ?: return
    val pickedLabel = storyState.madeChoices[page.nodeId]
    val locked = pickedLabel != null

    Text(
        text = if (locked) "Your decision:" else "What do you do?",
        style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(if (compact) 8.dp else 12.dp))

    page.choices.forEach { choice ->
        val isPicked = pickedLabel == choice.label
        val available = gemsFor(choice.category, gameState)
        val canAfford = choice.gemCost <= 0 || (available ?: 0) >= choice.gemCost
        val enabled = !storyState.isLoading && !locked && canAfford

        // Picked choice keeps the primary outline color so the player can see
        // their commitment; unpicked options after a lock fade to the muted
        // outlineVariant tone.
        val borderColor = when {
            isPicked -> MaterialTheme.colorScheme.primary
            locked -> MaterialTheme.colorScheme.outlineVariant
            !canAfford -> MaterialTheme.colorScheme.outlineVariant
            else -> MaterialTheme.colorScheme.outline
        }
        val contentColor = when {
            isPicked -> MaterialTheme.colorScheme.primary
            locked || !canAfford -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurface
        }

        OutlinedButton(
            onClick = { onChoice(choice.label) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (compact) 2.dp else 4.dp),
            shape = RoundedCornerShape(8.dp),
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = contentColor,
                disabledContentColor = contentColor,
            ),
            border = BorderStroke(
                width = if (isPicked) 2.dp else 1.5.dp,
                color = borderColor,
            ),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = choice.label,
                    style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Cost / state line. Free choices stay clean; categorized
                // choices show "<cat> x N" plus an affordability or locked
                // tag so the player understands why a button is disabled.
                val tag = buildChoiceTag(
                    choice = choice,
                    available = available,
                    isPicked = isPicked,
                    locked = locked,
                    canAfford = canAfford,
                )
                if (tag != null) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = if (isPicked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun buildChoiceTag(
    choice: StoryChoiceUi,
    available: Int?,
    isPicked: Boolean,
    locked: Boolean,
    canAfford: Boolean,
): String? {
    val parts = mutableListOf<String>()
    if (choice.category != null && choice.gemCost > 0) {
        parts += "${choice.category} x ${choice.gemCost} (have ${available ?: 0})"
    }
    when {
        isPicked -> parts += "Chosen"
        locked -> parts += "Locked"
        !canAfford -> parts += "Not enough"
    }
    return if (parts.isEmpty()) null else parts.joinToString(" • ")
}

// Asset paths for chapter cards. Chapter 1 uses the dramatic destroyedVillage
// shot since that's the opener's tone. Chapter 2 art doesn't exist yet, so
// builtVillage stands in dimmed under a "Coming Soon" overlay until proper
// cover art lands.
private const val Chapter1CoverAsset = "images/backgrounds/destroyedVillage.png"
private const val Chapter2CoverAsset = "images/backgrounds/builtVillage.png"

@Composable
private fun ChaptersBackButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 10.dp,
            vertical = 4.dp,
        ),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text("Chapters", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ChapterSelectScreen(
    storyState: StoryUiState,
    onBegin: () -> Unit,
    onResume: () -> Unit,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ch1InProgress = storyState.pages.isNotEmpty() && !storyState.hasShownEnding
    val ch1Complete = storyState.hasShownEnding
    val ch1ButtonLabel = when {
        ch1Complete -> "Replay"
        ch1InProgress -> "Resume"
        else -> "Begin the Story"
    }
    val ch1OnClick: () -> Unit = when {
        ch1Complete -> onReplay
        ch1InProgress -> onResume
        else -> onBegin
    }
    val ch1Status: String = when {
        ch1Complete -> "Completed"
        ch1InProgress -> "In progress"
        else -> "Not started"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Chapters",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))

        ChapterCard(
            coverAsset = Chapter1CoverAsset,
            chapterLabel = "Chapter 1",
            title = "Survival",
            tagline = "Ashenveil is in ashes. The Veil is creeping in. Pick up your tools.",
            statusLabel = ch1Status,
            primaryActionLabel = ch1ButtonLabel,
            onPrimaryAction = ch1OnClick,
            secondaryActionLabel = if (ch1InProgress) "Restart" else null,
            onSecondaryAction = if (ch1InProgress) onReplay else null,
            locked = false,
        )

        Spacer(Modifier.height(16.dp))

        ChapterCard(
            coverAsset = Chapter2CoverAsset,
            chapterLabel = "Chapter 2",
            title = "Coming Soon",
            tagline = "The ground is shifting. The next chapter releases later.",
            statusLabel = "Locked",
            primaryActionLabel = "Coming Soon",
            onPrimaryAction = {},
            secondaryActionLabel = null,
            onSecondaryAction = null,
            locked = true,
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ChapterCard(
    coverAsset: String,
    chapterLabel: String,
    title: String,
    tagline: String,
    statusLabel: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String?,
    onSecondaryAction: (() -> Unit)?,
    locked: Boolean,
) {
    val context = LocalContext.current
    val cover = remember(coverAsset) {
        decodeStoryAsset(context, coverAsset, BackgroundMaxEdgePx)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column {
            // Thumbnail. Square-ish 16:10 crop reads as a "chapter cover"
            // without dominating the screen on small phones. Locked
            // chapters get an extra dark scrim and Coming Soon banner.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
            ) {
                if (cover != null) {
                    Image(
                        bitmap = cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                    )
                }
                // Bottom gradient for label legibility on busy art.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = if (locked) 0.7f else 0.55f),
                                ),
                            ),
                        ),
                )
                if (locked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Coming Soon",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = chapterLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Status: $statusLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onPrimaryAction,
                    enabled = !locked,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(primaryActionLabel)
                }
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(secondaryActionLabel)
                    }
                }
            }
        }
    }
}

// End-of-chapter footer that lives inside the page card on the TBC page so it
// shares the scroll with the prose. Reveal-on-scroll: only appears once the
// reader has scrolled past the prose, which keeps the "Coming Soon" line and
// restart action from spoiling the closing beat. Restart is rendered as an
// outlined choice-style button to match the locked decision look elsewhere
// in the chapter.
@Composable
private fun TbcCardFooter(
    onRestart: () -> Unit,
    compact: Boolean,
) {
    Text(
        text = "Chapter 2 — Coming Soon",
        style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
    OutlinedButton(
        onClick = onRestart,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 2.dp else 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.outline,
        ),
    ) {
        Text(
            text = "Restart Chapter",
            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

// Shown when the next intro node requires a higher level than the player has
// reached. Single line, accent-colored, stays in the bottom controls strip.
@Composable
private fun LockedBanner(reason: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            textAlign = TextAlign.Center,
        )
    }
}
