"""Edit the captured Outpost footage into a 1080p extended gameplay trailer.
Uses FFmpeg and Python's standard library; titles are rendered by trailerTitles.
"""
import argparse, array, csv, hashlib, json, math, shutil, subprocess, sys, wave
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def run(args):
    subprocess.run([str(a) for a in args],check=True)
def stamp(seconds):
    millis=round(seconds*1000);h,millis=divmod(millis,3600000);m,millis=divmod(millis,60000);s,millis=divmod(millis,1000)
    return f'{h:02}:{m:02}:{s:02},{millis:03}'
def read_pcm(path):
    with wave.open(str(path),'rb') as f:
        if f.getsampwidth()!=2 or f.getnchannels()!=2:raise ValueError(f'Expected stereo PCM16: {path}')
        samples=array.array('h',f.readframes(f.getnframes()));rate=f.getframerate()
        if sys.byteorder!='little':samples.byteswap()
    return samples,rate

def soundtrack(folder,shots,ffmpeg):
    rate=24000;duration=sum(float(s['seconds']) for s in shots);mix=array.array('f',[0])*(round(duration*rate)*2)
    cache={}
    def samples(name):
        if name not in cache:
            source=ROOT/'resources/audio'/f'{name}.wav';data,sr=read_pcm(source)
            if sr!=rate:
                target=folder/'audio24'/source.name;target.parent.mkdir(exist_ok=True)
                run([ffmpeg,'-v','error','-y','-i',source,'-ar',rate,'-ac',2,'-c:a','pcm_s16le',target]);data,sr=read_pcm(target)
            cache[name]=data
        return cache[name]
    elapsed=0
    for shot in shots:
        duration=float(shot['seconds']);offset=round(elapsed*rate);frames=round(duration*rate)
        theme=shot['music'];tracks=[('music-'+theme,.63)]
        if theme=='battle':tracks=[('music-meadow',.48),('music-battle',.48)]
        if theme=='enrage':tracks=[('music-boss',.57),('music-enrage',.40)]
        for name,gain in tracks:
            data=samples(name);length=len(data)//2
            for i in range(frames):
                envelope=min(1,i/(rate*.7),(frames-i)/(rate*.9));source=(i%length)*2;target=(offset+i)*2
                mix[target]+=data[source]/32768*gain*envelope;mix[target+1]+=data[source+1]/32768*gain*envelope
        cuefile=folder/'raw'/f'{shot["id"]}-cues.tsv'
        if cuefile.exists():
            for line in cuefile.read_text().splitlines():
                at,name=line.split('\t');start=round((elapsed+float(at))*rate)
                if not (ROOT/'resources/audio'/f'{name}.wav').exists():continue
                data=samples(name)
                for i in range(min(len(data)//2,len(mix)//2-start)):
                    if start+i<0:continue
                    mix[(start+i)*2]+=data[i*2]/32768*.70;mix[(start+i)*2+1]+=data[i*2+1]/32768*.70
        elapsed+=duration
    output=array.array('h',(round(math.tanh(value)*29000) for value in mix))
    if sys.byteorder!='little':output.byteswap()
    with wave.open(str(folder/'soundtrack.wav'),'wb') as f:
        f.setnchannels(2);f.setsampwidth(2);f.setframerate(rate);f.writeframes(output.tobytes())

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--folder',default='build/marketing');parser.add_argument('--resume',action='store_true');args=parser.parse_args()
    folder=(ROOT/args.folder).resolve();ffmpeg=shutil.which('ffmpeg');ffprobe=shutil.which('ffprobe')
    if not ffmpeg or not ffprobe:raise SystemExit('Install FFmpeg and ffprobe first.')
    with (folder/'shots.tsv').open() as f:shots=list(csv.DictReader(f,delimiter='\t'))
    edited=folder/'edited';edited.mkdir(exist_ok=True);concat=[];captions=[];elapsed=0;hashes=[]
    for index,shot in enumerate(shots):
        source=folder/'raw'/f'{shot["id"]}.mp4';overlay=folder/'overlays'/f'{shot["id"]}.png';target=edited/source.name;duration=float(shot['seconds'])
        if not source.exists() or not overlay.exists():raise SystemExit(f'Missing capture/title: {shot["id"]}')
        hashes.append(hashlib.sha256(source.read_bytes()).hexdigest())
        if not args.resume or not target.exists():
            filters=f'[1:v]format=rgba,fade=t=in:st=0.35:d=0.65:alpha=1,fade=t=out:st={max(1,duration-1.2)}:d=0.7:alpha=1[title];[0:v][title]overlay=0:0:shortest=1,fade=t=in:st=0:d=0.35,fade=t=out:st={duration-.35}:d=0.35,format=yuv420p[v]'
            run([ffmpeg,'-v','error','-y','-i',source,'-loop','1','-framerate','24','-i',overlay,'-filter_complex',filters,'-map','[v]','-an','-t',duration,'-r','24','-c:v','libx264','-preset','veryfast','-crf','19','-movflags','+faststart',target])
        concat.append("file '"+target.as_posix().replace("'","'\\''")+"'")
        captions.append(f'{index+1}\n{stamp(elapsed+.5)} --> {stamp(elapsed+duration-.5)}\n{shot["title"]}\n{shot["subtitle"]}\n')
        elapsed+=duration;print('EDITED',shot['id'],flush=True)
    if len(set(hashes))!=len(hashes):raise ValueError('Repeated source footage detected')
    (folder/'concat.txt').write_text('\n'.join(concat)+'\n');(folder/'Outpost-Trailer-Captions.srt').write_text('\n'.join(captions))
    soundtrack(folder,shots,ffmpeg)
    output=folder/'Outpost-Extended-Gameplay-Trailer.mp4'
    run([ffmpeg,'-v','error','-y','-f','concat','-safe','0','-i',folder/'concat.txt','-i',folder/'soundtrack.wav','-map','0:v:0','-map','1:a:0','-c:v','copy','-c:a','aac','-b:a','192k','-ar','48000','-af','loudnorm=I=-16:TP=-1.5:LRA=11','-metadata','title=Outpost | Guard the Last Light','-metadata','comment=Actual gameplay from the Outpost development build. Original in-game music and recorded gameplay cues.','-t',elapsed,'-movflags','+faststart',output])
    probe=json.loads(subprocess.check_output([ffprobe,'-v','error','-show_format','-show_streams','-of','json',str(output)]))
    video=next(s for s in probe['streams'] if s['codec_type']=='video');audio=next(s for s in probe['streams'] if s['codec_type']=='audio')
    if abs(float(probe['format']['duration'])-elapsed)>.2 or video['width']!=1920 or video['height']!=1080 or audio['channels']!=2:raise ValueError('Export validation failed')
    if elapsed>=300 and len(shots)==16:print('Minimum five-minute duration verified',flush=True)
    run([ffmpeg,'-v','error','-i',output,'-f','null','-'])
    run([ffmpeg,'-v','error','-y','-ss',3 if elapsed>=300 else 1.5,'-i',output,'-frames:v','1','-q:v','2',folder/'Outpost-Trailer-Poster.jpg'])
    report={'duration_seconds':float(probe['format']['duration']),'resolution':[video['width'],video['height']],'fps':video['avg_frame_rate'],'video_codec':video['codec_name'],'audio_codec':audio['codec_name'],'audio_channels':audio['channels'],'unique_gameplay_shots':len(set(hashes)),'decode_check':'passed','file':str(output),'bytes':output.stat().st_size}
    report['embedded_captions']=False
    report['caption_file']='Outpost-Trailer-Captions.srt'
    (folder/'export-report.json').write_text(json.dumps(report,indent=2)+'\n')
    if elapsed>=300:
        delivery=ROOT/'marketing/exports';delivery.mkdir(parents=True,exist_ok=True)
        for name in ('Outpost-Extended-Gameplay-Trailer.mp4','Outpost-Trailer-Captions.srt','Outpost-Trailer-Poster.jpg','export-report.json'):
            shutil.copy2(folder/name,delivery/name)
        copy=(ROOT/'marketing/README.md').read_text().split('## Rebuild the video')[0]
        (delivery/'Outpost-Upload-Copy.md').write_text(copy)
    print(json.dumps(report,indent=2),flush=True)
if __name__=='__main__':main()
