import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Properties;

class MerchantReminderTest {
    @Test
    void reminderNeverBlocksBattleAndExpiresDuringRest() {
        BlessingReminder reminder = new BlessingReminder();
        reminder.update(.1f, false, 1);
        assertFalse(reminder.visible());
        reminder.update(.1f, true, 1);
        assertTrue(reminder.visible());
        reminder.update(5, true, 1);
        assertFalse(reminder.visible());
        reminder.update(.1f, true, 1);
        assertFalse(reminder.visible());
        reminder.update(.1f, false, 1);
        reminder.update(.1f, true, 1);
        assertTrue(reminder.visible());
        reminder.update(.1f, false, 1);
        assertFalse(reminder.visible());
    }

    @Test
    void purchasesConsumeCrystalsScaleCapAndPersist() {
        Progression p = new Progression();
        assertFalse(p.buyCaravan(0));
        Properties props = new Properties();
        p.store(props);
        props.setProperty("crystals", "100000");
        p = Progression.restore(props);
        int hp = p.maxHealth(), stamina = p.maxStamina(), recovery = p.staminaRecovery();
        for (int item = 0; item < 3; item++) {
            int prior = p.caravanPrice(item);
            for (int rank = 0; rank < 5; rank++) assertTrue(p.buyCaravan(item));
            assertFalse(p.buyCaravan(item));
            assertTrue(p.caravanPrice(item) > prior);
        }
        assertEquals(hp + 25, p.maxHealth());
        assertEquals(stamina + 25, p.maxStamina());
        assertEquals(recovery + 5, p.staminaRecovery());
        p.heal(100);
        p.store(props);
        Progression loaded = Progression.restore(props);
        assertEquals(p.crystals(), loaded.crystals());
        assertEquals(p.maxHealth(), loaded.maxHealth());
        assertEquals(p.health(), loaded.health());
        assertEquals(5, loaded.caravanRank(2));
    }
}
