# Where to change things

| Change | Primary files | What to check |
| --- | --- | --- |
| Starting class stats and attack timings | `HeroClass`, `Combat` | `HeroTest`, `CombatTest`, native combat-motion |
| Solo costs, stamina/mana and defense windows | `Entities/CombatDiscipline`, `Entities/Player`, `Game` | discipline tests, input smoke |
| Co-op combat or economy | `CoopWorld`, `CoopBalance` | co-op unit/campaign tests, two-client native check |
| Gear prices, ownership and bonuses | `Progression`, `Entities/ArmorSlot` | ownership/save tests, inventory regression |
| Inventory layout and item previews | `EquipmentScreen`, `WorldAssets` | native class/ownership/inventory review |
| Title menu | `MainMenuScreen`, action handling in `Game` | full input smoke; keep button hit boxes in sync |
| HUD and action belt | `HudScreen`, `HudLayout`, `Menu` | native HUD review at multiple sizes |
| Keys/settings | `Config/Controls`, `Settings`, settings UI in `Game` | settings and input tests; preserve old defaults |
| Chapters and wave compositions | `Campaign`, `WaveDirector`, `WaveFormation` | campaign/formation tests and save resume |
| Enemy AI and attack windows | `Entities/Enemy`, `Entities/SquadTactics`, `Game`, `CoopWorld` | combat and co-op tests; test every affected variant |
| Boss move rules and hazards | `Entities/BossMove`, `BossHazard`, solo/co-op impact resolution | boss regression, hazard geometry, balance |
| Terrain/roads/scenery | `EnvironmentLayout`, `RoadLayout`, `ArenaNavigation`, `WorldAssets` | terrain/navigation tests and art review |
| Lighting, time and shaders | `DayNightCycle`, `ChapterLook`, `ModernRenderer`, `resources/outpost` shaders | day/night art review and package launch |
| Animation transitions or weapon grips | `Entities/Player`, `Entities/Entity`, `tools/prepare_assets.py` | native combat-motion on each affected tier |
| Music and effects | `GameAudio`, `resources/audio`, authoring tools | native load logs, mute/volume, listening |
| Solo persistence | `SaveGame`, `Progression`, `AppData`, `PlayerSaves` | old-save migration and invalid-data tests |
| Network serialization | `CoopProtocol`, `CoopState`, `CoopInput` | protocol fixtures and two-client check |
| Portable downloads | `build.gradle`, `tools/archive_release.py`, GitHub workflow | native package smoke and all four CI platforms |

## Extending an enemy variant

Published variants are numeric: 0 raider, 1 scout, 2 brute, 3 archer, 4 shaman, 5 boss. Do not insert an enum or shift these values casually: saves and network packets store them.

A new variant requires coordinated simulation, rendering, health/damage bounds, roster scheduling, solo save validation, co-op packet bounds and tests. Adding a number only in `Enemy` can load successfully in one mode and fail or render the wrong rig in another. The archived roadmap branch contains an unfinished variant-6 experiment; it is not part of this baseline.

## Changing equipment

Ownership is a bit mask, not “everything below the highest tier.” `ownsWeapon`/`ownsArmor` decide availability; the `weaponUnlocked`/`armorUnlocked` values represent the highest owned tier. Keep ownership, current equipment, inventory buttons and save validation consistent.

Boss rewards grant one missing weapon tier; armor is crafted. The top-tier Archer uses a two-handed siege crossbow, tier 2 uses a hand crossbow. Both models and animation clips must match. Solo `Progression` and co-op chapter loadouts are separate systems.

## Safe next extractions

The remaining `Game` responsibilities can be moved gradually: settings view, player-history view, tutorial presentation, then attack coordination. Give a screen explicit input data and return an intent, as `MainMenuScreen` does. Preserve update/render ordering and retain the existing native regression while moving each section.

Keep balance changes out of a structural refactor. Run the smallest relevant check first, then the full native regression when input/state routing changes. Do not treat formatting as evidence that game feel improved.
