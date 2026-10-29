"""Build original Dawn weapons. Class-fitted armor is built by fit_armor.py."""
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
(out/'tier-3.mtl').write_text('newmtl metal\nKd .58 .65 .73\nnewmtl trim\nKd .95 .66 .21\nnewmtl gem\nKd .15 .95 .8\n')
m=Mesh(3);m.box(0,.95,0,.24,1.6,.12);m.peak(0,1.75,0,.36,.46,'metal');m.box(0,.10,0,.82,.13,.2,'trim');m.box(0,-.2,0,.12,.5,.13);m.box(0,-.48,0,.2,.14,.2,'trim');m.box(0,.38,.085,.13,.2,.06,'gem');m.save('dawnblade')
m=Mesh(3);m.box(0,.70,0,.09,2.0,.09);m.box(0,1.60,0,.48,.1,.18,'trim');m.peak(0,1.66,0,.40,.52,'gem');m.peak(-.27,1.40,0,.14,.60);m.peak(.27,1.40,0,.14,.60);m.save('dawnstaff')
