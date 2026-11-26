import static org.junit.jupiter.api.Assertions.*;

import Entities.*;

import org.junit.jupiter.api.Test;

class BossEnduranceTest {
    @Test
    void enduranceExtendsBothFightsWithASturdierFinalBoss() {
        for (Difficulty difficulty : Difficulty.values()) {
            Enemy warden = new Enemy(0, 0, 5, 5, .5f, difficulty);
            Enemy king = new Enemy(0, 0, 10, 5, .5f, difficulty);
            assertTrue(warden.maxHealth >= 450 * difficulty.health * 2.4f);
            assertTrue(
                    king.maxHealth / (700 * difficulty.health)
                            > warden.maxHealth / (450 * difficulty.health));
            assertEquals(40 + 50 + 8, new Enemy(0, 0, 5, 0, .5f, Difficulty.NORMAL).maxHealth);
        }
    }

    @Test
    void pressureIncreasesWithoutShorteningWarningsOrRemovingDamageWindows() {
        Enemy boss = new Enemy(0, 0, 5, 5, .5f);
        boss.beginAttack(0, 2, true);
        assertTrue(boss.cooldown - boss.attackDuration() >= .8f);
        float committed = boss.attackDuration();
        boss.hit(boss.maxHealth * .6f, 0, 2, 0);
        assertEquals(committed, boss.attackDuration());
        assertEquals(.65f, BossBalance.recovery(Difficulty.EASY, true, true));
        assertTrue(BossBalance.recovery(Difficulty.NORMAL, true, true) < .48f);
        assertEquals(120, boss.reward());
    }

    @Test
    void oldAndCurrentBossSavesPreserveHealthPercentage(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        var boss = new Enemy(0, 2, 5, 5, .5f);
        boss.health = boss.maxHealth * .25f;
        var file = dir.resolve("boss.properties");
        SaveGame.write(
                file,
                new SaveGame.Snapshot(
                        "Warden",
                        0,
                        4,
                        0,
                        new Progression(),
                        new WaveDirector.State(5, 0, 1, 8, 0, 21, 0),
                        java.util.List.of(boss),
                        Arena.HIGHLANDS));
        assertEquals(.25f, SaveGame.read(file).enemies().getFirst().health / boss.maxHealth, .001f);
        var legacy = AppData.read(file);
        legacy.remove("enemy.0.maxHealth");
        legacy.setProperty("enemy.0.health", "225");
        AppData.write(file, legacy);
        assertEquals(.5f, SaveGame.read(file).enemies().getFirst().health / boss.maxHealth, .001f);
    }
}
