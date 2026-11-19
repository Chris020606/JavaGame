import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ClassResourceTest {
    @Test
    void mageTrainingAndResourceUseManaNames() {
        Progression mage = new Progression(HeroClass.MAGE);
        assertEquals("MANA", mage.resourceLabel());
        assertTrue(mage.upgradeEffect(Progression.Upgrade.ENDURANCE).contains("mana"));
        assertTrue(mage.upgradeEffect(Progression.Upgrade.RECOVERY).contains("mana"));
        assertEquals("STAMINA", new Progression(HeroClass.ARCHER).resourceLabel());
        Progression archer = new Progression(HeroClass.ARCHER);
        assertNotEquals(archer.weaponName(2), archer.weaponName(3));
    }
}
