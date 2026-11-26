import java.util.function.BiPredicate;

/** Small home-centred daily routine: visit neighbours, pause, then return home. */
public final class CitizenRoutine {
    public float x, z, yaw;
    public boolean walking;
    private float homeX, homeZ;
    private boolean placed;
    private int stop = 1;
    private final float pace;
    private float speed;
    public float strideFrame;
    private boolean attentive;

    /** Separate approach and departure distances prevent flicker at conversation range. */
    public boolean attend(float distance, boolean speaking) {
        attentive = speaking || distance < (attentive ? 3.5f : 2.7f);
        return attentive;
    }

    private float wait = 2;
    private static final float[][] STOPS = {{0, 0}, {2, 0}, {2, 3}, {0, 3}, {-2, 1}};

    public CitizenRoutine(CitadelNpc npc) {
        x = homeX = npc.x();
        z = homeZ = npc.z();
        yaw = npc.yaw();
        pace = .78f + .07f * (npc.ordinal() % 4);
        wait += npc.ordinal() % 4;
    }

    public void update(float dt, boolean talking, BiPredicate<Float, Float> walkable) {
        walking = false;
        if (!Float.isFinite(dt) || dt <= 0) return;
        // Old home positions can overlap scenery. Recover into the nearest clear courtyard.
        if (!placed && !walkable.test(x, z)) {
            boolean found = false;
            for (float radius = .5f; radius <= 8 && !found; radius += .5f) {
                for (int i = 0; i < 16; i++) {
                    double angle = i * Math.PI / 8;
                    float nx = x + (float) Math.cos(angle) * radius,
                            nz = z + (float) Math.sin(angle) * radius;
                    if (walkable.test(nx, nz)) {
                        homeX = x = nx;
                        homeZ = z = nz;
                        found = true;
                        break;
                    }
                }
            }
            if (!found) return;
        }
        placed = true;
        if (talking) {
            speed = 0;
            wait = Math.max(wait, .65f);
            return;
        }
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
        // Ease into the stride and shorten the last steps before each stop.
        speed = Math.min(speed + dt * 1.8f, Math.min(pace, (float) Math.sqrt(2.6f * distance)));
        float step = Math.min(distance, dt * speed);
        float nx = x + dx / distance * step, nz = z + dz / distance * step;
        if (!walkable.test(nx, nz)) {
            next();
            return;
        }
        x = nx;
        z = nz;
        yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        strideFrame += step * 32f;
        walking = true;
    }

    private void next() {
        speed = 0;
        stop = (stop + 1) % STOPS.length;
        wait = stop == 0 ? 8 : 3;
    }
}
