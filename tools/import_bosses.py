"""Fetch the pinned CC0 KayKit Skeletons models and their original license."""
import hashlib,json,urllib.request,struct
from pathlib import Path
REV='15b62b9bad122f72926c10fb14d622c73819fa54'
BASE=f'https://raw.githubusercontent.com/KayKit-Game-Assets/KayKit-Character-Pack-Skeletons-1.0/{REV}/'
ROOT='addons/kaykit_character_pack_skeletons/'
out=Path(__file__).resolve().parents[1]/'resources/outpost/bosses'
out.mkdir(parents=True,exist_ok=True)
files={name:ROOT+'Characters/gltf/'+name for name in ('Skeleton_Warrior.glb','Skeleton_Mage.glb','Skeleton_Minion.glb','Skeleton_Rogue.glb','skeleton_texture.png')}
for name in ('Skeleton_Axe.gltf','Skeleton_Axe.bin','Skeleton_Staff.gltf','Skeleton_Staff.bin'):
    files[name]=ROOT+'Assets/gltf/'+name
files['LICENSE.txt']='LICENSE.txt'
hashes={}
for name,path in files.items():
    data=urllib.request.urlopen(BASE+path,timeout=60).read()
    (out/name).write_bytes(data);hashes[name]=hashlib.sha256(data).hexdigest()
# The Hollow King keeps the animated skeleton and robes; replace the wizard hat
# with a crown effect at runtime to give the final boss a distinct silhouette.
source=(out/'Skeleton_Mage.glb').read_bytes()
length=struct.unpack_from('<I',source,12)[0]
data=json.loads(source[20:20+length]);binary=source[28+length:]
removed=next(node.pop('mesh') for node in data['nodes'] if node.get('name')=='Skeleton_Mage_Hat')
data['meshes'].pop(removed)
for node in data['nodes']:
    if node.get('mesh',-1)>removed: node['mesh']-=1
payload=json.dumps(data,separators=(',',':')).encode();payload+=b' '*(-len(payload)%4)
derived=struct.pack('<III',0x46546c67,2,28+len(payload)+len(binary))+struct.pack('<II',len(payload),0x4e4f534a)+payload+struct.pack('<II',len(binary),0x004e4942)+binary
(out/'Hollow_King.glb').write_bytes(derived)
hashes['Hollow_King.glb']=hashlib.sha256(derived).hexdigest()
(out/'SOURCE.json').write_text(json.dumps(dict(author='Kay Lousberg',license='CC0-1.0',
    repository='https://github.com/KayKit-Game-Assets/KayKit-Character-Pack-Skeletons-1.0',revision=REV,sha256=hashes,derivation='Hollow_King.glb removes the mage hat; skeleton, textures and animation are retained.'),indent=2)+'\n')
print('Imported',len(files),'boss files at',REV)
