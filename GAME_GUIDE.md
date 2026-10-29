# Outpost game guide (v1.6.0)

## Start here

Outpost is a single-player 3D defense game with a finite story campaign, The Last Ember. Protect yourself and the sanctuary. Either reaching zero health ends the defense; defeating the final boss wins the campaign.

Download the archive for your operating system from https://github.com/Chris020606/JavaGame/releases, extract it, and launch Outpost. The release includes Java. Keep the extracted folders together. Source instructions are below.

Select New Game, enter a name (up to 16 characters), choose a class and difficulty, then Begin or Enter. A blank name becomes Warden. New Game starts fresh; Load Game restores the last saved defense. Starting fresh does not replace the disk save until a save or automatic save occurs. Try Again restarts with the same class.

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

Tab opens an equipment grid with a rotatable 3D player preview, five equipped slots, stat comparisons, and potion storage. Click an owned item to equip it, or drag it into its matching slot. Right-click an equipped armor slot to remove that piece. Drag the preview to rotate the character. Armor and weapons are visible on the animated hero during play.

Potions restore up to 60 HP, capped at maximum health. Click Use Potion or press H during play. You can carry five and buy more for 30 crystals in the workshop's Supplies tab. Full-health heroes do not consume potions.

Regular enemies award 15 crystals; bosses award 120. Stand within six units of the sanctuary and press E. Time pauses in the workshop. Training cards show the current level, exact improvement, and price. New stat training starts at 45 crystals; the next price is `45 × current level + 10 × (current level − 1)²`. Resilience and Sanctuary cost `45 × current level`. All seven tracks cap at level 10. Previous saves retain their older combined character upgrades.

| Training | Effect per purchase |
| --- | --- |
| Vitality | +20 maximum and current HP |
| Power | +5 base damage |
| Endurance | +15 maximum stamina |
| Recovery | +3 stamina regenerated per second |
| Agility | +4% of the class's starting movement speed |
| Resilience | +2 damage reduction |
| Sanctuary | +100 maximum integrity and full repair |

Base damage starts at 25, stamina at 100, recovery at 22/second, and outpost integrity at 250. Defense reduces incoming player damage to a minimum of 1; it does not protect the sanctuary. Standing inside its marked six-unit ring heals `3 + sanctuary level` HP/second.

## Weapon loot and equipment

Each defeated boss unlocks the next class weapon tier. The Equipment Forge also sells tiers directly, including Legendary during this campaign. Buying a higher tier grants access to the lower tiers. Purchases unlock gear; equip it in Tab.

| Tier | Weapon price / damage bonus | Armor set price / full-set defense |
| --- | --- | --- |
| Forged / Iron | 100 / +8 | 100 / +7 |
| Runic | 240 / +16 | 220 / +14 |
| Legendary / Dawnsteel | 450 / +24 | 380 / +21 |

Warriors switch between a sword, longsword, waraxe, and Dawnblade with different geometry. Archers progress from bows to crossbows; mages use a staff, wand, skull staff, and Dawnstaff. Iron, Runic, and Dawnsteel sets each include a helmet, chest/shoulder armor, greaves, and boots. Mix individual pieces freely. Head/chest/legs/boots provide 1/3/2/1 defense per tier; boots also add .12 movement speed per tier. Equipped bonuses replace lower tiers rather than stacking. Starting a new campaign resets its equipment and training.

## Difficulty

Choose Easy, Normal, Hard, or Extreme on the name/class screen. The choice is fixed for that campaign, saved with it, and remembered as the preference for new games.

| Difficulty | Enemy HP | Damage | Speed | Raid size | Concurrent melee attackers on hero |
| --- | --- | --- | --- | --- | --- |
| Easy | 75% | 65% | 90% | 80% | 1 |
| Normal | 100% | 100% | 100% | 100% | 2 |
| Hard | 125% | 135% | 108% | 115% | 3 |
| Extreme | 160% | 175% | 116% | 135% | 4 |

Harder settings also shorten attack recoveries, windups, and arrivals. Raid sizes round to whole enemies and cap at 36. Boss-only chapters always contain one boss. Ranged fire and boss moves are independent of the regular melee assault limit.

## Waves and enemies

The first wave begins after five seconds. Normal defense waves queue up to 36 enemies (4 + 3 times the global wave number). Arrivals are staggered along four trails; the Frozen Pass uses its north/south approaches. The next wave waits until both the queue and living enemies are empty, followed by eight seconds of rest. Rings and the minimap warn of imminent arrivals. Chapters 2 and 4 contain exactly one boss each, with no regular raiders. A boss has a gold tint, crown marker, gold ground ring, minimap marker, and a large health bar. Chapter boundaries wait for Continue; no eleventh wave is scheduled.

| Enemy | Behavior |
| --- | --- |
| Raider | Standard melee attacker |
| Scout | Faster movement, detects the hero farther away |
| Brute | More health, slower movement, stronger melee hits |
| Archer | Appears from wave 2; retreats when close and shoots arrows |
| Shaman / Frost Hexer | Fires magic; channels an interruptible ward to heal wounded allies |
| Warlord | Ash Warden (chapter 2) and Hollow King (chapter 4); melee duels with weapon loot |

Enemies commit briefly to a target and use detection hysteresis to avoid constantly switching between hero and sanctuary. Scouts flank widely; ordinary melee enemies circle and backstep during recovery; brutes press forward. Nearby allies steer apart. Ranged enemies retreat when crowded, strafe during recovery, reposition around blocked firing lanes, and lead a moving hero. Shamans and frost hexers can heal a wounded nearby ally after a visible, interruptible channel. Melee attackers coordinate their windups according to difficulty. Attacks require facing the target and lock their aim during the warning, giving you a chance to evade. Hits interrupt regular enemies; bosses resist interruption. Movement accelerates smoothly and turns at a bounded rate, with idle, walking, running, strafing, and attack clips.

The meadow uses human raiders, scouts, brutes, archers, and shamans. The cemetery contains the skeletal Ash Warden. The Frozen Pass uses Boneguards, Rattlescouts, Frost Reavers, Bone Archers, and Frost Hexers. The necropolis contains the Hollow King. Each chapter changes scenery, sky, fog, palette, and lighting. Snow drifts through the pass; cemetery gates, graves and crypts replace the meadow's trees and camp. Scenery is decorative and does not provide physical cover; the playable boundary remains ±22 units.

Gold numbers show damage dealt, red shows damage taken, and teal shows crystal rewards. The HUD displays health, outpost integrity, crystals, queued enemies, wave, attack recovery, and potion count. The minimap marks your hero, enemies, the outpost, and announced arrivals.

## Saves and settings

There is one save slot. F5, the pause menu, and Save & Main Menu save the defense. Chapter completions save automatically. Closing during an active defense, pause, chapter completion, or victory also saves. Closing from a defeated run does not overwrite your previous save. Settings are saved separately.

| OS | Data folder |
| --- | --- |
| Windows | `%APPDATA%/Outpost` (falls back to `Outpost` under your home folder) |
| macOS | `~/Library/Application Support/Outpost` |
| Linux | `$XDG_DATA_HOME/outpost`, or `~/.local/share/outpost` |

Files are `save.properties` and `settings.properties`. Replacing a file keeps the prior version as `.bak`. You can manually copy that backup over a damaged file while the game is closed. Invalid saves report an error without replacing the active defense. Back up the folder before editing files.

Saves retain your position, facing, class, potions, health, upgrades, crystals, kills, difficulty, independent stat training, unlocked armor sets, equipped armor pieces, upgraded stamina, unlocked and equipped weapon tiers, wave queue, and living enemies. In-flight projectiles and attack windups are cancelled on load, and enemies get a short grace period. v1.6 writes save format 2 and reads format 1 saves from both earlier releases. Older saves retain their class and potions when present, defaulting to Warrior and two potions otherwise, and start with the Starter weapon. Older game releases reject format 2 saves; keep a backup if you plan to downgrade.

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

Character and most weapon assets are from KayKit under CC0; modular armor and Dawn weapons are original generated meshes. See THIRD_PARTY_NOTICES.md and the included licenses for dependencies. See DEVELOPMENT_NOTES.md for known limits and future work.

## The Last Ember campaign

The skyfires died a century ago. One ember beneath the sanctuary still warms the valley. Your warden must wake the beacon, reclaim the northern road, protect a refugee caravan, and confront the power that swallowed the other fires.

| Level | Encounter | Goal |
| --- | --- | --- |
| 1 — The Last Ember | Meadow / four raider waves | Wake the sanctuary beacon |
| 2 — The Ash Warden | Cemetery / one skeleton boss | Reclaim the northern road |
| 3 — Road to Dawn | Frozen Pass / four skeleton waves | Protect the caravan's crossing |
| 4 — The Hollow King | Necropolis / one final skeleton boss | Bring morning back to the valley |

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
