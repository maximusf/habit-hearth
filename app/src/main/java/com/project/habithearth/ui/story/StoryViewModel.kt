package com.project.habithearth.ui.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.project.habithearth.BuildConfig
import com.project.habithearth.ui.state.GameUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StoryUiState(
    val chapterTitle: String = "Chapter 1: Survival",
    val storyText: String = "",
    val isLoading: Boolean = false,
    val choices: List<String> = emptyList(),
    val errorMessage: String? = null,
)

class StoryViewModel : ViewModel() {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = BuildConfig.GEMINI_API_KEY,
    )

    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    fun generateStory(gameState: GameUiState) {
        viewModelScope.launch {
            if (!hasGeminiApiKey()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Missing Gemini API key. Add GEMINI_API_KEY to local.properties.",
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val dominantCategory = getDominantCategory(gameState)
                val xpPercent = (gameState.xpProgress * 100).toInt()
                val prompt = buildStoryPrompt(gameState, dominantCategory, xpPercent)

                val response = generativeModel.generateContent(prompt)
                val text = response.text ?: "The veil grows thicker... no tale emerged."

                val choices = if (xpPercent in 40..60) {
                    generateChoices(dominantCategory)
                } else {
                    emptyList()
                }

                _uiState.value = _uiState.value.copy(
                    storyText = text,
                    isLoading = false,
                    choices = choices,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error: ${e.localizedMessage}",
                )
            }
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

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                choices = emptyList(),
                errorMessage = null,
            )

            try {
                val prompt = """
                    Continue the village rebuilding story. The player chose: "$choice"

                    Previous story context: ${_uiState.value.storyText}

                    Write a 100-150 word continuation showing the result of their choice.
                    Style: punchy, dramatic, with a touch of humor. Second person ("you").
                    Setting: a village recovering from a dragon attack, fighting the "Veil of Stagnation" (purple vines and fog).
                    Do not use markdown formatting.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val text = response.text ?: "The veil swallowed your choice... try again."

                _uiState.value = _uiState.value.copy(
                    storyText = _uiState.value.storyText + "\n\n" + text,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error: ${e.localizedMessage}",
                )
            }
        }
    }

    private fun buildStoryPrompt(
        gameState: GameUiState,
        dominantCategory: String,
        xpPercent: Int,
    ): String {
        val phase = when {
            xpPercent < 25 -> "beginning"
            xpPercent < 75 -> "middle"
            else -> "climax"
        }

        val completedTasks = gameState.tasks.count { it.isCompleted }
        val totalTasks = gameState.tasks.size

        return """
            You are the narrator of "Habit Hearth," a village rebuilding story.

            Setting: A village destroyed by dragons. The "Veil of Stagnation" — purple vines
            and thick fog — creeps in whenever work stops. The player is the lead engineer
            tasked with rebuilding the village.

            Current game state:
            - Chapter phase: $phase ($xpPercent% XP earned toward the chapter goal)
            - Player's dominant activity category: $dominantCategory
            - Strength gems: ${gameState.strengthGems}, Wisdom gems: ${gameState.wisdomGems}
            - Vitality gems: ${gameState.vitalityGems}, Spirit gems: ${gameState.spiritGems}
            - Coins: ${gameState.coins}
            - Tasks completed: $completedTasks / $totalTasks

            Flavor the narrative based on the dominant category:
            - Strength: physical labor, hauling debris, forging tools, combat training
            - Wisdom: salvaging burnt books, studying old blueprints, calculating solutions
            - Vitality: restoring hot springs, tending gardens, healing villagers
            - Spirit: creating art, baking bread, inspiring the community with music

            Write a 150-200 word story segment for the "$phase" phase.
            Style: punchy, dramatic, and lightly comedic. Written in second person ("you").
            Do not use markdown formatting.
        """.trimIndent()
    }

    private fun getDominantCategory(gameState: GameUiState): String {
        val categories = mapOf(
            "Strength" to gameState.strengthGems,
            "Wisdom" to gameState.wisdomGems,
            "Vitality" to gameState.vitalityGems,
            "Spirit" to gameState.spiritGems,
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
}
