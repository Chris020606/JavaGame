"""Build class-fitted, fully skinned armor from the bundled CC0 character surfaces.
Keeps each class's skeleton and weights. No rigid chest/leg boxes are used.
"""
import copy,json,struct,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'resources/outpost/equipment';OUT.mkdir(exist_ok=True)
def read(path):
    data=path.read_bytes();n=struct.unpack_from('<I',data,12)[0]
    return json.loads(data[20:20+n]),data[28+n:]
def write(path,d,raw):
    raw+=b'\0'*(-len(raw)%4);d['buffers']=[{'byteLength':len(raw)}]
    text=json.dumps(d,separators=(',',':')).encode();text+=b' '*(-len(text)%4)
    path.write_bytes(struct.pack('<III',0x46546c67,2,28+len(text)+len(raw))+struct.pack('<II',len(text),0x4e4f534a)+text+struct.pack('<II',len(raw),0x004e4942)+raw)
SIZE={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4,'MAT4':16};FMT={5121:'B',5123:'H',5125:'I',5126:'f'}
def values(d,raw,index):
    a=d['accessors'][index];v=d['bufferViews'][a['bufferView']];fmt='<'+FMT[a['componentType']]*SIZE[a['type']];size=struct.calcsize(fmt)
    offset=v.get('byteOffset',0)+a.get('byteOffset',0);stride=v.get('byteStride',size)
    return [struct.unpack_from(fmt,raw,offset+i*stride) for i in range(a['count'])]
class Buffer:
    def __init__(self,d):self.d=d;self.raw=bytearray()
    def add(self,rows,kind,component=5126):
        self.raw+=b'\0'*(-len(self.raw)%4);offset=len(self.raw);fmt='<'+FMT[component]*SIZE[kind]
        for row in rows:self.raw+=struct.pack(fmt,*row)
        vi=len(self.d['bufferViews']);self.d['bufferViews'].append({'buffer':0,'byteOffset':offset,'byteLength':len(self.raw)-offset})
        a={'bufferView':vi,'componentType':component,'count':len(rows),'type':kind}
        if kind=='VEC3':a.update(min=[min(r[i] for r in rows) for i in range(3)],max=[max(r[i] for r in rows) for i in range(3)])
        index=len(self.d['accessors']);self.d['accessors'].append(a);return index
knight,kraw=read(ROOT/'resources/knight.glb')
for hero,file in [('warrior','resources/knight.glb'),('archer','resources/outpost/hero_archer.glb'),('mage','resources/outpost/hero_mage.glb')]:
    source,raw=read(ROOT/file);head=next(m for m in source['meshes'] if m['name'].endswith('_Head'))
    bounds=source['accessors'][head['primitives'][0]['attributes']['POSITION']];head_min=bounds['min'];head_max=bounds['max']
    skin=source['skins'][0];joint_names=[source['nodes'][n]['name'] for n in skin['joints']];head_joint=joint_names.index('head')
    for tier in range(1,4):
        for slot in ['head','chest','legs','boots']:
            d={'asset':{'version':'2.0','generator':'Outpost fitted armor'},'scene':source.get('scene',0),'scenes':copy.deepcopy(source['scenes']),
               'nodes':copy.deepcopy(source['nodes']),'skins':copy.deepcopy(source['skins']),'meshes':[],'bufferViews':[],'accessors':[]}
            for node in d['nodes']:node.pop('mesh',None);node.pop('skin',None)
            metal=[.47,.55,.62,1] if tier==1 else [.17,.27,.32,1] if tier==2 else [.55,.62,.67,1]
            trim=[.17,.23,.28,1] if tier==1 else [.12,.72,.68,1] if tier==2 else [.93,.61,.19,1]
            d['materials']=[{'name':'Fitted metal','pbrMetallicRoughness':{'baseColorFactor':metal,'metallicFactor':.65,'roughnessFactor':.36}},
                            {'name':'Trim','pbrMetallicRoughness':{'baseColorFactor':trim,'metallicFactor':.5,'roughnessFactor':.3}}]
            buf=Buffer(d);ibm=values(source,raw,skin['inverseBindMatrices']);d['skins'][0]['inverseBindMatrices']=buf.add(ibm,'MAT4')
            selected=[m for m in source['meshes'] if (slot=='chest' and any(m['name'].endswith(s) for s in ['_Body','_ArmLeft','_ArmRight'])) or (slot in ['legs','boots'] and '_Leg' in m['name'])]
            if slot=='head':selected=[m for m in (source if hero=='mage' else knight)['meshes'] if m['name'] in (['Mage_Hat'] if hero=='mage' else ['Knight_Helmet'] if hero=='archer' else ['Knight_Helmet','Knight_HelmetVisor'])]
            for mesh in selected:
                src,sraw=(knight,kraw) if slot=='head' and hero!='mage' else (source,raw)
                for primitive in mesh['primitives']:
                    att=primitive['attributes'];pos=values(src,sraw,att['POSITION']);norm=values(src,sraw,att['NORMAL'])
                    joints=values(src,sraw,att['JOINTS_0']);weights=values(src,sraw,att['WEIGHTS_0']);indices=[r[0] for r in values(src,sraw,primitive['indices'])]
                    if slot=='head' and hero!='mage':
                        # Fit the original helmet to each class's head bounds, preserving the face opening.
                        kh=next(m for m in knight['meshes'] if m['name']=='Knight_Head');kb=knight['accessors'][kh['primitives'][0]['attributes']['POSITION']]
                        scale=[(head_max[i]-head_min[i])/(kb['max'][i]-kb['min'][i]) for i in range(3)]
                        center=[(head_max[i]+head_min[i])/2 for i in range(3)];kc=[(kb['max'][i]+kb['min'][i])/2 for i in range(3)]
                        pos=[tuple((p[i]-kc[i])*scale[i]+center[i] for i in range(3)) for p in pos]
                        joints=[(head_joint,0,0,0)]*len(pos);weights=[(1.,0.,0.,0.)]*len(pos)
                    # Small normal clearance follows the exact source surface and skin weights.
                    # Welded positions need identical clearance even when flat-shaded normals differ.
                    smooth={}
                    for p,n in zip(pos,norm):
                        key=tuple(round(v,5) for v in p);total=smooth.setdefault(key,[0.,0.,0.])
                        for j in range(3):total[j]+=n[j]
                    offset=.012+tier*.004;expanded=[]
                    for p,n in zip(pos,norm):
                        total=smooth[tuple(round(v,5) for v in p)];length=math.sqrt(sum(v*v for v in total)) or 1
                        expanded.append(tuple(p[j]+total[j]/length*offset for j in range(3)))
                    pos=expanded
                    groups=[[],[]]
                    for i in range(0,len(indices),3):
                        tri=indices[i:i+3];y=sum(pos[k][1] for k in tri)/3;x=sum(pos[k][0] for k in tri)/3;z=sum(pos[k][2] for k in tri)/3
                        if slot=='legs' and y<.22 or slot=='boots' and y>=.22:continue
                        accent=(slot=='head' and (mesh['name'].endswith('Visor') or y>head_max[1]-.08)) or (slot=='chest' and (.95<y<1.03 or .55<y<.62 or abs(x)>.43 and y>1.10)) or (slot in ['legs','boots'] and .16<y<.26)
                        groups[int(accent)].extend(tri)
                    attributes={'POSITION':buf.add(pos,'VEC3'),'NORMAL':buf.add(norm,'VEC3'),'JOINTS_0':buf.add(joints,'VEC4',5123),'WEIGHTS_0':buf.add(weights,'VEC4')}
                    primitives=[]
                    for material,group in enumerate(groups):
                        if group:primitives.append({'attributes':attributes,'indices':buf.add([(v,) for v in group],'SCALAR',5123),'material':material})
                    if not primitives:continue
                    index=len(d['meshes']);d['meshes'].append({'name':mesh['name']+'_'+slot,'primitives':primitives})
                    node=len(d['nodes']);d['nodes'].append({'name':'Armor_'+str(index),'mesh':index,'skin':0})
                    d['scenes'][d['scene']]['nodes'].append(node)
            write(OUT/f'{hero}-{slot}-{tier}.glb',d,buf.raw)
print('Built 36 fitted, skinned armor pieces')
