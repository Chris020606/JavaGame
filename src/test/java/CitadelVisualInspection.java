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
            // Travel to Citadel and unlock all 4 rescued survivors so they appear in town
            game.travelToCitadel();
            game.getProgression().rescueSurvivor();
            game.getProgression().rescueSurvivor();
            game.getProgression().rescueSurvivor();
            game.getProgression().rescueSurvivor();

            game.getProgression().rescueSurvivor();
            game.getProgression().rescueSurvivor();
            // Run warm-up frames
            renderFrames(game, 30);

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
                BeginDrawing();
                ClearBackground(Menu.INK);
                Camera2D overlay = new Camera2D().zoom((float) GetRenderWidth() / GetScreenWidth());
                BeginMode2D(overlay);
                OpeningStory.draw(page);
                EndMode2D();
                EndDrawing();
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
            for (int frame = 0; frame < 3; frame++) {
                BeginDrawing();
                ClearBackground(Menu.INK);
                BeginMode2D(new Camera2D().zoom((float) GetRenderWidth() / GetScreenWidth()));
                RescueLedgerScreen.draw(game.getProgression());
                EndMode2D();
                EndDrawing();
            }
            Image ledger = LoadImageFromScreen();
            ExportImage(ledger, outDir.resolve("rescue-ledger.png").toString());
            UnloadImage(ledger);

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

            game.setState(GameState.PLAYING);
            game.getPlayer().getPosition().x(-10).y(0).z(12);
            if (cam != null) {
                cam.setYaw(0);
                cam.setPitch(24);
                cam.setDistance(8);
            }
            renderFrames(game, 3);
            game.getProgression().fieldEvent().begin(2);
            for (int frame = 0; frame < 3; frame++) {
                BeginDrawing();
                ClearBackground(Menu.INK);
                game.render();
                EndDrawing();
            }
            Image fieldCitizen = LoadImageFromScreen();
            ExportImage(fieldCitizen, outDir.resolve("field-survivor-shared-model.png").toString());
            UnloadImage(fieldCitizen);

            System.out.println(
                    "All visual inspection screenshots captured successfully in " + outDir);
        } finally {
            if (game != null) game.dispose();
            CloseWindow();
        }
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
