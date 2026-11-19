import static org.junit.jupiter.api.Assertions.*;

import Entities.*;

import org.junit.jupiter.api.Test;

class BossPatternsTest {
    @Test
    void kingHasDistinctSecondPhaseAndReadableWarnings() {
        Enemy king = new Enemy(0, 0, 10, 5, .5f, Difficulty.NORMAL);
        assertEquals(5, BossPatterns.sigils(king, 8, 8, 10).size());
        king.health = king.maxHealth * .4f;
        var fields = BossPatterns.sigils(king, 8, 8, 10);
        assertEquals(7, fields.size());
        for (int i = 0; i < fields.size(); i++) {
            assertTrue(fields.get(i).delay >= .9f);
            if (i > 0) assertTrue(fields.get(i).delay > fields.get(i - 1).delay);
        }
        assertEquals(3, BossPatterns.shockwaves(king, 10).size());
    }

    @Test
    void easyHasFewerSlowerWarningsEvenWhenEnragedAndTargetsStayInArena() {
        Enemy boss = new Enemy(0, 0, 10, 5, .5f, Difficulty.EASY);
        boss.health = 1;
        var fields = BossPatterns.sigils(boss, 22, 22, 10);
        assertEquals(3, fields.size());
        for (var field : fields) {
            assertTrue(Math.abs(field.x) <= 20 && Math.abs(field.z) <= 20);
            assertTrue(field.delay >= 1.15f);
        }
        assertEquals(1, BossPatterns.shockwaves(boss, 10).size());
    }

    @Test
    void wardenLineCanBeEscapedByMovingSidewaysAndWavesAllowSeparateDodges() {
        Enemy boss = new Enemy(0, 0, 5, 5, .5f, Difficulty.NORMAL);
        var fields = BossPatterns.sigils(boss, 8, 0, 5);
        for (var field : fields) assertEquals(0, field.z, .001f);
        boss.health = 1;
        var rings = BossPatterns.shockwaves(boss, 5);
        assertTrue(rings.get(1).delay - rings.get(0).delay >= .8f);
    }

    @Test
    void enragedCrownRetainsAnOpenEscapeSector() {
        Enemy king = new Enemy(0, -8, 10, 5, .5f, Difficulty.NORMAL);
        king.health = 1;
        var fields = BossPatterns.sigils(king, 0, 0, 10);
        double angle = Math.PI / 2 + 6 * Math.PI * 2 / 7;
        float escapeX = (float) Math.cos(angle) * 4.2f;
        float escapeZ = (float) Math.sin(angle) * 4.2f;
        for (var field : fields)
            assertTrue(Math.hypot(escapeX - field.x, escapeZ - field.z) > field.radius);
    }
}
