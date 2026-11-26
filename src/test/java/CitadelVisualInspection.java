import static com.raylib.Raylib.*;

import Scene.CameraController;

import java.nio.file.Files;
import java.nio.file.Path;

public class CitadelVisualInspection {

    public static void main(String[] args) throws Exception {
        Path profile = Files.createTempDirectory(Path.of("build"), "citadel-inspect-");
        System.setProperty("outpost.dataDir", profile.toAbsolutePath().toString());
        Settings initialSettings = new Settings();
        initialSettings.playerName = "VisualInspector";
        initialSettings.save(profile.resolve("settings.properties"));

        Path outDir = Path.of("build/citadel_views");
        Files.createDirectories(outDir);

        SetConfigFlags(FLAG_WINDOW_RESIZABLE | FLAG_WINDOW_HIDDEN);
        InitWindow(1280, 800, "Citadel Visual Inspection");
        SetTargetFPS(60);

        Game game = null;
        try {
            game = new Game();
            // Capture the title at both normal and minimum supported window sizes.
            for (int[] size : new int[][] {{1280, 800}, {1000, 680}}) {
                SetWindowSize(size[0], size[1]);
                MainMenuScreen titleScreen = new MainMenuScreen();
                for (int frame = 0; frame < 8; frame++) {
                    BeginDrawing();
                    ClearBackground(Menu.INK);
                    Camera2D overlay =
                            new Camera2D().zoom((float) GetRenderWidth() / GetScreenWidth());
                    BeginMode2D(overlay);
                    titleScreen.draw(
                            size[0] == 1280 ? "Warden of the Northern Kingdom" : "Warden",
                            size[0] == 1280,
                            "");
                    EndMode2D();
                    EndDrawing();
                }
                Image title = LoadImageFromScreen();
                try {
                    ExportImage(title, outDir.resolve("title-" + size[0] + ".png").toString());
                } finally {
                    UnloadImage(title);
                }
            }
            SetWindowSize(1280, 800);
            MainMenuScreen cycleTitle = new MainMenuScreen();
            for (double phase : new double[] {.02, .25, .48, .75}) {
                cycleTitle.sky().phase(phase);
                for (int frame = 0; frame < 8; frame++) {
                    BeginDrawing();
                    ClearBackground(Menu.INK);
                    BeginMode2D(new Camera2D().zoom((float) GetRenderWidth() / GetScreenWidth()));
                    cycleTitle.draw("Warden", true, "");
                    EndMode2D();
                    EndDrawing();
                }
                Image skyView = LoadImageFromScreen();
                try {
                    ExportImage(
                            skyView, outDir.resolve("title-cycle-" + phase + ".png").toString());
                } finally {
                    UnloadImage(skyView);
                }
            }

            // Exercise actual Raylib mouse input rather than only the clock's projection math.
            for (int body = 0; body < 2; body++) {
                cycleTitle.sky().phase(body == 0 ? .25 : .75);
                TitleSky.Body orb = cycleTitle.sky().body(body, 1280, 800);
                titlePointer(Math.round(orb.x()), Math.round(orb.y()));
                try (AutomationEvent down = new AutomationEvent().type(6)) {
                    down.params(0, MOUSE_BUTTON_LEFT);
                    PlayAutomationEvent(down);
                }
                titleFrame(cycleTitle);
                if (!cycleTitle.sky().dragging())
                    throw new AssertionError("Cannot grab sky body " + body);
                titlePointer(Math.round(1280 * .76f), Math.round(800 * .76f));
                titleFrame(cycleTitle);
                if (Math.abs(cycleTitle.sky().daylight() - (body == 0 ? 0 : 1)) > .01)
                    throw new AssertionError("Sky drag did not switch lighting");
                try (AutomationEvent up = new AutomationEvent().type(5)) {
                    up.params(0, MOUSE_BUTTON_LEFT);
                    PlayAutomationEvent(up);
                }
                titleFrame(cycleTitle);
                if (cycleTitle.sky().dragging())
                    throw new AssertionError("Sky body still held after release");
            }
            System.out.println(
                    "PASS: sun and moon mouse dragging changes lighting and releases correctly");
            if (java.util.Arrays.asList(args).contains("--title-only")) return;

            // Repeated directional impacts stay pooled; recalling clears the prior encounter.
            for (int burst = 0; burst < 100; burst++)
                game.combatEffects().strike(0, 4, -1, 3, Menu.GOLD, true);
            if (game.combatEffects().particleCount() > 240
                    || game.combatEffects().particleCount() == 0)
                throw new AssertionError("Directional impact particle budget invalid");
            game.travelToCitadel();
            if (game.combatEffects().particleCount() != 0)
                throw new AssertionError("Recall retained combat effects");
            System.out.println(
                    "PASS: directional impact pool stays bounded and recall clears effects");
            // Unlock rescued survivors so they appear in town.
            game.getProgression().rescueSurvivor();
            game.getProgression().rescueSurvivor();
            game.getProgression().rescueSurvivor();
            game.getProgression().rescueSurvivor();

            game.getProgression().rescueSurvivor();
            game.getProgression().rescueSurvivor();
            // Run warm-up frames
            renderFrames(game, 30);

            game.getProgression().grantStarterWeapon();
            int radarX = GetScreenWidth() - 184;
            inventoryClick(
                    game,
                    radarX + 80 + Math.round(CitadelNpc.CARAVAN_TRADER.x() * .95f),
                    82 + 88 + Math.round((CitadelNpc.CARAVAN_TRADER.z() + 7) * .95f),
                    MOUSE_BUTTON_LEFT);
            if (!CityMinimap.destination(game.getProgression(), game.getCitadelHub())
                    .title()
                    .equals(CitadelNpc.CARAVAN_TRADER.npcName()))
                throw new AssertionError("Radar click did not track merchant");
            capture(game, outDir.resolve("city-tracked-merchant.png").toString());
            inventoryClick(game, radarX + 80, 82 + 88, MOUSE_BUTTON_RIGHT);
            if (CityMinimap.destination(game.getProgression(), game.getCitadelHub()).z()
                    != CitadelHub.PORTAL_Z)
                throw new AssertionError("Radar right-click did not clear tracking");

            // 1. Capture Spawn North Avenue (Default View)
            capture(game, outDir.resolve("01_spawn_north_avenue.png").toString());

            // 2. Capture Castle Overlook looking South down the boulevard
            CameraController cam = game.getSceneManager().getOpenScene().getCameraController();
            game.getPlayer().getPosition().x(0).y(0).z(-14f);
            game.getPlayer().getRotation().y(0f); // Face South
            if (cam != null) {
                cam.setYaw(180f); // Camera behind player looking South (+Z)
                cam.setPitch(20f);
                cam.setDistance(6.0f);
            }
            renderFrames(game, 20);
            capture(game, outDir.resolve("02_castle_overlook_south.png").toString());

            // 3. Capture Blacksmith District (Player stands near Goran at -7.5, 1.5)
            game.getPlayer().getPosition().x(-4.5f).y(0).z(1.5f);
            game.getPlayer().getRotation().y(-90f);
            if (cam != null) {
                cam.setYaw(90f); // Looking West toward Goran
                cam.setPitch(18f);
                cam.setDistance(5.2f);
            }
            renderFrames(game, 20);
            capture(game, outDir.resolve("03_blacksmith_district.png").toString());

            // 4. Capture Market Square (Player facing North through the bustling market plaza)
            game.getPlayer().getPosition().x(0f).y(0).z(22f);
            game.getPlayer().getRotation().y(180f);
            if (cam != null) {
                cam.setYaw(0f);
                cam.setPitch(20f);
                cam.setDistance(7.5f);
            }
            renderFrames(game, 20);
            capture(game, outDir.resolve("04_market_square.png").toString());

            game.getPlayer().getPosition().x(.5f).y(0).z(15.5f);
            if (cam != null) {
                cam.setYaw(90f);
                cam.setPitch(20f);
                cam.setDistance(5.5f);
            }
            renderFrames(game, 20);
            capture(game, outDir.resolve("torvald-market.png").toString());

            // 5. Capture Cathedral & Monastery District (Open courtyard framing Cathedral Facade &
            // Spire)
            game.getPlayer().getPosition().x(15.0f).y(0).z(-15.0f);
            game.getPlayer().getRotation().y(90f); // Facing East towards Cathedral
            if (cam != null) {
                cam.setYaw(270f); // Looking East across the open monastery courtyard
                cam.setPitch(18f);
                cam.setDistance(6.0f);
            }
            renderFrames(game, 20);
            capture(game, outDir.resolve("05_cathedral_district.png").toString());

            // 6. Capture Monumental Castle Keep (Looking North)
            game.getPlayer().getPosition().x(0f).y(0).z(-18f);
            game.getPlayer().getRotation().y(180f);
            if (cam != null) {
                cam.setYaw(0f);
                cam.setPitch(16f);
                cam.setDistance(11.0f);
            }
            renderFrames(game, 20);
            capture(game, outDir.resolve("06_castle_monumental.png").toString());

            // 7. Capture Commander Sean & Portal at South Wall Gate (Sean at (2.6, 29.5), Portal at
            // (0, 29.5))
            game.getProgression().grantStarterWeapon();
            game.getPlayer().getPosition().x(0.0f).y(0).z(27.2f);
            game.getPlayer().getRotation().y(180f);
            if (cam != null) {
                cam.setYaw(180f);
                cam.setPitch(16f);
                cam.setDistance(5.2f);
            }
            renderFrames(game, 20);
            capture(game, outDir.resolve("07_commander_portal.png").toString());

            // 8. Capture Arcanist Sanctum (Player talking with Lady Elyndra at 7.5, 1.5)
            game.getPlayer().getPosition().x(6.2f).y(0).z(0.0f);
            game.getPlayer().getRotation().y(135f); // Facing Southeast towards Elyndra
            if (cam != null) {
                cam.setYaw(285f); // Clear view of Elyndra, crystal beacon & Sanctum
                cam.setPitch(16f);
                cam.setDistance(3.8f);
            }
            renderFrames(game, 20);
            capture(game, outDir.resolve("08_arcanist_sanctum.png").toString());

            // 9. Capture Residential City Block (High-angle street overview of unique homes)
            game.getPlayer().getPosition().x(15.0f).y(0).z(14.0f);
            game.getPlayer().getRotation().y(0f); // Facing South down the residential street
            if (cam != null) {
                cam.setYaw(180f); // Looking South along the residential street block
                cam.setPitch(32f);
                cam.setDistance(8.5f);
            }
            renderFrames(game, 20);
            capture(game, outDir.resolve("09_residential_district.png").toString());

            // 10. Capture Diana the Huntress & Central Archery Range at (-7.5, -5.5)
            game.getPlayer().getPosition().x(-5.2f).y(0).z(-4.2f);
            game.getPlayer().getRotation().y(-135f); // Facing Northwest towards Diana
            if (cam != null) {
                cam.setYaw(135f);
                cam.setPitch(16f);
                cam.setDistance(4.5f);
            }
            renderFrames(game, 20);
            capture(game, outDir.resolve("10_archer_mentor_range.png").toString());

            // 11. Capture Equipment & Inventory Screen (Gear Grid with Warden Stats summary)
            game.openInventory();
            renderFrames(game, 15);
            capture(game, outDir.resolve("11_inventory_screen.png").toString());

            Progression gear = game.getProgression();
            for (int i = 0; i < 20; i++) gear.rewardEnemy(false);
            if (!gear.buyArmor(Entities.ArmorSlot.BOOTS, 1)
                    || !gear.buyArmor(Entities.ArmorSlot.HEAD, 1))
                throw new AssertionError("Inventory test funding failed");
            int ix = (GetScreenWidth() - 960) / 2, iy = (GetScreenHeight() - 640) / 2;
            inventoryClick(game, ix + 298 + 2 * 86 + 36, iy + 246, MOUSE_BUTTON_LEFT);
            inventoryClick(game, ix + 800, iy + 428, MOUSE_BUTTON_LEFT);
            if (gear.armorTier(Entities.ArmorSlot.BOOTS) != 1)
                throw new AssertionError("Detail equip did not equip boots");
            inventoryClick(game, ix + 193, iy + 430, MOUSE_BUTTON_RIGHT);
            if (gear.armorTier(Entities.ArmorSlot.BOOTS) != 0)
                throw new AssertionError("Slot right click did not remove boots");
            inventoryClick(game, ix + 600, iy + 179, MOUSE_BUTTON_LEFT);
            gear.damagePlayer(20);
            int potionsBefore = gear.potions();
            inventoryClick(game, ix + 800, iy + 428, MOUSE_BUTTON_LEFT);
            if (gear.potions() != potionsBefore - 1)
                throw new AssertionError("Supply action did not use a potion");
            inventoryClick(game, ix + 329, iy + 179, MOUSE_BUTTON_LEFT);
            capture(game, outDir.resolve("inventory-owned-gear.png").toString());
            inventoryClick(game, ix + 578, iy + 126, MOUSE_BUTTON_LEFT);
            inventoryClick(game, ix + 800, iy + 428, MOUSE_BUTTON_LEFT);
            if (gear.armorTier(Entities.ArmorSlot.HEAD) != 1)
                throw new AssertionError("Upgrade filter did not select and equip owned helmet");
            capture(game, outDir.resolve("inventory-upgrades.png").toString());
            inventoryClick(game, ix + 43, iy + 430, MOUSE_BUTTON_RIGHT);
            inventoryClick(game, ix + 578, iy + 126, MOUSE_BUTTON_LEFT);

            inventoryClick(game, ix + 460, iy + 595, MOUSE_BUTTON_LEFT);
            capture(game, outDir.resolve("inventory-all-gear.png").toString());

            // 12. Capture Detailed Stats Dossier Sheet (Vitals & Defense, Offense & Mobility)
            game.getEquipmentScreen().toggleStatsSheet();
            renderFrames(game, 15);
            capture(game, outDir.resolve("12_stats_dossier.png").toString());

            // 13. Capture South Front Wall & Mountain Forest Horizon (turned camera view)
            game.closeInventory();
            game.getPlayer().getPosition().x(0.0f).y(0).z(30.8f);
            game.getPlayer().getRotation().y(0f); // Facing South toward front gate wall
            if (cam != null) {
                cam.setYaw(0f); // Looking South across the front wall
                cam.setPitch(25f);
                cam.setDistance(12.0f);
            }
            renderFrames(game, 20);
            capture(game, outDir.resolve("13_front_wall_forest_panorama.png").toString());

            game.beginTutorial();
            game.tutorial().movement(4);
            renderFrames(game, 3);
            if (game.getEnemies().isEmpty()) throw new AssertionError("Practice partner missing");
            game.finishTutorial();
            renderFrames(game, 3);
            if (!game.getEnemies().isEmpty())
                throw new AssertionError("Practice partner survived tutorial");
            for (int page = 0; page < OpeningStory.PAGES.length; page++) {
                for (int frame = 0; frame < 5; frame++) {
                    BeginDrawing();
                    ClearBackground(Menu.INK);
                    Camera2D overlay =
                            new Camera2D().zoom((float) GetRenderWidth() / GetScreenWidth());
                    BeginMode2D(overlay);
                    OpeningStory.draw(page);
                    EndMode2D();
                    EndDrawing();
                }
                Image story = LoadImageFromScreen();
                ExportImage(story, outDir.resolve("story-" + page + ".png").toString());
                UnloadImage(story);
            }

            WorkshopScreen shop = new WorkshopScreen();
            shop.selectTab(2);
            for (int frame = 0; frame < 3; frame++) {
                BeginDrawing();
                ClearBackground(Menu.INK);
                BeginMode2D(new Camera2D().zoom((float) GetRenderWidth() / GetScreenWidth()));
                shop.draw(game.getProgression(), "ESC", game.audio());
                EndMode2D();
                EndDrawing();
            }
            Image merchant = LoadImageFromScreen();
            ExportImage(merchant, outDir.resolve("merchant-stock.png").toString());
            UnloadImage(merchant);
            shop.setMentor(HeroClass.WARRIOR);
            for (int frame = 0; frame < 3; frame++) {
                BeginDrawing();
                ClearBackground(Menu.INK);
                BeginMode2D(new Camera2D().zoom((float) GetRenderWidth() / GetScreenWidth()));
                shop.draw(game.getProgression(), "ESC", game.audio());
                EndMode2D();
                EndDrawing();
            }
            Image forge = LoadImageFromScreen();
            ExportImage(forge, outDir.resolve("individual-armor-forge.png").toString());
            UnloadImage(forge);
            Progression roster = new Progression();
            roster.rescueSurvivor();
            int ledgerX = (GetScreenWidth() - 900) / 2, ledgerY = (GetScreenHeight() - 590) / 2;
            for (int frame = 0; frame < 4; frame++) {
                titlePointer(ledgerX + (frame < 2 ? 70 : 660), ledgerY + (frame < 2 ? 145 : 550));
                try (AutomationEvent click = new AutomationEvent().type(frame % 2 == 0 ? 6 : 5)) {
                    click.params(0, MOUSE_BUTTON_LEFT);
                    PlayAutomationEvent(click);
                }
                BeginDrawing();
                ClearBackground(Menu.INK);
                BeginMode2D(new Camera2D().zoom((float) GetRenderWidth() / GetScreenWidth()));
                boolean closed = RescueLedgerScreen.draw(roster, game.getCitadelHub());
                EndMode2D();
                EndDrawing();
                if (frame == 1) {
                    Image ledger = LoadImageFromScreen();
                    ExportImage(ledger, outDir.resolve("rescue-ledger.png").toString());
                    UnloadImage(ledger);
                }
                if (frame == 2 && !closed)
                    throw new AssertionError("Visit did not close the ledger");
            }
            var visit = CityMinimap.destination(roster, game.getCitadelHub());
            if (!visit.title().equals(CitadelNpc.SURVIVOR_BRAM.npcName())
                    || visit.x() != game.getCitadelHub().npcX(CitadelNpc.SURVIVOR_BRAM)
                    || visit.z() != game.getCitadelHub().npcZ(CitadelNpc.SURVIVOR_BRAM))
                throw new AssertionError("Ledger did not mark Bram's current location");

            for (CitadelNpc npc : CitadelNpc.RESCUED_SURVIVORS) {
                game.getPlayer().getPosition().x(npc.x()).y(0).z(npc.z() + 2.5f);
                game.getPlayer().getRotation().y(180f);
                if (cam != null) {
                    cam.setYaw(180f);
                    cam.setPitch(12f);
                    cam.setDistance(3.8f);
                }
                renderFrames(game, 8);
                capture(
                        game,
                        outDir.resolve("citizen-" + npc.name().toLowerCase() + ".png").toString());
            }

            Progression escortKit = new Progression();
            escortKit.rescueSurvivor(); // Kaelen follows Bram in both the field and city roster.
            escortKit.fieldEvent().begin(2);
            SaveGame.write(
                    profile.resolve("save.properties"),
                    new SaveGame.Snapshot(
                            "Escort",
                            -10,
                            9,
                            0,
                            escortKit,
                            new WaveDirector.State(2, 0, 1, 8, 0, 21, 0),
                            java.util.List.of(new Entities.Enemy(18, 18, 2))));
            if (!game.loadCurrent()) throw new AssertionError("Escort inspection save failed");
            game.setState(GameState.PLAYING);
            if (cam != null) {
                cam.setYaw(0);
                cam.setPitch(24);
                cam.setDistance(8);
            }
            renderFrames(game, 3);
            for (int frame = 0; frame < 3; frame++) {
                BeginDrawing();
                ClearBackground(Menu.INK);
                game.render();
                EndDrawing();
            }
            Image fieldCitizen = LoadImageFromScreen();
            ExportImage(fieldCitizen, outDir.resolve("field-survivor-shared-model.png").toString());
            UnloadImage(fieldCitizen);

            capture(game, outDir.resolve("objective-survivor-nearby.png").toString());
            FieldEvent companion = game.getProgression().fieldEvent();
            companion.update(.01f, companion.x(), companion.z(), true);
            game.getPlayer().getPosition().x(-8).z(6);
            capture(game, outDir.resolve("escort-beacon-route.png").toString());
            if (!companion.walking()) throw new AssertionError("Nearby escort failed to walk");
            game.getPlayer().getPosition().x(15).z(-15);
            if (cam != null) cam.setDistance(4);
            capture(game, outDir.resolve("objective-survivor-offscreen.png").toString());

            if (companion.walking()) throw new AssertionError("Separated escort walked in place");

            // Result content at minimum size: earned values, short next destination, no story
            // overlap.
            SetWindowSize(1000, 680);
            for (int chapter = 0; chapter < 4; chapter++) {
                for (int frame = 0; frame < 5; frame++) {
                    BeginDrawing();
                    ClearBackground(Menu.INK);
                    BeginMode2D(new Camera2D().zoom((float) GetRenderWidth() / GetScreenWidth()));
                    Menu.panel(146, 76, 708, 510);
                    ChapterResultScreen.draw(
                            game.getProgression(), chapter, chapter == 3, 170, 100);
                    EndMode2D();
                    EndDrawing();
                }
                Image result = LoadImageFromScreen();
                ExportImage(
                        result, outDir.resolve("chapter-results-" + chapter + ".png").toString());
                UnloadImage(result);
            }
            SetWindowSize(1280, 800);

            // Returning through the city must consume the already-completed chapter transition
            // once.
            SaveGame.write(
                    profile.resolve("save.properties"),
                    new SaveGame.Snapshot(
                            "Return",
                            0,
                            4,
                            0,
                            new Progression(),
                            new WaveDirector.State(4, 0, 1, 8, 0, 21, 0),
                            java.util.List.of()));
            if (!game.loadCurrent() || game.getState() != GameState.LEVEL_COMPLETE)
                throw new AssertionError("Chapter completion fixture invalid");
            game.travelToCitadel();
            game.deployFromCitadel();
            renderFrames(game, 3);
            if (game.getState() == GameState.LEVEL_COMPLETE)
                throw new AssertionError("Repeated congratulations after city return");

            Progression rescueProgress = new Progression();
            for (int rescueWave : new int[] {2, 4, 6, 7, 8, 9}) {
                rescueProgress.fieldEvent().resetForTravel();
                rescueProgress.fieldEvent().begin(rescueWave);
                Entities.Enemy lastEnemy = new Entities.Enemy(18, 18, rescueWave);
                SaveGame.write(
                        profile.resolve("save.properties"),
                        new SaveGame.Snapshot(
                                "Rescue",
                                0,
                                4,
                                0,
                                rescueProgress,
                                new WaveDirector.State(rescueWave, 0, 1, 8, 0, 21, 0),
                                java.util.List.of(lastEnemy)));
                if (!game.loadCurrent()) throw new AssertionError("Rescue fixture failed");
                game.setState(GameState.PLAYING);
                game.getEnemies().getFirst().health = 0;
                renderFrames(game, 5);
                if (game.getState() != GameState.PLAYING
                        || !game.getProgression().fieldEvent().visible())
                    throw new AssertionError("Wave cut off rescue " + rescueWave);
                if (Campaign.boundary(rescueWave)) {
                    if (!game.saveCurrent()
                            || !game.loadCurrent()
                            || game.getState() != GameState.PAUSED
                            || game.getProgression().unlockedChapter() > Campaign.index(rescueWave))
                        throw new AssertionError("Reload skipped unfinished rescue " + rescueWave);
                    game.setState(GameState.PLAYING);
                }
                FieldEvent escort = game.getProgression().fieldEvent();
                escort.update(.01f, escort.x(), escort.z(), true, game.chapterIndex());
                for (int step = 0; step < 500 && escort.visible(); step++) {
                    float distance = (float) Math.hypot(escort.x(), escort.z());
                    escort.update(
                            .05f,
                            escort.x() - escort.x() / distance * 3,
                            escort.z() - escort.z() / distance * 3,
                            false,
                            game.chapterIndex());
                }
                if (escort.status() != FieldEvent.Status.COMPLETE)
                    throw new AssertionError("Escort could not finish " + rescueWave);
                renderFrames(game, 2);
                rescueProgress = game.getProgression();
            }
            if (rescueProgress.rescuedSurvivors() != 6)
                throw new AssertionError("Campaign did not return all six citizens");
            game.travelToCitadel();
            renderFrames(game, 10);
            CitadelHub city = game.getCitadelHub();
            CitadelNpc[] residents = CitadelNpc.values();
            float[] startX = new float[residents.length], startZ = new float[residents.length];
            boolean[] walked = new boolean[residents.length];
            for (CitadelNpc npc : residents) {
                startX[npc.ordinal()] = city.npcX(npc);
                startZ[npc.ordinal()] = city.npcZ(npc);
            }
            for (int step = 0; step < 800; step++) {
                city.updateCitizens(.05f, 30, 0, 6);
                for (CitadelNpc npc : residents) {
                    float x = city.npcX(npc), z = city.npcZ(npc);
                    if (!city.citizenWalkable(x, z))
                        throw new AssertionError(
                                "Citizen inside rendered building: " + npc.npcName());
                    int index = npc.ordinal();
                    walked[index] |= Math.hypot(x - startX[index], z - startZ[index]) > .5;
                }
            }
            for (CitadelNpc npc : residents)
                if (!walked[npc.ordinal()])
                    throw new AssertionError("Stationary citizen: " + npc.npcName());
            System.out.println(
                    "PASS: no repeat chapter result, all six rescue opportunities retained and"
                            + " citizens outside rendered houses");

            // The gate road extends beyond the expedition's 22 metre save boundary.
            game.getPlayer().getPosition().x(0).z(26);
            if (!game.saveCurrent()
                    || !game.loadCurrent()
                    || game.getState() != GameState.CITADEL
                    || game.getPlayer().getPosition().z() != 26
                    || game.getProgression().rescuedSurvivors() != 6
                    || game.getPlayer().isBoundsEnabled())
                throw new AssertionError(
                        "City save did not return to gate road with the full roster");
            capture(game, outDir.resolve("citadel-resumed-gate-road.png").toString());
            System.out.println(
                    "PASS: open chapter-end escorts survive reload and city saves restore gate road"
                            + " and roster");

            // Resume a saved mentor visit through the pause menu's actual Save & Exit action.
            var savedVisit = AppData.read(profile.resolve("save.properties"));
            savedVisit.setProperty("cityVisit", "MENTOR");
            AppData.write(profile.resolve("save.properties"), savedVisit);
            if (!game.loadCurrent()
                    || game.tutorial() == null
                    || game.tutorial().step() != Tutorial.Step.FOCUS)
                throw new AssertionError("City tutorial did not resume at mentor");
            try (AutomationEvent down = new AutomationEvent().type(2)) {
                down.params(0, KEY_ESCAPE);
                PlayAutomationEvent(down);
            }
            renderFrames(game, 1);
            try (AutomationEvent up = new AutomationEvent().type(1)) {
                up.params(0, KEY_ESCAPE);
                PlayAutomationEvent(up);
            }
            renderFrames(game, 1);
            if (game.getState() != GameState.PAUSED) throw new AssertionError("City pause failed");
            inventoryClick(
                    game,
                    GetScreenWidth() / 2,
                    (GetScreenHeight() - 440) / 2 + 336,
                    MOUSE_BUTTON_LEFT);
            if (game.getState() != GameState.MAIN_MENU
                    || !game.loadCurrent()
                    || game.getState() != GameState.CITADEL
                    || game.tutorial() == null
                    || game.tutorial().step() != Tutorial.Step.FOCUS
                    || game.getPlayer().getPosition().z() != 26)
                throw new AssertionError("Save & Exit lost mentor visit or gate position");
            System.out.println(
                    "PASS: paused Save & Exit retains the city tutorial and coordinates");

            // Mixed loadout must survive actual death/retry and repeated checkpoint reloads.
            java.util.Properties retryProps = new java.util.Properties();
            new Progression().store(retryProps);
            retryProps.setProperty("characterLevel", "10");
            retryProps.setProperty("crystals", "10000");
            Progression entryGear = Progression.restore(retryProps);
            entryGear.buyWeapon(2);
            entryGear.equipWeapon(2);
            for (Entities.ArmorSlot slot : Entities.ArmorSlot.values()) {
                entryGear.buyArmor(slot, 2);
                entryGear.equipArmor(slot, 2);
            }
            var checkpoint = profile.resolve("chapter-checkpoint.properties");
            SaveGame.write(
                    checkpoint,
                    new SaveGame.Snapshot(
                            "Warden",
                            0,
                            4,
                            0,
                            entryGear,
                            new WaveDirector.State(4, 0, 0, 5, 0, 21, 0),
                            java.util.List.of(),
                            Arena.HIGHLANDS));
            entryGear.store(retryProps);
            Progression defeatGear = Progression.restore(retryProps);
            defeatGear.buyWeapon(3);
            defeatGear.equipWeapon(3);
            defeatGear.buyArmor(Entities.ArmorSlot.HEAD, 3);
            defeatGear.equipArmor(Entities.ArmorSlot.HEAD, 3);
            SaveGame.write(
                    profile.resolve("save.properties"),
                    new SaveGame.Snapshot(
                            "Warden",
                            0,
                            4,
                            0,
                            defeatGear,
                            new WaveDirector.State(5, 0, 1, 8, 0, 21, 0),
                            java.util.List.of(new Entities.Enemy(0, 12, 5, 5, .5f)),
                            Arena.HIGHLANDS));
            if (!game.loadCurrent()) throw new AssertionError("Retry setup failed");
            for (int attempt = 0; attempt < 2; attempt++) {
                game.getProgression().damagePlayer(100000);
                game.setState(GameState.GAME_OVER);
                game.retryChapter();
                if (game.getState() != GameState.PLAYING || game.getProgression().defeated())
                    throw new AssertionError("Retry did not restore a playable encounter");
                Progression retried = game.getProgression();
                if (retried.weaponTier() != 3 || retried.armorTier(Entities.ArmorSlot.HEAD) != 3)
                    throw new AssertionError("Retry lost final sword/helmet");
                for (Entities.ArmorSlot slot : Entities.ArmorSlot.values())
                    if (slot != Entities.ArmorSlot.HEAD && retried.armorTier(slot) != 2)
                        throw new AssertionError("Retry changed mixed armor selection");
                if (retried.crystals() > defeatGear.crystals())
                    throw new AssertionError("Retry refunded gear");
            }
            capture(game, outDir.resolve("retry-preserved-loadout.png").toString());
            if (SaveGame.read(profile.resolve("save.properties")).progression().weaponTier() != 3)
                throw new AssertionError("Retry equipment was not persisted");
            game.travelToCitadel();
            System.out.println(
                    "PASS: final sword/helmet and second-tier other armor survive death and"
                            + " repeated retries");

            // NPC conversations: real Talk input, readable body height and dismissal.
            game.getPlayer()
                    .getPosition()
                    .x(CitadelNpc.ARCANIST.x() + .5f)
                    .z(CitadelNpc.ARCANIST.z());
            renderFrames(game, 4);
            inventoryClick(game, GetScreenWidth() / 2, GetScreenHeight() - 90, MOUSE_BUTTON_LEFT);
            if (game.getCitadelHub().activeNpc() != CitadelNpc.ARCANIST)
                throw new AssertionError("Clickable Talk prompt did not open nearby NPC");
            String greeting = game.getCitadelHub().npcDialogue();
            int talkHeight =
                    Math.min(
                            GetScreenHeight() - 48,
                            Math.max(350, Menu.wrappedHeight(greeting, 692, 18) + 244));
            int dialogueX = (GetScreenWidth() - 740) / 2;
            int dialogueY = (GetScreenHeight() - talkHeight) / 2;
            inventoryClick(game, dialogueX + 500, dialogueY + talkHeight - 110, MOUSE_BUTTON_LEFT);
            if (!game.getCitadelHub().npcDialogue().equals(CitadelNpc.ARCANIST.lore()))
                throw new AssertionError("Talk did not open the NPC conversation");
            capture(game, outDir.resolve("npc-talk-conversation.png").toString());
            SetWindowSize(1000, 680);
            capture(game, outDir.resolve("npc-talk-conversation-1000.png").toString());
            inventoryClick(game, 10, 300, MOUSE_BUTTON_LEFT);
            if (game.getCitadelHub().activeNpc() != null)
                throw new AssertionError("Outside click did not return to city");
            SetWindowSize(1280, 800);
            renderFrames(game, 3);
            System.out.println(
                    "PASS: Talk interaction, readable conversation layouts and outside dismissal");

            // Actual settings input, separate audio buses and backward-compatible persistence.
            game.setState(GameState.PAUSED);
            renderFrames(game, 3);
            inventoryClick(
                    game,
                    (GetScreenWidth() - 440) / 2 + 100,
                    (GetScreenHeight() - 440) / 2 + 230,
                    MOUSE_BUTTON_LEFT);
            if (game.getState() != GameState.SETTING_MENU)
                throw new AssertionError("Settings did not open");
            int settingsX = (GetScreenWidth() - 820) / 2, settingsY = (GetScreenHeight() - 650) / 2;
            inventoryClick(game, settingsX + 430 + 70, settingsY + 64 + 28, MOUSE_BUTTON_LEFT);
            inventoryClick(game, settingsX + 263, settingsY + 118 + 28, MOUSE_BUTTON_LEFT);
            capture(game, outDir.resolve("polish-audio-settings.png").toString());
            SetWindowSize(1000, 680);
            capture(game, outDir.resolve("polish-audio-settings-1000.png").toString());
            SetWindowSize(1280, 800);
            renderFrames(game, 5);
            inventoryClick(game, settingsX + 100, settingsY + 600, MOUSE_BUTTON_LEFT);
            Settings mix = new Settings();
            mix.load(profile.resolve("settings.properties"));
            if (Math.abs(mix.musicVolume - .2f) > .01 || Math.abs(mix.effectsVolume - .75f) > .01)
                throw new AssertionError("Audio slider changes were not independently saved");
            game.setState(GameState.CITADEL);
            for (int i = 0; i < 180; i++)
                game.audio().update(1f / 60, 0, false, false, false, false);
            if (game.audio().ready()
                    && (!game.audio().musicStreaming() || game.audio().activeStreams() > 1))
                throw new AssertionError("Inactive music streams still decoding");
            System.out.println(
                    "PASS: independent audio sliders persist and inactive music streams stop");

            // Capture real encounter states using the normal save/load and reward paths.
            Progression polish = new Progression();
            polish.grantStarterWeapon();
            Entities.Enemy foe = new Entities.Enemy(2, 7, 1, 0, .5f);
            SaveGame.write(
                    profile.resolve("save.properties"),
                    new SaveGame.Snapshot(
                            "Warden",
                            0,
                            4,
                            0,
                            polish,
                            new WaveDirector.State(1, 3, 7, 8, 1.8f, 20, 0),
                            java.util.List.of(foe),
                            Arena.HIGHLANDS));
            if (!game.loadCurrent()) throw new AssertionError("Polish encounter failed to load");
            game.setState(GameState.PLAYING);
            game.getEnemies().getFirst().stun = 100;
            capture(game, outDir.resolve("polish-encounter-hud.png").toString());
            var contact = game.getEnemies().getFirst();
            game.combatEffects()
                    .strike(
                            contact.x,
                            contact.z,
                            game.getPlayer().getPosition().x(),
                            game.getPlayer().getPosition().z(),
                            Menu.GOLD,
                            true);
            capture(game, outDir.resolve("directional-contact-impact.png").toString());
            game.getEnemies().getFirst().health = 0;
            renderFrames(game, 2);
            capture(game, outDir.resolve("polish-rewards.png").toString());
            if (game.getProgression().kills() != 1)
                throw new AssertionError("Reward path did not resolve");

            Progression sortie = new Progression(HeroClass.ARCHER);
            sortie.grantStarterWeapon();
            SaveGame.write(
                    profile.resolve("save.properties"),
                    new SaveGame.Snapshot(
                            "Watch",
                            0,
                            6,
                            180,
                            sortie,
                            new WaveDirector.State(3, 0, 1, 8, 0, 21, 0),
                            java.util.List.of(new Entities.Enemy(-3, 1, 3, 3, .5f))));
            if (!game.loadCurrent()) throw new AssertionError("Ranged volley fixture failed");
            game.setState(GameState.PLAYING);
            Entities.Enemy shooter = game.getEnemies().getFirst();
            shooter.beginAttack(0, 6, true);
            for (int frame = 0; frame < 7; frame++) renderFrames(game, 1);
            try (AutomationEvent down = new AutomationEvent().type(2)) {
                down.params(0, KEY_T);
                PlayAutomationEvent(down);
            }
            renderFrames(game, 1);
            try (AutomationEvent up = new AutomationEvent().type(1)) {
                up.params(0, KEY_T);
                PlayAutomationEvent(up);
            }
            SetWindowSize(1000, 680);
            capture(game, outDir.resolve("ranged-volley-1000.png").toString());
            if (game.focusedEnemy() != shooter || !shooter.impactPending)
                throw new AssertionError("Expected the focused archer's committed windup");
            renderFrames(game, 24);
            if (shooter.impactPending) throw new AssertionError("Archer never released the shot");
            SetWindowSize(1280, 800);

            TargetFrameScreen targetView = new TargetFrameScreen();
            Entities.Enemy king = new Entities.Enemy(0, 0, 10, 5, .5f);
            if (!king.boss() || !king.king)
                throw new AssertionError("Expected Hollow King presentation fixture");
            king.health = king.maxHealth * .4f;
            king.bossMove = Entities.BossMove.SIGILS;
            king.attackTime = 1.4f;
            for (int i = 0; i < 5; i++) {
                BeginDrawing();
                ClearBackground(Menu.INK);
                BeginMode2D(new Camera2D().zoom((float) GetRenderWidth() / GetScreenWidth()));
                targetView.draw(king, 12, "T");
                EndMode2D();
                EndDrawing();
            }
            Image targetReview = LoadImageFromScreen();
            ExportImage(targetReview, outDir.resolve("polish-boss-cast-frame.png").toString());
            UnloadImage(targetReview);

            System.out.println(
                    "All visual inspection screenshots captured successfully in " + outDir);
        } finally {
            if (game != null) game.dispose();
            CloseWindow();
        }
    }

    private static void titlePointer(int x, int y) {
        try (AutomationEvent move = new AutomationEvent().type(7)) {
            move.params(0, x).params(1, y);
            PlayAutomationEvent(move);
        }
    }

    private static void titleFrame(MainMenuScreen title) {
        BeginDrawing();
        ClearBackground(Menu.INK);
        BeginMode2D(new Camera2D().zoom((float) GetRenderWidth() / GetScreenWidth()));
        MainMenuScreen.Action action = title.draw("Warden", true, "");
        EndMode2D();
        EndDrawing();
        if (action != MainMenuScreen.Action.NONE)
            throw new AssertionError("Sky drag activated a menu button");
    }

    private static void inventoryClick(Game game, int x, int y, int button) {
        try (AutomationEvent move = new AutomationEvent().type(7)) {
            move.params(0, x).params(1, y);
            PlayAutomationEvent(move);
        }
        try (AutomationEvent down = new AutomationEvent().type(6)) {
            down.params(0, button);
            PlayAutomationEvent(down);
        }
        renderFrames(game, 1);
        try (AutomationEvent up = new AutomationEvent().type(5)) {
            up.params(0, button);
            PlayAutomationEvent(up);
        }
        renderFrames(game, 2);
    }

    private static void renderFrames(Game game, int count) {
        for (int f = 0; f < count; f++) {
            game.update(1f / 60f);
            BeginDrawing();
            ClearBackground(Menu.INK);
            game.render();
            EndDrawing();
        }
    }

    private static void capture(Game game, String outputPath) {
        renderFrames(game, 5);
        Image image = LoadImageFromScreen();
        try {
            ExportImage(image, outputPath);
            System.out.println("Exported view: " + outputPath);
        } finally {
            UnloadImage(image);
        }
    }
}
