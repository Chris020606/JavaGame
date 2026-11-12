# Warden's Journey: design and implementation

This pass moves the game toward a readable small-party RPG: a stable enemy focus, clear enemy casts, persistent zone access, and recognizable landmarks. It builds on the maintained v4.2.1 code. It does not turn the four-chapter defense campaign into an MMO or import the archived balance experiment.

## Enemy focus and cast frame

`EnemyFocus` is a pure Java selection rule. The Focus action defaults to **T** and can be rebound. Cycling considers living enemies within 28 metres that have line of sight, sorted by distance. A selected actor is cleared when it dies or disappears from the current actor list.

`TargetFrameScreen` displays name, current/max health, distance and cast progress. Enemy damage resolves when `attackTime` reaches .25 seconds, so the displayed impact countdown subtracts .25 instead of counting to animation completion. Boss casts that cannot be parried use red; other casts use gold. Healing wards have their own label.

The same view/selection classes are used by solo and co-op. In co-op the candidates are snapshot presentation actors; focus is client-side and adds no protocol fields or authority. Selecting a target does not rotate shots, grant homing, ignore cover or extend range. Existing aiming and hit rules remain authoritative.

The compact HUD below 1000 pixels omits the extra target panel to preserve space. The selection still works. This is a focus feature, not a clickable enemy portrait or a complete tab-target spell system.

## Solo chapter travel

Pause and choose **World Map / Chapters**, or open it from a chapter/final-victory result screen. Closing it returns to the screen it came from. The connected route shows reached/unlocked chapters. New profiles begin with chapter 1. Completing a chapter unlocks the next; old saves infer reached and completed chapters when loaded.

`Progression.unlockedChapter` stores the highest unlocked index (0–3). It is optional in save version 2 and validated on restore. Visiting an earlier chapter does not lower this value.

`ChapterSelectScreen` only returns a selection. `Game.travelToChapter` validates access and restarts the chosen chapter with current gear, currency, health and sanctuary condition. It clears enemies, corpses, player/hostile projectiles, boss hazards, focus, recovery wisps and the optional field encounter. It resets player combat and writes a new chapter checkpoint and active save. The screen explicitly explains that this replaces the current encounter/checkpoint.

Co-op retains its shared linear campaign and existing checkpoint/reconnect flow. Solo travel is not silently applied to a remote party.

## Map authoring

- Meadow: unequal landmarks replace mirrored shrine arrangements. The western ruined garden and eastern wayfarer stop give direction cues; the central sanctuary clearing stays open.
- Cemetery: low altar/coffin/fire-basket details frame the boss space without filling its dodge area.
- Highlands: road control points bend more noticeably through the pass; distant ruin markers identify the bends. Wave spawning and combat balance are unchanged.
- Campfires: ember positions are derived from authored campfire props, avoiding effects left at old coordinates after moving a campsite.
- Tree visibility: `SceneryVisibility.treeOpacity` fades trees only when their ground projection lies between the camera and hero and intersects a conservative canopy corridor. The shadow pass stays opaque. This improves visibility without moving collision footprints or changing projectile cover.

The environment is still an authored arena per chapter. These are landmark/road/readability improvements, not a new open-world terrain system.

## Comments and maintenance

Comments are placed around behavior that is easy to break: native actor references, cast-impact timing, optional save migration, travel resets, camera occlusion and separate visible/shadow passes. Avoid comments that merely repeat a Java assignment.

Relevant files:

- `EnemyFocus.java`, `TargetFrameScreen.java`
- `ChapterSelectScreen.java`, `Game.travelToChapter`, `Progression`
- `EnvironmentLayout.java`, `SceneryVisibility.java`, `WorldAssets.java`
- `Config/Controls.java`, `CoopGame.java`

Checks: `AdventureSystemsTest`, `inputSmokeTest --args=--adventure-review`, four-chapter art review, full solo input regression, and native co-op scenarios. Existing protocol fixtures ensure no wire-layout drift.

## Validation record

115 unit tests pass, including focus selection/visibility, old custom key bindings, chapter unlock save migration and scenery fade boundaries. The targeted native adventure review verifies the real focus key and cast frame, pause-map button, encounter reset, save/load, legacy reached-zone inference, locked-zone guard and return from the map to victory. The full solo regression passed in4m20s; all three native co-op pairings passed in42s with focus selection assertions on both clients. Four-chapter art captures and the target/map captures were visually reviewed. Final packaging and cross-platform release status are recorded by the release workflow.

## v4.3.1: focus training and local map identity

Training now has nine steps. After movement, FOCUS requires a real visible partner selection with the configured focus key (T by default). `Tutorial.focus()` only advances that step. `Game.prepareTutorial` keeps that actor for the following strike lesson, so selection is not lost immediately. Practice target frames hide the artificial health and identify the partner as safe practice. The Archer dodge bonus is named Precision to distinguish it from target focus; its numbers are unchanged.

`WorldLandmarks` contains small chapter-local named areas. A location caption appears near camps/ruins outside tutorials and boss fights. These names do not grant rewards or introduce save keys. Keep their coordinates aligned with authored scenery if a campsite moves.

`EnvironmentLayout.addTravelDetails` uses an independent random seed, keeping existing cover and tree placement stable. Soft verge vegetation stays outside the sanctuary's central clearing. Meadow lily pads are placed on known water tiles below ground height. These props do not add navigation obstacles or change projectile cover.

Tests cover rebinding the focus lesson, demonstrating focus before striking, landmark bounds and lily placement. The native tutorial scenario presses the real focus key for every class. Map-detail and four-chapter art reviews provide captures for visual checking.

![The target-focus lesson](../images/player-focus-training.png)

Validation for v4.3.1: 117 unit tests; full solo input regression (4m 22s); focused all-class tutorial review (40s); co-op smoke (14s); four-chapter art and map-detail captures; final format/package checks passed.

## Working-tree XP repair (2026-11-08)

Enemy-death feedback advertised XP without awarding it. `Progression.rewardEnemy` now grants 25 XP for a regular enemy or 300 for a boss; `rewardElite` adds the remaining 60 XP for an 85-XP elite total. The existing chapter-clear award remains separate. Four regular defeats reach level 2, grant one attribute point, restore health and apply the existing level stats. Excess XP carries forward; level 1000 caps further awards within save bounds.

`usesLevelPoint` and `canPurchase` now supply the workshop's currency and availability rules as well as the purchase mutation. Resilience costs crystals; other attributes prefer a point when available, then crystals. Buttons show the currency actually charged. The HUD draws XP values once and uses the configured Workshop binding for its point hint.

Validation: 125 unit tests pass, including actual reward entry points, elite/boss totals, point spending, disk save/load and level-cap checks. `inputSmokeTest --args=--xp-review` exercises four deaths through Game's update, clicks the Power training button, and reloads the save. Captures are `build/xp-level-up.png` and `build/xp-training.png`. This is a solo progression fix; co-op uses its existing separate credits system.

The entry-screen pass removes gameplay help from the title menu, reveals the world background, separates character creation and preview panels, and marks class/difficulty selections. `--entry-review` captures both 1280x800 and 1000x680 layouts. Larger meadow ridge/ruin pieces were added outside actor bounds. Other working-tree game changes are retained.

## Story and returning citizens (2026-11-11)

New characters see OpeningStory before control begins: criminal betrayal, Aethelgard as refuge, Beacon teleport/rescue and defense, then mentor and deployment. Enter or Space advances the four panels. The opening pauses game updates. It runs for new characters, not existing saves.

Practice spawning is restricted to PLAYING. finishTutorial clears practice actors, target selection, shots, hazards, corpses and pending combat before restoring any saved practice-return encounter. The Citadel tutorial no longer creates a practice actor when movement advances to Focus.

CitizenRoutine provides home-centred walking and resting with collision checks; approaching players and active conversations pause movement. CitadelHub uses the same moving coordinates for rendering and interaction. Rescue ownership still uses the existing persisted rescuedSurvivors count; transient walking positions reset near each citizen's home when the application restarts. The existing four named survivors appear as they are rescued.

Starter gifts use RECEIVE plus the actual tier-zero weapon name. Existing class-specific introductory greetings explain the gift before acceptance; forging remains available after receipt.

## Discoverability and rescue expansion (2026-11-11)

Removed the outline circles under street lamps (DrawCircle3D draws rings rather than filled illumination). Portal and nearby-NPC interaction markers remain meaningful.

The city now shows ProgressionSummary: XP, current max health/attack power and adjacent next-level bars. Progression owns preview calculations; tests compare them to actual level-up results for every class, armed/unarmed. The dossier also states exact next-level gains. City equipment guidance identifies the matching mentor and world direction/distance, with crystal/level requirements. Ember Blessing availability now has a larger central card stating its benefit and the Beacon/clear-field/interact requirements.

FieldEvent offers six survivor escorts (waves 2, 4, 6, 7, 8, 9), preserving supplies on wave 1 and the waystone encounter on wave 3. Boss-only chapters have no escort. Added Mira the Weaver and Owen the Baker as the fifth/sixth returned citizens using existing models and CitizenRoutine; their greetings and stories describe rebuilding the city. Existing rescue count and event saves remain compatible.

## Inventory growth, minimap and progression balance

Removed the persistent city stat/crafting panels. InventoryGrowth now shows adjacent current/next-level health and power bars under the character preview. The opening introduces returning to the class mentor to forge equipment once per new character; mentor gift conversations retain their crafting guidance. CityMinimap is north-up with player, matching mentor, other service NPCs and expedition portal markers.

Level growth preserves the first two increments (+10 attack/+25 HP), then becomes +3 attack/+10 HP per level. Next-level previews derive from those same curves. Unarmed growth stays +2 attack. Solo non-boss enemies gain eight extra base HP per wave beyond wave four; co-op party-scaled enemies and bosses retain existing health tuning. Tier-two weapon/armor forging costs 400/380 crystals; tier-three costs 850/800. Existing level requirements and ownership remain intact. Old saves accept the previous health maximum then clamp to the revised cap.

Balance regression verifies an Easy wave-nine regular survives a level-12 Legendary basic hit without exceeding three base hits. This does not prohibit one-shot critical/heavy attacks or maximum attribute builds. Full campaign co-op tests remain part of validation; subjective combat feel still benefits from player feedback.

## Merchant stock and quiet combat HUD

CityMinimap now follows the combat radar frame: 160px panel, 59px circular radar, cardinal labels, muted crosshair, teal gate/gold class mentor/white player, top-right placement. City coordinates are scaled to fit the city boundary inside the circle.

BlessingReminder displays for four seconds after entering a safe interval with unspent embers. Any enemies, queued arrivals, tutorial, inventory or workshop suppress it. The card appears at the right side instead of over the player's combat view. It does not consume a blessing or change claim requirements.

Torvald's merchant button opens the supplies tab (legacy selectTab mapping requires index 2). Merchant stock adds Heartwood Charm (+5 max HP/rank), Traveler's Flask (+5 stamina/mana capacity/rank) and Restoration Incense (+1 resource regeneration/s/rank). Each caps at five ranks and costs 180 + 140 per existing rank. Ranks persist as optional caravan.0..2 keys with zero defaults. Existing health migration includes charm capacity. Purchases immediately apply and save through the workshop coordinator. Potions and Beacon repair remain repeatable purchases.

MerchantReminderTest checks safe-window expiry and combat suppression, prices, cap, effects and persistence. Native inspection includes merchant-stock.png.

## Vendor correction

Torvald was interactable but absent from drawNpcs, and his old position (-5.5,15.5) lay inside the wagon footprint. He is now drawn with the matching Barbarian rig at (-2.5,15.5), beside the open central market avenue. Proximity and minimap use CitadelNpc's same position. The inspection suite captures torvald-market.png.

WorkshopScreen stock is now determined by activeMentor: class mentor means equipment forge; null means Torvald's provisions. Removed switchable tabs so mentor visits cannot expose caravan upgrades and merchant visits cannot expose equipment forging. VendorStockTest checks role isolation and placement. Existing tutorial NPC proximity expectations follow the corrected position.

## Individual armor ownership

The class forge offers four separately priced piece buttons per tier. Head/boots cost 20%, chest 40%, legs 30% of the former bundle price (rounded); buying a slot grants only that slot/tier and never auto-equips it. Inventory ownership and Best Gear use per-slot masks. Progression stores optional ownedArmor.HEAD/CHEST/LEGS/BOOTS bitmasks; saves without these inherit their old tier ownership for every slot. The aggregate ownedArmor mask stays as the union for legacy metadata. Validation rejects equipped pieces that are not owned. The old bundle purchase method is retained for legacy/test callers; the game UI uses only slot purchases.

The empty boots pictogram is now a matching pair with cuff, shaft, toe and sole. City scenery adds residential tree pockets and a few rural/workyard props outside the central avenue. ArmorPiecesTest covers isolated ownership, duplicate purchase prevention, equipping, persistence and legacy set migration. Inspection includes individual-armor-forge.png.

## Forge clarity and city service discovery

ForgeAdvice supplies armor hover explanations: wrong class mentor, missing starter, required level, exact missing crystals, owned/equip guidance, or defense change against the equipped slot. Purchase confirmations identify the forged piece and tell the player to equip it from inventory. Weapon buttons name their restriction instead of saying BLOCKED. Inventory dossier now correctly calls defense flat damage blocked, matching damagePlayer's subtraction rule.

City radar gives Torvald a distinct diamond marker. Hovering service markers shows NPC name and services without a permanent crafting panel. ForgeAdviceTest covers class, level, currency, gain and ownership explanations.

## Forge visual redesign

The forge now selects one quality tier at a time across three top tabs. A dedicated weapon card shows a large class weapon icon, weapon power and equipped comparison. Four larger armor cards show slot icons, protection values, equipped deltas and individual forge buttons. This replaces the dense three-column/four-small-row layout; vendor stock restrictions, save format and purchase rules are unchanged. Selection affects only presentation. Existing hover reasons and purchase feedback remain in the footer. The native inspection screenshot individual-armor-forge.png shows the new layout at 1280x800.

## City character labels and district arrivals

CityPresentation draws contextual name/role labels for at most two nearby service NPCs or rescued citizens. It uses CitadelHub.npcX/npcZ so labels follow CitizenRoutine movement. Camera-facing and screen-bound checks avoid offscreen labels; names are hidden during dialogue, inventory and shops. Labels are proximity aids rather than a through-wall navigation system.

District arrivals use authored coordinate regions (market, southern gate, castle approach, archery green, forge, sanctum, cathedral and riverside). An 0.8-second dwell precedes a 3.5-second title; crossing back and forth briefly does not retrigger the label. Titles do not affect quests or save state. Update region bounds when city layout changes.

## Rescue ledger

City HUD offers a Rescue Ledger button, opening a paused roster of the six named survivors with safe/missing status from the existing saved rescue count. The overlay returns with Escape or its Return button. This view does not issue rewards, create new save keys or alter progression; citizens are unlocked in the existing roster order. Native inspection exports rescue-ledger.png.

The field rescue card now distinguishes starting an escort from bringing an active survivor to the Beacon. Active escorts show distance to the Beacon instead of repeating the generic interact instruction. Non-rescue field events retain their original instructions and timers.

## Distinct rescued citizen meshes and outskirts activity

Each rescued citizen now uses a separate generated GLB under resources/outpost/citizens: Bram/Barbarian, Kaelen/Ranger, Rowan/Rogue, Theron/Knight, Mira/Mage, Owen/Rogue_Hooded. These are existing licensed KayKit meshes rather than new downloaded artwork. tools/prepare_citizens.py appends only unarmed idle/walking clips using the established bone-name remapper. Textures remain embedded. Each citizen's model and animation allocation is separate; native loading verifies skeleton compatibility before playback and unloads all clip allocations on shutdown. CitadelNpc model paths match the visible generated models.

The previous rescued population reused two enemy meshes (raider and archer). Six individual captures now verify the changed silhouettes in the actual city; the inspection profile rescues all six. Mill-delivery sacks/wheelbarrow and a small lumber workyard add authored outskirts scenes. Nine small animated bird silhouettes circle outside the walls, independent of combat and navigation.

## Equipment comparison and service pictograms

Selected gear now shows side-by-side current/selected power or defense bars with the same scale and numeric delta. Progression.weaponDamage previews hypothetical armed damage without granting ownership, fixing comparisons while unarmed. Inventory and forge use that calculation. EquipmentPreviewTest verifies previews leave weapon ownership untouched and match a subsequently equipped starter.

Nearby city nameplates have role-specific pictograms: merchant bag, mentor class weapon, expedition shield and rescued citizen crown. Work is isolated from enemy behavior files reserved for the parallel model.

## Complete inventory layout redesign

EquipmentScreen uses a 960x640 three-column layout: character preview/equipped slots/growth, filtered collection, and selected-item details. Owned items are shown initially; Include Unowned reveals forge equipment. Categories are All, Weapons, Armor and Supplies. Selection details show a larger item thumbnail, ownership/equipped state, current/selected comparison and contextual Equip/Remove/Drink action. The character stats view occupies the collection/detail area while keeping the character and equipped slots visible.

Mouse orbit/zoom, double-click equip, drag-to-slot, right-click remove and keyboard navigation remain supported. Keyboard grid navigation is four columns. Clicking equipped gear resets category to All so its details remain selected; Delete only removes a selected item if currently equipped. A guard prevents accessing an unprepared thumbnail. Ownership/save formats are unchanged.

CitadelVisualInspection now injects native mouse events to select owned boots, equip through the details action, remove via right-click and consume a potion through Supplies, with assertions on state after each action. It captures owned/unowned collection views and the dossier. Earlier coordinate-based InputSmokeTest inventory scenarios use the old layout and will need coordinate migration before the full historical native regression is used; unit tests and the new inventory interaction checks are the validation for this redesign.

## Character stat explanations and citizen stories

CharacterStatsScreen replaces the dense dossier table with six selectable stat cards: max health, attack power, flat defense, stamina/mana capacity, recovery and movement. Each shows a value and short hint; selected-card text explains the real gameplay rule and available improvement sources. Health/power preview gains still come from Progression. This is presentation only and does not change combat math.

RescueLedgerScreen now lets players select returned citizens to read their existing lore. Missing citizens remain marked as missing; opening the ledger grants no rewards and changes no ownership. Selection resets for a different Progression owner. These UI changes do not touch the enemy behavior files allocated to the other model.

## Market and residential visual pass

CitadelHub adds a public well at (8,21), using the bundled KayKit well, with a matching circular collision footprint. Two overhead pennant strings mark Market Square entrances at z=9 and z=24. Thin wooden supports sit at x=±4.8 with collision; the central avenue stays open. Pennants use red/gold/blue cloth triangles with a small wind motion, drawn on both sides. Residential and sanctum frontages gain authored yellow/purple flowers and bushes, and the well courtyard has a tree and small barrel. Scenery uses existing licensed assets and adds no save fields or combat changes.

## Enemy combat intelligence, inventory test migration, city presentation & lore integration (2026-11-12)

EnemyCombatBehavior adds dedicated navigation intelligence: ranged kiting and sweet-spot positioning (4.5m–7.4m) with boundary deflection away from arena walls and obstacle avoidance, with an Easy difficulty safeguard (3.8m retreat threshold and gentler speed). Melee variants specialize into distinct roles: Scouts flank at 1.4m–3.0m, Raiders distribute approach angles, and Brutes anchor the center with heavy separation resistance. Zero-distance overlap and large frame steps are clamped safely. Unit test coverage increased with 8 dedicated tests in EnemyCombatBehaviorTest.

InputSmokeTest inventory mouse coordinates were migrated from the obsolete 940x600 layout to the new 960x640 design: pack Return button, post-boss weapon unlock/equip, and the full endgame equipment routine covering the 4-column collection grid, drag-and-drop to slots, right-click unequip, Enter-key equipping, category filter switching, and double-click equipping.

CityPresentation was enhanced with HUD exclusion zones (preventing 3D nameplates from colliding with the top-left Guide panel or the top-right City Radar) and point-blank suppression when the player approaches within interaction range. The district arrival title transition was fixed to immediately reset lingering banners upon crossing district boundaries, eliminating confusing stale title displays during entry dwell. CityPresentationTest adds 2 unit tests covering district classification and dwell mechanics.

Civilian silhouettes were improved by modifying tools/prepare_citizens.py to cleanly strip combat helmets, bear pelt hats, quivers, and assassin masks while preserving underlying KayKit rigs and animations, giving Bram, Theron, Mira, Owen, Kaelen, and Rowan genuine civilian appearances.

OpeningStory was reconciled with the campaign's Hollow Frost narrative, bridging the internal criminal betrayal and breached seals with the awakening of the ancient frost legions, the Primordial Beacon's solar ember, and the defense of Aethelgard. All 172 unit tests and the citadelInspection visual test suite pass.

## Boss rewards, shared rescue actors and world corrections

Boss rewards now grant currency/XP only: rewardEnemy(true) no longer unlocks missing weapons. Both immediate and outcome-screen copy state +120 crystals/+300 XP. Existing owned gear is preserved. BossEquipmentTest and EquipmentOwnershipTest now test the intended no-free-equipment policy rather than the former loot behavior.

Game routes survivor field rendering through CitadelHub.drawFieldSurvivor, selecting the next citizen by the existing saved rescue count and using the exact city mesh/animation allocations. Native skeleton compatibility checks still apply. Other field-event kinds keep WorldAssets rendering. Active field citizens walk; waiting citizens idle. Completed rescues no longer leave a lingering field actor.

Inventory preview now has an aspect-matched 960x1040 render target and revised full-body camera. Player.poseForInventory uses armed melee idle when appropriate, rather than posing every non-Archer unarmed while rendering their weapon.

Prop placement subtracts cached model-bound minimum Y (scaled), grounding props without changing terrain/building origins. City trees within 4.8m of road-tile centres are removed. Citizen collision clearance around buildings is widened; blocked starting positions search for nearby clear ground. CitizenRoutineTest simulates all six routines for a minute and verifies every position stays outside the city obstacles. Existing Antigravity EnemyCombatBehavior code is preserved.

Validation: full unit suite, native city/inventory inspection and packaging; field-survivor-shared-model.png exercises the new mission render path. Historical boss-loot assumptions in any other native scenarios must be updated before claiming the entire historical input suite passes.
