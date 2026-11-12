import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EquipmentPreviewTest {
    @Test
    void unarmedPreviewReflectsWeaponPowerWithoutGrantingWeapon() {
        Progression p = new Progression();
        p.setHasWeapon(false);
        int preview = p.weaponDamage(0);
        assertFalse(p.hasWeapon());
        assertEquals(5, p.damage());
        p.grantStarterWeapon();
        assertEquals(preview, p.damage());
        assertThrows(IllegalArgumentException.class, () -> p.weaponDamage(4));
    }
}
