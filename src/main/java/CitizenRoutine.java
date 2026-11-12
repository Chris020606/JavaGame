import java.util.function.BiPredicate;

/** Small home-centred daily routine: visit neighbours, pause, then return home. */
public final class CitizenRoutine {
    public float x, z, yaw;
    public boolean walking;
    private final float homeX, homeZ;
    private int stop;
    private float wait = 2;
    private static final float[][] STOPS = {{0, 0}, {2, 0}, {2, 3}, {0, 3}, {-2, 1}};

    public CitizenRoutine(CitadelNpc npc) {
        x = homeX = npc.x();
        z = homeZ = npc.z();
        yaw = npc.yaw();
        wait += npc.ordinal() % 4;
    }

    public void update(float dt, boolean talking, BiPredicate<Float, Float> walkable) {
        walking = false;
        if (!Float.isFinite(dt) || dt <= 0) return;
        // Old home positions can overlap scenery. Recover into the nearest clear courtyard.
        if (!walkable.test(x, z)) {
            boolean found = false;
            for (float radius = .5f; radius <= 8 && !found; radius += .5f) {
                for (int i = 0; i < 16; i++) {
                    double angle = i * Math.PI / 8;
                    float nx = x + (float) Math.cos(angle) * radius,
                            nz = z + (float) Math.sin(angle) * radius;
                    if (walkable.test(nx, nz)) {
                        x = nx;
                        z = nz;
                        found = true;
                        break;
                    }
                }
            }
        }
        if (talking) return;
        dt = Math.min(dt, .05f);
        if (wait > 0) {
            wait -= dt;
            return;
        }
        float tx = homeX + STOPS[stop][0], tz = homeZ + STOPS[stop][1];
        float dx = tx - x, dz = tz - z, distance = (float) Math.hypot(dx, dz);
        if (distance < .1f) {
            next();
            return;
        }
        float step = Math.min(distance, dt * .85f);
        float nx = x + dx / distance * step, nz = z + dz / distance * step;
        if (!walkable.test(nx, nz)) {
            next();
            return;
        }
        x = nx;
        z = nz;
        yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        walking = true;
    }

    private void next() {
        stop = (stop + 1) % STOPS.length;
        wait = stop == 0 ? 8 : 3;
    }
}
