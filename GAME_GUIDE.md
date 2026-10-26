# Outpost game guide (v1.2.0)

## Start here

Outpost is a single-player 3D survival game. Protect yourself and the settlement at the center of the map. Either reaching zero health ends the defense. There is currently one arena with endless waves, rather than a campaign or a final victory screen.

Download the archive for your operating system from https://github.com/Chris020606/JavaGame/releases, extract it, and launch Outpost. The release includes Java. Keep the extracted folders together. Source instructions are below.

Select New Game, enter a name (up to 16 characters), click a class, then Begin or Enter. A blank name becomes Warden. New Game starts fresh; Load Game restores the last saved defense. Starting fresh does not replace the disk save until a save or automatic save occurs. Try Again restarts with the same class.

## Classes and equipment

| Class | Starting HP | Movement speed | Weapon and attack | Heavy attack |
| --- | ---: | ---: | --- | --- |
| Warrior | 100 | 5 units/s | Sword, wide three-hit combo | Heavy cleave |
| Archer | 90 | 6 units/s | Bow, fast arrow, 12-unit range | Power shot, 16-unit range, double damage |
| Mage | 80 | 5.2 units/s | Staff, piercing bolt, 10-unit range | Arcane surge, 12-unit range, 2.2x damage |

Class is fixed during a defense and saved with it. All heroes use the existing knight character model; their held weapon changes. Ranged attacks currently share the knight's attack animations. Each class starts with two potions. There is no mana, stamina, or ammunition limit.

Movement sets your facing. Attacks lock that facing until the animation finishes and slow movement to 40%. Move briefly toward your target before attacking. Arrows and bolts travel in that direction without automatic aiming. An arrow stops at the first enemy it hits; a magic bolt can hit multiple enemies, once each. World decorations are visual and do not block movement or shots.

Warrior swings apply damage once, partway through the animation, in a forward arc. Regular attacks deal base damage; the third combo hit deals 1.4x, and heavy cleave deals 1.8x. Press near the end of a swing to queue the next attack. Archer regular arrows deal 0.85x base damage, and Mage regular bolts deal 1.1x. Damage is rounded to a whole number.

## Controls

| Action | Default |
| --- | --- |
| Move | W A S D |
| Orbit camera | Hold right mouse and move |
| Primary attack | Space or left mouse |
| Heavy class attack | Q |
| Workshop near the outpost | E |
| Open or close inventory | Tab |
| Drink potion during play | H |
| Save / load | F5 / F9 |
| Mute | M |
| Pause, close panel, or go back | Escape |
| Start or resume | Enter |

Settings lets you rebind keyboard actions, adjust camera sensitivity and volume, mute sound, and change fullscreen mode. Mouse buttons stay fixed. Escape and Enter are reserved. Inventory and workshop pause simulation; Escape closes the open panel first. Save/load shortcuts also work while paused. Loading always pauses so you can resume when ready.

## Inventory, crystals, and upgrades

Tab shows your class, equipment, damage, armor reduction, crystal balance, and potions. Click Use Potion there or press H during play. Each potion restores up to 60 HP, capped at your maximum. It is not consumed at full health, and cannot revive a defeated hero. You can carry five. Buy more at the workshop for 30 crystals each; potions do not drop from enemies.

Regular enemies award 15 crystals each; Warlords award 120. Stand within six units of the center and press E to shop. Time pauses while shopping. Each upgrade costs 45 times its current level, starting at 45 crystals.

| Upgrade | Effect |
| --- | --- |
| Outpost | +100 maximum integrity and a full repair |
| Armor | +2 damage reduction per level after level 1 |
| Character | +10 base damage, +25 maximum hero HP, and a full heal |

Base damage starts at 25 before weapon bonuses, outpost integrity at 250, and armor reduction at zero. Positive incoming damage to the player is reduced by armor but always deals at least 1 HP. Armor does not protect the outpost. Standing within six units of the center regenerates 3 + outpost level HP per second.

## Weapon loot and equipment

Every Warlord defeated unlocks the next tier of your class weapon. The first three unlock Forged (+8 damage), Runic (+16), and Legendary (+24). Further Warlords still award 120 crystals but do not add more tiers. Drops go directly into inventory and never disappear on the ground. Starting a new defense resets this progression.

Open Tab and click an unlocked weapon to equip it. The `>` marker identifies your current weapon. Locked tiers show what you can earn next. You can switch back to any unlocked tier. Bonuses do not stack: a Legendary weapon adds 24 damage total. Bonuses add to character upgrades and apply before each attack's damage multiplier. The weapon's tint changes with its tier; tiers share the sword, bow, or staff model of your class. Loot does not automatically replace the equipped weapon.

## Waves and enemies

The first wave begins after five seconds. Each wave queues up to 36 enemies (4 + 3 times the wave number). Arrivals are staggered along four trails. The next wave waits until both the queue and living enemies are empty, followed by eight seconds of rest. Rings and the minimap warn of imminent arrivals. Every fifth wave starts with exactly one Warlord, followed by the usual raiders; the boss replaces one regular arrival. A boss wave banner announces it. A living Warlord has a gold tint, crown marker, gold ground ring, minimap marker, and a large health bar.

| Enemy | Behavior |
| --- | --- |
| Raider | Standard melee attacker |
| Scout | Faster movement, detects the hero farther away |
| Brute | More health, slower movement, stronger melee hits |
| Archer | Appears from wave 2; retreats when close and shoots arrows |
| Shaman | Appears from wave 3; fires slower, stronger magic |
| Warlord | One leads every fifth wave; tough melee boss with heavy attacks and weapon loot |

Enemies choose the hero when close enough, otherwise attack the outpost. Melee enemies can flank and crowd around targets. Attack warnings show the direction of a pending strike. Move away before impact, or interrupt regular enemies with a hit. Warlords resist interruption and knockback: hitting them will not cancel their attack. Enemy health and speed increase with waves. Warlords have five times a normal raider’s health, slower movement, and a 1.2-second attack animation with impact after 0.95 seconds. They deal 22 + 3 times the wave number damage to the hero, or 30 + 3 times the wave number to the outpost. Their longer reach is shown by the attack warning. The game still has one arena.

Gold numbers show damage dealt, red shows damage taken, and teal shows crystal rewards. The HUD displays health, outpost integrity, crystals, queued enemies, wave, attack recovery, and potion count. The minimap marks your hero, enemies, the outpost, and announced arrivals.

## Saves and settings

There is one save slot. F5, the pause menu, and Save & Main Menu save the defense. Closing during an active or paused defense also saves. Closing from a defeated run does not overwrite your previous save. Settings are saved separately.

| OS | Data folder |
| --- | --- |
| Windows | `%APPDATA%/Outpost` (falls back to `Outpost` under your home folder) |
| macOS | `~/Library/Application Support/Outpost` |
| Linux | `$XDG_DATA_HOME/outpost`, or `~/.local/share/outpost` |

Files are `save.properties` and `settings.properties`. Replacing a file keeps the prior version as `.bak`. You can manually copy that backup over a damaged file while the game is closed. Invalid saves report an error without replacing the active defense. Back up the folder before editing files.

Saves retain your position, facing, class, potions, health, upgrades, crystals, kills, unlocked and equipped weapon tiers, wave queue, and living enemies. In-flight projectiles and attack windups are cancelled on load, and enemies get a short grace period. v1.2 writes save format 2 and reads format 1 saves from both earlier releases. Older saves retain their class and potions when present, defaulting to Warrior and two potions otherwise, and start with the Starter weapon. Older game releases reject format 2 saves; keep a backup if you plan to downgrade.

## Build, test, and release

Install JDK 25 or newer. From the repository root:

```sh
./gradlew run
./gradlew test
./gradlew inputSmokeTest
./gradlew packageGame
```

Use `gradlew.bat` on Windows. The first run needs internet access for Gradle and dependencies. `--offline` works once cached. The desktop smoke test needs a graphical session; it uses a temporary profile and exports screenshots under `build/`. Unit tests do not require a game window. Add `--args=--standard-dpi` to the smoke task to check standard DPI.

`packageGame` builds a native application under `build/native/`, including a Java runtime and game resources. Build on each target OS. Gradle's version controls the packaged application version. The GitHub Actions workflow tests and builds Windows x64, Linux x64, macOS ARM64, and macOS x64; pushing a `v*` tag publishes their archives after every platform succeeds. The guide and development notes ship inside the package resources.

For isolated manual testing, Java system property `outpost.dataDir` overrides the data folder. `Main --smoke-test --hidden` runs 120 frames with temporary user data when launched with those arguments.

## Code map

| File or folder | Responsibility |
| --- | --- |
| `Main.java` | Window lifecycle and game loop |
| `Game.java` | Screens, input, simulation coordination, HUD and menus |
| `HeroClass.java` | Hero health, movement, and attack definitions |
| `HeroShot.java` | Player projectile movement, swept collision, piercing hit tracking |
| `Combat.java` | Sword timing and attack arc geometry |
| `Progression.java` | Health, inventory, rewards, upgrades, save properties |
| `Entities/Player.java` | Camera-relative movement, facing and animation |
| `Entities/Enemy.java` | Enemy stats, movement, windups, stun and knockback |
| `Projectile.java` | Enemy projectiles and swept collision |
| `WaveDirector.java` | Arrival scheduling, wave queue and rest periods |
| `WorldAssets.java` | Shared GPU models, weapon attachment, map decoration |
| `Scene/` | Entity container and orbit camera |
| `Settings.java`, `Config/Controls.java` | User settings and keyboard bindings |
| `SaveGame.java`, `AppData.java` | Validated snapshots, data locations and atomic file writes |
| `CombatFeedback.java`, `GameAudio.java`, `Menu.java` | Floating labels, audio and UI helpers |
| `assets/AssetPaths.java` | Resource lookup for source and packaged launches |
| `src/test/java/` | Combat, economy, persistence, hero and desktop input checks |
| `resources/`, `tools/`, `packaging/` | Models/audio, asset/release scripts, application icons |
| `.github/workflows/build.yml` | Cross-platform package and release pipeline |

Gameplay rules are separated from native rendering where practical so JUnit can check them directly. Rendering and input still live together in Game. Enemy variants are numeric: 0 Raider, 1 Scout, 2 Brute, 3 Archer, 4 Shaman, 5 Warlord. Update persistence bounds and rendering together if adding variants. Add class rules in HeroClass and keep equipment rendering consistent in WorldAssets.

Character and weapon assets are from KayKit under CC0. See THIRD_PARTY_NOTICES.md and the included licenses for dependencies. See DEVELOPMENT_NOTES.md for known limits and future work.
