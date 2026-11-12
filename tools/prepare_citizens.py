"""Build six distinct citizen meshes with animations remapped to each mesh's own skeleton."""

import ast
import copy
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
# Reuse the established GLB remapper without executing the full asset rebuild.
source = ast.parse((ROOT / "tools/prepare_assets.py").read_text())
helpers = ast.Module(
    body=[
        node
        for node in source.body
        if isinstance(node, ast.FunctionDef)
        and node.name in {"read_glb", "write_glb", "append_animations"}
    ],
    type_ignores=[],
)
exec(compile(helpers, "prepare_assets.py", "exec"))
out = ROOT / "resources/outpost/citizens"
out.mkdir(exist_ok=True)
base = ROOT / "resources/KayKit_Adventurers_2.0_FREE/Characters/gltf"

EXCLUDE = {
    "bram": {"Barbarian_BearHat"},
    "kaelen": {"Ranger_Quiver", "Ranger_Cape"},
    "rowan": {"Rogue_Cape"},
    "theron": {"Knight_Helmet", "Knight_HelmetVisor", "Knight_Cape"},
    "mira": {"Mage_Hat", "Mage_Cape"},
    "owen": {"RogueHooded_Mask", "RogueHooded_Cape"},
}

for name, mesh in [
    ("bram", "Barbarian"),
    ("kaelen", "Ranger"),
    ("rowan", "Rogue"),
    ("theron", "Knight"),
    ("mira", "Mage"),
    ("owen", "Rogue_Hooded"),
]:
    data, binary = read_glb(base / (mesh + ".glb"))
    data["animations"] = []
    binary = append_animations(
        data,
        binary,
        ROOT / "resources/knight.glb",
        {"Melee_Unarmed_Idle", "Walking_A.001", "Walking_A"},
    )
    if len(data["animations"]) < 2:
        raise RuntimeError("Missing idle/walk clips for " + name)

    # Detach military/combat gear and helmets so rescued citizens have distinct civilian silhouettes
    to_remove = set()
    for idx, node in enumerate(data.get("nodes", [])):
        if node.get("name") in EXCLUDE.get(name, set()):
            to_remove.add(idx)
            node.pop("mesh", None)
            node.pop("skin", None)

    for node in data.get("nodes", []):
        if "children" in node:
            node["children"] = [c for c in node["children"] if c not in to_remove]

    write_glb(out / (name + ".glb"), data, binary)
    print(name, mesh, [a["name"] for a in data["animations"]], "detached:", to_remove)
