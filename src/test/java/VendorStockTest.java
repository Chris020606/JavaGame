import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VendorStockTest {
    @Test
    void classMentorsCannotOfferMerchantStockEvenAfterSwitchingTabs() {
        WorkshopScreen shop = new WorkshopScreen();
        for (HeroClass hero : HeroClass.values()) {
            shop.setMentor(hero);
            shop.selectTab(2);
            assertFalse(shop.offersSupplies());
            assertEquals(hero, shop.mentor());
        }
        shop.setMentor(null);
        shop.selectTab(0);
        assertTrue(shop.offersSupplies());
    }

    @Test
    void merchantStandsOutsideBuildingsAndWagonCollision() {
        try (CitadelHub hub = new CitadelHub()) {
            assertTrue(
                    hub.citizenWalkable(
                            CitadelNpc.CARAVAN_TRADER.x(), CitadelNpc.CARAVAN_TRADER.z()));
        }
    }
}
