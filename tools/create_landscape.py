"""Create original low-poly mountain terrain beyond the playable clearing."""
from pathlib import Path
import math
out=Path(__file__).resolve().parents[1]/'resources/outpost/landscape';out.mkdir(exist_ok=True)
(out/'mountains.mtl').write_text('newmtl foothill\nKd 0.24 0.32 0.25\nnewmtl rock\nKd 0.32 0.37 0.40\nnewmtl snow\nKd 0.79 0.85 0.88\n')
peaks=[(-92,-78,46,26),(-38,-107,35,24),(57,-95,54,30),(106,-15,40,26),(98,78,49,28),(23,116,39,25),(-77,93,53,31),(-119,11,38,24)]
def height(x,z):
    r=math.hypot(x,z)
    if r<38:return -2
    h=max(a*math.exp(-((x-cx)**2+(z-cz)**2)/(w*w)) for cx,cz,a,w in peaks)
    rough=(math.sin(x*.19+z*.11)+math.sin(x*.09-z*.23))*min(3,h*.12)
    return max(-1,h+rough-1)
for frost in (False,True):
    lines=['mtllib mountains.mtl'];index=0
    for x in range(-176,176,8):
        for z in range(-176,176,8):
            if abs(x)<32 and abs(z)<32:continue
            quad=[(x,height(x,z),z),(x+8,height(x+8,z),z),(x+8,height(x+8,z+8),z+8),(x,height(x,z+8),z+8)]
            for tri in ((0,2,1),(0,3,2)):
                pts=[quad[i] for i in tri];h=sum(p[1] for p in pts)/3
                mat='snow' if h>(14 if frost else 34) else 'rock' if h>9 else 'foothill';lines.append('usemtl '+mat)
                a,b,c=pts;u=[b[i]-a[i] for i in range(3)];v=[c[i]-a[i] for i in range(3)];n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]];d=math.sqrt(sum(k*k for k in n)) or 1
                lines+=['v '+' '.join(map(str,p)) for p in pts];lines+=['vn '+' '.join(str(k/d) for k in n)]
                lines.append('f '+' '.join(f'{index+j+1}//{index//3+1}' for j in range(3)));index+=3
    (out/('mountains-frost.obj' if frost else 'mountains.obj')).write_text('\n'.join(lines)+'\n')
print('Built layered mountain terrain')
