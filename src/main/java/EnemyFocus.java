import Entities.Enemy;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** Client-side focus selection. Focusing an enemy never bypasses range, cover or hit checks. */
public final class EnemyFocus {
    private Enemy selected;

    public Enemy selected() {
        return selected;
    }

    public void clear() {
        selected = null;
    }

    public void update(List<Enemy> enemies) {
        // Loads and chapter changes replace actor objects; do not keep a prior encounter selected.
        if (selected != null && (selected.health <= 0 || !enemies.contains(selected))) clear();
    }

    public void cycle(float x, float z, List<Enemy> enemies, Predicate<Enemy> visible) {
        List<Enemy> candidates =
                enemies.stream()
                        .filter(enemy ->
                                enemy.health > 0
                                        && enemy.distance(x, z) <= 28
                                        && visible.test(enemy))
                        .sorted(Comparator.comparingDouble(enemy -> enemy.distance(x, z)))
                        .toList();
        int next = candidates.indexOf(selected) + 1;
        selected = candidates.isEmpty() ? null : candidates.get(next % candidates.size());
    }
}
