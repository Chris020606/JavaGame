import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TitleSkyTest {
    @Test
    void clockMovesAndWrapsWithoutChangingOnLargeFrameStalls() {
        TitleSky sky = new TitleSky();
        sky.phase(.99999);
        sky.update(5, 0, 0, false, false, 1000, 680);
        assertEquals((.99999 + .1 / TitleSky.CYCLE_SECONDS) % 1, sky.phase(), 1e-8);
        assertFalse(sky.dragging());
    }

    @Test
    void sunAndMoonAlternateAndLightFollowsTheSky() {
        TitleSky sky = new TitleSky();
        sky.phase(.25);
        assertEquals(1, sky.daylight());
        assertTrue(sky.body(0, 1000, 680).visible());
        assertFalse(sky.body(1, 1000, 680).visible());
        sky.phase(.75);
        assertEquals(0, sky.daylight());
        assertTrue(sky.body(1, 1000, 680).visible());
        assertFalse(sky.body(0, 1000, 680).visible());
    }

    @Test
    void eitherBodyCanBeDraggedAcrossTheHorizonAndResumesOnRelease() {
        for (int body = 0; body < 2; body++) {
            TitleSky sky = new TitleSky();
            double start = body == 0 ? .25 : .75;
            sky.phase(start);
            TitleSky.Body b = sky.body(body, 1280, 800);
            sky.update(0, b.x(), b.y(), true, true, 1280, 800);
            assertTrue(sky.dragging());
            assertEquals(start, sky.phase(), 1e-6, "Grabbing must not jump the clock");
            // Pull below the horizon: the grabbed body's opposite takes over the visible sky.
            sky.update(0, 1280 * .76f, 800 * .76f, false, true, 1280, 800);
            assertEquals((start + .5) % 1, sky.phase(), 1e-6);
            double released = sky.phase();
            sky.update(.1f, 0, 0, false, false, 1280, 800);
            assertFalse(sky.dragging());
            assertTrue(sky.phase() > released);
        }
    }

    @Test
    void clicksElsewhereDoNotScrubAndCentreDoesNotSnap() {
        TitleSky sky = new TitleSky();
        sky.update(0, 100, 400, true, true, 1000, 680);
        assertFalse(sky.dragging());
        var moon = sky.body(1, 1000, 680);
        sky.update(0, moon.x(), moon.y(), true, true, 1000, 680);
        double phase = sky.phase();
        sky.update(0, 760, 680 * .42f, false, true, 1000, 680);
        assertEquals(phase, sky.phase());
    }
}
