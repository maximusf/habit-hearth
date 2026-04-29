package com.project.habithearth.ui.story

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.project.habithearth.HabitHearthApplication
import com.project.habithearth.data.story.Chapter1ProgressRepository
import com.project.habithearth.ui.state.GameUiState
import com.project.habithearth.ui.state.levelFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Top-level screen mode. The story flow opens at the chapter-select cover and
// only transitions to Reading on a Begin/Resume/Replay tap, so a returning
// player can revisit the cover, see chapter 2's "Coming Soon" placeholder, and
// pick which chapter to dive back into.
enum class StoryViewMode { Select, Reading }

// UI-side mirror of StoryChoice. Keeps the screen package free of any direct
// dependency on Chapter1 graph internals while still letting the screen render
// per-choice gem costs and category gates.
data class StoryChoiceUi(
    val label: String,
    val category: String? = null,
    val gemCost: Int = 0,
)

data class StoryPage(
    val nodeId: String,
    val text: String,
    val choices: List<StoryChoiceUi> = emptyList(),
    val backgroundAsset: String? = null,
    val characterAssets: List<String> = emptyList(),
)

data class StoryUiState(
    val chapterTitle: String = Chapter1.TITLE,
    val pages: List<StoryPage> = emptyList(),
    val currentPageIndex: Int = 0,
    val errorMessage: String? = null,
    val hasShownEnding: Boolean = false,
    val isLoading: Boolean = false,
    // Decisions, keyed by the intro node id that hosted the choice page.
    // Once recorded, the screen renders the picked option highlighted and all
    // other options as disabled — choices are permanent.
    val madeChoices: Map<String, String> = emptyMap(),
    // Populated when goToNextPage is blocked by a level requirement on the
    // upcoming intro node. Cleared on the next successful advance.
    val lockedReason: String? = null,
    // Mirror of lockedReason for use as an effect key — when the player's
    // level catches up to this number, the screen auto-advances.
    val lockedRequiredLevel: Int? = null,
    // True once hydrate has run, so the screen can avoid flashing the begin
    // button before disk-loaded pages arrive on cold start.
    val isHydrated: Boolean = false,
    // Which top-level surface to render. Cold launch always opens at Select
    // so the player can see the chapter list (and any future-chapter
    // placeholders) before re-entering the story.
    val viewMode: StoryViewMode = StoryViewMode.Select,
) {
    val currentPage: StoryPage? get() = pages.getOrNull(currentPageIndex)
    val canGoBack: Boolean get() = currentPageIndex > 0
    val canGoForward: Boolean get() = currentPageIndex < pages.size - 1
    val isOnChoicePage: Boolean get() = currentPage?.choices?.isNotEmpty() == true
    val isLocked: Boolean get() = lockedReason != null
    val isAtLastPage: Boolean get() = pages.isEmpty() || currentPageIndex == pages.size - 1
}

class StoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: Chapter1ProgressRepository =
        (application as HabitHearthApplication).chapter1ProgressRepository

    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    init {
        // Hydrate from disk on first construction. Saved snapshots use only
        // node ids; the StoryPage list is rebuilt from Chapter1.nodes so the
        // chapter author can edit prose without invalidating saves.
        viewModelScope.launch {
            val snap = repo.load()
            _uiState.value = if (snap.visitedNodeIds.isEmpty()) {
                _uiState.value.copy(isHydrated = true)
            } else {
                rebuildState(snap)
            }
        }
        // Listen for the debug "Reset progress" signal. The game-state VM
        // wipes its own state plus the chapter repo, then emits, so by the
        // time we see it the on-disk snapshot is already empty.
        viewModelScope.launch {
            getApplication<HabitHearthApplication>().debugResetEvents.collect {
                _uiState.value = StoryUiState(isHydrated = true, viewMode = StoryViewMode.Select)
            }
        }
    }

    fun beginStory(gameState: GameUiState) {
        // Begin handles both first-launch (no pages yet) and resume after the
        // player navigated back to the chapter cover. In both cases we end up
        // in Reading mode; only first-launch needs to seed the start node.
        if (_uiState.value.pages.isEmpty()) {
            tryAppendNode(Chapter1.START_ID, gameState)
        }
        _uiState.value = _uiState.value.copy(viewMode = StoryViewMode.Reading)
    }

    /** Resume an in-progress chapter without seeding. Used by the chapter card. */
    fun resumeStory() {
        _uiState.value = _uiState.value.copy(viewMode = StoryViewMode.Reading)
    }

    /** Return to the chapter-select cover. Pages stay so Resume picks back up. */
    fun goToChapterSelect() {
        _uiState.value = _uiState.value.copy(viewMode = StoryViewMode.Select)
        persist()
    }

    // Wipe pages, locked-in decisions, and ending flags so the player can run
    // the chapter again. Gem costs were never consumed (only gated), so there's
    // no balance to refund. Surfaced from the screen as a "Restart Chapter"
    // button on the TBC page or "Replay" on the chapter card.
    fun restartChapter(gameState: GameUiState) {
        _uiState.value = StoryUiState(isHydrated = true, viewMode = StoryViewMode.Reading)
        tryAppendNode(Chapter1.START_ID, gameState)
    }

    fun goToNextPage(gameState: GameUiState) {
        val state = _uiState.value
        if (state.canGoForward) {
            _uiState.value = state.copy(
                currentPageIndex = state.currentPageIndex + 1,
                lockedReason = null,
                lockedRequiredLevel = null,
            )
            persist()
            return
        }
        val current = state.currentPage ?: return
        val node = Chapter1.node(current.nodeId) ?: return
        val nextId = node.nextNodeId ?: return
        tryAppendNode(nextId, gameState)
    }

    fun goToPreviousPage() {
        val state = _uiState.value
        if (state.canGoBack) {
            _uiState.value = state.copy(
                currentPageIndex = state.currentPageIndex - 1,
                lockedReason = null,
                lockedRequiredLevel = null,
            )
            persist()
        }
    }

    fun makeChoice(choice: String, gameState: GameUiState) {
        val state = _uiState.value
        val current = state.currentPage ?: return
        val node = Chapter1.node(current.nodeId) ?: return
        if (state.madeChoices.containsKey(node.id)) return
        val picked = node.choices.firstOrNull { it.label == choice } ?: return
        if (!canAfford(picked.category, picked.gemCost, gameState)) return

        val trimmed = state.pages.take(state.currentPageIndex + 1)
        _uiState.value = state.copy(
            pages = trimmed,
            currentPageIndex = trimmed.lastIndex,
            madeChoices = state.madeChoices + (node.id to choice),
        )
        tryAppendNode(picked.nextNodeId, gameState)
    }

    private fun canAfford(category: String?, gemCost: Int, gameState: GameUiState): Boolean {
        if (category == null || gemCost <= 0) return true
        val available = when (category) {
            Chapter1.CATEGORY_STRENGTH -> gameState.strengthGems
            Chapter1.CATEGORY_WISDOM -> gameState.wisdomGems
            Chapter1.CATEGORY_VITALITY -> gameState.vitalityGems
            Chapter1.CATEGORY_SPIRIT -> gameState.spiritGems
            else -> 0
        }
        return available >= gemCost
    }

    private fun tryAppendNode(nodeId: String, gameState: GameUiState) {
        val node = Chapter1.node(nodeId) ?: return
        val playerLevel = levelFor(gameState.totalXp)
        if (playerLevel < node.requiredLevel) {
            _uiState.value = _uiState.value.copy(
                lockedReason = "Reach Level ${node.requiredLevel} to continue the story.",
                lockedRequiredLevel = node.requiredLevel,
            )
            persist()
            return
        }
        appendNode(node)
    }

    private fun appendNode(node: StoryNode) {
        val mainPage = StoryPage(
            nodeId = node.id,
            text = node.text,
            choices = node.choices.map {
                StoryChoiceUi(label = it.label, category = it.category, gemCost = it.gemCost)
            },
            backgroundAsset = node.backgroundAsset,
            characterAssets = node.characterAssets,
        )

        if (node.id == "s_tbc") {
            val state = _uiState.value
            val dominant = dominantCategory(state.madeChoices)
            val diaryPage = StoryPage(
                nodeId = "s_diary",
                text = Chapter1.diaryFor(dominant),
                backgroundAsset = node.backgroundAsset,
                characterAssets = node.characterAssets,
            )
            val newPages = state.pages + diaryPage + mainPage
            _uiState.value = state.copy(
                pages = newPages,
                currentPageIndex = state.pages.size,
                hasShownEnding = true,
                lockedReason = null,
                lockedRequiredLevel = null,
            )
            persist()
            return
        }

        val newPages = _uiState.value.pages + mainPage
        _uiState.value = _uiState.value.copy(
            pages = newPages,
            currentPageIndex = newPages.size - 1,
            hasShownEnding = _uiState.value.hasShownEnding || node.isEnding,
            lockedReason = null,
            lockedRequiredLevel = null,
        )
        persist()
    }

    private fun dominantCategory(picks: Map<String, String>): String? {
        if (picks.isEmpty()) return null
        val counts = mutableMapOf<String, Int>()
        for ((nodeId, label) in picks) {
            val node = Chapter1.node(nodeId) ?: continue
            val choice = node.choices.firstOrNull { it.label == label } ?: continue
            val cat = choice.category ?: continue
            counts[cat] = (counts[cat] ?: 0) + 1
        }
        if (counts.isEmpty()) return null
        val max = counts.values.max()
        val leaders = counts.filter { it.value == max }
        return if (leaders.size == 1) leaders.keys.first() else null
    }

    // Reconstructs the StoryPage list and current index from a saved snapshot.
    // Diary text is regenerated from the saved choices so a content edit to
    // Chapter1.diaryFor() is reflected on the next launch even for old saves.
    private fun rebuildState(snap: Chapter1ProgressRepository.Snapshot): StoryUiState {
        val rebuiltPages = snap.visitedNodeIds.map { id ->
            if (id == "s_diary") {
                // Diary is dynamic: rebuilt against current choices, with the
                // s_tbc node's art reused (matches appendNode behavior).
                val tbc = Chapter1.node("s_tbc")
                StoryPage(
                    nodeId = "s_diary",
                    text = Chapter1.diaryFor(dominantCategory(snap.madeChoices)),
                    backgroundAsset = tbc?.backgroundAsset,
                    characterAssets = tbc?.characterAssets.orEmpty(),
                )
            } else {
                val node = Chapter1.node(id)
                if (node == null) {
                    // Unknown id (mid-flight chapter content rename). Fall back
                    // to a placeholder page so the pagination stays consistent
                    // rather than silently dropping the entry.
                    StoryPage(nodeId = id, text = "")
                } else {
                    StoryPage(
                        nodeId = node.id,
                        text = node.text,
                        choices = node.choices.map {
                            StoryChoiceUi(it.label, it.category, it.gemCost)
                        },
                        backgroundAsset = node.backgroundAsset,
                        characterAssets = node.characterAssets,
                    )
                }
            }
        }
        val safeIndex = snap.currentPageIndex.coerceIn(0, (rebuiltPages.size - 1).coerceAtLeast(0))
        return StoryUiState(
            pages = rebuiltPages,
            currentPageIndex = safeIndex,
            madeChoices = snap.madeChoices,
            hasShownEnding = snap.hasShownEnding,
            isHydrated = true,
        )
    }

    private fun persist() {
        // Don't write back during the initial hydrate pass: until isHydrated
        // flips true, _uiState may still be the empty default and would clobber
        // a real save on disk.
        if (!_uiState.value.isHydrated) return
        val state = _uiState.value
        viewModelScope.launch {
            repo.save(
                Chapter1ProgressRepository.Snapshot(
                    visitedNodeIds = state.pages.map { it.nodeId },
                    madeChoices = state.madeChoices,
                    currentPageIndex = state.currentPageIndex,
                    hasShownEnding = state.hasShownEnding,
                ),
            )
        }
    }
}
