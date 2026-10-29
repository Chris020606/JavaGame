"""Import selected Kenney Graveyard Kit 5.0 meshes from the original CC0 zip."""
from pathlib import Path
import zipfile,json,hashlib,sys
names=('crypt','crypt-large','crypt-large-roof','crypt-door','gravestone-cross','gravestone-round','gravestone-broken',
       'grave','coffin','iron-fence','iron-fence-border-gate','stone-wall','stone-wall-column','altar-stone',
       'pillar-obelisk','pine-crooked','trunk','lantern-candle','lightpost-single','fire-basket','road','rocks-tall')
archive=Path(sys.argv[1]);out=Path(__file__).resolve().parents[1]/'resources/outpost/graveyard';out.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(archive) as pack:
    for name in names:
        for extension in ('.obj','.mtl'):(out/(name+extension)).write_bytes(pack.read('Models/OBJ format/'+name+extension))
    (out/'Textures').mkdir(exist_ok=True)
    (out/'Textures/colormap.png').write_bytes(pack.read('Models/OBJ format/Textures/colormap.png'))
    (out/'License.txt').write_bytes(pack.read('License.txt'))
(out/'SOURCE.json').write_text(json.dumps(dict(author='Kenney',license='CC0',version='5.0',
    source='https://opengameart.org/content/graveyard-kit',archive_sha256=hashlib.sha256(archive.read_bytes()).hexdigest(),models=names),indent=2)+'\n')
