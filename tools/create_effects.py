"""Original soft particle and rune-decal textures, generated without image libraries."""
import math, struct, zlib
from pathlib import Path
out=Path(__file__).resolve().parents[1]/'resources/outpost/effects';out.mkdir(exist_ok=True)
def png(name,sample,size=256):
    raw=bytearray()
    for y in range(size):
        raw.append(0)
        for x in range(size):
            u=(x+.5)/size*2-1;v=(y+.5)/size*2-1
            a=max(0,min(1,sample(u,v)))
            raw.extend((255,255,255,round(a*255)))
    def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    data=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',size,size,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')
    (out/(name+'.png')).write_bytes(data)
def rune(x,y):
    r=math.hypot(x,y);a=math.atan2(y,x);ring=math.exp(-((r-.88)/.018)**2)+.5*math.exp(-((r-.68)/.012)**2)
    tick=abs(math.sin(a*12))<.075 and .72<r<.84
    diamond=abs(abs(x)+abs(y)-.86)<.014 and r>.62
    glyph=abs(math.sin(a*6))<.05 and .48<r<.61
    return min(1,ring+tick*.8+diamond*.7+glyph*.7)*max(0,min(1,(1-r)*20))
png('rune',rune)
png('soft',lambda x,y:math.exp(-(x*x+y*y)*7)*max(0,1-x*x-y*y))
png('spark',lambda x,y:math.exp(-(x*x*24+y*y*5))*max(0,1-x*x-y*y))
png('ring',lambda x,y:math.exp(-((math.hypot(x,y)-.88)/.04)**2))
