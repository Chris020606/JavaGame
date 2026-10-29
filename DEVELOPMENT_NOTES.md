# Development progress and future work

## v1.9.0: Fitted class armor, restored swings, living meadow, and hero welcome

Replaced all rigid box armor with 36 class-specific skinned GLBs. tools/fit_armor.py derives chest/gauntlets, greaves and boots from the corresponding CC0 KayKit character surfaces, preserving each class's skeleton and weights. Head pieces fit the class's bounds: full Warrior helm, open-faced Archer helmet, Mage's armored wizard hat. Covered original surfaces are masked while equipped; displayed armor uses the exact current player clip/frame, including the independent inventory pose. Averaged clearance normals preserve welded seams instead of opening cracks. Removed the old rigid OBJ pieces and kept original Dawn weapons in their separate generator. Updated CC0 attribution and reproduction instructions.

Found the missing Warrior animation root cause: raylib's 32-byte ModelAnimation.name buffer truncates the 32-character horizontal-slice name to 31 characters. The previous lookup silently ignored it and retained the prior pose. Entity now resolves that exact native-name limit; required Player clips fail explicitly if genuinely missing. All three light swings have hand-motion assertions rather than only distinct-index checks. Lunge immediately poses its selected stab clip. Chapter completion waits for the final visible swing to finish, with new attacks blocked while the cleared chapter awaits recovery. A desktop regression checks both the uninterrupted finishing swing and the eventual chapter transition.

The meadow now has a connected winding stream with imported river bend/straight tiles, a wooden crossing beyond the playable edge, water lilies, interior trees, red/yellow flower colonies, understory, fungi, stumps and fallen logs. Small butterflies flutter around flower beds. Water uses subtle animated ripple normals; lower meadow haze/ambient fill improves contrast without changing the other chapters. Scenery stays decorative and does not add collision. Added the selected original Kenney meshes to the existing source/license manifest.

Rebuilt the name/class screen with independent animated hero portraits, class-colored accents, character flavor text, ember motes and a Light the Beacon action. Portrait resources are isolated from the active hero, so changing the selection cannot mutate an existing character. Updated native mouse-input checks for the new layout.

Rewrote README for players: the story, class choices, combat tips, journey, visible gear, no-grind playtest, controls and where to play. Replaced its technical test/menu captures with fresh in-game landscape/action shots and the new welcome screen. Screenshot gallery mode hides overlays only for capture; gameplay is unchanged. Technical details remain here and in the guide. README still accurately distinguishes the current development build from published v1.2.0 downloads. Bundled documentation includes the four new referenced screenshots.

Validation: 50 unit tests; complete standard/high-DPI desktop suites; each Warrior light swing moves the weapon hand; final blows preserve their animation before chapter completion; class selection and playtest/campaign save isolation. Gallery review covers all three armor tiers on each class while standing, running and dodging. All 36 fitted rigs load without asset warnings. Packaged v1.9.0 completes --playtest --smoke-test from /tmp without asset/shader warnings; all new README images/links are present in the package. Additional asset checks verify finite geometry, normalized weights, valid joints and bounded indices on all 36 pieces.

Next roadmap: terrain collision and navigation around cover; smoother animation blending and a full crossbow reload; longer balance/audio playtests on varied hardware. No requested feature has been deferred.

## v1.8.0: Instant all-unlocked playtest

Added a title-screen playtest entry with class, difficulty, and chapter selection. A direct --playtest launcher argument opens the full inventory immediately. The sandbox grants every weapon/armor tier, equips Dawnsteel, starts with five potions and 100,000 crystals, and optionally maxes all seven training tracks without inventing kills. Pause > Playtest Tools resets the kit or switches class/chapter. Save/load and automatic saves use playtest-save.properties; normal campaign saves remain separate, and the main Load Game action still loads the campaign.

Validation: 50 unit tests pass, including the sandbox factory for every class and both training settings. Both desktop DPI suites pass menu selection, equipped gear, chapter jump, separate save/resume, and byte-for-byte campaign preservation. The packaged v1.8.0 launcher completes --playtest --smoke-test outside the repository; its isolated profile contains the complete kit in playtest-save.properties and no normal campaign save. No asset/shader warnings. README and guide explain direct launch and playtest controls. Remaining roadmap stays in the sections below; no requested feature work remains.

## v1.7.0: Authored scenery, workshop redesign, soundtrack, and combat effects

Completed the presentation polish request. Replaced repeated rings/grids and evenly spaced boundary stones with EnvironmentLayout: winding approach paths, an authored expedition camp, grouped vegetation/rock colonies, rotated burial plots, broken cemetery walls and gateways, uneven mountain outcrops, and asymmetrical crypt/obelisk landmarks. Seeded groves create a stable, irregular horizon. Both lit and shadow passes consume the same cached prop layout. Central healing space stays clear; decorative scenery still has no collision/cover. Grave earth and crooked pines have more restrained tints. Camp lighting follows the new fire locations.

Replaced the workshop card grid with a dedicated WorkshopScreen. Its sidebar selects Training, Equipment Forge, and Supplies and retains core hero stats. Seven aligned training rows show attribute, level/progress, next improvement, price and purchase action without squeezed card wrapping. Forge columns align weapon and armor offerings by tier. Supplies has visible five-potion storage. Navigation highlight and content update together on the click frame. The panel fits the 1000×680 minimum window; purchase rules, time pausing, and save behavior are preserved.

Composed five original 24-second stereo themes for title/meadow/cemetery/frost/necropolis and a synchronized percussion stem. GameAudio streams all six, crossfades scene themes, adds percussion in active combat, and fades intensity in paused menus. Master audio and mute affect music and Foley immediately. Rebuilt sound effects using layered filtered noise, pitched resonances, envelopes and stereo reflections; swings/hits have three variations. Bow, magic, parry, dodge/lunge, boss slam, healing ward, and upgrade cues are distinct. Music ducks briefly behind effects. Audio remains optional without a device. Rebuild all audio with tools/prepare_audio.py; there are no third-party recordings/samples.

CombatVfx replaces attack wireframes with tapered additive weapon ribbons, spell/ring decals, soft impact sparks, charge-lane motes, spell wisps, projectile trails, and modeled arrow shafts/tips. Boss sigils show a textured warned seal before emitting rising particles; shockwaves use textured expanding rings and dust. Parry/charge effects and healing channels share the presentation. Particle count is capped at 240; particles pause/clear with gameplay. Effect textures are original analytic images from tools/create_effects.py. Damage, telegraph timing, targeting, hit areas and parry rules are unchanged. Readable gameplay warning colors are preserved.

Rewrote README with all current features, controls, campaign/environment/roster mapping, training and equipment, soundtrack, build/test/package instructions, fresh screenshots, and verified version history. Published GitHub packages currently end at v1.2.0; 1.3–1.7 are clearly labeled source updates, with workflow artifacts or a local build used for current features. No new tagged release has been published. Updated the player guide, asset/audio documentation and notices; version is 1.7.0. Distribution/native staging also includes the images referenced by README so the bundled document stays readable offline.

Validation: 49 unit tests pass, including new deterministic-layout/clear-sanctuary/model-reference checks and PCM tests for stereo content, silence, clipping, duration and loop seams. Full standard/high-DPI desktop regressions pass across combat, saves, difficulty, equipment, aligned training rows, forge and supplies purchases, active music streams, chapter theme selection and mute. Reviewed all four maps, all three workshop tabs, equipment, warned sigils and real detonations. Native macOS packaging reports 1.7.0; its launcher completes a smoke test from /tmp using bundled assets/audio with no asset/shader warnings. Cross-platform CI still provides package checks on the pushed commit.

Remaining roadmap (not unfinished items from this request): terrain collision/navigation around physical cover; animation blending and full crossbow reload; larger loot/crafting/storage systems; longer balance and audio-mix playtests on different hardware. No requested feature work remains. If a future session is interrupted, record its exact pending checks and changes here before stopping.

## v1.6.0: Tactical squads, four biomes, and visible equipment

Completed the enemy, campaign, and progression expansion. Enemies use target commitment and detection hysteresis, smooth acceleration and bounded turning, local ally avoidance, scout flanking, melee recovery backsteps, and facing checks before committing attacks. Ranged squads maintain distance, strafe, avoid allied firing lanes, and lead a moving hero. Shamans/frost hexers channel visible, interruptible heals on wounded nearby allies. Difficulty limits concurrent ordinary melee windups against the hero; bosses and ranged attacks remain independent threats. Human rigs now include idle/walk/strafe clips, and skeleton squads use their own compatible animation set.

Easy, Normal, Hard, and Extreme are selectable before a campaign and persist in both settings and saves. They scale HP, damage, speed, raid size, arrival/attack cadence, and assault pressure. Defense raids cap at 36; boss-only chapters stay single bosses. Existing saves default to Normal. Difficulty stays fixed for the active campaign.

Every chapter changes its environment and matching roster: Ember Meadow with human raiders; Ash Cemetery with the skeletal Warden; Frozen Pass with skeleton minions, rogues, warriors, archers, and hexers; Hollow Necropolis with the King. Downloaded and integrated 22 Kenney Graveyard Kit 5.0 meshes with the shared texture atlas and original CC0 license. Added Nature Kit bridge/cliff meshes and KayKit skeleton Minion/Rogue rigs. Crypts, graves, walls, gates, moon, snow, different ground layouts, sky palettes, lighting and fog distinguish the stages. Tall necropolis crypts are placed outside the playable boundary. The pass uses north/south spawn routes. Source/archive checksums and import scripts are retained.

Rebuilt the workshop into Training / Equipment Forge / Supplies tabs. Seven capped tracks independently improve health, damage, stamina capacity, stamina recovery, movement, defense, and sanctuary integrity. Cards show price and actual before/after effects. The forge sells three weapon tiers and three complete armor sets; boss drops still unlock weapons. Legacy combined character upgrades remain intact when loaded. Saves include all new stats, equipment and stamina capacity without changing format 2.

The new inventory has a rotatable 3D paper doll, equipment slots, an item grid, locks, equipped markers, stat comparisons, click/drag equipping, right-click removal, and potions. Four armor slots follow animated bones in gameplay and preview; equipped helmets suppress the original Knight helmet/Mage hat. Original generated Iron/Runic/Dawnsteel meshes change appearance and defense; boots also improve speed. Warriors visibly switch sword/longsword/waraxe/Dawnblade; Archers progress to a crossbow with its own aiming/shooting clips and right-hand socket; Mages use staff/wand/skull staff/Dawnstaff. The preview has independent studio lighting and cannot advance combat.

Validation: 46 unit tests cover legacy behavior plus difficulty scaling and boss counts, target commitment, ranged retreat/strafe, flanking, bounds, firing lanes, assault limits, healer selection/interruption, independent stat caps, transactional purchases, equipment bonuses, expanded saves, and old-property migration. Desktop regression additionally exercises difficulty buttons, training cards, forge purchases, armor click/drag/remove, upgraded stamina save/load, crossbow impact, and a real interrupted healing channel. Fresh environment, workshop, forge, and all-class equipment captures are saved in docs/images. On this Apple Silicon macOS host, both high-DPI and standard-DPI desktop suites pass. Native packaging succeeds with bundle version 1.6.0; the packaged launcher completes its smoke test from /tmp using bundled assets and runtime, without asset/shader warnings. CI on the pushed commit remains the cross-platform packaging check.

Next steps: collision-aware scenery and navigation around physical cover; animation blending and a full crossbow reload sequence; longer campaign balance sessions on all four difficulties; more loot item types beyond the fixed weapon/armor catalog. The inventory is a fixed equipment catalog with potion storage, not arbitrary crafting/chest storage. Scenery remains decorative, and AI uses local steering rather than a navigation mesh. Cross-platform runtime behavior requires CI and platform-specific playtesting.


## v1.5.0: Combat discipline, skeletal bosses, and modern lighting

Added a persistent 100-point stamina meter. Light attacks cost 14, heavies 32, lunges 24, dodges 22, and parries 12; sprinting drains 16/second. Stamina regenerates after a brief spending delay, and exhausted sprinting waits for 25 stamina before re-enabling. Older saves default to full stamina. New F parry and R lunge bindings migrate around existing custom keys rather than resetting them.

Light chains require a late input (last .14 seconds of recovery or .23 seconds afterwards), with a 12% chain bonus. Early mashing loses the chain. Hold/release Q to charge; release at .75–1.05 seconds for a 1.6x heavy bonus, with reduced reward for overholding. A directional .22-second parry deflects melee/projectile attacks, restores stamina, staggers melee attackers, and grants one 1.75x riposte for 1.4 seconds. Missing the window commits the hero to recovery. R lunges forward and strikes/shoots; dodges can cancel late recovery at stamina cost, reset the chain, and grant invulnerability only during .03–.24 seconds of the .34-second animation. No new move bypasses the world boundary.

Downloaded KayKit Skeletons from the creator's pinned CC0 repository. Ash Warden now uses the horned armored warrior and an axe at 2.1 scale. Hollow King uses a 2.45-scale skeletal caster with its hat removed and a floating crown/eye effects. Both use their own animation sets. Bosses lock targets during telegraphs, rotate through cleaves, warned ground slams plus expanding shockwaves, committed charges, and delayed sigil fields. Gold cleaves/charges can be parried; red slams, shockwaves, and purple sigils cannot. At half health, windups shorten and sigil patterns expand from three to five zones. Charges use swept collision and stop at world bounds. Parried rushes stop and open a punish window. Transient boss windups, hazards, and parry/riposte states reset on load; health and stamina persist.

Replaced simple diffuse shading with GGX/Schlick material lighting, hemisphere ambient light, fire/sanctuary/boss point illumination, wind-deformed foliage, height/distance fog, and filtered directional cast shadows. RGB-packed shadow depth has opaque alpha to avoid blending corruption; the material BRDF slot carries the atlas. Scene-only FXAA, bloom, ACES tone mapping, gamma correction, and mild vignette leave the native-DPI HUD sharp. Framebuffers resize with the window; depth is explicitly cleared each pass. High/Balanced graphics settings control shadow/scene resolution and bloom. A translucent hero silhouette remains visible when a towering boss stands between the camera and hero. This is a stylized forward renderer, with RGBA8 intermediate buffers, rather than a photorealistic HDR/PBR asset pipeline.

Validation: 39 unit tests cover stamina limits/recovery, parry/riposte timing, boss patterns/enrage, swept hazards/charges, stamina save migration, and binding migration as well as the earlier campaign/combat tests. Expanded desktop checks exercise actual parries (including wrong facing), empty-stamina attacks, perfect charges, timed chains, lunges, unblockable slams, dodge/parry rush counters, warned sigil detonations, five-zone enraged patterns, graphics-quality switching, and shadow-atlas coverage. Validated both DPI modes, High/Balanced quality switching, native macOS packaging, shadow-atlas coverage, and a packaged-app launch outside the repo. Visual-only review mode: `./gradlew inputSmokeTest --args="--standard-dpi --visual-only"`.

Next: animation blending, dedicated boss defeat animations, optional aim controls, collision-aware chapter layouts, and longer balance playtests. The boss types currently share a move framework with different model/weapon presentation and pattern order. Cross-platform native builds still require CI.

## v1.4.0: The Last Ember campaign and living valley

Removed Classic Woodland selection and its rendering path. Older arena IDs remain accepted in saves, but every live scene now uses the imported Highland environment.

The Last Ember is a finite four-chapter campaign: The Last Ember (waves 1–4), The Ash Warden (a single boss at wave 5), Road to Dawn (waves 6–9), and The Hollow King (a single final boss at wave 10). The director stops at chapter boundaries until the player continues; it never schedules wave 11. The HUD shows the chapter, wave within it, and waves still to clear, including the active wave. Boss chapters are marked BOSS ONLY. Briefings, a Chronicle accessible from the title/pause menus, chapter aftermaths, and a victory ending tell the valley's story. Inventory remains accessible between chapters. Chapter completions save automatically. Wave/queue/total values distinguish an unacknowledged chapter ending from the next chapter's preparation, without changing save format 2. Old active waves can finish their saved queues; older post-wave-10 runs conclude with the campaign ending when cleared.

Healing now has a pulsing six-unit ring matching the actual healing radius, plus ground crosses and rising motes. Imported terrain and forest extend far outside the bounded playable clearing; low border stones mark the edge. Added a gradient sky, drifting clouds, flying birds, fire/embers, haze, and more chapter-dependent ruin props. Background scenery is visual only. Player movement remains clamped to ±22, including dodges. Lowered the default camera and widened its view to expose the horizon.

Added configurable Shift sprint and Ctrl directional dodge (1.5-second cooldown, .34-second evasion window), smooth acceleration/deceleration and turning, normal/running/bow-running/directional-dodge clips, and bow draw-to-release transitions. Warrior combos now use horizontal, diagonal, and chopping clips; heavy attacks use the jumping chop, with faster contact poses and a small committed step. Facing is normalized so long play sessions remain saveable. Dodge prevents player damage during its animation; it does not protect the sanctuary. Added vector pictograms to relevant menu buttons, workshop cards, and the combat hotbar while retaining text labels. Settings now accommodate the two new bindings; older settings default them automatically.

Validation: 30 passing unit tests, including coverage for route boundaries, remaining counts, single-boss scheduling/resume, no wave 11, and chapter save round-trips. Expanded visible desktop smoke coverage checks sprint speed, dodge cooldown/bounds, distinct warrior clips, chapter continuation/save/load, boss-only encounters, the final ending, and Chronicle navigation, alongside all earlier combat/persistence/UI checks. Validated high and standard DPI, native macOS packaging, and a short packaged-app launch outside the repo. The automation harness injects combat clicks and disables camera sensitivity so desktop pointer events cannot alter its scripted combat/movement cases; menu clicks still use native automation events. Fresh gameplay, horizon, story, boss, and victory captures are retained in docs/images.

Next steps: chapter-specific combat layouts and collision, more distinct boss move sets, animation blending, and higher-wave balance playtests. The current chapters reuse the sanctuary layout with additional ruin dressing. Cross-platform packaging still requires CI.

## v1.3.0: Highland Sanctuary and interface rebuild

Rebuilt the new level so every scenery mesh is imported from Kenney Nature Kit: tiled grass and branching dirt tracks, a raised stone sanctuary/ring, expedition tents, campfire stones, fences, ruins, vegetation, and a layered tree/rock border. Its render path returns before any classic scenery is drawn. No classic hut, crates, hand-built paths/fences/lanterns, generated ground plane, or haven cylinder is drawn in Highland Sanctuary. Characters, weapons, combat telegraphs, and other gameplay effects remain separate from environment scenery.

New games default to Highland Sanctuary at wave 1. The main menu has a Sanctuary Expedition action, and the name screen offers Sanctuary or Classic Woodland. Arena selection is explicitly saved; older saves without this optional property infer the stage from their wave as before. Woodland still transitions to Sanctuary from wave 6. No save format version bump is needed for the optional compatible property.

Added directional sunlight with cool ambient light and distance haze to imported scenery and characters. Rebuilt the HUD around a central raid header, compact health/resource cards, a bottom combat hotbar, radar, and sanctuary integrity. All menus use a bundled SIL Open Font License Rajdhani font, rounded cards and restyled buttons. Lowered the camera angle for a clearer view of scenery height. Version is 1.3.0.

Validation covers unit tests (including explicit arena round-trip, old-save inference, and rejection of unknown levels), visible high/standard DPI desktop smoke tests, new-game access to Sanctuary, restored stage selection, class combat, boss loot, and macOS packaging. Fresh captures show the actual gameplay view and an overview; screenshots are in docs/images.

Next steps: add collision-aware terrain layouts, light/shadow quality settings, bow draw/hold/release transitions, and higher-wave balance playtests. Imported scenery remains decorative and does not provide cover. Other desktop packages must be verified by CI; local tests only certify this macOS host.

## Environment pack and bow correction

Downloaded Kenney Nature Kit (2.1), CC0, from the creator’s OpenGameArt listing. Integrated 13 selected unmodified OBJ/MTLs: varied pines/oaks, boulders, shrubs, grass, flowers, mushrooms, tents, logs, damaged columns, and obelisks. Woodland gains a layered forest and small camp; Highlands gains rocky borders and imported ruin props. Models load once, validate on startup, and unload at shutdown. Runtime material colors adapt foliage to the forest palette; source OBJ/MTL files stay unchanged. Runtime packaging already includes `outpost/**`, including the license and source/checksum manifest. Reimport with `tools/import_nature.py` and the downloaded archive.

Corrected bow orientation with a 180-degree rotation around its local long axis before applying the animated left-hand slot transform. The shared attachment path fixes player and enemy bows. Archer now uses the bundled bow idle animation rather than the melee idle pose.

Validation: 25 unit tests, the standard-DPI desktop regression (including side-view bow and environment overview captures), and macOS native packaging.

Next: tune terrain and lighting, add bow draw/hold transitions, and build a distinct highland route layout. Environment props remain decorative; they do not provide cover or block movement/projectiles.

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
