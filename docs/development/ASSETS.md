# Assets and native resources

## Runtime lookup

`assets.AssetPaths` first considers `outpost.assets`, then resource directories near the running classes/JAR, then the working directory. A valid root contains `knight.glb`. Runtime files must be copied with their textures, glTF buffers and relative paths intact.

`build.gradle`'s `runtimeAssets` copy specification controls portable packages. A source run can work while a download fails if a newly used asset is missing from that specification. Always run the packaged app after changing asset paths.

## What is authored and what is generated

| Tool | Purpose |
| --- | --- |
| `prepare_assets.py` | Builds hero/enemy GLBs, imports compatible clips, composes ranged grip locomotion, and creates legacy project scenery |
| `import_nature.py` | Imports retained Kenney nature assets into runtime OBJ/MTL output |
| `import_graveyard.py` | Imports cemetery props |
| `import_bosses.py` | Builds retained skeleton boss/roster assets |
| `fit_armor.py` | Produces class-fitted, skinned armor |
| `create_equipment.py` | Produces project equipment geometry/materials |
| `create_landscape.py` | Produces mountain backdrop meshes |
| `create_effects.py` | Produces procedural effect textures |
| `prepare_audio.py` | Generates original synthesized WAV scores and effects |
| `create_icon.py` | Produces app icons |
| `edit_trailer.py` | Edits captured footage; not part of the gameplay runtime |
| `archive_release.py` | Archives a platform's native package and writes its checksum |

Run authoring tools from the repository root. Read the chosen script's inputs before running it: import scripts may require retained source archives or source directories, and generators overwrite their output files. You do not need to regenerate assets to compile or play the tracked game.

For example:

```sh
python3 tools/prepare_assets.py
python3 tools/fit_armor.py
```

Generated assets are committed. Review binary changes separately from Java changes. When replacing an asset, retain source/license provenance in `THIRD_PARTY_NOTICES.md` and `licenses/`.

## Animation pipeline

`Entities.PlayerAnimation` maps gameplay poses to exact clip names. `Player` selects draw/hold/release, aim/shoot/reload, spell and locomotion stages. `Entity` manages fractional frame sampling and crossfades. Armor follows the same animated pose. `WorldAssets` attaches weapons to hand slots.

The Ranger asset has composed `Bow_*`, `Handbow_*` and `Siegebow_*` movement clips. They retain the movement clip's legs/root while using a stable weapon grip for arm/hand channels. Enemy human Archers use their bow movement variants. This is generated in `prepare_assets.py`; editing only the Java name cannot create a missing pose.

When adding a clip or model:

1. Check bone names/order, bind pose and skeleton compatibility.
2. Add the correct clip during asset generation and reference its exact exported name.
3. Check grip/orientation with the actual weapon; a bow held in the left hand and a crossbow held in the right are different attachments.
4. Check fractional animation bounds and matching armor poses with native combat-motion tests.
5. Check idle, charge, release, recovery, movement and death in a real render.

Never call `UpdateModelAnimation` with an incompatible rig or unload a model while another object still uses its native data. A Java exception handler cannot recover from a native segmentation fault.

## Render passes and ownership

Shadow and lit passes must use the same actor/scenery transforms. `WorldAssets` caches animation views, prop transforms and model material bindings; invalidate/rebuild caches when replacing assets. `ModernRenderer` owns framebuffer/shader lifetime. UI textures belong to their screen classes.

Offscreen previews require a live graphics context. They are prepared before drawing inventory. Tests deliberately use visible native windows because hidden rendering has stalled on this machine in the past.

## Audio

`GameAudio` streams eight slots and mixes/crossfades by scene. Meadow, battle and boss slots load CC0 OGG recordings; the remaining chapter/title tracks and effects use generated WAV files. Combat recordings are full arrangements, so the unrelated chapter melody is faded out during them. Enrage changes the boss track's playback pitch slightly.

`prepare_audio.py` generates WAVs and does not replace the downloaded OGG recordings. The exact recording sources and licenses are in `THIRD_PARTY_NOTICES.md`. Successful loading and decode checks do not establish that a mix sounds good: audition it in exploration, combat, boss phases, pause, mute and low volume.
