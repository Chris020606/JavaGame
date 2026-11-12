import static com.raylib.Colors.WHITE;
import static com.raylib.Raylib.*;

import Entities.*;

import Scene.CameraController;

import assets.AssetPaths;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Complete host/join front end and snapshot presentation for the two-player campaign. */
public final class CoopGame implements AutoCloseable {
    private final Settings settings = new Settings();
    private final Color allyGreen = Menu.color(75, 210, 95);
    private final WorldAssets assets = new WorldAssets();
    private final ModernRenderer renderer = new ModernRenderer();
    private final GameAudio audio = new GameAudio();
    private final CombatVfx effects = new CombatVfx();
    private final CombatVfx[] blades = {new CombatVfx(), new CombatVfx()};
    private final Player[] avatars = new Player[2];
    private final HeroClass[] avatarTypes = new HeroClass[2];
    private final Progression[] loadouts = new Progression[2];
    private final int[] tiers = {-1, -1};
    private final float[] downAge = new float[2];
    private final HashMap<Integer, Enemy> enemyViews = new HashMap<>();
    private final EnemyFocus enemyFocus = new EnemyFocus();
    private final TargetFrameScreen targetFrameScreen = new TargetFrameScreen();
    private final Camera2D ui = new Camera2D().zoom(1);
    private CameraController cameraControl;
    private CoopServer server;
    private CoopClient client;
    private CompletableFuture<CoopClient> connecting;
    private CoopState state, previous;
    private long observedTick = -1, sequence;
    private int attempt, focused = -1, pendingButtons;
    private float sendTime;
    private boolean finished, disposed, confirmLeave, capturedMouse;
    private String message = "", name, address = "127.0.0.1:27878", code = "", portText = "27878";
    private HeroClass selected = HeroClass.WARRIOR;
    private Difficulty difficulty = Difficulty.NORMAL;
    private List<String> addresses = localAddresses();
    private int inviteIndex;
    private String connectAddress, connectCode;
    private int connectPort, connectSlot;
    private final Path checkpoint = AppData.directory().resolve("coop-checkpoint.properties");
    private final Color skyTopBlend = new Color(), skyHorizonBlend = new Color();

    public CoopGame() {
        settings.load(AppData.directory().resolve("settings.properties"));
        name = settings.playerName;
        difficulty = settings.difficulty;
        renderer.quality(settings.highQuality);
        audio.configure(settings.volume, settings.muted);
        ensureAvatar(0, selected, 0);
        avatars[0].getPosition().z(4);
        cameraControl = new CameraController(avatars[0]);
        cameraControl.setSensitivity(settings.sensitivity);
        cameraControl.update(0);
    }

    private static List<String> localAddresses() {
        List<String> result = new ArrayList<>();
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface net = interfaces.nextElement();
                if (!net.isUp() || net.isLoopback()) continue;
                var ips = net.getInetAddresses();
                while (ips.hasMoreElements()) {
                    InetAddress ip = ips.nextElement();
                    if (ip instanceof Inet4Address
                            && !ip.isLoopbackAddress()
                            && !ip.isLinkLocalAddress()) result.add(ip.getHostAddress());
                }
            }
        } catch (SocketException ignored) {
        }
        result.sort(Comparator.comparing((String ip) -> !ip.startsWith("192.168.")));
        if (result.isEmpty()) result.add("127.0.0.1");
        return List.copyOf(result);
    }

    private void ensureAvatar(int slot, HeroClass type, int tier) {
        if (avatars[slot] == null || avatarTypes[slot] != type) {
            if (avatars[slot] != null) avatars[slot].dispose();
            Player player = new Player();
            String path =
                    AssetPaths.path(
                            "resources/outpost/hero_"
                                    + type.name().toLowerCase(Locale.ROOT)
                                    + ".glb");
            player.setModel(LoadModel(path));
            player.loadAnimations(path);
            player.setAnimationFPS(60);
            player.setWeaponStyle(type.ordinal());
            player.resetCombat();
            avatars[slot] = player;
            avatarTypes[slot] = type;
            tiers[slot] = -1;
        }
        if (tiers[slot] != tier) {
            Progression p = Progression.playtest(type, Difficulty.NORMAL, false);
            p.equipWeapon(Math.min(3, tier));
            for (ArmorSlot armor : ArmorSlot.values()) p.equipArmor(armor, Math.min(3, tier));
            loadouts[slot] = p;
            tiers[slot] = tier;
            avatars[slot].setWeaponTier(tier);
            avatars[slot].setCrossbow(type == HeroClass.ARCHER && tier >= 2);
        }
    }

    private void connect(String host, int port, int slot, String secret) {
        if (client != null) client.close();
        client = null;
        previous = state = null;
        enemyViews.clear();
        enemyFocus.clear();
        effects.clear();
        for (CombatVfx blade : blades) blade.clear();
        Arrays.fill(downAge, 0);
        observedTick = -1;
        sequence = 0;
        pendingButtons = 0;
        connectAddress = host;
        connectPort = port;
        connectSlot = slot;
        connectCode = secret;
        int id = ++attempt;
        String playerName = name.trim();
        HeroClass hero = selected;
        message = "";
        connecting =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        CoopClient made =
                                new CoopClient(host, port, slot, secret, playerName, hero);
                        if (disposed || attempt != id) {
                            made.close();
                            throw new IOException("Connection cancelled");
                        }
                        return made;
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                });
    }

    void host(int port, boolean resume) {
        try {
            if (server != null) server.close();
            CoopWorld world =
                    resume
                            ? CoopWorld.restore(AppData.read(checkpoint))
                            : new CoopWorld(difficulty, System.nanoTime());
            server =
                    new CoopServer(
                            port,
                            world,
                            p -> {
                                try {
                                    AppData.write(checkpoint, p);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
            connect("127.0.0.1", server.port(), 0, server.hostKey());
        } catch (IOException | IllegalArgumentException failure) {
            message =
                    resume
                            ? "Could not resume the co-op checkpoint: " + failure.getMessage()
                            : "Could not host. Check the port or close another host: "
                                    + failure.getMessage();
        }
    }

    void join(String location, String invite) {
        try {
            if (server != null) {
                server.close();
                server = null;
            }
            String host = location.trim();
            int port = CoopServer.DEFAULT_PORT;
            int colon = host.lastIndexOf(':');
            if (colon >= 0) {
                port = Integer.parseInt(host.substring(colon + 1));
                host = host.substring(0, colon);
            }
            if (host.isBlank() || host.contains(":") || port < 1 || port > 65535)
                throw new IllegalArgumentException(
                        "Use the host's IPv4 address or computer name and port");
            if (!invite.matches("[0-9]{6}"))
                throw new IllegalArgumentException("Enter the six-digit session code");
            connect(host, port, 1, invite);
        } catch (IllegalArgumentException failure) {
            message = failure.getMessage();
        }
    }

    private CoopState.Hero own() {
        if (state == null || client == null) return null;
        return state.heroes().stream()
                .filter(h -> h.slot() == client.slot())
                .findFirst()
                .orElse(null);
    }

    private CoopState.Hero partner() {
        if (state == null || client == null) return null;
        return state.heroes().stream()
                .filter(h -> h.slot() != client.slot())
                .findFirst()
                .orElse(null);
    }

    Camera3D camera() {
        return cameraControl.getCamera();
    }

    void configure(String[] args) {
        String hosting = null, joining = null, invite = "";
        boolean resume = false;
        for (String arg : args) {
            if (arg.startsWith("--coop-class="))
                selected = HeroClass.valueOf(arg.substring(13).toUpperCase(Locale.ROOT));
            if (arg.startsWith("--coop-name=")) name = arg.substring(12);
            if (arg.startsWith("--coop-host=")) hosting = arg.substring(12);
            if (arg.startsWith("--coop-resume=")) {
                hosting = arg.substring(14);
                resume = true;
            }
            if (arg.startsWith("--coop-join=")) joining = arg.substring(12);
            if (arg.startsWith("--coop-code=")) invite = arg.substring(12);
        }
        if (hosting != null) host(Integer.parseInt(hosting), resume);
        else if (joining != null) join(joining, invite);
    }

    public boolean finished() {
        return finished;
    }

    CoopClient network() {
        return client;
    }

    CoopServer hosted() {
        return server;
    }

    Enemy focusedEnemy() {
        return enemyFocus.selected();
    }

    CoopState snapshot() {
        return state;
    }

    public void update(float dt) {
        dt = Math.max(0, Math.min(.05f, dt));
        if (connecting != null && connecting.isDone()) {
            try {
                client = connecting.join();
            } catch (CompletionException error) {
                message = error.getCause().getMessage();
            }
            connecting = null;
        }
        if (client != null && client.state() != null) state = client.state();
        if (!IsMouseButtonDown(MOUSE_BUTTON_LEFT)) capturedMouse = false;
        else if (state == null
                || state.phase() != CoopState.Phase.PLAYING
                || state.paused()
                || confirmLeave) capturedMouse = true;
        if (state != null) {
            for (var h : state.heroes()) {
                int totalPoints = h.vitality() + h.power() + h.endurance();
                int tier =
                        Math.max(
                                state.chapter(),
                                totalPoints >= 6
                                        ? (totalPoints >= 10 ? 3 : 2)
                                        : (totalPoints >= 3 ? 1 : 0));
                ensureAvatar(h.slot(), HeroClass.values()[h.hero()], tier);
                Player p = avatars[h.slot()];
                float age = client.snapshotAge();
                float tx = h.x() + h.vx() * age, tz = h.z() + h.vz() * age;
                if (previous == null
                        || Math.hypot(p.getPosition().x() - tx, p.getPosition().z() - tz) > 4)
                    p.getPosition().x(tx).z(tz);
                else {
                    float blend = 1 - (float) Math.exp(-dt * (h.slot() == client.slot() ? 40 : 16));
                    p.getPosition()
                            .x(p.getPosition().x() + (tx - p.getPosition().x()) * blend)
                            .z(p.getPosition().z() + (tz - p.getPosition().z()) * blend);
                }
                float delta = (float) Math.IEEEremainder(h.facing() - p.getRotation().y(), 360);
                p.getRotation().y(p.getRotation().y() + delta * Math.min(1, dt * 25));
                downAge[h.slot()] = h.health() <= 0 ? downAge[h.slot()] + dt : 0;
                p.presentCoop(
                        dt,
                        Math.max(0, h.attack() - age),
                        h.duration(),
                        h.impact(),
                        h.heavy(),
                        h.combo(),
                        h.charge(),
                        Math.max(0, h.dodge() - age),
                        Math.max(0, h.parry() - age),
                        h.vx(),
                        h.vz(),
                        h.health(),
                        downAge[h.slot()],
                        state.phase() == CoopState.Phase.VICTORY);
                blades[h.slot()].update(dt);
                float elapsed = h.duration() - h.attack();
                if (h.hero() == HeroClass.WARRIOR.ordinal()
                        && h.attack() > 0
                        && elapsed > h.impact() * .65f
                        && elapsed < h.impact() + .13f)
                    assets.sampleBlade(p, state.chapter(), blades[h.slot()].blade());
            }
            if (avatars[client.slot()] != null
                    && cameraControl.getTarget() != avatars[client.slot()]) {
                cameraControl = new CameraController(avatars[client.slot()]);
                cameraControl.setSensitivity(settings.sensitivity);
            }
            cameraControl.update(dt);
            observe();
            HashSet<Integer> ids = new HashSet<>();
            for (var m : state.enemies()) {
                ids.add(m.id());
                Enemy e =
                        enemyViews.computeIfAbsent(
                                m.id(),
                                id ->
                                        new Enemy(
                                                m.x(),
                                                m.z(),
                                                m.wave(),
                                                m.variant(),
                                                m.personality(),
                                                Difficulty.values()[state.difficulty()],
                                                CoopBalance.healthScale(m.variant() == 5)));
                float blend = Math.min(1, dt * 20);
                e.x += (m.x() - e.x) * blend;
                e.z += (m.z() - e.z) * blend;
                e.facing = m.facing();
                e.health = m.health();
                e.attackTime = m.attack();
                e.presentationDuration(m.duration());
                e.attackX = m.attackX();
                e.attackZ = m.attackZ();
                e.bossMove = BossMove.values()[m.move()];
                e.chargeTime = m.charge();
                e.stun = m.stun();
                e.animationTime = m.clock();
                e.deathTime = m.death();
            }
            enemyViews.keySet().removeIf(id -> !ids.contains(id));
        } else {
            ensureAvatar(0, selected, 0);
            avatars[0].getPosition().z(4);
            if (cameraControl.getTarget() != avatars[0]) {
                cameraControl = new CameraController(avatars[0]);
                cameraControl.setSensitivity(settings.sensitivity);
            }
            avatars[0].updateAnimation(dt);
            cameraControl.update(dt);
        }
        effects.update(dt);
        boolean playing = state != null && state.phase() == CoopState.Phase.PLAYING;
        // Focus is presentation-only; no new client authority or protocol fields are needed.
        var focusCandidates = List.copyOf(enemyViews.values());
        enemyFocus.update(focusCandidates);
        if (playing
                && !confirmLeave
                && !state.paused()
                && own() != null
                && settings.controls.pressed(Config.Controls.Action.FOCUS)) {
            var me = own();
            enemyFocus.cycle(
                    me.x(),
                    me.z(),
                    focusCandidates,
                    enemy ->
                            ArenaNavigation.projectileFraction(
                                            state.chapter(), me.x(), me.z(), enemy.x, enemy.z)
                                    >= 1);
        }
        Enemy boss =
                enemyViews.values().stream()
                        .filter(e -> e.boss() && e.health > 0)
                        .findFirst()
                        .orElse(null);
        audio.update(
                dt,
                state == null ? 0 : state.chapter(),
                !playing,
                playing && !state.paused() && !state.enemies().isEmpty(),
                playing && boss != null,
                boss != null && boss.enraged());
        if (client == null) {
            if (IsKeyPressed(KEY_ESCAPE) && connecting == null) finished = true;
            return;
        }
        if (!client.error().isEmpty()) return;
        if (playing && !confirmLeave && IsKeyPressed(KEY_ESCAPE)) pendingButtons |= CoopInput.PAUSE;
        if (playing && !state.paused() && !confirmLeave) {
            if (settings.controls.pressed(Config.Controls.Action.DODGE))
                pendingButtons |= CoopInput.DODGE;
            if (settings.controls.pressed(Config.Controls.Action.PARRY))
                pendingButtons |= CoopInput.PARRY;
            if (settings.controls.pressed(Config.Controls.Action.POTION))
                pendingButtons |= CoopInput.POTION;
            if (settings.controls.pressed(Config.Controls.Action.SLASH)
                    || !capturedMouse && IsMouseButtonPressed(MOUSE_BUTTON_LEFT))
                pendingButtons |= CoopInput.LIGHT;
        }
        if (!playing && IsKeyPressed(KEY_ENTER)) pendingButtons |= CoopInput.READY;
        sendTime += dt;
        if (sendTime < 1f / 30) return;
        sendTime = 0;
        float mx = 0, mz = 0, aim = own() == null ? 0 : own().facing();
        int held = 0;
        if (playing && !state.paused() && !confirmLeave) {
            Camera3D c = cameraControl.getCamera();
            float fx = c.target().x() - c._position().x(),
                    fz = c.target().z() - c._position().z(),
                    len = (float) Math.hypot(fx, fz);
            fx /= len;
            fz /= len;
            int
                    forward =
                            (settings.controls.down(Config.Controls.Action.FORWARD) ? 1 : 0)
                                    - (settings.controls.down(Config.Controls.Action.BACK) ? 1 : 0),
                    right =
                            (settings.controls.down(Config.Controls.Action.RIGHT) ? 1 : 0)
                                    - (settings.controls.down(Config.Controls.Action.LEFT) ? 1 : 0);
            mx = fx * forward - fz * right;
            mz = fz * forward + fx * right;
            float magnitude = Math.max(1, (float) Math.hypot(mx, mz));
            mx /= magnitude;
            mz /= magnitude;
            if (!IsMouseButtonDown(MOUSE_BUTTON_RIGHT)) {
                Ray ray = GetScreenToWorldRay(GetMousePosition(), c);
                if (Math.abs(ray.direction().y()) > .0001f) {
                    float t = (1.1f - ray._position().y()) / ray.direction().y();
                    if (t > 0 && own() != null)
                        aim =
                                (float)
                                        Math.toDegrees(
                                                Math.atan2(
                                                        ray._position().x()
                                                                + ray.direction().x() * t
                                                                - own().x(),
                                                        ray._position().z()
                                                                + ray.direction().z() * t
                                                                - own().z()));
                }
            }
            if (!capturedMouse
                            && IsMouseButtonDown(MOUSE_BUTTON_LEFT)
                            && !IsMouseButtonDown(MOUSE_BUTTON_RIGHT)
                    || settings.controls.down(Config.Controls.Action.SLASH))
                held |= CoopInput.LIGHT;
            if (settings.controls.down(Config.Controls.Action.HEAVY)) held |= CoopInput.HEAVY;
            if (settings.controls.down(Config.Controls.Action.INTERACT)) held |= CoopInput.INTERACT;
            if (settings.controls.down(Config.Controls.Action.SPRINT)) held |= CoopInput.SPRINT;
        }
        client.submit(new CoopInput(++sequence, mx, mz, aim, held | pendingButtons));
        pendingButtons = 0;
    }

    private void observe() {
        if (state.tick() == observedTick) return;
        observedTick = state.tick();
        if (previous != null) {
            if (state.wave() != previous.wave()) audio.play("wave");
            if (state.phase() != previous.phase()
                    && (state.phase() == CoopState.Phase.INTERMISSION
                            || state.phase() == CoopState.Phase.VICTORY)) audio.play("victory");
            for (var h : state.heroes()) {
                var old =
                        previous.heroes().stream()
                                .filter(p -> p.slot() == h.slot())
                                .findFirst()
                                .orElse(null);
                if (old == null) continue;
                if (h.attack() > 0 && old.attack() <= 0) {
                    audio.play(h.hero() == 0 ? "swing" : h.hero() == 1 ? "bow" : "magic");
                    blades[h.slot()].beginMelee(h.heavy());
                }
                if (h.health() < old.health()) {
                    audio.play("hurt");
                    effects.impact(h.x(), h.z(), Menu.RED, false);
                }
                if (h.health() > old.health() + 8) {
                    audio.play("ward");
                    effects.impact(h.x(), h.z(), Menu.TEAL, true);
                }
            }
            for (var m : state.enemies()) {
                var old =
                        previous.enemies().stream()
                                .filter(e -> e.id() == m.id())
                                .findFirst()
                                .orElse(null);
                if (old == null) continue;
                if (m.health() < old.health()) {
                    effects.impact(m.x(), m.z(), Menu.GOLD, m.health() <= 0);
                    audio.play(m.health() <= 0 ? (m.variant() == 5 ? "boss-fall" : "kill") : "hit");
                }
            }
        }
        previous = state;
    }

    private static Vector3 v(float x, float y, float z) {
        return new Vector3().x(x).y(y).z(z);
    }

    private static Color blendSky(
            DayNightCycle.Rgb base, Color chapter, float amount, Color target) {
        return target.r(
                        (byte)
                                Math.round(
                                        base.r() * 255 * (1 - amount)
                                                + Byte.toUnsignedInt(chapter.r()) * amount))
                .g(
                        (byte)
                                Math.round(
                                        base.g() * 255 * (1 - amount)
                                                + Byte.toUnsignedInt(chapter.g()) * amount))
                .b(
                        (byte)
                                Math.round(
                                        base.b() * 255 * (1 - amount)
                                                + Byte.toUnsignedInt(chapter.b()) * amount))
                .a((byte) 255);
    }

    private void drawSanctuaryRing() {
        Color ring = Fade(Menu.TEAL, .62f);
        float radius = 6;
        DrawCircle3D(v(0, .05f, 0), radius, v(1, 0, 0), 90, ring);
        float pulse = (float) Math.sin(GetTime() * 2) * .08f;
        DrawCircle3D(v(0, .06f, 0), radius + pulse, v(1, 0, 0), 90, Fade(Menu.GOLD, .42f));
        DrawCylinderWires(v(0, 0, 0), radius, radius, .1f, 32, Fade(Menu.TEAL, .35f));
    }

    private void actors() {
        if (state == null) {
            assets.preparePlayer(avatars[0]);
            assets.drawPlayerBody(avatars[0], loadouts[0], WHITE);
            assets.drawPlayerWeapon(avatars[0], selected.ordinal(), 0);
            return;
        }
        for (var h : state.heroes()) {
            Player p = avatars[h.slot()];
            int totalPoints = h.vitality() + h.power() + h.endurance();
            int tier =
                    Math.max(
                            state.chapter(),
                            totalPoints >= 6
                                    ? (totalPoints >= 10 ? 3 : 2)
                                    : (totalPoints >= 3 ? 1 : 0));
            assets.preparePlayer(p);
            assets.drawPlayerBody(
                    p, loadouts[h.slot()], h.hurt() > 0 && h.health() > 0 ? Menu.RED : WHITE);
            assets.drawPlayerWeapon(p, h.hero(), tier);
            assets.drawArmor(p, loadouts[h.slot()]);
        }
        for (Enemy e : enemyViews.values()) assets.drawEnemy(e);
    }

    private void world() {
        int chapter = state == null ? 0 : state.chapter();
        float time = (float) GetTime();
        Camera3D camera = cameraControl.getCamera();
        DayNightCycle.Frame light =
                DayNightCycle.sample(
                        (DayNightCycle.initial(chapter)
                                        + (state == null ? 0 : state.tick() / 60f)
                                                / DayNightCycle.PERIOD)
                                % 1);
        assets.view(
                avatars[state == null || avatars[client.slot()] == null ? 0 : client.slot()],
                camera);
        renderer.beginShadow(time, light);
        assets.pass(renderer.shadowShader, true);
        assets.drawEnvironment(chapter, time);
        actors();
        renderer.endShadow();
        renderer.beginScene();
        ChapterLook look = ChapterLook.at(chapter);
        float atmosphere = .12f + .34f * light.daylight();
        DrawRectangleGradientV(
                0,
                0,
                renderer.width(),
                renderer.height(),
                blendSky(light.top(), look.skyTop(), atmosphere, skyTopBlend),
                blendSky(light.horizon(), look.skyBottom(), atmosphere, skyHorizonBlend));
        BeginMode3D(camera);
        assets.drawSky(camera, light);
        Enemy boss =
                enemyViews.values().stream()
                        .filter(e -> e.boss() && e.health > 0)
                        .findFirst()
                        .orElse(null);
        assets.litPass(renderer, camera, time, boss, light);
        assets.drawEnvironment(chapter, time);
        actors();
        assets.drawFadedScenery(chapter, time);
        drawSanctuaryRing();
        if (state != null) {
            for (var h : state.heroes())
                if (h.health() <= 0)
                    DrawCylinderWires(v(h.x(), .1f, h.z()), 2.8f, 2.8f, .05f, 32, Menu.TEAL);
            for (var m : state.enemies())
                if (m.health() > 0 && m.attack() > 0 && m.variant() == 5) {
                    Color warning = BossMove.values()[m.move()].parryable ? Menu.GOLD : Menu.RED;
                    if (m.move() == BossMove.SLAM.ordinal())
                        DrawCylinderWires(
                                v(m.attackX(), .08f, m.attackZ()), 3.5f, 3.5f, .08f, 40, warning);
                    else if (m.move() == BossMove.CHARGE.ordinal())
                        DrawLine3D(v(m.x(), .1f, m.z()), v(m.attackX(), .1f, m.attackZ()), warning);
                    else DrawCylinderWires(v(m.x(), .08f, m.z()), 4.2f, 4.2f, .08f, 36, warning);
                }
            for (var d : state.dangers()) {
                BossHazard h =
                        new BossHazard(
                                BossHazard.Kind.values()[d.kind()], d.x(), d.z(), d.delay(), 0);
                h.radius = d.radius();
                h.age = d.age();
                effects.hazard(camera, h, time);
            }
            effects.begin();
            for (var b : state.bolts())
                effects.projectile(
                        camera,
                        b.x(),
                        b.z(),
                        b.vx(),
                        b.vz(),
                        b.magic(),
                        b.hostile() ? Menu.RED : b.magic() ? Menu.TEAL : Menu.GOLD,
                        time);
            effects.particles(camera);
            effects.end();
            for (CombatVfx blade : blades) {
                blade.begin();
                blade.drawBlade();
                blade.end();
            }
        }
        if (state != null)
            for (var h : state.heroes())
                if (h.health() > 0
                        && ArenaNavigation.projectileFraction(
                                        chapter,
                                        camera._position().x(),
                                        camera._position().z(),
                                        h.x(),
                                        h.z())
                                < 1) assets.drawHeroSilhouette(avatars[h.slot()]);
        EndMode3D();
        assets.finishLitPass();
        renderer.present();
    }

    public void render() {
        world();
        ui.zoom((float) GetRenderWidth() / Math.max(1, GetScreenWidth()));
        BeginMode2D(ui);
        try {
            if (confirmLeave) {
                if (state != null) hud();
                leavePanel();
                return;
            }
            if (connecting != null) {
                connectingPanel();
                return;
            }
            if (client == null) setup();
            else if (state == null) connectingPanel();
            else {
                hud();
                if (!client.error().isEmpty()) connectionError();
                else if (state.phase() != CoopState.Phase.PLAYING) lobby();
                else if (state.paused()) pausePanel();
            }

        } finally {
            EndMode2D();
        }
    }

    private void dim() {
        DrawRectangle(0, 0, GetScreenWidth(), GetScreenHeight(), Fade(Menu.INK, .91f));
    }

    private String field(String label, String value, int x, int y, int width, int id, int max) {
        Menu.panel(x, y, width, 60);
        Menu.text(label, x + 10, y + 7, 11, Menu.MUTED);
        Menu.text(
                value + (focused == id && (int) (GetTime() * 2) % 2 == 0 ? "|" : ""),
                x + 10,
                y + 29,
                18,
                Menu.TEXT);
        Vector2 mouse = GetMousePosition();
        if (IsMouseButtonPressed(MOUSE_BUTTON_LEFT)
                && mouse.x() >= x
                && mouse.x() < x + width
                && mouse.y() >= y
                && mouse.y() < y + 60) focused = id;
        if (focused == id) {
            DrawRectangleLines(x, y, width, 60, Menu.GOLD);
            for (int ch = GetCharPressed(); ch > 0; ch = GetCharPressed())
                if (!Character.isISOControl(ch) && value.length() < max)
                    value += new String(Character.toChars(ch));
            if (IsKeyPressed(KEY_BACKSPACE) && !value.isEmpty())
                value = value.substring(0, value.offsetByCodePoints(value.length(), -1));
        }
        return value;
    }

    private void setup() {
        dim();
        int x = (GetScreenWidth() - 900) / 2, y = (GetScreenHeight() - 620) / 2;
        Menu.panel(x, y, 900, 620);
        Menu.text("TWO WARDENS / CO-OP", x + 30, y + 25, 32, Menu.GOLD);
        Menu.text(
                "A shared sanctuary. Two heroes. One road to dawn.", x + 30, y + 70, 17, Menu.TEXT);
        name = field("YOUR NAME", name, x + 30, y + 100, 280, 0, 24);
        Menu.text("YOUR CLASS", x + 350, y + 90, 12, Menu.MUTED);
        for (HeroClass hero : HeroClass.values()) {
            int left = x + 350 + hero.ordinal() * 174;
            if (Menu.button(hero.label, left, y + 110, 164, true)) selected = hero;
            if (selected == hero) DrawRectangle(left, y + 156, 164, 2, Menu.GOLD);
        }
        Menu.text(
                selected == HeroClass.WARRIOR
                        ? "Wide melee strikes and close-range control."
                        : selected == HeroClass.ARCHER
                                ? "Quick arrows, crossbows and safer firing distance."
                                : "Piercing spells; heavy bolts heal an injured ally.",
                x + 350,
                y + 166,
                13,
                Menu.TEAL);
        Menu.text(
                "HOST DIFFICULTY / ENCOUNTERS ARE SCALED FOR TWO", x + 30, y + 190, 13, Menu.MUTED);
        for (Difficulty d : Difficulty.values()) {
            int left = x + 30 + d.ordinal() * 210;
            if (Menu.button(d.label, left, y + 212, 200, true)) difficulty = d;
            if (difficulty == d) DrawRectangle(left, y + 258, 200, 2, Menu.GOLD);
        }
        if (Menu.button("HOST NEW GAME", x + 30, y + 282, 260, connecting == null))
            startHost(false);
        if (Menu.button(
                "RESUME CO-OP",
                x + 310,
                y + 282,
                260,
                connecting == null && Files.exists(checkpoint))) startHost(true);
        portText = field("HOST PORT", portText, x + 610, y + 270, 260, 3, 5);
        DrawLine(x + 30, y + 345, x + 870, y + 345, Menu.LINE);
        address = field("HOST ADDRESS / IP:PORT", address, x + 30, y + 369, 460, 1, 80);
        code = field("SESSION CODE", code, x + 510, y + 369, 160, 2, 6);
        if (Menu.button("PASTE INVITE", x + 690, y + 379, 180, true)) {
            String invite = GetClipboardText().getString();
            String[] parts = invite.split("\\|", -1);
            if (parts.length == 3 && parts[0].equals("OUTPOST COOP 1")) {
                address = parts[1];
                code = parts[2];
            } else message = "Copy the invite from the host's lobby first.";
        }
        if (Menu.primaryButton("JOIN SESSION", x + 30, y + 444, 460)) {
            if (!name.isBlank()) join(address, code);
            else message = "Enter your name first.";
        }
        Menu.text(
                "Use the same Wi-Fi/LAN or a private VPN. Share the host address and six-digit"
                        + " code.",
                x + 30,
                y + 506,
                14,
                Menu.TEXT);
        Menu.text(
                "Co-op checkpoints and upgrades are separate from your solo wardens.",
                x + 30,
                y + 530,
                14,
                Menu.MUTED);
        if (Menu.button("MAIN MENU", x + 30, y + 558, 260, true)) finished = true;
        if (!message.isEmpty()) Menu.wrapped(message, x + 310, y + 555, 560, 14, Menu.RED);
    }

    private void startHost(boolean resume) {
        try {
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535 || name.isBlank()) throw new NumberFormatException();
            host(port, resume);
        } catch (NumberFormatException bad) {
            message = "Enter a name and a port between 1 and 65535.";
        }
    }

    private void connectingPanel() {
        dim();
        int x = (GetScreenWidth() - 520) / 2, y = (GetScreenHeight() - 190) / 2;
        Menu.panel(x, y, 520, 190);
        Menu.center("CONNECTING TO YOUR PARTNER", x + 260, y + 30, 23, Menu.GOLD);
        Menu.center(
                "The host and guest must use the same game version.",
                x + 260,
                y + 78,
                15,
                Menu.TEXT);
        if (Menu.button("CANCEL", x + 130, y + 120, 260, true)) {
            attempt++;
            connecting = null;
            if (client != null) client.close();
            client = null;
            state = null;
        }
    }

    private void heroPanel(CoopState.Hero h, int x, int y, int width) {
        Menu.panel(x, y, width, 112);
        Color color = h.slot() == client.slot() ? Menu.GOLD : Menu.TEAL;
        String title = h.name() + " / " + HeroClass.values()[h.hero()].label;
        int size = 18;
        while (size > 12 && Menu.measure(title, size) > width - 24) size--;
        Menu.text(title, x + 12, y + 10, size, color);
        Menu.text(
                h.connected()
                        ? (h.health() <= 0
                                ? "DOWNED / PARTNER HOLD G TO REVIVE"
                                : h.ready()
                                        ? "READY"
                                        : state.phase() == CoopState.Phase.PLAYING
                                                        && Math.hypot(h.x(), h.z()) < 6
                                                ? "SANCTUARY / +12 HEALTH PER SECOND"
                                                : "LEVEL " + (state.chapter() + 1))
                        : "DISCONNECTED / GAME PAUSED",
                x + 12,
                y + 36,
                11,
                h.health() <= 0 ? Menu.RED : Menu.MUTED);
        meter(x + 12, y + 57, width - 24, h.health(), h.maxHealth(), "HEALTH", Menu.TEAL);
        meter(
                x + 12,
                y + 83,
                width - 24,
                h.stamina(),
                h.maxStamina(),
                h.hero() == HeroClass.MAGE.ordinal() ? "MANA" : "STAMINA",
                h.hero() == HeroClass.MAGE.ordinal() ? Menu.color(65, 135, 235) : Menu.GOLD);
    }

    private void meter(
            int x, int y, int width, float value, float maximum, String label, Color color) {
        DrawRectangle(x, y, width, 19, Menu.INK);
        DrawRectangle(
                x,
                y,
                Math.round(width * Math.max(0, Math.min(1, value / maximum))),
                19,
                Fade(color, .65f));
        Menu.text(label, x + 6, y + 3, 11, Menu.TEXT);
        String text = Math.round(value) + " / " + Math.round(maximum);
        Menu.text(text, x + width - Menu.measure(text, 11) - 6, y + 3, 11, Menu.TEXT);
    }

    private void hud() {
        int w = GetScreenWidth(), h = GetScreenHeight();
        CoopState.Hero me = own(), other = partner();
        if (me == null) return;
        Enemy focused = enemyFocus.selected();
        if (focused != null && state.phase() == CoopState.Phase.PLAYING)
            targetFrameScreen.draw(
                    focused,
                    focused.distance(me.x(), me.z()),
                    settings.controls.label(Config.Controls.Action.FOCUS));
        heroPanel(me, 16, 18, 280);
        if (me.credits() > 0) {
            int uw = 460, ux = w - uw - 16, uy = 18;
            Menu.panel(ux, uy, uw, 85);
            Menu.icon("crown", ux + 12, uy + 12, 20, Menu.GOLD);
            Menu.text(
                    me.credits() + " UPGRADE POINT" + (me.credits() > 1 ? "S" : "") + " AVAILABLE",
                    ux + 38,
                    uy + 12,
                    14,
                    Menu.GOLD);
            if (Menu.button("[1] +25 HP", ux + 12, uy + 38, 138, true) || IsKeyPressed(KEY_ONE))
                pendingButtons |= CoopInput.VITALITY;
            if (Menu.button("[2] +15% DMG", ux + 158, uy + 38, 138, true) || IsKeyPressed(KEY_TWO))
                pendingButtons |= CoopInput.POWER;
            if (Menu.button(
                            me.hero() == HeroClass.MAGE.ordinal() ? "[3] +25 MANA" : "[3] +25 STAM",
                            ux + 304,
                            uy + 38,
                            144,
                            true)
                    || IsKeyPressed(KEY_THREE)) pendingButtons |= CoopInput.ENDURANCE;
        }
        int center = w / 2;
        Menu.panel(center - 170, 18, 340, 95);
        Menu.center(
                "CO-OP / " + Campaign.CHAPTERS[state.chapter()].title(), center, 30, 15, Menu.GOLD);
        Menu.center(
                "WAVE "
                        + Math.max(0, state.wave() - Campaign.CHAPTERS[state.chapter()].first() + 1)
                        + " / "
                        + Campaign.CHAPTERS[state.chapter()].waves()
                        + " / "
                        + Difficulty.values()[state.difficulty()].label,
                center,
                57,
                14,
                Menu.TEXT);
        long alive = state.enemies().stream().filter(e -> e.health() > 0).count();
        Menu.center(
                alive + " ENEMIES / " + state.queued() + " INCOMING", center, 83, 12, Menu.MUTED);
        Menu.panel(center - 180, h - 115, 360, 47);
        Menu.center(
                "SANCTUARY / "
                        + Math.round(state.baseHealth())
                        + " / "
                        + Math.round(state.baseMax()),
                center,
                h - 108,
                13,
                Menu.GOLD);
        Menu.bar(center - 163, h - 86, 326, state.baseHealth(), state.baseMax(), Menu.TEAL);
        Menu.panel(16, h - 57, w - 32, 42);
        Menu.center(
                "WASD MOVE / SHIFT SPRINT / LMB OR SPACE ATTACK / Q CHARGE / CTRL DODGE / F PARRY /"
                        + " G REVIVE / H POTION / ESC PAUSE",
                center,
                h - 43,
                11,
                Menu.TEXT);
        Menu.text(
                "POTIONS / " + me.potions() + "    TEAM DEFEATS / " + state.kills(),
                24,
                h - 91,
                12,
                Menu.TEXT);
        if (me.repair() > 0) {
            Menu.panel(center - 210, h - 178, 420, 49);
            Menu.center("REPAIRING SANCTUARY / COST: 1 POTION", center, h - 170, 13, Menu.TEAL);
            Menu.bar(center - 190, h - 146, 380, me.repair(), 3, Menu.TEAL);
        } else if (Math.hypot(me.x(), me.z()) < 6 && state.baseHealth() < state.baseMax())
            Menu.center(
                    "HOLD G FOR 3s / REPAIR +100 / COST: 1 POTION", center, h - 137, 12, Menu.TEAL);
        Camera3D camera = cameraControl.getCamera();
        for (var p : state.heroes()) {
            Vector2 s =
                    GetWorldToScreen(
                            v(
                                    avatars[p.slot()].getPosition().x(),
                                    p.health() <= 0
                                            ? 1
                                            : p.hero() == HeroClass.MAGE.ordinal() ? 3.5f : 3.1f,
                                    avatars[p.slot()].getPosition().z()),
                            camera);
            if (p.slot() != client.slot()) {
                float px = Math.max(55, Math.min(w - 55, s.x())),
                        py = Math.max(150, Math.min(h - 145, s.y()));
                String tag =
                        p.name()
                                + (p.health() <= 0
                                        ? " / HOLD G"
                                        : " / "
                                                + Math.round(
                                                        Math.hypot(me.x() - p.x(), me.z() - p.z()))
                                                + "m");
                int tagWidth = Menu.measure(tag, 14),
                        tagX = Math.max(8, Math.min(w - tagWidth - 8, (int) px - tagWidth / 2));
                DrawRectangle(tagX - 6, (int) py - 4, tagWidth + 12, 24, Fade(Menu.INK, .88f));
                Menu.text(tag, tagX, (int) py, 14, p.health() <= 0 ? Menu.RED : allyGreen);
                if (p.health() <= 0)
                    Menu.bar((int) px - 30, (int) py + 24, 140, p.revive(), 3, Menu.TEAL);
                else {
                    int barX = (int) px - 48, barY = (int) py + 23;
                    DrawRectangle(barX - 1, barY - 1, 98, 10, Menu.INK);
                    DrawRectangle(barX, barY, 96, 8, Menu.PANEL);
                    DrawRectangle(
                            barX, barY, Math.round(96 * p.health() / p.maxHealth()), 8, allyGreen);
                }
            }
        }
        for (var e : state.enemies())
            if (e.health() > 0) {
                Vector2 s = GetWorldToScreen(v(e.x(), e.variant() == 5 ? 6 : 2.3f, e.z()), camera);
                int ex = (int) s.x(), ey = (int) s.y();
                Menu.bar(
                        ex - 26,
                        ey,
                        52,
                        e.health(),
                        e.maxHealth(),
                        e.variant() == 5 ? Menu.GOLD : Menu.RED);
                if (e.attack() > 0) {
                    String label = e.variant() == 5 ? BossMove.values()[e.move()].name() : "ATTACK";
                    DrawRectangle(ex - 44, ey - 19, 88, 17, Menu.INK);
                    Menu.center(
                            label,
                            ex,
                            ey - 17,
                            10,
                            e.variant() == 5 && !BossMove.values()[e.move()].parryable
                                    ? Menu.RED
                                    : Menu.GOLD);
                }
            }
        if (me.health() <= 0) {
            Menu.panel(center - 230, h / 2 - 40, 460, 80);
            Menu.center("DOWNED / YOUR PARTNER CAN REVIVE YOU", center, h / 2 - 23, 18, Menu.RED);
            Menu.center(
                    "They must hold G nearby for 3 seconds without taking damage.",
                    center,
                    h / 2 + 7,
                    13,
                    Menu.TEXT);
        }
        if (server != null && !server.warning().isEmpty())
            Menu.center(server.warning(), center, 138, 13, Menu.RED);
    }

    private void lobby() {
        dim();
        int x = (GetScreenWidth() - 900) / 2, y = (GetScreenHeight() - 560) / 2;
        Menu.panel(x, y, 900, 560);
        CoopState.Hero me = own();
        String title =
                switch (state.phase()) {
                    case VICTORY -> "THE VALLEY SEES DAWN / TOGETHER";
                    case DEFEAT -> "THE WATCH IS NOT OVER";
                    case INTERMISSION -> "CHAPTER COMPLETE / TWO WARDENS";
                    default -> "TWO WARDENS / PRIVATE LOBBY";
                };
        Menu.center(title, x + 450, y + 24, 27, Menu.GOLD);
        if (state.phase() == CoopState.Phase.VICTORY) {
            Menu.center("Both wardens brought back the dawn.", x + 450, y + 93, 24, Menu.TEXT);
            Menu.center(
                    state.kills() + " enemies defeated together / Four chapters cleared",
                    x + 450,
                    y + 144,
                    18,
                    Menu.TEAL);
            Menu.center("Thank you for sharing the watch.", x + 450, y + 206, 22, Menu.GOLD);
            if (Menu.primaryButton("MAIN MENU", x + 230, y + 355, 440)) confirmLeave = true;
            return;
        }
        if (server != null && state.phase() == CoopState.Phase.LOBBY) {
            String ip = addresses.get(inviteIndex % addresses.size());
            Menu.text("HOST ADDRESS / " + ip + ":" + server.port(), x + 30, y + 83, 18, Menu.TEXT);
            Menu.text("SESSION CODE / " + server.code(), x + 30, y + 118, 24, Menu.TEAL);
            if (Menu.button("COPY INVITE", x + 610, y + 76, 260, true)) {
                SetClipboardText(
                        "OUTPOST COOP 1|" + ip + ":" + server.port() + "|" + server.code());
                message = "Invite copied. Send it to your partner.";
            }
            if (addresses.size() > 1 && Menu.button("NEXT ADDRESS", x + 610, y + 126, 260, true))
                inviteIndex++;
        } else {
            Menu.wrapped(
                    state.phase() == CoopState.Phase.DEFEAT
                            ? "Both players can ready up to retry this chapter with restored health"
                                    + " and supplies."
                            : state.phase() == CoopState.Phase.INTERMISSION
                                    ? Campaign.CHAPTERS[state.chapter()].aftermath()
                                    : "Connected to "
                                            + connectAddress
                                            + ":"
                                            + connectPort
                                            + ". Both players must ready up to begin.",
                    x + 30,
                    y + 87,
                    830,
                    18,
                    Menu.TEXT);
            if (state.phase() == CoopState.Phase.INTERMISSION)
                Menu.text(
                        "NEXT / " + Campaign.CHAPTERS[Math.min(3, state.chapter() + 1)].title(),
                        x + 30,
                        y + 142,
                        18,
                        Menu.TEAL);
        }
        int card = 0;
        for (var hero : state.heroes()) {
            heroPanel(hero, x + 30 + card * 430, y + 184, 410);
            card++;
        }
        if (state.heroes().size() < 2) {
            Menu.panel(x + 460, y + 184, 410, 112);
            Menu.center("WAITING FOR YOUR PARTNER", x + 665, y + 223, 20, Menu.MUTED);
        }
        if (me != null) {
            Menu.text(
                    me.credits() > 0
                            ? "UPGRADE POINTS / " + me.credits() + " / CHOOSE YOUR BUILD"
                            : "YOUR BUILD / +"
                                    + (me.vitality() * 25)
                                    + " HP / +"
                                    + (me.power() * 15)
                                    + "% DAMAGE / +"
                                    + (me.endurance() * 25)
                                    + (me.hero() == HeroClass.MAGE.ordinal()
                                            ? " MANA"
                                            : " STAMINA"),
                    x + 30,
                    y + 320,
                    15,
                    Menu.GOLD);
            if (Menu.button("+25 MAX HEALTH", x + 30, y + 350, 265, me.credits() > 0))
                pendingButtons |= CoopInput.VITALITY;
            if (Menu.button("+15% DAMAGE", x + 317, y + 350, 265, me.credits() > 0))
                pendingButtons |= CoopInput.POWER;
            if (Menu.button(
                    me.hero() == HeroClass.MAGE.ordinal() ? "+25 MANA" : "+25 STAMINA",
                    x + 605,
                    y + 350,
                    265,
                    me.credits() > 0)) pendingButtons |= CoopInput.ENDURANCE;
            if (Menu.primaryButton(
                    me.ready()
                            ? "UNREADY [ENTER]"
                            : state.phase() == CoopState.Phase.DEFEAT
                                    ? "READY TO RETRY [ENTER]"
                                    : "READY [ENTER]",
                    x + 30,
                    y + 420,
                    550)) pendingButtons |= CoopInput.READY;
        }
        if (Menu.button("LEAVE SESSION", x + 605, y + 420, 265, true)) confirmLeave = true;
        Menu.text(
                message.isEmpty()
                        ? "Shared enemy pressure / No friendly fire / Hold G beside a downed"
                                + " partner to revive"
                        : message,
                x + 30,
                y + 495,
                14,
                Menu.TEXT);
        Menu.text(
                "AETHELGARD CITADEL / Goran (Forge: Tier 1 Lv 3, Tier 2 Lv 6, Tier 3 Lv 10) /"
                        + " Elyndra (Spire & Hearth) / Commander Sean (Deployment)",
                x + 30,
                y + 522,
                12,
                Menu.TEAL);
    }

    private void pausePanel() {
        dim();
        int x = (GetScreenWidth() - 600) / 2, y = (GetScreenHeight() - 300) / 2;
        Menu.panel(x, y, 600, 300);
        Menu.center("PARTY PAUSED", x + 300, y + 27, 30, Menu.GOLD);
        boolean disconnected =
                state.heroes().size() < 2 || state.heroes().stream().anyMatch(p -> !p.connected());
        Menu.wrapped(
                disconnected
                        ? "Waiting for your partner to reconnect. Combat and the sanctuary are"
                                + " frozen."
                        : "Each warden controls their own pause. Combat resumes when both are"
                                + " ready.",
                x + 30,
                y + 85,
                540,
                19,
                Menu.TEXT);
        if (Menu.button(
                "RESUME MY GAME [ESC]", x + 30, y + 180, 540, own() != null && own().paused()))
            pendingButtons |= CoopInput.PAUSE;
        if (Menu.button("LEAVE SESSION", x + 30, y + 235, 540, true)) confirmLeave = true;
    }

    private void connectionError() {
        dim();
        int x = (GetScreenWidth() - 620) / 2, y = (GetScreenHeight() - 250) / 2;
        Menu.panel(x, y, 620, 250);
        Menu.center("CONNECTION INTERRUPTED", x + 310, y + 25, 28, Menu.GOLD);
        Menu.wrapped(client.error(), x + 30, y + 82, 560, 18, Menu.TEXT);
        if (Menu.button("RECONNECT", x + 30, y + 168, 270, true))
            connect(connectAddress, connectPort, connectSlot, connectCode);
        if (Menu.button("MAIN MENU", x + 320, y + 168, 270, true)) confirmLeave = true;
    }

    private void leavePanel() {
        dim();
        int x = (GetScreenWidth() - 600) / 2, y = (GetScreenHeight() - 245) / 2;
        Menu.panel(x, y, 600, 245);
        Menu.center("LEAVE CO-OP?", x + 300, y + 25, 30, Menu.GOLD);
        Menu.wrapped(
                server != null
                        ? "Leaving closes your hosted session. You can resume the chapter"
                                + " checkpoint later; your partner can join the new session."
                        : "Your partner's game pauses when you disconnect. Rejoin with the same"
                                + " class and session code to resume.",
                x + 30,
                y + 78,
                540,
                18,
                Menu.TEXT);
        if (Menu.button("STAY", x + 30, y + 177, 250, true)) confirmLeave = false;
        if (Menu.button("LEAVE", x + 320, y + 177, 250, true)) finished = true;
    }

    @Override
    public void close() {
        disposed = true;
        attempt++;
        if (client != null) client.close();
        if (server != null) server.close();
        for (Player p : avatars) if (p != null) p.dispose();
        for (CombatVfx blade : blades) blade.close();
        effects.close();
        audio.close();
        assets.close();
        renderer.close();
    }
}
