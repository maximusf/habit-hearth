package com.project.habithearth.model

/**
 * One narrative beat in the player's story log. Append-only - segments are
 * never edited or removed once written.
 *
 * [isGenerated] distinguishes Gemini output from the offline strings.xml
 * fallback so future renderers (or analytics) can tell them apart without
 * re-deriving the source. Both kinds cache the resolved prose verbatim.
 *
 * [anchorId] ties a segment to a plot point or choice id (e.g. "p2" or
 * "p2.choice_a") so the UI can reconstruct branches without re-running the
 * generator.
 */
data class StorySegment(
    val anchorId: String,
    val isGenerated: Boolean,
    val text: String,
)
