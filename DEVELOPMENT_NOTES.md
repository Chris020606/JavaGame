# Development progress and future work

## v1.1.0 scope

Implemented Warrior, Archer, and Mage selection on the name screen; class health, movement, and equipped weapons; player arrows and piercing magic with swept collision; heavy ranged attacks; a paused inventory; five-slot healing potion storage; potion use and workshop purchases; configurable inventory/potion keys; class and inventory persistence with defaults for old saves; and the complete GAME_GUIDE.md. Version is now 1.1.0 in Gradle. Existing local formatting edits in AppData and CombatFeedback were preserved.

This is a complete, bounded update. No unfinished feature is enabled. Use git history and the release notes to identify the exact shipped revision. Cross-platform build results are recorded in GitHub Actions, not inferred from the local macOS build.

## Future work, in suggested order

1. Give Archer and Mage distinct animated character models and ranged animation clips. Currently the player uses the knight mesh and shared attack animations with a class-specific held weapon.
2. Expand inventory beyond potions: equipment slots, weapon drops, comparison UI, and explicit save migration. Current equipment is determined by class and cannot be swapped mid-run.
3. Add a second arena and a level-selection or campaign flow. Currently there is one arena and endless waves.
4. Add boss waves and new enemy behaviors. Coordinate numeric enemy variants in Enemy, WorldAssets, WaveDirector, and SaveGame bounds; add combat and round-trip save tests.
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

The input test uses a temporary profile. It intentionally opens a visible window: a hidden window stalled inside native EndDrawing on this macOS host during v1.1 work. It covers name/menu flow, resizing, sword timing, damage, saves and corruption, settings/rebinding, enemy ranged attacks, Archer selection and ranged hits, inventory pause, and potion persistence. Unit tests cover all classes' persistence, potion constraints, projectile sweep/range/piercing, and existing combat and economy rules.

Release workflow: commit and push main, create and push the version tag, monitor all four desktop builds, and verify the release has four downloadable archives. If CI fails, diagnose before publishing a corrective tag; do not silently treat a pending build as a completed release.
