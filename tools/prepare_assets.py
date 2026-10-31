"""Rebuild Outpost assets using only Python's standard library and bundled KayKit files."""
import copy, json, struct
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'resources/outpost'
OUT.mkdir(exist_ok=True)
def read_glb(path):
    b = path.read_bytes(); n = struct.unpack_from('<I', b, 12)[0]
    return json.loads(b[20:20+n]), b[28+n:]
def write_glb(path, d, b):
    d['buffers'] = [{'byteLength': len(b)}]
    j = json.dumps(d, separators=(',', ':')).encode(); j += b' ' * (-len(j)%4)
    b += b'\0' * (-len(b)%4)
    path.write_bytes(struct.pack('<III', 0x46546c67, 2, 28+len(j)+len(b)) + struct.pack('<II',len(j),0x4e4f534a)+j+struct.pack('<II',len(b),0x004e4942)+b)
# Remap animation targets by bone name, preserving each character's own bind pose.
def append_animations(d,b,source,selected):
    a,ab=read_glb(source)
    b += b'\0'*(-len(b)%4); offset=len(b); av=len(d['accessors']); bv=len(d['bufferViews'])
    for view in copy.deepcopy(a['bufferViews']):
        view['byteOffset']=view.get('byteOffset',0)+offset; d['bufferViews'].append(view)
    for acc in copy.deepcopy(a['accessors']):
        acc['bufferView']+=bv; d['accessors'].append(acc)
    names={n['name']:i for i,n in enumerate(d['nodes'])}
    for anim in copy.deepcopy(a['animations']):
        if anim['name'] not in selected: continue
        for sampler in anim['samplers']: sampler['input']+=av; sampler['output']+=av
        anim['channels']=[c for c in anim['channels'] if a['nodes'][c['target']['node']]['name'] in names]
        for c in anim['channels']: c['target']['node']=names[a['nodes'][c['target']['node']]['name']]
        d['animations'].append(anim)
    return b+ab
for character,output in [('Rogue_Hooded','raider'),('Ranger','archer'),('Mage','shaman')]:
    d,b=read_glb(ROOT/('resources/KayKit_Adventurers_2.0_FREE/Characters/gltf/'+character+'.glb'))
    d['animations']=[]
    b=append_animations(d,b,ROOT/'resources/knight.glb',('Running_A','Melee_1H_Attack_Chop','Melee_2H_Idle','Walking_A.001','Running_Strafe_Left','Running_Strafe_Right'))
    b=append_animations(d,b,ROOT/'resources/source/Rig_Medium_General.glb',('Death_A',))
    b=append_animations(d,b,ROOT/'resources/source/Rig_Medium_Simulation.glb',('Lie_Idle',))
    if output!='raider':
        clips=('Ranged_Bow_Draw','Ranged_Bow_Release') if output=='archer' else ('Ranged_Magic_Spellcasting',)
        b=append_animations(d,b,ROOT/'resources/source/Rig_Medium_CombatRanged.glb',clips)
    for img in d.get('images',[]):
        if 'uri' in img: img['uri']='../KayKit_Adventurers_2.0_FREE/Characters/gltf/'+img['uri']
    write_glb(OUT/(output+'.glb'),d,b)
# Player variants retain the full shared locomotion/combat set plus ranged clips.
for character, output, clips in [
    ('Knight', 'hero_warrior', ()),
    ('Ranger', 'hero_archer', ('Ranged_Bow_Draw', 'Ranged_Bow_Release', 'Ranged_Bow_Idle', 'Ranged_2H_Aiming', 'Ranged_2H_Shoot', 'Ranged_2H_Reload')),
    ('Mage', 'hero_mage', ('Ranged_Magic_Spellcasting','Ranged_Magic_Raise','Ranged_Magic_Shoot'))]:
    d,b=read_glb(ROOT/('resources/KayKit_Adventurers_2.0_FREE/Characters/gltf/'+character+'.glb'))
    d['animations']=[]
    source,_=read_glb(ROOT/'resources/knight.glb')
    b=append_animations(d,b,ROOT/'resources/knight.glb',tuple(a['name'] for a in source['animations']))
    b=append_animations(d,b,ROOT/'resources/source/Rig_Medium_CombatRanged.glb',clips)
    b=append_animations(d,b,ROOT/'resources/source/Rig_Medium_General.glb',('Death_A',))
    b=append_animations(d,b,ROOT/'resources/source/Rig_Medium_Simulation.glb',('Cheering','Lie_Idle'))
    for img in d.get('images',[]):
        if 'uri' in img: img['uri']='../KayKit_Adventurers_2.0_FREE/Characters/gltf/'+img['uri']
    write_glb(OUT/(output+'.glb'),d,b)
# Original flat-shaded scenery, exported as reusable model assets.
colors={'stone':(.36,.43,.46),'roof':(.15,.43,.43),'wood':(.36,.23,.14),'leaf':(.20,.38,.27),'leaflight':(.29,.48,.32),'iron':(.2,.25,.27)}
(OUT/'scenery.mtl').write_text(''.join(f'newmtl {k}\nKd {r} {g} {b}\nKa .2 .2 .2\n' for k,(r,g,b) in colors.items()))
class Model:
    def __init__(self): self.lines=['mtllib scenery.mtl']; self.count=0
    def mesh(self,verts,faces,mat):
        self.lines+=['usemtl '+mat]+['v %s %s %s'%v for v in verts]
        for face in faces:
            for j in range(1,len(face)-1): self.lines.append('f '+' '.join(str(self.count+i+1) for i in (face[0],face[j+1],face[j])))
        self.count+=len(verts)
    def box(self,x,y,z,w,h,l,mat):
        self.mesh([(x+dx*w/2,y+dy*h/2,z+dz*l/2) for dx,dy,dz in [(-1,-1,-1),(1,-1,-1),(1,-1,1),(-1,-1,1),(-1,1,-1),(1,1,-1),(1,1,1),(-1,1,1)]],[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)],mat)
    def peak(self,x,y,z,w,h,mat):
        self.mesh([(x-w/2,y,z-w/2),(x+w/2,y,z-w/2),(x+w/2,y,z+w/2),(x-w/2,y,z+w/2),(x,y+h,z)],[(0,3,2,1),(0,1,4),(1,2,4),(2,3,4),(3,0,4)],mat)
    def save(self,name): (OUT/(name+'.obj')).write_text('\n'.join(self.lines)+'\n')
m=Model();m.box(0,.7,0,2.8,1.4,2.8,'stone');m.box(0,1.65,0,2.5,.5,2.5,'wood');m.peak(0,1.9,0,3.8,1.5,'roof');m.box(0,.6,1.42,.7,1.2,.08,'wood')
for x in [-1.4,1.4]:
 for z in [-1.4,1.4]: m.box(x,1,z,.25,2,.25,'wood')
m.save('outpost')
m=Model();m.box(0,.9,0,.35,1.8,.35,'wood');m.peak(0,.7,0,2.6,2.1,'leaf');m.peak(0,1.7,0,2,1.9,'leaflight');m.save('pine')
m=Model();m.box(0,.35,0,1.3,.7,1,'stone');m.peak(-.1,.7,0,1.25,.5,'stone');m.save('rock')
m=Model();m.box(0,.4,0,.8,.8,.8,'wood')
for y in [.12,.68]: m.box(0,y,0,.84,.09,.84,'iron')
m.save('crate')
