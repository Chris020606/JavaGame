import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ImpactDirectionTest {
    @Test
    void contactSparksFollowTheStrikeAcrossEveryFacing() {
        for (int facing = 0; facing < 360; facing += 15) {
            double heading = Math.toRadians(facing);
            float hx = (float) Math.cos(heading) * 4, hz = (float) Math.sin(heading) * 4;
            for (int spark = 0; spark < 24; spark++) {
                double angle = ImpactDirection.angle(0, 0, hx, hz, spark);
                assertTrue(Math.cos(angle) * hx + Math.sin(angle) * hz > 2.4);
            }
        }
    }

    @Test
    void overlappingContactPointsProduceFiniteSparks() {
        for (int i = 0; i < 24; i++) {
            assertTrue(Double.isFinite(ImpactDirection.angle(2, 2, 2, 2, i)));
        }
    }
}
