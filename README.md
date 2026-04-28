# Habit Hearth

A gamified habit tracker where completing real-life habits rebuilds a fantasy village. Built with Kotlin and Jetpack Compose.

## The Idea

Most habit trackers lose their pull after a few weeks. Checkboxes and streaks aren't enough to keep people engaged. Habit Hearth wraps the same mechanics in a story you care about. Instead of "marking a task done," you're hauling debris off the blacksmith so your village can forge tools again.

The **Veil of Stagnation** (purple vines and thick fog) creeps into the village whenever you stop completing tasks. Keep working, and the veil retreats. Stop, and it consumes your buildings and villagers. It's never fully defeated, mirroring the real, ongoing effort of maintaining habits.

### Core Loop

**Complete habits → Earn gems & XP → Progress the story → Unlock buildings**

Every task belongs to one of four categories:

| Category | Gem | In the Story |
|----------|-----|--------------|
| Strength | Red | Hauling debris, forging tools, combat training |
| Wisdom | Blue | Studying blueprints, salvaging books, calculations |
| Vitality | Green | Healing villagers, gardening, restoring hot springs |
| Spirit | Purple | Art, baking bread, inspiring the community with music |

As XP accumulates, an AI-generated narrative unfolds, flavored by whichever category you've been focusing on. At key milestones, you face narrative choices that shape how the story plays out.

### Task Creation: Mad Libs

Tasks aren't created through a boring form. Instead, you build sentences by tapping placeholders:

> **I will** (verb) **for** (number) (time period).

Pick a verb, choose a clause type, fill in the blanks. The result is a structured, parseable task that still feels personal and flexible.

## Screenshots

| Map | Story | Home |
|-----|-------|------|
| Hex-grid village with hand-drawn buildings. Purple vines visible at the edges. | AI-generated narrative in a book-style layout. Choices appear at story milestones. | Task list with gem rewards. Tap to complete and collect. |

## Tech Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **MVVM** architecture with **StateFlow**
- **Jetpack DataStore** (typed Proto + legacy Preferences during refactor) for local persistence
- **Gemini 3.0 Flash** for AI story generation
- Gradle with Kotlin DSL and version catalog

No cloud backend, no accounts, no login. All data stays on-device.

## Project Structure

```
com.project.habithearth/
├── data/                    # Persistence (DataStore repos: task, story, legacy progress)
├── model/                   # HabitTask, ResourceProgress, TaskCategory
├── ui/
│   ├── components/          # Shared composables
│   ├── home/                # Task list + building directory
│   ├── map/                 # Hex-grid village map
│   ├── navigation/          # Routes + top resource bar
│   ├── profile/             # Player profile + settings (+ hidden debug panel)
│   ├── state/               # GameStateViewModel + Leveling.kt (XP/level math)
│   ├── story/               # AI narrative screen + ViewModel
│   ├── tasks/               # Task list/editor ViewModels + TaskMakerScreen
│   └── theme/               # Colors, typography, Material 3 theme
├── HabitHearthApplication.kt
└── MainActivity.kt
```

## Setup

1. Clone the repo
2. Open in Android Studio
3. Add your Gemini API key to `local.properties`:
   ```
   GEMINI_API_KEY=your_key_here
   ```
4. Build and run on a device or emulator (tested on Pixel 7a)

The app works without a Gemini key, but you won't be able to generate story content.

## XP & Leveling

- 10 XP per task completion. Uncompleting refunds gems but **not** XP — XP is monotonic.
- 100 XP per level. Level shown in the top resource bar as `Lv N · X/100`.
- Story plot points gate by level: segment 0 always available, segment N requires Lv N+1. Story-screen wiring lands with the rest of the second-milestone story work; the math lives in `ui/state/Leveling.kt` already.

## Developer Notes

**Hidden debug panel** (debug builds only): tap the Profile bottom-tab 7 times in a row. A toast confirms unlock and a Debug section appears at the bottom of the Profile screen with controls to mint gems/coins/XP, unlock all buildings, or reset progress. Hide it via the "Hide" button or via process death; counter resets if you tap any other tab mid-streak. `BuildConfig.DEBUG`-gated, so release builds can't reach it.

**Active refactor**: `PLAN.md` has the in-progress DataStore migration. Auth has been fully removed; `ProgressRepository` / `SettingsRepository` and the deletion of legacy `data/UserProgressRepository.kt` are still pending.

## Inspiration

Habitica, ClassCraft, BitLife, Ring Fit Adventure, Oregon Trail.

## License

MIT