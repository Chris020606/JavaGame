import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Properties;

class RecoveryWispsTest {
    @Test
    void dropsRewardMovementAndAreNotWastedAtFullResources() {
        RecoveryWisps w = new RecoveryWisps();
        w.enemyDefeated(4, 8, 1, false);
        assertTrue(w.active().isEmpty());
        w.enemyDefeated(4, 8, 5, false);
        assertEquals(1, w.active().size());
        assertFalse(w.collect(4, 8, false));
        assertFalse(w.collect(0, 0, true));
        assertTrue(w.collect(4, 8, true));
        assertFalse(w.collect(4, 8, true));
        w.enemyDefeated(0, 0, 6, true);
        w.update(29);
        assertEquals(1, w.active().size());
        w.update(1);
        assertTrue(w.active().isEmpty());
    }

    @Test
    void savePreservesRemainingLifeAndLegacySavesStartEmpty() {
        Progression progress = new Progression();
        progress.recoveryWisps().enemyDefeated(-5, 9, 3, true);
        progress.recoveryWisps().update(7);
        Properties p = new Properties();
        progress.store(p);
        Progression restored = Progression.restore(p);
        assertEquals(progress.recoveryWisps().active(), restored.recoveryWisps().active());
        restored.recoveryWisps().update(23);
        assertTrue(restored.recoveryWisps().active().isEmpty());
        p.remove("wisps.count");
        assertTrue(Progression.restore(p).recoveryWisps().active().isEmpty());
        p.setProperty("wisps.count", "13");
        assertThrows(IllegalArgumentException.class, () -> Progression.restore(p));
    }

    @Test
    void capAndChapterCleanupKeepWorldBounded() {
        RecoveryWisps w = new RecoveryWisps();
        for (int i = 0; i < 30; i++) w.enemyDefeated(0, 0, i, true);
        assertEquals(12, w.active().size());
        w.clear();
        assertTrue(w.active().isEmpty());
    }
}
