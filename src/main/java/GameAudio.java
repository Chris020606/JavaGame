import java.util.HashMap;
import static com.raylib.Raylib.*;

/** Optional device: gameplay remains available when the machine has no audio output. */
public final class GameAudio implements AutoCloseable {
    private final HashMap<String,Sound> sounds=new HashMap<>();
    private boolean ready, muted;
    public GameAudio() {
        InitAudioDevice(); ready=IsAudioDeviceReady();
        if (ready) for (String name:new String[]{"swing","hit","hurt","kill","wave","upgrade"}) {
            Sound sound=LoadSound(assets.AssetPaths.path("resources/audio/"+name+".wav"));
            if (sound.frameCount()>0) { sounds.put(name,sound); SetSoundVolume(sound,.45f); }
        }
    }
    public void play(String name) { if (!muted && sounds.containsKey(name)) PlaySound(sounds.get(name)); }
    public void configure(float volume,boolean mute) { muted=mute; for (Sound sound:sounds.values()) { SetSoundVolume(sound,volume); if (muted) StopSound(sound); } }
    public void toggle() { muted=!muted; if (muted) for (Sound s:sounds.values()) StopSound(s); }
    public boolean muted() { return muted; }
    public boolean ready() { return ready; }
    @Override public void close() {
        if (ready) { for (Sound s:sounds.values()) UnloadSound(s); CloseAudioDevice(); ready=false; }
    }
}
