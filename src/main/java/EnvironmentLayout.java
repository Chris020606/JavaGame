import java.util.*;

/** Authored landmarks and seeded vegetation clusters, shared by both render passes. */
public final class EnvironmentLayout {
    public record Prop(
            boolean graveyard, String model, float x, float y, float z, float yaw, float scale) {}

    public record Point(float x, float z) {}

    private static final List<List<Prop>> CHAPTERS = new ArrayList<>();
    private static final List<List<Point>> PATHS =
            List.of(
                    List.of(
                            new Point(-5, -32),
                            new Point(-4, -23),
                            new Point(-2, -13),
                            new Point(0, 0),
                            new Point(2, 10),
                            new Point(7, 20),
                            new Point(10, 32)),
                    List.of(
                            new Point(2, -30),
                            new Point(2, -21),
                            new Point(0, -10),
                            new Point(0, 0),
                            new Point(3, 12),
                            new Point(4, 23),
                            new Point(4, 31)),
                    List.of(
                            new Point(-12, -32),
                            new Point(-10, -22),
                            new Point(-5, -12),
                            new Point(0, 0),
                            new Point(7, 12),
                            new Point(10, 23),
                            new Point(12, 32)),
                    List.of(
                            new Point(0, -32),
                            new Point(0, -23),
                            new Point(-2, -12),
                            new Point(0, 0),
                            new Point(4, 13),
                            new Point(4, 24),
                            new Point(4, 32)));

    static {
        for (int chapter = 0; chapter < 4; chapter++) CHAPTERS.add(build(chapter));
    }

    private static void n(List<Prop> p, String name, float x, float z, float yaw, float scale) {
        p.add(new Prop(false, name, x, 0, z, yaw, scale));
    }

    private static void g(List<Prop> p, String name, float x, float z, float yaw, float scale) {
        p.add(new Prop(true, name, x, 0, z, yaw, scale));
    }

    private static List<Prop> build(int chapter) {
        List<Prop> p = new ArrayList<>();
        Random random = new Random(9201 + chapter * 31);
        if (chapter == 0) {
            n(p, "tent_detailedOpen", -13, -10, 58, 3.7f);
            n(p, "tent_detailedOpen", -10, -15, 12, 3.1f);
            n(p, "campfire_stones", -9, -8, 0, 2.2f);
            n(p, "log_stack", -15, -13, 103, 2.1f);
            n(p, "fence_simple", -17, -8, 17, 2.4f);
            n(p, "fence_simple", -17, -11, 7, 2.2f);
            n(p, "statue_columnDamaged", 9, 14, 11, 3.4f);
            n(p, "statue_columnDamaged", 13, 16, 82, 2.5f);
            n(p, "statue_obelisk", 11, 19, -9, 3.1f);
            // Unequal landmarks help the player orient: ruins west, a wayfarer stop east.
            // Keep the central eight-metre sanctuary clearing open for fights and healing.
            n(p, "platform_stone", -10, 5, 0, 3.5f);
            n(p, "statue_columnDamaged", -12, 3, 18, 3.2f);
            n(p, "statue_ring", -9, 6, 90, 2.8f);
            n(p, "stone_largeA", -13, 7, 72, 2.2f);
            n(p, "flower_purpleC", -8, 7, 0, 2.2f);
            n(p, "tent_detailedOpen", 13, -3, -75, 2.8f);
            n(p, "campfire_stones", 11, -1, 0, 1.7f);
            n(p, "log_stack", 15, 1, 15, 1.8f);
            n(p, "fence_simple", 16, -4, 5, 2);
            n(p, "plant_bushDetailed", 16, 2, 40, 2.1f);
            n(p, "flower_yellowC", 9, -4, 0, 1.8f);
            // A ruined lookout and stone stair terrace frame the eastern approach.
            n(p, "platform_stone", 24, 7, 0, 5.5f);
            n(p, "platform_stone", 27, 7, 0, 5.5f);
            n(p, "statue_columnDamaged", 23, 4, 0, 5);
            n(p, "statue_columnDamaged", 28, 4, 180, 4);
            n(p, "statue_ring", 26, 8, 90, 4);
            n(p, "stone_largeA", 22, 11, 35, 3);
            n(p, "flower_purpleC", 21, 9, 0, 2);
            n(p, "plant_bushDetailed", 29, 11, 40, 3);
            n(p, "bridge_wood", -4, -28, 90, 4.2f);
            // Layered riverbank groves conceal the distant seam from low orbit angles.
            for (int side : new int[] {-1, 1})
                for (int i = 0; i < 14; i++) {
                    float x = side * (31 + i * 2.8f + (random.nextFloat() - .5f) * 1.4f),
                            z = -24 + (i % 3) * 4.2f + (random.nextFloat() - .5f) * 2;
                    n(
                            p,
                            i % 3 == 0 ? "tree_oak" : "tree_pineTallA_detailed",
                            x,
                            z,
                            random.nextFloat() * 360,
                            5.5f + random.nextFloat() * 2.5f);
                    n(
                            p,
                            "plant_bushDetailed",
                            x - side * 1.7f,
                            -17 + (i % 2) * 2,
                            random.nextFloat() * 360,
                            2.4f + random.nextFloat());
                    if (i % 3 == 0) n(p, "stone_largeA", x, -23, random.nextFloat() * 360, 3.2f);
                }
            // Tree screens continue along both banks until the river leaves the distant view.
            for (int end : new int[] {-1, 1})
                for (int i = 0; i < 11; i++)
                    for (int bank : new int[] {-1, 1}) {
                        float x = end * (82 + i * 9 + (random.nextFloat() - .5f) * 3),
                                z = bank < 0 ? -31 : -17 + (random.nextFloat() - .5f) * 2;
                        n(
                                p,
                                i % 3 == 0 ? "tree_oak" : "tree_pineTallA_detailed",
                                x,
                                z,
                                random.nextFloat() * 360,
                                5 + random.nextFloat() * 2.8f);
                        if (i % 2 == 0)
                            n(
                                    p,
                                    "plant_bushDetailed",
                                    x - end * 2.2f,
                                    z + bank * 1.3f,
                                    random.nextFloat() * 360,
                                    2 + random.nextFloat());
                    }
            n(p, "tree_oak", -19, 6, 110, 3.2f);
            n(p, "tree_pineRoundC", 18, -15, 35, 3.7f);
            n(p, "tree_oak", 20, 15, 210, 3.3f);
            n(p, "stump_old", -15, 12, 16, 2.3f);
            n(p, "log_large", -17, 15, 58, 2.1f);
            for (int i = 0; i < 72; i++) {
                boolean bank = i < 24;
                float cx = bank ? -14 : i < 48 ? 13 : -12, cz = bank ? -21 : i < 48 ? -7 : 15;
                float x = cx + (float) random.nextGaussian() * 2.8f,
                        z = cz + (float) random.nextGaussian() * 2;
                if (Math.hypot(x, z) < 8) continue;
                n(
                        p,
                        i % 6 == 0
                                ? "plant_bushDetailed"
                                : i % 3 == 0
                                        ? "grass_leafsLarge"
                                        : i % 2 == 0 ? "flower_redB" : "flower_yellowC",
                        x,
                        z,
                        random.nextFloat() * 360,
                        1.2f + random.nextFloat() * 1.2f);
                if (i % 8 == 0)
                    n(p, "mushroom_redGroup", x + .4f, z, random.nextFloat() * 360, 1.3f);
            }
            for (float[] lily :
                    new float[][] {{-24, -20}, {-8, -28}, {0, -28}, {16, -20}, {24, -20}})
                p.add(new Prop(false, "lily_small", lily[0], -.18f, lily[1], 35, 1.1f));
            cluster(p, random, 12, -13, 7);
            cluster(p, random, -15, 12, 9);
            cluster(p, random, 17, 5, 5);
        } else if (chapter == 1) {
            burialPlot(p, random, -13, -11, 9, 3, 3);
            burialPlot(p, random, 13, 11, -12, 3, 3);
            burialPlot(p, random, 14, -14, -5, 2, 3);
            burialPlot(p, random, -16, 14, 16, 2, 2);
            g(p, "crypt", -28, -14, 80, 5);
            g(p, "crypt", 24, -28, -23, 5.8f);
            g(p, "pine-crooked", -21, -8, 33, 5);
            g(p, "pine-crooked", 20, 17, 202, 5.8f);
            g(p, "pine-crooked", -22, 12, 18, 4.1f);
            g(p, "pine-crooked", 18, -3, 227, 4.5f);
            // Low ritual markers frame the duel without obstructing its central dodge space.
            g(p, "altar-stone", -11, 4, 20, 2.5f);
            g(p, "fire-basket", -9, 5, 0, 1.5f);
            g(p, "coffin", 12, -5, -25, 2.1f);
            cemeteryWall(p);
            g(p, "iron-fence-border-gate", 2, -24, 0, 5);
        } else if (chapter == 2) {
            // Uneven outcrops suggest a winding cut through the mountain, not two tiled rails.
            for (int side : new int[] {-1, 1})
                for (int i = 0; i < 8; i++) {
                    float z = -29 + i * 8 + random.nextFloat() * 4,
                            x = side * (25 + random.nextFloat() * 7);
                    n(
                            p,
                            "cliff_half_stone",
                            x,
                            z,
                            side * 90 + random.nextFloat() * 32 - 16,
                            5 + random.nextFloat() * 4);
                    n(
                            p,
                            "stone_tallB",
                            x - side * 4,
                            z + 2,
                            random.nextFloat() * 360,
                            2.8f + random.nextFloat() * 2);
                }
            // Distant shoulders mark the pass bends; these stay outside the active arena.
            n(p, "statue_columnDamaged", -23, -10, 35, 4);
            n(p, "statue_obelisk", 23, 15, -15, 4.2f);
            cluster(p, random, -16, -14, 6);
            cluster(p, random, 17, 13, 6);
            n(p, "tent_detailedOpen", -12, 6, 85, 3.2f);
            n(p, "log_stack", -15, 9, 112, 2.3f);
            n(p, "campfire_stones", -9, 8, 0, 2);
            n(p, "bridge_stone", 0, -12, 0, 3.5f);
        } else {
            g(p, "crypt-large", 0, -30, 0, 5);
            p.add(new Prop(true, "crypt-large-roof", 0, 5, -30, 0, 5));
            g(p, "crypt-large", 30, -8, 270, 4);
            p.add(new Prop(true, "crypt-large-roof", 30, 4, -8, 270, 4));
            g(p, "crypt", -28, 10, 90, 6);
            g(p, "crypt", 23, 29, 193, 5);
            for (int i = 0; i < 4; i++) {
                g(p, "pillar-obelisk", -12 + i * .6f, -18 + i * 9, 3 + i * 7, 3.6f - i * .2f);
                if (i != 2)
                    g(p, "pillar-obelisk", 13 + i * .7f, -17 + i * 9, -8 - i * 9, 3.6f - i * .3f);
            }
            burialPlot(p, random, -18, -17, 24, 2, 2);
            burialPlot(p, random, 19, 14, -20, 2, 2);
            g(p, "coffin", -17, 19, 27, 2.6f);
            g(p, "gravestone-broken", -15, 21, 32, 2);
            g(p, "pine-crooked", -22, -4, 18, 4.3f);
            g(p, "pine-crooked", 20, 18, 227, 4.8f);
            cemeteryWall(p);
            g(p, "iron-fence-border-gate", 0, -24, 0, 5);
            g(p, "fire-basket", -4, -17, 0, 2.7f);
            g(p, "fire-basket", 5, -18, 0, 2.4f);
        }
        // Natural tree belts extend the playable clearing into a wooded horizon.
        // Keep the roadside corridor open while layering trunks behind its edges.
        for (int side : new int[] {-1, 1})
            for (int i = 0; i < 12; i++) {
                float z = -56 + i * 10 + (random.nextFloat() - .5f) * 4;
                float x = pathCenterX(chapter, z) + side * (13 + random.nextFloat() * 4);
                float scale = 4.1f + random.nextFloat() * 3.3f, yaw = random.nextFloat() * 360;
                String tree =
                        chapter == 0
                                ? (i % 3 == 0 ? "tree_oak" : "tree_pineTallA_detailed")
                                : chapter == 2
                                        ? (i % 3 == 0
                                                ? "tree_pineRoundC"
                                                : "tree_pineTallA_detailed")
                                        : "pine-crooked";
                if (chapter == 1 || chapter == 3) g(p, tree, x, z, yaw, scale);
                else n(p, tree, x, z, yaw, scale);
                if (i % 2 == 0) {
                    float bushX = x - side * (2 + random.nextFloat() * 2),
                            bushZ = z + (random.nextFloat() - .5f) * 3;
                    String undergrowth =
                            chapter == 0
                                    ? "plant_bushDetailed"
                                    : chapter == 2 ? "stone_tallB" : "rocks-tall";
                    if (chapter == 1 || chapter == 3)
                        g(p, undergrowth, bushX, bushZ, yaw, 1.8f + random.nextFloat() * 1.4f);
                    else n(p, undergrowth, bushX, bushZ, yaw, 1.8f + random.nextFloat() * 1.4f);
                }
            }
        // A pair of far treelines breaks the road silhouette at both ends of the clearing.
        String horizonTree =
                chapter == 0
                        ? ("tree_pineTallA_detailed")
                        : chapter == 2 ? "tree_pineRoundC" : "pine-crooked";
        for (int end : new int[] {-1, 1}) {
            float z = end * 62, center = pathCenterX(chapter, z), yaw = random.nextFloat() * 360;
            if (chapter == 1 || chapter == 3) g(p, horizonTree, center, z, yaw, 6.4f);
            else n(p, horizonTree, center, z, yaw, 6.4f);
            for (int side : new int[] {-1, 1}) {
                float x = center + side * (6.5f + random.nextFloat() * 2),
                        backZ = z + end * (2 + random.nextFloat() * 2);
                if (chapter == 1 || chapter == 3)
                    g(
                            p,
                            horizonTree,
                            x,
                            backZ,
                            random.nextFloat() * 360,
                            5 + random.nextFloat() * 2);
                else
                    n(
                            p,
                            horizonTree,
                            x,
                            backZ,
                            random.nextFloat() * 360,
                            5 + random.nextFloat() * 2);
            }
        }
        // Mixed-scale groves form an irregular horizon beyond the playable boundary.
        float[][] groves = {
            {-35, -27}, {-41, 4}, {-28, 35}, {5, 42}, {34, 29}, {43, -4}, {29, -34}, {-4, -44}
        };
        for (float[] grove : groves)
            for (int i = 0; i < 9; i++) {
                float x = grove[0] + (float) random.nextGaussian() * 7,
                        z = grove[1] + (float) random.nextGaussian() * 7;
                if (Math.abs(x) < 25 && Math.abs(z) < 25) continue;
                float yaw = random.nextFloat() * 360, scale = 3.5f + random.nextFloat() * 5;
                if (chapter == 1 || chapter == 3)
                    g(p, i % 4 == 0 ? "rocks-tall" : "pine-crooked", x, z, yaw, scale);
                else
                    n(
                            p,
                            i % 4 == 0
                                    ? "stone_tallB"
                                    : i % 3 == 0 ? "tree_oak" : "tree_pineTallA_detailed",
                            x,
                            z,
                            yaw,
                            scale);
            }
        return List.copyOf(p);
    }

    private static float pathCenterX(int chapter, float z) {
        List<Point> path = PATHS.get(chapter);
        for (int i = 1; i < path.size(); i++) {
            Point a = path.get(i - 1), b = path.get(i);
            if (z <= b.z()) {
                float t = (z - a.z()) / (b.z() - a.z());
                return a.x() + (b.x() - a.x()) * t;
            }
        }
        Point a = path.get(path.size() - 2), b = path.getLast();
        return b.x() + (b.x() - a.x()) / (b.z() - a.z()) * (z - b.z());
    }

    private static void cemeteryWall(List<Prop> p) {
        // A deliberate built boundary with a gate opening and broken east/west sections.
        for (int i = -6; i <= 6; i++) {
            if (i != 0 && i != 1) g(p, "stone-wall", i * 4, -26, 0, 4);
            if (i > -5 && i < 5 && i != 1) g(p, "stone-wall", i * 4, 26, 0, 4);
            if (i % 4 != 0) g(p, "stone-wall", -26, i * 4, 90, 4);
            if (i < -2 || i > 2) g(p, "stone-wall", 26, i * 4, 90, 4);
        }
    }

    private static void burialPlot(
            List<Prop> p, Random r, float cx, float cz, float yaw, int rows, int columns) {
        double a = Math.toRadians(yaw);
        for (int row = 0; row < rows; row++)
            for (int col = 0; col < columns; col++) {
                float dx = (col - (columns - 1) * .5f) * 3.2f + (r.nextFloat() - .5f) * .9f,
                        dz = (row - (rows - 1) * .5f) * 4.3f + (r.nextFloat() - .5f) * 1.2f;
                float x = cx + (float) (dx * Math.cos(a) + dz * Math.sin(a)),
                        z = cz + (float) (dz * Math.cos(a) - dx * Math.sin(a));
                float tilt = yaw + (r.nextFloat() - .5f) * 17;
                g(p, "grave", x, z, tilt, 2.6f);
                g(
                        p,
                        (row + col) % 4 == 0
                                ? "gravestone-broken"
                                : (row + col) % 3 == 0 ? "gravestone-cross" : "gravestone-round",
                        x - (float) Math.sin(a),
                        z - (float) Math.cos(a),
                        tilt,
                        1.5f + r.nextFloat() * .3f);
                if (row == 0 && col == 0) g(p, "lantern-candle", x + 1.1f, z, tilt, 2.2f);
            }
    }

    private static void cluster(List<Prop> p, Random r, float cx, float cz, int count) {
        n(p, "stone_largeA", cx, cz, r.nextFloat() * 360, 2.3f + r.nextFloat());
        for (int i = 0; i < count; i++) {
            float x = cx + (float) r.nextGaussian() * 2.5f,
                    z = cz + (float) r.nextGaussian() * 2.5f;
            if (Math.hypot(x, z) < 8) continue;
            n(
                    p,
                    i % 4 == 0 ? "plant_bushDetailed" : "grass_large",
                    x,
                    z,
                    r.nextFloat() * 360,
                    1.2f + r.nextFloat());
            if (i % 3 == 0)
                n(
                        p,
                        "flower_purpleC",
                        x + .4f,
                        z + .2f,
                        r.nextFloat() * 360,
                        .8f + r.nextFloat() * .5f);
        }
    }

    /** Flat land runs leave the recessed river meshes open instead of covering their water. */
    public record GroundStrip(float x, float z, float width, float depth) {}

    public static List<GroundStrip> groundStrips(int chapter) {
        List<GroundStrip> strips = new ArrayList<>();
        int extent = chapter == 0 ? RIVER_EXTENT : 8;
        for (int z = -8; z <= 8; z++) {
            int start = -extent;
            for (int x = -extent; x <= extent + 1; x++) {
                boolean water = x <= extent && chapter == 0 && riverTile(x, z) != null;
                if (water || x == extent + 1) {
                    if (start < x)
                        strips.add(new GroundStrip((start + x - 1) * 2, z * 4, (x - start) * 4, 4));
                    start = x + 1;
                }
            }
        }
        return List.copyOf(strips);
    }

    public record RiverTile(String model, int yaw) {}

    public static final int RIVER_EXTENT = 44;

    public static RiverTile riverTile(int x, int z) {
        if (z == -5 && (x < -4 || x > 3) || z == -7 && x > -4 && x < 3)
            return new RiverTile("ground_riverStraight", 90);
        if (z == -6 && (x == -4 || x == 3)) return new RiverTile("ground_riverStraight", 0);
        if (x == -4 && z == -5) return new RiverTile("ground_riverBend", 0);
        if (x == -4 && z == -7) return new RiverTile("ground_riverBend", 180);
        if (x == 3 && z == -7) return new RiverTile("ground_riverBend", 90);
        if (x == 3 && z == -5) return new RiverTile("ground_riverBend", 270);
        return null;
    }

    public static List<Prop> props(int chapter) {
        return CHAPTERS.get(Math.max(0, Math.min(3, chapter)));
    }

    public static List<Point> path(int chapter) {
        return PATHS.get(Math.max(0, Math.min(3, chapter)));
    }

    private EnvironmentLayout() {}
}
