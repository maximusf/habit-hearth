package com.project.habithearth.model

/**
 * Per-user story progress as the rest of the app sees it.
 *
 * Restart-survival contract:
 *   - same user re-opens app: identical [segments] in identical order
 *   - same user updates app or model version: identical text (cache, not regenerate)
 *   - different user: may diverge via [frozenCategory] tone + live-stat choice gating
 *
 * [frozenCategory] snapshots the dominant TaskCategory at story start so the
 * narrative voice stays coherent across the whole arc. It is null until the
 * first plot point freezes it. Choice gating at each plot point still reads
 * live stats off the player's current ResourceProgress, so reactivity is
 * preserved without destabilizing the tone.
 */
data class StoryState(
    val segments: List<StorySegment> = emptyList(),
    val choices: List<StoryChoice> = emptyList(),
    val plotPointsReached: Int = 0,
    val isComplete: Boolean = false,
    val frozenCategory: TaskCategory? = null,
)
