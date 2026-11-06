import Entities.Enemy;

import java.util.List;
import java.util.function.Predicate;

/**
 * Small launch correction, not homing: only visible targets already close to the intended heading.
 */
public final class RangedAim {
    public static Enemy target(
            float x,
            float z,
            float facing,
            float range,
            List<Enemy> enemies,
            Predicate<Enemy> visible) {
        Enemy best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (Enemy enemy : enemies) {
            double distance = Math.hypot(enemy.x - x, enemy.z - z);
            if (enemy.health <= 0 || distance > range || distance < .01) continue;
            double angle =
                    Math.abs(
                            Math.IEEEremainder(
                                    Math.toDegrees(Math.atan2(enemy.x - x, enemy.z - z)) - facing,
                                    360));
            if (angle > 12) continue;
            double score = angle + distance * .12;
            if (score < bestScore && visible.test(enemy)) {
                best = enemy;
                bestScore = score;
            }
        }
        return best;
    }

    private RangedAim() {}
}
