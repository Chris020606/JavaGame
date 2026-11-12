import static org.junit.jupiter.api.Assertions.*;

import Entities.ArmorSlot;

import org.junit.jupiter.api.Test;

import java.util.Properties;

class ArmorPiecesTest {
    @Test
    void buyingBootsDoesNotGrantChestOrOtherPiecesAndSurvivesSave() {
        Progression p = new Progression();
        p.addXp(300);
        Properties props = new Properties();
        p.store(props);
        props.setProperty("crystals", "1000");
        p = Progression.restore(props);
        int before = p.crystals();
        assertTrue(p.buyArmor(ArmorSlot.BOOTS, 1));
        assertEquals(before - p.armorPrice(ArmorSlot.BOOTS, 1), p.crystals());
        assertFalse(p.buyArmor(ArmorSlot.BOOTS, 1));
        assertFalse(p.equipArmor(ArmorSlot.CHEST, 1));
        assertTrue(p.equipArmor(ArmorSlot.BOOTS, 1));
        p.store(props);
        Progression copy = Progression.restore(props);
        assertTrue(copy.ownsArmor(ArmorSlot.BOOTS, 1));
        assertFalse(copy.ownsArmor(ArmorSlot.CHEST, 1));
        assertEquals(1, copy.armorTier(ArmorSlot.BOOTS));
    }

    @Test
    void legacySetOwnershipPreservesAllPieces() {
        Progression p = new Progression();
        Properties props = new Properties();
        p.store(props);
        props.setProperty("crystals", "1000");
        p = Progression.restore(props);
        assertTrue(p.buyArmor(1));
        p.store(props);
        for (ArmorSlot slot : ArmorSlot.values()) props.remove("ownedArmor." + slot);
        Progression copy = Progression.restore(props);
        for (ArmorSlot slot : ArmorSlot.values()) assertTrue(copy.ownsArmor(slot, 1));
    }
}
