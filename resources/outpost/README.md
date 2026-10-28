# Outpost assets

`raider.glb`, `archer.glb`, and `shaman.glb` are derived from the bundled KayKit Adventurers Rogue_Hooded, Ranger, and Mage. Movement/melee clips come from `resources/knight.glb`; bow and spellcasting clips come from the retained KayKit source `resources/source/Rig_Medium_CombatRanged.glb`. KayKit character and animation packs are CC0; see the License.txt files in their respective resource folders. The sword remains in the original Adventurers Assets/gltf directory with its texture and binary buffer.

The scenery OBJ/MTL files are original geometry created for this project. Rebuild all derived assets from the project root with `python3 tools/prepare_assets.py`. Do not move the derived raider independently of its referenced textures.

`hero_archer.glb` and `hero_mage.glb` use the Ranger and Mage meshes, the full knight movement/melee animation set, and class-specific ranged clips from the same bundled CC0 sources. Their textures remain relative to the Adventurers character directory.

The `nature/` directory contains selected unmodified Kenney Nature Kit (2.1) GLBs downloaded from https://opengameart.org/content/nature-kit under CC0. The original license and source/archive checksum are included. To reimport, run `python3 tools/import_nature.py /path/to/Nature-Kit.zip`. These downloaded models are independent of `prepare_assets.py`.
