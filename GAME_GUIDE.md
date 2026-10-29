# Outpost game guide (v1.5.0)

## Start here

Outpost is a single-player 3D defense game with a finite story campaign, The Last Ember. Protect yourself and the sanctuary. Either reaching zero health ends the defense; defeating the final boss wins the campaign.

Download the archive for your operating system from https://github.com/Chris020606/JavaGame/releases, extract it, and launch Outpost. The release includes Java. Keep the extracted folders together. Source instructions are below.

Select New Game, enter a name (up to 16 characters), click a class, then Begin or Enter. A blank name becomes Warden. New Game starts fresh; Load Game restores the last saved defense. Starting fresh does not replace the disk save until a save or automatic save occurs. Try Again restarts with the same class.

## Classes and equipment

| Class | Starting HP | Movement speed | Weapon and attack | Heavy attack |
| --- | ---: | ---: | --- | --- |
| Warrior | 100 | 5 units/s | Sword, wide three-hit combo | Heavy cleave |
| Archer | 90 | 6 units/s | Bow, fast arrow, 12-unit range | Power shot, 16-unit range, double damage |
| Mage | 80 | 5.2 units/s | Staff, piercing bolt, 10-unit range | Arcane surge, 12-unit range, 2.2x damage |

Class is fixed during a defense and saved with it. Warrior, Archer, and Mage have distinct models. Archer has bow idle, draw/release, and bow-running animations; Mage has spellcasting gestures. Each class starts with two potions. There is no mana or ammunition limit; stamina limits attacks, sprinting, and defensive moves.

Movement sets your facing. Attacks lock that facing until the animation finishes and slow movement to 30%. Warrior swings add a short committed step. Move briefly toward your target before attacking. Arrows and bolts travel in that direction without automatic aiming. An arrow stops at the first enemy it hits; a magic bolt can hit multiple enemies, once each. World decorations are visual and do not block movement or shots.

Warrior swings apply damage once, partway through the animation, in a forward arc. Regular attacks deal base damage; the third combo hit deals 1.4x, and heavy cleave deals 1.8x. Press near the end of a swing to queue the next attack. Archer regular arrows deal 0.85x base damage, and Mage regular bolts deal 1.1x. Damage is rounded to a whole number.

## Controls

| Action | Default |
| --- | --- |
| Move | W A S D |
| Sprint | Left Shift |
| Directional dodge | Left Ctrl |
| Orbit camera | Hold right mouse and move |
| Primary attack | Space or left mouse |
| Charged class attack | Hold/release Q |
| Timed parry / lunge | F / R |
| Workshop near the outpost | E |
| Open or close inventory | Tab |
| Drink potion during play | H |
| Save / load | F5 / F9 |
| Mute | M |
| Pause, close panel, or go back | Escape |
| Start or resume | Enter |

Settings lets you rebind keyboard actions, adjust camera sensitivity and volume, mute sound, and change fullscreen mode. Mouse buttons stay fixed. Escape and Enter are reserved. Inventory and workshop pause simulation; Escape closes the open panel first. Save/load shortcuts also work while paused. Loading pauses active combat; chapter-end and victory saves reopen their story screens.

## Inventory, crystals, and upgrades

Tab shows your class, equipment, damage, armor reduction, crystal balance, and potions. Click Use Potion there or press H during play. Each potion restores up to 60 HP, capped at your maximum. It is not consumed at full health, and cannot revive a defeated hero. You can carry five. Buy more at the workshop for 30 crystals each; potions do not drop from enemies.

Regular enemies award 15 crystals each; Warlords award 120. Stand within six units of the center and press E to shop. Time pauses while shopping. Each upgrade costs 45 times its current level, starting at 45 crystals.

| Upgrade | Effect |
| --- | --- |
| Outpost | +100 maximum integrity and a full repair |
| Armor | +2 damage reduction per level after level 1 |
| Character | +10 base damage, +25 maximum hero HP, and a full heal |

Base damage starts at 25 before weapon bonuses, outpost integrity at 250, and armor reduction at zero. Positive incoming damage to the player is reduced by armor but always deals at least 1 HP. Armor does not protect the outpost. Standing within six units of the center regenerates 3 + outpost level HP per second. A pulsing teal ring, ground crosses, and rising motes mark that exact radius. The HUD shows the healing rate when inside.

## Weapon loot and equipment

Every Warlord defeated unlocks the next tier of your class weapon. The two campaign bosses unlock Forged (+8 damage) and Runic (+16). Legendary (+24) remains supported for existing upgraded saves. Drops go directly into inventory and never disappear on the ground. Starting a new defense resets this progression.

Open Tab and click an unlocked weapon to equip it. The `>` marker identifies your current weapon. Locked tiers show what you can earn next. You can switch back to any unlocked tier. Bonuses do not stack: a Legendary weapon adds 24 damage total. Bonuses add to character upgrades and apply before each attack's damage multiplier. The weapon's tint changes with its tier; tiers share the sword, bow, or staff model of your class. Loot does not automatically replace the equipped weapon.

## Waves and enemies

The first wave begins after five seconds. Normal defense waves queue up to 36 enemies (4 + 3 times the global wave number). Arrivals are staggered along four trails. The next wave waits until both the queue and living enemies are empty, followed by eight seconds of rest. Rings and the minimap warn of imminent arrivals. Chapters 2 and 4 contain exactly one boss each, with no regular raiders. A boss has a gold tint, crown marker, gold ground ring, minimap marker, and a large health bar. Chapter boundaries wait for Continue; no eleventh wave is scheduled.

| Enemy | Behavior |
| --- | --- |
| Raider | Standard melee attacker |
| Scout | Faster movement, detects the hero farther away |
| Brute | More health, slower movement, stronger melee hits |
| Archer | Appears from wave 2; retreats when close and shoots arrows |
| Shaman | Appears from wave 3; fires slower, stronger magic |
| Warlord | Ash Warden (chapter 2) and Hollow King (chapter 4); melee duels with weapon loot |

Enemies choose the hero when close enough, otherwise attack the outpost. Melee enemies can flank and crowd around targets. Attack warnings show the direction of a pending strike. Move away before impact, or interrupt regular enemies with a hit. Warlords resist interruption and knockback: hitting them will not cancel their attack. Enemy health and speed increase with waves. Warlords have five times a normal raider’s health, slower movement, and a 1.2-second attack animation with impact after 0.95 seconds. They deal 22 + 3 times the wave number damage to the hero, or 30 + 3 times the wave number to the outpost. Their longer reach is shown by the attack warning. Chapters share the sanctuary clearing, with additional ruins in later chapters.

Gold numbers show damage dealt, red shows damage taken, and teal shows crystal rewards. The HUD displays health, outpost integrity, crystals, queued enemies, wave, attack recovery, and potion count. The minimap marks your hero, enemies, the outpost, and announced arrivals.

## Saves and settings

There is one save slot. F5, the pause menu, and Save & Main Menu save the defense. Chapter completions save automatically. Closing during an active defense, pause, chapter completion, or victory also saves. Closing from a defeated run does not overwrite your previous save. Settings are saved separately.

| OS | Data folder |
| --- | --- |
| Windows | `%APPDATA%/Outpost` (falls back to `Outpost` under your home folder) |
| macOS | `~/Library/Application Support/Outpost` |
| Linux | `$XDG_DATA_HOME/outpost`, or `~/.local/share/outpost` |

Files are `save.properties` and `settings.properties`. Replacing a file keeps the prior version as `.bak`. You can manually copy that backup over a damaged file while the game is closed. Invalid saves report an error without replacing the active defense. Back up the folder before editing files.

Saves retain your position, facing, class, potions, health, upgrades, crystals, kills, unlocked and equipped weapon tiers, wave queue, and living enemies. In-flight projectiles and attack windups are cancelled on load, and enemies get a short grace period. v1.4 writes save format 2 and reads format 1 saves from both earlier releases. Older saves retain their class and potions when present, defaulting to Warrior and two potions otherwise, and start with the Starter weapon. Older game releases reject format 2 saves; keep a backup if you plan to downgrade.

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

## The Last Ember campaign

The skyfires died a century ago. One ember beneath the sanctuary still warms the valley. Your warden must wake the beacon, reclaim the northern road, protect a refugee caravan, and confront the power that swallowed the other fires.

| Level | Encounter | Goal |
| --- | --- | --- |
| 1 — The Last Ember | Four defense waves | Wake the sanctuary beacon |
| 2 — The Ash Warden | One boss only | Reclaim the northern road |
| 3 — Road to Dawn | Four defense waves | Protect the caravan's crossing |
| 4 — The Hollow King | One final boss only | Bring morning back to the valley |

The top-center HUD shows the level, wave within that level, and waves left to clear (including the active wave). Boss chapters read BOSS ONLY. Defeat all queued and living enemies to complete a level. Read its aftermath and next briefing, open Inventory if desired, then Continue or Enter to advance. Defeating the Hollow King opens the victory ending. The Chronicle on the title and pause menus collects all chapter briefings. Classic Woodland selection has been removed; old saves use the campaign scenery.

## Movement, attacks, and the living valley

Hold Shift to sprint at 1.55 times your normal speed. Movement accelerates and decelerates smoothly. Ctrl dodges in your movement direction, or forward when standing still, using forward/back/side animations relative to your facing. A dodge lasts .34 seconds, prevents damage only during its .03–.24-second evasion window, and has a 1.5-second cooldown shown on the HUD. It does not protect the sanctuary. A dodge can cancel the last 48% of attack recovery, at stamina cost. You cannot begin an attack while dodging.

Warrior's three-hit combo now plays horizontal, diagonal, and chopping swings, while the heavy attack uses a jumping chop. Windup, contact, and recovery have distinct pacing. Archer has running-with-bow and draw/release transitions. Ranged aim still follows facing; there is no mouse aiming or stamina cost.

The imported-model sanctuary is surrounded by distant forest, hills, open sky, drifting clouds, birds, and campfire embers. These background areas are visual scenery. Border stones mark the bounded playable clearing; walking, sprinting, and dodging stop at its edge. Hold right mouse to orbit down toward the horizon or up for a tactical view. Environment props remain decorative rather than solid cover. Button pictograms supplement their text labels.

## Combat discipline

| Action | Stamina | Timing / reward |
| --- | ---: | --- |
| Light strike | 14 | Chain in the last .14 seconds of recovery or .23 seconds after it; later combo hits gain a 12% bonus |
| Charged heavy | 32 | Hold Q, then release at .75–1.05 seconds for a 1.6x bonus; overholding reduces the bonus |
| Lunge | 24 | R advances and attacks in the locked facing direction |
| Dodge | 22 | Ctrl commits to a direction; only the middle evasion window avoids damage |
| Parry | 12 | F opens a .22-second forward-facing window; failed attempts have .70 seconds of recovery |
| Sprint | 16 per second | An exhausted hero must recover to 25 stamina before sprinting again |

Stamina recovers at 22 per second after a short spending delay. Running out blocks the next attack; it does not queue an automatic attack when stamina returns. Early attack mashing drops the chain. The hotbar says CHAIN NOW during the valid continuation window. Heavy-charge timing and RIPOSTE READY are shown above it.

A successful parry restores 18 stamina and staggers a melee attacker for 1.1 seconds. Your next attack within 1.4 seconds gains a single 1.75x riposte multiplier. It works against incoming melee or projectiles from the front, not attacks from behind. You cannot parry ground magic, slams, or shockwaves. Charging slows movement and prevents parrying; a dodge can abandon the charge. Stamina is saved; transient charge/parry/riposte states reset on load.

## Skeletal boss moves

The Ash Warden is a towering, horned axe warrior. The Hollow King is a crowned skeletal caster. Their own animation sets replace the earlier raider mesh.

- **Gold cleave:** a broad, forward strike. Step outside the arc or time a parry.
- **Gold rush:** the boss locks your position, warns its lane, then charges along it. Leave the lane, evade during contact, or parry to stop the rush.
- **Red fault slam:** the marked ground at your previous position erupts. Leave the circle; the boss also emits an expanding shockwave. Evade the ring rather than parrying it.
- **Purple sigils:** several ground zones warn before detonating. Move clear before their delay expires. Each field can hit once.

At half health, the boss becomes ENRAGED: windups shorten and sigils expand from three zones to five. The HUD names the move and marks unblockable attacks. Targets stop tracking during the warning, so deliberate movement and counterattacks beat standing still and trading hits. Boss attacks focus the hero during duels; ordinary raiders still threaten the sanctuary.

## Graphics quality

Settings offers High (2048 shadow atlas, scene width up to 1920) or Balanced (1024 shadows, scene width up to 1440 and less bloom). Both retain material lighting, filtered shadows, anti-aliasing, tone mapping, fog, and wind effects. The UI is drawn after post-processing at native display resolution. Choose Balanced if the effects cost too much performance on your computer.
