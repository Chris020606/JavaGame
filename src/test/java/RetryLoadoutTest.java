import static org.junit.jupiter.api.Assertions.*;

import Entities.ArmorSlot;

import org.junit.jupiter.api.Test;

import java.util.Properties;

class RetryLoadoutTest {
    private Progression funded() {
        Properties p = new Properties();
        new Progression().store(p);
        p.setProperty("characterLevel", "10");
        p.setProperty("crystals", "10000");
        return Progression.restore(p);
    }

    @Test
    void retryPreservesFinalSwordAndHelmetWithSecondTierOtherPieces() {
        Progression entry = funded();
        entry.buyWeapon(2);
        entry.equipWeapon(2);
        for (ArmorSlot slot : ArmorSlot.values()) {
            entry.buyArmor(slot, 2);
            entry.equipArmor(slot, 2);
        }
        Properties saved = new Properties();
        entry.store(saved);
        Progression current = Progression.restore(saved);
        current.buyWeapon(3);
        current.equipWeapon(3);
        current.buyArmor(ArmorSlot.HEAD, 3);
        current.equipArmor(ArmorSlot.HEAD, 3);
        var retry = RetryLoadout.preserve(entry, current);
        assertEquals(3, retry.weaponTier());
        for (ArmorSlot slot : ArmorSlot.values())
            assertEquals(slot == ArmorSlot.HEAD ? 3 : 2, retry.armorTier(slot));
        assertTrue(retry.ownsWeapon(3));
        assertTrue(retry.ownsArmor(ArmorSlot.HEAD, 3));
        assertFalse(retry.ownsArmor(ArmorSlot.CHEST, 3));
        assertEquals(current.crystals(), retry.crystals());
        assertEquals(2, entry.weaponTier());
        assertEquals(2, entry.armorTier(ArmorSlot.HEAD));
    }

    @Test
    void retryKeepsIntentionalLowerTierSelectionAndEmptySlots() {
        Progression entry = funded();
        entry.buyWeapon(3);
        entry.equipWeapon(3);
        Properties saved = new Properties();
        entry.store(saved);
        Progression current = Progression.restore(saved);
        current.grantStarterWeapon();
        current.equipWeapon(0);
        current.equipArmor(ArmorSlot.HEAD, 0);
        var retry = RetryLoadout.preserve(entry, current);
        assertEquals(current.weaponTier(), retry.weaponTier());
        assertEquals(current.armorTier(ArmorSlot.HEAD), retry.armorTier(ArmorSlot.HEAD));
        assertTrue(retry.ownsWeapon(3));
    }
}
