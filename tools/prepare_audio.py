"""Generate original short game sound effects; no third-party audio or dependencies."""
import math, random, struct, wave
from pathlib import Path
out=Path(__file__).resolve().parents[1]/'resources/audio'
out.mkdir(exist_ok=True)
rng=random.Random(19)
def write(name,duration,sample):
    data=[]
    for i in range(int(22050*duration)):
        t=i/22050; u=t/duration
        envelope=min(1,t/.008)*(1-u)**2
        value=max(-1,min(1,sample(t,u)*envelope))
        data.append(struct.pack('<h',int(value*26000)))
    with wave.open(str(out/(name+'.wav')),'wb') as f:
        f.setnchannels(1);f.setsampwidth(2);f.setframerate(22050);f.writeframes(b''.join(data))
write('swing',.23,lambda t,u: .45*rng.uniform(-1,1)*math.sin(math.pi*u)+.12*math.sin(2*math.pi*(650*t-900*t*t)))
write('hit',.18,lambda t,u: .5*rng.uniform(-1,1)*math.exp(-u*8)+.4*math.sin(2*math.pi*(170*t-260*t*t)))
write('hurt',.3,lambda t,u: .35*math.sin(2*math.pi*95*t)+.18*rng.uniform(-1,1))
write('kill',.4,lambda t,u: .28*math.sin(2*math.pi*(520+int(u*3)*180)*t))
write('wave',.8,lambda t,u: .22*(math.sin(2*math.pi*220*t)+math.sin(2*math.pi*330*t)))
write('upgrade',.65,lambda t,u: .35*math.sin(2*math.pi*[440,554,659,880][min(3,int(u*4))]*t))
