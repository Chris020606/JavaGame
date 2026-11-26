import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ObjectiveTrackerTest {
    @Test
    void separatedSurvivorAnswersACallAndDamageRedirectsTheMarker() {
        FieldEvent event = new FieldEvent();
        event.begin(2);
        float x = event.x(), z = event.z();
        event.update(.05f, x, z, true);
        event.update(.05f, x + 18, z, false);
        assertEquals(x, event.x());
        assertTrue(ObjectiveStatus.of(event, "Bram", x + 18, z, "K").instruction().contains("[K]"));
        event.update(.05f, x + 18, z, true);
        assertTrue(event.walking());
        event.damage(5);
        assertFalse(ObjectiveStatus.of(event, "Bram", x, z, "K").guideToBeacon());
        for (int i = 0; i < 51; i++) event.update(.05f, x, z, false);
        assertTrue(ObjectiveStatus.of(event, "Bram", x, z, "K").guideToBeacon());
    }

    @Test
    void markerKeepsOffscreenAndBehindTargetsInsideThePlayableViewport() {
        for (int[] size : new int[][] {{1000, 680}, {1280, 800}}) {
            for (float[] point :
                    new float[][] {{-1000, 400}, {4000, 300}, {700, -900}, {700, 4000}}) {
                var marker = ObjectiveTracker.place(point[0], point[1], false, size[0], size[1]);
                assertTrue(marker.edge());
                assertTrue(marker.x() >= 420 && marker.x() <= size[0] - 200);
                assertTrue(marker.y() >= 140 && marker.y() <= size[1] - 285);
            }
            assertTrue(ObjectiveTracker.place(500, 300, true, size[0], size[1]).edge());
            var singular =
                    ObjectiveTracker.place(
                            Float.POSITIVE_INFINITY, Float.NaN, false, size[0], size[1]);
            assertTrue(Float.isFinite(singular.x()) && Float.isFinite(singular.y()));
            assertTrue(singular.edge());
            assertFalse(ObjectiveTracker.place(500, 300, false, size[0], size[1]).edge());
        }
    }

    @Test
    void rescueStatusExplainsInteractionAndWhyTheSurvivorStops() {
        FieldEvent event = new FieldEvent();
        event.begin(2);
        assertTrue(
                ObjectiveStatus.of(event, "Bram the Woodcutter", event.x() + 8, event.z(), "G")
                        .instruction()
                        .contains("8m"));
        assertTrue(
                ObjectiveStatus.of(event, "Bram the Woodcutter", event.x(), event.z(), "G")
                        .instruction()
                        .contains("[G]"));
        var waiting = ObjectiveStatus.of(event, "Kaelen the Scout", event.x(), event.z(), "G");
        assertEquals("Kaelen the Scout", waiting.title());
        assertFalse(waiting.guideToBeacon());
        event.update(.01f, event.x(), event.z(), true);
        assertTrue(
                ObjectiveStatus.of(event, "Bram", event.x() + 15.9f, event.z(), "G")
                        .guideToBeacon());
        assertFalse(
                ObjectiveStatus.of(event, "Bram", event.x() + 16f, event.z(), "G").guideToBeacon());
        assertTrue(
                ObjectiveStatus.of(event, "Bram the Woodcutter", event.x() + 17, event.z(), "G")
                        .instruction()
                        .contains("WAITING"));
        assertTrue(
                ObjectiveStatus.of(event, "Bram the Woodcutter", event.x() + 5, event.z(), "G")
                        .instruction()
                        .contains("FOLLOWING"));
        assertTrue(
                ObjectiveStatus.of(event, "Bram the Woodcutter", event.x(), event.z(), "G")
                        .instruction()
                        .contains("LEAD"));
    }
}
