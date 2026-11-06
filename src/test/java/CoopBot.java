/** Legitimate player intentions for deterministic campaign/network soak checks. No state edits. */
final class CoopBot {
    static CoopInput input(CoopState s, int slot, long sequence) {
        var me = s.heroes().stream().filter(h -> h.slot() == slot).findFirst().orElse(null);
        if (me == null) return new CoopInput(sequence, 0, 0, 0, 0);
        if (s.phase() != CoopState.Phase.PLAYING) {
            int flags = 0;
            if (me.credits() > 0)
                flags |=
                        me.hero() == 0 && me.vitality() == 0
                                ? CoopInput.VITALITY
                                : me.power() < 2 ? CoopInput.POWER : CoopInput.VITALITY;
            if (!me.ready() && s.phase() != CoopState.Phase.VICTORY) flags |= CoopInput.READY;
            return new CoopInput(sequence, 0, 0, 0, flags);
        }
        if (me.health() <= 0) return new CoopInput(sequence, 0, 0, me.facing(), 0);
        var partner = s.heroes().stream().filter(h -> h.slot() != slot).findFirst().orElse(null);
        CoopState.Mob nearest = null;
        double best = Double.MAX_VALUE;
        for (var e : s.enemies())
            if (e.health() > 0) {
                double d = Math.hypot(e.x() - me.x(), e.z() - me.z()),
                        score = d * .3 + Math.hypot(e.x(), e.z()) * 2.5;
                if (score < best) {
                    best = score;
                    nearest = e;
                }
            }
        float tx = me.x(), tz = me.z(), aim = me.facing();
        int flags = 0;
        if (nearest != null) {
            double dx = nearest.x() - me.x(),
                    dz = nearest.z() - me.z(),
                    distance = Math.max(.01, Math.hypot(dx, dz));
            aim = (float) Math.toDegrees(Math.atan2(dx, dz));
            float desired = me.hero() == 0 ? 1.7f : 5.7f;
            if (distance > desired + .3) {
                tx = nearest.x() - (float) (dx / distance) * desired;
                tz = nearest.z() - (float) (dz / distance) * desired;
            } else if (me.hero() != 0 && distance < 4) {
                tx = me.x() - (float) (dx / distance) * 2;
                tz = me.z() - (float) (dz / distance) * 2;
            }
            if (distance < (me.hero() == 0 ? 3 : 9.5f))
                flags |= sequence % 220 < 55 ? CoopInput.HEAVY : CoopInput.LIGHT;
            if (nearest.attack() > .25f
                    && nearest.attack() < .46f
                    && distance < (nearest.variant() == 5 ? 5 : 3)
                    && (nearest.variant() != 5
                            || Entities.BossMove.values()[nearest.move()].parryable))
                flags |= CoopInput.PARRY;
            if (nearest.charge() > 0 && distance < 5) {
                tx = me.x() - (float) (dz / distance) * 4;
                tz = me.z() + (float) (dx / distance) * 4;
                flags |= CoopInput.DODGE;
            }
        }
        if (me.health() < me.maxHealth() * .5f) {
            tx = slot == 0 ? -2 : 2;
            tz = 4;
            flags &= ~(CoopInput.LIGHT | CoopInput.HEAVY);
            if (me.potions() > 0) flags |= CoopInput.POTION;
            else if (Math.hypot(me.x() - tx, me.z() - tz) > 3) flags |= CoopInput.SPRINT;
        }
        if (s.baseHealth() < s.baseMax() - 80
                && me.potions() > 1
                && (nearest == null || Math.hypot(nearest.x(), nearest.z()) > 8)) {
            tx = slot == 0 ? -2 : 2;
            tz = 4;
            if (Math.hypot(me.x() - tx, me.z() - tz) < .4) {
                tx = me.x();
                tz = me.z();
                flags = CoopInput.INTERACT;
            }
        }
        if (partner != null
                && partner.health() <= 0
                && me.health() > me.maxHealth() * .6f
                && s.enemies().stream()
                        .noneMatch(e ->
                                e.health() > 0
                                        && Math.hypot(e.x() - partner.x(), e.z() - partner.z())
                                                < 8)) {
            tx = partner.x();
            tz = partner.z();
            if (Math.hypot(tx - me.x(), tz - me.z()) < 2.3) {
                tx = me.x();
                tz = me.z();
                flags |= CoopInput.INTERACT;
            }
        }
        for (var d : s.dangers())
            if (d.delay() < .8f && Math.hypot(d.x() - me.x(), d.z() - me.z()) < d.radius() + 1) {
                float dx = me.x() - d.x(),
                        dz = me.z() - d.z(),
                        len = Math.max(.01f, (float) Math.hypot(dx, dz));
                if (len < .1f) dx = slot == 0 ? 1 : -1;
                tx = me.x() + dx / Math.max(.2f, len) * 4;
                tz = me.z() + dz / Math.max(.2f, len) * 4;
                if (d.delay() < .4f) flags |= CoopInput.DODGE;
            }
        for (var b : s.bolts())
            if (b.hostile() && Math.hypot(b.x() - me.x(), b.z() - me.z()) < 2) {
                float length = Math.max(.1f, (float) Math.hypot(b.vx(), b.vz()));
                tx = me.x() - b.vz() / length * 3;
                tz = me.z() + b.vx() / length * 3;
                flags |= CoopInput.DODGE;
            }
        float dx = tx - me.x(), dz = tz - me.z(), len = (float) Math.hypot(dx, dz);
        if (len > .2) {
            dx /= len;
            dz /= len;
            var steering = ArenaNavigation.steering(s.chapter(), me.x(), me.z(), tx, tz);
            dx += steering.x();
            dz += steering.z();
            len = Math.max(1, (float) Math.hypot(dx, dz));
            dx /= len;
            dz /= len;
        } else dx = dz = 0;
        return new CoopInput(sequence, dx, dz, aim, flags);
    }
}
