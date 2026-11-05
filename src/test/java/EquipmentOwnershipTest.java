import Entities.ArmorSlot;
import org.junit.jupiter.api.Test;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;
class EquipmentOwnershipTest {
    private Progression funded() { Properties p=new Properties();new Progression().store(p);p.setProperty("crystals","10000");return Progression.restore(p); }
    @Test void craftingTopTierNeverGrantsOrEquipsLowerTiers() {
        Progression p=funded();assertTrue(p.buyArmor(3));assertTrue(p.buyWeapon(3));
        for(int tier=1;tier<=2;tier++) { assertFalse(p.ownsArmor(tier));assertFalse(p.ownsWeapon(tier));assertFalse(p.equipArmor(ArmorSlot.CHEST,tier));assertFalse(p.equipWeapon(tier)); }
        assertTrue(p.equipArmor(ArmorSlot.CHEST,3));assertTrue(p.equipWeapon(3));
        assertTrue(p.buyArmor(1));assertTrue(p.buyWeapon(1));assertTrue(p.ownsArmor(1));assertFalse(p.ownsArmor(2));assertFalse(p.ownsWeapon(2));
        int before=p.crystals();assertFalse(p.buyArmor(3));assertFalse(p.buyWeapon(3));assertEquals(before,p.crystals());
    }
    @Test void gapsPersistAndBossGrantsOnlyOneMissingWeapon() {
        Progression p=funded();p.buyWeapon(3);p.buyArmor(3);p.rewardEnemy(true);
        assertEquals(1,p.lastBossWeapon());assertTrue(p.ownsWeapon(1));assertFalse(p.ownsWeapon(2));assertTrue(p.ownsWeapon(3));
        Properties saved=new Properties();p.store(saved);Progression loaded=Progression.restore(saved);
        assertTrue(loaded.ownsWeapon(1));assertFalse(loaded.ownsWeapon(2));assertTrue(loaded.ownsWeapon(3));assertFalse(loaded.ownsArmor(1));
        saved.setProperty("weaponTier","2");assertThrows(IllegalArgumentException.class,()->Progression.restore(saved));
    }
    @Test void legacySaveKeepsPreviouslyAvailableGearWithoutLosingPurchasedItems() {
        Progression p=funded();p.buyArmor(3);p.buyWeapon(3);p.equipWeapon(3);Properties saved=new Properties();p.store(saved);
        saved.remove("ownedArmor");saved.remove("ownedWeapons");saved.setProperty("equipped.HEAD","1");
        Progression migrated=Progression.restore(saved);assertTrue(migrated.ownsArmor(1));assertTrue(migrated.ownsArmor(3));assertTrue(migrated.ownsArmor(2));assertTrue(migrated.ownsWeapon(1));assertTrue(migrated.ownsWeapon(3));
    }
}
