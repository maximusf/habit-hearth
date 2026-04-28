package com.project.habithearth.ui.theme

import androidx.compose.ui.graphics.Color
import com.project.habithearth.model.TaskCategory

/**
 * Compose color mapping for [TaskCategory]. Lives in `ui/theme/` (not on the
 * enum itself) so the persisted model stays free of Compose dependencies.
 *
 * Phase 2 split: previously a `Color` field on the enum constructor. Two known
 * Compose call sites read this: `HabitTaskRowCard` (card border + label) and
 * `BuildingDirectoryDialog` (building bullet color).
 */
val TaskCategory.outlineColor: Color
    get() = when (this) {
        TaskCategory.UNSORTED -> Color(0xFF6B7280)
        TaskCategory.WISDOM -> Color(0xFF5C6BB8)
        TaskCategory.STRENGTH -> Color(0xFFB85C5C)
        TaskCategory.VITALITY -> Color(0xFF5CB86B)
        TaskCategory.SPIRIT -> Color(0xFF8B5CB8)
    }
