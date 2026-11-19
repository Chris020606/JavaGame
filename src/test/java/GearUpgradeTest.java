import static org.junit.jupiter.api.Assertions.*;

import Entities.*;

import org.junit.jupiter.api.Test;

class GearUpgradeTest {
    @Test
    void onlyOwnedImprovementsAppearAndDisappearWhenEquipped() {
        Progression p = new Progression();
        assertEquals(0, GearUpgrade.count(p));
        assertFalse(GearUpgrade.isUpgrade(p, 16));
        assertFalse(GearUpgrade.isUpgrade(p, 3));
        Progression geared = Progression.playtest(HeroClass.WARRIOR, Difficulty.NORMAL, false);
        assertEquals(0, GearUpgrade.count(geared));
        geared.equipArmor(ArmorSlot.BOOTS, 0);
        assertTrue(GearUpgrade.isUpgrade(geared, 7));
        assertEquals(3, GearUpgrade.count(geared));
        geared.equipArmor(ArmorSlot.BOOTS, 2);
        assertFalse(GearUpgrade.isUpgrade(geared, 7));
        assertTrue(GearUpgrade.isUpgrade(geared, 15));
        assertEquals(1, GearUpgrade.count(geared));
        geared.equipArmor(ArmorSlot.BOOTS, 3);
        assertEquals(0, GearUpgrade.count(geared));
    }
}
