import Entities.*;

import java.util.*;

/** Fixed-step, host-authoritative cooperative campaign. No window, GPU, keyboard or disk access. */
public final class CoopWorld {
    private static final class Hero {
        int slot;
        String name;
        HeroClass type;
        boolean connected, ready, paused;
        CoopInput input = CoopInput.NONE;
        long sequence = -1;
        int previousButtons, pressed;
        boolean heavyReleased;
        float inputAge,
                x,
                z,
                facing,
                health,
                stamina,
                velocityX,
                velocityZ,
                attack,
                duration,
                impact,
                charge,
                dodge,
                dodgeCooldown,
                parry,
                parryCooldown,
                recovery,
                hurt,
                revive,
                repair,
                regenDelay;
        boolean heavy, pending, perfect, sprintExhausted;
        int combo, potions = 3, credits = 1, vitality, power, endurance;

        float maxHealth() {
            return type.health + vitality * 25;
        }

        float maxStamina() {
            return 100 + endurance * 25;
        }
    }

    private static final class Mob {
        int id, wave, target = -1, chargeMask;
        Enemy enemy;

        Mob(int id, int wave, Enemy enemy) {
            this.id = id;
            this.wave = wave;
            this.enemy = enemy;
        }
    }

    private record Shot(int id, int owner, HeroShot shot) {}

    private record Hostile(int id, Projectile shot) {}

    private static final class Danger {
        int id, mask;
        BossHazard hazard;

        Danger(int id, BossHazard hazard) {
            this.id = id;
            this.hazard = hazard;
        }
    }

    private final Hero[] heroes = new Hero[2];
    private final int[][] savedBuild = new int[2][4];
    private final ArrayList<Mob> enemies = new ArrayList<>();
    private final ArrayList<Shot> shots = new ArrayList<>();
    private final ArrayList<Hostile> hostile = new ArrayList<>();
    private final ArrayList<Danger> dangers = new ArrayList<>();
    private final Random random;
    private final Difficulty difficulty;
    private CoopState.Phase phase = CoopState.Phase.LOBBY;
    private final float[] volley = new float[3];
    private long tick;
    private int chapter, wave, queued, total, kills, nextId = 1;
    private float rest = 3, spawnClock, baseHealth = CoopBalance.BASE_HEALTH, wardClock;

    public CoopWorld(Difficulty difficulty, long seed) {
        this.difficulty = difficulty;
        random = new Random(seed);
        for (int[] build : savedBuild) build[3] = 1;
    }

    public synchronized void attach(int slot, String name, HeroClass type) {
        if (slot < 0
                || slot > 1
                || name.isBlank()
                || name.length() > 24
                || name.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Use a name of 1-24 letters or characters");
        Hero p = heroes[slot];
        if (p != null && p.connected)
            throw new IllegalArgumentException("That player slot is occupied");
        if (p != null && p.type != type)
            throw new IllegalArgumentException(
                    "Reconnect using your original class: " + p.type.label);
        if (p == null) {
            p = new Hero();
            p.slot = slot;
            p.type = type;
            p.name = name;
            p.x = slot == 0 ? -2 : 2;
            p.z = 4;
            p.vitality = savedBuild[slot][0];
            p.power = savedBuild[slot][1];
            p.endurance = savedBuild[slot][2];
            p.credits = savedBuild[slot][3];
            p.health = p.maxHealth();
            p.stamina = p.maxStamina();
            heroes[slot] = p;
        }
        p.connected = true;
        p.paused = false;
        p.ready = false;
        p.input = CoopInput.NONE;
        p.previousButtons = p.pressed = 0;
        p.sequence = -1;
        p.inputAge = 0;
        p.charge = 0;
    }

    public synchronized void detach(int slot) {
        if (heroes[slot] != null) {
            heroes[slot].connected = false;
            heroes[slot].ready = false;
            heroes[slot].input = CoopInput.NONE;
            heroes[slot].charge = 0;
        }
    }

    public synchronized void input(int slot, CoopInput input) {
        Hero p = heroes[slot];
        if (p == null || !p.connected || input.sequence() <= p.sequence) return;
        p.sequence = input.sequence();
        p.pressed |= input.buttons() & ~p.previousButtons;
        if ((p.previousButtons & CoopInput.HEAVY) != 0 && (input.buttons() & CoopInput.HEAVY) == 0)
            p.heavyReleased = true;
        p.previousButtons = input.buttons();
        p.input = input;
        p.inputAge = 0;
    }

    private boolean together() {
        return heroes[0] != null && heroes[1] != null && heroes[0].connected && heroes[1].connected;
    }

    private boolean paused() {
        return !together()
                || heroes[0].paused
                || heroes[1].paused
                || heroes[0].inputAge > 3
                || heroes[1].inputAge > 3;
    }

    public synchronized void tick(float dt) {
        if (!Float.isFinite(dt) || dt <= 0 || dt > .051f)
            throw new IllegalArgumentException("Invalid simulation step");
        tick++;
        int[] presses = new int[2];
        for (Hero p : heroes)
            if (p != null) {
                p.inputAge += dt;
                if (p.inputAge > .5f) {
                    p.input = CoopInput.NONE;
                    p.charge = 0;
                    p.heavyReleased = false;
                }
                int pressed = p.pressed;
                p.pressed = 0;
                presses[p.slot] = pressed;
                if ((pressed & CoopInput.PAUSE) != 0 && phase == CoopState.Phase.PLAYING)
                    p.paused = !p.paused;
                if (phase != CoopState.Phase.PLAYING && phase != CoopState.Phase.VICTORY) {
                    if (p.credits > 0) {
                        if ((pressed & CoopInput.VITALITY) != 0) {
                            p.vitality++;
                            p.credits--;
                            p.health = p.maxHealth();
                        } else if ((pressed & CoopInput.POWER) != 0) {
                            p.power++;
                            p.credits--;
                        } else if ((pressed & CoopInput.ENDURANCE) != 0) {
                            p.endurance++;
                            p.credits--;
                            p.stamina = p.maxStamina();
                        }
                    }
                    if ((pressed & CoopInput.READY) != 0) p.ready = !p.ready;
                }
                // Presses are consumed even while paused, so resuming cannot unleash buffered
                // attacks.
            }
        if (phase == CoopState.Phase.PLAYING && !paused())
            for (Hero p : heroes) updateHero(p, presses[p.slot], dt);
        if (phase != CoopState.Phase.PLAYING) {
            if (phase != CoopState.Phase.VICTORY
                    && together()
                    && heroes[0].ready
                    && heroes[1].ready) {
                if (phase == CoopState.Phase.INTERMISSION) chapter++;
                startChapter();
            }
            return;
        }
        if (paused()) {
            for (Hero p : heroes)
                if (p != null) {
                    p.velocityX = p.velocityZ = 0;
                    p.charge = 0;
                    p.heavyReleased = false;
                }
            return;
        }
        for (int i = 0; i < volley.length; i++) volley[i] = Math.max(0, volley[i] - dt);
        revive(dt);
        updateEnemies(dt);
        updateShots(dt);
        updateDangers(dt);
        if (baseHealth <= 0 || heroes[0].health <= 0 && heroes[1].health <= 0) {
            finish(CoopState.Phase.DEFEAT);
            return;
        }
        if (queued > 0) {
            spawnClock -= dt;
            if (spawnClock <= 0) {
                spawn();
                queued--;
                spawnClock = 1.05f * difficulty.cadence;
            }
        } else if (enemies.stream().noneMatch(m -> m.enemy.health > 0)) {
            if (wave >= Campaign.CHAPTERS[chapter].last()) {
                finish(chapter == 3 ? CoopState.Phase.VICTORY : CoopState.Phase.INTERMISSION);
                if (phase == CoopState.Phase.INTERMISSION) for (Hero p : heroes) p.credits++;
            } else {
                rest -= dt;
                if (rest <= 0) startWave(wave + 1);
            }
        }
        wardClock -= dt;
        if (wardClock <= 0) {
            Enemy target = SanctuaryWard.target(enemies.stream().map(m -> m.enemy).toList());
            if (target != null)
                target.hit(SanctuaryWard.damage(2 + chapter, target.health), 0, 0, 0);
            wardClock = 4;
        }
    }

    private void finish(CoopState.Phase result) {
        phase = result;
        shots.clear();
        hostile.clear();
        dangers.clear();
        for (Hero p : heroes) {
            p.ready = false;
            p.charge = 0;
            p.attack = 0;
            p.paused = false;
        }
    }

    private void startChapter() {
        enemies.clear();
        shots.clear();
        hostile.clear();
        dangers.clear();
        baseHealth =
                Math.min(
                        CoopBalance.BASE_HEALTH,
                        Math.max(baseHealth + 130, CoopBalance.BASE_HEALTH * .65f));
        if (phase == CoopState.Phase.DEFEAT || phase == CoopState.Phase.LOBBY)
            baseHealth = CoopBalance.BASE_HEALTH;
        for (Hero p : heroes) {
            p.health = p.maxHealth();
            p.stamina = p.maxStamina();
            p.x = p.slot == 0 ? -2 : 2;
            p.z = 4;
            p.ready = p.paused = false;
            p.potions = 3;
            p.attack = p.dodge = p.charge = p.revive = 0;
            p.pending = false;
            p.input = CoopInput.NONE;
            p.previousButtons = 0;
        }
        phase = CoopState.Phase.PLAYING;
        startWave(Campaign.CHAPTERS[chapter].first());
        spawnClock = 3;
    }

    private void startWave(int next) {
        wave = next;
        total = queued = CoopBalance.count(wave, difficulty);
        rest = 8;
        spawnClock = 1.5f;
    }

    private void spawn() {
        int gate = chapter == 2 ? (random.nextBoolean() ? 0 : 2) : random.nextInt(4);
        double a = gate * Math.PI / 2 + (random.nextFloat() - .5f) * .5;
        int variant =
                Campaign.CHAPTERS[chapter].bossOnly()
                        ? 5
                        : WaveFormation.variant(wave, total - queued);
        Enemy e =
                new Enemy(
                        (float) Math.sin(a) * 21,
                        (float) Math.cos(a) * 21,
                        wave,
                        variant,
                        random.nextFloat(),
                        difficulty,
                        CoopBalance.healthScale(variant == 5));
        enemies.add(new Mob(nextId++, wave, e));
    }

    private void updateHero(Hero p, int pressed, float dt) {
        p.hurt = Math.max(0, p.hurt - dt);
        p.recovery = Math.max(0, p.recovery - dt);
        p.dodgeCooldown = Math.max(0, p.dodgeCooldown - dt);
        p.parryCooldown = Math.max(0, p.parryCooldown - dt);
        p.parry = Math.max(0, p.parry - dt);
        p.regenDelay = Math.max(0, p.regenDelay - dt);
        if (p.health <= 0) {
            p.velocityX = p.velocityZ = 0;
            p.charge = p.repair = 0;
            return;
        }
        Hero partner = heroes[1 - p.slot];
        boolean repairing =
                (p.input.buttons() & CoopInput.INTERACT) != 0
                        && Math.hypot(p.x, p.z) < 6
                        && partner.health > 0
                        && p.hurt <= 0
                        && p.potions > 0
                        && baseHealth < CoopBalance.BASE_HEALTH
                        && p.attack <= 0
                        && p.charge <= 0
                        && p.dodge <= 0
                        && Math.hypot(p.input.moveX(), p.input.moveZ()) < .1;
        p.repair = repairing ? p.repair + dt : 0;
        if (p.repair >= 3) {
            p.potions--;
            baseHealth = Math.min(CoopBalance.BASE_HEALTH, baseHealth + 100);
            p.repair = 0;
        }
        float oldAttack = p.attack;
        p.attack = Math.max(0, p.attack - dt);
        if (p.pending && oldAttack > p.duration - p.impact && p.attack <= p.duration - p.impact) {
            p.pending = false;
            impact(p);
        }
        boolean canAct = p.attack <= 0 && p.dodge <= 0 && p.recovery <= 0;
        if ((pressed & CoopInput.POTION) != 0 && p.potions > 0 && p.health < p.maxHealth()) {
            p.health = Math.min(p.maxHealth(), p.health + 60);
            p.potions--;
        }
        if ((pressed & CoopInput.DODGE) != 0
                && p.dodgeCooldown <= 0
                && p.stamina >= 22
                && p.recovery <= 0) {
            p.dodge = .34f;
            p.dodgeCooldown = 1.2f;
            p.attack = 0;
            p.pending = false;
            p.charge = 0;
            spend(p, 22);
            float len = (float) Math.hypot(p.input.moveX(), p.input.moveZ());
            p.velocityX =
                    len > .01
                            ? p.input.moveX() / len * 10
                            : (float) Math.sin(Math.toRadians(p.facing)) * 10;
            p.velocityZ =
                    len > .01
                            ? p.input.moveZ() / len * 10
                            : (float) Math.cos(Math.toRadians(p.facing)) * 10;
        }
        if ((pressed & CoopInput.PARRY) != 0 && canAct && p.parryCooldown <= 0 && p.stamina >= 12) {
            spend(p, 12);
            p.parry = .24f;
            p.parryCooldown = .8f;
            p.recovery = .34f;
        }
        if (p.attack <= 0 && p.dodge <= 0) p.facing = p.input.aim();
        boolean heavyDown = (p.input.buttons() & CoopInput.HEAVY) != 0;
        if (heavyDown && canAct) p.charge = Math.min(1.6f, p.charge + dt);
        if (p.heavyReleased) {
            if (p.charge >= .15f && canAct) attack(p, true);
            p.charge = 0;
            p.heavyReleased = false;
        }
        if (((p.input.buttons() | pressed) & CoopInput.LIGHT) != 0 && !heavyDown && canAct)
            attack(p, false);
        if (p.dodge > 0) {
            p.dodge = Math.max(0, p.dodge - dt);
        } else {
            float len = Math.max(1, (float) Math.hypot(p.input.moveX(), p.input.moveZ())),
                    speed = p.type.speed * (p.attack > 0 ? .38f : p.charge > 0 ? .55f : 1);
            if (p.stamina >= 25) p.sprintExhausted = false;
            if ((p.input.buttons() & CoopInput.SPRINT) != 0
                    && (p.type == HeroClass.MAGE || !p.sprintExhausted)
                    && p.attack <= 0
                    && p.charge <= 0
                    && Math.hypot(p.input.moveX(), p.input.moveZ()) > .1) {
                if (p.type == HeroClass.MAGE || p.stamina > 1) {
                    speed *= 1.35f;
                    if (p.type != HeroClass.MAGE) spend(p, Math.min(p.stamina, 12 * dt));
                } else p.sprintExhausted = true;
            }
            p.velocityX = p.input.moveX() / len * speed;
            p.velocityZ = p.input.moveZ() / len * speed;
        }
        ArenaNavigation.Position moved =
                ArenaNavigation.move(
                        chapter, p.x, p.z, p.x + p.velocityX * dt, p.z + p.velocityZ * dt, .42f);
        p.x = moved.x();
        p.z = moved.z();
        if (p.regenDelay <= 0) p.stamina = Math.min(p.maxStamina(), p.stamina + 24 * dt);
        if (Math.hypot(p.x, p.z) < 6) p.health = Math.min(p.maxHealth(), p.health + 12 * dt);
    }

    private void spend(Hero p, float amount) {
        p.stamina -= amount;
        p.regenDelay = .65f;
    }

    private void attack(Hero p, boolean heavy) {
        float cost = heavy ? 32 : 14;
        if (p.stamina < cost || p.attack > 0 || p.dodge > 0 || p.recovery > 0) return;
        spend(p, cost);
        p.heavy = heavy;
        p.perfect = heavy && p.charge >= .75f && p.charge <= 1.05f;
        p.combo = p.combo % 3 + 1;
        Combat.Attack a = p.type.attack(p.combo, heavy);
        p.duration = a.duration();
        p.impact = a.impact();
        p.attack = p.duration;
        p.pending = true;
    }

    private int damage(Hero p) {
        return Math.round(
                (p.type == HeroClass.ARCHER ? 23 : 25)
                        * (1 + .15f * p.power)
                        * (1 + chapter * .2f));
    }

    private void impact(Hero p) {
        Combat.Attack a = p.type.attack(p.combo, p.heavy);
        if (p.perfect) a = Combat.scale(a, 1.35f, a.label());
        if (p.type == HeroClass.WARRIOR) {
            for (Mob m : enemies)
                if (m.enemy.health > 0
                        && Combat.inArc(
                                p.x, p.z, p.facing, m.enemy.x, m.enemy.z, a.range(), a.halfAngle()))
                    m.enemy.hit(
                            Math.round(damage(p) * a.multiplier()), p.x, p.z, p.heavy ? .7f : .3f);
        } else {
            float x = p.x + (float) Math.sin(Math.toRadians(p.facing)) * .6f,
                    z = p.z + (float) Math.cos(Math.toRadians(p.facing)) * .6f,
                    facing = p.facing;
            Hero partner = heroes[1 - p.slot];
            boolean support = p.type == HeroClass.MAGE && p.heavy;
            boolean aimsSupport =
                    support
                            && (partner.health > 0
                                            && partner.health < partner.maxHealth()
                                            && Combat.inArc(
                                                    p.x, p.z, p.facing, partner.x, partner.z,
                                                    a.range(), 12)
                                    || baseHealth < CoopBalance.BASE_HEALTH
                                            && Combat.inArc(
                                                    p.x, p.z, p.facing, 0, 0, a.range(), 12));
            Enemy target =
                    aimsSupport
                            ? null
                            : RangedAim.target(
                                    p.x,
                                    p.z,
                                    p.facing,
                                    a.range(),
                                    enemies.stream().map(m -> m.enemy).toList(),
                                    e ->
                                            ArenaNavigation.projectileFraction(
                                                            chapter, x, z, e.x, e.z)
                                                    >= 1);
            if (target != null)
                facing = (float) Math.toDegrees(Math.atan2(target.x - x, target.z - z));
            HeroShot shot = new HeroShot(x, z, facing, a, damage(p), p.type == HeroClass.MAGE);
            shot.restorative = support;
            shot.heavy = p.heavy;
            shots.add(new Shot(nextId++, p.slot, shot));
        }
    }

    private void revive(float dt) {
        for (Hero p : heroes)
            if (p.health <= 0) {
                Hero partner = heroes[1 - p.slot];
                boolean helping =
                        partner.health > 0
                                && partner.hurt <= 0
                                && (partner.input.buttons() & CoopInput.INTERACT) != 0
                                && Math.hypot(partner.x - p.x, partner.z - p.z) < 2.8f;
                p.revive = helping ? p.revive + dt : 0;
                if (p.revive >= 3) {
                    p.health = p.maxHealth() * .5f;
                    p.stamina = p.maxStamina() * .5f;
                    p.revive = 0;
                    p.hurt = 1;
                    p.attack = 0;
                }
            }
    }

    /** Returns true for a parry, allowing a boss rush to be stopped. */
    private boolean hurt(Hero p, float damage, float x, float z, boolean parryable, Enemy enemy) {
        if (p.health <= 0 || p.hurt > 0 || p.dodge > .10f && p.dodge < .31f) return false;
        if (parryable && p.parry > 0 && Combat.inArc(p.x, p.z, p.facing, x, z, 100, 75)) {
            p.parry = 0;
            p.stamina = Math.min(p.maxStamina(), p.stamina + 15);
            if (enemy != null) {
                enemy.stun = 1;
                enemy.attackTime = enemy.chargeTime = 0;
                enemy.impactPending = false;
                enemy.cooldown = 1.2f;
            }
            return true;
        }
        p.health =
                Math.max(
                        0,
                        p.health
                                - damage * CoopBalance.PLAYER_DAMAGE_FACTOR * (1 - .04f * chapter));
        p.hurt = CoopBalance.HIT_GRACE;
        p.revive = p.repair = 0;
        if (p.health == 0) {
            p.attack = p.charge = p.dodge = 0;
            p.pending = false;
        }
        return false;
    }

    synchronized void enemyDamage(int slot, float damage, float x, float z, boolean parryable) {
        hurt(heroes[slot], damage, x, z, parryable, null);
    }

    private void updateEnemies(float dt) {
        List<Enemy> squad = enemies.stream().map(m -> m.enemy).toList();
        for (Iterator<Mob> it = enemies.iterator(); it.hasNext(); ) {
            Mob m = it.next();
            Enemy e = m.enemy;
            if (e.health <= 0) {
                if (e.deathTime == 0) {
                    e.attackTime = e.chargeTime = 0;
                    e.impactPending = false;
                    kills++;
                    for (Hero p : heroes)
                        if (p.health > 0) p.stamina = Math.min(p.maxStamina(), p.stamina + 5);
                }
                e.deathTime += dt;
                if (e.deathTime > 4) it.remove();
                continue;
            }
            Hero target = null;
            for (Hero p : heroes)
                if (p.health > 0
                        && (target == null
                                || e.distance(p.x, p.z) < e.distance(target.x, target.z)))
                    target = p;
            boolean player = target != null && (e.boss() || e.chooseHero(target.x, target.z, dt));
            float tx = player ? target.x : 0, tz = player ? target.z : 0;
            float sx = 0, sz = 0;
            for (Mob other : enemies)
                if (other != m && other.enemy.health > 0) {
                    float dx = e.x - other.enemy.x,
                            dz = e.z - other.enemy.z,
                            d = (float) Math.hypot(dx, dz);
                    if (d > .01f && d < 1.4f) {
                        sx += dx / d * (1.4f - d);
                        sz += dz / d * (1.4f - d);
                    }
                }
            var steer = ArenaNavigation.steering(chapter, e.x, e.z, tx, tz);
            float ox = e.x, oz = e.z;
            e.navigate(tx, tz, sx + steer.x(), sz + steer.z(), dt);
            var pos = ArenaNavigation.move(chapter, ox, oz, e.x, e.z, e.boss() ? .9f : .45f);
            e.x = pos.x();
            e.z = pos.z();
            if (e.chargeTime > 0)
                for (Hero p : heroes)
                    if ((m.chargeMask & (1 << p.slot)) == 0
                            && Combat.segmentDistance(p.x, p.z, ox, oz, e.x, e.z) < 1.3f) {
                        m.chargeMask |= 1 << p.slot;
                        hurt(p, e.meleeDamage(wave), ox, oz, true, e);
                    }
            if (e.impactPending && e.attackTime <= .25f && e.stun <= 0) {
                e.impactPending = false;
                if (e.ranged()) hostile.add(new Hostile(nextId++, new Projectile(e, wave)));
                else if (e.boss() && e.bossMove == BossMove.CHARGE) {
                    e.launchCharge();
                    m.chargeMask = 0;
                } else if (e.boss() && e.bossMove == BossMove.SIGILS) {
                    /* Warned fields were placed during the windup. */
                } else {
                    boolean slam = e.boss() && e.bossMove == BossMove.SLAM;
                    for (Hero p : heroes)
                        if (slam
                                ? Math.hypot(p.x - e.attackX, p.z - e.attackZ) < 3.5f
                                : Combat.inArc(
                                        e.x,
                                        e.z,
                                        e.facing,
                                        p.x,
                                        p.z,
                                        e.meleeRange() + .4f,
                                        e.boss() ? 75 : 65))
                            hurt(p, e.meleeDamage(wave), e.x, e.z, !slam, e);
                    if (!e.targetsPlayer && e.distance(0, 0) < 4.5f)
                        baseHealth = Math.max(0, baseHealth - e.siegeDamage(wave));
                    if (slam)
                        dangers.add(
                                new Danger(
                                        nextId++,
                                        new BossHazard(
                                                BossHazard.Kind.SHOCKWAVE,
                                                e.attackX,
                                                e.attackZ,
                                                0,
                                                e.meleeDamage(wave) * .65f)));
                }
            }
            if (e.stun <= 0
                    && e.attackTime <= 0
                    && e.cooldown <= 0
                    && e.distance(tx, tz) < e.attackRange()
                    && e.facingTarget(tx, tz)) {
                if (e.ranged() && volley[player ? target.slot + 1 : 0] > 0) continue;
                if (e.ranged()
                        && (ArenaNavigation.projectileFraction(chapter, e.x, e.z, tx, tz) < 1
                                || !SquadTactics.clearShot(e, tx, tz, squad))) {
                    e.sideStepTime = .65f;
                    continue;
                }
                int attacking = 0;
                for (Mob other : enemies)
                    if (other.target == (player ? targetSlot(target) : -1)
                            && other.enemy.health > 0
                            && other.enemy.attackTime > .25f) attacking++;
                if (!e.boss() && attacking >= difficulty.attackers + (e.ranged() ? 1 : 0)) continue;
                m.target = player ? target.slot : -1;
                e.beginAttack(tx, tz, player);
                if (e.ranged())
                    volley[player ? target.slot + 1 : 0] =
                            CoopBalance.VOLLEY_INTERVAL * difficulty.cadence;
                if (e.boss() && e.bossMove == BossMove.SIGILS)
                    for (Hero p : heroes)
                        if (p.health > 0)
                            for (int i = 0; i < (e.enraged() ? 5 : 3); i++) {
                                double a = i * 2.4;
                                float r = i == 0 ? 0 : 2.5f;
                                dangers.add(
                                        new Danger(
                                                nextId++,
                                                new BossHazard(
                                                        BossHazard.Kind.SIGIL,
                                                        p.x + (float) Math.cos(a) * r,
                                                        p.z + (float) Math.sin(a) * r,
                                                        e.attackDuration() - .25f + i * .12f,
                                                        e.meleeDamage(wave) * .75f)));
                            }
            }
        }
    }

    private static int targetSlot(Hero p) {
        return p == null ? -1 : p.slot;
    }

    private void updateShots(float dt) {
        for (Iterator<Shot> it = shots.iterator(); it.hasNext(); ) {
            Shot entry = it.next();
            HeroShot s = entry.shot();
            s.update(dt);
            float f =
                    ArenaNavigation.projectileFraction(chapter, s.previousX, s.previousZ, s.x, s.z);
            if (f < 1) {
                s.x = s.previousX + (s.x - s.previousX) * f;
                s.z = s.previousZ + (s.z - s.previousZ) * f;
            }
            boolean stop = false;
            Hero partner = heroes[1 - entry.owner()];
            if (s.restorative
                    && partner.health > 0
                    && partner.health < partner.maxHealth()
                    && s.intersects(partner.x, partner.z, .9f)) {
                partner.health = Math.min(partner.maxHealth(), partner.health + 18);
                stop = true;
            } else if (s.restorative
                    && baseHealth < CoopBalance.BASE_HEALTH
                    && s.intersects(0, 0, 2)) {
                baseHealth = Math.min(CoopBalance.BASE_HEALTH, baseHealth + 12);
                stop = true;
            }
            var sorted =
                    enemies.stream()
                            .filter(m -> m.enemy.health > 0)
                            .sorted(
                                    Comparator.comparingDouble(
                                            m -> m.enemy.distance(s.previousX, s.previousZ)))
                            .toList();
            for (Mob m : sorted)
                if (!stop && s.hit(m.enemy)) {
                    m.enemy.hit(Math.min(m.enemy.health, s.damage), s.previousX, s.previousZ, .25f);
                    if (!s.magic) {
                        stop = true;
                        break;
                    }
                }
            if (stop || f < 1 || s.remaining <= 0) it.remove();
        }
        for (Iterator<Hostile> it = hostile.iterator(); it.hasNext(); ) {
            Projectile s = it.next().shot();
            s.update(dt);
            float f =
                    ArenaNavigation.projectileFraction(chapter, s.previousX, s.previousZ, s.x, s.z);
            if (f < 1) {
                it.remove();
                continue;
            }
            boolean stop = false;
            for (Hero p : heroes)
                if (p.health > 0 && s.hits(p.x, p.z, .55f)) {
                    hurt(p, s.damage, s.previousX, s.previousZ, true, null);
                    stop = true;
                    break;
                }
            if (!stop && !s.playerTarget && s.hits(0, 0, 2)) {
                baseHealth = Math.max(0, baseHealth - s.damage);
                stop = true;
            }
            if (stop || s.life <= 0) it.remove();
        }
    }

    private void updateDangers(float dt) {
        for (Iterator<Danger> it = dangers.iterator(); it.hasNext(); ) {
            Danger d = it.next();
            BossHazard h = d.hazard;
            h.update(dt);
            if (h.delay <= 0 && !h.expired())
                for (Hero p : heroes)
                    if ((d.mask & (1 << p.slot)) == 0) {
                        float distance = (float) Math.hypot(p.x - h.x, p.z - h.z);
                        if (h.kind == BossHazard.Kind.SIGIL
                                ? distance < h.radius
                                : distance >= h.previousRadius - .45f
                                        && distance <= h.radius + .45f) {
                            d.mask |= 1 << p.slot;
                            hurt(p, h.damage, h.x, h.z, false, null);
                        }
                    }
            if (h.expired()) it.remove();
        }
    }

    public synchronized CoopState snapshot() {
        List<CoopState.Hero> players = new ArrayList<>();
        for (Hero p : heroes)
            if (p != null)
                players.add(
                        new CoopState.Hero(
                                p.slot,
                                p.name,
                                p.type.ordinal(),
                                p.connected,
                                p.ready,
                                p.paused,
                                p.x,
                                p.z,
                                p.facing,
                                p.health,
                                p.maxHealth(),
                                p.stamina,
                                p.maxStamina(),
                                p.velocityX,
                                p.velocityZ,
                                p.attack,
                                p.duration,
                                p.impact,
                                p.heavy,
                                p.combo,
                                p.charge,
                                p.dodge,
                                p.parry,
                                p.hurt,
                                p.revive,
                                p.repair,
                                p.potions,
                                p.credits,
                                p.vitality,
                                p.power,
                                p.endurance));
        List<CoopState.Mob> mobs =
                enemies.stream()
                        .map(m -> {
                            Enemy e = m.enemy;
                            return new CoopState.Mob(
                                    m.id,
                                    m.wave,
                                    e.variant,
                                    e.personality,
                                    e.x,
                                    e.z,
                                    e.facing,
                                    e.health,
                                    e.maxHealth,
                                    e.attackTime,
                                    e.attackDuration(),
                                    e.attackX,
                                    e.attackZ,
                                    e.bossMove.ordinal(),
                                    e.chargeTime,
                                    e.stun,
                                    e.animationTime,
                                    e.deathTime);
                        })
                        .toList();
        List<CoopState.Bolt> bolts = new ArrayList<>();
        for (Shot s : shots)
            bolts.add(
                    new CoopState.Bolt(
                            s.id, s.shot.x, s.shot.z, s.shot.vx, s.shot.vz, s.shot.magic, false));
        for (Hostile s : hostile)
            bolts.add(
                    new CoopState.Bolt(
                            s.id, s.shot.x, s.shot.z, s.shot.vx, s.shot.vz, s.shot.magic, true));
        List<CoopState.Danger> hazards =
                dangers.stream()
                        .map(d ->
                                new CoopState.Danger(
                                        d.id,
                                        d.hazard.kind.ordinal(),
                                        d.hazard.x,
                                        d.hazard.z,
                                        d.hazard.delay,
                                        d.hazard.radius,
                                        d.hazard.age))
                        .toList();
        return new CoopState(
                tick,
                phase,
                chapter,
                wave,
                queued,
                rest,
                baseHealth,
                CoopBalance.BASE_HEALTH,
                kills,
                difficulty.ordinal(),
                paused(),
                List.copyOf(players),
                mobs,
                List.copyOf(bolts),
                hazards);
    }

    public synchronized Properties checkpoint() {
        Properties p = new Properties();
        p.setProperty("coop.version", "1");
        p.setProperty("chapter", chapter + "");
        p.setProperty("difficulty", difficulty.name());
        for (int i = 0; i < 2; i++) {
            Hero h = heroes[i];
            int[] b =
                    h == null
                            ? savedBuild[i]
                            : new int[] {h.vitality, h.power, h.endurance, h.credits};
            for (int j = 0; j < 4; j++) p.setProperty("build." + i + "." + j, b[j] + "");
        }
        return p;
    }

    public static CoopWorld restore(Properties p) {
        if (!"1".equals(p.getProperty("coop.version")))
            throw new IllegalArgumentException("Unsupported co-op checkpoint");
        CoopWorld w =
                new CoopWorld(Difficulty.valueOf(p.getProperty("difficulty")), System.nanoTime());
        w.chapter = SaveGame.integer(p, "chapter", 0, 3);
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 4; j++)
                w.savedBuild[i][j] = SaveGame.integer(p, "build." + i + "." + j, 0, 4);
        return w;
    }
}
