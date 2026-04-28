package com.project.habithearth.model

/**
 * Domain enum identifying which gem pool a task rewards on completion.
 *
 * Lives under `model/` (not `ui/model/`) so that data-layer code (repositories,
 * proto mappers) can reference it without pulling in any Compose dependency.
 *
 * Compose-only colors that used to live on this enum moved to
 * [com.project.habithearth.ui.theme.outlineColor]. Keep this file free of
 * Compose imports - that separation is the whole point of the split.
 */
enum class TaskCategory(val displayName: String) {
    UNSORTED("Unsorted"),
    WISDOM("Wisdom"),
    STRENGTH("Strength"),
    VITALITY("Vitality"),
    SPIRIT("Spirit"),
}
