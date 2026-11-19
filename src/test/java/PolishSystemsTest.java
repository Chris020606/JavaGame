import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

class PolishSystemsTest {
    @TempDir Path dir;

    @Test
    void oldSettingsKeepTheExistingMixAndNewLevelsRoundTrip() throws Exception {
        Path path = dir.resolve("settings.properties");
        Files.writeString(path, "version=1\nvolume=0.4\nsensitivity=0.2\n");
        Settings settings = new Settings();
        settings.load(path);
        assertEquals(.4f, settings.volume);
        assertEquals(1, settings.musicVolume);
        assertEquals(1, settings.effectsVolume);
        settings.musicVolume = .25f;
        settings.effectsVolume = .8f;
        settings.save(path);
        Settings loaded = new Settings();
        loaded.load(path);
        assertEquals(.25f, loaded.musicVolume);
        assertEquals(.8f, loaded.effectsVolume);
        assertEquals(.4f, loaded.volume);
    }

    @Test
    void invalidMixerLevelsDoNotPartiallyApply() throws Exception {
        Path path = dir.resolve("settings.properties");
        for (String invalid : new String[] {"NaN", "Infinity", "-0.1", "1.1"}) {
            Files.writeString(
                    path, "version=1\nvolume=0.1\nsensitivity=0.2\nmusicVolume=" + invalid + "\n");
            Settings settings = new Settings();
            settings.load(path);
            assertEquals(.6f, settings.volume);
            assertFalse(settings.warning.isEmpty());
        }
    }

    @Test
    void sliderCapturesOnlyItsOwnPressAndClampsOutsideItsTrack() {
        SettingsSlider slider = new SettingsSlider();
        assertEquals(.4f, slider.update(.4f, 150, 110, true, true, 100, 100, 200));
        assertEquals(.4f, slider.update(.4f, 200, 130, false, true, 100, 100, 200));
        assertEquals(.5f, slider.update(.4f, 200, 130, true, true, 100, 100, 200));
        assertEquals(1, slider.update(.5f, 900, 500, false, true, 100, 100, 200));
        assertEquals(0, slider.update(1, 0, 500, false, true, 100, 100, 200));
        assertEquals(.3f, slider.update(.3f, 200, 130, false, false, 100, 100, 200));
    }

    @Test
    void crowdSoundsAreLimitedWithoutSuppressingImportantCues() {
        AudioCueGate gate = new AudioCueGate();
        assertTrue(gate.allow("hit0"));
        assertFalse(gate.allow("hit1"));
        assertTrue(gate.allow("kill"));
        assertFalse(gate.allow("kill"));
        assertTrue(gate.allow("parry"));
        assertTrue(gate.allow("parry"));
        assertTrue(gate.allow("boss-fall"));
        gate.update(.05f);
        assertTrue(gate.allow("hit2"));
        assertFalse(gate.allow("kill"));
        gate.update(.05f);
        assertTrue(gate.allow("kill"));
    }

    @Test
    void pendingRescueNeverShowsAFalseCountdown() {
        var status = EncounterStatus.of(Campaign.CHAPTERS[0], 2, 0, 0, 10, 8, true, false);
        assertTrue(status.detail().contains("EXTRACTION"));
        assertFalse(status.detail().contains("8s"));
        var clear = EncounterStatus.of(Campaign.CHAPTERS[0], 2, 0, 0, 10, 7.1f, false, false);
        assertTrue(clear.detail().contains("8s"));
        assertEquals(1, clear.clearedFraction());
    }

    @Test
    void queuedEnemiesAreNotCountedAsDefeatedAndPreparationIsNotWaveZero() {
        var active = EncounterStatus.of(Campaign.CHAPTERS[0], 1, 3, 2, 10, 0, false, false);
        assertEquals(.5f, active.clearedFraction());
        assertTrue(active.detail().contains("3 INCOMING"));
        var start = EncounterStatus.of(Campaign.CHAPTERS[0], 0, 0, 0, 0, 5, false, false);
        assertEquals("PREPARE FOR BATTLE", start.title());
        var boss = EncounterStatus.of(Campaign.CHAPTERS[1], 4, 0, 0, 0, 5, false, false);
        assertEquals("BOSS APPROACHING", boss.title());
        var done = EncounterStatus.of(Campaign.CHAPTERS[3], 10, 0, 0, 1, 0, false, true);
        assertEquals("CHAPTER SECURED", done.title());
    }

    @Test
    void citizenTurnsTakeTheShortArcAndStallsCannotSnapThePose() {
        assertEquals(181, NpcFacing.approach(179, -179, .025f), .001f);
        assertEquals(-181, NpcFacing.approach(-179, 179, .025f), .001f);
        assertEquals(8, NpcFacing.approach(0, 180, 5));
        assertEquals(12, NpcFacing.approach(12, 170, 0));
        assertEquals(12, NpcFacing.approach(12, 170, Float.NaN));
    }
}
