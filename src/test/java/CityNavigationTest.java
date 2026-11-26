import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CityNavigationTest {
    @Test
    void ledgerOnlyRoutesToCitizensWhoHaveReturned() {
        Progression progress = new Progression();
        CityNavigation navigation = new CityNavigation();
        assertFalse(navigation.visit(CitadelNpc.SURVIVOR_BRAM, progress));
        progress.rescueSurvivor();
        assertTrue(navigation.visit(CitadelNpc.SURVIVOR_BRAM, progress));
        assertFalse(navigation.visit(CitadelNpc.SURVIVOR_MIRA, progress));
        assertEquals(CitadelNpc.SURVIVOR_BRAM.npcName(), navigation.destination(progress).title());
    }

    @Test
    void trackingMerchantChangesOnlyGuidanceAndCanBeCleared() {
        Progression p = new Progression();
        CityNavigation navigation = new CityNavigation();
        int crystals = p.crystals();
        assertTrue(navigation.track(CitadelNpc.CARAVAN_TRADER));
        assertEquals(CitadelNpc.CARAVAN_TRADER.x(), navigation.destination(p).x());
        assertEquals("Potions and caravan upgrades", navigation.destination(p).instruction());
        assertEquals(crystals, p.crystals());
        navigation.clear();
        assertEquals(CitadelHub.PORTAL_Z, navigation.destination(p).z());
    }

    @Test
    void newWardensKeepTheirStarterObjectiveAndSurvivorsAreNotServiceMarkers() {
        Progression p = new Progression(HeroClass.MAGE);
        p.setHasWeapon(false);
        CityNavigation navigation = new CityNavigation();
        navigation.track(CitadelNpc.CARAVAN_TRADER);
        assertEquals(CitadelNpc.ARCANIST.npcName(), navigation.destination(p).title());
        assertFalse(navigation.track(CitadelNpc.SURVIVOR_BRAM));
        assertEquals(CitadelNpc.CARAVAN_TRADER, navigation.tracked());
    }
}
