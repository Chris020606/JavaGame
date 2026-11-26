import Entities.Enemy;

public final class Projectile {
    static final float LIFETIME = 3,
            BEACON_RADIUS = 1.8f,
            OBJECTIVE_RADIUS = .8f,
            GUARD_RADIUS = 1.1f;
    public float x, z, previousX, previousZ, life = LIFETIME;
    public final float vx, vz, damage;
    public final boolean playerTarget, magic;
    public final boolean travelerTarget;

    public Projectile(Enemy enemy, int wave) {
        x = previousX = enemy.x;
        z = previousZ = enemy.z;
        float dx = enemy.attackX - x, dz = enemy.attackZ - z, len = (float) Math.hypot(dx, dz);
        magic = enemy.variant == 4;
        playerTarget = enemy.targetsPlayer;
        travelerTarget = enemy.targetsTraveler;
        damage = ((magic ? 14 : 8) + wave * 2) * enemy.difficulty.damage;
        float speed = speed(magic);
        vx = len > .001f ? dx / len * speed : 0;
        vz = len > .001f ? dz / len * speed : 0;
    }

    static float speed(boolean magic) {
        return magic ? 6 : 10;
    }

    public void update(float dt) {
        previousX = x;
        previousZ = z;
        x += vx * dt;
        z += vz * dt;
        life -= dt;
    }

    /** First contact along the swept shot, or infinity if this circle is missed. */
    public float contact(float px, float pz, float radius) {
        return contact(previousX, previousZ, x - previousX, z - previousZ, px, pz, radius);
    }

    static float contact(
            float startX, float startZ, float dx, float dz, float px, float pz, float radius) {
        float ox = startX - px, oz = startZ - pz;
        float outside = ox * ox + oz * oz - radius * radius;
        if (outside <= 0) return 0;
        float length = dx * dx + dz * dz, along = -(ox * dx + oz * dz);
        float discriminant = along * along - length * outside;
        if (along <= 0 || discriminant < 0) return Float.POSITIVE_INFINITY;
        float t = outside / (along + (float) Math.sqrt(discriminant));
        return t <= 1 ? t : Float.POSITIVE_INFINITY;
    }

    public boolean hits(float px, float pz, float radius) {
        return contact(px, pz, radius) <= 1;
    }
}
