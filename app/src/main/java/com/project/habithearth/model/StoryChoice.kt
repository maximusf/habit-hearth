package com.project.habithearth.model

/**
 * A locked-in choice at a plot point. Choices are not revisitable, so a row
 * is written once per [plotPointIndex] and never updated.
 *
 * [choiceId] is opaque to the domain layer; matchers live alongside whatever
 * defines the available choices (Gemini prompt or strings.xml fallback).
 */
data class StoryChoice(
    val plotPointIndex: Int,
    val choiceId: String,
)
