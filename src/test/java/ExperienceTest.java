import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

class ExperienceTest {
    @TempDir Path directory;

    @Test
    void actualKillRewardsReachLevelTwoOnFourthKill() {
        Progression p = new Progression();
        for (int i = 1; i <= 3; i++) {
            p.rewardEnemy(false);
            assertEquals(i * 25, p.xp());
            assertEquals(1, p.characterLevel());
        }
        p.damagePlayer(20);
        p.rewardEnemy(false);
        assertEquals(2, p.characterLevel());
        assertEquals(0, p.xp());
        assertEquals(1, p.levelPoints());
        assertEquals(p.maxHealth(), p.health());
        assertEquals(35, p.damage());
    }

    @Test
    void eliteAndBossAwardsCarryRemainderAcrossMultipleLevels() {
        Progression p = new Progression();
        p.rewardEnemy(false);
        p.rewardElite();
        assertEquals(85, p.xp());
        p.rewardEnemy(true);
        assertEquals(3, p.characterLevel());
        assertEquals(110, p.xp());
        assertEquals(2, p.levelPoints());
    }

    @Test
    void earnedExperienceAndSpentPointsSurviveDiskSave() throws Exception {
        Progression p = new Progression();
        p.rewardEnemy(true);
        assertTrue(p.purchase(Progression.Upgrade.POWER));
        Path file = directory.resolve("xp.properties");
        SaveGame.write(
                file,
                new SaveGame.Snapshot(
                        "XP", 0, 8, 0, p, new WaveDirector.State(1, 0, 0, 8, 0, 21, 0), List.of()));
        Progression loaded = SaveGame.read(file).progression();
        assertEquals(3, loaded.characterLevel());
        assertEquals(25, loaded.xp());
        assertEquals(1, loaded.levelPoints());
        assertEquals(2, loaded.level(Progression.Upgrade.POWER));
        assertEquals(120, loaded.crystals());
    }

    @Test
    void resilienceDoesNotAdvertiseAnUnspendableAttributePoint() {
        Progression p = new Progression();
        p.addXp(100);
        assertFalse(p.usesLevelPoint(Progression.Upgrade.ARMOR));
        assertFalse(p.canPurchase(Progression.Upgrade.ARMOR));
        assertFalse(p.purchase(Progression.Upgrade.ARMOR));
        assertEquals(1, p.levelPoints());
        assertTrue(p.canPurchase(Progression.Upgrade.POWER));
        assertTrue(p.purchase(Progression.Upgrade.POWER));
        assertEquals(0, p.levelPoints());
    }

    @Test
    void hugeAwardStopsAtSaveableLevelCap() {
        Progression p = new Progression();
        assertTrue(p.addXp(Integer.MAX_VALUE));
        assertEquals(1000, p.characterLevel());
        assertEquals(0, p.xp());
        assertFalse(p.addXp(25));
        java.util.Properties properties = new java.util.Properties();
        p.store(properties);
        assertEquals(1000, Progression.restore(properties).characterLevel());
    }
}
