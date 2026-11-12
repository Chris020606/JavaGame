import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class CityPresentationTest {

    @Test
    public void testDistrictBoundaries() {
        // South Gate
        assertEquals("SOUTHERN EXPEDITION GATE", CityPresentation.districtAt(0f, 26f));
        // Market Square and Riverside
        assertEquals("MARKET SQUARE", CityPresentation.districtAt(0f, 15f));
        assertEquals("RIVERSIDE WORKYARDS", CityPresentation.districtAt(18f, 15f));
        // Castle approach
        assertEquals("CASTLE APPROACH", CityPresentation.districtAt(0f, -29f));
        // Cathedral Quarter
        assertEquals("CATHEDRAL QUARTER", CityPresentation.districtAt(14f, -10f));
        // Archery Green
        assertEquals("ARCHERY GREEN", CityPresentation.districtAt(-7.5f, -5.5f));
        // Ironhand Forge
        assertEquals("IRONHAND FORGE", CityPresentation.districtAt(-7.5f, 1.5f));
        // Arcanist Sanctum
        assertEquals("ARCANIST SANCTUM", CityPresentation.districtAt(7.5f, 1.5f));
        // Central Warden's Avenue
        assertEquals("WARDEN'S AVENUE", CityPresentation.districtAt(0f, 0f));
    }

    @Test
    public void testDistrictTransitionAndDwellClearing() {
        CityPresentation cp = new CityPresentation();

        // Initially in Warden's Avenue
        cp.update(0.1f, 0f, 0f);
        assertEquals("WARDEN'S AVENUE", cp.candidateDistrict());
        assertEquals("", cp.currentDistrict()); // Dwell not yet met (< 0.8s)

        // Advance past 0.8s dwell
        cp.update(0.8f, 0f, 0f);
        assertEquals("WARDEN'S AVENUE", cp.currentDistrict());
        assertTrue(cp.remainingTime() > 3.0f);

        // Step into Market Square
        cp.update(0.1f, 0f, 15f);
        assertEquals("MARKET SQUARE", cp.candidateDistrict());
        // Stale banner is immediately suppressed on crossing into a new district
        assertEquals(0f, cp.remainingTime());

        // Under 0.8s, the new banner is not yet active
        cp.update(0.5f, 0f, 15f);
        assertEquals(0.6f, cp.dwellTime(), 0.01f);
        assertEquals(0f, cp.remainingTime());

        // Once 0.8s dwell is reached, Market Square banner appears for 3.5s
        cp.update(0.3f, 0f, 15f);
        assertEquals("MARKET SQUARE", cp.currentDistrict());
        assertTrue(cp.remainingTime() > 3.0f);
    }
}
