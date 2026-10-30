"""Compose original looping scores and layered Foley with Python's standard library.
No downloaded recordings, samples, or additional dependencies are used.
"""
import math, random, struct, wave
from array import array
from pathlib import Path
OUT=Path(__file__).resolve().parents[1]/'resources/audio';OUT.mkdir(exist_ok=True)
TAU=math.tau

def pcm(name,left,right,rate):
    data=array('h')
    peak=max(max(map(abs,left)),max(map(abs,right)),.001)
    gain=.86/max(1,peak)
    for l,r in zip(left,right):
        data.extend((round(math.tanh(l*gain)*30000),round(math.tanh(r*gain)*30000)))
    import sys
    if sys.byteorder!='little':data.byteswap()
    with wave.open(str(OUT/(name+'.wav')),'wb') as f:
        f.setnchannels(2);f.setsampwidth(2);f.setframerate(rate);f.writeframes(data.tobytes())

def effect(name,duration,kind,seed=1):
    rate=44100;rng=random.Random(seed);left=array('f');right=array('f');low=0
    for i in range(int(rate*duration)):
        t=i/rate;u=t/duration;noise=rng.uniform(-1,1);low+=.16*(noise-low)
        attack=min(1,t/.004);tail=(1-u)**2;value=0
        if kind=='swing':value=.44*(noise-low)*math.sin(math.pi*u)**2+.12*math.sin(TAU*(260*t-150*t*t))*tail
        elif kind=='hit':value=(.46*low*math.exp(-t*24)+.27*math.sin(TAU*(115*t-70*t*t))*math.exp(-t*17)+.15*math.sin(TAU*2310*t)*math.exp(-t*50))
        elif kind=='hurt':value=.24*low*math.exp(-t*12)+.30*math.sin(TAU*(83*t-25*t*t))*tail
        elif kind=='kill':value=.22*low*math.exp(-t*9)+.14*math.sin(TAU*196*t)*tail+.1*math.sin(TAU*294*t)*tail
        elif kind=='wave':value=sum(.08*math.sin(TAU*f*t+.15*math.sin(TAU*4*t)) for f in (146.83,220,293.66))*math.sin(math.pi*u)
        elif kind=='upgrade':
            value=sum(.13*math.sin(TAU*f*t)*math.exp(-max(0,t-start)*5)*(t>=start) for f,start in ((523.25,0),(659.25,.09),(783.99,.18),(1046.5,.30)))
        elif kind=='bow':value=.38*math.sin(TAU*(170*t+180*t*t))*math.exp(-t*24)+.13*(noise-low)*math.exp(-t*15)
        elif kind=='magic':value=(.15*math.sin(TAU*(380*t+320*t*t))+.13*math.sin(TAU*(570*t+500*t*t))+.12*low)*math.sin(math.pi*u)
        elif kind=='parry':value=sum(.16*math.sin(TAU*f*t)*math.exp(-t*decay) for f,decay in ((1318,9),(2093,14),(2794,20)))+.12*noise*math.exp(-t*80)
        elif kind=='slam':value=.45*math.sin(TAU*(55*t-12*t*t))*math.exp(-t*4)+.38*low*math.exp(-t*7)
        elif kind=='dash':value=.35*(noise-low)*math.sin(math.pi*u)*tail
        elif kind=='ward':value=(.17*math.sin(TAU*392*t)+.12*math.sin(TAU*587.33*t)+.08*math.sin(TAU*783.99*t))*math.sin(math.pi*u)**2
        elif kind=='victory':
            for f,start in ((392,0),(493.88,.18),(587.33,.36),(783.99,.60),(987.77,.60)):
                age=t-start
                if age>=0:value+=(.17*math.sin(TAU*f*age)+.04*math.sin(TAU*f*2*age))*(1-math.exp(-age*60))*math.exp(-age*2.7)
        elif kind=='boss-fall':value=.40*math.sin(TAU*(64*t-17*t*t))*math.exp(-t*3)+.32*low*math.exp(-t*5)+.16*noise*math.exp(-t*14)*(.5+.5*math.cos(t*95))
        value*=attack*min(1,(duration-t)/.012)
        left.append(value);right.append(value*(.97+.03*math.sin(t*11)))
    # Short stereo reflections soften synthetic edges and give metal/stone a space.
    for delay,amount in ((.031,.16),(.067,.1)):
        offset=int(rate*delay)
        for i in range(len(left)-1,offset-1,-1):
            left[i]+=right[i-offset]*amount;right[i]+=left[i-offset]*amount*.8
    pcm(name,left,right,rate)

for name,duration in [('swing',.30),('hit',.28),('hurt',.46),('kill',.5),('wave',1.5),('upgrade',1.1),('bow',.28),('magic',.55),('parry',.65),('slam',1.0),('dash',.35),('ward',.95),('victory',1.8),('boss-fall',1.2)]:
    effect(name,duration,name,19)
for kind in ('swing','hit'):
    for variant in range(3):effect(kind+str(variant),(.27 if kind=='swing' else .25)+variant*.025,kind,31+variant*17)

RATE=24000;BEAT=.75;LENGTH=24;FRAMES=RATE*LENGTH
CHORDS=((50,53,57),(46,50,53),(53,57,60),(48,52,55),(50,53,57),(46,50,53),(43,46,50),(45,49,52))
MELODIES=((74,77,81,79,77,74,72,69),(74,69,72,65,69,67,64,61),(81,79,77,74,76,72,69,72),(62,69,65,61,62,58,57,61),(74,77,72,69,70,74,69,73))
def score(name,theme):
    left=array('f',[0])*FRAMES;right=array('f',[0])*FRAMES
    def note(start,duration,midi,amp,voice,pan=0):
        freq=440*2**((midi-69)/12);offset=round(start*RATE);samples=int(duration*RATE)
        lg=math.sqrt((1-pan)*.5);rg=math.sqrt((1+pan)*.5)
        for i in range(samples):
            t=i/RATE;u=t/duration
            if voice=='pad':
                env=math.sin(math.pi*u)**1.5
                sound=.65*math.sin(TAU*freq*t)+.22*math.sin(TAU*freq*1.003*t)+.13*math.sin(TAU*freq*2*t)
            elif voice=='pluck':
                env=(1-math.exp(-t*180))*math.exp(-t*3.4)*min(1,(duration-t)/.08)
                sound=math.sin(TAU*freq*t)+.28*math.sin(TAU*freq*2*t)*math.exp(-t*5)+.12*math.sin(TAU*freq*3*t)*math.exp(-t*8)
            else:
                env=min(1,t/.11)*min(1,(duration-t)/.18)
                phase=TAU*freq*t+.035*math.sin(TAU*4.8*t)
                sound=.8*math.sin(phase)+.13*math.sin(phase*2)+.07*math.sin(phase*3)
            value=amp*sound*max(0,env);index=(offset+i)%FRAMES
            left[index]+=value*lg;right[index]+=value*rg
    for bar,chord in enumerate(CHORDS):
        start=bar*3
        if theme<5:
            for j,midi in enumerate(chord):note(start,4.5,midi,.095 if theme!=3 else .075,'pad',(j-1)*.55)
            note(start,3,chord[0]-12,.09,'pad',0)
            for beat in range(4):
                pitch=chord[beat%3]+12
                note(start+beat*BEAT,1.8,pitch,.07 if theme in (0,2,4) else .035,'pluck',(-1 if beat%2 else 1)*.4)
            melody=MELODIES[theme]
            note(start+.375,1.3,melody[bar],.10 if theme!=3 else .07,'flute',.18)
            if bar%2==1:note(start+1.875,1.6,melody[(bar+2)%8],.07,'pluck',-.25)
        else:
            # Harmony-free percussion stem follows the same 8-bar loop as every theme.
            rng=random.Random(700+bar)
            for beat in range(8):
                offset=int((start+beat*.375)*RATE)
                for i in range(int(.36*RATE)):
                    t=i/RATE;noise=rng.uniform(-1,1)
                    drum=.24*math.sin(TAU*(72*t-54*t*t))*math.exp(-t*16) if beat%2==0 else .07*noise*math.exp(-t*45)
                    if beat in (2,6):drum+=.12*noise*math.exp(-t*22)
                    index=(offset+i)%FRAMES;left[index]+=drum;right[index]+=drum*.94
    # Circular delay preserves the tails over the loop boundary, with no hard reset.
    dryL=array('f',left);dryR=array('f',right)
    for delay,amount in ((.173,.18),(.287,.13),(.431,.09)):
        shift=int(delay*RATE)
        for i in range(FRAMES):
            left[i]+=dryR[(i-shift)%FRAMES]*amount;right[i]+=dryL[(i-shift-97)%FRAMES]*amount
    pcm(name,left,right,RATE)
for theme,name in enumerate(('meadow','cemetery','frost','necropolis','title','battle')):
    score('music-'+name,theme);print('Composed',name,flush=True)

def boss_score(enrage=False):
    """160-BPM duel score: low strings, brass calls, driving toms and cymbals."""
    left=array('f',[0])*FRAMES;right=array('f',[0])*FRAMES
    rng=random.Random(194)
    def voice(start,duration,midi,amp,pan,brass=False):
        freq=440*2**((midi-69)/12);offset=round(start*RATE)
        for i in range(round(duration*RATE)):
            t=i/RATE;env=min(1,t/.018)*min(1,(duration-t)/.06)*math.exp(-t*(1 if brass else 6))
            phase=TAU*freq*t
            tone=math.sin(phase)+.30*math.sin(phase*2)+.17*math.sin(phase*3)+.08*math.sin(phase*5)
            index=(offset+i)%FRAMES;value=amp*env*tone
            left[index]+=value*math.sqrt((1-pan)/2);right[index]+=value*math.sqrt((1+pan)/2)
    for bar in range(16):
        start=bar*1.5;root=(38,38,34,34,41,41,36,37)[bar%8]
        for step in range(8 if not enrage else 16):
            interval=.1875 if not enrage else .09375
            voice(start+step*interval,.24,root+(12 if step%4 in (1,3) else 0),.13 if not enrage else .075,(-1 if step%2 else 1)*.45)
        if not enrage:
            for midi in (root+12,root+19,root+24):voice(start,.7,midi,.085,0,True)
            voice(start+.75,.60,root+([27,24,22,19][bar%4]),.11,.22,True)
        for beat in range(8):
            offset=round((start+beat*.1875)*RATE)
            for i in range(round(.30*RATE)):
                t=i/RATE;noise=rng.uniform(-1,1)
                if enrage:value=.065*noise*math.exp(-t*55)+(.14*noise*math.exp(-t*16) if beat in (2,6) else 0)
                else:value=(.34*math.sin(TAU*(78*t-70*t*t))*math.exp(-t*14) if beat%2==0 else .07*noise*math.exp(-t*60))+(.16*noise*math.exp(-t*24) if beat in (2,6) else 0)
                index=(offset+i)%FRAMES;left[index]+=value;right[index]+=value*.94
    dryL=array('f',left);dryR=array('f',right)
    for i in range(FRAMES):
        left[i]+=dryR[(i-round(.1875*RATE))%FRAMES]*.14
        right[i]+=dryL[(i-round(.28125*RATE))%FRAMES]*.14
    pcm('music-enrage' if enrage else 'music-boss',left,right,RATE)
boss_score();boss_score(True);print('Composed boss duel and enrage layer',flush=True)
