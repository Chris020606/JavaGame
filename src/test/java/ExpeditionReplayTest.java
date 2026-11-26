import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

class ExpeditionReplayTest {
    @Test
    void replayRewardsCanBeSavedAfterEveryBlessingIsFunded(@TempDir Path directory)
            throws Exception {
        Progression progress = new Progression();
        for (int i = 0; i < 40; i++) progress.rewardObjective();
        Path file = directory.resolve("save.properties");
        SaveGame.write(
                file,
                new SaveGame.Snapshot(
                        "Wayfarer",
                        0,
                        4,
                        0,
                        progress,
                        new WaveDirector.State(1, 0, 0, 5, 0, 21, 0),
                        List.of()));
        // Previous builds could write these totals and then refuse to load the profile.
        Properties legacy = AppData.read(file);
        legacy.setProperty("expedition.embers", "40");
        legacy.setProperty("rescuedSurvivors", "9");
        AppData.write(file, legacy);
        var loaded = SaveGame.read(file);
        var expedition = loaded.progression().expedition();
        assertEquals(6, loaded.progression().rescuedSurvivors());
        assertEquals(40, expedition.objectives());
        assertEquals(18, expedition.embers());
        while (expedition.embers() > 0)
            assertTrue(expedition.choose(expedition.offers().getFirst()));
        loaded.progression().rewardObjective();
        assertFalse(expedition.waveCleared(4));
        assertEquals(0, expedition.embers());
        SaveGame.write(file, loaded);
        Expedition restored = SaveGame.read(file).progression().expedition();
        assertEquals(41, restored.objectives());
        assertEquals(18, restored.choices());
    }

    @Test
    void returningToRescueWavesAfterEveryoneIsHomeProvidesSupplies() {
        Progression p = new Progression();
        for (CitadelNpc citizen : CitadelNpc.RESCUED_SURVIVORS) {
            assertEquals(citizen.npcName(), p.survivorName());
            p.rescueSurvivor();
        }
        p.rescueSurvivor();
        assertEquals(CitadelNpc.RESCUED_SURVIVORS.length, p.rescuedSurvivors());
        for (int wave : new int[] {2, 4, 6, 7, 8, 9}) {
            p.fieldEvent().begin(wave, false);
            assertEquals(FieldEvent.Kind.SUPPLIES, p.fieldEvent().kind());
            assertEquals(FieldEvent.Status.WAITING, p.fieldEvent().status());
        }
        p.fieldEvent().begin(10, false);
        assertFalse(p.fieldEvent().visible());
    }
}
