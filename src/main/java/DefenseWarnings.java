import Entities.CombatDiscipline;
import Entities.Enemy;

import java.util.List;

/**
 * Read-only forecast for shots aimed at an ally. Re-evaluate against the current positions so a
 * survivor moving out of the firing line clears the warning immediately.
 */
final class DefenseWarnings {
    record Threat(
            float x,
            float z,
            float targetX,
            float targetZ,
            float impactIn,
            boolean magic,
            boolean flying) {
        String label() {
            if (flying) return magic ? "HEX BOLT INCOMING" : "ARROW INCOMING";
            return magic ? "HEXER AIMING" : "ARCHER AIMING";
        }
    }

    private Threat beacon, objective;
    private boolean parryNow;

    Threat beacon() {
        return beacon;
    }

    Threat objective() {
        return objective;
    }

    boolean parryNow() {
        return parryNow;
    }

    void update(
            int chapter,
            List<Enemy> enemies,
            List<Projectile> shots,
            FieldEvent event,
            float px,
            float pz,
            float facing) {
        beacon = objective = null;
        parryNow = false;
        for (Enemy enemy : enemies) {
            if (!enemy.impactPending
                    || !enemy.ranged()
                    || enemy.health <= 0
                    || enemy.attackTime <= 0
                    || enemy.targetsPlayer
                    || enemy.healingTarget != null) continue;
            float dx = enemy.attackX - enemy.x, dz = enemy.attackZ - enemy.z;
            float distance = (float) Math.hypot(dx, dz);
            if (distance < .001f) continue;
            boolean magic = enemy.variant == 4;
            float speed = Projectile.speed(magic);
            forecast(
                    chapter,
                    event,
                    px,
                    pz,
                    facing,
                    enemy.x,
                    enemy.z,
                    dx / distance * speed,
                    dz / distance * speed,
                    Projectile.LIFETIME,
                    Math.max(0, enemy.attackTime - .25f),
                    enemy.targetsTraveler,
                    magic,
                    false);
        }
        for (Projectile shot : shots) {
            if (!shot.playerTarget)
                forecast(
                        chapter,
                        event,
                        px,
                        pz,
                        facing,
                        shot.x,
                        shot.z,
                        shot.vx,
                        shot.vz,
                        shot.life,
                        0,
                        shot.travelerTarget,
                        shot.magic,
                        true);
        }
    }

    private void forecast(
            int chapter,
            FieldEvent event,
            float px,
            float pz,
            float facing,
            float x,
            float z,
            float vx,
            float vz,
            float life,
            float delay,
            boolean traveler,
            boolean magic,
            boolean flying) {
        if (life <= 0 || traveler && !event.vulnerable()) return;
        float tx = traveler ? event.x() : 0, tz = traveler ? event.z() : 0;
        float dx = vx * life, dz = vz * life;
        float fraction =
                Projectile.contact(
                        x,
                        z,
                        dx,
                        dz,
                        tx,
                        tz,
                        traveler ? Projectile.OBJECTIVE_RADIUS : Projectile.BEACON_RADIUS);
        if (!Float.isFinite(fraction)) return;
        float endX = x + dx * fraction, endZ = z + dz * fraction;
        if (ArenaNavigation.projectileFraction(chapter, x, z, endX, endZ) < 1) return;
        float impact = delay + fraction * life;
        Threat current = traveler ? objective : beacon;
        if (current == null || impact < current.impactIn) {
            Threat next = new Threat(x, z, tx, tz, impact, magic, flying);
            if (traveler) objective = next;
            else beacon = next;
        }
        float guard = Projectile.contact(x, z, dx, dz, px, pz, Projectile.GUARD_RADIUS);
        if (guard < fraction
                && delay + guard * life < CombatDiscipline.PARRY_WINDOW
                && Combat.inArc(px, pz, facing, px - vx, pz - vz, 1000, 75)) parryNow = true;
    }
}
