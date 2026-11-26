import static com.raylib.Raylib.*;

import Entities.*;

import org.bytedeco.javacpp.BytePointer;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Films actual game simulation directly from its framebuffer, never the user's desktop. */
public final class TrailerCapture {
    private static int WIDTH = 1600, HEIGHT = 900;
    private static int FPS = 24;
    private static boolean shortTake;
    private static final Set<Integer> held = new HashSet<>(), taps = new HashSet<>();
    private static final AtomicBoolean attack = new AtomicBoolean();
    private static Path profile;
    private static int activeFrame;
    private static Enemy lockedTarget;
    private static boolean recovering;
    private static PrintWriter cues;

    record Shot(
            String id,
            int seconds,
            HeroClass hero,
            int wave,
            String mode,
            String title,
            String subtitle,
            String music) {}

    private static final Shot[] SHOTS = {
        new Shot(
                "01-ember",
                8,
                HeroClass.WARRIOR,
                0,
                "scenic",
                "OUTPOST",
                "Guard the last light. Bring back the dawn.",
                "title"),
        new Shot(
                "02-valley",
                25,
                HeroClass.WARRIOR,
                1,
                "scenic",
                "ONE VALLEY. ONE LAST FIRE.",
                "A living sky above a sanctuary worth defending.",
                "meadow"),
        new Shot(
                "03-warrior",
                30,
                HeroClass.WARRIOR,
                3,
                "combat",
                "COMMIT TO EVERY STRIKE",
                "Chain your attacks. Watch your stamina. Read the enemy.",
                "battle"),
        new Shot(
                "04-counter",
                18,
                HeroClass.WARRIOR,
                5,
                "counter",
                "TURN DEFENSE INTO OPPORTUNITY",
                "Gold attacks can be parried. Unblockable attacks demand a dodge.",
                "boss"),
        new Shot(
                "05-archer",
                25,
                HeroClass.ARCHER,
                6,
                "combat",
                "HOLD YOUR DISTANCE",
                "Bow, crossbow, reposition, reload. Make every opening count.",
                "frost"),
        new Shot(
                "06-mage",
                25,
                HeroClass.MAGE,
                8,
                "combat",
                "LET THE EMBER ANSWER",
                "Piercing magic and charged surges against the dead.",
                "necropolis"),
        new Shot(
                "07-supplies",
                16,
                HeroClass.ARCHER,
                1,
                "supplies",
                "LEAVE THE SAFETY OF THE FIRE",
                "Recover supplies before the opportunity is lost.",
                "meadow"),
        new Shot(
                "08-blessings",
                18,
                HeroClass.WARRIOR,
                0,
                "blessings",
                "SHAPE YOUR EXPEDITION",
                "Earn ember blessings. Choose the strengths your build needs.",
                "title"),
        new Shot(
                "09-survivor",
                25,
                HeroClass.WARRIOR,
                2,
                "survivor",
                "SOMEONE IS STILL OUT THERE",
                "Find a survivor. Stay close. Bring them home alive.",
                "cemetery"),
        new Shot(
                "10-waystone",
                25,
                HeroClass.MAGE,
                3,
                "beacon",
                "DEFEND MORE THAN YOURSELF",
                "Wake a waystone, draw the enemy, and hold until it answers.",
                "battle"),
        new Shot(
                "11-equipment",
                18,
                HeroClass.ARCHER,
                0,
                "equipment",
                "MAKE THE WARDEN YOUR OWN",
                "Visible weapons, fitted armor, and a build you can change.",
                "title"),
        new Shot(
                "12-pass",
                15,
                HeroClass.MAGE,
                6,
                "scenic",
                "FOUR CHAPTERS. A ROAD TO DAWN.",
                "Meadow, cemetery, frozen pass, and the Hollow Necropolis.",
                "frost"),
        new Shot(
                "13-warden",
                25,
                HeroClass.WARRIOR,
                5,
                "boss",
                "THE ASH WARDEN",
                "Learn the cleave, evade the slam, and survive the rush.",
                "boss"),
        new Shot(
                "14-king",
                25,
                HeroClass.MAGE,
                10,
                "boss",
                "THE HOLLOW KING",
                "Enrage changes the rhythm. Soul sigils change the battlefield.",
                "enrage"),
        new Shot(
                "15-dawn",
                12,
                HeroClass.WARRIOR,
                10,
                "victory",
                "BRING BACK THE DAWN",
                "The final blow. The fallen king. The valley remembers warmth.",
                "boss"),
        new Shot(
                "16-return",
                15,
                HeroClass.WARRIOR,
                0,
                "ending",
                "OUTPOST",
                "Try the latest development build and the all-gear playtest.",
                "title")
    };

    // A single rescue, filmed at normal speed with the game's follow camera.
    private static final Shot[] HOMEWARD = {
        new Shot("gate", 7, HeroClass.ARCHER, 2, "gate", "", "", "title"),
        new Shot("contact", 11, HeroClass.ARCHER, 2, "contact", "", "", "meadow"),
        new Shot("rescue", 12, HeroClass.ARCHER, 2, "rescue", "", "", "meadow"),
        new Shot("home", 9, HeroClass.ARCHER, 2, "home", "", "", "title"),
        new Shot("stranded", 5, HeroClass.ARCHER, 2, "stranded", "", "", "meadow"),
        new Shot("guard", 7, HeroClass.ARCHER, 2, "guard", "", "", "battle"),
        new Shot("skyline", 5, HeroClass.ARCHER, 2, "skyline", "", "", "title")
    };

    private static void homewardSetup(Game game, Shot shot) throws IOException {
        release();
        Progression p = new Progression(HeroClass.ARCHER);
        p.grantStarterWeapon();
        var enemies = new ArrayList<Enemy>();
        boolean city =
                shot.mode.equals("gate") || shot.mode.equals("home") || shot.mode.equals("skyline");
        if (!city) p.fieldEvent().begin(2);
        float x = -10, z = 12;
        if (shot.mode.equals("contact")) {
            enemies.add(new Enemy(-9, 3, 2, 1, .35f));
            enemies.add(new Enemy(-12, 1, 2, 0, .65f));
        }
        if (shot.mode.equals("guard")) {
            x = p.fieldEvent().x();
            z = p.fieldEvent().z() + 1.5f;
            p.fieldEvent().update(0, x, z, true);
            enemies.add(new Enemy(x, z + 2f, 2, 4, .5f));
        }
        if (shot.mode.equals("stranded")) {
            x = 0;
            z = 4;
        }
        if (shot.mode.equals("home")) p.rescueSurvivor();
        SaveGame.write(
                profile.resolve("save.properties"),
                new SaveGame.Snapshot(
                        "Wren",
                        x,
                        z,
                        180,
                        p,
                        new WaveDirector.State(2, 0, Math.max(1, enemies.size()), 8, 0, 21, 0),
                        enemies,
                        Arena.HIGHLANDS,
                        100,
                        .32f));
        if (!game.loadCurrent()) throw new IOException("Homeward scene failed to load");
        game.setState(GameState.PLAYING);
        if (city) game.travelToCitadel();
        var camera = game.getSceneManager().getOpenScene().getCameraController();
        switch (shot.mode) {
            case "gate" -> {
                game.getPlayer().getPosition().x(.6f).z(8);
                camera.setYaw(180);
                camera.setDistance(10);
                camera.setPitch(14);
                game.dayPhase(.27f);
            }
            case "contact" -> {
                camera.setYaw(7);
                camera.setDistance(12);
                camera.setPitch(20);
            }
            case "rescue" -> {
                game.getPlayer().getPosition().x(-13.5f).z(9.3f);
                camera.setYaw(-57);
                camera.setDistance(10);
                camera.setPitch(17);
            }
            case "home" -> {
                game.getPlayer().getPosition().x(-1.6f).z(7.7f);
                camera.setYaw(80);
                camera.setDistance(8);
                camera.setPitch(15);
                game.dayPhase(.66f);
            }
        }
        if (shot.mode.equals("skyline")) game.dayPhase(.695f);
        if (shot.mode.equals("home")) {
            CitadelHub hub = game.getCitadelHub();
            hub.updateCitizens(.03f, 30, 0, 1);
            float xHome = hub.npcX(CitadelNpc.SURVIVOR_BRAM),
                    zHome = hub.npcZ(CitadelNpc.SURVIVOR_BRAM);
            // Approach along the public street; use the resident's actual collision-safe position.
            double angle = Math.atan2(-xHome, -zHome * .15f);
            float dx = (float) Math.sin(angle), dz = (float) Math.cos(angle);
            game.getPlayer().getPosition().x(xHome + dx * 4.4f).z(zHome + dz * 4.4f);
            game.getPlayer().aimAt(xHome, zHome);
            camera.setYaw((float) Math.toDegrees(angle));
            System.out.printf(Locale.ROOT, "Bram at %.2f, %.2f%n", xHome, zHome);
        }
        game.pointerAim(false);
        game.photoMode(true);
        camera.update(0);
        // Settle lighting, idle poses and the follow spring before the first recorded frame.
        for (int n = 0; n < 12; n++) game.update(1f / FPS);
    }

    private static void homewardInput(Game game, Shot shot, int frame) {
        for (int key : taps) hold(key, false);
        taps.clear();
        float t = frame / (float) FPS;
        switch (shot.mode) {
            case "gate" -> hold(KEY_W, t > .45f && game.getPlayer().getPosition().z() < 28);
            case "contact" -> {
                if (frame == 3) tap(KEY_T);
                hold(KEY_Q, t >= .4f && t < 1.25f);
                if (frame == 54
                        || frame == 86
                        || frame == 131
                        || frame == 169
                        || frame == 209
                        || frame == 248) tap(KEY_SPACE);
                hold(KEY_D, t >= 2.2f && t < 2.95f);
                if (frame == 69) tap(KEY_LEFT_CONTROL);
                hold(KEY_A, t > 6 && t < 6.55f);
                if (frame == 59 && game.getEnemies().size() == 1) tap(KEY_T);
            }
            case "rescue" -> {
                if (frame == 18) tap(KEY_G);
                hold(KEY_W, t > 1.1f && game.getPlayer().getPosition().x() < -2.1f);
            }
            case "guard" -> {
                if (frame == 0) {
                    Enemy attacker = game.getEnemies().getFirst();
                    FieldEvent escort = game.getProgression().fieldEvent();
                    attacker.beginAttack(escort.x(), escort.z(), false);
                    attacker.targetsTraveler = true;
                    attacker.attackTime = .60f;
                    game.getPlayer().getPosition().x(escort.x()).z(escort.z() + 1.5f);
                    attacker.x = escort.x();
                    attacker.z = escort.z() + 3.5f;
                    game.getPlayer().getRotation().y(0);
                    game.getPlayer().aimAt(attacker.x, attacker.z);
                    tap(KEY_T);
                }
                if (frame == 12) tap(KEY_F);
                if (frame == 39 || frame == 70 || frame == 101) tap(KEY_SPACE);
            }
            case "home" -> {
                hold(KEY_W, t >= .25f && t < .72f);
                if (frame == 54)
                    game.getPlayer()
                            .aimAt(
                                    game.getCitadelHub().npcX(CitadelNpc.SURVIVOR_BRAM),
                                    game.getCitadelHub().npcZ(CitadelNpc.SURVIVOR_BRAM));
            }
        }
    }

    private static void homewardCamera(Game game, String mode, float seconds) {
        Camera3D c = game.getSceneManager().getOpenScene().getCameraController().getCamera();
        if (mode.equals("stranded")) {
            float dolly = .12f * seconds;
            c._position().x(-8.4f - dolly).y(1.6f).z(13.2f - dolly);
            c.target().x(-12.4f).y(1.03f).z(8);
        } else if (mode.equals("skyline")) {
            c._position().x(4 + seconds * .19f).y(10.5f + seconds * .2f).z(25);
            c.target().x(-1).y(5).z(-16);
        } else if (mode.equals("home")) {
            float x = game.getCitadelHub().npcX(CitadelNpc.SURVIVOR_BRAM);
            float z = game.getCitadelHub().npcZ(CitadelNpc.SURVIVOR_BRAM);
            c._position().x(x + 4.7f).y(1.65f).z(z + 2.9f + seconds * .08f);
            c.target().x(x + 1.0f).y(.95f).z(z);
        } else if (mode.equals("guard")) {
            c._position().x(-5.3f - seconds * .08f).y(3.3f).z(11.8f);
            c.target().x(-12).y(1f).z(10.8f);
        }
    }

    private static void event(int type, int a, int b) {
        try (AutomationEvent e = new AutomationEvent().type(type)) {
            e.params(0, a).params(1, b);
            PlayAutomationEvent(e);
        }
    }

    private static void hold(int key, boolean down) {
        if (down ? held.add(key) : held.remove(key)) event(down ? 2 : 1, key, 0);
    }

    private static void tap(int key) {
        hold(key, true);
        taps.add(key);
    }

    private static void release() {
        for (int key : new ArrayList<>(held)) hold(key, false);
        taps.clear();
        attack.set(false);
    }

    private static void steer(Game game, float tx, float tz, boolean sprint) {
        Player player = game.getPlayer();
        Camera3D c = game.getSceneManager().getOpenScene().getCameraController().getCamera();
        float fx = c.target().x() - c._position().x(),
                fz = c.target().z() - c._position().z(),
                len = (float) Math.hypot(fx, fz);
        fx /= len;
        fz /= len;
        float dx = tx - player.getPosition().x(),
                dz = tz - player.getPosition().z(),
                d = (float) Math.hypot(dx, dz);
        float forward = dx * fx + dz * fz, right = -dx * fz + dz * fx;
        hold(KEY_W, d > .8f && forward > .55f);
        hold(KEY_S, d > .8f && forward < -.55f);
        hold(KEY_D, d > .8f && right > .55f);
        hold(KEY_A, d > .8f && right < -.55f);
        hold(KEY_LEFT_SHIFT, sprint && d > 6);
    }

    private static void setup(Game game, Shot shot) throws Exception {
        release();
        lockedTarget = null;
        recovering = false;
        game.photoMode(false);
        Progression p = Progression.playtest(shot.hero, Difficulty.NORMAL, false);
        // All equipment/training shown is obtainable in the shipped all-gear sandbox.
        for (int i = 0; i < 5; i++) p.purchase(Progression.Upgrade.VITALITY);
        for (int i = 0; i < 9; i++) p.purchase(Progression.Upgrade.BASE);
        if (shot.mode.equals("beacon"))
            for (int i = 0; i < 2; i++) p.purchase(Progression.Upgrade.POWER);
        if (shot.mode.equals("boss") || shot.mode.equals("counter")) p.equipWeapon(0);
        else if (shot.mode.equals("victory")) p.equipWeapon(3);
        else p.equipWeapon(shot.hero == HeroClass.ARCHER ? 2 : 3);
        ArrayList<Enemy> enemies = new ArrayList<>();
        boolean fight =
                Set.of("combat", "boss", "counter", "victory", "survivor", "beacon", "supplies")
                        .contains(shot.mode);
        if (fight) {
            if (Campaign.chapter(shot.wave).bossOnly()) {
                Enemy boss = new Enemy(1, 11, shot.wave, 5, .5f);
                if (shot.mode.equals("victory")) boss.health = 80;
                if (shot.mode.equals("counter")) boss.health = boss.maxHealth * .55f;
                enemies.add(boss);
            } else {
                int count = shot.mode.equals("combat") ? 6 : shot.mode.equals("beacon") ? 5 : 3;
                for (int i = 0; i < count; i++) {
                    double a = i * Math.PI * 2 / count;
                    float x = (float) Math.sin(a) * (shot.mode.equals("combat") ? 11 : 15),
                            z = (float) Math.cos(a) * 11;
                    if (shot.mode.equals("combat")) {
                        x = (i % 2 == 0 ? -1 : 1) * (1.5f + i * .45f);
                        z = (shot.hero == HeroClass.WARRIOR ? 8 : 12) + i * .85f;
                    }
                    enemies.add(
                            new Enemy(
                                    x,
                                    z,
                                    shot.wave,
                                    WaveFormation.variant(shot.wave, i),
                                    .2f + (i % 6) * .12f));
                }
            }
        }
        float px = 0, pz = 5;
        if (Set.of("survivor", "beacon", "supplies").contains(shot.mode)) {
            p.fieldEvent().begin(shot.wave);
            px = p.fieldEvent().x() + 1;
            pz = p.fieldEvent().z() + 1;
            for (int i = 0; i < enemies.size(); i++) {
                enemies.get(i).x = p.fieldEvent().x() - 7 + i * 3;
                enemies.get(i).z = p.fieldEvent().z() - 10;
            }
        }
        if (shot.mode.equals("blessings")) {
            p.expedition().waveCleared(1);
            p.expedition().waveCleared(2);
        }
        int queued = shortTake && shot.mode.equals("combat") ? 4 : 0;
        int total = enemies.size() + queued;
        WaveDirector.State waves =
                new WaveDirector.State(shot.wave, queued, total, 8, queued > 0 ? .8f : 0, -8, 15);
        SaveGame.write(
                profile.resolve("save.properties"),
                new SaveGame.Snapshot(
                        "Ember",
                        px,
                        pz,
                        0,
                        p,
                        waves,
                        enemies,
                        Arena.HIGHLANDS,
                        100,
                        DayNightCycle.initial(Campaign.index(shot.wave))));
        if (!game.loadCurrent())
            throw new IllegalStateException("Trailer setup did not load: " + shot.id);
        game.setState(GameState.PLAYING);
        game.audio().configure(0, false);
        if (!fight) game.setState(GameState.PAUSED);
        if (shot.mode.equals("blessings")) game.setState(GameState.PLAYING);
        camera(game, shot, 0);
        game.photoMode(
                Set.of("scenic", "ending", "boss", "counter", "victory").contains(shot.mode));
    }

    private static void direct(Game game, Shot shot, int frame, int frames) {
        for (int key : new ArrayList<>(taps)) hold(key, false);
        taps.clear();
        for (int key = 32; key <= 348; key++) {
            if (IsKeyDown(key) && !held.contains(key)) event(1, key, 0);
            else if (!IsKeyDown(key) && held.contains(key)) event(2, key, 0);
        }
        float t = frame / (float) FPS;
        if (shot.mode.equals("scenic") || shot.mode.equals("ending")) {
            game.dayPhase(
                    shot.id.startsWith("02")
                            ? .30f + .45f * frame / frames
                            : shot.id.startsWith("12") ? .43f + .48f * frame / frames : .36f);
            return;
        }
        if (shot.mode.equals("blessings")) {
            if (frame == FPS / 2 || frame == FPS * 10) tap(KEY_G);
            if (frame == FPS * 7) tap(KEY_TWO);
            if (frame == FPS * 15) tap(KEY_ONE);
            return;
        }
        if (shot.mode.equals("equipment")) {
            if (frame == 0) {
                game.setState(GameState.PLAYING);
                tap(KEY_TAB);
            }
            int x = (WIDTH - 940) / 2, y = (HEIGHT - 600) / 2;
            if (frame == FPS * 4 || frame == FPS * 10) {
                int item = frame == FPS * 4 ? 1 : 8;
                event(7, x + 400 + (item % 6) * 75 + 30, y + 90 + (item / 6) * 76 + 30);
                event(6, 0, 0);
            }
            if (frame == FPS * 4 + 1 || frame == FPS * 10 + 1) {
                event(5, 0, 0);
                tap(KEY_ENTER);
            }
            return;
        }
        if (game.getState() == GameState.LEVEL_COMPLETE && !shot.mode.equals("victory")) {
            // Hold genuine results briefly; subsequent footage shows the real aftermath.
            release();
            return;
        }
        if (game.getState() != GameState.PLAYING) return;
        Player player = game.getPlayer();
        float px = player.getPosition().x(), pz = player.getPosition().z();
        FieldEvent field = game.getProgression().fieldEvent();
        boolean objective =
                Set.of("survivor", "beacon", "supplies").contains(shot.mode) && field.visible();
        if (objective) {
            if (field.status() == FieldEvent.Status.WAITING || shot.mode.equals("supplies")) {
                steer(game, field.x(), field.z(), false);
                hold(KEY_G, true);
                if (shot.mode.equals("supplies")) return;
            } else if (shot.mode.equals("survivor")) {
                float d = Math.max(.1f, (float) Math.hypot(field.x(), field.z()));
                steer(game, field.x() - field.x() / d * 3, field.z() - field.z() / d * 3, false);
            }
        } else hold(KEY_G, false);
        Enemy nearest = null;
        float distance = Float.MAX_VALUE, bestScore = Float.MAX_VALUE;
        for (Enemy e : game.getEnemies())
            if (e.health > 0) {
                float score =
                        objective && shot.mode.equals("beacon")
                                ? e.distance(field.x(), field.z()) - (e.variant == 2 ? 2 : 0)
                                : e.distance(px, pz);
                if (score < bestScore) {
                    nearest = e;
                    distance = e.distance(px, pz);
                    bestScore = score;
                }
            }
        if (shot.mode.equals("combat")) {
            if (lockedTarget != null
                    && lockedTarget.health > 0
                    && lockedTarget.distance(px, pz) < 18) nearest = lockedTarget;
            else lockedTarget = nearest;
            if (nearest != null) distance = nearest.distance(px, pz);
        }
        if (nearest == null) {
            if (!objective) steer(game, 0, 4, false);
            return;
        }
        float desired =
                shot.hero == HeroClass.WARRIOR ? 1.7f : shot.hero == HeroClass.ARCHER ? 5 : 5.5f;
        if (!objective || shot.mode.equals("beacon")) {
            float dx = nearest.x - px, dz = nearest.z - pz, len = Math.max(.1f, distance);
            float tx = px, tz = pz;
            if (shot.hero == HeroClass.WARRIOR && distance > 2.7f
                    || shot.hero != HeroClass.WARRIOR && distance > 8) {
                tx = nearest.x - dx / len * desired;
                tz = nearest.z - dz / len * desired;
            } else if (shot.hero != HeroClass.WARRIOR && distance < 4) {
                tx = px - dx / len * 3;
                tz = pz - dz / len * 3;
            }
            if (objective
                    && shot.mode.equals("beacon")
                    && Math.hypot(tx - field.x(), tz - field.z()) > 4) {
                tx = field.x();
                tz = field.z();
            }
            steer(game, tx, tz, distance > 9);
        }
        player.aimAt(nearest.x, nearest.z);
        // Give ranged movement a real opening instead of filming repeated attacks
        // while the director lets melee enemies crowd the hero at the sanctuary.
        if (shot.mode.equals("combat")
                && shot.hero != HeroClass.WARRIOR
                && distance < desired * .72f) {
            hold(KEY_Q, false);
            if (frame % 30 == 0) tap(KEY_LEFT_CONTROL);
            return;
        }
        if (game.getProgression().health() < game.getProgression().maxHealth() * .52f
                && frame % 24 == 0) tap(KEY_H);
        boolean danger =
                nearest.attackTime > .15f
                        && nearest.attackTime < .42f
                        && distance < nearest.meleeRange() + 1;
        if (danger) {
            if (!nearest.boss() || nearest.bossMove.parryable) {
                if (frame % 5 == 0) tap(KEY_F);
            } else if (frame % 8 == 0) tap(KEY_LEFT_CONTROL);
        }
        if (shot.mode.equals("counter")) {
            if (!danger && frame % 38 == 0) attack.set(true);
            return;
        }
        if (shortTake) {
            if (player.discipline().stamina() < 28) recovering = true;
            if (player.discipline().stamina() >= 62) recovering = false;
            if (recovering) {
                hold(KEY_Q, false);
                attack.set(false);
                if (distance < 4) {
                    float dx = px - nearest.x, dz = pz - nearest.z, len = Math.max(.1f, distance);
                    steer(game, px + dx / len * 3, pz + dz / len * 3, false);
                }
                return;
            }
        }
        if (distance < (shot.hero == HeroClass.WARRIOR ? 2.8f : 9)) {
            int cycle = frame % (FPS * 4);
            boolean heavy =
                    cycle < FPS * .8f
                            && !player.isAttacking()
                            && player.discipline().stamina() >= 55;
            hold(KEY_Q, heavy);
            if (!heavy && frame % 14 == 0) attack.set(true);
            if (shot.hero == HeroClass.WARRIOR && distance > 2.4f && frame % (FPS * 5) == 0)
                tap(KEY_R);
        } else hold(KEY_Q, false);
    }

    private static void camera(Game game, Shot shot, float t) {
        Camera3D c = game.getSceneManager().getOpenScene().getCameraController().getCamera();
        boolean scenic = shot.mode.equals("scenic") || shot.mode.equals("ending");
        if (scenic) {
            float angle = -.7f + t * .026f, r = shot.id.startsWith("12") ? 38 : 31;
            c._position()
                    .x((float) Math.sin(angle) * r)
                    .y(10 + (float) Math.sin(t * .08f) * 2)
                    .z((float) Math.cos(angle) * r);
            c.target().x(0).y(1.6f).z(0);
        } else {
            boolean duel =
                    shot.mode.equals("boss")
                            || shot.mode.equals("counter")
                            || shot.mode.equals("victory");
            float px = game.getPlayer().getPosition().x(),
                    pz = game.getPlayer().getPosition().z(),
                    angle = -2.65f;
            c._position()
                    .x(px + (float) Math.sin(angle) * 12)
                    .y(8.2f)
                    .z(pz + (float) Math.cos(angle) * 12);
            c.target().x(px).y(1.4f).z(pz + 1.5f);
        }
    }

    public static void main(String[] args) throws Exception {
        boolean homeward = Arrays.asList(args).contains("--homeward");
        if (homeward) {
            WIDTH = 1920;
            HEIGHT = 1080;
            FPS = 30;
        }
        boolean shortVideo = Arrays.asList(args).contains("--short");
        shortTake = shortVideo;
        if (shortVideo) FPS = 60;
        Shot[] shots =
                shortVideo
                        ? new Shot[] {
                            new Shot(
                                    "01-ember",
                                    2,
                                    HeroClass.WARRIOR,
                                    0,
                                    "scenic",
                                    "OUTPOST",
                                    "Guard the last light.",
                                    "title"),
                            new Shot(
                                    "03-warrior",
                                    7,
                                    HeroClass.WARRIOR,
                                    3,
                                    "combat",
                                    "STRIKE. PARRY. EVADE.",
                                    "Commit to your attacks. Protect the sanctuary.",
                                    "battle"),
                            new Shot(
                                    "05-archer",
                                    6,
                                    HeroClass.ARCHER,
                                    6,
                                    "combat",
                                    "FIND YOUR OPENING",
                                    "Reposition and fire through the enemy ranks.",
                                    "battle"),
                            new Shot(
                                    "06-mage",
                                    6,
                                    HeroClass.MAGE,
                                    8,
                                    "combat",
                                    "CHANNEL THE EMBER",
                                    "Piercing spells and powerful charged attacks.",
                                    "battle"),
                            new Shot(
                                    "13-warden",
                                    7,
                                    HeroClass.WARRIOR,
                                    5,
                                    "boss",
                                    "FACE THE ASH WARDEN",
                                    "Read the warning. Survive the rush.",
                                    "boss"),
                            new Shot(
                                    "16-return",
                                    2,
                                    HeroClass.WARRIOR,
                                    0,
                                    "ending",
                                    "OUTPOST",
                                    "Choose your warden. Bring back the dawn.",
                                    "title")
                        }
                        : homeward ? HOMEWARD : SHOTS;
        boolean preview = Arrays.asList(args).contains("--preview");
        int from = 0;
        Set<Integer> only = new HashSet<>();
        for (String arg : args) {
            if (arg.startsWith("--from=")) from = Integer.parseInt(arg.substring(7));
            if (arg.startsWith("--only="))
                for (String id : arg.substring(7).split(",")) only.add(Integer.parseInt(id));
        }
        Path out =
                Path.of(
                                homeward
                                        ? "build/homeward"
                                        : shortVideo
                                                ? "build/marketing-short"
                                                : preview
                                                        ? "build/marketing-preview"
                                                        : "build/marketing")
                        .toAbsolutePath();
        Files.createDirectories(out.resolve("raw"));
        profile = Files.createTempDirectory(Path.of("build"), "trailer-profile-").toAbsolutePath();
        System.setProperty("outpost.dataDir", profile.toString());
        Settings settings = new Settings();
        settings.playerName = "Ember";
        settings.volume = 0;
        settings.sensitivity = 0;
        settings.save(profile.resolve("settings.properties"));
        SetTraceLogLevel(LOG_WARNING);
        SetConfigFlags(FLAG_MSAA_4X_HINT);
        InitWindow(WIDTH, HEIGHT, "Outpost | Capturing actual gameplay");
        SetExitKey(KEY_NULL);
        SetTargetFPS(0);
        Game game = null;
        try (PrintWriter manifest =
                new PrintWriter(Files.newBufferedWriter(out.resolve("shots.tsv")))) {
            manifest.println("id\tseconds\ttitle\tsubtitle\tmusic");
            for (Shot shot : shots)
                manifest.printf(
                        Locale.ROOT,
                        "%s\t%d\t%s\t%s\t%s%n",
                        shot.id,
                        preview ? Math.min(3, shot.seconds) : shot.seconds,
                        shot.title,
                        shot.subtitle,
                        shot.music);
            manifest.flush();
            game = new Game(() -> attack.getAndSet(false));
            game.getSceneManager().getOpenScene().getCameraController().setSensitivity(0);
            game.audio()
                    .observeCues(name -> {
                        if (cues != null && activeFrame >= 0)
                            cues.printf(Locale.ROOT, "%.6f\t%s%n", activeFrame / (float) FPS, name);
                    });
            for (int index = from; index < shots.length; index++) {
                if (!only.isEmpty() && !only.contains(index)) continue;
                Shot shot = shots[index];
                activeFrame = -1;
                if (homeward) homewardSetup(game, shot);
                else setup(game, shot);
                int frames = (preview ? Math.min(3, shot.seconds) : shot.seconds) * FPS;
                Path clip = out.resolve("raw/" + shot.id + ".mp4");
                Process encoder =
                        new ProcessBuilder(
                                        System.getenv().getOrDefault("OUTPOST_FFMPEG", "ffmpeg"),
                                        "-hide_banner",
                                        "-loglevel",
                                        "error",
                                        "-y",
                                        "-f",
                                        "rawvideo",
                                        "-pix_fmt",
                                        "rgba",
                                        "-s",
                                        WIDTH + "x" + HEIGHT,
                                        "-r",
                                        FPS + "",
                                        "-i",
                                        "pipe:0",
                                        "-an",
                                        "-vf",
                                        "scale=1920:1080:flags=lanczos",
                                        "-c:v",
                                        "libx264",
                                        "-preset",
                                        "veryfast",
                                        "-crf",
                                        "19",
                                        "-pix_fmt",
                                        "yuv420p",
                                        "-movflags",
                                        "+faststart",
                                        clip.toString())
                                .redirectError(
                                        out.resolve("raw/" + shot.id + "-encoder.log").toFile())
                                .start();
                int defeats = 0, playing = 0;
                try (OutputStream pixels =
                                new BufferedOutputStream(
                                        encoder.getOutputStream(), 4 * 1024 * 1024);
                        PrintWriter cueFile =
                                new PrintWriter(
                                        Files.newBufferedWriter(
                                                out.resolve("raw/" + shot.id + "-cues.tsv")))) {
                    cues = cueFile;
                    byte[] buffer = new byte[WIDTH * HEIGHT * 4];
                    for (int f = 0; f < frames; f++) {
                        if (WindowShouldClose())
                            throw new IOException("Trailer capture window was closed");
                        activeFrame = f;
                        if (homeward) homewardInput(game, shot, f);
                        else direct(game, shot, f, frames);
                        game.update(1f / FPS);
                        if (homeward) homewardCamera(game, shot.mode, f / (float) FPS);
                        else camera(game, shot, f / (float) FPS);
                        BeginDrawing();
                        ClearBackground(Menu.INK);
                        game.render();
                        EndDrawing();
                        Image image = LoadImageFromScreen();
                        try {
                            if (image.width() != WIDTH || image.height() != HEIGHT)
                                throw new IOException("Capture dimensions changed");
                            ImageFormat(image, PIXELFORMAT_UNCOMPRESSED_R8G8B8A8);
                            new BytePointer(image.data()).get(buffer);
                            pixels.write(buffer);
                        } finally {
                            UnloadImage(image);
                        }
                        if (game.getState() == GameState.GAME_OVER) defeats++;
                        if (game.getState() == GameState.PLAYING) playing++;
                    }
                    cues = null;
                }
                if (encoder.waitFor() != 0)
                    throw new IOException("Video encoding failed: " + shot.id);
                Files.writeString(
                        out.resolve("raw/" + shot.id + "-quality.txt"),
                        "frames="
                                + frames
                                + "\nplaying="
                                + playing
                                + "\nplayerDefeat="
                                + defeats
                                + "\nendState="
                                + game.getState()
                                + "\nhero="
                                + game.getProgression().heroClass()
                                + "\nobjective="
                                + game.getProgression().fieldEvent().status()
                                + "\nblessings="
                                + game.getProgression().expedition().choices()
                                + "\n");
                System.out.println(
                        "CAPTURED "
                                + shot.id
                                + " / "
                                + frames
                                + " frames / player defeat frames "
                                + defeats);
                release();
            }
        } finally {
            cues = null;
            if (game != null) game.dispose();
            CloseWindow();
        }
    }
}
