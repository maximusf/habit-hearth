package com.project.habithearth.ui.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.project.habithearth.BuildConfig
import com.project.habithearth.ui.state.GameUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StoryPage(
    val text: String,
    val choices: List<String> = emptyList(),
)

data class StoryUiState(
    val chapterTitle: String = "Chapter 1: Survival",
    val pages: List<StoryPage> = emptyList(),
    val currentPageIndex: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasShownOpening: Boolean = false,
    val hasShownEnding: Boolean = false,
    val pendingStoryText: String? = null,
) {
    val currentPage: StoryPage? get() = pages.getOrNull(currentPageIndex)
    val canGoBack: Boolean get() = currentPageIndex > 0
    val canGoForward: Boolean get() = currentPageIndex < pages.size - 1
    val isAtLastPage: Boolean get() = pages.isEmpty() || currentPageIndex == pages.size - 1
    val isOnChoicePage: Boolean get() = currentPage?.choices?.isNotEmpty() == true
}

class StoryViewModel : ViewModel() {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = BuildConfig.GEMINI_API_KEY,
    )

    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    private var preloadJob: Job? = null

    // Dummy game state — wire to real GameUiState later
    // 50% XP puts us in the choice window (40–60%) for testing
    private val dummyXpProgress = 0.50f
    private val dummyStrengthGems = 12
    private val dummyWisdomGems = 4
    private val dummyVitalityGems = 7
    private val dummySpiritGems = 3
    private val dummyCoins = 20

    fun beginStory(gameState: GameUiState) {
        if (_uiState.value.pages.isNotEmpty() || _uiState.value.isLoading) return
        generateNextPage(gameState)
    }

    fun goToNextPage(gameState: GameUiState) {
        val state = _uiState.value
        if (state.isLoading) return

        if (state.canGoForward) {
            _uiState.value = state.copy(currentPageIndex = state.currentPageIndex + 1)
            return
        }

        if (!state.hasShownEnding) generateNextPage(gameState)
    }

    fun goToPreviousPage() {
        val state = _uiState.value
        if (state.canGoBack) {
            _uiState.value = state.copy(currentPageIndex = state.currentPageIndex - 1)
        }
    }

    fun makeChoice(choice: String) {
        viewModelScope.launch {
            if (!hasGeminiApiKey()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Missing Gemini API key. Add GEMINI_API_KEY to local.properties.",
                )
                return@launch
            }

            preloadJob?.cancel()
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                pendingStoryText = null,
            )

            try {
                val prompt = """
                    Continue the village rebuilding story. The player chose: "$choice"

                    Previous story context: ${_uiState.value.currentPage?.text ?: ""}

                    Write a 100-150 word continuation showing the result of their choice.
                    Style: punchy, dramatic, with a touch of humor. Second person ("you").
                    Setting: a village recovering from a dragon attack, fighting the "Veil of Stagnation" (purple vines and fog).
                    Do not use markdown formatting.
                    Do not end with "What do you do?" or any choice prompt.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val text = response.text ?: "The veil swallowed your choice... try again."

                addPage(text)
                _uiState.value = _uiState.value.copy(isLoading = false)

                val xpPercent = (dummyXpProgress * 100).toInt()
                preloadNextSegment(getDominantCategory(), xpPercent)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error: ${e.localizedMessage}",
                )
            }
        }
    }

    private fun generateNextPage(gameState: GameUiState) {
        viewModelScope.launch {
            if (!hasGeminiApiKey()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Missing Gemini API key. Add GEMINI_API_KEY to local.properties.",
                )
                return@launch
            }

            val xpPercent = (dummyXpProgress * 100).toInt()
            val dominantCategory = getDominantCategory()

            val phase = when {
                xpPercent < 25 -> "beginning"
                xpPercent < 75 -> "middle"
                else -> "climax"
            }

            // Use pre-generated content if ready (middle phase only)
            val pending = _uiState.value.pendingStoryText
            if (pending != null && phase == "middle") {
                preloadJob?.cancel()
                val choices = if (xpPercent in 40..60 && !hasChoicePageAlready()) {
                    generateChoices(dominantCategory)
                } else {
                    emptyList()
                }
                addPage(pending, choices)
                _uiState.value = _uiState.value.copy(
                    pendingStoryText = null,
                    hasShownOpening = true,
                )
                if (choices.isEmpty()) preloadNextSegment(dominantCategory, xpPercent)
                return@launch
            }

            preloadJob?.cancel()
            _uiState.value = _uiState.value.copy(pendingStoryText = null)

            when (phase) {
                "beginning" -> {
                    addPage(CHAPTER_1_OPENING)
                    _uiState.value = _uiState.value.copy(hasShownOpening = true)
                    preloadNextSegment(dominantCategory, xpPercent)
                }
                "middle" -> {
                    _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                    try {
                        if (!_uiState.value.hasShownOpening) {
                            addPage(CHAPTER_1_OPENING)
                            _uiState.value = _uiState.value.copy(hasShownOpening = true)
                        }
                        val prompt = buildStoryPrompt(dominantCategory, xpPercent, "middle")
                        val response = generativeModel.generateContent(prompt)
                        val text = response.text ?: "The veil grows thicker... no tale emerged."
                        val choices = if (xpPercent in 40..60 && !hasChoicePageAlready()) {
                            generateChoices(dominantCategory)
                        } else {
                            emptyList()
                        }
                        addPage(text, choices)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasShownOpening = true,
                        )
                        if (choices.isEmpty()) preloadNextSegment(dominantCategory, xpPercent)
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Error: ${e.localizedMessage}",
                        )
                    }
                }
                "climax" -> {
                    _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                    try {
                        if (!_uiState.value.hasShownOpening) {
                            addPage(CHAPTER_1_OPENING)
                            _uiState.value = _uiState.value.copy(hasShownOpening = true)
                        }
                        val prompt = buildStoryPrompt(dominantCategory, xpPercent, "climax")
                        val response = generativeModel.generateContent(prompt)
                        val text = response.text ?: "The veil grows thicker... no tale emerged."
                        addPage(text)
                        addPage(CHAPTER_1_ENDING)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasShownOpening = true,
                            hasShownEnding = true,
                        )
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Error: ${e.localizedMessage}",
                        )
                    }
                }
            }
        }
    }

    private fun addPage(text: String, choices: List<String> = emptyList()) {
        val newPages = _uiState.value.pages + StoryPage(text = text, choices = choices)
        _uiState.value = _uiState.value.copy(
            pages = newPages,
            currentPageIndex = newPages.size - 1,
        )
    }

    private fun hasChoicePageAlready(): Boolean =
        _uiState.value.pages.any { it.choices.isNotEmpty() }

    private fun preloadNextSegment(dominantCategory: String, xpPercent: Int) {
        if (_uiState.value.hasShownEnding) return
        val nextPhase = if (xpPercent < 75) "middle" else "climax"
        preloadJob = viewModelScope.launch {
            try {
                val prompt = buildStoryPrompt(dominantCategory, xpPercent, nextPhase)
                val response = generativeModel.generateContent(prompt)
                val text = response.text ?: return@launch
                _uiState.value = _uiState.value.copy(pendingStoryText = text)
            } catch (e: Exception) {
                // Silent fail — on-demand generation is the fallback
            }
        }
    }

    private fun buildStoryPrompt(
        dominantCategory: String,
        xpPercent: Int,
        phase: String,
    ): String = """
        You are the narrator of "Habit Hearth," a village rebuilding story.

        Setting: A village destroyed by dragons. The "Veil of Stagnation" — purple vines
        and thick fog — creeps in whenever work stops. The player is the lead engineer
        tasked with rebuilding the village.

        Current game state:
        - Chapter phase: $phase ($xpPercent% XP earned toward the chapter goal)
        - Player's dominant activity category: $dominantCategory
        - Strength gems: $dummyStrengthGems, Wisdom gems: $dummyWisdomGems
        - Vitality gems: $dummyVitalityGems, Spirit gems: $dummySpiritGems
        - Coins: $dummyCoins
        - Tasks completed: 0 / 0

        Flavor the narrative based on the dominant category:
        - Strength: physical labor, hauling debris, forging tools, combat training
        - Wisdom: salvaging burnt books, studying old blueprints, calculating solutions
        - Vitality: restoring hot springs, tending gardens, healing villagers
        - Spirit: creating art, baking bread, inspiring the community with music

        Write a 150-200 word story segment for the "$phase" phase.
        Style: punchy, dramatic, and lightly comedic. Written in second person ("you").
        Do not use markdown formatting.
        Do not end with "What do you do?" or any choice prompt — choices are shown as separate UI buttons.
    """.trimIndent()

    private fun getDominantCategory(): String {
        val categories = mapOf(
            "Strength" to dummyStrengthGems,
            "Wisdom" to dummyWisdomGems,
            "Vitality" to dummyVitalityGems,
            "Spirit" to dummySpiritGems,
        )
        return categories.maxByOrNull { it.value }?.key ?: "Strength"
    }

    private fun generateChoices(dominantCategory: String): List<String> {
        return when (dominantCategory) {
            "Strength" -> listOf(
                "Use heavy shears to hack through the vines",
                "Drive the anchor bolts with a sledgehammer before the next quake",
                "Clear debris to open a path to the training grounds",
            )
            "Wisdom" -> listOf(
                "Mix a chemical solvent in the library to dissolve the vines",
                "Calculate the exact stress points to use fewer bolts with higher efficiency",
                "Decode an old blueprint for a vine-resistant coating",
            )
            "Vitality" -> listOf(
                "Redirect the hot springs steam to wilt the vines",
                "Brew a healing tonic to protect the villagers from the fog",
                "Plant resilient herbs to push back the veil",
            )
            "Spirit" -> listOf(
                "Organize a work song to keep the villagers' rhythm steady",
                "Paint murals on the buildings to ward off the veil",
                "Bake morale-boosting bread for the exhausted workers",
            )
            else -> listOf(
                "Use heavy shears to hack through the vines",
                "Mix a chemical solvent to dissolve the vines",
                "Redirect the hot springs steam to wilt the vines",
            )
        }
    }

    private fun hasGeminiApiKey(): Boolean = BuildConfig.GEMINI_API_KEY.isNotBlank()

    companion object {
        val CHAPTER_1_OPENING = """
            The village of Ashenveil is unrecognizable. Three days ago, a dragon named Morrath passed overhead — not in attack, just passing — and the heat from its wings was enough. Thatched roofs caught first. Then the granary. Then everything. You are Ashenveil's lead engineer, and right now you're standing in the ash looking at what used to be the town square.

            The Veil of Stagnation got here fast. Purple vines are already crawling up the blacksmith's wall, thick as rope. The fog pooled overnight in the low spots between buildings. You've seen the Veil before, on abandoned villages, but never on your own.

            The survivors are watching you. Thirty-one of them. They're not asking questions yet, but they will be soon. You take stock: the tools are mostly intact, a few buildings are salvageable, and the hot springs to the north are untouched. It's not nothing.

            You pull out your notebook. The Veil only advances when nothing is being done. So. You'd better get to work.
        """.trimIndent()

        val CHAPTER_1_ENDING = """
            The last vine shivers and goes still. Around the four main buildings, the Veil has pulled back — not gone, never gone, but far enough that you can see the stonework again. A cheer goes up from the workers. Someone starts banging a rhythm on an overturned barrel.

            You let yourself feel it for exactly four seconds.

            Then you crouch down and press your hand to the town square flagstones. They're warm. Not from the sun — from below. Morrath's heat baked the ground itself when it passed. The stone has been cooling for three days, contracting unevenly, and now you can feel it: a hairline fracture running under the square, east to west, corner to corner.

            The foundation is failing.

            You close your notebook. There's a lot more work to do.
        """.trimIndent()
    }
}
