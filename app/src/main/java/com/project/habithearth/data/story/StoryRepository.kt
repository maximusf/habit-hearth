package com.project.habithearth.data.story

import androidx.datastore.core.DataStore
import com.project.habithearth.data.proto.UserProgressProto
import com.project.habithearth.model.StoryChoice
import com.project.habithearth.model.StorySegment
import com.project.habithearth.model.StoryState
import com.project.habithearth.model.TaskCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Single owner of story persistence on top of `DataStore<UserProgressProto>`.
 *
 * Pure persistence layer: no Gemini, no strings.xml fallback, no plot-point
 * arithmetic beyond what the wire format requires. Generation/fallback
 * resolution lives a layer up (forthcoming `StoryViewModel` rewrite) so this
 * class can be unit-tested with a fake DataStore and zero network state.
 *
 * All mutations route through [DataStore.updateData] for read-modify-write
 * atomicity. That matters for [recordChoice] and [freezeCategory] in
 * particular: both are first-write-wins, and the atomic update is what
 * enforces "not revisitable" without a separate lock.
 */
class StoryRepository(
    private val dataStore: DataStore<UserProgressProto>,
) {

    val story: Flow<StoryState> = dataStore.data
        .map { it.story.toDomain() }
        .distinctUntilChanged()

    fun observeStory(): Flow<StoryState> = story

    /**
     * Snapshots the dominant category at story start. First write wins:
     * subsequent calls are silently dropped so tone stays coherent across
     * the whole arc even if the player's gem distribution shifts later.
     *
     * Returns true iff this call actually set the category (caller can use
     * the signal to log "story started" exactly once).
     */
    suspend fun freezeCategory(category: TaskCategory): Boolean {
        // Set `localFroze` to the freeze decision *every* invocation of the
        // transform. DataStore may re-run the transform on contention, so the
        // boolean must reflect whichever pass actually persisted - simply
        // setting it to true on the freeze branch (without resetting on retries
        // that took the no-op branch) would leak a stale `true` from an
        // earlier attempt.
        var localFroze = false
        dataStore.updateData { current ->
            val canFreeze = current.story.frozenCategory.isBlank()
            localFroze = canFreeze
            if (!canFreeze) {
                current
            } else {
                current.copy(story = current.story.copy(frozenCategory = category.name))
            }
        }
        return localFroze
    }

    /**
     * Appends a resolved narrative beat. Append-only by contract - segments
     * are never edited or removed once written, so the on-disk order matches
     * reading order without needing a separate index.
     */
    suspend fun appendSegment(segment: StorySegment) {
        dataStore.updateData { current ->
            current.copy(
                story = current.story.copy(
                    segments = current.story.segments + segment.toProto(),
                ),
            )
        }
    }

    /**
     * Locks a choice at [StoryChoice.plotPointIndex]. First write wins: if a
     * choice already exists at that index, the new one is dropped and this
     * returns false. Returns true iff the choice was newly recorded.
     *
     * Also bumps `plotPointsReached` to `max(current, plotPointIndex + 1)` so
     * a resumed session can tell which beats are already locked without
     * scanning the choices list.
     */
    suspend fun recordChoice(choice: StoryChoice): Boolean {
        // See [freezeCategory] for why `localRecorded` is overwritten on every
        // invocation of the transform: DataStore may re-run the lambda on
        // write contention, and the return value has to track the pass that
        // actually persisted.
        var localRecorded = false
        dataStore.updateData { current ->
            val already = current.story.choices.any { it.plotPointIndex == choice.plotPointIndex }
            localRecorded = !already
            if (already) {
                current
            } else {
                val nextReached = maxOf(
                    current.story.plotPointsReached,
                    choice.plotPointIndex + 1,
                )
                current.copy(
                    story = current.story.copy(
                        choices = current.story.choices + choice.toProto(),
                        plotPointsReached = nextReached,
                    ),
                )
            }
        }
        return localRecorded
    }

    /**
     * Marks the story as finished. Idempotent: re-flagging an already-complete
     * story is a no-op. The renderer uses this to switch to an epilogue view
     * instead of offering more choices.
     */
    suspend fun markComplete() {
        dataStore.updateData { current ->
            if (current.story.isComplete) {
                current
            } else {
                current.copy(story = current.story.copy(isComplete = true))
            }
        }
    }

    /**
     * Wipes story state back to the empty default. Intended for "start new
     * account" flows so a fresh user does not inherit the previous arc. Does
     * not touch tasks/progress/settings.
     */
    suspend fun reset() {
        dataStore.updateData { current ->
            current.copy(story = com.project.habithearth.data.proto.StoryProto())
        }
    }
}
