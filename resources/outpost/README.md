# Outpost assets

`raider.glb`, `archer.glb`, and `shaman.glb` are derived from the bundled KayKit Adventurers Rogue_Hooded, Ranger, and Mage. Movement/melee clips come from `resources/knight.glb`; bow and spellcasting clips come from the retained KayKit source `resources/source/Rig_Medium_CombatRanged.glb`. KayKit character and animation packs are CC0; see the License.txt files in their respective resource folders. The sword remains in the original Adventurers Assets/gltf directory with its texture and binary buffer.

The scenery OBJ/MTL files are original geometry created for this project. Rebuild all derived assets from the project root with `python3 tools/prepare_assets.py`. Do not move the derived raider independently of its referenced textures.
