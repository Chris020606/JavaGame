import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Cached title-tree placement with whole-crown clearance around every road segment. */
final class TitleForest {
    record Tree(int x, int y, int size, int crown) {}

    private static int cachedHeight = -1;
    private static List<Tree> cached = List.of();

    static List<Tree> at(int height) {
        if (height == cachedHeight) return cached;
        int base = height * 2 / 3 + 110;
        float depth = height - base;
        ArrayList<Tree> trees = new ArrayList<>();
        java.util.Random random = new java.util.Random(452771);
        for (int i = 0; i < 1000; i++) {
            float t = .12f + random.nextFloat() * 1.08f;
            int y = Math.round(base + depth * t);
            float shoulder = 183 + 207 * Math.max(0, y - base) / (depth + 45);
            int x = Math.round((random.nextFloat() * 2 - 1) * (shoulder - 24));
            int size = Math.round(31 + t * 32 + random.nextFloat() * 19);
            int crown = 6 + (int) (t * 3) + random.nextInt(4);
            // Even coverage with a minimum spacing prevents overlapping walls of foliage.
            boolean crowded = false;
            for (Tree other : trees) {
                float dx = (x - other.x) / 34f, dy = (y - other.y) / 16f;
                if (dx * dx + dy * dy < 1) {
                    crowded = true;
                    break;
                }
            }
            if (crowded) continue;
            Tree tree = new Tree(x, y, size, crown);
            if (Math.abs(x) + crown + 5 < shoulder && clear(tree, height)) trees.add(tree);
        }
        trees.sort(Comparator.comparingInt(Tree::y));
        cachedHeight = height;
        return cached = List.copyOf(trees);
    }

    static boolean clear(Tree tree, int height) {
        float base = height * 2 / 3 + 110, depth = height - base;
        float[] xs = {2, 45, 90, 24, -76, -23};
        float[] ys = {-.045f, .13f, .3f, .5f, .75f, 1.15f};
        float half = tree.crown + 17;
        for (int i = 0; i < xs.length - 1; i++) {
            float margin = 9 + i * 1.2f;
            float left = tree.x - half - margin, right = tree.x + half + margin;
            float top = tree.y - tree.size - margin, bottom = tree.y + margin;
            float ax = xs[i], ay = base + depth * ys[i];
            float dx = xs[i + 1] - ax, dy = depth * (ys[i + 1] - ys[i]);
            float min = 0, max = 1;
            if (Math.abs(dx) < .001f) {
                if (ax < left || ax > right) continue;
            } else {
                float a = (left - ax) / dx, b = (right - ax) / dx;
                min = Math.max(min, Math.min(a, b));
                max = Math.min(max, Math.max(a, b));
            }
            if (Math.abs(dy) < .001f) {
                if (ay < top || ay > bottom) continue;
            } else {
                float a = (top - ay) / dy, b = (bottom - ay) / dy;
                min = Math.max(min, Math.min(a, b));
                max = Math.min(max, Math.max(a, b));
            }
            if (min <= max) return false;
        }
        return true;
    }
}
