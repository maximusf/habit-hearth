package com.project.habithearth.data.story

import com.project.habithearth.data.proto.StoryChoiceProto
import com.project.habithearth.data.proto.StoryProto
import com.project.habithearth.data.proto.StorySegmentProto
import com.project.habithearth.model.StoryChoice
import com.project.habithearth.model.StorySegment
import com.project.habithearth.model.StoryState
import com.project.habithearth.model.TaskCategory

/**
 * Conversion between the wire-format [StoryProto] tree and the domain
 * [StoryState]. Mirrors the split used by TaskMappers so [StoryRepository]
 * (forthcoming) reads as pure flow/edit logic.
 *
 * [StoryProto.frozenCategory] is a string for forward-compat with new
 * categories; an unknown or blank value decodes to a null
 * [StoryState.frozenCategory] rather than throwing, so a future category
 * removal cannot crash an existing install. Callers treat null as "tone not
 * yet frozen", which matches the pre-first-plot-point state.
 */
internal fun StoryProto.toDomain(): StoryState =
    StoryState(
        segments = segments.map { it.toDomain() },
        choices = choices.map { it.toDomain() },
        plotPointsReached = plotPointsReached.coerceAtLeast(0),
        isComplete = isComplete,
        frozenCategory = frozenCategory
            .takeIf { it.isNotBlank() }
            ?.let { name -> TaskCategory.entries.firstOrNull { it.name == name } },
    )

internal fun StoryState.toProto(): StoryProto =
    StoryProto(
        segments = segments.map { it.toProto() },
        choices = choices.map { it.toProto() },
        plotPointsReached = plotPointsReached,
        isComplete = isComplete,
        frozenCategory = frozenCategory?.name.orEmpty(),
    )

internal fun StorySegmentProto.toDomain(): StorySegment =
    StorySegment(
        anchorId = anchorId,
        isGenerated = isGenerated,
        text = text,
    )

internal fun StorySegment.toProto(): StorySegmentProto =
    StorySegmentProto(
        anchorId = anchorId,
        isGenerated = isGenerated,
        text = text,
    )

internal fun StoryChoiceProto.toDomain(): StoryChoice =
    StoryChoice(
        plotPointIndex = plotPointIndex,
        choiceId = choiceId,
    )

internal fun StoryChoice.toProto(): StoryChoiceProto =
    StoryChoiceProto(
        plotPointIndex = plotPointIndex,
        choiceId = choiceId,
    )
