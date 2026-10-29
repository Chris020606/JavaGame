import java.util.HashMap;
import static com.raylib.Raylib.*;

/** Streamed original chapter scores with smooth scene/intensity crossfades. */
public final class GameAudio implements AutoCloseable {
    private final HashMap<String,Sound> sounds=new HashMap<>();
    private final Music[] music=new Music[6];
    private final float[] gains=new float[6];
    private boolean ready,muted;
    private float volume=.6f,duck;
    private int variation,theme=4;
    public GameAudio() {
        InitAudioDevice();ready=IsAudioDeviceReady();
        if(!ready)return;
        for(String name:new String[]{"swing","hit","hurt","kill","wave","upgrade","bow","magic","parry","slam","dash","ward","swing0","swing1","swing2","hit0","hit1","hit2"}) {
            Sound sound=LoadSound(assets.AssetPaths.path("resources/audio/"+name+".wav"));
            if(sound.frameCount()>0) { sounds.put(name,sound);SetSoundVolume(sound,volume*.65f); }
        }
        String[] themes={"meadow","cemetery","frost","necropolis","title","battle"};
        for(int i=0;i<themes.length;i++) {
            Music stream=LoadMusicStream(assets.AssetPaths.path("resources/audio/music-"+themes[i]+".wav"));
            if(stream.frameCount()>0) { music[i]=stream;stream.looping(true);SetMusicVolume(stream,0);PlayMusicStream(stream); }
        }
    }
    public void update(float dt,int chapter,boolean title,boolean battle) {
        theme=title?4:Math.max(0,Math.min(3,chapter));duck=Math.max(0,duck-dt);
        float blend=1-(float)Math.exp(-Math.max(0,dt)*2.4f);
        for(int i=0;i<music.length;i++)if(music[i]!=null) {
            UpdateMusicStream(music[i]);
            float target=i==theme?1:i==5 && battle && !title?.85f:0;
            gains[i]+=(target-gains[i])*blend;
            SetMusicVolume(music[i],muted?0:volume*(i==5?.5f:.48f)*gains[i]*(duck>0?.78f:1));
        }
    }
    public void play(String name) {
        if(muted || !ready)return;
        if(name.equals("swing") || name.equals("hit"))name+=variation++%3;
        Sound sound=sounds.get(name);if(sound!=null) { PlaySound(sound);duck=.13f; }
    }
    public void configure(float volume,boolean mute) {
        this.volume=volume;muted=mute;
        for(Sound sound:sounds.values()) { SetSoundVolume(sound,volume*.65f);if(muted)StopSound(sound); }
        // Apply immediately, including while a menu or paused overlay is open.
        for(int i=0;i<music.length;i++)if(music[i]!=null)SetMusicVolume(music[i],muted?0:volume*(i==5?.5f:.48f)*gains[i]);
    }
    public void toggle() { configure(volume,!muted); }
    public boolean muted() { return muted; }
    public boolean ready() { return ready; }
    int loadedMusic() { int count=0;for(Music track:music)if(track!=null)count++;return count; }
    int theme() { return theme; }
    boolean musicStreaming() { for(Music track:music)if(track!=null && !IsMusicStreamPlaying(track))return false;return loadedMusic()==6; }
    @Override public void close() {
        if(ready) { for(Music track:music)if(track!=null)UnloadMusicStream(track);for(Sound s:sounds.values())UnloadSound(s);CloseAudioDevice();ready=false; }
    }
}
