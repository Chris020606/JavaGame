"""Import the selected, unmodified CC0 OBJ/MTL models from Kenney Nature Kit (2.1).
Usage: python3 tools/import_nature.py /path/to/Nature-Kit.zip
Source: https://opengameart.org/content/nature-kit
"""
from pathlib import Path
import hashlib, json, sys, zipfile
names = ('tree_pineTallA_detailed', 'tree_pineRoundC', 'tree_oak',
         'stone_largeA', 'stone_tallB', 'plant_bushDetailed', 'grass_large',
         'statue_columnDamaged', 'statue_obelisk', 'tent_detailedOpen',
         'log_stack', 'flower_purpleC', 'mushroom_redGroup',
         'ground_grass','ground_pathCross','ground_pathStraight','platform_stone',
         'campfire_stones','fence_simple','statue_ring','bridge_stone','cliff_half_stone')
archive=Path(sys.argv[1])
out=Path(__file__).resolve().parents[1]/'resources/outpost/nature'
out.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(archive) as pack:
    for name in names:
        for extension in ('.obj','.mtl'):
            (out/(name+extension)).write_bytes(pack.read('Models/OBJ format/'+name+extension))
    (out/'License.txt').write_bytes(pack.read('License.txt'))
(out/'SOURCE.json').write_text(json.dumps(dict(author='Kenney',pack='Nature Kit (2.1)',
    license='CC0',source='https://opengameart.org/content/nature-kit',
    archive_sha256=hashlib.sha256(archive.read_bytes()).hexdigest(),models=names),indent=2)+'\n')
