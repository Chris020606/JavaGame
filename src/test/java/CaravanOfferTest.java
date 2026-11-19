import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Properties;

class CaravanOfferTest {
    @Test
    void explainsCostAndActualClassResourceWithoutSpendingCurrency() {
        Progression p = new Progression(HeroClass.MAGE);
        assertTrue(CaravanOffer.forItem(p, 0).action().contains("180"));
        assertTrue(CaravanOffer.forItem(p, 1).preview().contains("MANA"));
        assertTrue(CaravanOffer.forItem(p, 1).preview().contains("100 > 105"));
        assertEquals(0, p.crystals());
        assertEquals(0, p.caravanRank(1));
    }

    @Test
    void previewsMatchPurchasedBonusAndStopAtCap() {
        Properties properties = new Properties();
        new Progression().store(properties);
        properties.setProperty("crystals", "10000");
        Progression p = Progression.restore(properties);
        int before = p.staminaRecovery();
        assertTrue(CaravanOffer.forItem(p, 2).preview().contains(before + " > " + (before + 1)));
        for (int rank = 0; rank < 5; rank++) assertTrue(p.buyCaravan(2));
        CaravanOffer complete = CaravanOffer.forItem(p, 2);
        assertEquals("ALL RANKS OWNED", complete.action());
        assertTrue(complete.preview().contains(p.staminaRecovery() + " > " + p.staminaRecovery()));
    }
}
