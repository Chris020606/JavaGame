# Development progress and future work

## Current update: heroes and highlands

Implemented distinct Ranger and Mage player meshes with shared locomotion and class-specific bow-release / spellcasting clips. Asset generation is reproducible with the bundled CC0 sources. Switching class replaces and unloads GPU assets, resets animation lookup state, and validates skeleton compatibility.

Added a second visual stage, Highland Ruins, reached at wave 6 and restored from saved wave progress. It features a slate ground palette, stone colonnades, rocky perimeter, and distant mountain silhouettes. Woodland also gains distant silhouettes; both stages show the healing boundary. Stage II retains the same arena layout, combat rules, and endless-wave flow; a separate campaign/map selector remains future work. HUD panels gain shadows and teal accents, with a stage label under player stats.

Validation: unit tests, visible desktop smoke tests at high and standard DPI, and native macOS packaging. Smoke coverage now includes restored wave-6 scenery and stage save/load, alongside class switching, ranged combat, equipment, and prior regressions. See the latest commit for the exact checked revision; cross-platform builds still require CI.

## v1.2.0 scope

Added one Warlord boss at the start of every fifth wave, stronger melee and siege damage, longer attack warnings, interruption resistance, gold/crown visuals, and a dedicated boss health bar. Boss rewards grant 120 crystals and unlock Forged, Runic, then Legendary class weapons. The inventory supports equipping unlocked tiers with damage bonuses and weapon tints. Added save format 2 with format 1 migration; equipped tiers cannot exceed unlocked tiers. Tests cover scheduling and resumed queues, boss mechanics, rewards, equipment, save migration, and desktop boss impact/loot/equipping. Version is 1.2.0. The game still has one arena.

## v1.1.0 scope

Implemented Warrior, Archer, and Mage selection on the name screen; class health, movement, and equipped weapons; player arrows and piercing magic with swept collision; heavy ranged attacks; a paused inventory; five-slot healing potion storage; potion use and workshop purchases; configurable inventory/potion keys; class and inventory persistence with defaults for old saves; and the complete GAME_GUIDE.md. That release used Gradle version 1.1.0. Existing local formatting edits in AppData and CombatFeedback were preserved.

This is a complete, bounded update. No unfinished feature is enabled. Use git history and the release notes to identify the exact shipped revision. Cross-platform build results are recorded in GitHub Actions, not inferred from the local macOS build.

## Future work, in suggested order

1. Polish ranged animation transitions (bow draw/hold/release) and add directional aim feedback. Distinct Ranger/Mage meshes and attack clips now exist.
2. Expand equipment with distinct weapon models, armor slots, randomized loot, and more comparison stats. Class weapon tiers and format migration now exist.
3. Expand the highland stage into a distinct playable layout with route changes and level selection. The wave-6 visual stage currently shares the woodland combat layout.
4. Add additional boss types and new enemy behaviors. Warlord waves now exist. Coordinate numeric enemy variants in Enemy, WorldAssets, WaveDirector, and SaveGame bounds; add combat and round-trip save tests.
5. Playtest ranged class balance at higher waves, including piercing damage, enemy pressure, and potion economy.
6. Add optional aim controls, range feedback, and terrain collision if desired. Current aim follows movement direction and decorative props do not obstruct projectiles.
7. Split Game's UI and combat coordination into smaller components as these systems grow.

## Useful checks for the next session

```sh
./gradlew test
./gradlew inputSmokeTest
./gradlew inputSmokeTest --args=--standard-dpi
./gradlew packageGame
```

The input test uses a temporary profile. It intentionally opens a visible window: a hidden window stalled inside native EndDrawing on this macOS host during v1.1 work. It covers name/menu flow, resizing, sword timing, damage, saves and corruption, settings/rebinding, enemy ranged attacks, Archer selection and ranged hits, inventory pause, potion persistence, Warlord attacks and rewards, and equipping weapon loot. Unit tests cover all classes' persistence, potion constraints, projectile sweep/range/piercing, and existing combat and economy rules.

Release workflow: commit and push master, create and push the version tag, monitor all four desktop builds, and verify the release has four downloadable archives. If CI fails, diagnose before publishing a corrective tag; do not silently treat a pending build as a completed release.
