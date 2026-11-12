import Entities.Enemy;

import java.util.List;

/**
 * A supporting defense: threats nearest the sanctuary take priority; every foe needs a finishing
 * blow.
 */
public final class SanctuaryWard {
    public static final float RANGE = 9, INTERVAL = 4;

    public static float range(int spireLevel) {
        return RANGE + (spireLevel - 1) * 1.5f;
    }

    public static float interval(int spireLevel) {
        return Math.max(2.0f, INTERVAL - (spireLevel - 1) * 0.35f);
    }

    public static Enemy target(List<Enemy> enemies) {
        return target(enemies, 1);
    }

    public static Enemy target(List<Enemy> enemies, int spireLevel) {
        float r = range(spireLevel);
        Enemy nearest = null;
        for (Enemy e : enemies)
            if (e.health > 1
                    && e.distance(0, 0) < r
                    && (nearest == null || e.distance(0, 0) < nearest.distance(0, 0))) nearest = e;
        return nearest;
    }

    public static float damage(int level, float health) {
        return level < 2 ? 0 : Math.max(0, Math.min(health - 1, Math.min(14, 6 + level * 2)));
    }

    public static float damage(int baseLevel, int spireLevel, float health) {
        if (baseLevel < 2 && spireLevel < 2) return 0;
        float baseDmg = 6 + baseLevel * 2 + (spireLevel - 1) * 3;
        float cap = 14 + (spireLevel - 1) * 3;
        return Math.max(0, Math.min(health - 1, Math.min(cap, baseDmg)));
    }
}
