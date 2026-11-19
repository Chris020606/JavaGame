import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.List;

class SurvivorEscortTest {
    @Test
    void survivorFollowTetherAndCatchUpSpeed() {
        FieldEvent event = new FieldEvent();
        event.begin(2);
        // Activate survivor by interacting close by
        event.update(0.1f, event.x(), event.z(), true, 0);
        assertTrue(event.vulnerable());
        assertEquals(FieldEvent.Status.ACTIVE, event.status());

        // Player at 14m distance (beyond old 11m limit, inside new 16m limit)
        float startX = event.x();
        float startZ = event.z();
        float playerX = startX + 10f;
        float playerZ = startZ + 10f; // hypot is ~14.14m
        float d = (float) Math.hypot(playerX - startX, playerZ - startZ);
        assertTrue(d > 11f && d < 16f, "Distance should be between 11m and 16m");

        event.update(0.5f, playerX, playerZ, false, 0);
        float movedD = (float) Math.hypot(event.x() - startX, event.z() - startZ);
        assertTrue(
                movedD > 1.0f, "Survivor should follow when player is within extended 16m tether");

        // When player is far (> 6m), catch-up speed is higher (3.8m/s vs 2.9m/s)
        FieldEvent closeEvent = new FieldEvent();
        closeEvent.begin(2);
        closeEvent.update(0.1f, closeEvent.x(), closeEvent.z(), true, 0);
        float cStartX = closeEvent.x();
        float cStartZ = closeEvent.z();
        // 4m distance (between 2 and 6)
        closeEvent.update(0.5f, cStartX + 4f, cStartZ, false, 0);
        float closeStep = closeEvent.x() - cStartX;

        FieldEvent farEvent = new FieldEvent();
        farEvent.begin(2);
        farEvent.update(0.1f, farEvent.x(), farEvent.z(), true, 0);
        float fStartX = farEvent.x();
        float fStartZ = farEvent.z();
        // 10m distance (> 6m)
        farEvent.update(0.5f, fStartX + 10f, fStartZ, false, 0);
        float farStep = farEvent.x() - fStartX;

        assertTrue(
                farStep > closeStep,
                "Catch-up speed when far ("
                        + farStep
                        + ") should exceed close speed ("
                        + closeStep
                        + ")");
    }

    @Test
    void survivorNavigatesAroundObstaclesWithoutPenetration() {
        FieldEvent event = new FieldEvent();
        event.begin(2);
        event.update(0.1f, event.x(), event.z(), true, 0);

        List<ArenaNavigation.Obstacle> obstacles = ArenaNavigation.obstacles(0);
        assertFalse(obstacles.isEmpty(), "Chapter 0 must contain terrain obstacles");

        ArenaNavigation.Obstacle obs = obstacles.get(0);
        // Position player on the opposite side of the obstacle
        float playerX = obs.x() + 3.0f;
        float playerZ = obs.z();

        for (int frame = 0; frame < 60; frame++) {
            event.update(0.05f, playerX, playerZ, false, 0);
            float distToObs = (float) Math.hypot(event.x() - obs.x(), event.z() - obs.z());
            assertTrue(
                    distToObs >= obs.radius() + 0.44f,
                    "Survivor must not penetrate obstacle collision boundary (dist="
                            + distToObs
                            + ", limit="
                            + (obs.radius() + 0.45f)
                            + ")");
        }
    }

    @Test
    void hurtCooldownPreventsInstantSimultaneousBurstDeath() {
        FieldEvent event = new FieldEvent();
        event.begin(2);
        event.update(0.1f, event.x(), event.z(), true, 0);
        assertEquals(100f, event.health());

        // First hit connects
        event.damage(30);
        assertEquals(70f, event.health());

        // Immediate consecutive hit in same or adjacent frame without time passage is throttled
        event.damage(30);
        assertEquals(
                70f, event.health(), "Simultaneous burst hit should be throttled by hurt cooldown");

        // Step time by 0.2s (still under 0.35s window)
        event.update(0.2f, event.x(), event.z(), false, 0);
        event.damage(30);
        assertEquals(70f, event.health(), "Hit within 0.35s window should remain throttled");

        // Step remaining 0.2s (total 0.4s > 0.35s cooldown)
        event.update(0.2f, event.x(), event.z(), false, 0);
        event.damage(30);
        assertEquals(40f, event.health(), "Hit after cooldown expiration must deal damage");
    }

    @Test
    void fullEscortToSanctuaryCompletesRescue() {
        FieldEvent event = new FieldEvent();
        event.begin(2);
        event.update(0.1f, event.x(), event.z(), true, 0);
        assertTrue(event.vulnerable());

        // Player leads the survivor toward the sanctuary
        for (int i = 0; i < 300 && event.visible(); i++) {
            float toCenter = (float) Math.hypot(event.x(), event.z());
            float targetD = Math.max(0, toCenter - 3.0f);
            float px = toCenter > 0.01f ? event.x() / toCenter * targetD : 0;
            float pz = toCenter > 0.01f ? event.z() / toCenter * targetD : 0;
            event.update(0.1f, px, pz, false, 0);
        }

        assertEquals(FieldEvent.Status.COMPLETE, event.status());
        assertTrue(event.claim());
        assertFalse(event.claim());
    }
}
