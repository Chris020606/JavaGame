import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RadarPointTest {
    @Test
    void cornersStayInsideCircleAndKeepTheirDirection() {
        for (int x : new int[] {-22, 0, 22})
            for (int z : new int[] {-22, 0, 22}) {
                var p = RadarPoint.of(x, z, 2.5f, 51);
                assertTrue(Math.hypot(p.x(), p.y()) <= 51.001);
                if (x != 0) assertEquals(Math.signum(x), Math.signum(p.x()));
                if (z != 0) assertEquals(Math.signum(z), Math.signum(p.y()));
            }
        assertEquals(new RadarPoint(5, 10), RadarPoint.of(2, 4, 2.5f, 51));
    }

    @Test
    void invalidProjectionCannotEscapeIntoTheHud() {
        assertEquals(new RadarPoint(0, 0), RadarPoint.of(Float.NaN, 2, 2.5f, 51));
        assertEquals(new RadarPoint(0, 0), RadarPoint.of(2, Float.POSITIVE_INFINITY, 2.5f, 51));
    }
}
