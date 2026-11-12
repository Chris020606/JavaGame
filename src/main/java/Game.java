import static Config.Controls.Action.*;

import static com.raylib.Colors.WHITE;
import static com.raylib.Raylib.*;

import Config.Controls;

import Entities.BossMove;
import Entities.CombatDiscipline;
import Entities.Difficulty;
import Entities.Enemy;
import Entities.Player;
import Entities.SquadTactics;

import Scene.CameraController;
import Scene.Scene;
import Scene.SceneManager;

import assets.AssetPaths;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * Coordinates the single-player session: input, simulation, screens and persistence.
 *
 * <p>Main owns this object's native-resource lifetime. Call update before render on the window
 * thread, and dispose before closing Raylib. Gameplay advances only in PLAYING; inventory and
 * workshop overlays pause combat. Draw-time buttons can change state, so inventory textures are
 * prepared before the UI pass rather than lazily from a button callback.
 */
public class Game {
    private final java.util.function.BooleanSupplier mouseAttack;
    private final EnemyFocus enemyFocus = new EnemyFocus();

    int focusKey() {
        return settings.controls.key(FOCUS);
    }

    Enemy focusedEnemy() {
        return enemyFocus.selected();
    }

    private final TargetFrameScreen targetFrameScreen = new TargetFrameScreen();
    private final ChapterSelectScreen chapterSelectScreen = new ChapterSelectScreen();
    private GameState chapterMapReturn = GameState.PAUSED;
    private boolean pointerAim;
    private boolean coopRequested;

    boolean consumeCoopRequest() {
        boolean value = coopRequested;
        coopRequested = false;
        return value;
    }

    void pointerAim(boolean enabled) {
        pointerAim = enabled;
    }

    private final Settings settings = new Settings();
    private final PlayerSaves playerSaves = new PlayerSaves(AppData.directory());
    private java.util.List<PlayerSaves.Entry> playerHistory = java.util.List.of();
    private int historyPage;
    private PlayerSaves.Entry pendingDelete;
    private String historyMessage = "";
    private Tutorial tutorial;
    private SaveGame.Snapshot practiceReturn;
    private GameState practiceReturnState;
    private boolean practiceReturnPlaytest;
    private Tutorial.Step preparedStep;
    private final CombatMastery mastery = new CombatMastery();
    private float hitPause, hitMarker;
    private float sanctuaryGhostHealth = -1, sanctuaryGhostDelay;
    private float bossGhostHealth = -1, bossGhostDelay;
    private boolean activeHeavy, activeCharged;
    private String lastDiscoveredLandmark = "";
    private float landmarkBannerTime;
    private final ArrayList<Enemy> shotTargets = new ArrayList<>();

    Tutorial tutorial() {
        return tutorial;
    }

    void beginTutorial() {
        if (tutorial != null) return;
        practiceReturnState = state;
        practiceReturnPlaytest = playtest;
        practiceReturn =
                new SaveGame.Snapshot(
                        player.getName(),
                        player.getPosition().x(),
                        player.getPosition().z(),
                        player.getRotation().y(),
                        progress,
                        director.snapshot(),
                        java.util.List.copyOf(enemies),
                        Arena.HIGHLANDS,
                        player.discipline().stamina(),
                        dayNight.phase());
        tutorial = new Tutorial();
        preparedStep = null;
        playtest = false;
        startPracticeArena();
        storyTime = 0;
    }

    void beginCombatTutorial() {
        if (tutorial != null && tutorial.step() == Tutorial.Step.MOVE) {
            tutorial.movement(4);
        } else if (tutorial == null) {
            tutorial = new Tutorial();
            tutorial.movement(4);
        }
        practiceReturnState = state;
        practiceReturnPlaytest = playtest;
        practiceReturn =
                new SaveGame.Snapshot(
                        player.getName(),
                        player.getPosition().x(),
                        player.getPosition().z(),
                        player.getRotation().y(),
                        progress,
                        director.snapshot(),
                        java.util.List.copyOf(enemies),
                        Arena.HIGHLANDS,
                        player.discipline().stamina(),
                        dayNight.phase());
        preparedStep = null;
        playtest = false;
        startPracticeArena();
        storyTime = 0;
    }

    private void startPracticeArena() {
        applyHeroModel();
        guardianCooldown = guardianFlash = 0;
        storyTime = 12;
        hudCharging = false;
        mastery.reset();
        hitPause = hitMarker = 0;
        if (progress == null) {
            progress = new Progression(selectedClass, selectedDifficulty);
        }
        if (!progress.hasWeapon()) {
            progress.grantStarterWeapon();
        }
        player.setMovementSpeed(selectedClass.speed);
        inventory = false;
        fallen.clear();
        outcomeTime = 0;
        heroShots.clear();
        enemies.clear();
        projectiles.clear();
        wave = 0;
        nextWave = 5;
        attackCooldown = slashTime = noticeTime = heavyCharge = 0;
        bossHazards.clear();
        workshop = false;
        player.getPosition().x(0).y(0).z(4);
        player.resetCombat();
        director.reset();
        director.difficulty(progress.difficulty());
        dayNight.phase(DayNightCycle.initial(0));
        applyProgression();
        lastHeroX = 0;
        lastHeroZ = 4;
        feedback.clear();
        vfx.clear();
        combo = 0;
        comboWindow = inputBuffer = attackElapsed = hurtFlash = 0;
        impactPending = false;
        player.setName(settings.playerName);
        menuMessage = "";
        state = GameState.PLAYING;
    }

    void finishTutorial() {
        if (tutorial == null) return;
        tutorial = null;
        preparedStep = null;
        enemies.clear();
        enemyFocus.clear();
        heroShots.clear();
        projectiles.clear();
        bossHazards.clear();
        fallen.clear();
        player.resetCombat();
        impactPending = false;
        settings.tutorialSeen = true;
        persistSettings();
        if (practiceReturn != null) {
            restoreSnapshot(practiceReturn, practiceReturnPlaytest);
            state = practiceReturnState;
            practiceReturn = null;
            applyProgression();
            notifyPlayer("PRACTICE COMPLETE / Your adventure is ready.");
        } else if (state == GameState.CITADEL) {
            // Keep the player in the Citadel right where they are!
            // Do NOT grant starter weapon automatically; do NOT deploy to mission.
            applyProgression();
            notifyPlayer("TUTORIAL DISMISSED / Explore the Citadel freely.");
        } else {
            // In combat mission: clean up practice dummy and start real battle / wave
            enemies.clear();
            enemyFocus.clear();
            applyProgression();
            notifyPlayer("PRACTICE COMPLETE / Your adventure is ready.");
        }
    }

    private void prepareTutorial() {
        if (state != GameState.PLAYING || tutorial == null || preparedStep == tutorial.step())
            return;
        preparedStep = tutorial.step();
        // Retain the selected practice actor between focus and strike so the frame stays visible.
        boolean keepPartner =
                preparedStep == Tutorial.Step.STRIKE
                        && enemyFocus.selected() != null
                        && enemies.contains(enemyFocus.selected());
        if (!keepPartner) {
            enemies.clear();
            enemyFocus.clear();
        }
        heroShots.clear();
        projectiles.clear();
        player.discipline().reset();
        if (!keepPartner
                && (preparedStep == Tutorial.Step.FOCUS
                        || preparedStep == Tutorial.Step.STRIKE
                        || preparedStep == Tutorial.Step.CHARGE
                        || preparedStep == Tutorial.Step.PARRY)) {
            Enemy partner =
                    new Enemy(
                            player.getPosition().x() + 2.2f,
                            player.getPosition().z(),
                            1,
                            0,
                            .5f,
                            Difficulty.EASY,
                            6.0f);
            enemies.add(partner);
            player.aimAt(partner.x, partner.z);
        }
        if (preparedStep == Tutorial.Step.HAVEN) progress.damagePlayer(25);
        audio.play("upgrade");
    }

    private void connected(Enemy enemy, boolean heavy, boolean charged) {
        if (heavy || progress.heroClass() == HeroClass.WARRIOR)
            sceneManager.getOpenScene().getCameraController().impact(heavy ? .10f : .035f);
        if (tutorial == null && charged)
            progress.chapterFeats().record(chapterIndex(), ChapterFeats.Action.CHARGED_HIT);
        hitMarker = .16f;
        float pause =
                heavy
                        ? (charged ? .05f : .038f)
                        : (progress.heroClass() == HeroClass.WARRIOR ? .025f : 0);
        if (pause > 0) hitPause = Math.max(hitPause, pause);
        if (!heavy) mastery.lightHit(progress.heroClass());
        if (tutorial != null) {
            tutorial.hit(charged);
            if (enemy.health < 60) {
                enemy.health = enemy.maxHealth;
            }
        }
    }

    private final Path settingsPath = AppData.directory().resolve("settings.properties"),
            savePath = AppData.directory().resolve("save.properties");
    private final Path
            checkpointPath = AppData.directory().resolve("chapter-checkpoint.properties"),
            playtestCheckpointPath = AppData.directory().resolve("playtest-checkpoint.properties");
    private final Path playtestSavePath = AppData.directory().resolve("playtest-save.properties");
    private boolean playtest, maxPlaytestTraining = true;
    private int playtestChapter, playtestTime;
    private final DayNightCycle dayNight = new DayNightCycle();
    private static final float[] PLAYTEST_TIMES = {.36f, .5f, .73f, 0};
    private static final String[] TIME_LABELS = {"MORNING", "NOON", "SUNSET", "MIDNIGHT"};

    void dayPhase(float phase) {
        dayNight.phase(phase);
    }

    float dayPhase() {
        return dayNight.phase();
    }

    private GameState playtestReturn = GameState.MAIN_MENU;
    private GameState settingsReturn = GameState.MAIN_MENU;
    private Controls.Action rebinding;
    private String nameDraft = "", menuMessage = "";
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private Player player;
    private GameState journalReturn = GameState.MAIN_MENU;
    private boolean journalFeats;
    private int journalTab;
    private int featsChapter;
    private GameState blessingReturn = GameState.PLAYING;
    private GameState pauseReturn = GameState.PLAYING;
    private float storyTime;
    private Difficulty selectedDifficulty = Difficulty.NORMAL;
    private float lastHeroX, lastHeroZ;
    private final HudScreen hudScreen = new HudScreen();
    private boolean hudCharging, uiMouseCaptured;
    private final CitadelHub citadelHub = new CitadelHub();
    private final CitadelScreen citadelScreen = new CitadelScreen();
    private final WorkshopScreen workshopScreen = new WorkshopScreen();
    private final EquipmentScreen equipmentScreen = new EquipmentScreen();
    private final HeroPortrait heroPortrait = new HeroPortrait();
    private HeroClass selectedClass = HeroClass.WARRIOR;
    private boolean inventory;
    private final ArrayList<HeroShot> heroShots = new ArrayList<>();
    private final WorldAssets assets = new WorldAssets();
    private final ModernRenderer renderer = new ModernRenderer();
    private final ArrayList<BossHazard> bossHazards = new ArrayList<>();
    private GameState state = GameState.MAIN_MENU;
    private SceneManager sceneManager = new SceneManager();
    private Progression progress = new Progression();
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Enemy> fallen = new ArrayList<>();
    private float outcomeTime;
    private float guardianCooldown, guardianFlash, guardianX, guardianZ, solarWardCooldown;
    private final Random random = new Random();
    private final WaveDirector director = new WaveDirector(random);
    private final GameAudio audio = new GameAudio();
    private final Color skyTopBlend = new Color(), skyHorizonBlend = new Color();
    private final CombatFeedback feedback = new CombatFeedback();
    private final CombatVfx vfx = new CombatVfx();
    private int wave, combo;
    private float comboWindow, inputBuffer, attackElapsed, hurtFlash;
    private boolean bufferedHeavy, bufferedLunge, bufferedTimed, impactPending;
    private float heavyCharge, releasedCharge;
    private Combat.Attack activeAttack = Combat.attack(1, false);
    private float nextWave = 5, attackCooldown, slashTime, noticeTime;
    private String notice = "";
    private boolean workshop;
    private boolean quitRequested;
    private boolean photoMode;

    void photoMode(boolean enabled) {
        photoMode = enabled;
    }

    private final Camera2D uiCamera = new Camera2D().zoom(1);

    public Game() {
        this(() -> IsMouseButtonPressed(MOUSE_BUTTON_LEFT));
        pointerAim = true;
    }

    Game(java.util.function.BooleanSupplier mouseAttack) {
        this.mouseAttack = mouseAttack;
        settings.load(settingsPath);
        selectedDifficulty = settings.difficulty;
        menuMessage = settings.warning;
        Scene scene = sceneManager.createScene("Outpost");
        sceneManager.setOpenScene(scene);
        Player knight = new Player();
        knight.setName(settings.playerName);
        knight.setControls(settings.controls);
        knight.setPosition(new Vector3().x(0).y(0).z(4));
        knight.setModel(LoadModel(AssetPaths.path("resources/outpost/hero_warrior.glb")));
        knight.loadAnimations(AssetPaths.path("resources/outpost/hero_warrior.glb"));
        knight.setAnimationFPS(60);
        knight.resetCombat();
        setPlayer(knight);
        scene.getCameraController().update(0);
        applySettings();
    }

    private HeroClass modelClass = HeroClass.WARRIOR;

    private void applyHeroModel() {
        if (modelClass != selectedClass) {
            player.dispose();
            String path =
                    AssetPaths.path(
                            selectedClass == HeroClass.WARRIOR
                                    ? "resources/outpost/hero_warrior.glb"
                                    : selectedClass == HeroClass.ARCHER
                                            ? "resources/outpost/hero_archer.glb"
                                            : "resources/outpost/hero_mage.glb");
            player.setModel(LoadModel(path));
            player.loadAnimations(path);
            if (player.getModel().meshCount() == 0 || player.getAnimationCount() == 0)
                throw new IllegalStateException("Missing hero model or animations: " + path);
            for (int i = 0; i < player.getAnimationCount(); i++)
                if (!IsModelAnimationValid(
                        player.getModel(), new ModelAnimation(player.getAnimation()).position(i)))
                    throw new IllegalStateException("Hero animation skeleton mismatch: " + path);
            modelClass = selectedClass;
        }
        player.setWeaponStyle(selectedClass.ordinal());
        player.setMovementSpeed(selectedClass.speed);
    }

    Arena arena() {
        return Arena.HIGHLANDS;
    }

    Campaign.Chapter chapter() {
        return director.chapter();
    }

    int chapterIndex() {
        return director.chapterIndex();
    }

    private void start() {
        applyHeroModel();
        guardianCooldown = guardianFlash = 0;
        storyTime = 12;
        hudCharging = false;
        mastery.reset();
        hitPause = hitMarker = 0;
        progress =
                playtest
                        ? Progression.playtest(
                                selectedClass, selectedDifficulty, maxPlaytestTraining)
                        : new Progression(selectedClass, selectedDifficulty);
        if (!playtest) {
            progress.setHasWeapon(false);
        }
        player.setMovementSpeed(selectedClass.speed);
        inventory = false;
        fallen.clear();
        outcomeTime = 0;
        heroShots.clear();
        enemies.clear();
        projectiles.clear();
        wave = 0;
        nextWave = 5;
        attackCooldown = slashTime = noticeTime = heavyCharge = 0;
        bossHazards.clear();
        workshop = false;
        player.getPosition().x(0).y(0).z(4);
        player.resetCombat();
        director.reset();
        director.difficulty(progress.difficulty());
        if (playtest) {
            director.restore(
                    new WaveDirector.State(
                            Campaign.CHAPTERS[playtestChapter].first() - 1, 0, 0, 5, 0, 0, 21));
            wave = director.wave();
            inventory = true;
        }
        dayNight.phase(playtest ? PLAYTEST_TIMES[playtestTime] : DayNightCycle.initial(0));
        applyProgression();
        lastHeroX = 0;
        lastHeroZ = 4;
        feedback.clear();
        vfx.clear();
        combo = 0;
        comboWindow = inputBuffer = attackElapsed = hurtFlash = 0;
        impactPending = false;
        player.setName(settings.playerName);
        menuMessage = "";
        state = GameState.PLAYING;
        writeCheckpoint();
    }

    public void travelToCitadel() {
        state = GameState.CITADEL;
        workshop = false;
        inventory = false;
        citadelHub.closeNpc();
        enemies.clear();
        fallen.clear();
        heroShots.clear();
        projectiles.clear();
        bossHazards.clear();
        player.clearBounds();
        player.getPosition().x(0).y(0).z(2);
        player.getRotation().y(180);
        lastHeroX = 0;
        lastHeroZ = 2;
        player.resetCombat();
        CameraController cam = sceneManager.getOpenScene().getCameraController();
        if (cam != null) {
            cam.setYaw(0.0f);
            cam.setPitch(20.0f);
            cam.setDistance(8.0f);
            cam.update(0);
        }
        notifyPlayer(
                progress.rescuedSurvivors() > 0
                        ? "ARRIVED AT AETHELGARD CITADEL / "
                                + progress.rescuedSurvivors()
                                + " RESCUED CITIZENS SHELTERED HERE"
                        : "ARRIVED AT AETHELGARD CITADEL / TALK TO NPCS OR ENTER PORTAL");
        if (wave == 0 && !progress.hasWeapon() && tutorial == null) {
            tutorial = new Tutorial();
        }
    }

    public void deployFromCitadel() {
        state = GameState.PLAYING;
        citadelHub.closeNpc();
        player.resetBounds();
        player.getPosition().x(0).y(0).z(4);
        player.getRotation().y(0);
        applyProgression();
        player.resetCombat();
        CameraController cam = sceneManager.getOpenScene().getCameraController();
        if (cam != null) {
            cam.setYaw(0.0f);
            cam.setPitch(24.0f);
            cam.setDistance(17.0f);
            cam.update(0);
        }
        audio.play("upgrade");
        vfx.impact(0, 4, Menu.GOLD, true);
        notifyPlayer("EXPEDITION DEPLOYED / DEFEND THE PRIMORDIAL BEACON!");
        if (tutorial != null || !settings.tutorialSeen || (wave == 0 && !progress.hasWeapon())) {
            beginCombatTutorial();
        }
    }

    private void updateCitadel(float dt) {
        noticeTime = Math.max(0, noticeTime - dt);
        vfx.update(dt);
        if (citadelHub.updateTeleport(dt)) {
            deployFromCitadel();
            return;
        }
        if (citadelHub.isTeleporting()) {
            return;
        }
        if (workshop) {
            if (IsKeyPressed(KEY_ESCAPE)) {
                workshop = false;
            }
            return;
        }
        if (inventory) {
            if (settings.controls.pressed(INVENTORY)
                    || IsKeyPressed(KEY_ESCAPE)
                    || IsKeyPressed(KEY_E)
                    || IsKeyPressed(KEY_TAB)) {
                inventory = false;
            }
            return;
        }
        if (citadelHub.activeNpc() != null) {
            if (IsKeyPressed(KEY_ESCAPE)
                    || settings.controls.pressed(INTERACT)
                    || IsKeyPressed(KEY_F)) {
                citadelHub.closeNpc();
                return;
            }
            return;
        }
        if (IsKeyPressed(KEY_ESCAPE)) {
            pauseReturn = GameState.CITADEL;
            state = GameState.PAUSED;
            return;
        }
        if (settings.controls.pressed(INVENTORY) || IsKeyPressed(KEY_E) || IsKeyPressed(KEY_TAB)) {
            inventory = true;
            return;
        }
        if (settings.controls.pressed(INTERACT)
                || IsKeyPressed(KEY_F)
                || IsKeyPressed(KEY_ENTER)
                || IsKeyPressed(KEY_SPACE)) {
            CitadelNpc nearby =
                    citadelHub.checkNearbyNpc(
                            player.getPosition().x(),
                            player.getPosition().z(),
                            progress.rescuedSurvivors());
            if (nearby != null) {
                citadelHub.openNpc(nearby, progress);
                audio.play("menu");
                return;
            } else if (citadelHub.isNearPortal(
                    player.getPosition().x(), player.getPosition().z())) {
                if (!progress.hasWeapon()) {
                    notifyPlayer(
                            "UNARMED! RECEIVE A WEAPON FROM YOUR "
                                    + progress.heroClass().label.toUpperCase()
                                    + " MENTOR BEFORE DEPLOYING.");
                    audio.play("error");
                    return;
                }
                citadelHub.startTeleport();
                audio.play("upgrade");
                vfx.impact(CitadelHub.PORTAL_X, CitadelHub.PORTAL_Z, Menu.GOLD, true);
                return;
            }
        }
        if (IsKeyPressed(KEY_J)) {
            journalReturn = GameState.CITADEL;
            journalTab = 1;
            state = GameState.JOURNAL;
            audio.play("menu");
            return;
        }

        if (player.isBoundsEnabled()) {
            player.clearBounds();
        }
        sceneManager.getOpenScene().updatesEntities(dt);
        citadelHub.resolvePlayerCollision(player);

        if (tutorial != null && tutorial.step() == Tutorial.Step.MOVE) {
            float moveDist =
                    (float)
                            Math.hypot(
                                    player.getPosition().x() - lastHeroX,
                                    player.getPosition().z() - lastHeroZ);
            if (moveDist > 0.001f) {
                Tutorial.Step beforeStep = tutorial.step();
                tutorial.movement(moveDist);
                if (tutorial.step() != beforeStep) {
                    audio.play("upgrade");
                    notifyPlayer(
                            "MOVEMENT PRACTICED! Visit your class mentor to claim your weapon.");
                }
            }
        }
        lastHeroX = player.getPosition().x();
        lastHeroZ = player.getPosition().z();

        // Expedition portal step check: stepping directly into the portal stone
        if (citadelHub.isNearPortal(player.getPosition().x(), player.getPosition().z())
                && Math.abs(player.getPosition().x() - CitadelHub.PORTAL_X) <= 1.2f
                && Math.abs(player.getPosition().z() - CitadelHub.PORTAL_Z) <= 1.2f) {
            if (!progress.hasWeapon()) {
                // Like Pokemon Oak guard: player cannot leave unarmed!
                player.getPosition().z(CitadelHub.PORTAL_Z - 1.6f);
                notifyPlayer(
                        "HALT! THE WILDS ARE TOO DANGEROUS UNARMED! SPEAK WITH YOUR CLASS MENTOR"
                                + " FIRST!");
                audio.play("error");
            } else if (!citadelHub.isTeleporting()) {
                // Stepping into the active expedition portal triggers deployment
                citadelHub.startTeleport();
                audio.play("upgrade");
                vfx.impact(CitadelHub.PORTAL_X, CitadelHub.PORTAL_Z, Menu.GOLD, true);
            }
        }
    }

    private void applyProgression() {
        player.setCrossbow(progress.heroClass() == HeroClass.ARCHER && progress.weaponTier() >= 2);
        player.setHasWeapon(progress.hasWeapon());
        player.setWeaponTier(progress.hasWeapon() ? progress.weaponTier() : 0);
        player.setMovementSpeed(progress.movementSpeed());
        player.setDodgeRecovery(
                1.5f - .15f * progress.expedition().rank(Expedition.Blessing.WAYFARER));
        player.discipline().configure(progress.maxStamina(), progress.staminaRecovery());
    }

    private void aimCursor() {
        if (!pointerAim || IsMouseButtonDown(MOUSE_BUTTON_RIGHT)) return;
        Ray ray =
                GetScreenToWorldRay(
                        GetMousePosition(),
                        sceneManager.getOpenScene().getCameraController().getCamera());
        if (Math.abs(ray.direction().y()) < .0001f) return;
        float t = (1.1f - ray._position().y()) / ray.direction().y();
        if (t > 0)
            player.aimAt(
                    ray._position().x() + ray.direction().x() * t,
                    ray._position().z() + ray.direction().z() * t);
    }

    private boolean nearBase() {
        return Math.hypot(player.getPosition().x(), player.getPosition().z()) < 6;
    }

    private float sanctuaryDistance() {
        return (float) Math.hypot(player.getPosition().x(), player.getPosition().z());
    }

    private void notifyPlayer(String message) {
        notice = message;
        noticeTime = 3;
    }

    private boolean rescueLedgerOpen;
    private boolean storyOpening;
    private int storyPage;

    private final CityPresentation cityPresentation = new CityPresentation();
    private final BlessingReminder blessingReminder = new BlessingReminder();

    public void update(float dt) {
        if (rescueLedgerOpen) {
            if (IsKeyPressed(KEY_ESCAPE)) rescueLedgerOpen = false;
            return;
        }
        if (storyOpening) {
            if (IsKeyPressed(KEY_ENTER) || IsKeyPressed(KEY_SPACE)) {
                if (++storyPage >= OpeningStory.PAGES.length) storyOpening = false;
            }
            return;
        }
        dt = Math.max(0, Math.min(dt, .05f));
        if (state == GameState.CITADEL)
            citadelHub.updateCitizens(
                    dt,
                    player.getPosition().x(),
                    player.getPosition().z(),
                    progress.rescuedSurvivors());
        if (state == GameState.CITADEL)
            cityPresentation.update(dt, player.getPosition().x(), player.getPosition().z());
        applyProgression();
        if (!IsMouseButtonDown(MOUSE_BUTTON_LEFT)) uiMouseCaptured = false;
        else if (inventory || workshop || state != GameState.PLAYING) uiMouseCaptured = true;
        hitMarker = Math.max(0, hitMarker - dt);
        if (state == GameState.PLAYING && !inventory && !workshop) {
            mastery.tick(dt);
            if (hitPause > 0) {
                float held = Math.min(dt, hitPause);
                hitPause -= held;
                dt -= held;
            }
        }
        blessingReminder.update(
                dt,
                state == GameState.PLAYING
                        && tutorial == null
                        && enemies.isEmpty()
                        && director.remaining() == 0
                        && !inventory
                        && !workshop,
                progress.expedition().embers());
        prepareTutorial();
        final float presentationStep = dt;
        boolean outcome =
                state == GameState.LEVEL_COMPLETE
                        || state == GameState.VICTORY
                        || state == GameState.GAME_OVER;
        if ((state == GameState.PLAYING || outcome) && !inventory && !workshop) {
            fallen.removeIf(e -> {
                e.deathTime += presentationStep;
                return e.deathTime > CorpsePresentation.lifetime(e.boss());
            });
            if (outcome) {
                outcomeTime += dt;
                if (state == GameState.VICTORY) {
                    dayNight.phase(.26f + Math.min(1, outcomeTime / 8) * .10f);
                    if ((int) (outcomeTime * 3) != (int) ((outcomeTime - dt) * 3)
                            && outcomeTime < 12) {
                        float angle = outcomeTime * 2.4f;
                        vfx.impact(
                                player.getPosition().x() + (float) Math.cos(angle) * 4,
                                player.getPosition().z() + (float) Math.sin(angle) * 4,
                                Menu.GOLD,
                                true);
                    }
                }
                noticeTime = Math.max(0, noticeTime - dt);
                landmarkBannerTime = Math.max(0, landmarkBannerTime - dt);
                feedback.update(dt);
                vfx.update(dt);
                player.poseForOutcome(state != GameState.GAME_OVER, outcomeTime);
            }
        }
        boolean titleAudio =
                state == GameState.PLAYTEST_SETUP && playtestReturn == GameState.MAIN_MENU
                        || state == GameState.MAIN_MENU
                        || state == GameState.LOGIN
                        || state == GameState.PLAYER_HISTORY
                        || state == GameState.JOURNAL && journalReturn == GameState.MAIN_MENU
                        || state == GameState.SETTING_MENU && settingsReturn == GameState.MAIN_MENU;
        boolean battle =
                state == GameState.PLAYING && !inventory && !workshop && !enemies.isEmpty();
        Enemy musicBoss = activeBoss();
        audio.update(
                dt,
                chapterIndex(),
                titleAudio,
                battle,
                battle && musicBoss != null,
                battle && musicBoss != null && musicBoss.enraged());
        if (state == GameState.RELIC_CHOICE) {
            if (IsKeyPressed(KEY_ESCAPE)) state = blessingReturn;
            else if (settings.controls.pressed(SAVE)) saveCurrent();
            else
                for (int i = 0; i < progress.expedition().offers().size(); i++)
                    if (IsKeyPressed(KEY_ONE + i)) {
                        chooseBlessing(i);
                        break;
                    }
            return;
        }
        if (state == GameState.PLAYING
                && settings.controls.pressed(INTERACT)
                && nearBase()
                && progress.baseHealth() < progress.maxBaseHealth()
                && (enemies.size() > 0
                        || progress.expedition().embers() == 0
                        || progress.expedition().offers().isEmpty())) {
            if (progress.repairBase()) {
                audio.play("upgrade");
                vfx.impact(0, 0, Menu.TEAL, true);
                notifyPlayer("SANCTUARY REPAIRED / +100 INTEGRITY");
                saveCurrent();
            } else if (progress.crystals() < 30) notifyPlayer("SANCTUARY REPAIR NEEDS 30 CRYSTALS");
            else notifyPlayer("SANCTUARY INTEGRITY IS ALREADY FULL");
            return;
        }
        if ((state == GameState.PLAYING || state == GameState.LEVEL_COMPLETE)
                && settings.controls.pressed(INTERACT)
                && nearBase()
                && enemies.isEmpty()
                && director.remaining() == 0
                && progress.expedition().embers() > 0
                && !progress.expedition().offers().isEmpty()) {
            blessingReturn = state;
            state = GameState.RELIC_CHOICE;
            return;
        }
        if (state == GameState.PLAYING && IsKeyPressed(KEY_B)) {
            boolean inCombat = !enemies.isEmpty() || attackCooldown > 0 || slashTime > 0;
            if (inCombat) {
                notifyPlayer("CANNOT RETURN DURING BATTLE! CLEAR NEARBY THREATS FIRST.");
                audio.play("error");
            } else {
                travelToCitadel();
                return;
            }
        }
        if (state == GameState.CHAPTER_SELECT) {
            if (IsKeyPressed(KEY_ESCAPE)) state = chapterMapReturn;
            return;
        }
        if (state == GameState.PLAYER_HISTORY) {
            if (IsKeyPressed(KEY_ESCAPE)) {
                if (pendingDelete != null) pendingDelete = null;
                else state = GameState.MAIN_MENU;
            }
            return;
        }
        if (state == GameState.PLAYTEST_SETUP) {
            if (IsKeyPressed(KEY_ESCAPE)) state = playtestReturn;
            else if (IsKeyPressed(KEY_ENTER)) startPlaytest();
            return;
        }
        if (state == GameState.JOURNAL) {
            if (IsKeyPressed(KEY_ESCAPE) || IsKeyPressed(KEY_ENTER) || IsKeyPressed(KEY_J))
                state = journalReturn;
            return;
        }
        if (state == GameState.CITADEL) {
            updateCitadel(dt);
            return;
        }
        if (state == GameState.LEVEL_COMPLETE || state == GameState.VICTORY) {
            if (settings.controls.pressed(SAVE)) saveCurrent();
            if ((settings.controls.pressed(INVENTORY)
                            || IsKeyPressed(KEY_E)
                            || IsKeyPressed(KEY_TAB))
                    && state == GameState.LEVEL_COMPLETE) {
                inventory = !inventory;
                return;
            }
            if (inventory && IsKeyPressed(KEY_ESCAPE)) {
                inventory = false;
                return;
            }
            if (IsKeyPressed(KEY_ENTER) && !inventory) {
                continueCampaign();
                return;
            }
            if (IsKeyPressed(KEY_ESCAPE) && !inventory) {
                saveCurrent();
                state = GameState.MAIN_MENU;
                return;
            }
            return;
        }
        if (state == GameState.LOGIN) {
            for (int ch = GetCharPressed(); ch != 0; ch = GetCharPressed())
                if (nameDraft.length() < 16
                        && (Character.isLetterOrDigit(ch) && ch < 128
                                || ch == ' '
                                || ch == '_'
                                || ch == '-')) nameDraft += (char) ch;
            if (IsKeyPressed(KEY_BACKSPACE) && !nameDraft.isEmpty())
                nameDraft = nameDraft.substring(0, nameDraft.length() - 1);
            if (IsKeyPressed(KEY_ESCAPE)) state = GameState.MAIN_MENU;
            else if (IsKeyPressed(KEY_ENTER)) confirmName();
            return;
        }
        if (state == GameState.SETTING_MENU) {
            if (rebinding != null) {
                int key = GetKeyPressed();
                if (key == KEY_ESCAPE) {
                    rebinding = null;
                    menuMessage = "";
                } else if (key != 0)
                    try {
                        settings.controls.bind(rebinding, key);
                        rebinding = null;
                        menuMessage = "Binding updated.";
                    } catch (IllegalArgumentException e) {
                        menuMessage = e.getMessage();
                    }
            } else if (IsKeyPressed(KEY_ESCAPE)) closeSettings();
            return;
        }
        if (settings.controls.pressed(MUTE)) {
            settings.muted = !settings.muted;
            applySettings();
            persistSettings();
        }
        if (IsKeyPressed(KEY_ESCAPE)) {
            if (inventory) inventory = false;
            else if (workshop) workshop = false;
            else if (state == GameState.PLAYING || state == GameState.CITADEL) {
                pauseReturn = state;
                state = GameState.PAUSED;
            } else if (state == GameState.PAUSED) {
                state = pauseReturn;
            }
            return;
        }
        if ((state == GameState.PLAYING || state == GameState.PAUSED)
                && settings.controls.pressed(SAVE)) {
            saveCurrent();
            return;
        }
        if (tutorial == null && settings.controls.pressed(LOAD)) {
            loadActive();
            return;
        }
        if (IsKeyPressed(KEY_ENTER)) {
            if (state == GameState.MAIN_MENU) {
                requestNewGame();
                return;
            }
            if (state == GameState.GAME_OVER) {
                retryChapter();
                return;
            }
            if (state == GameState.PAUSED) {
                state = pauseReturn;
                return;
            }
        }
        if (state != GameState.PLAYING) return;
        if (!player.isBoundsEnabled()) {
            player.resetBounds();
        }
        HudLayout belt = new HudLayout(GetScreenWidth(), GetScreenHeight());
        Vector2 pointer = GetMousePosition();
        int hovered =
                pointerAim && !inventory && !workshop ? belt.hit(pointer.x(), pointer.y()) : -1;
        int beltClick = hovered >= 0 && IsMouseButtonPressed(MOUSE_BUTTON_LEFT) ? hovered : -1;
        if (beltClick == 1) hudCharging = true;
        if (!IsMouseButtonDown(MOUSE_BUTTON_LEFT)) hudCharging = false;
        boolean overBelt =
                pointerAim && !inventory && !workshop && belt.contains(pointer.x(), pointer.y())
                        || hudCharging
                        || uiMouseCaptured;
        boolean heavyDown = settings.controls.down(HEAVY) || hudCharging;
        if (beltClick == 2) player.requestParry();
        if (!inventory && !workshop && tutorial == null && IsKeyPressed(KEY_J)) {
            journalReturn = state;
            journalTab = 1;
            state = GameState.JOURNAL;
            audio.play("menu");
            return;
        }
        if (inventory) {
            if (settings.controls.pressed(INVENTORY)
                    || IsKeyPressed(KEY_ESCAPE)
                    || IsKeyPressed(KEY_E)
                    || IsKeyPressed(KEY_TAB)) {
                inventory = false;
            }
            vfx.blade().clear();
            return;
        }
        if (settings.controls.pressed(INVENTORY)
                || IsKeyPressed(KEY_E)
                || IsKeyPressed(KEY_TAB)
                || beltClick == 5) {
            inventory = true;
            workshop = false;
            if (tutorial != null) tutorial.inventory();
            return;
        }
        if (settings.controls.pressed(POTION) || beltClick == 4) {
            float before = progress.health();
            if (progress.usePotion()) {
                float healed = progress.health() - before;
                feedback.add(
                        player.getPosition().x(),
                        player.getPosition().z(),
                        "+" + Math.round(healed) + " HP",
                        Menu.TEAL);
                notifyPlayer("Potion used / +" + Math.round(healed) + " HP");
            } else {
                notifyPlayer("Need missing health and a potion.");
            }
        }
        if (workshop) {
            vfx.blade().clear();
            return;
        }
        enemyFocus.update(enemies);
        if (settings.controls.pressed(FOCUS)) {
            float focusX = player.getPosition().x(), focusZ = player.getPosition().z();
            enemyFocus.cycle(
                    focusX,
                    focusZ,
                    enemies,
                    enemy ->
                            ArenaNavigation.projectileFraction(
                                            chapterIndex(), focusX, focusZ, enemy.x, enemy.z)
                                    >= 1);
            if (tutorial != null && enemyFocus.selected() != null) tutorial.focus();
        }
        if (tutorial == null) {
            RecoveryWisps wisps = progress.recoveryWisps();
            wisps.update(dt);
            if (wisps.collect(
                    player.getPosition().x(),
                    player.getPosition().z(),
                    progress.health() < progress.maxHealth()
                            || player.discipline().stamina() < player.discipline().maximum() - 1)) {
                progress.restoreHealth(RecoveryWisps.HEALTH);
                player.discipline().stamina(player.discipline().stamina() + RecoveryWisps.STAMINA);
                audio.play("ward");
                notifyPlayer("RECOVERY WISP / +18 HEALTH / +26 " + progress.resourceLabel());
                feedback.add(
                        player.getPosition().x(), player.getPosition().z(), "+18 HP", Menu.TEAL);
                vfx.impact(player.getPosition().x(), player.getPosition().z(), Menu.TEAL, false);
            }
        }
        noticeTime = Math.max(0, noticeTime - dt);
        landmarkBannerTime = Math.max(0, landmarkBannerTime - dt);
        storyTime = Math.max(0, storyTime - dt);
        attackCooldown = Math.max(0, attackCooldown - dt);
        slashTime = Math.max(0, slashTime - dt);
        hurtFlash = Math.max(0, hurtFlash - dt);
        comboWindow = Math.max(0, comboWindow - dt);
        inputBuffer = Math.max(0, inputBuffer - dt);
        feedback.update(dt);
        vfx.update(dt);
        dayNight.update(dt);
        boolean wasDodging = player.isDodging();
        float beforeMoveX = player.getPosition().x(), beforeMoveZ = player.getPosition().z();
        if (!overBelt
                && (IsMouseButtonDown(MOUSE_BUTTON_LEFT)
                        || heavyDown
                        || heavyCharge > 0
                        || settings.controls.down(HEAVY)
                        || settings.controls.pressed(HEAVY))) {
            aimCursor();
        }
        sceneManager.getOpenScene().updatesEntities(dt);
        ArenaNavigation.Position heroMove =
                ArenaNavigation.move(
                        chapterIndex(),
                        beforeMoveX,
                        beforeMoveZ,
                        player.getPosition().x(),
                        player.getPosition().z(),
                        .38f);
        player.getPosition().x(heroMove.x()).z(heroMove.z());
        if (progress.heroClass() == HeroClass.WARRIOR && player.isAttacking() && dt > 0) {
            float elapsed = activeAttack.duration() - player.attackRemaining();
            if (elapsed >= activeAttack.impact() - .07f
                    && elapsed <= activeAttack.impact() + (activeHeavy ? .20f : .14f))
                assets.sampleBlade(player, progress.weaponTier(), vfx.blade());
        }
        if (!wasDodging && player.isDodging()) {
            audio.play("dash");
            if (tutorial != null) tutorial.dodge();
        }
        if (tutorial != null) {
            tutorial.movement(
                    (float)
                            Math.hypot(
                                    player.getPosition().x() - beforeMoveX,
                                    player.getPosition().z() - beforeMoveZ));
            tutorial.healing(dt, nearBase());
        }
        float px = player.getPosition().x(), pz = player.getPosition().z();
        FieldEvent event = progress.fieldEvent();
        FieldEvent.Status previousEventStatus = event.status();
        event.update(dt, px, pz, settings.controls.down(INTERACT));
        if (event.claim()) {
            if (tutorial == null)
                progress.chapterFeats().record(chapterIndex(), ChapterFeats.Action.OBJECTIVE);
            int lvlBefore = progress.characterLevel();
            progress.rewardObjective();
            progress.addXp(120);
            audio.play("upgrade");
            vfx.impact(event.x(), event.z(), Menu.GOLD, true);
            if (event.kind() == FieldEvent.Kind.SURVIVOR) {
                progress.rescueSurvivor();
                feedback.add(
                        event.x(),
                        event.z() - 1.2f,
                        "SURVIVOR RESCUED -> EVACUATED TO CITADEL!",
                        Menu.GOLD);
                notifyPlayer(
                        "SURVIVOR RESCUED / REFUGEE HAS ARRIVED SAFELY AT AETHELGARD CITADEL!");
            }
            if (progress.characterLevel() > lvlBefore) {
                feedback.add(
                        player.getPosition().x(),
                        player.getPosition().z(),
                        "LEVEL UP! REACHED LEVEL " + progress.characterLevel(),
                        Menu.GOLD);
            } else if (event.kind() != FieldEvent.Kind.SURVIVOR) {
                feedback.add(event.x(), event.z(), "+120 XP", Menu.color(140, 80, 210));
            }
            if (event.kind() != FieldEvent.Kind.SURVIVOR) {
                notifyPlayer("OBJECTIVE COMPLETE / +120 XP / +65 CRYSTALS / +1 EMBER BLESSING");
            }
            saveCurrent();
        }
        float heroVx = Math.max(-15, Math.min(15, (px - lastHeroX) / Math.max(.001f, dt))),
                heroVz = Math.max(-15, Math.min(15, (pz - lastHeroZ) / Math.max(.001f, dt)));
        lastHeroX = px;
        lastHeroZ = pz;
        if (nearBase()) progress.heal(dt);
        guardianFlash = Math.max(0, guardianFlash - dt);
        guardianCooldown = Math.max(0, guardianCooldown - dt);
        solarWardCooldown = Math.max(0, solarWardCooldown - dt);
        boolean hasSpire = progress.baseLevel() >= 2 || progress.spireLevel() >= 2;
        if (tutorial == null && hasSpire && guardianCooldown <= 0) {
            Enemy target = SanctuaryWard.target(enemies, progress.spireLevel());
            if (target != null) {
                float damage =
                        SanctuaryWard.damage(
                                progress.baseLevel(), progress.spireLevel(), target.health);
                target.health -= damage;
                target.hitFlash = .16f;
                guardianX = target.x;
                guardianZ = target.z;
                guardianFlash = .28f;
                guardianCooldown = SanctuaryWard.interval(progress.spireLevel());
                vfx.impact(target.x, target.z, Menu.TEAL, false);
                feedback.add(target.x, target.z, "WARD -" + Math.round(damage), Menu.TEAL);
                audio.play("ward");
            }
        }
        if (progress.solarWardLevel() > 0
                && progress.baseHealth() < progress.maxBaseHealth() * 0.35f
                && solarWardCooldown <= 0) {
            solarWardCooldown = 45f;
            float novaRadius = 12f;
            float novaDmg = 25f + progress.solarWardLevel() * 15f;
            for (Enemy enemy : enemies) {
                if (enemy.health > 0 && enemy.distance(0, 0) <= novaRadius) {
                    enemy.hit(novaDmg, 0, 0, 0);
                    float dist = Math.max(0.1f, enemy.distance(0, 0));
                    enemy.x += (enemy.x / dist) * 5.0f;
                    enemy.z += (enemy.z / dist) * 5.0f;
                    enemy.stun = 2.0f;
                }
            }
            vfx.impact(0, 0, Menu.GOLD, true);
            feedback.add(0, 0, "SOLAR NOVA WARD ACTIVATED!", Menu.GOLD);
            audio.play("ward");
        }
        // Impact occurs part-way through the swing, once only, in the locked facing arc.
        if (impactPending) {
            attackElapsed += dt;
            if (attackElapsed >= activeAttack.impact()) {
                resolvePlayerHit(px, pz);
                impactPending = false;
            }
        }
        boolean finishingChapter = director.levelCleared(0) && !hasLivingEnemy();
        if (finishingChapter) {
            inputBuffer = 0;
            heavyCharge = 0;
            player.setCharging(false);
        }
        if (player.isDodging()) {
            inputBuffer = 0;
            comboWindow = 0;
            combo = 0;
            heavyCharge = 0;
            player.setCharging(false);
        }
        if (!finishingChapter
                && heavyDown
                && attackCooldown <= 0
                && !player.isDodging()
                && player.discipline().recovery() <= 0) {
            heavyCharge = Math.min(1.4f, heavyCharge + dt);
            player.setCharging(true);
        } else if (heavyCharge > 0 && !heavyDown) {
            releasedCharge = heavyCharge;
            heavyCharge = 0;
            player.setCharging(false);
            bufferedHeavy = true;
            bufferedLunge = false;
            inputBuffer = .22f;
        }
        boolean mousePressed = mouseAttack.getAsBoolean() && !overBelt;
        boolean mouseHeld =
                pointerAim
                        && !overBelt
                        && IsMouseButtonDown(MOUSE_BUTTON_LEFT)
                        && !IsMouseButtonDown(MOUSE_BUTTON_RIGHT)
                        && attackCooldown <= 0;
        boolean lightInput =
                settings.controls.pressed(SLASH) || mousePressed || mouseHeld || beltClick == 0;
        if (!overBelt
                && (mousePressed
                        || mouseHeld
                        || heavyCharge > 0
                        || heavyDown
                        || settings.controls.pressed(HEAVY)
                        || settings.controls.down(HEAVY))) {
            aimCursor();
        }
        if (lightInput && !finishingChapter && heavyCharge <= 0) {
            bufferedHeavy = bufferedLunge = false;
            bufferedTimed =
                    (!mouseHeld || mousePressed)
                            && combo > 0
                            && comboWindow > 0
                            && attackCooldown <= .22f;
            inputBuffer = .22f;
        }
        if (!finishingChapter
                && (settings.controls.pressed(LUNGE) || beltClick == 3)
                && heavyCharge <= 0) {
            bufferedLunge = true;
            bufferedHeavy = false;
            inputBuffer = .22f;
        }
        if (inputBuffer > 0 && !progress.hasWeapon()) {
            inputBuffer = 0;
            heavyCharge = 0;
            notifyPlayer("UNARMED! VISIT YOUR CLASS MENTOR IN CITADEL TO RECEIVE A WEAPON.");
            return;
        }
        if (inputBuffer > 0
                && attackCooldown <= 0
                && !player.isDodging()
                && heavyCharge <= 0
                && player.discipline().recovery() <= 0) {
            float cost =
                    bufferedLunge
                            ? CombatDiscipline.LUNGE
                            : bufferedHeavy ? CombatDiscipline.HEAVY : CombatDiscipline.LIGHT;
            inputBuffer = 0;
            if (!player.discipline().spend(cost))
                notifyPlayer("LOW " + progress.resourceLabel() + " / Recover before attacking");
            else {
                combo = bufferedLunge || bufferedHeavy ? 0 : bufferedTimed ? combo % 3 + 1 : 1;
                activeAttack =
                        bufferedLunge
                                ? Combat.lunge(progress.heroClass())
                                : progress.heroClass().attack(combo, bufferedHeavy);
                if (!bufferedLunge
                        && progress.heroClass() == HeroClass.ARCHER
                        && progress.weaponTier() >= 2)
                    activeAttack =
                            new Combat.Attack(
                                    bufferedHeavy ? 1.1f : .84f,
                                    bufferedHeavy ? .20f : .16f,
                                    activeAttack.range(),
                                    activeAttack.halfAngle(),
                                    activeAttack.multiplier(),
                                    activeAttack.label());
                if (bufferedHeavy && releasedCharge > .35f)
                    activeAttack =
                            new Combat.Attack(
                                    activeAttack.duration(),
                                    progress.heroClass() == HeroClass.WARRIOR ? .18f : .12f,
                                    activeAttack.range(),
                                    activeAttack.halfAngle(),
                                    activeAttack.multiplier(),
                                    activeAttack.label());
                if (bufferedHeavy)
                    activeAttack =
                            Combat.scale(
                                    activeAttack,
                                    1
                                            + .12f
                                                    * progress.expedition()
                                                            .rank(Expedition.Blessing.KINDLING),
                                    activeAttack.label());
                if (bufferedHeavy) {
                    boolean perfect = releasedCharge >= .75f && releasedCharge <= 1.05f;
                    float bonus =
                            perfect
                                    ? 1.6f
                                    : releasedCharge > 1.2f ? 1.05f : 1 + releasedCharge * .25f;
                    activeAttack =
                            Combat.scale(
                                    activeAttack,
                                    bonus,
                                    perfect ? "PERFECT CHARGE" : activeAttack.label());
                } else if (combo > 1)
                    activeAttack =
                            Combat.scale(activeAttack, 1.12f, "TIMED CHAIN " + combo + " / 3");
                float riposte = player.discipline().consumeRiposte();
                if (riposte > 1) activeAttack = Combat.scale(activeAttack, riposte, "RIPOSTE");
                float classBonus = mastery.attackBonus(progress.heroClass(), bufferedHeavy);
                if (classBonus > 1)
                    activeAttack =
                            Combat.scale(
                                    activeAttack, classBonus, "EMPOWERED " + activeAttack.label());
                attackCooldown = activeAttack.duration();
                comboWindow = attackCooldown + .23f;
                attackElapsed = 0;
                impactPending = true;
                activeHeavy = bufferedHeavy;
                activeCharged = bufferedHeavy && releasedCharge >= .75f;
                if (!overBelt) {
                    aimCursor();
                }
                vfx.beginMelee(activeHeavy);
                if (bufferedLunge) {
                    player.lunge(activeAttack.duration(), activeAttack.impact());
                    audio.play("dash");
                } else
                    player.attack(
                            combo,
                            bufferedHeavy,
                            activeAttack.duration(),
                            activeAttack.impact(),
                            bufferedHeavy ? releasedCharge : 0);
            }
        }
        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            if (enemy.health <= 0) {
                iterator.remove();
                int lvlBefore = progress.characterLevel();
                progress.rewardEnemy(enemy.boss());
                if (enemy.elite) progress.rewardElite();
                int xpGained = Progression.enemyXp(enemy.boss(), enemy.elite);
                feedback.add(enemy.x, enemy.z, "+" + xpGained + " XP", Menu.color(140, 80, 210));
                if (progress.characterLevel() > lvlBefore) {
                    feedback.add(
                            player.getPosition().x(),
                            player.getPosition().z(),
                            "LEVEL UP! REACHED LEVEL " + progress.characterLevel(),
                            Menu.GOLD);
                    audio.play("upgrade");
                    vfx.impact(player.getPosition().x(), player.getPosition().z(), Menu.GOLD, true);
                }
                audio.play(enemy.boss() ? "boss-fall" : "kill");
                enemy.attackTime = enemy.chargeTime = enemy.stun = enemy.hitFlash = 0;
                enemy.healingTarget = null;
                enemy.deathTime = 0;
                enemy.corpseSettled = false;
                if (tutorial == null && !enemy.boss())
                    progress.recoveryWisps()
                            .enemyDefeated(enemy.x, enemy.z, progress.kills(), enemy.elite);
                if (fallen.size() >= 48) fallen.remove(0);
                fallen.add(enemy);
                vfx.impact(enemy.x, enemy.z, enemy.boss() ? Menu.GOLD : Menu.TEAL, enemy.boss());
                feedback.add(enemy.x, enemy.z, "+" + enemy.reward() + " crystals", Menu.TEAL);
                if (enemy.boss())
                    notifyPlayer(chapter().title() + " DEFEATED / +120 CRYSTALS / +300 XP");
                continue;
            }
            if (tutorial != null) {
                enemy.health = 9999;
                if (tutorial.step() != Tutorial.Step.PARRY) {
                    enemy.stun = .2f;
                    enemy.attackTime = 0;
                    enemy.impactPending = false;
                }
            }
            boolean huntsPlayer = tutorial != null || enemy.chooseHero(px, pz, dt);
            boolean huntsTraveler =
                    !enemy.boss()
                            && event.vulnerable()
                            && enemy.distance(event.x(), event.z()) < 9
                            && (!huntsPlayer
                                    || enemy.distance(event.x(), event.z())
                                            < enemy.distance(px, pz) * .75f);
            if (huntsTraveler) huntsPlayer = false;
            float tx = huntsTraveler ? event.x() : huntsPlayer ? px : 0,
                    tz = huntsTraveler ? event.z() : huntsPlayer ? pz : 0,
                    separationX = 0,
                    separationZ = 0;
            for (Enemy other : enemies)
                if (other != enemy && other.health > 0) {
                    float dx = enemy.x - other.x,
                            dz = enemy.z - other.z,
                            d = (float) Math.hypot(dx, dz);
                    if (d > .01f && d < 2.5f) {
                        float force = (2.5f - d) / 2.5f;
                        separationX += dx / d * force;
                        separationZ += dz / d * force;
                    }
                }
            float ex = enemy.x, ez = enemy.z;
            boolean charging = enemy.chargeTime > 0;
            ArenaNavigation.Position avoidance =
                    ArenaNavigation.steering(chapterIndex(), enemy.x, enemy.z, tx, tz);
            enemy.navigate(tx, tz, separationX + avoidance.x(), separationZ + avoidance.z(), dt);
            {
                ArenaNavigation.Position moved =
                        ArenaNavigation.move(
                                chapterIndex(), ex, ez, enemy.x, enemy.z, enemy.boss() ? .8f : .4f);
                enemy.x = moved.x();
                enemy.z = moved.z();
            }
            if (enemy.variant == 4
                    && enemy.healCooldown <= 0
                    && enemy.stun <= 0
                    && enemy.attackTime <= 0) {
                Enemy ally = SquadTactics.woundedAlly(enemy, enemies);
                if (ally != null) {
                    enemy.healingTarget = ally;
                    enemy.attackTime = .9f;
                    enemy.impactPending = true;
                    enemy.healCooldown = 8;
                    enemy.cooldown = 1.5f;
                    audio.play("ward");
                }
            }
            if ((charging || enemy.chargeTime > 0)
                    && !enemy.chargeHit
                    && Combat.segmentDistance(px, pz, ex, ez, enemy.x, enemy.z) < 1.45f) {
                enemy.chargeHit = true;
                strikeHero(enemy.meleeDamage(wave) * 1.15f, ex, ez, true, enemy);
            }
            if (enemy.impactPending && enemy.attackTime <= .25f) {
                enemy.impactPending = false;
                if (enemy.healingTarget != null) {
                    Enemy ally = enemy.healingTarget;
                    enemy.healingTarget = null;
                    if (ally.health > 0 && ally.distance(enemy.x, enemy.z) < 7) {
                        float healed =
                                Math.min(ally.maxHealth - ally.health, ally.maxHealth * .18f);
                        ally.health = Math.min(ally.maxHealth, ally.health + ally.maxHealth * .18f);
                        feedback.add(ally.x, ally.z, "+" + Math.round(healed) + " WARD", Menu.TEAL);
                    }
                } else if (enemy.boss()) {
                    switch (enemy.bossMove) {
                        case CHARGE -> enemy.launchCharge();
                        case SLAM -> {
                            audio.play("slam");
                            vfx.impact(enemy.attackX, enemy.attackZ, Menu.GOLD, true);
                            if (Math.hypot(px - enemy.attackX, pz - enemy.attackZ) <= 3.4f)
                                strikeHero(
                                        enemy.meleeDamage(wave) * 1.3f,
                                        enemy.x,
                                        enemy.z,
                                        false,
                                        enemy);
                            bossHazards.add(
                                    new BossHazard(
                                            BossHazard.Kind.SHOCKWAVE,
                                            enemy.x,
                                            enemy.z,
                                            .25f,
                                            enemy.meleeDamage(wave) * .7f));
                        }
                        case SIGILS -> {}
                        case CLEAVE -> {
                            if (Combat.inArc(
                                    enemy.x,
                                    enemy.z,
                                    enemy.facing,
                                    px,
                                    pz,
                                    enemy.meleeRange(),
                                    100))
                                strikeHero(enemy.meleeDamage(wave), enemy.x, enemy.z, true, enemy);
                        }
                    }
                } else if (enemy.ranged()) {
                    projectiles.add(new Projectile(enemy, wave));
                    if (enemy.distance(px, pz) < 15)
                        audio.play(enemy.variant == 4 ? "magic" : "bow");
                } else if (enemy.targetsPlayer
                        && Combat.inArc(
                                enemy.x, enemy.z, enemy.facing, px, pz, enemy.meleeRange(), 65)) {
                    strikeHero(enemy.meleeDamage(wave), enemy.x, enemy.z, true, enemy);
                } else if (enemy.targetsTraveler
                        && event.vulnerable()
                        && enemy.distance(event.x(), event.z()) < enemy.meleeRange() + .1f) {
                    event.damage(enemy.siegeDamage(wave));
                    vfx.impact(event.x(), event.z(), Menu.RED, false);
                    audio.play("hurt");
                } else if (!enemy.targetsPlayer
                        && !enemy.targetsTraveler
                        && enemy.distance(0, 0) < enemy.meleeRange() + .1f) {
                    float before = progress.baseHealth();
                    progress.damageBase(enemy.siegeDamage(wave));
                    feedback.add(
                            0,
                            0,
                            "OUTPOST -" + Math.round(before - progress.baseHealth()),
                            Menu.RED);
                    audio.play("hurt");
                }
            }
            if (enemy.stun <= 0
                    && enemy.attackTime <= 0
                    && enemy.cooldown <= 0
                    && enemy.distance(tx, tz) < enemy.attackRange()
                    && enemy.facingTarget(tx, tz)
                    && (!huntsPlayer || SquadTactics.canJoinAssault(enemy, enemies))) {
                if (enemy.ranged()
                        && (!SquadTactics.clearShot(enemy, tx, tz, enemies)
                                || ArenaNavigation.projectileFraction(
                                                chapterIndex(), enemy.x, enemy.z, tx, tz)
                                        < 1)) {
                    enemy.sideStepTime = .65f;
                    continue;
                }
                float lead =
                        enemy.ranged() && huntsPlayer
                                ? Math.min(.6f, enemy.distance(px, pz) / 10)
                                        * (.45f + (1 - enemy.difficulty.cadence))
                                : 0;
                enemy.beginAttack(
                        Math.max(-22, Math.min(22, tx + heroVx * lead)),
                        Math.max(-22, Math.min(22, tz + heroVz * lead)),
                        huntsPlayer);
                enemy.targetsTraveler = huntsTraveler;
                if (!enemy.ranged() && enemy.distance(px, pz) < 10) audio.play("swing");
                if (enemy.boss() && enemy.bossMove == BossMove.SIGILS) {
                    int count = enemy.enraged() ? 5 : 3;
                    for (int i = 0; i < count; i++) {
                        double a = (i - 1) * Math.PI * 2 / (count - 1);
                        float hx = px + (i == 0 ? 0 : (float) Math.cos(a) * 3),
                                hz = pz + (i == 0 ? 0 : (float) Math.sin(a) * 3);
                        bossHazards.add(
                                new BossHazard(
                                        BossHazard.Kind.SIGIL,
                                        hx,
                                        hz,
                                        enemy.attackDuration() - .25f + i * .12f,
                                        enemy.meleeDamage(wave) * .75f));
                    }
                }
            }
        }
        for (Iterator<HeroShot> shots = heroShots.iterator(); shots.hasNext(); ) {
            HeroShot shot = shots.next();
            shot.update(dt);
            float obstruction =
                    ArenaNavigation.projectileFraction(
                            chapterIndex(), shot.previousX, shot.previousZ, shot.x, shot.z);
            if (obstruction < 1) {
                shot.x = shot.previousX + (shot.x - shot.previousX) * obstruction;
                shot.z = shot.previousZ + (shot.z - shot.previousZ) * obstruction;
                vfx.impact(shot.x, shot.z, Menu.MUTED, false);
            }
            boolean stopped = false;
            if (shot.restorative) {
                FieldEvent ally = progress.fieldEvent();
                if (ally.kind() == FieldEvent.Kind.SURVIVOR
                        && ally.vulnerable()
                        && shot.intersects(ally.x(), ally.z(), .9f)
                        && ally.support(shot.damage * .65f)) {
                    stopped = true;
                    vfx.impact(ally.x(), ally.z(), Menu.TEAL, true);
                    feedback.add(
                            ally.x(),
                            ally.z(),
                            "ALLY +" + Math.round(shot.damage * .65f),
                            Menu.TEAL);
                    audio.play("ward");
                } else if (shot.intersects(0, 0, 2.0f) && progress.restoreBase(shot.damage * .8f)) {
                    stopped = true;
                    vfx.impact(0, 0, Menu.TEAL, true);
                    feedback.add(0, 0, "SANCTUARY +" + Math.round(shot.damage * .8f), Menu.TEAL);
                    audio.play("ward");
                }
            }
            // Nearest intersections first, regardless of enemy spawn order.
            shotTargets.clear();
            if (!stopped) shotTargets.addAll(enemies);
            shotTargets.sort(
                    java.util.Comparator.comparingDouble(e ->
                            (e.x - shot.previousX) * (e.x - shot.previousX)
                                    + (e.z - shot.previousZ) * (e.z - shot.previousZ)));
            for (Enemy enemy : shotTargets)
                if (shot.hit(enemy)) {
                    float damage = Math.min(enemy.health, shot.damage);
                    boolean isCrit = shot.heavy || shot.charged;
                    boolean wasCasting = enemy.healingTarget != null;
                    enemy.hit(damage, shot.previousX, shot.previousZ, .25f);
                    vfx.impact(enemy.x, enemy.z, shot.magic ? Menu.TEAL : Menu.GOLD, isCrit);
                    String fct =
                            isCrit ? "-" + Math.round(damage) + "! CRIT" : "-" + Math.round(damage);
                    feedback.add(enemy.x, enemy.z, fct, Menu.GOLD, isCrit);
                    if (wasCasting) {
                        feedback.add(enemy.x, enemy.z, "INTERRUPTED!", Menu.TEAL, true);
                        enemy.healCooldown = Math.max(enemy.healCooldown, 4.0f);
                        vfx.parrySpark(enemy.x, enemy.z);
                        audio.play("slam");
                    }
                    audio.play("hit");
                    connected(enemy, shot.heavy, shot.charged);
                    if (!shot.magic) {
                        stopped = true;
                        break;
                    }
                }
            if (stopped || obstruction < 1 || shot.remaining <= 0) shots.remove();
        }
        separateEnemies(dt);
        for (Iterator<Projectile> shots = projectiles.iterator(); shots.hasNext(); ) {
            Projectile shot = shots.next();
            shot.update(dt);
            float obstruction =
                    ArenaNavigation.projectileFraction(
                            chapterIndex(), shot.previousX, shot.previousZ, shot.x, shot.z);
            if (obstruction < 1) {
                shot.x = shot.previousX + (shot.x - shot.previousX) * obstruction;
                shot.z = shot.previousZ + (shot.z - shot.previousZ) * obstruction;
                vfx.impact(shot.x, shot.z, Menu.MUTED, false);
            }
            boolean hit =
                    shot.travelerTarget
                            ? event.vulnerable() && shot.hits(event.x(), event.z(), .8f)
                            : shot.playerTarget ? shot.hits(px, pz, .65f) : shot.hits(0, 0, 1.8f);
            if (hit) {
                if (shot.travelerTarget) {
                    event.damage(shot.damage);
                    vfx.impact(event.x(), event.z(), Menu.RED, false);
                    audio.play("hurt");
                } else if (shot.playerTarget)
                    strikeHero(
                            shot.damage,
                            shot.x - shot.vx * .2f,
                            shot.z - shot.vz * .2f,
                            true,
                            null);
                else {
                    float before = progress.baseHealth();
                    progress.damageBase(shot.damage);
                    feedback.add(
                            0,
                            0,
                            "OUTPOST -" + Math.round(before - progress.baseHealth()),
                            Menu.RED);
                    audio.play("hurt");
                }
            }
            if (hit || obstruction < 1 || shot.life <= 0) shots.remove();
        }
        for (Iterator<BossHazard> hazards = bossHazards.iterator(); hazards.hasNext(); ) {
            BossHazard hazard = hazards.next();
            hazard.update(dt);
            if (hazard.hits(px, pz)) strikeHero(hazard.damage, hazard.x, hazard.z, false, null);
            if (hazard.expired()) hazards.remove();
        }
        if (event.status() == FieldEvent.Status.FAILED
                && previousEventStatus != FieldEvent.Status.FAILED)
            notifyPlayer("OPTIONAL OBJECTIVE LOST / The sanctuary still needs you.");
        if (progress.defeated()) {
            state = GameState.GAME_OVER;
            outcomeTime = 0;
            player.resetCombat();
            impactPending = false;
            heavyCharge = 0;
            player.poseForOutcome(false, 0);
            return;
        }
        if (wave > 0
                && director.total() > 0
                && director.remaining() == 0
                && enemies.isEmpty()
                && progress.expedition().waveCleared(wave))
            notifyPlayer(
                    "EMBER BLESSING EARNED / Return to the sanctuary and press "
                            + settings.controls.label(INTERACT));
        if (tutorial != null) return;
        WaveDirector.Spawn spawn = director.update(dt, enemies.size());
        if (director.wave() != wave) {
            wave = director.wave();
            progress.fieldEvent().begin(wave);
            audio.play("wave");
            notifyPlayer(
                    chapter().bossOnly()
                            ? chapter().title() + " / BOSS DUEL"
                            : "WAVE "
                                    + (wave - chapter().first() + 1)
                                    + " / "
                                    + WaveFormation.label(wave));
        }
        if (spawn != null)
            enemies.add(
                    new Enemy(
                            spawn.x(),
                            spawn.z(),
                            spawn.wave(),
                            spawn.variant(),
                            random.nextFloat(),
                            progress.difficulty()));
        nextWave = director.rest();
        if (director.levelCleared(enemies.size()) && !player.isAttacking()) {
            heroShots.clear();
            projectiles.clear();
            bossHazards.clear();
            heavyCharge = 0;
            player.resetCombat();
            impactPending = false;
            attackCooldown = 0;
            int waveXp = 100 + wave * 25;
            int lvlBefore = progress.characterLevel();
            progress.addXp(waveXp);
            progress.rewardChapterFeats(chapterIndex());
            progress.unlockChapter(chapterIndex() + 1);
            state = wave >= 10 ? GameState.VICTORY : GameState.LEVEL_COMPLETE;
            outcomeTime = 0;
            audio.play("victory");
            player.poseForOutcome(true, 0);
            vfx.impact(player.getPosition().x(), player.getPosition().z(), Menu.GOLD, true);
            if (progress.characterLevel() > lvlBefore) {
                feedback.add(
                        player.getPosition().x(),
                        player.getPosition().z(),
                        "LEVEL UP! REACHED LEVEL " + progress.characterLevel(),
                        Menu.GOLD);
            }
            saveCurrent();
        }
    }

    private void strikeHero(
            float damage, float sourceX, float sourceZ, boolean parryable, Enemy attacker) {
        float px = player.getPosition().x(), pz = player.getPosition().z();
        if (player.invulnerable()) {
            if (tutorial == null)
                progress.chapterFeats().record(chapterIndex(), ChapterFeats.Action.EVADE);
            mastery.evade(progress.heroClass());
            feedback.add(
                    px,
                    pz,
                    progress.heroClass() == HeroClass.ARCHER ? "EVADE / PRECISION" : "EVADE",
                    Menu.TEAL);
            return;
        }
        if (parryable
                && Combat.inArc(px, pz, player.getRotation().y(), sourceX, sourceZ, 1000, 75)
                && player.discipline().deflect()) {
            boolean isBoss = attacker != null && attacker.boss();
            if (attacker != null) {
                attacker.stun = isBoss ? .95f : 1.1f;
                attacker.attackTime = attacker.chargeTime = 0;
                attacker.impactPending = false;
                attacker.cooldown = 1.3f;
            }
            mastery.parry(progress.heroClass());
            if (tutorial != null) tutorial.parry();
            else progress.chapterFeats().record(chapterIndex(), ChapterFeats.Action.PARRY);
            player.discipline()
                    .stamina(
                            player.discipline().stamina()
                                    + 10
                                            * progress.expedition()
                                                    .rank(Expedition.Blessing.SECOND_WIND));
            vfx.parrySpark(px, pz);
            vfx.impact(px, pz, Menu.TEAL, true);
            feedback.add(
                    px, pz, isBoss ? "BOSS PUSH BLOCKED / RIPOSTE" : "PARRY / RIPOSTE", Menu.GOLD);
            sceneManager.getOpenScene().getCameraController().impact(isBoss ? .18f : .11f);
            hitPause = Math.max(hitPause, isBoss ? .08f : .05f);
            audio.play("parry");
            return;
        }
        if (tutorial != null) {
            feedback.add(px, pz, "SAFE PRACTICE / TRY AGAIN", Menu.TEAL);
            return;
        }
        float before = progress.health();
        progress.damagePlayer(damage);
        progress.fieldEvent().interrupt();
        vfx.impact(px, pz, Menu.RED, false);
        feedback.add(px, pz, "-" + Math.round(before - progress.health()), Menu.RED);
        hurtFlash = .25f;
        sceneManager
                .getOpenScene()
                .getCameraController()
                .impact(Math.min(.14f, .035f + damage * .002f));
        audio.play("hurt");
    }

    /** Return to the originating pause/results screen when the player closes the map. */
    void openChapterMap() {
        if (state != GameState.PAUSED
                && state != GameState.LEVEL_COMPLETE
                && state != GameState.VICTORY) return;
        chapterMapReturn = state;
        state = GameState.CHAPTER_SELECT;
    }

    /** Restart an unlocked chapter with current gear; discard encounter-local state only. */
    void travelToChapter(int chapter) {
        int unlocked = Math.max(progress.unlockedChapter(), chapterIndex());
        if (chapter < 0 || chapter > unlocked) return;
        progress.unlockChapter(unlocked);
        int firstWave = Campaign.CHAPTERS[chapter].first();
        director.restore(new WaveDirector.State(firstWave - 1, 0, 0, 5, 0, 21, 0));
        wave = firstWave - 1;
        nextWave = 5;
        // Encounter objects cannot follow the player to a different chapter.
        enemies.clear();
        fallen.clear();
        heroShots.clear();
        projectiles.clear();
        bossHazards.clear();
        enemyFocus.clear();
        progress.recoveryWisps().clear();
        progress.fieldEvent().resetForTravel();
        player.resetCombat();
        player.getPosition().x(0).y(0).z(4);
        storyTime = 12;
        outcomeTime = 0;
        inventory = workshop = false;
        state = GameState.PLAYING;
        writeCheckpoint();
        saveCurrent();
    }

    private void continueCampaign() {
        if (state == GameState.VICTORY) {
            state = GameState.MAIN_MENU;
            return;
        }
        director.advanceLevel();
        progress.recoveryWisps().clear();
        storyTime = 12;
        inventory = workshop = false;
        fallen.clear();
        outcomeTime = 0;
        float sanctuaryBefore = progress.baseHealth();
        progress.chapterRespite();
        player.getPosition().x(0).y(0).z(4);
        player.resetCombat();
        state = GameState.PLAYING;
        writeCheckpoint();
        notifyPlayer(
                "CHAPTER SUPPLIES / SANCTUARY +"
                        + Math.round(progress.baseHealth() - sanctuaryBefore)
                        + " INTEGRITY");
        saveCurrent();
    }

    private void resolvePlayerHit(float px, float pz) {
        slashTime = .16f;
        audio.play(
                progress.heroClass() == HeroClass.WARRIOR
                        ? "swing"
                        : progress.heroClass() == HeroClass.ARCHER ? "bow" : "magic");
        if (progress.heroClass() != HeroClass.WARRIOR) {
            Vector3 origin = player.projectileOrigin();
            float facing = player.getRotation().y();
            boolean restorative =
                    progress.heroClass() == HeroClass.MAGE
                            && progress.weaponTier() >= 3
                            && activeHeavy;
            FieldEvent ally = progress.fieldEvent();
            boolean aimingSupport =
                    restorative
                            && (progress.baseHealth() < progress.maxBaseHealth()
                                            && Combat.inArc(
                                                    px, pz, facing, 0, 0, activeAttack.range(), 12)
                                    || ally.kind() == FieldEvent.Kind.SURVIVOR
                                            && ally.vulnerable()
                                            && ally.health() < 100
                                            && Combat.inArc(
                                                    px,
                                                    pz,
                                                    facing,
                                                    ally.x(),
                                                    ally.z(),
                                                    activeAttack.range(),
                                                    12));
            Enemy assisted =
                    aimingSupport
                            ? null
                            : RangedAim.target(
                                    px,
                                    pz,
                                    facing,
                                    activeAttack.range(),
                                    enemies,
                                    enemy ->
                                            ArenaNavigation.projectileFraction(
                                                            chapterIndex(),
                                                            origin.x(),
                                                            origin.z(),
                                                            enemy.x,
                                                            enemy.z)
                                                    >= 1);
            if (assisted != null)
                facing =
                        (float)
                                Math.toDegrees(
                                        Math.atan2(
                                                assisted.x - origin.x(), assisted.z - origin.z()));
            HeroShot shot =
                    new HeroShot(
                            origin.x(),
                            origin.z(),
                            facing,
                            activeAttack,
                            Math.round(
                                    progress.damage()
                                            * (1
                                                    + .10f
                                                            * progress.expedition()
                                                                    .rank(
                                                                            Expedition.Blessing
                                                                                    .HUNTERS_MARK))),
                            progress.heroClass() == HeroClass.MAGE);
            shot.startHeight(origin.y());
            shot.heavy = activeHeavy;
            shot.charged = activeCharged;
            shot.restorative = restorative;
            heroShots.add(shot);
            return;
        }
        boolean hit = false;
        for (Enemy enemy : enemies)
            if (enemy.health > 0
                    && Combat.inArc(
                            px,
                            pz,
                            player.getRotation().y(),
                            enemy.x,
                            enemy.z,
                            activeAttack.range(),
                            activeAttack.halfAngle())) {
                float damage =
                        Math.min(
                                enemy.health,
                                Math.round(progress.damage() * activeAttack.multiplier()));
                boolean isCrit =
                        activeHeavy
                                || activeCharged
                                || activeAttack.multiplier() > 1.2f
                                || mastery.stacks() > 0;
                boolean wasCasting = enemy.healingTarget != null;
                enemy.hit(damage, px, pz, activeAttack.multiplier() > 1 ? .8f : .35f);
                vfx.impact(enemy.x, enemy.z, Menu.GOLD, isCrit);
                String fct =
                        isCrit ? "-" + Math.round(damage) + "! CRIT" : "-" + Math.round(damage);
                feedback.add(enemy.x, enemy.z, fct, Menu.GOLD, isCrit);
                if (wasCasting) {
                    feedback.add(enemy.x, enemy.z, "INTERRUPTED!", Menu.TEAL, true);
                    enemy.healCooldown = Math.max(enemy.healCooldown, 4.0f);
                    vfx.parrySpark(enemy.x, enemy.z);
                    audio.play("slam");
                }
                connected(enemy, activeHeavy, activeCharged);
                hit = true;
            }
        if (hit) audio.play("hit");
    }

    private void separateEnemies(float dt) {
        for (int i = 0; i < enemies.size(); i++)
            for (int j = i + 1; j < enemies.size(); j++) {
                Enemy a = enemies.get(i), b = enemies.get(j);
                float dx = a.x - b.x, dz = a.z - b.z, d = (float) Math.hypot(dx, dz);
                if (d >= 1.1f) continue;
                if (d < .001f) {
                    dx = 1;
                    dz = 0;
                    d = 1;
                }
                float push = Math.min(.055f, dt * 1.5f);
                if (a.attackTime <= 0) {
                    a.x += dx / d * push;
                    a.z += dz / d * push;
                }
                if (b.attackTime <= 0) {
                    b.x -= dx / d * push;
                    b.z -= dz / d * push;
                }
            }
    }

    public void render() {
        drawWorld();
        // UI buttons can open inventory later in this render. Show it next frame,
        // after its offscreen previews have been prepared outside the 2D overlay.
        boolean inventoryPrepared = inventory;
        if (inventoryPrepared) equipmentScreen.prepare(assets, player, progress);
        if (state == GameState.LOGIN) heroPortrait.prepare(assets, selectedClass);
        // Jaylib 6.0.1-0's SDL backend restores a framebuffer-pixel projection
        // after 3D. Scale the overlay to window pixels, matching GetMousePosition.
        // Recompute each frame for resizing and moving between Retina/1x displays.
        uiCamera.zoom((float) GetRenderWidth() / Math.max(1, GetScreenWidth()));
        BeginMode2D(uiCamera);
        try {
            if (photoMode) return;
            if (storyOpening) {
                OpeningStory.draw(storyPage);
                return;
            }
            if (rescueLedgerOpen) {
                if (RescueLedgerScreen.draw(progress)) rescueLedgerOpen = false;
                return;
            }
            if (state == GameState.CITADEL) {
                if (citadelHub.activeNpc() == null && !workshop && !inventory)
                    cityPresentation.draw(
                            citadelHub,
                            progress,
                            player.getPosition().x(),
                            player.getPosition().z(),
                            sceneManager.getOpenScene().getCameraController().getCamera());
                CitadelScreen.Action act =
                        citadelScreen.draw(
                                citadelHub,
                                progress,
                                settings.controls,
                                player.getPosition().x(),
                                player.getPosition().z(),
                                chapterIndex());
                switch (act) {
                    case CLAIM_STARTER_WEAPON -> {
                        audio.play("upgrade");
                        vfx.impact(
                                player.getPosition().x(),
                                player.getPosition().z(),
                                Menu.GOLD,
                                true);
                        notifyPlayer(
                                "STARTER WEAPON CLAIMED: "
                                        + progress.weaponName(0).toUpperCase()
                                        + "!");
                        applyProgression();
                        saveCurrent();
                    }
                    case OPEN_FORGE -> {
                        workshop = true;
                        workshopScreen.selectTab(0);
                        workshopScreen.setMentor(
                                citadelHub.activeNpc() != null
                                        ? citadelHub.activeNpc().mentorClass()
                                        : null);
                    }
                    case OPEN_SUPPLIES -> {
                        workshop = true;
                        workshopScreen.selectTab(2);
                        workshopScreen.setMentor(
                                citadelHub.activeNpc() != null
                                        ? citadelHub.activeNpc().mentorClass()
                                        : null);
                    }
                    case OPEN_TRAINING -> {}
                    case DEPLOY_MISSION -> {
                        citadelHub.startTeleport();
                        audio.play("upgrade");
                        vfx.impact(CitadelHub.PORTAL_X, CitadelHub.PORTAL_Z, Menu.GOLD, true);
                    }
                    case OPEN_RESCUE_LEDGER -> {
                        rescueLedgerOpen = true;
                        audio.play("menu");
                    }
                    case OPEN_JOURNAL -> {
                        journalReturn = GameState.CITADEL;
                        journalTab = 1;
                        state = GameState.JOURNAL;
                        audio.play("menu");
                    }
                    case CLOSE_DIALOGUE -> citadelHub.closeNpc();
                    case NONE -> {}
                }
                if (workshop) drawWorkshop();
                if (inventory && inventoryPrepared) drawInventory();
                if (state == GameState.PAUSED) drawOverlay();
                if (tutorial != null && !workshop && !inventory && citadelHub.activeNpc() == null) {
                    drawCitadelTutorial();
                }
                return;
            }
            drawEnemyHealth();
            if (state == GameState.MAIN_MENU) {
                drawMainMenu();
                return;
            }
            if (state == GameState.LOGIN) {
                drawNameEntry();
                return;
            }
            if (state == GameState.PLAYER_HISTORY) {
                drawPlayerHistory();
                return;
            }
            if (state == GameState.PLAYTEST_SETUP) {
                drawPlaytestSetup();
                return;
            }
            if (state == GameState.SETTING_MENU) {
                drawSettings();
                return;
            }
            if (state == GameState.RELIC_CHOICE) {
                drawBlessings();
                return;
            }
            feedback.draw(sceneManager.getOpenScene().getCameraController().getCamera());
            if (hurtFlash > 0) {
                Color tint = Fade(Menu.RED, hurtFlash * 1.5f);
                DrawRectangle(0, 0, GetScreenWidth(), 9, tint);
                DrawRectangle(0, GetScreenHeight() - 9, GetScreenWidth(), 9, tint);
                DrawRectangle(0, 0, 9, GetScreenHeight(), tint);
                DrawRectangle(GetScreenWidth() - 9, 0, 9, GetScreenHeight(), tint);
            }
            drawHud();
            if (tutorial != null && !inventory && !workshop) drawTutorial();
            if (tutorial == null
                    && storyTime > 0
                    && state == GameState.PLAYING
                    && !inventory
                    && !workshop) {
                int pw = 400;
                int textW = pw - 36;
                int textH = Menu.wrappedHeight(chapter().story(), textW, 14);
                int ph = textH + 68;
                int px = GetScreenWidth() - pw - 20;
                int py = 210;
                Menu.panel(px, py, pw, ph);
                Menu.text(
                        "CHAPTER " + (chapterIndex() + 1) + " / " + chapter().title(),
                        px + 18,
                        py + 16,
                        15,
                        Menu.GOLD);
                Menu.wrapped(chapter().story(), px + 18, py + 42, textW, 14, Menu.TEXT);
            }
            if ((state == GameState.LEVEL_COMPLETE || state == GameState.VICTORY) && !inventory)
                drawChapterEnd();
            if (state == GameState.JOURNAL) drawJournal();
            if (state == GameState.CHAPTER_SELECT) {
                dim();
                int selected =
                        chapterSelectScreen.draw(
                                Math.max(progress.unlockedChapter(), chapterIndex()),
                                chapterIndex());
                if (selected == ChapterSelectScreen.BACK) state = chapterMapReturn;
                else if (selected >= 0) travelToChapter(selected);
            }
            if (workshop) drawWorkshop();
            if (inventory && inventoryPrepared) drawInventory();
            if (state == GameState.PAUSED || state == GameState.GAME_OVER) drawOverlay();
        } finally {
            EndMode2D();
        }
    }

    private Vector3 v(float x, float y, float z) {
        return new Vector3().x(x).y(y).z(z);
    }

    private Enemy activeBoss() {
        for (Enemy enemy : enemies) if (enemy.boss() && enemy.health > 0) return enemy;
        return null;
    }

    private boolean hasLivingEnemy() {
        for (Enemy enemy : enemies) if (enemy.health > 0) return true;
        return false;
    }

    private boolean enemyInsideParryWindow() {
        for (Enemy enemy : enemies)
            if (enemy.attackTime > .25f && enemy.attackTime < .43f) return true;
        return false;
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

    private void drawWorld() {
        float time = (float) GetTime();
        Camera3D camera = sceneManager.getOpenScene().getCameraController().getCamera();
        assets.view(player, camera);
        DayNightCycle.Frame light = dayNight.frame();
        renderer.beginShadow(time, light);
        assets.pass(renderer.shadowShader, true);
        if (state == GameState.CITADEL) {
            citadelHub.prepareLighting(assets);
            citadelHub.drawShadowArchitecture();
            drawActors(true);
        } else {
            assets.drawEnvironment(chapterIndex(), time);
            drawActors(true);
        }
        renderer.endShadow();
        renderer.beginScene();
        ChapterLook palette = ChapterLook.at(state == GameState.CITADEL ? 0 : chapterIndex());
        float atmosphere = .12f + .34f * light.daylight();
        DrawRectangleGradientV(
                0,
                0,
                renderer.width(),
                renderer.height(),
                blendSky(light.top(), palette.skyTop(), atmosphere, skyTopBlend),
                blendSky(light.horizon(), palette.skyBottom(), atmosphere, skyHorizonBlend));
        BeginMode3D(camera);
        Enemy boss = activeBoss();
        assets.drawSky(camera, light);
        assets.litPass(renderer, camera, time, boss, light);
        if (state == GameState.CITADEL) {
            citadelHub.prepareLighting(assets);
            citadelHub.drawWorld(
                    time,
                    player.getPosition().x(),
                    player.getPosition().z(),
                    progress.rescuedSurvivors());
            drawActors(false);
        } else {
            assets.drawEnvironment(chapterIndex(), time);
            drawActors(false);
            assets.drawFadedScenery(chapterIndex(), time);
            drawSanctuaryRing();
            if (director.warning()) {
                DrawCylinderWires(
                        v(director.x(), .08f, director.z()), 1.4f, 1.4f, .15f, 24, Menu.RED);
                DrawLine3D(
                        v(director.x(), 0, director.z()),
                        v(director.x(), 3, director.z()),
                        Menu.GOLD);
            }
            vfx.begin();
            for (HeroShot shot : heroShots)
                vfx.projectile(
                        camera,
                        shot.x,
                        shot.z,
                        shot.vx,
                        shot.vz,
                        shot.magic,
                        shot.restorative
                                ? Menu.color(132, 255, 205)
                                : shot.magic ? Menu.TEAL : Menu.GOLD,
                        time,
                        shot.height());
            if (progress.heroClass() == HeroClass.WARRIOR) vfx.drawBlade();
            if (player.discipline().parrying())
                vfx.shield(
                        camera,
                        player.getPosition().x(),
                        player.getPosition().z(),
                        time,
                        Menu.TEAL);
            if (heavyCharge > 0)
                vfx.charge(
                        player.getPosition().x(),
                        player.getPosition().z(),
                        heavyCharge,
                        heavyCharge >= .75f && heavyCharge <= 1.05f ? Menu.GOLD : Menu.TEAL);
            for (Enemy enemy : enemies) {
                vfx.enemy(camera, enemy, time);
                if (enemy.elite) {
                    float radius = 1.05f;
                    for (int i = 0; i < 24; i++) {
                        double a = i * Math.PI / 12, b = (i + 1) * Math.PI / 12;
                        DrawLine3D(
                                v(
                                        enemy.x + (float) Math.cos(a) * radius,
                                        .13f,
                                        enemy.z + (float) Math.sin(a) * radius),
                                v(
                                        enemy.x + (float) Math.cos(b) * radius,
                                        .13f,
                                        enemy.z + (float) Math.sin(b) * radius),
                                Menu.GOLD);
                    }
                }
            }
            for (BossHazard hazard : bossHazards) vfx.hazard(camera, hazard, time);
            for (Projectile shot : projectiles)
                vfx.projectile(
                        camera,
                        shot.x,
                        shot.z,
                        shot.vx,
                        shot.vz,
                        shot.magic,
                        shot.magic ? Menu.color(180, 124, 235) : Menu.GOLD,
                        time);
            vfx.particles(camera);
            vfx.end();
        }
        EndMode3D();
        assets.finishLitPass();
        renderer.present();
    }

    private void drawActors(boolean shadowPass) {
        assets.preparePlayer(player);
        assets.drawPlayerBody(player, progress, hurtFlash > 0 ? Menu.RED : WHITE);
        if (progress.hasWeapon()) {
            assets.drawPlayerWeapon(player, progress.heroClass().ordinal(), progress.weaponTier());
        }
        assets.drawArmor(player, progress);
        if (state == GameState.CITADEL) {
            return;
        }
        for (Enemy enemy : enemies) assets.drawEnemy(enemy);
        for (Enemy enemy : fallen) assets.drawEnemy(enemy);
        if (progress.fieldEvent().kind() == FieldEvent.Kind.SURVIVOR)
            citadelHub.drawFieldSurvivor(
                    assets, progress.fieldEvent(), progress.rescuedSurvivors(), (float) GetTime());
        else assets.drawFieldEvent(progress.fieldEvent(), (float) GetTime());
        assets.drawRecoveryWisps(progress.recoveryWisps(), (float) GetTime());
        if (shadowPass) return;
        if (progress.baseLevel() >= 2) {
            float pulse = (float) Math.sin(GetTime() * 2) * .08f;
            DrawSphere(v(-2, 1.7f + pulse, 0), .22f, Menu.TEAL);
            DrawSphere(v(2, 1.7f - pulse, 0), .22f, Menu.GOLD);
        }
        if (guardianFlash > 0)
            DrawCylinderEx(
                    v(guardianX < 0 ? -2 : 2, 1.7f, 0),
                    v(guardianX, 1, guardianZ),
                    .035f,
                    .015f,
                    8,
                    Fade(Menu.TEAL, guardianFlash / .28f));
    }

    private void drawSanctuaryRing() {
        Color ring = Fade(Menu.TEAL, .62f);
        float radius = 6;
        int segments = 64;
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2 / segments, b = (i + 1) * Math.PI * 2 / segments;
            DrawLine3D(
                    v((float) Math.cos(a) * radius, .10f, (float) Math.sin(a) * radius),
                    v((float) Math.cos(b) * radius, .10f, (float) Math.sin(b) * radius),
                    ring);
        }
        DrawCylinderWires(v(0, .10f, 0), .52f, .52f, .08f, 12, Menu.GOLD);
    }

    private void drawEnemyHealth() {
        RecoveryWisps.Wisp nearestWisp = null;
        double nearestDistance = 10;
        for (var wisp : progress.recoveryWisps().active()) {
            double distance =
                    Math.hypot(
                            wisp.x() - player.getPosition().x(),
                            wisp.z() - player.getPosition().z());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestWisp = wisp;
            }
        }
        if (nearestWisp != null) {
            Vector2 point =
                    GetWorldToScreen(
                            v(nearestWisp.x(), 1.6f, nearestWisp.z()),
                            sceneManager.getOpenScene().getCameraController().getCamera());
            int x = (int) point.x(), y = (int) point.y();
            Menu.panel(x - 84, y - 5, 168, 36);
            Menu.center("RECOVERY WISP", x, y, 11, Menu.TEAL);
            Menu.center("18 HP + 26 " + progress.resourceLabel(), x, y + 15, 10, Menu.TEXT);
        }
        for (Enemy enemy : enemies) {
            Vector2 screen =
                    GetWorldToScreen(
                            v(enemy.x, enemy.boss() ? (enemy.king ? 6.0f : 5.2f) : 2.1f, enemy.z),
                            sceneManager.getOpenScene().getCameraController().getCamera());
            Menu.bar(
                    (int) screen.x() - 22,
                    (int) screen.y(),
                    44,
                    enemy.health,
                    enemy.maxHealth,
                    enemy.boss() || enemy.elite || enemy.variant == 2 ? Menu.GOLD : Menu.RED);
            String label =
                    tutorial != null
                            ? enemy.attackTime > .25f ? "PARRY THE GOLD STRIKE" : "PRACTICE PARTNER"
                            : enemy.attackTime > .25f
                                    ? "! ATTACK"
                                    : enemy.boss()
                                            ? (wave == 10
                                                    ? "HOLLOW KING"
                                                    : wave == 5 ? "ASH WARDEN" : "WARLORD")
                                            : enemy.label();
            DrawRectangle(
                    (int) screen.x() - Menu.measure(label, 10) / 2 - 4,
                    (int) screen.y() - 17,
                    Menu.measure(label, 10) + 8,
                    15,
                    Fade(Menu.INK, .85f));
            Menu.text(
                    label,
                    (int) screen.x() - Menu.measure(label, 10) / 2,
                    (int) screen.y() - 14,
                    10,
                    tutorial != null ? Menu.GOLD : enemy.attackTime > .25f ? Menu.RED : Menu.TEXT);
        }
    }

    /** Waiting for the first arrival is preparation, not a playable "wave zero". */
    private String waveProgressText(int wavesLeft) {
        if (tutorial != null)
            return "LESSON " + (tutorial.step().ordinal() + 1) + " OF " + tutorial.lessonCount();
        if (wave < chapter().first())
            return chapter().bossOnly()
                    ? "BOSS APPROACHING"
                    : "PREPARING / " + chapter().waves() + " WAVES";
        if (chapter().bossOnly()) return "BOSS ONLY / " + enemies.size() + " HOSTILES";
        return "WAVE "
                + (wave - chapter().first() + 1)
                + " / "
                + chapter().waves()
                + " - "
                + wavesLeft
                + " LEFT";
    }

    private void drawHud() {
        int w = GetScreenWidth(), h = GetScreenHeight();
        boolean compact = w < 1000;
        int chapterCenter = compact ? (w + 216 - 140) / 2 : w / 2;
        int top = chapterCenter - (compact ? 116 : 130);
        Menu.panel(top, 20, compact ? 232 : 260, 82);
        Menu.center(
                tutorial != null
                        ? "TUTORIAL / SAFE PRACTICE"
                        : "LEVEL " + (chapterIndex() + 1) + " / " + chapter().title(),
                chapterCenter,
                29,
                compact ? 11 : 14,
                Menu.GOLD);
        int left =
                chapter().waves()
                        - chapter()
                                .cleared(
                                        wave,
                                        director.remaining(),
                                        enemies.size(),
                                        director.total());
        Menu.center(waveProgressText(left), chapterCenter, 54, compact ? 12 : 14, Menu.TEXT);
        Menu.center(
                tutorial != null
                        ? "No raids / Your campaign is safe"
                        : state == GameState.LEVEL_COMPLETE || state == GameState.VICTORY
                                ? "THE PRIMORDIAL BEACON ENDURES"
                                : director.remaining() > 0
                                        ? director.remaining() + " frost legions approaching"
                                        : enemies.isEmpty()
                                                ? WaveFormation.label(wave + 1)
                                                        + " / "
                                                        + (int) Math.ceil(nextWave)
                                                        + "s"
                                                : "Hold the sanctuary perimeter",
                chapterCenter,
                77,
                compact ? 9 : 11,
                Menu.MUTED);
        int currencyY = 20;
        Menu.panel(w - 156, currencyY, 140, 48);
        Menu.icon("crystal", w - 144, currencyY + 14, 20, Menu.GOLD);
        Menu.text("CRYSTALS", w - 114, currencyY + 6, 10, Menu.MUTED);
        Menu.text(String.valueOf(progress.crystals()), w - 114, currencyY + 22, 17, Menu.GOLD);
        Menu.text("[J] CODEX / CHRONICLE", w - 156, currencyY + 54, compact ? 9 : 10, Menu.TEAL);
        if (w >= 1000) drawMinimap(w - 184, 82);
        if (state == GameState.PLAYING && !inventory && !workshop) drawCombatCues();
        int sy = 194, sw = compact ? 130 : 208;
        float dt = Math.max(0, Math.min(.1f, GetFrameTime()));
        float currentBaseHp = progress.baseHealth();
        if (sanctuaryGhostHealth < 0 || currentBaseHp >= sanctuaryGhostHealth) {
            sanctuaryGhostHealth = currentBaseHp;
            sanctuaryGhostDelay = 0;
        } else {
            sanctuaryGhostDelay += dt;
            if (sanctuaryGhostDelay >= 0.28f) {
                sanctuaryGhostHealth =
                        Math.max(
                                currentBaseHp,
                                sanctuaryGhostHealth
                                        - Math.max(
                                                        35f,
                                                        (sanctuaryGhostHealth - currentBaseHp)
                                                                * 4.5f)
                                                * dt);
            }
        }
        Menu.panel(16, sy, sw, 80);
        Menu.text("BEACON INTEGRITY", 28, sy + 8, compact ? 10 : 13, Menu.GOLD);
        Menu.text(
                (int) progress.baseHealth() + " / " + progress.maxBaseHealth(),
                28,
                sy + 26,
                18,
                Menu.TEXT);
        Menu.bar(
                28,
                sy + 48,
                sw - 24,
                progress.baseHealth(),
                sanctuaryGhostHealth,
                progress.maxBaseHealth(),
                Menu.GOLD);
        String sanctuaryCue =
                nearBase()
                        ? (compact
                                ? "+"
                                        + Math.round(progress.healRate())
                                        + " HP/s  "
                                        + settings.controls.label(INTERACT)
                                        + " +100/30CR"
                                : "+"
                                        + Math.round(progress.healRate())
                                        + " HP/s / "
                                        + settings.controls.label(INTERACT)
                                        + " +100 / 30 CR")
                        : "BEACON " + String.format("%.0f", sanctuaryDistance()) + "m / RADAR";
        Menu.text(sanctuaryCue, 28, sy + 62, compact ? 8 : 9, nearBase() ? Menu.TEAL : Menu.MUTED);
        boolean hasSpire = progress.baseLevel() >= 2 || progress.spireLevel() >= 2;
        if (hasSpire && !compact && storyTime <= 0) {
            float maxCooldown = SanctuaryWard.interval(progress.spireLevel());
            float range = SanctuaryWard.range(progress.spireLevel());
            Menu.panel(16, sy + 86, 208, 43);
            Menu.text(
                    guardianCooldown > 0
                            ? "LIGHT SPIRE RECHARGING"
                            : "LIGHT SPIRE / " + String.format("%.0f", range) + "m",
                    28,
                    sy + 93,
                    10,
                    Menu.TEAL);
            Menu.bar(28, sy + 114, 184, maxCooldown - guardianCooldown, maxCooldown, Menu.TEAL);
        }
        hudScreen.draw(
                progress,
                player,
                settings.controls,
                new HudScreen.CombatState(
                        attackCooldown,
                        activeAttack.duration(),
                        activeAttack.impact(),
                        heavyCharge,
                        combo,
                        comboWindow,
                        mastery.label(progress.heroClass()),
                        mastery.stacks() > 0));
        if (!compact) {
            Menu.panel(w - 232, h - 88, 216, 76);
            Menu.text(
                    settings.controls.label(DODGE)
                            + " DODGE / "
                            + settings.controls.label(SPRINT)
                            + " SPRINT",
                    w - 220,
                    h - 75,
                    12,
                    Menu.TEXT);
            Menu.text(
                    settings.controls.label(INVENTORY) + " INVENTORY / ESC PAUSE",
                    w - 220,
                    h - 49,
                    11,
                    Menu.MUTED);
            Menu.text(
                    settings.controls.label(SAVE)
                            + " SAVE / "
                            + settings.controls.label(INTERACT)
                            + " INTERACT",
                    w - 220,
                    h - 27,
                    10,
                    Menu.MUTED);
        }
        if (blessingReminder.visible()) {
            int bx = w - 424, by = 260;
            Menu.panel(bx, by, 400, 92);
            Menu.icon("crystal", bx + 14, by + 15, 24, Menu.GOLD);
            Menu.text(
                    progress.expedition().embers() + " EMBER BLESSING AVAILABLE",
                    bx + 48,
                    by + 12,
                    16,
                    Menu.GOLD);
            Menu.text(
                    "Choose a bonus that strengthens this expedition.",
                    bx + 16,
                    by + 40,
                    12,
                    Menu.TEXT);
            Menu.text(
                    "Return to the Beacon, clear enemies, press "
                            + settings.controls.label(INTERACT),
                    bx + 16,
                    by + 64,
                    12,
                    Menu.TEAL);
        }
        FieldEvent event = progress.fieldEvent();
        if (event.visible() && state == GameState.PLAYING && !inventory && !workshop) {
            Menu.panel(24, 338, 270, 128);
            boolean rescue = event.kind() == FieldEvent.Kind.SURVIVOR;
            Menu.text(
                    rescue ? "RESCUE / BRING THEM HOME" : "OPTIONAL / " + event.title(),
                    38,
                    350,
                    15,
                    Menu.GOLD);
            Menu.wrapped(
                    rescue
                            ? event.status() == FieldEvent.Status.WAITING
                                    ? "Press "
                                            + settings.controls.label(INTERACT)
                                            + " near survivor. Escort to Beacon."
                                    : "Beacon / "
                                            + Math.round(Math.hypot(event.x(), event.z()))
                                            + "m away. Stay close and protect them."
                            : "[" + settings.controls.label(INTERACT) + "] " + event.instruction(),
                    38,
                    380,
                    244,
                    14,
                    Menu.TEXT);
            Menu.text(
                    (int) Math.ceil(event.time())
                            + "s / "
                            + (event.vulnerable()
                                    ? "INTEGRITY " + (int) event.health()
                                    : "+65 CRYSTALS + BLESSING"),
                    38,
                    443,
                    12,
                    Menu.TEAL);
            Vector2 marker =
                    GetWorldToScreen(
                            v(event.x(), 2.8f, event.z()),
                            sceneManager.getOpenScene().getCameraController().getCamera());
            Menu.text(
                    event.kind() == FieldEvent.Kind.SURVIVOR ? "SURVIVOR" : "EMBER OBJECTIVE",
                    (int) marker.x() - 50,
                    (int) marker.y(),
                    12,
                    Menu.TEAL);
        }
        for (Enemy enemy : enemies)
            if (enemy.boss() && enemy.health > 0) {
                if (bossGhostHealth < 0 || enemy.health >= bossGhostHealth) {
                    bossGhostHealth = enemy.health;
                    bossGhostDelay = 0;
                } else {
                    bossGhostDelay += dt;
                    if (bossGhostDelay >= 0.28f) {
                        bossGhostHealth =
                                Math.max(
                                        enemy.health,
                                        bossGhostHealth
                                                - Math.max(
                                                                40f,
                                                                (bossGhostHealth - enemy.health)
                                                                        * 4.5f)
                                                        * dt);
                    }
                }
                Menu.panel(w / 2 - 170, 114, 340, 72);
                Menu.center(
                        (wave == 10 ? "HOLLOW KING" : wave == 5 ? "ASH WARDEN" : "WARLORD")
                                + "  /  "
                                + (int) enemy.health
                                + " HP",
                        w / 2,
                        121,
                        18,
                        Menu.GOLD);
                Menu.bar(
                        w / 2 - 154,
                        151,
                        308,
                        enemy.health,
                        bossGhostHealth,
                        enemy.maxHealth,
                        Menu.RED);
                Menu.center(
                        (enemy.enraged() ? "ENRAGED / " : "")
                                + enemy.moveLabel()
                                + (enemy.bossMove.parryable
                                        ? " / PARRY OR EVADE"
                                        : " / UNBLOCKABLE"),
                        w / 2,
                        164,
                        12,
                        Menu.GOLD);
            }
        if (noticeTime > 0 && state == GameState.PLAYING) {
            int nw = compact ? 250 : 280, ny = compact ? h - 270 : 198;
            Menu.panel((w - nw) / 2, ny, nw, 74);
            Menu.wrapped(notice, w / 2 - nw / 2 + 12, ny + 10, nw - 24, 12, Menu.TEXT);
        }
    }

    private void drawCitadelTutorial() {
        if (tutorial == null) return;
        int width = Math.max(400, Math.min(580, GetScreenWidth() - 400)),
                x = (GetScreenWidth() - width) / 2,
                y = 88;
        if (tutorial.step() == Tutorial.Step.MOVE) {
            String text =
                    "Move with "
                            + settings.controls.label(FORWARD)
                            + " / "
                            + settings.controls.label(LEFT)
                            + " / "
                            + settings.controls.label(BACK)
                            + " / "
                            + settings.controls.label(RIGHT)
                            + ". Walk a few steps to find your footing in Aethelgard Citadel.";
            int textH = Menu.wrappedHeight(text, width - 40, 14);
            int panelH = 68 + textH + 50;
            Menu.panel(x, y, width, panelH);
            Menu.text("CITADEL ORIENTATION / MOVEMENT", x + 20, y + 14, 14, Menu.TEAL);
            Menu.text("Find your footing", x + 20, y + 36, 20, Menu.GOLD);
            Menu.wrapped(text, x + 20, y + 64, width - 40, 14, Menu.TEXT);
            if (Menu.button("SKIP TUTORIAL", x + width - 160, y + 64 + textH + 10, 140, true)) {
                finishTutorial();
            }
        } else if (!progress.hasWeapon()) {
            String mentorGuide =
                    progress.heroClass() == HeroClass.WARRIOR
                            ? "Visit Goran the Blacksmith at the forge (West of the central road)."
                            : progress.heroClass() == HeroClass.ARCHER
                                    ? "Visit Diana the Huntress at the Archery Range (Northwest of"
                                            + " spawn)."
                                    : "Visit Lady Elyndra at the Arcanist Sanctum (East of the"
                                            + " central road).";
            int textH = Menu.wrappedHeight(mentorGuide, width - 40, 14);
            int panelH = 68 + textH + 50;
            Menu.panel(x, y, width, panelH);
            Menu.text("CITADEL ORIENTATION / MOVEMENT COMPLETE", x + 20, y + 14, 14, Menu.TEAL);
            Menu.text("Claim Your Starter Weapon", x + 20, y + 36, 20, Menu.GOLD);
            Menu.wrapped(mentorGuide, x + 20, y + 64, width - 40, 14, Menu.TEXT);
            if (Menu.button("SKIP TUTORIAL", x + width - 160, y + 64 + textH + 10, 140, true)) {
                finishTutorial();
            }
        }
    }

    private void drawTutorial() {
        int width = Math.max(480, Math.min(620, GetScreenWidth() - 200)),
                x = (GetScreenWidth() - width) / 2,
                y = 100;
        int lessonIndex = Math.max(1, tutorial.step().ordinal());
        String instruction = tutorial.instruction(settings.controls, progress.heroClass());
        String hint =
                switch (tutorial.step()) {
                    case MOVE ->
                            "Green health / "
                                    + (progress.heroClass() == HeroClass.MAGE
                                            ? "Blue mana"
                                            : "Gold stamina")
                                    + " / Sanctuary: upper left";
                    case FOCUS ->
                            "Press "
                                    + settings.controls.label(FOCUS)
                                    + " to select the marked partner";
                    case STRIKE -> "Hit the marked partner twice with your weapon";
                    case DODGE -> "Move + " + settings.controls.label(DODGE);
                    case CHARGE ->
                            heavyCharge >= .75f && heavyCharge <= 1.05f
                                    ? "RELEASE NOW"
                                    : heavyCharge > 1.05f
                                            ? "Release, then try a shorter charge"
                                            : "Hold "
                                                    + settings.controls.label(HEAVY)
                                                    + " - wait for gold";
                    case PARRY ->
                            enemyInsideParryWindow()
                                    ? "PRESS " + settings.controls.label(PARRY) + " NOW"
                                    : "Face the partner - wait for their strike";
                    case GEAR ->
                            "Open and close your pack with " + settings.controls.label(INVENTORY);
                    case HAVEN ->
                            nearBase()
                                    ? "Inside the healing ring - stay here"
                                    : "Walk into the teal circle around the sanctuary";
                    case COMPLETE -> "Your adventure is ready";
                };
        int instH = Menu.wrappedHeight(instruction, width - 40, 16);
        int hintH = Menu.wrappedHeight(hint, width - 40, 13);
        int panelH = 75 + instH + 10 + hintH + 54;
        Menu.panel(x, y, width, panelH);
        Menu.text(
                "COMBAT TRAINING / LESSON " + lessonIndex + " OF " + (tutorial.lessonCount() - 1),
                x + 20,
                y + 14,
                15,
                Menu.TEAL);
        Menu.text(tutorial.title(), x + 20, y + 38, 22, Menu.GOLD);
        int instY = y + 70;
        Menu.wrapped(instruction, x + 20, instY, width - 40, 16, Menu.TEXT);
        int hintY = instY + instH + 10;
        Menu.wrapped(hint, x + 20, hintY, width - 40, 13, Menu.TEAL);
        int btnY = hintY + hintH + 12;
        if (Menu.button(
                tutorial.complete() ? "BEGIN YOUR ADVENTURE" : "SKIP TRAINING",
                x + 20,
                btnY,
                tutorial.complete() ? width - 40 : 180,
                true)) finishTutorial();
    }

    private void drawPlayerHistory() {
        dim();
        int x = (GetScreenWidth() - 760) / 2, y = 55;
        Menu.panel(x - 20, 30, 800, 650);
        Menu.text("PLAYERS & SAVES", x, y, 32, Menu.TEXT);
        Menu.text(
                "Each warden keeps their own adventure and chapter checkpoint.",
                x,
                y + 43,
                16,
                Menu.MUTED);
        if (playerHistory.isEmpty())
            Menu.text("Create your first warden with New Game.", x, y + 110, 22, Menu.GOLD);
        for (int row = 0; row < 5; row++) {
            int index = historyPage * 5 + row;
            if (index >= playerHistory.size()) break;
            var entry = playerHistory.get(index);
            var saved = entry.snapshot();
            int top = y + 85 + row * 90;
            Menu.panel(x, top, 760, 86);
            Menu.text(
                    saved.name() + " / " + saved.progression().heroClass().label,
                    x + 16,
                    top + 12,
                    21,
                    Menu.TEXT);
            Menu.text(
                    "Chapter "
                            + (Campaign.index(
                                            saved.wave().total() == 0
                                                    ? saved.wave().wave() + 1
                                                    : saved.wave().wave())
                                    + 1)
                            + " / "
                            + saved.progression().difficulty().label
                            + " / "
                            + saved.progression().kills()
                            + " defeats",
                    x + 16,
                    top + 43,
                    15,
                    Menu.MUTED);
            Menu.text(
                    "Saved "
                            + java.time.Instant.ofEpochMilli(entry.updated())
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDateTime()
                                    .format(
                                            java.time.format.DateTimeFormatter.ofPattern(
                                                    "MMM d, yyyy  HH:mm")),
                    x + 16,
                    top + 65,
                    11,
                    Menu.MUTED);
            if (Menu.button("CONTINUE", x + 530, top + 19, 124, pendingDelete == null)) {
                try {
                    playerSaves.activate(entry, savePath, checkpointPath);
                    loadCurrent();
                } catch (IOException e) {
                    menuMessage = "Could not load this player.";
                }
                return;
            }
            if (Menu.button("DELETE", x + 672, top + 19, 74, pendingDelete == null))
                pendingDelete = entry;
        }
        DrawLine(x, y + 550, x + 760, y + 550, Menu.LINE);
        if (Menu.button("BACK", x, y + 565, 180, pendingDelete == null))
            state = GameState.MAIN_MENU;
        Menu.text(playerHistory.size() + " WARDENS", x + 230, y + 580, 14, Menu.MUTED);
        if (Menu.button("<", x + 548, y + 565, 48, pendingDelete == null && historyPage > 0))
            historyPage--;
        Menu.center(
                (historyPage + 1) + " / " + Math.max(1, (playerHistory.size() + 4) / 5),
                x + 654,
                y + 579,
                16,
                Menu.TEXT);
        if (Menu.button(
                ">",
                x + 712,
                y + 565,
                48,
                pendingDelete == null && (historyPage + 1) * 5 < playerHistory.size()))
            historyPage++;
        if (!historyMessage.isEmpty()) Menu.text(historyMessage, x, y + 62, 14, Menu.RED);
        if (pendingDelete != null) {
            dim();
            int dx = (GetScreenWidth() - 540) / 2, dy = (GetScreenHeight() - 230) / 2;
            Menu.panel(dx, dy, 540, 230);
            Menu.text("DELETE THIS WARDEN?", dx + 24, dy + 22, 26, Menu.GOLD);
            Menu.text(
                    pendingDelete.snapshot().name()
                            + " / "
                            + pendingDelete.snapshot().progression().heroClass().label,
                    dx + 24,
                    dy + 68,
                    20,
                    Menu.TEXT);
            Menu.wrapped(
                    "Their adventure and chapter checkpoint will be removed. This cannot be"
                            + " undone.",
                    dx + 24,
                    dy + 105,
                    490,
                    16,
                    Menu.MUTED);
            if (Menu.button("CANCEL", dx + 24, dy + 164, 230, true)) pendingDelete = null;
            if (Menu.button("DELETE WARDEN", dx + 278, dy + 164, 238, true)) {
                try {
                    playerSaves.delete(pendingDelete, savePath, checkpointPath);
                    if (!Files.exists(savePath)) {
                        settings.playerName = "Warden";
                        persistSettings();
                    }
                    playerHistory = playerSaves.entries();
                    historyPage =
                            Math.min(historyPage, Math.max(0, (playerHistory.size() - 1) / 5));
                } catch (IOException e) {
                    historyMessage = "Could not delete this save. Please try again.";
                }
                pendingDelete = null;
            }
        }
    }

    private void drawCombatCues() {
        WorldLandmarks.Landmark place =
                WorldLandmarks.nearby(
                        chapterIndex(), player.getPosition().x(), player.getPosition().z());
        if (place != null) {
            if (!place.name().equals(lastDiscoveredLandmark)) {
                lastDiscoveredLandmark = place.name();
                landmarkBannerTime = 3.6f;
            }
        } else {
            lastDiscoveredLandmark = "";
        }
        if (tutorial == null && activeBoss() == null && place != null) {
            if (landmarkBannerTime > 0) {
                float alpha =
                        landmarkBannerTime > 2.8f
                                ? (3.6f - landmarkBannerTime) / 0.8f
                                : landmarkBannerTime < 0.8f ? landmarkBannerTime / 0.8f : 1.0f;
                alpha = Math.max(0f, Math.min(1f, alpha));
                int bw = 460, bh = 54;
                int bx = GetScreenWidth() / 2 - bw / 2, by = 110;
                DrawRectangleGradientH(
                        bx, by, bw / 2, bh, Fade(Menu.INK, 0), Fade(Menu.INK, 0.90f * alpha));
                DrawRectangleGradientH(
                        bx + bw / 2,
                        by,
                        bw / 2,
                        bh,
                        Fade(Menu.INK, 0.90f * alpha),
                        Fade(Menu.INK, 0));
                DrawLine(bx + 35, by, bx + bw - 35, by, Fade(Menu.GOLD, 0.80f * alpha));
                DrawLine(bx + 55, by + bh, bx + bw - 55, by + bh, Fade(Menu.GOLD, 0.80f * alpha));
                Menu.center(
                        place.name(), GetScreenWidth() / 2, by + 10, 20, Fade(Menu.GOLD, alpha));
                String sub = chapter().title() + " • CHAPTER " + (chapterIndex() + 1);
                Menu.center(sub, GetScreenWidth() / 2, by + 34, 11, Fade(Menu.TEXT, alpha * 0.9f));
            } else {
                Menu.panel(GetScreenWidth() / 2 - 130, 118, 260, 38);
                Menu.center(place.name(), GetScreenWidth() / 2, 129, 13, Menu.GOLD);
            }
        }
        // Practice partners have artificial health; keep their guided tutorial labels instead.
        Enemy focused = enemyFocus.selected();
        if (focused != null) {
            targetFrameScreen.draw(
                    focused,
                    focused.distance(player.getPosition().x(), player.getPosition().z()),
                    settings.controls.label(FOCUS),
                    tutorial != null);
            Vector2 mark =
                    GetWorldToScreen(
                            v(focused.x, .15f, focused.z),
                            sceneManager.getOpenScene().getCameraController().getCamera());
            DrawCircleLines((int) mark.x(), (int) mark.y(), 16, Menu.GOLD);
        }
        if (pointerAim && !IsMouseButtonDown(MOUSE_BUTTON_RIGHT)) {
            Vector2 mouse = GetMousePosition();
            int x = (int) mouse.x(), y = (int) mouse.y();
            Color color = hitMarker > 0 ? Menu.GOLD : Fade(Menu.TEXT, .65f);
            int r = hitMarker > 0 ? 10 : 6;
            DrawCircleLines(x, y, r, color);
            DrawLine(x - r - 4, y, x - r, y, color);
            DrawLine(x + r, y, x + r + 4, y, color);
            DrawLine(x, y - r - 4, x, y - r, color);
            DrawLine(x, y + r, x, y + r + 4, color);
        }
        if (hitMarker > 0)
            Menu.center("HIT", GetScreenWidth() / 2, GetScreenHeight() / 2 + 35, 13, Menu.GOLD);
    }

    private void drawMinimap(int x, int y) {
        Menu.panel(x, y, 160, 160);
        WorldLandmarks.Landmark place =
                WorldLandmarks.nearby(
                        chapterIndex(), player.getPosition().x(), player.getPosition().z());
        String locationTitle = place != null ? place.name() : chapter().title();
        if (Menu.measure(locationTitle, 11) > 136) {
            Menu.text(locationTitle, x + 12, y + 10, 9, place != null ? Menu.GOLD : Menu.MUTED);
        } else {
            Menu.text(locationTitle, x + 12, y + 10, 11, place != null ? Menu.GOLD : Menu.MUTED);
        }
        Menu.text(
                dayNight.frame().label() + " / " + dayNight.frame().clock(),
                x + 12,
                y + 143,
                11,
                Menu.GOLD);
        int cx = x + 80, cy = y + 88;
        Menu.center("N", cx, cy - 69, 10, Menu.GOLD);
        Menu.center("S", cx, cy + 61, 9, Menu.MUTED);
        Menu.center("W", cx - 69, cy - 5, 9, Menu.MUTED);
        Menu.center("E", cx + 69, cy - 5, 9, Menu.MUTED);

        DrawCircle(cx, cy, 59, Fade(Menu.INK, 0.6f));
        DrawCircleLines(cx, cy, 59, Menu.LINE);
        DrawCircleLines(cx, cy, 15, Fade(Menu.TEAL, .8f));
        DrawLine(cx - 59, cy, cx + 59, cy, Fade(Menu.LINE, 0.45f));
        DrawLine(cx, cy - 59, cx, cy + 59, Fade(Menu.LINE, 0.45f));
        DrawCircle(cx, cy, 5, Menu.TEAL);
        if (progress.fieldEvent().visible())
            DrawCircle(
                    cx + (int) (progress.fieldEvent().x() * 2.5f),
                    cy + (int) (progress.fieldEvent().z() * 2.5f),
                    4,
                    Menu.TEAL);
        for (var wisp : progress.recoveryWisps().active())
            DrawCircleLines(
                    cx + (int) (wisp.x() * 2.5f), cy + (int) (wisp.z() * 2.5f), 4, Menu.TEAL);
        Enemy focused = enemyFocus.selected();
        for (Enemy e : enemies) {
            int ex = cx + (int) (e.x * 2.5f);
            int ez = cy + (int) (e.z * 2.5f);
            DrawCircle(ex, ez, 3, e.boss() || e.elite || e.variant == 2 ? Menu.GOLD : Menu.RED);
            if (e == focused) {
                DrawPolyLines(new Vector2().x(ex).y(ez), 4, 7, 45, Menu.GOLD);
            }
        }
        if (director.warning())
            DrawCircleLines(
                    cx + (int) (director.x() * 2.5f),
                    cy + (int) (director.z() * 2.5f),
                    6,
                    Menu.GOLD);
        float px = cx + player.getPosition().x() * 2.5f;
        float pz = cy + player.getPosition().z() * 2.5f;
        float facingRad = (float) Math.toRadians(player.getRotation().y());
        float noseX = px + (float) Math.sin(facingRad) * 6.5f;
        float noseZ = pz + (float) Math.cos(facingRad) * 6.5f;
        float leftX = px + (float) Math.sin(facingRad + 2.5f) * 4.5f;
        float leftZ = pz + (float) Math.cos(facingRad + 2.5f) * 4.5f;
        float rightX = px + (float) Math.sin(facingRad - 2.5f) * 4.5f;
        float rightZ = pz + (float) Math.cos(facingRad - 2.5f) * 4.5f;
        DrawTriangle(
                new Vector2().x(noseX).y(noseZ),
                new Vector2().x(leftX).y(leftZ),
                new Vector2().x(rightX).y(rightZ),
                Menu.TEXT);
    }

    private void dim() {
        DrawRectangle(0, 0, GetScreenWidth(), GetScreenHeight(), Fade(Menu.INK, .92f));
    }

    private final MainMenuScreen mainMenuScreen = new MainMenuScreen();

    private void drawMainMenu() {
        DrawRectangle(0, 0, GetScreenWidth(), GetScreenHeight(), Fade(Menu.INK, .55f));
        switch (mainMenuScreen.draw(settings.playerName, Files.exists(savePath), menuMessage)) {
            case PLAY_TOGETHER -> coopRequested = true;
            case NEW_GAME -> requestNewGame();
            case LOAD_GAME -> loadCurrent();
            case QUIT -> quitRequested = true;
            case SETTINGS -> openSettings();
            case CHRONICLE -> {
                journalReturn = state;
                state = GameState.JOURNAL;
            }
            case PLAYER_HISTORY -> openPlayerHistory();
            case NONE -> {}
        }
    }

    private void openPlayerHistory() {
        try {
            if (Files.exists(savePath)) {
                try {
                    playerSaves.save(SaveGame.read(savePath), checkpointPath);
                } catch (IOException damaged) {
                    menuMessage = "Current save could not be imported.";
                }
            }
            playerHistory = playerSaves.entries();
            historyPage = 0;
            pendingDelete = null;
            historyMessage = "";
            state = GameState.PLAYER_HISTORY;
        } catch (IOException failure) {
            menuMessage = "Could not open player saves.";
        }
    }

    private void drawInventory() {
        dim();
        if (equipmentScreen.draw(progress, settings.controls.label(INVENTORY))) inventory = false;
    }

    private void chooseBlessing(int index) {
        var offers = progress.expedition().offers();
        if (index < 0 || index >= offers.size()) return;
        Expedition.Blessing choice = offers.get(index);
        if (progress.expedition().choose(choice)) {
            applyProgression();
            audio.play("upgrade");
            state = blessingReturn;
            notifyPlayer(
                    "BLESSING CLAIMED / "
                            + choice.title.toUpperCase()
                            + " / RANK "
                            + progress.expedition().rank(choice));
            saveCurrent();
        }
    }

    private void drawBlessings() {
        dim();
        int x = (GetScreenWidth() - 900) / 2, y = (GetScreenHeight() - 510) / 2;
        Menu.panel(x - 20, y - 20, 940, 550);
        Menu.text("CHOOSE YOUR EMBER BLESSING", x + 20, y + 12, 32, Menu.GOLD);
        Menu.text(
                "Shape your expedition / "
                        + progress.expedition().embers()
                        + " ember"
                        + (progress.expedition().embers() == 1 ? "" : "s")
                        + " to spend",
                x + 20,
                y + 60,
                18,
                Menu.TEAL);
        var offers = progress.expedition().offers();
        Vector2 mouse = GetMousePosition();
        for (int i = 0; i < offers.size(); i++) {
            var b = offers.get(i);
            int left = x + 20 + i * 292;
            int cardY = y + 108;
            boolean overCard =
                    mouse.x() >= left
                            && mouse.x() <= left + 272
                            && mouse.y() >= cardY
                            && mouse.y() <= cardY + 300;
            Menu.panel(left, cardY, 272, 300);
            if (overCard) DrawRectangleLines(left - 2, cardY - 2, 276, 304, Menu.GOLD);
            Menu.icon(
                    b == Expedition.Blessing.BLOODROOT
                            ? "heart"
                            : b == Expedition.Blessing.BULWARK ? "crown" : "sword",
                    left + 24,
                    y + 128,
                    34,
                    Menu.TEAL);
            Menu.text(b.title, left + 22, y + 182, 25, Menu.GOLD);
            Menu.text(
                    "RANK "
                            + progress.expedition().rank(b)
                            + " > "
                            + (progress.expedition().rank(b) + 1)
                            + " / 3",
                    left + 22,
                    y + 220,
                    16,
                    Menu.MUTED);
            Menu.wrapped(b.description, left + 22, y + 254, 228, 18, Menu.TEXT);
            boolean claim = Menu.button("[" + (i + 1) + "] SELECT", left + 20, y + 350, 232, true);
            if (claim || overCard && IsMouseButtonPressed(MOUSE_BUTTON_LEFT)) {
                chooseBlessing(i);
                break;
            }
        }
        Menu.text(
                "Select anywhere on a card or press its number. Your choice is saved.",
                x + 20,
                y + 440,
                17,
                Menu.MUTED);
        if (Menu.button("RETURN [ESC]", x + 620, y + 466, 252, true)) state = blessingReturn;
    }

    private void drawWorkshop() {
        dim();
        int crystalsBefore = progress.crystals();
        if (workshopScreen.draw(progress, "ESC", audio)) workshop = false;
        if (progress.crystals() != crystalsBefore) {
            applyProgression();
            saveCurrent();
        }
    }

    private void drawOverlay() {
        if (state == GameState.GAME_OVER && outcomeTime < 1.4f) {
            int center = GetScreenWidth() / 2;
            Menu.panel(center - 235, 186, 470, 74);
            Menu.center(
                    progress.health() <= 0 ? "WARDEN FALLEN" : "THE SANCTUARY HAS FALLEN",
                    center,
                    199,
                    27,
                    Menu.RED);
            Menu.center("The ember waits for your return.", center, 238, 16, Menu.TEXT);
            return;
        }
        dim();
        int x = (GetScreenWidth() - 440) / 2, y = (GetScreenHeight() - 440) / 2;
        Menu.panel(x - 24, y - 20, 488, 440);
        boolean lost = state == GameState.GAME_OVER;
        Menu.text(
                lost
                        ? (progress.health() <= 0 ? "WARDEN FALLEN" : "OUTPOST FALLEN")
                        : (pauseReturn == GameState.CITADEL || state == GameState.CITADEL
                                ? "CITADEL SANCTUARY"
                                : "DEFENSE PAUSED"),
                x,
                y,
                36,
                Menu.TEXT);
        Menu.text(
                lost
                        ? "Wave " + wave + " / " + progress.kills() + " defeated"
                        : player.getName() + " / Wave " + wave,
                x,
                y + 54,
                20,
                Menu.MUTED);
        if (Menu.button(
                lost
                        ? (Files.exists(playtest ? playtestCheckpointPath : checkpointPath)
                                ? "RETRY CHAPTER [ENTER]"
                                : "NEW RUN [ENTER]")
                        : "RESUME",
                x,
                y + 100,
                440,
                true)) {
            if (lost) retryChapter();
            else state = pauseReturn;
        }
        if (Menu.button(
                playtest ? "SAVE PLAYTEST" : "SAVE GAME",
                x,
                y + 154,
                212,
                !lost && tutorial == null)) saveCurrent();
        if (Menu.button(
                playtest ? "LOAD PLAYTEST" : "LOAD GAME",
                x + 228,
                y + 154,
                212,
                tutorial == null && Files.exists(playtest ? playtestSavePath : savePath)))
            loadActive();
        if (Menu.button("SETTINGS", x, y + 208, 212, true)) openSettings();
        if (Menu.button("CHRONICLE", x + 228, y + 208, 212, true)) {
            journalReturn = state;
            state = GameState.JOURNAL;
        }
        boolean canRecall = !lost && tutorial == null && (enemies.isEmpty() && attackCooldown <= 0);
        String recallLabel = canRecall ? "VISIT CITADEL" : "VISIT CITADEL (IN COMBAT)";
        if (!lost && tutorial == null && Menu.button(recallLabel, x, y + 262, 212, canRecall))
            travelToCitadel();
        if (!lost && tutorial == null && Menu.button("WORLD MAP", x + 228, y + 262, 212, true))
            openChapterMap();
        if (Menu.button("SAVE & EXIT", x, y + 316, 440, !lost)) {
            saveCurrent();
            if (practiceReturn != null) {
                restoreSnapshot(practiceReturn, practiceReturnPlaytest);
                practiceReturn = null;
            }
            tutorial = null;
            state = GameState.MAIN_MENU;
        }
        if (lost && Menu.button("MAIN MENU", x, y + 262, 440, true)) state = GameState.MAIN_MENU;
        Menu.text(
                lost
                        ? "Retry restores your gear at the start of this chapter."
                        : playtest
                                ? "Separate playtest save / Campaign save is preserved."
                                : "Closing the game also saves an active defense.",
                x,
                y + 380,
                15,
                Menu.MUTED);
    }

    private void drawChapterEnd() {
        if (chapter().bossOnly() && outcomeTime < (state == GameState.VICTORY ? 8f : 1.6f)) {
            int center = GetScreenWidth() / 2;
            if (noticeTime > 0) {
                Menu.panel(center - 270, 122, 540, 48);
                Menu.center(notice, center, 136, 16, Menu.GOLD);
            }
            Menu.panel(center - 270, 186, 540, 80);
            Menu.center(chapter().title().toUpperCase() + " DEFEATED", center, 201, 27, Menu.GOLD);
            Menu.center(
                    state == GameState.VICTORY
                            ? "The curse breaks. Watch the valley wake beneath the dawn."
                            : "The ember endures. Congratulations, warden!",
                    center,
                    239,
                    16,
                    Menu.TEXT);
            return;
        }
        dim();
        int x = (GetScreenWidth() - 660) / 2, y = (GetScreenHeight() - 480) / 2;
        for (int i = 0; i < 48; i++) {
            float t = outcomeTime + i * .17f, life = (t % 3) / 3;
            int cx = (int) ((i * 149.7f + Math.sin(t * 2) * 32) % GetScreenWidth()),
                    cy = (int) (GetScreenHeight() * (1 - life));
            DrawCircle(
                    cx, cy, 2 + i % 3, Fade(i % 2 == 0 ? Menu.GOLD : Menu.TEAL, (1 - life) * .65f));
        }
        Menu.panel(x - 24, y - 24, 708, 510);
        if (noticeTime > 0) {
            Menu.panel(x, y - 78, 660, 48);
            Menu.center(notice, x + 330, y - 64, 15, Menu.GOLD);
        }
        Menu.icon("crown", x, y, 32, Menu.GOLD);
        Menu.text(
                state == GameState.VICTORY ? "THE VALLEY SEES DAWN" : "CONGRATULATIONS, WARDEN!",
                x + 48,
                y,
                30,
                Menu.GOLD);
        Menu.text(chapter().title(), x, y + 60, 25, Menu.TEXT);
        Menu.wrapped(chapter().aftermath(), x, y + 107, 640, 20, Menu.TEXT);
        if (state != GameState.VICTORY) {
            Campaign.Chapter next = Campaign.CHAPTERS[Math.min(3, chapterIndex() + 1)];
            Menu.text(
                    "NEXT / "
                            + next.title()
                            + (next.bossOnly() ? " / BOSS DUEL" : " / " + next.waves() + " WAVES"),
                    x,
                    y + 205,
                    21,
                    Menu.TEAL);
            Menu.wrapped(next.story(), x, y + 244, 640, 18, Menu.MUTED);
        } else {
            Menu.text("THE LONG NIGHT IS OVER", x, y + 205, 24, Menu.GOLD);
            Menu.text(
                    progress.kills()
                            + " enemies defeated / "
                            + progress.crystals()
                            + " crystals carried home",
                    x,
                    y + 245,
                    18,
                    Menu.TEAL);
            Menu.text(
                    "Your watch is complete. The valley remembers its warden.",
                    x,
                    y + 276,
                    16,
                    Menu.TEXT);
        }
        Menu.text(
                chapter().bossOnly()
                        ? "BOSS / +120 CRYSTALS / +300 XP"
                        : "CHAPTER COMPLETE / The ember burns brighter.",
                x,
                y + 310,
                17,
                Menu.TEAL);
        Menu.text(
                "CHAPTER FEATS / "
                        + progress.chapterFeats().completed(chapterIndex())
                        + " OF 3 / +"
                        + progress.chapterFeats().awarded(chapterIndex())
                        + " BONUS CRYSTALS",
                x,
                y + 332,
                12,
                Menu.GOLD);
        if (Menu.button(
                state == GameState.VICTORY ? "MAIN MENU [ENTER]" : "CONTINUE [ENTER]",
                x,
                y + 350,
                380,
                true)) continueCampaign();
        if (state != GameState.VICTORY && Menu.button("INVENTORY", x + 400, y + 350, 240, true))
            inventory = true;
        Menu.text(
                playtest
                        ? "Your playtest has been saved separately."
                        : "Your campaign progress has been saved.",
                x,
                y + 468,
                12,
                Menu.MUTED);
        if (Menu.button("WORLD MAP / CHAPTERS", x, y + 416, 260, true)) openChapterMap();
        if (Menu.button("VISIT CITADEL", x + 280, y + 416, 170, true)) travelToCitadel();
        if (state != GameState.VICTORY
                && Menu.button("MAIN MENU [ESC]", x + 470, y + 416, 170, true)) {
            saveCurrent();
            state = GameState.MAIN_MENU;
        }
    }

    private void drawJournal() {
        dim();
        int x = (GetScreenWidth() - 760) / 2, y = (GetScreenHeight() - 610) / 2;
        Menu.panel(x - 20, y - 16, 800, 642);
        Menu.icon("book", x, y, 30, Menu.GOLD);
        Menu.text("CHRONICLE & WARDEN'S CODEX", x + 46, y, 26, Menu.TEXT);
        if (Menu.button("STORY & LORE", x, y + 48, 246, true)) journalTab = 0;
        if (Menu.button("WARDEN'S CODEX", x + 256, y + 48, 246, true)) journalTab = 1;
        if (Menu.button("CHAPTER FEATS", x + 512, y + 48, 248, true)) {
            journalTab = 2;
            featsChapter = chapterIndex();
        }
        DrawRectangle(x + journalTab * 256, y + 94, 246, 2, Menu.GOLD);
        if (journalTab == 2) drawFeatsPage(x, y);
        else if (journalTab == 1) drawCodexPage(x, y);
        else drawStoryPage(x, y);
        if (Menu.button("RETURN [ENTER / ESC / J]", x, y + 546, 360, true)) state = journalReturn;
    }

    private void drawStoryPage(int x, int y) {
        Menu.panel(x, y + 104, 760, 68);
        Menu.text("THE PRIMORDIAL BEACON & THE HOLLOW FROST", x + 16, y + 112, 15, Menu.GOLD);
        Menu.wrapped(
                "When the Celestial Sun-Spires fell a century ago, eternal winter swallowed the"
                        + " realm. The Primordial Beacon holds the valley's last living solar forge"
                        + " ember. If the beacon is extinguished, humanity freezes forever. Awaken"
                        + " its solar ley-lines, hold the perimeter, and march north to destroy the"
                        + " Hollow King.",
                x + 16,
                y + 132,
                728,
                12,
                Menu.TEXT);
        for (int i = 0; i < Campaign.CHAPTERS.length; i++) {
            Campaign.Chapter c = Campaign.CHAPTERS[i];
            int top = y + 178 + i * 86;
            boolean unlocked = i <= Math.max(progress.unlockedChapter(), chapterIndex());
            boolean cleared = i < progress.unlockedChapter();
            Menu.panel(x, top, 760, 80);
            Menu.text(
                    (i + 1) + " / " + c.title(),
                    x + 16,
                    top + 8,
                    16,
                    unlocked ? Menu.GOLD : Menu.MUTED);
            Menu.text(
                    cleared ? "CLEARED" : unlocked ? "ACTIVE" : "LOCKED",
                    x + 400,
                    top + 10,
                    12,
                    cleared ? Menu.TEAL : unlocked ? Menu.GOLD : Menu.MUTED);
            Menu.text(
                    c.bossOnly() ? "BOSS DUEL" : c.waves() + " WAVES",
                    x + 630,
                    top + 10,
                    12,
                    Menu.MUTED);
            Menu.wrapped(
                    cleared ? c.aftermath() : c.story(),
                    x + 16,
                    top + 30,
                    728,
                    12,
                    unlocked ? (cleared ? Menu.color(180, 220, 255) : Menu.TEXT) : Menu.MUTED);
        }
    }

    private void drawCodexPage(int x, int y) {
        int card1Y = y + 104;
        Menu.panel(x, card1Y, 760, 100);
        Menu.icon("sword", x + 14, card1Y + 12, 22, Menu.GOLD);
        Menu.text(
                "COMBAT TIMING / 3-HIT COMBOS & CRITICAL CHARGE",
                x + 44,
                card1Y + 12,
                15,
                Menu.GOLD);
        Menu.wrapped(
                "- 3-HIT COMBO CHAIN: Light strikes [Space / Left Click] chain into 3 attacks. The"
                    + " 3rd finisher deals +40% stagger and knockback.\n"
                    + "- CRITICAL CHARGE RELEASE: Hold Heavy [Q / Right Click]. Watch the cast bar"
                    + " fill. Release during the GOLD BRACKETS (0.75s-1.05s) for a guaranteed +75%"
                    + " critical hit with armor-piercing bonus!\n"
                    + "- PARRY & RIPOSTE: Press Parry [F] right before an incoming golden attack to"
                    + " deflect 100% damage, stun the attacker, and ready a devastating"
                    + " counter-riposte.",
                x + 16, card1Y + 36, 728, 12, Menu.TEXT);

        int card2Y = y + 212;
        Menu.panel(x, card2Y, 760, 100);
        Menu.icon("shield", x + 14, card2Y + 12, 22, Menu.TEAL);
        Menu.text("TACTICS / SPELL INTERRUPTS & MOBILITY", x + 44, card2Y + 12, 15, Menu.TEAL);
        Menu.wrapped(
                "- SPELL INTERRUPTS: Shamans and frost casters show an arched cast bar over their"
                    + " head. Hit them during their cast to INTERRUPT and lock out their magic for"
                    + " 4.5 seconds!\n"
                    + "- MOBILITY: Lunge [R] dashes forward to close distance. Dodge [Ctrl] and"
                    + " Sprint [Shift] consume stamina to evade heavy boss hazards.\n"
                    + "- TARGET LOCK: Press Focus [T] or Middle Click to lock your combat camera"
                    + " and aim toward high-threat enemies.",
                x + 16,
                card2Y + 36,
                728,
                12,
                Menu.TEXT);

        int card3Y = y + 320;
        Menu.panel(x, card3Y, 760, 100);
        Menu.icon("potion", x + 14, card3Y + 12, 22, Menu.GOLD);
        Menu.text("PROGRESSION / XP LEVELING VS CRYSTAL FORGE", x + 44, card3Y + 12, 15, Menu.GOLD);
        Menu.wrapped(
                "- COMBAT XP & LEVELING: Slaying enemies and surviving waves earns XP. Leveling up"
                        + " fully restores Health & Stamina, and grants 1 Attribute Point.\n"
                        + "- ATTRIBUTE TRAINING: Spend Attribute Points at The Workshop [E] on"
                        + " Vitality (+Max HP), Power (+Dmg), Endurance (+Stamina), Recovery"
                        + " (+Regen), Agility (+Speed).\n"
                        + "- FORGE & LEVEL REQUIREMENTS: Crystals craft weapons and armor. Higher"
                        + " tiers require Warden Levels: Tier 1 (Lv 2), Tier 2 (Lv 4), Tier 3 (Lv"
                        + " 6).",
                x + 16,
                card3Y + 36,
                728,
                12,
                Menu.TEXT);

        int card4Y = y + 428;
        Menu.panel(x, card4Y, 760, 100);
        Menu.icon("crown", x + 14, card4Y + 12, 22, Menu.TEAL);
        Menu.text(
                "THE SANCTUARY / PRIMORDIAL BEACON FORTIFICATIONS",
                x + 44,
                card4Y + 12,
                15,
                Menu.TEAL);
        Menu.wrapped(
                "- BEACON INTEGRITY: Strengthen the beacon's max health in the Workshop to"
                    + " withstand heavy raids.\n"
                    + "- LIGHT SPIRE BATTERY: Upgrades the automated radiant turret to blast foes"
                    + " with increasing fire rate and holy damage.\n"
                    + "- WARMTH HEARTH & SOLAR NOVA: Enhances passive healing while standing near"
                    + " the beacon, and unlocks an emergency 35% HP knockback pulse that stuns"
                    + " surrounding hordes.",
                x + 16, card4Y + 36, 728, 12, Menu.TEXT);
    }

    private void drawFeatsPage(int x, int y) {
        int chapter = featsChapter;
        ChapterFeats feats = progress.chapterFeats();
        for (int i = 0; i < 3; i++) {
            int top = y + 108 + i * 122;
            boolean complete = feats.complete(chapter, i), paid = feats.paid(chapter, i);
            Menu.panel(x, top, 760, 112);
            Menu.icon(
                    complete ? "crown" : "sword",
                    x + 16,
                    top + 15,
                    26,
                    complete ? Menu.GOLD : Menu.MUTED);
            Menu.text(
                    feats.title(chapter, i),
                    x + 58,
                    top + 16,
                    21,
                    complete ? Menu.GOLD : Menu.TEXT);
            Menu.text(
                    paid ? "AWARDED +15" : complete ? "READY / +15 ON CLEAR" : "15 CRYSTALS",
                    x + 520,
                    top + 20,
                    14,
                    paid ? Menu.TEAL : Menu.GOLD);
            Menu.text(feats.description(chapter, i), x + 20, top + 52, 16, Menu.TEXT);
            DrawRectangle(x + 20, top + 83, 650, 8, Menu.INK);
            int fill = 650 * feats.progress(chapter, i) / feats.goal(chapter, i);
            if (fill > 0)
                DrawRectangle(x + 20, top + 83, fill, 8, complete ? Menu.GOLD : Menu.TEAL);
            Menu.text(
                    feats.progress(chapter, i) + " / " + feats.goal(chapter, i),
                    x + 688,
                    top + 79,
                    16,
                    Menu.TEXT);
        }
        Menu.text(
                "CHAPTER "
                        + (chapter + 1)
                        + " / "
                        + Campaign.CHAPTERS[chapter].title()
                        + " / UP TO 45 BONUS CRYSTALS",
                x + 12,
                y + 492,
                15,
                Menu.GOLD);
        Menu.text(
                "Optional goals. Rewards arrive when this chapter is cleared.",
                x + 12,
                y + 515,
                14,
                Menu.MUTED);
        Menu.text("VIEW " + (chapter + 1) + " OF 4", x + 440, y + 554, 14, Menu.MUTED);
        if (Menu.button("<", x + 640, y + 540, 52, chapter > 0)) featsChapter--;
        if (Menu.button(">", x + 704, y + 540, 52, chapter < 3)) featsChapter++;
    }

    private void requestNewGame() {
        playtest = false;
        nameDraft = settings.playerName.equals("Warden") ? "" : settings.playerName;
        menuMessage = "";
        state = GameState.LOGIN;
        while (GetCharPressed() != 0) {}
    }

    private void confirmName() {
        try {
            playerSaves.newPlayer(savePath, checkpointPath);
        } catch (IOException e) {
            menuMessage = "Could not preserve the previous player. Please try again.";
            return;
        }
        settings.playerName = Settings.cleanName(nameDraft);
        settings.difficulty = selectedDifficulty;
        settings.tutorialSeen = false;
        persistSettings();
        start();
        travelToCitadel();
        tutorial = new Tutorial();
        storyPage = 0;
        storyOpening = true;
        saveCurrent();
    }

    void startPlaytest() {
        playtest = true;
        start();
    }

    boolean isPlaytest() {
        return playtest;
    }

    void openPlaytestSetup() {
        playtestReturn = state;
        state = GameState.PLAYTEST_SETUP;
    }

    private void drawPlaytestSetup() {
        dim();
        int x = (GetScreenWidth() - 800) / 2, y = (GetScreenHeight() - 600) / 2;
        Menu.panel(x, y, 800, 600);
        Menu.text("PLAYTEST / EVERYTHING UNLOCKED", x + 24, y + 20, 30, Menu.GOLD);
        Menu.text(
                "100,000 crystals / all gear / 5 potions / separate save",
                x + 24,
                y + 69,
                18,
                Menu.TEAL);
        Menu.text("CLASS", x + 24, y + 93, 12, Menu.MUTED);
        for (HeroClass hero : HeroClass.values())
            if (Menu.button(
                    (selectedClass == hero ? "> " : "") + hero.label,
                    x + 24 + hero.ordinal() * 256,
                    y + 112,
                    240,
                    true)) selectedClass = hero;
        Menu.text("DIFFICULTY", x + 24, y + 181, 12, Menu.MUTED);
        for (Difficulty d : Difficulty.values())
            if (Menu.button(
                    (selectedDifficulty == d ? "> " : "") + d.label,
                    x + 24 + d.ordinal() * 194,
                    y + 202,
                    176,
                    true)) selectedDifficulty = d;
        Menu.text("JUMP TO CHAPTER", x + 24, y + 270, 12, Menu.MUTED);
        for (int i = 0; i < 4; i++)
            if (Menu.button(
                    (playtestChapter == i ? "> " : "")
                            + (i + 1)
                            + " / "
                            + Campaign.CHAPTERS[i].title(),
                    x + 24 + (i % 2) * 388,
                    y + 292 + (i / 2) * 56,
                    364,
                    true)) playtestChapter = i;
        if (Menu.button(
                maxPlaytestTraining ? "TRAINING / MAXED" : "TRAINING / STARTER STATS",
                x + 24,
                y + 420,
                364,
                true)) maxPlaytestTraining = !maxPlaytestTraining;
        if (Menu.button("TIME / " + TIME_LABELS[playtestTime], x + 412, y + 420, 364, true))
            playtestTime = (playtestTime + 1) % 4;
        if (Menu.button("START / RESET PLAYTEST [ENTER]", x + 24, y + 490, 364, true))
            startPlaytest();
        if (Menu.button("BACK [ESC]", x + 412, y + 490, 176, true)) state = playtestReturn;
        if (Menu.button("LOAD PLAYTEST", x + 608, y + 490, 168, Files.exists(playtestSavePath)))
            loadFrom(playtestSavePath, true);
        Menu.text(
                "Tab changes equipment. Pause > Playtest Tools switches class/chapter or resets the"
                        + " kit.",
                x + 24,
                y + 556,
                15,
                Menu.MUTED);
    }

    private void drawNameEntry() {
        dim();
        int x = (GetScreenWidth() - 940) / 2, y = (GetScreenHeight() - 600) / 2;
        // Separate character preview from the form instead of enclosing everything in one box.
        Menu.panel(x, y, 600, 600);
        Menu.panel(x + 612, y, 328, 600);
        DrawRectangle(x + 24, y + 126, 552, 2, Fade(Menu.GOLD, .4f));
        Color accent =
                selectedClass == HeroClass.WARRIOR
                        ? Menu.GOLD
                        : selectedClass == HeroClass.ARCHER ? Menu.TEAL : Menu.color(182, 149, 237);
        heroPortrait.draw(x + 624, y + 65, 292, 408);
        Menu.text("NEW ADVENTURE", x + 24, y + 22, 16, accent);
        Menu.text("CREATE YOUR WARDEN", x + 24, y + 53, 32, Menu.TEXT);
        Menu.text("Choose a name, a class, and your challenge.", x + 24, y + 102, 18, Menu.MUTED);
        Menu.panel(x + 24, y + 137, 560, 58);
        Menu.text(
                nameDraft.isEmpty()
                        ? "Enter your name"
                        : nameDraft + ((int) (GetTime() * 2) % 2 == 0 ? "_" : ""),
                x + 40,
                y + 153,
                24,
                nameDraft.isEmpty() ? Menu.MUTED : Menu.TEXT);
        for (HeroClass hero : HeroClass.values())
            if (Menu.button(
                    (selectedClass == hero ? "> " : "") + hero.label,
                    x + 24 + hero.ordinal() * 190,
                    y + 216,
                    180,
                    true)) selectedClass = hero;
        Menu.text(
                switch (selectedClass) {
                    case WARRIOR -> "Sword and shield. Block, counter, and break their guard.";
                    case ARCHER -> "Bows and crossbows. Dodge, aim, and strike from range.";
                    case MAGE -> "Arcane spells. Spend mana to control the battlefield.";
                },
                x + 24,
                y + 278,
                15,
                accent);
        // Underlines remain visible without hover, making both selections unambiguous.
        DrawRectangle(x + 28 + selectedClass.ordinal() * 190, y + 260, 172, 3, accent);
        Menu.text("CHOOSE YOUR CHALLENGE", x + 24, y + 326, 13, Menu.GOLD);
        for (Difficulty choice : Difficulty.values())
            if (Menu.button(
                    (choice == selectedDifficulty ? "> " : "") + choice.label,
                    x + 24 + choice.ordinal() * 142,
                    y + 350,
                    134,
                    true)) selectedDifficulty = choice;
        DrawRectangle(x + 28 + selectedDifficulty.ordinal() * 142, y + 394, 126, 3, Menu.GOLD);
        Menu.wrapped(selectedDifficulty.description, x + 24, y + 411, 548, 17, Menu.MUTED);
        if (Menu.button("BEGIN ADVENTURE [ENTER]", x + 24, y + 482, 286, true)) confirmName();
        if (Menu.button("CLEAR NAME", x + 326, y + 482, 118, true)) nameDraft = "";
        if (Menu.button("BACK", x + 462, y + 482, 122, true)) state = GameState.MAIN_MENU;
        Menu.center(selectedClass.label.toUpperCase(), x + 763, y + 489, 28, accent);
        Menu.center(
                selectedClass.health
                        + " HEALTH / "
                        + (selectedClass == HeroClass.WARRIOR
                                ? "CLOSE COMBAT"
                                : selectedClass == HeroClass.ARCHER ? "PRECISION" : "ARCANE POWER"),
                x + 763,
                y + 530,
                13,
                Menu.MUTED);
        Menu.text("Your adventure begins with guided training.", x + 24, y + 556, 17, accent);
        for (int i = 0; i < 9; i++) {
            float rise = ((float) GetTime() * .18f + i * .13f) % 1;
            DrawCircle(
                    x + 630 + i * 29 + (int) (Math.sin(GetTime() + i) * 5),
                    y + 460 - (int) (rise * 365),
                    1.5f,
                    Fade(accent, (1 - rise) * .6f));
        }
    }

    private void openSettings() {
        settingsReturn = state;
        state = GameState.SETTING_MENU;
        menuMessage = "";
        rebinding = null;
    }

    private void closeSettings() {
        persistSettings();
        rebinding = null;
        state = settingsReturn;
    }

    private void applySettings() {
        audio.configure(settings.volume, settings.muted);
        sceneManager.getOpenScene().getCameraController().setSensitivity(settings.sensitivity);
        renderer.quality(settings.highQuality);
        if (IsWindowFullscreen() != settings.fullscreen) ToggleFullscreen();
    }

    private void persistSettings() {
        try {
            settings.save(settingsPath);
        } catch (IOException e) {
            menuMessage = "Could not save settings. Check folder permissions.";
            System.err.println(e.getMessage());
        }
    }

    private void drawSettings() {
        dim();
        int x = (GetScreenWidth() - 820) / 2, y = (GetScreenHeight() - 650) / 2;
        Menu.panel(x - 20, y - 12, 860, 674);
        Menu.text("SETTINGS", x, y, 34, Menu.TEXT);
        Menu.text(
                "Master audio " + Math.round(settings.volume * 100) + "%",
                x,
                y + 68,
                20,
                Menu.TEXT);
        if (Menu.button("-", x + 240, y + 54, 50, true)) {
            settings.volume = Math.max(0, settings.volume - .1f);
            applySettings();
        }
        if (Menu.button("+", x + 300, y + 54, 50, true)) {
            settings.volume = Math.min(1, settings.volume + .1f);
            applySettings();
            audio.play("hit");
        }
        if (Menu.button(settings.muted ? "UNMUTE" : "MUTE", x + 430, y + 54, 180, true)) {
            settings.muted = !settings.muted;
            applySettings();
        }
        Menu.text(
                "Camera speed " + Math.round(settings.sensitivity * 100),
                x,
                y + 122,
                20,
                Menu.TEXT);
        if (Menu.button("-", x + 240, y + 108, 50, true)) {
            settings.sensitivity = Math.max(.05f, settings.sensitivity - .05f);
            applySettings();
        }
        if (Menu.button("+", x + 300, y + 108, 50, true)) {
            settings.sensitivity = Math.min(.5f, settings.sensitivity + .05f);
            applySettings();
        }
        if (Menu.button(
                settings.fullscreen ? "WINDOWED" : "FULLSCREEN", x + 430, y + 108, 260, true)) {
            settings.fullscreen = !settings.fullscreen;
            applySettings();
        }
        Menu.text(
                "Click an action, then press a key. ESC cancels. Mouse buttons stay fixed.",
                x,
                y + 174,
                16,
                Menu.MUTED);
        Controls.Action[] actions = Controls.Action.values();
        for (int i = 0; i < actions.length; i++) {
            Controls.Action action = actions[i];
            if (Menu.button(
                    action.label
                            + " / "
                            + (rebinding == action ? "PRESS KEY" : settings.controls.label(action)),
                    x + (i % 3) * 280,
                    y + 210 + (i / 3) * 46,
                    260,
                    rebinding == null)) {
                rebinding = action;
                menuMessage = "Press a key for " + action.label + " (ESC cancels).";
                while (GetKeyPressed() != 0) {}
            }
        }
        if (Menu.button(
                settings.highQuality
                        ? "GRAPHICS / HIGH - 2K SHADOWS + BLOOM"
                        : "GRAPHICS / BALANCED - 1K SHADOWS",
                x,
                y + 492,
                820,
                rebinding == null)) {
            settings.highQuality = !settings.highQuality;
            applySettings();
        }
        Menu.text(menuMessage, x, y + 542, 15, Menu.GOLD);
        if (Menu.button("BACK / SAVE", x, y + 580, 260, rebinding == null)) closeSettings();
        if (Menu.button("RESTORE DEFAULTS", x + 280, y + 580, 280, rebinding == null)) {
            settings.controls.reset();
            settings.volume = .6f;
            settings.sensitivity = .15f;
            settings.muted = false;
            settings.fullscreen = false;
            settings.highQuality = true;
            applySettings();
            menuMessage = "Defaults restored.";
        }
    }

    private void writeCheckpoint() {
        if (tutorial != null) return;
        try {
            SaveGame.write(
                    playtest ? playtestCheckpointPath : checkpointPath,
                    new SaveGame.Snapshot(
                            player.getName(),
                            player.getPosition().x(),
                            player.getPosition().z(),
                            player.getRotation().y(),
                            progress,
                            director.snapshot(),
                            java.util.List.copyOf(enemies),
                            Arena.HIGHLANDS,
                            player.discipline().stamina(),
                            dayNight.phase()));
        } catch (IOException e) {
            notifyPlayer("Chapter checkpoint could not be written.");
            System.err.println(e.getMessage());
        }
    }

    void retryChapter() {
        Path path = playtest ? playtestCheckpointPath : checkpointPath;
        if (!Files.exists(path)) {
            start();
            return;
        }
        if (loadFrom(path, playtest)) {
            state = GameState.PLAYING;
            saveCurrent();
            notifyPlayer("CHAPTER RETRY / Entry equipment and upgrades restored");
        }
    }

    boolean saveCurrent() {
        if (tutorial != null && state != GameState.CITADEL) {
            notifyPlayer("Practice stays separate / Your campaign save is safe.");
            return false;
        }
        if (progress.defeated()) return false;
        try {
            SaveGame.write(
                    playtest ? playtestSavePath : savePath,
                    new SaveGame.Snapshot(
                            player.getName(),
                            player.getPosition().x(),
                            player.getPosition().z(),
                            player.getRotation().y(),
                            progress,
                            director.snapshot(),
                            enemies.stream().filter(e -> e.health > 0).toList(),
                            Arena.HIGHLANDS,
                            player.discipline().stamina(),
                            dayNight.phase()));
            if (!playtest) playerSaves.save(SaveGame.read(savePath), checkpointPath);
            menuMessage = playtest ? "Playtest saved separately." : "Game saved.";
            notifyPlayer(menuMessage);
            return true;
        } catch (IOException e) {
            menuMessage = "Save failed. Previous save was kept.";
            notifyPlayer(menuMessage);
            System.err.println(e.getMessage());
            return false;
        }
    }

    private boolean loadActive() {
        return loadFrom(playtest ? playtestSavePath : savePath, playtest);
    }

    boolean loadCurrent() {
        return loadFrom(savePath, false);
    }

    private boolean loadFrom(Path path, boolean sandbox) {
        try {
            SaveGame.Snapshot saved = SaveGame.read(path);
            restoreSnapshot(saved, sandbox);
            menuMessage = "Game loaded. Press Enter to resume.";
            return true;
        } catch (IOException e) {
            menuMessage = "Could not load game. Current defense was kept.";
            notifyPlayer(menuMessage);
            System.err.println(e.getMessage());
            return false;
        }
    }

    private void restoreSnapshot(SaveGame.Snapshot saved, boolean sandbox) {
        playtest = sandbox;
        progress = saved.progression();
        enemyFocus.clear();
        // Older saves predate the travel unlock field; their current chapter remains accessible.
        int savedChapter =
                Campaign.index(
                        saved.wave().total() == 0 ? saved.wave().wave() + 1 : saved.wave().wave());
        progress.unlockChapter(savedChapter);
        if (saved.enemies().isEmpty()
                && saved.wave().remaining() == 0
                && saved.wave().total() > 0
                && Campaign.boundary(saved.wave().wave())) progress.unlockChapter(savedChapter + 1);
        selectedDifficulty = progress.difficulty();
        director.difficulty(progress.difficulty());
        director.restore(saved.wave());
        fallen.clear();
        outcomeTime = 0;
        enemies.clear();
        enemies.addAll(saved.enemies());
        projectiles.clear();
        heroShots.clear();
        inventory = false;
        selectedClass = progress.heroClass();
        if (playtest) {
            playtestChapter = director.chapterIndex();
            maxPlaytestTraining = progress.level(Progression.Upgrade.VITALITY) == 10;
        }
        applyHeroModel();
        player.setName(saved.name());
        settings.playerName = saved.name();
        player.getPosition().x(saved.x()).z(saved.z());
        player.getRotation().y(saved.facing());
        applyProgression();
        player.resetCombat();
        player.discipline().stamina(saved.stamina());
        dayNight.phase(saved.dayPhase());
        lastHeroX = saved.x();
        lastHeroZ = saved.z();
        heavyCharge = 0;
        bossHazards.clear();
        feedback.clear();
        vfx.clear();
        wave = director.wave();
        nextWave = director.rest();
        workshop = false;
        combo = 0;
        comboWindow = inputBuffer = attackElapsed = hurtFlash = slashTime = noticeTime = 0;
        attackCooldown = .3f;
        impactPending = false;
        CameraController cam = sceneManager.getOpenScene().getCameraController();
        if (cam != null) {
            cam.setYaw(0.0f);
            cam.setPitch(24.0f);
            cam.setDistance(17.0f);
            cam.update(0);
        }
        tutorial = null;
        preparedStep = null;
        state =
                director.levelCleared(enemies.size())
                        ? (wave >= 10 ? GameState.VICTORY : GameState.LEVEL_COMPLETE)
                        : GameState.PAUSED;
        pauseReturn = GameState.PLAYING;
        if (state == GameState.LEVEL_COMPLETE || state == GameState.VICTORY) outcomeTime = 3;

        hudCharging = false;
        guardianCooldown = guardianFlash = 0;
        mastery.reset();
        hitPause = hitMarker = 0;
    }

    WeaponTrail weaponTrail() {
        return vfx.blade();
    }

    Combat.Attack currentAttack() {
        return activeAttack;
    }

    java.util.List<HeroShot> getHeroShots() {
        return java.util.List.copyOf(heroShots);
    }

    java.util.List<BossHazard> getBossHazards() {
        return java.util.Collections.unmodifiableList(bossHazards);
    }

    boolean weaponMaterialsValid() {
        return assets.weaponMaterialsValid();
    }

    boolean shadowAtlasValid() {
        return renderer.shadowAtlasValid();
    }

    java.util.List<Enemy> getEnemies() {
        return java.util.Collections.unmodifiableList(enemies);
    }

    Progression getProgression() {
        return progress;
    }

    GameAudio audio() {
        return audio;
    }

    boolean isWorkshopOpen() {
        return workshop;
    }

    void openWorkshop() {
        workshop = true;
        workshopScreen.setMentor(progress != null ? progress.heroClass() : null);
    }

    boolean isInventoryOpen() {
        return inventory;
    }

    int fallenCount() {
        return fallen.size();
    }

    public boolean shouldQuit() {
        return quitRequested;
    }

    public void dispose() {
        persistSettings();
        if (state == GameState.CHAPTER_SELECT
                || state == GameState.RELIC_CHOICE
                || state == GameState.PLAYING
                || state == GameState.CITADEL
                || state == GameState.PAUSED
                || state == GameState.LEVEL_COMPLETE
                || state == GameState.VICTORY
                || state == GameState.JOURNAL && journalReturn == GameState.PAUSED
                || state == GameState.SETTING_MENU && settingsReturn == GameState.PAUSED
                || state == GameState.PLAYTEST_SETUP && playtestReturn == GameState.PAUSED)
            saveCurrent();
        citadelHub.close();
        audio.close();
        vfx.close();
        assets.close();
        renderer.close();
        equipmentScreen.close();
        heroPortrait.close();
        player.dispose();
        Menu.close();
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }

    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public Player getPlayer() {
        return player;
    }

    public void openInventory() {
        inventory = true;
    }

    public void closeInventory() {
        inventory = false;
    }

    public EquipmentScreen getEquipmentScreen() {
        return equipmentScreen;
    }

    public void setPlayer(Player player) {
        this.player = player;
        Scene scene = sceneManager.getOpenScene();
        scene.addEntity(player);
        scene.setMainCamera(player);
        player.setCameraController(scene.getCameraController());
    }
}
