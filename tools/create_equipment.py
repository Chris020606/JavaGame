"""Build original low-poly armor attachments and the Dawn weapons, including normals."""
from pathlib import Path
import math
out=Path(__file__).resolve().parents[1]/'resources/outpost/equipment';out.mkdir(parents=True,exist_ok=True)
class Mesh:
    def __init__(self,tier):
        self.lines=[f'mtllib tier-{tier}.mtl'];self.v=0;self.n=0
    def face(self,points,material):
        self.lines+=['usemtl '+material]
        a,b,c=points[:3];u=[b[i]-a[i] for i in range(3)];v=[c[i]-a[i] for i in range(3)]
        n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]];length=math.sqrt(sum(x*x for x in n)) or 1
        self.lines+=['v '+' '.join(map(str,p)) for p in points]+['vn '+' '.join(str(x/length) for x in n)]
        self.n+=1
        for i in range(1,len(points)-1):self.lines.append('f '+' '.join(f'{self.v+j+1}//{self.n}' for j in [0,i,i+1]))
        self.v+=len(points)
    def box(self,x,y,z,w,h,d,mat='metal'):
        points=[(x+a*w/2,y+b*h/2,z+c*d/2) for a,b,c in [(-1,-1,-1),(1,-1,-1),(1,-1,1),(-1,-1,1),(-1,1,-1),(1,1,-1),(1,1,1),(-1,1,1)]]
        for face in [(0,1,2,3),(4,7,6,5),(3,2,6,7),(0,4,5,1),(1,5,6,2),(0,3,7,4)]:self.face([points[i] for i in face],mat)
    def peak(self,x,y,z,w,h,mat='trim'):
        p=[(x-w/2,y,z-w/2),(x+w/2,y,z-w/2),(x+w/2,y,z+w/2),(x-w/2,y,z+w/2),(x,y+h,z)]
        for f in [(0,1,4),(1,2,4),(2,3,4),(3,0,4)]:self.face([p[i] for i in f],mat)
    def save(self,name):(out/(name+'.obj')).write_text('\n'.join(self.lines)+'\n')
for tier,(metal,trim) in enumerate([((.58,.66,.73),(.19,.29,.36)),((.26,.25,.4),(.12,.8,.76)),((.58,.65,.73),(.95,.66,.21))],1):
    (out/f'tier-{tier}.mtl').write_text(f'newmtl metal\nKd {metal[0]} {metal[1]} {metal[2]}\nnewmtl trim\nKd {trim[0]} {trim[1]} {trim[2]}\nnewmtl gem\nKd .15 .95 .8\n')
    m=Mesh(tier);m.box(0,.77,0,.91,.27,.82);m.box(0,.76,.43,.12,.42,.08,'trim');m.box(0,.90,0,.95,.07,.86,'trim')
    if tier>=2:m.peak(0,.94,0,.26,.25+tier*.08)
    if tier==3:
        for x in [-.35,.35]:m.peak(x,.99,0,.15,.28)
    m.save(f'head-{tier}')
    m=Mesh(tier);m.box(0,.02,.14,.64,.5,.39);m.box(0,.03,.35,.13,.38,.06,'trim');m.box(0,-.22,.14,.69,.08,.41,'trim')
    if tier>=2:m.peak(0,.28,.12,.24,.13)
    m.save(f'chest-{tier}')
    m=Mesh(tier);m.box(0,0,0,.34,.19,.4);m.box(0,.12,0,.36,.06,.42,'trim')
    if tier>=2:m.peak(0,.16,0,.18,.13*tier)
    m.save(f'shoulder-{tier}')
    m=Mesh(tier);m.box(0,-.12,.045,.24,.31,.24);m.box(0,-.05,.18,.13,.24,.04,'trim');m.save(f'legs-{tier}')
    m=Mesh(tier);m.box(0,.015,.1,.29,.22,.42);m.box(0,.13,.03,.3,.08,.28,'trim');m.save(f'boots-{tier}')
m=Mesh(3);m.box(0,.95,0,.24,1.6,.12);m.peak(0,1.75,0,.36,.46,'metal');m.box(0,.10,0,.82,.13,.2,'trim');m.box(0,-.2,0,.12,.5,.13);m.box(0,-.48,0,.2,.14,.2,'trim');m.box(0,.38,.085,.13,.2,.06,'gem');m.save('dawnblade')
m=Mesh(3);m.box(0,.70,0,.09,2.0,.09);m.box(0,1.60,0,.48,.1,.18,'trim');m.peak(0,1.66,0,.40,.52,'gem');m.peak(-.27,1.40,0,.14,.60);m.peak(.27,1.40,0,.14,.60);m.save('dawnstaff')
