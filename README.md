# Habit Hearth

A gamified habit tracker where completing real-life habits rebuilds a fantasy village. Built with Kotlin and Jetpack Compose for Android.

## The Idea

Most habit trackers lose their pull after a few weeks. Checkboxes and streaks aren't enough to keep people engaged. Habit Hearth wraps the same mechanics in a story you care about. Instead of "marking a task done," you're hauling debris off the blacksmith so your village can forge tools again.

The **Veil of Stagnation** (purple vines and thick fog) creeps into the village whenever you stop completing tasks. Keep working, and the veil retreats. Stop, and it consumes your buildings and villagers. It is never fully defeated, mirroring the real, ongoing effort of maintaining habits.

## Core Loop

**Complete habits → Earn gems & XP → Progress the story → Unlock buildings**

Every task belongs to one of four categories:

| Category | Gem | In the Story |
|----------|-----|--------------|
| Strength | Red | Hauling debris, forging tools, combat training |
| Wisdom | Blue | Studying blueprints, salvaging books, calculations |
| Vitality | Green | Healing villagers, gardening, restoring hot springs |
| Spirit | Purple | Art, baking bread, inspiring the community with music |

Uncategorized tasks reward coins, which the player spends to unlock new village buildings.

## Features

- **Mad-libs task creation.** Build a habit by filling a sentence: *"I will (verb) for (number) (time period)."* Tap placeholders to swap parts.
- **Difficulty 1–5.** Selected difficulty becomes the per-completion gem reward, modified by streak.
- **Hex-grid village map.** Hand-drawn buildings unlock by spending gems and coins. Each habit can be filed under a building.
- **Scripted Chapter 1 story.** Branching narrative with locked-in choices, gem-cost gates, and level gates. A chapter-select screen previews future chapters.
- **Streak rewards.** Consecutive completions multiply gem payouts.
- **Daily reminder notifications.** Toggleable in profile settings.
- **Hidden debug panel.** Seven taps on the Profile tab unlocks dev tools (mint resources, unlock buildings, reset progress).

## Tech Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **MVVM** with **StateFlow**
- **Jetpack DataStore** (typed Proto + Preferences) for local persistence
- **Gemini 3.0 Flash** wiring for future AI-generated story content
- Gradle with Kotlin DSL and version catalog

No cloud backend, no accounts, no login. All data stays on-device.

## Project Structure

```
com.project.habithearth/
├── data/                    # DataStore repos (task, story, progress)
├── model/                   # HabitTask, ResourceProgress, TaskCategory
├── ui/
│   ├── components/          # Shared composables
│   ├── home/                # Task list + building directory
│   ├── map/                 # Hex-grid village map
│   ├── navigation/          # Routes + top resource bar
│   ├── profile/             # Player profile + settings + debug panel
│   ├── state/               # GameStateViewModel + Leveling.kt
│   ├── story/               # Chapter 1 scripted graph + ViewModel
│   ├── tasks/               # Task list/editor ViewModels + TaskMakerScreen
│   └── theme/               # Colors, typography, Material 3 theme
├── HabitHearthApplication.kt
└── MainActivity.kt
```

## Setup

1. Clone the repo
2. Open in Android Studio
3. (Optional) Add a Gemini API key to `local.properties` for future AI features:
   ```
   GEMINI_API_KEY=your_key_here
   ```
4. Build and run on a device or emulator (tested on Pixel 7a, minSdk 24)

The app runs fully without a Gemini key. Chapter 1 ships as a scripted experience.

## XP & Leveling

- 10 XP per task completion. Uncompleting refunds gems but **not** XP. XP is monotonic.
- 100 XP per level. Level shown in the top resource bar as `Lv N · X/100`.
- Story sections gate by level: later segments require higher player levels to unlock.

## Inspiration

Habitica, ClassCraft, BitLife, Ring Fit Adventure, Oregon Trail.

## License

MIT
