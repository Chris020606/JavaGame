import static org.junit.jupiter.api.Assertions.*;

import Entities.*;

import org.junit.jupiter.api.Test;

class LateGameBalanceTest {
    @Test
    void easyLateEnemiesSurviveALegendaryBasicHitWithoutBecomingDamageSponges() {
        Progression p = Progression.playtest(HeroClass.WARRIOR, Difficulty.EASY, false);
        while (p.characterLevel() < 12) p.addXp(p.xpToNextLevel());
        Enemy late = new Enemy(10, 10, 9, 0, .5f, Difficulty.EASY);
        assertTrue(late.maxHealth > p.damage());
        assertTrue(late.maxHealth / p.damage() < 3);
        int damage = p.damage(), health = p.maxHealth();
        p.addXp(p.xpToNextLevel());
        assertEquals(3, p.damage() - damage);
        assertEquals(10, p.maxHealth() - health);
    }

    @Test
    void olderHighHealthSaveLoadsAtNewHealthCap() {
        Progression p = new Progression();
        while (p.characterLevel() < 12) p.addXp(p.xpToNextLevel());
        java.util.Properties props = new java.util.Properties();
        p.store(props);
        props.setProperty("health", "375");
        Progression loaded = Progression.restore(props);
        assertEquals(p.characterLevel(), loaded.characterLevel());
        assertEquals(loaded.maxHealth(), loaded.health());
    }
}
