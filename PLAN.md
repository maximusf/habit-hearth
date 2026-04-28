# DataStore Refactor Plan

## Purpose

Refactor app storage so persistent data is:

1. on correct Jetpack DataStore version
2. typed where structure warrants it
3. retrieved and written through clear repository boundaries
4. free of redundant state and unused variables
5. organized so each feature's logic is easy for developers to find

This plan is for formal review before implementation.

## Scope notes

- **No real users yet.** Dev/testing only — no migration phase. Uninstall app before first proto build to clear stale Preferences DataStore.
- **Authentication is out of scope.** App boots directly to Home. No login screen, no session lock, no account/password storage.

## Verified Current State

### Current dependency

- Project currently uses `androidx.datastore:datastore-preferences:1.1.1` via [gradle/libs.versions.toml](/home/i-am-groot/StudioProjects/habit-hearth/gradle/libs.versions.toml:13) and [app/build.gradle.kts](/home/i-am-groot/StudioProjects/habit-hearth/app/build.gradle.kts:61).

### Official version check

Verified on April 27, 2026 against Android Developers release notes:

- latest stable DataStore release: `1.2.1`
- latest stable in `1.1.x` line: `1.1.7`
- official dependency examples use `1.2.1` for both `datastore-preferences` and typed `datastore`

Decision:

- upgrade DataStore artifacts to `1.2.1`
- add typed DataStore support with `androidx.datastore:datastore:1.2.1`

Pre-Phase 1 verification required:

- confirm `1.2.1` actually marked stable on developer.android.com/jetpack/androidx/releases/datastore before bumping
- confirm `com.google.protobuf` Gradle plugin compatible with AGP `9.1.0`
- if protobuf plugin incompatible, fall back to typed DataStore with `kotlinx.serialization` + custom `Serializer<T>`

### Current architecture problems

1. [UserProgressRepository.kt](/home/i-am-groot/StudioProjects/habit-hearth/app/src/main/java/com/project/habithearth/data/UserProgressRepository.kt:1) mixes too many concerns:
   - settings
   - profile avatar
   - game progress
   - task persistence

2. Complex app state is stored as one JSON blob under `KEY_GAME_STATE_JSON`.
   - tasks
   - gems/coins/xp
   - owned buildings

3. Persisted data model is coupled to UI model.
   - [GameUiState](/home/i-am-groot/StudioProjects/habit-hearth/app/src/main/java/com/project/habithearth/ui/state/GameStateViewModel.kt:20) is both screen state and storage shape
   - [HabitTask](/home/i-am-groot/StudioProjects/habit-hearth/app/src/main/java/com/project/habithearth/ui/model/HabitTask.kt:1) lives under `ui/model/` even though it is persisted

4. Task logic is split across multiple layers.
   - create/edit UI in [TaskMakerScreen.kt](/home/i-am-groot/StudioProjects/habit-hearth/app/src/main/java/com/project/habithearth/ui/tasks/TaskMakerScreen.kt:43)
   - mutation logic in [GameStateViewModel.kt](/home/i-am-groot/StudioProjects/habit-hearth/app/src/main/java/com/project/habithearth/ui/state/GameStateViewModel.kt:56)
   - persistence in [UserProgressRepository.kt](/home/i-am-groot/StudioProjects/habit-hearth/app/src/main/java/com/project/habithearth/data/UserProgressRepository.kt:230)

5. Redundant state exists.
   - persisted app state duplicated as in-memory UI state
   - task state mixed into broader game state
   - temporary UI state and persisted state not clearly separated

## Architecture Decision

Use **Proto DataStore** for all restart-surviving app data that is structured:

- tasks
- resources
- XP
- owned buildings
- profile/settings

Keep temporary form/input state in Compose/ViewModel only:

- text field contents before save
- dropdown expanded flags
- current screen-only selections

Reason:

- official docs recommend typed DataStore when storing custom objects with schema
- current app data is structured, not key/value only
- one typed schema removes JSON blob ambiguity
- one physical store keeps all persistent app data in DataStore, per project goal

Constraint:

- DataStore is suitable here because dataset is still small and local-first
- if app grows into large query-heavy history/analytics data, reevaluate Room later

## Target Source of Truth

Single persistent source of truth:

- `DataStore<UserProgressProto>`

Single schema file:

- `app/src/main/proto/user_progress.proto`

All persistent reads/writes go through repositories in `data/`.

UI must not:

- read/write DataStore directly
- own persisted model shapes
- serialize/deserialize JSON

## Target File Structure

```text
app/src/main/
├── java/com/project/habithearth/
│   ├── HabitHearthApplication.kt
│   ├── data/
│   │   ├── datastore/
│   │   │   ├── AppDataStore.kt
│   │   │   └── UserProgressSerializer.kt
│   │   ├── progress/
│   │   │   ├── ProgressRepository.kt
│   │   │   └── ProgressMappers.kt
│   │   ├── settings/
│   │   │   └── SettingsRepository.kt
│   │   └── task/
│   │       ├── TaskRepository.kt
│   │       └── TaskMappers.kt
│   ├── model/
│   │   ├── HabitTask.kt
│   │   ├── ProfileSettings.kt
│   │   ├── ResourceProgress.kt
│   │   └── TaskCategory.kt
│   └── ui/
│       ├── home/
│       ├── map/
│       ├── navigation/
│       ├── profile/
│       ├── story/
│       ├── theme/
│       │   └── TaskCategoryColors.kt
│       └── tasks/
│           ├── TaskEditorViewModel.kt
│           ├── TaskListViewModel.kt
│           └── TaskMakerScreen.kt
└── proto/
    └── user_progress.proto
```

Removed from earlier draft:

- `data/account/`, `data/session/` — auth out of scope
- `model/SessionState.kt` — auth out of scope
- `ui/account/`, `ui/state/AppEntryViewModel.kt` — no entry gate, app boots to Home
- `domain/task/CompleteTaskUseCase.kt` — YAGNI, no concrete reuse case yet
- `data/task/TaskQueries.kt` — premature split, merge into `TaskRepository`

## Data Ownership Rules

### `data/`

Owns:

- DataStore instance
- schema serialization
- all persistent CRUD

Does not own:

- Compose widget state
- screen formatting concerns

### `model/`

Owns:

- app/domain models used across repositories and UI

Does not own:

- Compose-only concerns such as colors or UI-only labels when they can be derived elsewhere

Note:

- `TaskCategory` currently mixes data concern and UI color. Split this.
- keep persisted enum + `displayName` in `model/TaskCategory.kt`
- move `outlineColor` mapping to `ui/theme/TaskCategoryColors.kt` as extension `val TaskCategory.outlineColor: Color`
- verified only 2 Compose call sites: `HabitTaskRowCard.kt:34,73` + `BuildingDirectoryDialog.kt:59`

### `ui/`

Owns:

- rendering
- temporary user input state
- collecting repository/viewmodel flows

Does not own:

- persistence logic
- DataStore keys
- JSON/proto mapping

## Proposed Proto Schema

Single schema file: `user_progress.proto`

Top-level message covers all persistent app data:

```proto
syntax = "proto3";

option java_package = "com.project.habithearth.data.proto";
option java_multiple_files = true;

message TaskProto {
  string id = 1;
  string title = 2;
  string note = 3;
  string category = 4;
  int32 reward_amount = 5;
  bool is_completed = 6;
  string building_id = 7;
}

message SettingsProto {
  bool push_notifications = 1;
  bool vacation_mode = 2;
  string theme_mode = 3;
  string language = 4;
  string text_size = 5;
  int32 profile_avatar_id = 6;
  string display_name = 7;
}

message ProgressProto {
  int32 strength_gems = 1;
  int32 wisdom_gems = 2;
  int32 vitality_gems = 3;
  int32 spirit_gems = 4;
  int32 coins = 5;
  float xp_progress = 6;
  repeated string owned_building_ids = 7;
}

message StoryChoiceProto {
  int32 plot_point_index = 1;   // 0..4
  string choice_id = 2;          // which of 1-3 options the user picked
}

message StorySegmentProto {
  string anchor_id = 1;          // prewritten beat id OR generated segment id
  bool is_generated = 2;         // true if Gemini-authored, false if strings.xml fallback
  string text = 3;
}

message StoryProto {
  repeated StorySegmentProto segments = 1;  // append-only narrative log
  repeated StoryChoiceProto choices = 2;     // append-only choice history
  int32 plot_points_reached = 3;             // 0..5
  bool is_complete = 4;                      // true once plot point 5 resolved
  string frozen_category = 5;                // dominant gem category at story start
}

message UserProgressProto {
  SettingsProto settings = 1;
  ProgressProto progress = 2;
  repeated TaskProto tasks = 3;
  StoryProto story = 4;
}
```

Notes:

- `category` stays string for forward-compat with future categories without proto enum churn
- can convert to proto enum later if categories stabilize
- empty `building_id` means Home/unassigned
- `display_name` lives in `SettingsProto` since profile screen edits it as a setting
- `frozen_category` snapshots the dominant category at story start so tone stays coherent across sessions; choice gating still uses *live* stats at each plot point

## Execution Plan

### Phase 1: DataStore foundation

Goal:

- get project onto correct DataStore version
- add typed DataStore infrastructure without changing app behavior yet

Changes:

1. update [gradle/libs.versions.toml](/home/i-am-groot/StudioProjects/habit-hearth/gradle/libs.versions.toml:1)
   - `datastorePreferences = "1.2.1"`
   - add version for typed `datastore`
   - add protobuf plugin + runtime versions

2. update [app/build.gradle.kts](/home/i-am-groot/StudioProjects/habit-hearth/app/build.gradle.kts:1)
   - add `androidx.datastore:datastore`
   - add protobuf Gradle plugin
   - configure proto source generation

3. add:
   - `app/src/main/proto/user_progress.proto`
   - `data/datastore/AppDataStore.kt`
   - `data/datastore/UserProgressSerializer.kt`

Deliverable:

- project builds with Proto DataStore available

### Phase 2: Domain model cleanup

Goal:

- remove persisted models from UI package
- remove UI-only fields from persisted model layer

Changes:

1. move:
   - [HabitTask.kt](/home/i-am-groot/StudioProjects/habit-hearth/app/src/main/java/com/project/habithearth/ui/model/HabitTask.kt:1) -> `model/HabitTask.kt`
   - split [TaskCategory](/home/i-am-groot/StudioProjects/habit-hearth/app/src/main/java/com/project/habithearth/ui/model/HabitTask.kt:6) into:
     - `model/TaskCategory.kt` — keep enum + `displayName`
     - `ui/theme/TaskCategoryColors.kt` — extension `val TaskCategory.outlineColor: Color`
   - update call sites: `HabitTaskRowCard.kt:34,73`, `BuildingDirectoryDialog.kt:59`

2. create:
   - `model/ResourceProgress.kt`
   - `model/ProfileSettings.kt`

3. move `GameUiState` extension fns onto `ResourceProgress`:
   - `ui/map/BuildingUnlock.kt:29` `GameUiState.canAfford(cost)` -> `ResourceProgress.canAfford(cost)`
   - `ui/map/BuildingUnlock.kt:42` `GameUiState.withUnlockCostPaid(cost)` -> `ResourceProgress.withUnlockCostPaid(cost)`
   - pure data, no UI coupling — reads only gem/coin fields
   - keep extension fns in `ui/map/BuildingUnlock.kt` or move to `model/` if no Compose deps

Deliverable:

- persisted model types no longer live under `ui/`
- unlock-cost math operates on `ResourceProgress`, not `GameUiState`

### Phase 3: Repository split by feature

Goal:

- all feature logic easy find

Changes:

1. create `TaskRepository`
   - `observeTasks()`
   - `observeTask(taskId)`
   - `createTask(...)`
   - `updateTask(...)`
   - `setTaskCompleted(...)`
   - `observeTasksForBuilding(buildingId)`

2. create `ProgressRepository`
   - `observeProgress()`
   - `tryPurchaseBuilding(buildingId)`
   - `addReward(category, amount)`

3. create `SettingsRepository`
   - push notifications
   - vacation mode
   - theme/language/text size
   - avatar id
   - display name

Best-practice clarification:

- `ProgressRepository` is required as data layer owner
- `ProgressViewModel` is optional — only if a screen needs progress-specific presentation state
- repository owns persistence and data access
- viewmodel owns screen-facing state only
- `ProgressMappers.kt` separate intentionally to isolate proto-to-model conversion from repository business logic

Naming note:

- Existing legacy `data/UserProgressRepository.kt` is the giant class being killed.
- New repos use distinct names: `ProgressRepository`, `TaskRepository`, `SettingsRepository`. No `UserProgressRepository` in target structure.

Deliverable:

- no single giant repository
- developers can find feature logic in one package

### Phase 4: ViewModel split

Goal:

- remove persistence logic from monolithic UI state holder

Pre-step — `GameUiState` consumer so:

- list every screen reading `GameUiState` before split
- map each consumer to new ViewModel/repository source
- prevents silent breakage

Changes:

1. introduce:
   - `ui/tasks/TaskListViewModel.kt`
   - `ui/tasks/TaskEditorViewModel.kt`

2. shrink or remove [GameStateViewModel.kt](/home/i-am-groot/StudioProjects/habit-hearth/app/src/main/java/com/project/habithearth/ui/state/GameStateViewModel.kt:1)
   - remove task CRUD
   - remove DataStore persistence trigger methods
   - keep only transient shell aggregation if still needed

Deliverable:

- task screens use task-specific viewmodels
- progress logic no longer hidden in one UI state class

### Phase 5: Screen rewiring

Goal:

- all screen data comes from repositories/viewmodels backed by typed DataStore

Changes:

1. [TaskMakerScreen.kt](/home/i-am-groot/StudioProjects/habit-hearth/app/src/main/java/com/project/habithearth/ui/tasks/TaskMakerScreen.kt:43)
   - read/edit through `TaskEditorViewModel`
   - keep only local form state that is temporary

2. [HomeScreen.kt](/home/i-am-groot/StudioProjects/habit-hearth/app/src/main/java/com/project/habithearth/ui/home/HomeScreen.kt:37)
   - consume tasks from `TaskListViewModel`
   - consume progress from `ProgressRepository`, optionally through `ProgressViewModel` only if screen-specific presentation state is needed

3. [BuildingDetailScreen.kt](/home/i-am-groot/StudioProjects/habit-hearth/app/src/main/java/com/project/habithearth/ui/map/BuildingDetailScreen.kt:49)
   - consume building-specific task flow
   - consume building ownership/progress through progress layer

4. [ProfileScreen.kt](/home/i-am-groot/StudioProjects/habit-hearth/app/src/main/java/com/project/habithearth/ui/profile/ProfileScreen.kt:1)
   - consume `SettingsRepository` only

Deliverable:

- no screen reads legacy storage shape directly
- app still boots straight to Home

### Phase 6: Remove redundant and unused state

Goal:

- eliminate duplicate storage/state variables

Audit targets:

1. delete legacy `data/UserProgressRepository.kt` entirely
2. remove `KEY_GAME_STATE_JSON` and all legacy Preferences DataStore keys
3. remove `loadGameState()` / `saveGameState()` and Gson dependency if no other consumers
4. remove duplicated task/resource state from `GameUiState`
5. remove dead imports, commented preview-only imports, stale remembered values, and unused helper variables

Deliverable:

- one persistent source of truth
- no duplicate task storage path

## Initial Milestone Recommendation

Implement first in this order (task vertical only):

1. Phase 1 — DataStore foundation
2. Phase 2 — model cleanup (incl. TaskCategory color split)
3. Phase 3 — `TaskRepository` only (defer Progress + Settings)
4. Phase 4 — task ViewModels only
5. Phase 5 — task screens only (`TaskMakerScreen`, task list portion of `HomeScreen`)
6. Phase 6 — task-scoped cleanup

Reason:

- biggest clarity win first
- lowest blast radius
- gives one clear place for tasks before broader settings/progress split

Second milestone: `ProgressRepository` + `SettingsRepository` + remaining screens.

## Story Persistence Design

The story is **per-user, stable across restarts/updates**. Slight variation between users via `frozen_category` + choice history. No rewinds: choices are locked once made.

### Shape

- 5 plot points (`plot_point_index = 0..4`)
- 1-3 choices surfaced at each plot point, gated on **live** stats
- One narrative segment per beat (no chunking)
- Generated text is cached verbatim — re-entry never re-calls Gemini for the same beat

### Why cache text instead of re-generating from a seed

Seed-replay assumes Gemini is bit-stable across model versions. It isn't. Caching the prose locks the story for the user even after model upgrades. Storage cost is trivial (tens of segments × short prose).

### Dominant category integration

- **Tone** — frozen at story start as `StoryProto.frozen_category`. Injected into Gemini prompt to flavor metaphors, descriptions, NPC reactions. Frozen so prose stays consistent across sessions.
- **Choice gating** — evaluated against *live* stats at each plot point. The frozen-category-aligned choice is always shown; off-category choices appear only when their stat threshold is met. This keeps tone coherent while letting choices stay reactive to current play.

### Offline / no-key fallback

Gemini is best-effort. Every prewritten beat and every choice option lives in `res/values/strings.xml` keyed by `(plot_point_index, category)`. `StoryRepository.nextBeat()` tries Gemini first; on failure or when `BuildConfig.GEMINI_API_KEY.isBlank()`, falls back to the string resource and stores the segment with `is_generated = false`. Same cache row format either way.

This also gives a deterministic, fully-offline build path for tests and CI.

### Repository / VM placement

- New: `data/story/StoryRepository.kt` — owns `StoryProto` reads/writes, Gemini call, fallback resolution
- New: `data/story/StoryMappers.kt` — proto ↔ model
- New: `model/StorySegment.kt`, `model/StoryChoice.kt`, `model/StoryState.kt`
- New: `ui/story/StoryViewModel.kt` — reads `observeStory()` from repo, exposes `chooseAt(plotPointIndex, choiceId)` → repo
- `ui/story/StoryScreen.kt` rewires onto `StoryViewModel`, drops direct Gemini call
- Slot story work into the **second milestone** alongside `ProgressRepository` + `SettingsRepository`. Do schema bump (add `StoryProto story = 4` to `UserProgressProto`) at the start of the second milestone — before the proto cutover — so subsequent writes don't need a proto migration.

## Acceptance Criteria

Plan considered complete when implementation meets all of these:

1. DataStore dependency is updated from `1.1.1` to `1.2.1`.
2. Structured persistent data is stored in typed Proto DataStore, not in `game_state_json`.
3. All restart-surviving app data is read from and written to DataStore-backed repositories.
4. Task feature has one obvious home:
   - model
   - repository
   - viewmodels
   - screens
5. No persisted model remains under `ui/model/`.
6. `GameStateViewModel` no longer owns task persistence.
7. Fresh install boots to Home with clean proto state, no crash.
8. Redundant persisted state and unused variables are removed.
9. App behavior remains functionally equivalent for:
   - task creation/edit/completion
   - building purchase/unlock
   - settings/profile updates
10. Story state survives restart and app update: same prose, same choices, same plot-point progress per user. Offline / missing-key path falls back to `strings.xml` and caches identically.

## Risks / Open Questions

1. **DataStore `1.2.1` stability** — verify on developer.android.com release notes before Phase 1.
2. **AGP 9.1.0 + protobuf plugin compat** — verify before Phase 1. Fallback: `kotlinx.serialization` + custom `Serializer<T>`.
3. ~~**Story data** — confirm whether story progress must persist.~~ **Resolved 2026-04-28.** Story is per-user, stable across restarts/updates. Schema (`StoryProto`) defined in [Proposed Proto Schema](#proposed-proto-schema); design captured in [Story Persistence Design](#story-persistence-design). Schema bump scheduled at the start of the second milestone, before the proto cutover.
4. **`GameStateViewModel` future** — shrink to shell aggregator or remove entirely. Decide during Phase 4 audit.
5. **Single proto file vs multiple** — single file first. Split only if file grows unwieldy.
6. **`TaskCategory` representation** — string in proto for forward-compat with new categories. Convert to proto enum later if categories stabilize.

## Sources

- AndroidX DataStore release notes: https://developer.android.com/jetpack/androidx/releases/datastore
- Android DataStore architecture docs: https://developer.android.com/topic/libraries/architecture/datastore
