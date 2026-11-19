/**
 * Shortest-arc turning keeps city characters from snapping or spinning through a full revolution.
 */
final class NpcFacing {
    static float approach(float current, float desired, float dt) {
        if (!Float.isFinite(dt) || dt <= 0) return current;
        float delta = (desired - current) % 360;
        if (delta > 180) delta -= 360;
        if (delta < -180) delta += 360;
        float step = 160 * Math.min(.05f, dt);
        return current + Math.max(-step, Math.min(step, delta));
    }
}
