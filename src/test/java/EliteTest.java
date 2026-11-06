import static org.junit.jupiter.api.Assertions.*;

import Entities.Enemy;

import org.junit.jupiter.api.Test;

class EliteTest {
    @Test
    void eliteStartsAfterOpeningWavesAndNeverReplacesBossRules() {
        assertFalse(new Enemy(0, 0, 1, 2, .95f).elite);
        Enemy elite = new Enemy(0, 0, 6, 2, .95f), regular = new Enemy(0, 0, 6, 2, .5f);
        assertTrue(elite.elite);
        assertEquals(regular.maxHealth * 1.5f, elite.maxHealth, .01f);
        assertEquals(40, elite.reward());
        assertTrue(elite.label().startsWith("ELITE"));
        assertFalse(new Enemy(0, 0, 5, 5, .95f).elite);
    }
}
