package com.project.habithearth.ui.state

/**
 * Pure leveling math, kept off [GameStateViewModel] so the story screen and
 * resource bar can derive level/progress from a `totalXp` snapshot without
 * pulling the VM in.
 *
 * Single source of truth: only `totalXp: Int` is persisted. Level and
 * within-level progress are recomputed on the fly via [levelFor] /
 * [xpInLevel]. Storing them separately would invite drift after debug pokes
 * or partial migrations.
 */

/** Flat curve: every level costs the same amount of XP. */
const val XP_PER_LEVEL: Int = 100

/** Flat XP grant per task completion. Uncompleting does not refund XP. */
const val XP_PER_TASK: Int = 10

/**
 * Level numbering starts at 1, so a brand-new user with totalXp == 0 is at
 * Lv 1 (not Lv 0). The +1 makes resource-bar copy read naturally.
 */
fun levelFor(totalXp: Int): Int =
    (totalXp.coerceAtLeast(0) / XP_PER_LEVEL) + 1

/** XP earned within the current level, in the [0, XP_PER_LEVEL) range. */
fun xpInLevel(totalXp: Int): Int =
    totalXp.coerceAtLeast(0) % XP_PER_LEVEL

/**
 * Story gate: chapter 1 has 5 plot segments (indices 0..4). Segment 0 is
 * always available; each subsequent segment requires the next level.
 *
 * segmentIndex -> required level
 *   0 -> 1   (no gate)
 *   1 -> 2
 *   2 -> 3
 *   3 -> 4
 *   4 -> 5
 *
 * Story-screen wiring lands in the second milestone alongside the actual
 * narrative content; the helper is here now so the resource model and the
 * story code can agree on the same constants.
 */
fun requiredLevelForSegment(segmentIndex: Int): Int =
    (segmentIndex + 1).coerceAtLeast(1)

/** True iff the player has earned enough XP to unlock [segmentIndex]. */
fun canAdvanceTo(segmentIndex: Int, totalXp: Int): Boolean =
    levelFor(totalXp) >= requiredLevelForSegment(segmentIndex)
