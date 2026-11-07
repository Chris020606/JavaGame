import java.util.List;

/** Small named areas provide orientation; they do not grant rewards or change combat rules. */
public final class WorldLandmarks {
    public record Landmark(String name, float x, float z, float radius) {}

    private static final List<List<Landmark>> CHAPTERS =
            List.of(
                    List.of(
                            new Landmark("WARDEN'S GARDEN", -10, 5, 4),
                            new Landmark("WAYFARER'S REST", 13, -3, 5),
                            new Landmark("OLD WATCH CAMP", -12, -11, 5)),
                    List.of(
                            new Landmark("THE BROKEN OATH", -13, -11, 4),
                            new Landmark("ASHEN REST", 13, 11, 4)),
                    List.of(
                            new Landmark("REFUGEE CAMP", -12, 6, 4),
                            new Landmark("DAWN APPROACH", 7, 16, 4)),
                    List.of(
                            new Landmark("THE KING'S ROAD", -2, -17, 5),
                            new Landmark("HOLLOW COURT", 13, -8, 4)));

    private WorldLandmarks() {}

    public static Landmark nearby(int chapter, float x, float z) {
        if (chapter < 0 || chapter >= CHAPTERS.size()) return null;
        Landmark nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (var place : CHAPTERS.get(chapter)) {
            double distance = Math.hypot(x - place.x(), z - place.z());
            if (distance <= place.radius() && distance < best) {
                nearest = place;
                best = distance;
            }
        }
        return nearest;
    }
}
