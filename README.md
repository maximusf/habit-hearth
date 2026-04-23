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
- **Jetpack DataStore** for local persistence
- **Gemini 3.0 Flash** for AI story generation
- Local-only auth with bcrypt-style password hashing
- Gradle with Kotlin DSL and version catalog

No cloud backend. All data stays on-device.

## Project Structure

```
com.project.habithearth/
├── data/                    # Persistence + auth
├── ui/
│   ├── account/             # Login, account creation
│   ├── components/          # Shared composables
│   ├── home/                # Task list + building directory
│   ├── map/                 # Hex-grid village map
│   ├── model/               # Data models (HabitTask, etc.)
│   ├── navigation/          # Routes + top resource bar
│   ├── profile/             # Player profile + settings
│   ├── state/               # GameStateViewModel (central state)
│   ├── story/               # AI narrative screen + ViewModel
│   ├── tasks/               # Task creation
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

## Inspiration

Habitica, ClassCraft, BitLife, Ring Fit Adventure, Oregon Trail.

## License

MIT