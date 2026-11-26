import static com.raylib.Raylib.*;

import Entities.BossMove;
import Entities.Enemy;

import assets.AssetPaths;

/**
 * Soft textured particles, weapon ribbons and authored spell decals. No collision rules live here.
 */
public final class CombatVfx implements AutoCloseable {
    private final Texture soft = texture("soft"),
            spark = texture("spark"),
            rune = texture("rune"),
            ring = texture("ring");
    private final Model ground = LoadModelFromMesh(GenMeshPlane(2, 2, 1, 1));
    private static final int MAX_PARTICLES = 240;
    private final Particle[] particles = particlePool();
    private int particleCount, replacement;

    int particleCount() {
        return particleCount;
    }

    private static Particle[] particlePool() {
        Particle[] pool = new Particle[MAX_PARTICLES];
        for (int i = 0; i < pool.length; i++) pool[i] = new Particle();
        return pool;
    }

    private final WeaponTrail blade = new WeaponTrail();
    private boolean heavyBlade;

    public WeaponTrail blade() {
        return blade;
    }

    public void beginMelee(boolean heavy) {
        heavyBlade = heavy;
        blade.begin(heavy);
    }

    private int serial;
    private float recallX, recallZ, recallTime;
    private static final float RECALL_DURATION = 1.3f;

    public void rescue(float x, float z) {
        recallX = x;
        recallZ = z;
        recallTime = RECALL_DURATION;
    }

    /** The traveler's light crosses the ring and gathers at the beacon. */
    private void drawRescue(Camera3D camera) {
        if (recallTime <= 0) return;
        float age = RECALL_DURATION - recallTime;
        for (int i = 0; i < 20; i++) {
            float t = (age - i * .017f) / .94f;
            if (t < 0 || t >= 1) continue;
            float eased = t * t * (3 - 2 * t);
            double a = 2.4 * i + t * 5;
            float radius = (1 - t) * .38f;
            float x = recallX * (1 - eased) + radius * (float) Math.cos(a);
            float z = recallZ * (1 - eased) + radius * (float) Math.sin(a);
            float y =
                    .3f
                            + (i % 6) * .22f * (1 - t)
                            + 2.6f * eased
                            + 1.8f * (float) Math.sin(Math.PI * t);
            glow(camera, x, y, z, .34f, i % 3 == 0 ? Menu.GOLD : Menu.TEAL, .8f * (1 - t));
        }
    }

    private static final class Particle {
        float x, y, z, vx, vy, vz, life, total, size;
        Color color;
    }

    private static Texture texture(String name) {
        Texture t = LoadTexture(AssetPaths.path("resources/outpost/effects/" + name + ".png"));
        if (t.id() == 0) throw new IllegalStateException("Missing combat effect: " + name);
        SetTextureFilter(t, TEXTURE_FILTER_BILINEAR);
        return t;
    }

    private static Vector3 v(float x, float y, float z) {
        return new Vector3().x(x).y(y).z(z);
    }

    public void clear() {
        particleCount = replacement = 0;
        recallTime = 0;
        blade.clear();
    }

    public void update(float dt) {
        blade.update(dt);
        recallTime = Math.max(0, recallTime - dt);
        for (int i = 0; i < particleCount; ) {
            Particle p = particles[i];
            p.life -= dt;
            if (p.life <= 0) {
                particles[i] = particles[--particleCount];
                particles[particleCount] = p;
            } else {
                p.x += p.vx * dt;
                p.y += p.vy * dt;
                p.z += p.vz * dt;
                p.vy -= 5 * dt;
                i++;
            }
        }
    }

    public void impact(float x, float z, Color color, boolean heavy) {
        emitImpact(x, z, color, heavy, x, z, false);
    }

    public void strike(float x, float z, float sourceX, float sourceZ, Color color, boolean heavy) {
        emitImpact(x, z, color, heavy, sourceX, sourceZ, true);
    }

    private void emitImpact(
            float x,
            float z,
            Color color,
            boolean heavy,
            float sourceX,
            float sourceZ,
            boolean directional) {
        for (int i = 0; i < (heavy ? 24 : 12); i++) {
            Particle p;
            if (particleCount < MAX_PARTICLES) p = particles[particleCount++];
            else {
                p = particles[replacement];
                replacement = (replacement + 1) % MAX_PARTICLES;
            }
            double a =
                    directional
                            ? ImpactDirection.angle(sourceX, sourceZ, x, z, i)
                            : serial++ * 2.399963;
            float force = (1.4f + (i % 5) * .45f) * (directional && heavy ? 1.35f : 1);
            p.x = x;
            p.y = .8f;
            p.z = z;
            p.vx = (float) Math.cos(a) * force;
            p.vz = (float) Math.sin(a) * force;
            p.vy = .8f + (i % 4) * .65f;
            p.total = p.life = .24f + (i % 5) * .08f;
            p.size = heavy ? .14f : .10f;
            p.color = directional && i % 6 == 0 ? Menu.TEXT : color;
        }
    }

    public void parrySpark(float x, float z) {
        for (int i = 0; i < 28; i++) {
            Particle p;
            if (particleCount < MAX_PARTICLES) p = particles[particleCount++];
            else {
                p = particles[replacement];
                replacement = (replacement + 1) % MAX_PARTICLES;
            }
            double a = i * (Math.PI * 2 / 28) + (serial++ * .15);
            float speed = 2.8f + (i % 3) * .8f;
            p.x = x;
            p.y = 1.1f;
            p.z = z;
            p.vx = (float) Math.cos(a) * speed;
            p.vz = (float) Math.sin(a) * speed;
            p.vy = .4f + (i % 4) * .4f;
            p.total = p.life = .18f + (i % 3) * .07f;
            p.size = .12f;
            p.color = i % 2 == 0 ? Menu.GOLD : Menu.TEAL;
        }
    }

    public void begin() {
        BeginBlendMode(BLEND_ADDITIVE);
        rlDisableDepthMask();
    }

    public void end() {
        rlEnableDepthMask();
        EndBlendMode();
    }

    private void glow(
            Camera3D camera, float x, float y, float z, float size, Color color, float alpha) {
        DrawBillboard(camera, soft, v(x, y, z), size, Fade(color, alpha));
    }

    private void decal(
            Texture texture, float x, float z, float radius, float yaw, Color color, float alpha) {
        new Material(ground.materials()).maps().texture(texture);
        // Ring artwork ends at .88 of the texture; keep its outer edge at the gameplay radius.
        float scale = radius / .88f;
        DrawModelEx(
                ground, v(x, .075f, z), v(0, 1, 0), yaw, v(scale, 1, scale), Fade(color, alpha));
    }

    private void ribbon(
            float x,
            float z,
            float facing,
            float radius,
            float halfAngle,
            float height,
            Color color,
            float alpha) {
        for (int layer = 0; layer < 3; layer++)
            for (int i = 0; i < 40; i++) {
                float t = i / 40f, t1 = (i + 1) / 40f;
                double a = Math.toRadians(facing - halfAngle + 2 * halfAngle * t),
                        b = Math.toRadians(facing - halfAngle + 2 * halfAngle * t1);
                float outer = radius + layer * .035f, inner = radius - (.07f + layer * .08f);
                float y = height + (float) Math.sin(t * Math.PI) * height * .3f,
                        y1 = height + (float) Math.sin(t1 * Math.PI) * height * .3f;
                Vector3 p = v(x + (float) Math.sin(a) * inner, y, z + (float) Math.cos(a) * inner),
                        q = v(x + (float) Math.sin(a) * outer, y, z + (float) Math.cos(a) * outer),
                        r = v(x + (float) Math.sin(b) * outer, y1, z + (float) Math.cos(b) * outer),
                        s = v(x + (float) Math.sin(b) * inner, y1, z + (float) Math.cos(b) * inner);
                Color c =
                        Fade(
                                color,
                                alpha
                                        * (float) Math.sin((t + .0125) * Math.PI)
                                        / (1 + layer * 1.8f));
                DrawTriangle3D(p, q, r, c);
                DrawTriangle3D(p, r, s, c);
                DrawTriangle3D(r, q, p, c);
                DrawTriangle3D(s, r, p, c);
            }
    }

    public void drawBlade() {
        if (blade.size() < 2) return;
        rlSetTexture(soft.id());
        rlBegin(RL_QUADS);
        for (int i = 1; i < blade.size(); i++) {
            float u0 = (i - 1f) / (blade.size() - 1), u1 = i / (float) (blade.size() - 1);
            // Vertex alpha and the soft texture feather both the hilt edge and the tail.
            bladeVertex(i - 1, false, u0);
            bladeVertex(i - 1, true, u0);
            bladeVertex(i, true, u1);
            bladeVertex(i, false, u1);
            bladeVertex(i, false, u1);
            bladeVertex(i, true, u1);
            bladeVertex(i - 1, true, u0);
            bladeVertex(i - 1, false, u0);
        }
        rlEnd();
        rlSetTexture(0);
    }

    private void bladeVertex(int sample, boolean tip, float u) {
        float fade = blade.opacity(sample);
        int alpha = Math.round(255 * fade * fade * (tip ? .52f : .02f));
        rlColor4ub(
                (byte) (heavyBlade ? 250 : 192),
                (byte) (heavyBlade ? 218 : 218),
                (byte) (heavyBlade ? 170 : 235),
                (byte) alpha);
        rlTexCoord2f(u, tip ? .82f : .12f);
        int offset = tip ? 3 : 0;
        rlVertex3f(
                blade.coordinate(sample, offset),
                blade.coordinate(sample, offset + 1),
                blade.coordinate(sample, offset + 2));
    }

    public void shield(Camera3D camera, float x, float z, float time, Color color) {
        DrawBillboard(camera, rune, v(x, 1.1f, z), 2.2f, Fade(color, .6f));
        glow(camera, x, 1.1f, z, 2.7f, color, .12f);
    }

    public void charge(float x, float z, float amount, Color color) {
        decal(rune, x, z, .7f + amount * .3f, amount * 80, color, .3f);
    }

    public void enemy(Camera3D camera, Enemy e, float time) {
        float clipPhase = Math.max(0, Math.min(1, 1 - e.attackTime / e.attackDuration()));
        float phase = AttackTelegraph.progress(e.attackTime, e.attackDuration());
        if (e.healingTarget != null) {
            for (int i = 0; i < 18; i++) {
                float t = (i / 18f + time * .7f) % 1;
                float x = e.x + (e.healingTarget.x - e.x) * t,
                        z = e.z + (e.healingTarget.z - e.z) * t;
                glow(
                        camera,
                        x + (float) Math.sin(t * 16 + time * 4) * .18f,
                        1.3f + (float) Math.sin(t * Math.PI) * .8f,
                        z,
                        .3f,
                        Menu.TEAL,
                        (float) Math.sin(t * Math.PI) * .7f);
            }
            decal(rune, e.healingTarget.x, e.healingTarget.z, 1.05f, time * 20, Menu.TEAL, .35f);
            return;
        }
        if (e.attackTime <= 0) return;
        Color color = e.boss() ? (e.bossMove.parryable ? Menu.GOLD : Menu.RED) : Menu.GOLD;
        if (!e.boss() || e.bossMove == BossMove.CLEAVE) {
            if (e.ranged()) {
                if (e.impactPending) rangedWindup(camera, e, phase);
            } else {
                float angle = e.boss() ? 100 : 65;
                ribbon(e.x, e.z, e.facing, e.meleeRange(), angle, .07f, color, .18f + phase * .20f);
                if (e.attackTime <= .25f)
                    ribbon(
                            e.x,
                            e.z,
                            e.facing - 50 + clipPhase * 100,
                            e.meleeRange() * .82f,
                            angle * .6f,
                            .9f,
                            color,
                            .9f * (1 - clipPhase));
            }
        } else
            switch (e.bossMove) {
                case SLAM -> {
                    decal(ring, e.attackX, e.attackZ, 3.4f, 0, color, .35f);
                    decal(
                            ring,
                            e.attackX,
                            e.attackZ,
                            Math.max(.2f, 3.4f * phase),
                            0,
                            color,
                            .55f + phase * .35f);
                    decal(rune, e.attackX, e.attackZ, 3.4f, time * 9, color, .25f + phase * .4f);
                    glow(camera, e.x, 1.9f, e.z, 1.3f, color, phase * .25f);
                }
                case CHARGE -> {
                    float dx = e.chargeEndX - e.x, dz = e.chargeEndZ - e.z;
                    for (int i = 0; i < 18; i++) {
                        float t = (i / 18f + time * 1.2f) % 1;
                        glow(camera, e.x + dx * t, .14f, e.z + dz * t, .45f, color, .12f + .3f * t);
                    }
                    decal(ring, e.chargeEndX, e.chargeEndZ, 1.45f, 0, color, .25f);
                }
                case SIGILS -> {
                    glow(camera, e.x, 3.5f, e.z, 1.8f, Menu.TEAL, .45f);
                    for (int i = 0; i < 6; i++) {
                        double a = time * 2 + i * Math.PI / 3;
                        glow(
                                camera,
                                e.x + (float) Math.cos(a) * .65f,
                                3.5f,
                                e.z + (float) Math.sin(a) * .65f,
                                .4f,
                                Menu.GOLD,
                                .5f);
                    }
                }
                default -> {}
            }
    }

    /**
     * The marks follow the committed firing line, including the archer's lead on a moving target.
     */
    private void rangedWindup(Camera3D camera, Enemy e, float phase) {
        Color tint = e.variant == 4 ? Menu.TEAL : Menu.GOLD;
        glow(camera, e.x, 1.4f, e.z, .35f + phase * .6f, tint, .25f + phase * .45f);
        float dx = e.attackX - e.x, dz = e.attackZ - e.z;
        float length = (float) Math.hypot(dx, dz);
        if (length < .6f) return;
        dx /= length;
        dz /= length;
        float start = Math.min(.65f, length * .3f);
        Vector3 tip = v(e.attackX, .12f, e.attackZ);
        DrawCylinderEx(
                v(e.x + dx * start, .12f, e.z + dz * start),
                tip,
                .035f,
                .022f,
                5,
                Fade(tint, .28f + phase * .32f));
        // The leading chevron travels to the aim point as the shot approaches release.
        float t = start + (length - start) * phase;
        float x = e.x + dx * t, z = e.z + dz * t;
        Vector3 arrow = v(x, .13f, z);
        for (int side = -1; side <= 1; side += 2)
            DrawCylinderEx(
                    v(x - dx * .3f + side * dz * .19f, .13f, z - dz * .3f - side * dx * .19f),
                    arrow,
                    .035f,
                    .045f,
                    5,
                    Fade(tint, .82f));
    }

    public void hazard(Camera3D camera, BossHazard h, float time) {
        Color color = h.kind == BossHazard.Kind.SIGIL ? Menu.color(182, 105, 255) : Menu.GOLD;
        if (h.delay > 0) {
            decal(
                    rune,
                    h.x,
                    h.z,
                    h.radius,
                    time * 14,
                    color,
                    .3f + .1f * (float) Math.sin(time * 9));
            return;
        }
        decal(
                h.kind == BossHazard.Kind.SIGIL ? rune : ring,
                h.x,
                h.z,
                h.radius,
                time * 8,
                color,
                h.kind == BossHazard.Kind.SIGIL ? .8f : .42f);
        int count = h.kind == BossHazard.Kind.SIGIL ? 18 : 32;
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2 / count;
            float r = h.radius * (h.kind == BossHazard.Kind.SIGIL ? .3f + (i % 5) * .14f : 1),
                    rise =
                            h.kind == BossHazard.Kind.SIGIL
                                    ? h.age * 5 + (i % 4) * .15f
                                    : .15f + (i % 3) * .15f;
            glow(
                    camera,
                    h.x + (float) Math.cos(a) * r,
                    rise,
                    h.z + (float) Math.sin(a) * r,
                    .3f + (i % 3) * .16f,
                    color,
                    Math.max(0, .65f - h.age * .4f));
        }
    }

    public void projectile(
            Camera3D camera,
            float x,
            float z,
            float vx,
            float vz,
            boolean magic,
            Color color,
            float time) {
        projectile(camera, x, z, vx, vz, magic, color, time, 1);
    }

    public void projectile(
            Camera3D camera,
            float x,
            float z,
            float vx,
            float vz,
            boolean magic,
            Color color,
            float time,
            float height) {
        float speed = (float) Math.hypot(vx, vz);
        if (speed < .001f) return;
        float sideX = -vz / speed, sideZ = vx / speed;
        if (magic) {
            for (int i = 5; i >= 0; i--)
                glow(
                        camera,
                        x - vx * i * .026f,
                        height + (float) Math.sin(time * 14 + i) * .035f,
                        z - vz * i * .026f,
                        .24f + (5 - i) * .07f,
                        color,
                        .20f + (5 - i) * .08f);
            // Wind paired sparks around the flight axis so the cast reads from either side.
            for (int i = 0; i < 7; i++) {
                float phase = time * 18 - i * .77f;
                float radius = .13f * (1 - i / 9f);
                float side = radius * (float) Math.cos(phase);
                float rise = radius * (float) Math.sin(phase);
                float tx = x - vx * i * .015f, tz = z - vz * i * .015f;
                glow(
                        camera,
                        tx + sideX * side,
                        height + rise,
                        tz + sideZ * side,
                        .12f,
                        Menu.TEXT,
                        .75f - i * .08f);
                glow(
                        camera,
                        tx - sideX * side,
                        height - rise,
                        tz - sideZ * side,
                        .15f,
                        color,
                        .85f - i * .08f);
            }
            glow(camera, x, height, z, .18f, Menu.TEXT, .9f);
        } else {
            DrawCylinderEx(
                    v(x - vx * .045f, height, z - vz * .045f),
                    v(x, height, z),
                    .018f,
                    .012f,
                    5,
                    Menu.TEXT);
            DrawCylinderEx(
                    v(x, height, z), v(x + vx * .012f, height, z + vz * .012f), .05f, 0, 5, color);
            // Swept vanes meet the shaft ahead of its tail.
            for (int sign = -1; sign <= 1; sign += 2) {
                DrawCylinderEx(
                        v(
                                x - vx * .05f + sign * sideX * .09f,
                                height + .045f,
                                z - vz * .05f + sign * sideZ * .09f),
                        v(x - vx * .032f, height, z - vz * .032f),
                        .028f,
                        .008f,
                        4,
                        color);
            }
            DrawCylinderEx(
                    v(x - vx * .12f, height, z - vz * .12f),
                    v(x - vx * .049f, height, z - vz * .049f),
                    .004f,
                    .029f,
                    5,
                    Fade(color, .52f));
            for (int i = 1; i < 4; i++)
                glow(camera, x - vx * i * .025f, height, z - vz * i * .025f, .10f, color, .18f / i);
        }
    }

    public void particles(Camera3D camera) {
        drawRescue(camera);
        for (int i = 0; i < particleCount; i++) {
            Particle p = particles[i];
            float fade = p.life / p.total;
            DrawBillboard(
                    camera,
                    spark,
                    v(p.x, Math.max(.08f, p.y), p.z),
                    p.size * (.6f + fade),
                    Fade(p.color, fade));
        }
    }

    @Override
    public void close() {
        UnloadModel(ground);
        for (Texture t : new Texture[] {soft, spark, rune, ring}) UnloadTexture(t);
    }
}
