# Java source index

All main-source Java files, including retained examples. See the [Warden's Journey notes](WARDENS_JOURNEY.md) for focus, travel and map rules.

| Source | Responsibility / first code comment |
| --- | --- |
| [AppData](../../src/main/java/AppData.java) | User files stay outside the install directory |
| [Arena](../../src/main/java/Arena.java) | Legacy save identifiers |
| [ArenaNavigation](../../src/main/java/ArenaNavigation.java) | Conservative trunk/stone footprints leave foliage, bridges and open tents traversable. |
| [BossHazard](../../src/main/java/BossHazard.java) | Warned ground magic and swept expanding rings |
| [BuildInfo](../../src/main/java/BuildInfo.java) | See implementation and architecture/change guides for its callers. |
| [Campaign](../../src/main/java/Campaign.java) | The finite story route |
| [ChapterFeats](../../src/main/java/ChapterFeats.java) | Optional chapter goals |
| [ChapterLook](../../src/main/java/ChapterLook.java) | A chapter changes geometry, ground palette, atmosphere and enemy roster together. |
| [ChapterSelectScreen](../../src/main/java/ChapterSelectScreen.java) | Zone selection is a view; Game owns starting and saving the selected expedition. |
| [Combat](../../src/main/java/Combat.java) | Deterministic combat geometry and timing, independent of rendering/input. |
| [CombatFeedback](../../src/main/java/CombatFeedback.java) | Short-lived world anchored damage/reward labels |
| [CombatMastery](../../src/main/java/CombatMastery.java) | Short-lived class rewards encourage different decisions without adding key bindings. |
| [CombatVfx](../../src/main/java/CombatVfx.java) | Soft textured particles, weapon ribbons and authored spell decals |
| [Controls](../../src/main/java/Config/Controls.java) | See implementation and architecture/change guides for its callers. |
| [CoopBalance](../../src/main/java/CoopBalance.java) | Tuned for two active heroes: more bodies, tougher bosses, unchanged readable attack timing. |
| [CoopClient](../../src/main/java/CoopClient.java) | UI submits intentions without blocking on network I/O |
| [CoopGame](../../src/main/java/CoopGame.java) | Complete host/join front end and snapshot presentation for the two-player campaign. |
| [CoopInput](../../src/main/java/CoopInput.java) | Inputs are intentions only: the host owns position, health, damage and rewards. |
| [CoopProtocol](../../src/main/java/CoopProtocol.java) | Bounded binary messages; deliberately avoids Java object deserialization over the network. |
| [CoopServer](../../src/main/java/CoopServer.java) | Two-player private host |
| [CoopState](../../src/main/java/CoopState.java) | Immutable host snapshots; networking and rendering never mutate the simulation. |
| [CoopWorld](../../src/main/java/CoopWorld.java) | Fixed-step, host-authoritative cooperative campaign |
| [CorpsePresentation](../../src/main/java/CorpsePresentation.java) | Fallen characters stay on the ground, then fade in place after the fight. |
| [DayNightCycle](../../src/main/java/DayNightCycle.java) | Eight-minute solar cycle |
| [EnemyFocus](../../src/main/java/EnemyFocus.java) | Client-side focus selection |
| [AnimationFrames](../../src/main/java/Entities/AnimationFrames.java) | Frame indices passed to raylib must stay inside the clip's keyframe array. |
| [ArmorSlot](../../src/main/java/Entities/ArmorSlot.java) | See implementation and architecture/change guides for its callers. |
| [AttackMotion](../../src/main/java/Entities/AttackMotion.java) | The visible contact frame follows the damage clock, independent of clip length. |
| [BossMove](../../src/main/java/Entities/BossMove.java) | See implementation and architecture/change guides for its callers. |
| [CombatDiscipline](../../src/main/java/Entities/CombatDiscipline.java) | Stamina and defensive windows, independent of graphics and desktop input. |
| [Difficulty](../../src/main/java/Entities/Difficulty.java) | See implementation and architecture/change guides for its callers. |
| [Enemy](../../src/main/java/Entities/Enemy.java) | Snapshot replicas retain the host's windup when a boss crosses the enrage threshold. |
| [Entity](../../src/main/java/Entities/Entity.java) | See implementation and architecture/change guides for its callers. |
| [Player](../../src/main/java/Entities/Player.java) | Present a host snapshot without reading the local keyboard or running combat twice. |
| [PlayerAnimation](../../src/main/java/Entities/PlayerAnimation.java) | See implementation and architecture/change guides for its callers. |
| [SkinBounds](../../src/main/java/Entities/SkinBounds.java) | Bounds of deformed vertices, rather than the undeformed bind-pose box. |
| [SquadTactics](../../src/main/java/Entities/SquadTactics.java) | Local perception and squad rules; no renderer or desktop input required. |
| [EnvironmentLayout](../../src/main/java/EnvironmentLayout.java) | Authored landmarks and seeded vegetation clusters, shared by both render passes. |
| [EquipmentScreen](../../src/main/java/EquipmentScreen.java) | Select/compare equipment, then equip explicitly, double-click, or drag to a labeled slot. |
| [Expedition](../../src/main/java/Expedition.java) | Persistent, optional build choices earned by clearing waves and field objectives. |
| [FieldEvent](../../src/main/java/FieldEvent.java) | Optional timed encounters |
| [Game](../../src/main/java/Game.java) | Coordinates the single-player session: input, simulation, screens and persistence |
| [GameAudio](../../src/main/java/GameAudio.java) | Streamed original chapter scores with smooth scene/intensity crossfades. |
| [GameState](../../src/main/java/GameState.java) | See implementation and architecture/change guides for its callers. |
| [HeroClass](../../src/main/java/HeroClass.java) | Starting equipment and combat style |
| [HeroPortrait](../../src/main/java/HeroPortrait.java) | An independent animated character stage; selecting a class never changes the active hero. |
| [HeroShot](../../src/main/java/HeroShot.java) | Player projectiles keep their own hit set so piercing bolts hit each enemy once. |
| [HudLayout](../../src/main/java/HudLayout.java) | Shared drawing and hit targets in window pixels, including compact windows. |
| [HudScreen](../../src/main/java/HudScreen.java) | Outpost's ember vitals and six-slot action belt |
| [Main](../../src/main/java/Main.java) | See implementation and architecture/change guides for its callers. |
| [MainMenuScreen](../../src/main/java/MainMenuScreen.java) | Draws the title menu and reports intent |
| [Menu](../../src/main/java/Menu.java) | Small vector pictograms remain crisp at both display scales. |
| [ModernRenderer](../../src/main/java/ModernRenderer.java) | Shadow atlas and scene-only post-processing |
| [PlayerSaves](../../src/main/java/PlayerSaves.java) | Named campaigns retain independent saves and chapter checkpoints. |
| [Progression](../../src/main/java/Progression.java) | Rules and economy kept independent of rendering so they can be tested. |
| [Projectile](../../src/main/java/Projectile.java) | See implementation and architecture/change guides for its callers. |
| [RangedAim](../../src/main/java/RangedAim.java) | Small launch correction, not homing: only visible targets already close to the intended heading. |
| [RecoveryWisps](../../src/main/java/RecoveryWisps.java) | Small battlefield recoveries; saved with the run and frozen whenever gameplay pauses. |
| [RoadLayout](../../src/main/java/RoadLayout.java) | A continuous, gently curved ribbon rather than rotated, overlapping path tiles. |
| [SanctuaryWard](../../src/main/java/SanctuaryWard.java) | A supporting defense: threats nearest the sanctuary take priority; every foe needs a finishing blow. |
| [SaveGame](../../src/main/java/SaveGame.java) | Versioned, validated data only: never Java object deserialization. |
| [CameraController](../../src/main/java/Scene/CameraController.java) | See implementation and architecture/change guides for its callers. |
| [Scene](../../src/main/java/Scene/Scene.java) | See implementation and architecture/change guides for its callers. |
| [SceneManager](../../src/main/java/Scene/SceneManager.java) | See implementation and architecture/change guides for its callers. |
| [SceneryVisibility](../../src/main/java/SceneryVisibility.java) | Conservative top-down occlusion test for decorative tree trunks and canopies. |
| [Settings](../../src/main/java/Settings.java) | See implementation and architecture/change guides for its callers. |
| [TargetFrameScreen](../../src/main/java/TargetFrameScreen.java) | A focused target has a readable health frame and cast warning, independent of world labels. |
| [Tutorial](../../src/main/java/Tutorial.java) | Practice advances through demonstrated actions, never elapsed reading time. |
| [WaveDirector](../../src/main/java/WaveDirector.java) | One announced arrival at a time; clearing the field never skips queued raiders. |
| [WaveFormation](../../src/main/java/WaveFormation.java) | Distinct squad compositions keep each siege wave readable and different. |
| [WeaponTrail](../../src/main/java/WeaponTrail.java) | Fixed-capacity history of the visible blade |
| [WorkshopScreen](../../src/main/java/WorkshopScreen.java) | Shared layout coordinates keep mouse targets aligned with the drawn rows. |
| [WorldAssets](../../src/main/java/WorldAssets.java) | Shared GPU assets: load once, pose each raider immediately before drawing. |
| [WorldLandmarks](../../src/main/java/WorldLandmarks.java) | Chapter-local names and proximity bounds; no rewards or combat side effects |
| [AssetPaths](../../src/main/java/assets/AssetPaths.java) | Locate external glTF/texture bundles independently of the launcher's working directory. |
| [AudioRawStream](../../src/main/java/examples/AudioRawStream.java) | Retained Raylib experiment; not used by the game entry point. |
| [CubicMap](../../src/main/java/examples/CubicMap.java) | raylib [models] example - Cubicmap loading and drawing This example has been created using raylib 1.8 (www.raylib.com) raylib is licensed under an unmodified zlib/libpng license (View raylib.h for details) Copyright (c) 2015 Ramon Santamaria (@raysan5) |
| [HeightMap](../../src/main/java/examples/HeightMap.java) | Retained Raylib experiment; not used by the game entry point. |
| [Physac](../../src/main/java/examples/Physac.java) | Retained Raylib experiment; not used by the game entry point. |
| [Light](../../src/main/java/examples/RLights/Light.java) | Retained Raylib experiment; not used by the game entry point. |
| [Raygui](../../src/main/java/examples/Raygui.java) | Retained Raylib experiment; not used by the game entry point. |
| [ShadersBasicLighting](../../src/main/java/examples/ShadersBasicLighting.java) | Retained Raylib experiment; not used by the game entry point. |
| [TexturesBunnymark](../../src/main/java/examples/TexturesBunnymark.java) | Retained Raylib experiment; not used by the game entry point. |
| [TexturesBunnymarkMoreJavaLike](../../src/main/java/examples/TexturesBunnymarkMoreJavaLike.java) | Retained Raylib experiment; not used by the game entry point. |
| [shapes](../../src/main/java/examples/shapes.java) | Retained Raylib experiment; not used by the game entry point. |
