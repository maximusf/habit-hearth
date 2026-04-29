package com.project.habithearth.ui.tasks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.project.habithearth.model.HabitTask

/**
 * Holds ephemeral sentence-building state for [BuildSentenceCard] and [DifficultyCard].
 * No DataStore interaction — persistence is handled by [TaskEditorViewModel].
 *
 * Adapted from SentenceStarterViewModel in SentenceStarter.kt:
 *   - Organization enum removed; building selection lives in TaskMakerScreen
 *     using the existing defaultVillageBuildings() pattern.
 *   - [seedFromTask] added for edit-mode pre-filling.
 *   - [selectedDifficulty] maps 1:1 to rewardAmount when saving.
 */
class TaskSentenceViewModel : ViewModel() {

    var taskSentence by mutableStateOf("I will ")

    var sentenceSegments = mutableStateListOf<SentenceSegment>(
        SentenceSegment.FixedText("I will "),
        SentenceSegment.EditableVerb(""),
    )

    var isBuildCardExpanded by mutableStateOf(true)
    var isDifficultyCardExpanded by mutableStateOf(true)

    fun completeEditing() {
        taskSentence = getFinalSentenceString()
        isBuildCardExpanded = false
    }

    // ── Difficulty ────────────────────────────────────────────────────────────

    var isDifficultyMenuVisible by mutableStateOf(false)
    var selectedDifficulty by mutableStateOf(0)

    enum class Difficulty(val label: String) {
        ONE("1"), TWO("2"), THREE("3"), FOUR("4"), FIVE("5")
    }

    // ── Prepositions ──────────────────────────────────────────────────────────

    var isPrepositionMenuVisible by mutableStateOf(false)
    var expandedPrepositionInMenu by mutableStateOf<Preposition?>(null)
    var selectedPreposition by mutableStateOf<Preposition?>(null)
    var selectedOption by mutableStateOf("")
    var isInsertingByMenu by mutableStateOf(false)
    var editingPrepositionIndex by mutableStateOf<Int?>(null)

    enum class Preposition(val label: String) {
        FOR("For"), IN("In"), BY("By"), WITH("With")
    }

    val prepositionOptions = mapOf(
        Preposition.FOR to listOf("Duration", "Distance", "Event", "Person", "Other"),
        Preposition.IN to listOf("Location", "Timeframe", "Condition", "Other"),
        Preposition.BY to listOf("Method", "Deadline", "Other"),
        Preposition.WITH to listOf("Tool", "Person", "Other"),
    )

    fun addPreposition(prep: Preposition, option: String) {
        val index = editingPrepositionIndex
        if (index != null) {
            updatePreposition(index, prep, option)
            editingPrepositionIndex = null
            return
        }
        sentenceSegments.add(SentenceSegment.PrepositionChip(prep, option))
        addAccompanyingFields(prep, option)
    }

    fun onPrepositionSelected(prep: Preposition, option: String) {
        val index = editingPrepositionIndex!!
        if (isInsertingByMenu) {
            sentenceSegments.add(index - 1, SentenceSegment.PrepositionChip(prep, option))
            addAccompanyingFields(prep, option, index)
        } else {
            deletePrepositionBlock(index)
            sentenceSegments.add(index, SentenceSegment.PrepositionChip(prep, option))
            addAccompanyingFields(prep, option, index + 1)
        }
        isPrepositionMenuVisible = false
        isInsertingByMenu = false
        editingPrepositionIndex = -1
    }

    fun deletePrepositionBlock(index: Int) {
        val range = getPrepositionBlockRange(index)
        for (i in range.last downTo range.first) {
            sentenceSegments.removeAt(i)
        }
    }

    fun deleteRemainder(index: Int) {
        while (sentenceSegments.size > index) {
            sentenceSegments.removeAt(index)
        }
    }

    private fun getPrepositionBlockRange(index: Int): IntRange {
        if (index < 0 || index >= sentenceSegments.size ||
            sentenceSegments[index] !is SentenceSegment.PrepositionChip
        ) return IntRange.EMPTY
        var endIndex = index + 1
        while (endIndex < sentenceSegments.size &&
            sentenceSegments[endIndex] !is SentenceSegment.PrepositionChip
        ) {
            endIndex++
        }
        return IntRange(index, endIndex - 1)
    }

    private fun updatePreposition(index: Int, prep: Preposition, option: String) {
        val oldRange = getPrepositionBlockRange(index)
        val oldSegments = oldRange.map { sentenceSegments[it] }
        val oldInputValue =
            oldSegments.filterIsInstance<SentenceSegment.InputField>().firstOrNull()?.value ?: ""

        for (i in oldRange.last downTo oldRange.first) {
            sentenceSegments.removeAt(i)
        }
        sentenceSegments.add(index, SentenceSegment.PrepositionChip(prep, option))
        addAccompanyingFields(prep, option, index + 1)

        val newRange = getPrepositionBlockRange(index)
        val newInputIndex = sentenceSegments.subList(newRange.first, newRange.last + 1)
            .indexOfFirst { it is SentenceSegment.InputField }
        if (newInputIndex != -1) {
            val globalInputIndex = newRange.first + newInputIndex
            val field = sentenceSegments[globalInputIndex] as SentenceSegment.InputField
            sentenceSegments[globalInputIndex] = field.copy(value = oldInputValue)
        }
    }

    private fun addAccompanyingFields(prep: Preposition, option: String, insertIndex: Int = -1) {
        val hint = when (option) {
            "Duration" -> "How long?"
            "Distance" -> "How far?"
            "Event" -> "What occasion?"
            "Person" -> "${prep.label} whom?"
            "Location" -> "Where?"
            "Timeframe" -> "${prep.label} what interval?"
            "Condition" -> "Under what condition?"
            "Method" -> "How?"
            "Deadline" -> "${prep.label} what date or time?"
            "Tool" -> "Which tool(s)?"
            else -> "details..."
        }
        val inputField = SentenceSegment.InputField(hint)
        if (insertIndex == -1) sentenceSegments.add(inputField)
        else sentenceSegments.add(insertIndex, inputField)

        val dropdownOptions = when (option) {
            "Duration" -> listOf("second(s)", "minute(s)", "hour(s)", "day(s)", "week(s)", "month(s)")
            "Distance" -> listOf("inch(es)", "centimeter(s)", "yard(s)", "meter(s)", "mile(s)", "kilometer(s)")
            else -> emptyList()
        }
        if (dropdownOptions.isNotEmpty()) {
            val dropdown = SentenceSegment.DropdownField(options = dropdownOptions)
            if (insertIndex == -1) sentenceSegments.add(dropdown)
            else sentenceSegments.add(insertIndex + 1, dropdown)
        }
    }

    fun getFinalSentenceString(): String {
        val result = sentenceSegments.joinToString("") { segment ->
            when (segment) {
                is SentenceSegment.FixedText -> segment.text
                is SentenceSegment.EditableVerb -> segment.text
                is SentenceSegment.InputField -> "${segment.value} "
                is SentenceSegment.PrepositionChip -> " ${segment.preposition.label.lowercase()} "
                is SentenceSegment.DropdownField -> segment.options[segment.selectedIndex]
            }
        }
        return ("$result.").replace(Regex("\\s+"), " ").replace(" .", ".").trim()
    }

    /**
     * Seeds the sentence builder from an existing task's title for edit mode.
     * Places the full title into the [SentenceSegment.EditableVerb] and resets
     * all preposition segments, since segment structure is not persisted.
     */
    fun seedFromTask(task: HabitTask) {
        // Strip the leading "I will " prefix if present, otherwise use as-is
        val verb = task.title
            .removePrefix("I will ")
            .removeSuffix(".")
            .trim()
        sentenceSegments.clear()
        sentenceSegments.add(SentenceSegment.FixedText("I will "))
        sentenceSegments.add(SentenceSegment.EditableVerb(verb))
        selectedDifficulty = task.rewardAmount.coerceIn(1, 5)
        taskSentence = task.title
    }
}
