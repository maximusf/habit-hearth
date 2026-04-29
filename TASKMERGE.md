# TASKMERGE: SentenceStarter.kt → Habit Hearth

## What this merge does

Replaces the plain-text `TaskMakerScreen` form with the sentence-building UI from
`SentenceStarter.kt` and wires the streak-based reward system into the proto DataStore.

---

## Files changed

### New files
| File | Purpose |
|------|---------|
| `model/CollectionLogEntry.kt` | Domain model for one completion log entry (timestamp + amount) |
| `ui/tasks/SentenceSegment.kt` | Sealed class for sentence parts (FixedText, EditableVerb, PrepositionChip, InputField, DropdownField) |
| `ui/tasks/TaskSentenceViewModel.kt` | Adapted from `SentenceStarterViewModel` — sentence building + difficulty state, no DataStore |
| `ui/tasks/BuildSentenceCard.kt` | `BuildSentenceCard` composable (renamed from `BuildSentence`) |
| `ui/tasks/DifficultyCard.kt` | `DifficultyCard` composable |

### Modified files
| File | Change |
|------|--------|
| `data/proto/UserProgressProto.kt` | Added `CollectionLogEntryProto`; extended `TaskProto` with fields 8–11 |
| `model/HabitTask.kt` | Added `completionCount`, `streakMultiplier`, `collectedCurrency`, `collectionLog` |
| `data/task/TaskMappers.kt` | Updated `toDomain`/`toProto` for new fields; added log-entry converters |
| `data/task/TaskRepository.kt` | Added `collectCurrency(taskId)`; `updateTask()` now accepts `rewardAmount` |
| `ui/tasks/TaskEditorViewModel.kt` | Added `rewardAmount` param to `save()`/`saveAsync()` |
| `ui/tasks/TaskMakerScreen.kt` | Title field replaced by `BuildSentenceCard` + `DifficultyCard`; note/category/building kept |
| `ui/tasks/TaskListViewModel.kt` | `setCompleted()` calls `collectCurrency()` on completion; `onTransition` now receives credited amount |
| `ui/home/HomeScreen.kt` | Updated `setCompleted` lambda signature `(HabitTask, Int)` |
| `ui/map/BuildingDetailScreen.kt` | Updated `setCompleted` lambda signature `(HabitTask, Int)` |

---

## Key design decisions

### Sentence → title
`sentenceSegments` is ephemeral UI state (not persisted). `getFinalSentenceString()` produces the task
title written to DataStore. In edit mode the existing title seeds the `EditableVerb` field.

### Difficulty → rewardAmount
`selectedDifficulty` (1–5) maps 1:1 to `rewardAmount` in `HabitTask`/`TaskProto`. The streak
multiplier then scales on top: actual credit = `rewardAmount × streakMultiplier`.

### Streak logic
Ported from `FullTaskClass.checkStreak()` into `TaskRepository.collectCurrency()` as an atomic
`DataStore.updateData {}` block. Streak increments if the gap since the last entry ≤ 1 day;
decrements (floor 1) otherwise. No streak update occurs on de-completion.

### Organization enum removed
`SentenceStarterViewModel.Organization` is replaced by the existing building list
(`defaultVillageBuildings()` + owned building IDs), keeping category and building as separate
concerns (category → gem pool, building → village placement).

### onTransition signature change
`TaskListViewModel.setCompleted` callback changed from `(HabitTask) -> Unit` to
`(HabitTask, Int) -> Unit` where `Int` is the signed delta (positive on complete, negative on
un-complete). Call sites pass the delta directly to `GameStateViewModel.applyRewardDelta()`.

---

## Proto wire compatibility

New `TaskProto` fields use `@ProtoNumber` 8–11 with safe defaults:
- `completionCount: Int = 0`
- `streakMultiplier: Int = 1`
- `collectedCurrency: Int = 0`
- `collectionLog: List<CollectionLogEntryProto> = emptyList()`

Existing installs deserialize correctly — missing fields fall back to defaults.
Never renumber or reuse proto field numbers 1–11.

---

## Verification checklist

- [ ] New task: build sentence, pick difficulty 3 → DataStore stores sentence as title, `rewardAmount = 3`
- [ ] Edit task: existing title pre-fills in EditableVerb; difficulty pre-fills from `rewardAmount`
- [ ] Complete task day 1 → credits `1 × rewardAmount`, `streakMultiplier` stays 1 (log < 2 entries)
- [ ] Complete task day 2 → credits `2 × rewardAmount`, `streakMultiplier` = 2
- [ ] Skip a day, complete → credits `1 × rewardAmount`, `streakMultiplier` decrements
- [ ] Un-complete → deducts `rewardAmount` (no streak logic), `collectCurrency` not called
- [ ] Category dropdown still routes gems to correct pool
- [ ] Building dropdown still filters to owned buildings
- [ ] Existing tasks (pre-migration) open and save without errors
