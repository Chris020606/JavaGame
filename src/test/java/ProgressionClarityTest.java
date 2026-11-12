import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProgressionClarityTest {
    @Test
    void previewMatchesActualNextLevelForAllClassesAndWeaponStates() {
        for (HeroClass hero : HeroClass.values()) {
            for (boolean armed : new boolean[] {false, true}) {
                Progression p = new Progression(hero);
                p.setHasWeapon(armed);
                int health = p.nextLevelHealth(), damage = p.nextLevelDamage();
                p.addXp(p.xpToNextLevel());
                assertEquals(health, p.maxHealth());
                assertEquals(damage, p.damage());
            }
        }
    }

    @Test
    void campaignOffersSixRescuesAndNoBossEscort() {
        int rescues = 0;
        for (int wave = 1; wave <= 10; wave++) {
            FieldEvent event = new FieldEvent();
            event.begin(wave);
            if (Campaign.chapter(wave).bossOnly()) assertFalse(event.visible());
            else if (event.kind() == FieldEvent.Kind.SURVIVOR) rescues++;
        }
        assertEquals(6, rescues);
        assertEquals(rescues, CitadelNpc.RESCUED_SURVIVORS.length);
        assertEquals(6, CitadelNpc.rescuedNpcs(6).length);
    }
}
