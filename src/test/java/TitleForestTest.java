import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TitleForestTest {
    @Test
    void forestIsDenseDepthSortedAndClearsTheWholeRoadAtBothSizes() {
        for (int height : new int[] {680, 800}) {
            var trees = TitleForest.at(height);
            assertTrue(trees.size() > 35, "Expected woodland on both sides of the approach");
            int previous = 0;
            for (var tree : trees) {
                assertTrue(tree.y() >= previous);
                assertTrue(TitleForest.clear(tree, height));
                for (var other : trees) {
                    if (tree == other) continue;
                    float dx = (tree.x() - other.x()) / 34f, dy = (tree.y() - other.y()) / 16f;
                    assertTrue(
                            dx * dx + dy * dy >= .999f,
                            "Trees must have room between their trunks");
                }
                previous = tree.y();
            }
            assertFalse(
                    TitleForest.clear(
                            new TitleForest.Tree(45, height * 2 / 3 + 130, 60, 12), height));
        }
    }
}
