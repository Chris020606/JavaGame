import Entities.Enemy;

import java.util.List;

/**
 * A supporting defense: threats nearest the sanctuary take priority; every foe needs a finishing
 * blow.
 */
public final class SanctuaryWard {
    public static final float RANGE = 9, INTERVAL = 4;

    public static Enemy target(List<Enemy> enemies) {
        Enemy nearest = null;
        for (Enemy e : enemies)
            if (e.health > 1
                    && e.distance(0, 0) < RANGE
                    && (nearest == null || e.distance(0, 0) < nearest.distance(0, 0))) nearest = e;
        return nearest;
    }

    public static float damage(int level, float health) {
        return level < 2 ? 0 : Math.max(0, Math.min(health - 1, Math.min(14, 6 + level * 2)));
    }
}
