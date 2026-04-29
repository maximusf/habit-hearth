package com.project.habithearth.ui.tasks

sealed class SentenceSegment {
    data class FixedText(val text: String) : SentenceSegment()

    data class EditableVerb(val text: String) : SentenceSegment()

    data class PrepositionChip(
        val preposition: TaskSentenceViewModel.Preposition,
        val option: String,
        val isMenuExpanded: Boolean = false,
    ) : SentenceSegment()

    data class InputField(
        val hint: String,
        val value: String = "",
    ) : SentenceSegment()

    data class DropdownField(
        val options: List<String>,
        val selectedIndex: Int = 0,
        val isExpanded: Boolean = false,
    ) : SentenceSegment()
}
