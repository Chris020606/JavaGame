import static com.raylib.Raylib.*;

import java.util.HashMap;

/** Streamed original chapter scores with smooth scene/intensity crossfades. */
public final class GameAudio implements AutoCloseable {
    private final HashMap<String, Sound> sounds = new HashMap<>();
    private final Music[] music = new Music[8];
    private final float[] gains = new float[8];
    private boolean ready, muted;
    private float volume = .6f, duck, enrageIntensity;
    private int variation, theme = 4;
    private java.util.function.Consumer<String> cueObserver;

    void observeCues(java.util.function.Consumer<String> observer) {
        cueObserver = observer;
    }

    public GameAudio() {
        InitAudioDevice();
        ready = IsAudioDeviceReady();
        if (!ready) return;
        for (String name :
                new String[] {
                    "swing",
                    "hit",
                    "hurt",
                    "kill",
                    "wave",
                    "upgrade",
                    "bow",
                    "magic",
                    "parry",
                    "slam",
                    "dash",
                    "ward",
                    "victory",
                    "boss-fall",
                    "swing0",
                    "swing1",
                    "swing2",
                    "hit0",
                    "hit1",
                    "hit2"
                }) {
            Sound sound = LoadSound(assets.AssetPaths.path("resources/audio/" + name + ".wav"));
            if (sound.frameCount() > 0) {
                sounds.put(name, sound);
                SetSoundVolume(sound, volume * .65f);
            }
        }
        String[] themes = {
            "meadow", "cemetery", "frost", "necropolis", "title", "battle", "boss", "enrage"
        };
        for (int i = 0; i < themes.length; i++) {
            Music stream =
                    LoadMusicStream(
                            assets.AssetPaths.path(
                                    "resources/audio/music-"
                                            + themes[i]
                                            + (i == 0 || i == 5 || i == 6 ? ".ogg" : ".wav")));
            if (stream.frameCount() > 0) {
                music[i] = stream;
                stream.looping(true);
                SetMusicVolume(stream, 0);
                PlayMusicStream(stream);
            }
        }
    }

    public void update(
            float dt, int chapter, boolean title, boolean battle, boolean boss, boolean enraged) {
        theme = title ? 4 : boss ? 6 : Math.max(0, Math.min(3, chapter));
        duck = Math.max(0, duck - dt);
        float blend = 1 - (float) Math.exp(-Math.max(0, dt) * 2.4f);
        enrageIntensity += ((boss && enraged && !title ? 1 : 0) - enrageIntensity) * blend;
        for (int i = 0; i < music.length; i++)
            if (music[i] != null) {
                if (i == 6) SetMusicPitch(music[i], 1 + enrageIntensity * .035f);
                UpdateMusicStream(music[i]);
                float target =
                        i == theme
                                ? (battle && !boss && !title ? 0 : 1)
                                : i == 7 && boss && enraged && !title
                                        ? 0
                                        : i == 5 && battle && !boss && !title ? 1 : 0;
                gains[i] += (target - gains[i]) * blend;
                SetMusicVolume(
                        music[i],
                        muted
                                ? 0
                                : volume
                                        * (i == 7 ? .34f : i == 5 ? .72f : .65f)
                                        * gains[i]
                                        * (duck > 0 ? .78f : 1));
            }
    }

    public void play(String name) {
        if (name.equals("swing") || name.equals("hit")) name += variation++ % 3;
        if (cueObserver != null) cueObserver.accept(name);
        if (muted || !ready) return;
        Sound sound = sounds.get(name);
        if (sound != null) {
            if (name.equals("bow") || name.equals("magic"))
                SetSoundPitch(sound, .96f + (variation++ % 5) * .02f);
            PlaySound(sound);
            if (name.startsWith("hit") || name.equals("slam") || name.equals("parry")) duck = .10f;
        }
    }

    public void configure(float volume, boolean mute) {
        this.volume = volume;
        muted = mute;
        for (Sound sound : sounds.values()) {
            SetSoundVolume(sound, volume * .65f);
            if (muted) StopSound(sound);
        }
        // Apply immediately, including while a menu or paused overlay is open.
        for (int i = 0; i < music.length; i++)
            if (music[i] != null)
                SetMusicVolume(
                        music[i],
                        muted ? 0 : volume * (i == 7 ? .34f : i == 5 ? .72f : .65f) * gains[i]);
    }

    public void toggle() {
        configure(volume, !muted);
    }

    public boolean muted() {
        return muted;
    }

    public boolean ready() {
        return ready;
    }

    int loadedMusic() {
        int count = 0;
        for (Music track : music) if (track != null) count++;
        return count;
    }

    int theme() {
        return theme;
    }

    float enrageGain() {
        return enrageIntensity;
    }

    boolean musicStreaming() {
        for (Music track : music) if (track != null && !IsMusicStreamPlaying(track)) return false;
        return loadedMusic() == music.length;
    }

    @Override
    public void close() {
        if (ready) {
            for (Music track : music) if (track != null) UnloadMusicStream(track);
            for (Sound s : sounds.values()) UnloadSound(s);
            CloseAudioDevice();
            ready = false;
        }
    }
}
