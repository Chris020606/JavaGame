import static com.raylib.Colors.WHITE;
import static com.raylib.Raylib.*;

import Entities.*;

import assets.AssetPaths;

/** Select/compare equipment, then equip explicitly, double-click, or drag to a labeled slot. */
public final class EquipmentScreen implements AutoCloseable {
    private final RenderTexture preview = LoadRenderTexture(960, 1040);
    private final RenderTexture[] thumbnails = new RenderTexture[16];
    private final Shader shader =
            LoadShader(
                    AssetPaths.path("resources/outpost/daylight.vs"),
                    AssetPaths.path("resources/outpost/preview.fs"));
    private final Camera3D camera =
            new Camera3D()
                    ._position(new Vector3().x(2.2f).y(2.3f).z(4.7f))
                    .target(new Vector3().y(1.05f))
                    .up(new Vector3().y(1))
                    .fovy(32)
                    .projection(CAMERA_PERSPECTIVE);
    private HeroClass thumbnailClass;
    private Progression owner;
    private int selected, dragged = -1, filter, lastClicked = -1;
    private double lastClick;
    private float yaw, dragX, dragY;
    private boolean orbit;
    private boolean viewDossier;
    private boolean showUnowned;

    private static Color tierColor(int tier) {
        return tier == 3
                ? Menu.GOLD
                : tier == 2 ? Menu.TEAL : tier == 1 ? Menu.color(182, 201, 220) : Menu.MUTED;
    }

    public void prepare(WorldAssets assets, Player player, Progression progress) {
        if (owner != progress) {
            owner = progress;
            selected = progress.weaponTier();
            dragged = lastClicked = -1;
            filter = 0;
            showUnowned = false;
            viewDossier = false;
        }
        Vector3 position = player.getPosition();
        float facing = player.getRotation().y();
        player.setPosition(new Vector3());
        player.getRotation().y(yaw);
        player.poseForInventory();
        assets.previewPass(shader);
        BeginTextureMode(preview);
        ClearBackground(Menu.INK);
        DrawRectangleGradientV(0, 0, 960, 1040, Menu.color(48, 65, 91), Menu.color(18, 25, 40));
        BeginMode3D(camera);
        DrawCylinder(new Vector3().y(-.03f), .95f, .95f, .06f, 32, Menu.color(93, 83, 59));
        assets.preparePlayer(player);
        assets.drawPlayerBody(player, progress, WHITE);
        if (progress.hasWeapon()) {
            assets.drawPlayerWeapon(player, progress.heroClass().ordinal(), progress.weaponTier());
        }
        assets.drawArmor(player, progress);
        EndMode3D();
        EndTextureMode();
        if (thumbnailClass != progress.heroClass()) {
            for (int item = 0; item < 16; item++) {
                if (thumbnails[item] == null) thumbnails[item] = LoadRenderTexture(128, 128);
                float target =
                        item < 4
                                ? (progress.heroClass() == HeroClass.ARCHER
                                        ? (item >= 2 ? .12f : 0)
                                        : .85f)
                                : slot(item) == ArmorSlot.HEAD
                                        ? 1.85f
                                        : slot(item) == ArmorSlot.CHEST
                                                ? .85f
                                                : slot(item) == ArmorSlot.LEGS ? .34f : .12f;
                float distance =
                        item < 4
                                ? (progress.heroClass() == HeroClass.ARCHER && item >= 2
                                        ? 1.7f
                                        : 4.3f)
                                : slot(item) == ArmorSlot.HEAD
                                        ? 2.7f
                                        : slot(item) == ArmorSlot.CHEST ? 2.8f : 1.2f;
                Camera3D itemCamera =
                        new Camera3D()
                                ._position(
                                        new Vector3()
                                                .x(distance * .5f)
                                                .y(target + distance * .25f)
                                                .z(distance))
                                .target(new Vector3().y(target))
                                .up(new Vector3().y(1))
                                .fovy(34)
                                .projection(CAMERA_PERSPECTIVE);
                BeginTextureMode(thumbnails[item]);
                ClearBackground(new Color().a((byte) 0));
                BeginMode3D(itemCamera);
                assets.drawInventoryItem(player, progress, item);
                EndMode3D();
                EndTextureMode();
            }
            thumbnailClass = progress.heroClass();
        }
        assets.endPreview();
        player.setPosition(position);
        player.getRotation().y(facing);
    }

    private static boolean inside(Vector2 m, int x, int y, int w, int h) {
        return m.x() >= x && m.x() < x + w && m.y() >= y && m.y() < y + h;
    }

    private static int tier(int item) {
        return item < 4 ? item : 1 + (item - 4) / 4;
    }

    private static ArmorSlot slot(int item) {
        return ArmorSlot.values()[(item - 4) % 4];
    }

    private static boolean owned(Progression p, int item) {
        return item < 4
                ? (p.hasWeapon() && p.ownsWeapon(item))
                : item < 16 ? p.ownsArmor(slot(item), tier(item)) : p.potions() > 0;
    }

    private static boolean equipped(Progression p, int item) {
        return item < 4
                ? (p.hasWeapon() && p.weaponTier() == item)
                : item < 16 && p.armorTier(slot(item)) == tier(item);
    }

    private static String name(Progression p, int item) {
        return item < 4
                ? p.weaponName(item)
                : item < 16 ? p.armorName(slot(item), tier(item)) : "Healing potions";
    }

    private static void equip(Progression p, int item) {
        if (item < 4) p.equipWeapon(item);
        else if (item < 16) p.equipArmor(slot(item), tier(item));
        else p.usePotion();
    }

    private static void remove(Progression p, int item) {
        if (item < 4) p.equipWeapon(0);
        else if (item < 16) p.equipArmor(slot(item), 0);
    }

    private void thumbnail(int item, int x, int y, int size, Color tint) {
        if (item == 16) {
            Menu.icon("potion", x + size / 4, y + size / 4, size / 2, Menu.RED);
            return;
        }
        if (thumbnails[item] == null) return;
        DrawTexturePro(
                thumbnails[item].texture(),
                new Rectangle().width(128).height(-128),
                new Rectangle().x(x).y(y).width(size).height(size),
                new Vector2(),
                0,
                tint);
    }

    private java.util.List<Integer> visible(Progression p) {
        java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();
        for (int i = 0; i < 17; i++)
            if ((showUnowned || owned(p, i))
                    && (filter == 0
                            || filter == 1 && i < 4
                            || filter == 2 && i >= 4 && i < 16
                            || filter == 3 && i == 16)) ids.add(i);
        return ids;
    }

    public boolean draw(Progression p, String inventoryKey) {
        int x = (GetScreenWidth() - 960) / 2, y = (GetScreenHeight() - 640) / 2;
        Vector2 mouse = GetMousePosition();
        Menu.panel(x, y, 960, 640);
        Menu.text("YOUR EQUIPMENT", x + 24, y + 18, 28, Menu.TEXT);
        Menu.text(
                p.heroClass().label + " / LEVEL " + p.characterLevel(),
                x + 24,
                y + 58,
                14,
                Menu.TEAL);
        Menu.icon("crystal", x + 714, y + 24, 20, Menu.GOLD);
        Menu.text(p.crystals() + " CRYSTALS", x + 744, y + 26, 17, Menu.GOLD);
        DrawLine(x + 20, y + 86, x + 940, y + 86, Menu.LINE);

        // Character and equipped pieces have their own stable column.
        Menu.frame(x + 20, y + 104, 248, 268, Menu.INK, Menu.LINE);
        DrawTexturePro(
                preview.texture(),
                new Rectangle().width(960).height(-1040),
                new Rectangle().x(x + 24).y(y + 108).width(240).height(260),
                new Vector2(),
                0,
                WHITE);
        Menu.text("DRAG TO TURN / SCROLL TO ZOOM", x + 28, y + 351, 10, Menu.MUTED);
        if (IsMouseButtonPressed(MOUSE_BUTTON_LEFT) && inside(mouse, x + 24, y + 108, 240, 240))
            orbit = true;
        if (orbit && IsMouseButtonDown(MOUSE_BUTTON_LEFT)) yaw += GetMouseDelta().x() * .8f;
        if (inside(mouse, x + 24, y + 108, 240, 240))
            camera.fovy(Math.max(28, Math.min(52, camera.fovy() - GetMouseWheelMove() * 2)));
        if (IsMouseButtonReleased(MOUSE_BUTTON_LEFT)) orbit = false;
        Menu.text("EQUIPPED", x + 20, y + 384, 12, Menu.GOLD);
        for (int s = 0; s < 5; s++) {
            int sx = x + 20 + s * 50, sy = y + 407;
            int level = s == 4 ? p.weaponTier() : p.armorTier(ArmorSlot.values()[s]);
            boolean filled = s == 4 ? p.hasWeapon() : level > 0;
            Menu.slot(sx, sy, 46, false, filled);
            if (filled)
                thumbnail(s == 4 ? level : 4 + (level - 1) * 4 + s, sx + 3, sy + 3, 40, WHITE);
            else
                Menu.icon(
                        s == 4 ? "sword" : ArmorSlot.values()[s].icon(),
                        sx + 11,
                        sy + 11,
                        24,
                        Menu.LINE);
            Menu.center(
                    s == 4 ? "WEAPON" : ArmorSlot.values()[s].name(),
                    sx + 23,
                    sy + 51,
                    8,
                    Menu.MUTED);
            if (inside(mouse, sx, sy, 46, 46)) {
                if (IsMouseButtonPressed(MOUSE_BUTTON_RIGHT)) {
                    if (s == 4) p.equipWeapon(0);
                    else p.equipArmor(ArmorSlot.values()[s], 0);
                }
                if (IsMouseButtonPressed(MOUSE_BUTTON_LEFT) && filled) {
                    selected = s == 4 ? level : 4 + (level - 1) * 4 + s;
                    filter = 0;
                    viewDossier = false;
                }
                if (IsMouseButtonReleased(MOUSE_BUTTON_LEFT)
                        && dragged >= 0
                        && (Math.abs(mouse.x() - dragX) + Math.abs(mouse.y() - dragY) > 6)
                        && (s == 4 && dragged < 4
                                || s < 4
                                        && dragged >= 4
                                        && dragged < 16
                                        && slot(dragged).ordinal() == s)) equip(p, dragged);
            }
        }
        InventoryGrowth.draw(p, x + 20, y + 482, 248);

        if (IsKeyPressed(KEY_C)) viewDossier = !viewDossier;
        if (Menu.button(
                viewDossier ? "BACK TO ITEMS [C]" : "CHARACTER STATS [C]",
                x + 286,
                y + 104,
                356,
                true)) viewDossier = !viewDossier;
        if (viewDossier) {
            drawStatsSheet(p, x + 286, y + 160, 654, 410);
        } else {
            String[] categories = {"ALL", "WEAPONS", "ARMOR", "SUPPLIES"};
            for (int i = 0; i < 4; i++) {
                int tx = x + 286 + i * 90;
                boolean hover = inside(mouse, tx, y + 164, 86, 30);
                Menu.frame(
                        tx,
                        y + 164,
                        86,
                        30,
                        filter == i ? Menu.color(53, 65, 75) : Menu.PANEL,
                        filter == i ? Menu.GOLD : Menu.LINE);
                Menu.center(
                        categories[i], tx + 43, y + 174, 11, filter == i ? Menu.GOLD : Menu.MUTED);
                if (hover && IsMouseButtonPressed(MOUSE_BUTTON_LEFT)) {
                    filter = i;
                    dragged = lastClicked = -1;
                }
            }
            if (Menu.button(
                    showUnowned ? "SHOW OWNED ONLY" : "INCLUDE UNOWNED",
                    x + 286,
                    y + 574,
                    356,
                    true)) showUnowned = !showUnowned;
            var ids = visible(p);
            if (!ids.isEmpty() && !ids.contains(selected)) selected = ids.get(0);
            int pos = Math.max(0, ids.indexOf(selected));
            int move =
                    IsKeyPressed(KEY_RIGHT)
                            ? 1
                            : IsKeyPressed(KEY_LEFT)
                                    ? -1
                                    : IsKeyPressed(KEY_DOWN) ? 4 : IsKeyPressed(KEY_UP) ? -4 : 0;
            if (move != 0 && !ids.isEmpty())
                selected = ids.get(Math.max(0, Math.min(ids.size() - 1, pos + move)));
            if (!ids.isEmpty() && IsKeyPressed(KEY_ENTER) && owned(p, selected)) equip(p, selected);
            if (!ids.isEmpty()
                    && equipped(p, selected)
                    && (IsKeyPressed(KEY_BACKSPACE) || IsKeyPressed(KEY_DELETE)))
                remove(p, selected);
            Menu.frame(x + 286, y + 204, 356, 356, Menu.color(18, 26, 38), Menu.LINE);
            if (ids.isEmpty()) {
                Menu.center("NO ITEMS IN THIS CATEGORY", x + 464, y + 338, 13, Menu.MUTED);
                Menu.center(
                        "Include unowned to browse forge equipment.",
                        x + 464,
                        y + 366,
                        10,
                        Menu.MUTED);
            }
            for (int cell = 0; cell < ids.size(); cell++) {
                int item = ids.get(cell),
                        cx = x + 298 + cell % 4 * 86,
                        cy = y + 216 + cell / 4 * 68;
                boolean available = owned(p, item), hover = inside(mouse, cx, cy, 72, 60);
                Menu.frame(
                        cx,
                        cy,
                        72,
                        60,
                        item == selected ? Menu.color(55, 62, 64) : Menu.PANEL,
                        item == selected ? Menu.GOLD : equipped(p, item) ? Menu.TEAL : Menu.LINE);
                thumbnail(item, cx + 10, cy, 50, available ? WHITE : Menu.color(90, 95, 100));
                Menu.text(
                        item == 16 ? "x" + p.potions() : "T" + tier(item),
                        cx + 5,
                        cy + 44,
                        10,
                        tierColor(tier(item)));
                if (!available) Menu.icon("lock", cx + 54, cy + 42, 12, Menu.MUTED);
                else if (equipped(p, item)) Menu.text("ON", cx + 49, cy + 44, 9, Menu.TEAL);
                if (hover && IsMouseButtonPressed(MOUSE_BUTTON_LEFT)) {
                    selected = item;
                    if (available && lastClicked == item && GetTime() - lastClick < .35) {
                        equip(p, item);
                        dragged = -1;
                        lastClicked = -1;
                    } else {
                        if (available) {
                            dragged = item;
                            dragX = mouse.x();
                            dragY = mouse.y();
                        }
                        lastClicked = item;
                        lastClick = GetTime();
                    }
                }
            }
            if (IsMouseButtonReleased(MOUSE_BUTTON_LEFT)) dragged = -1;
            drawSelected(p, x + 660, y + 104, ids.isEmpty());
        }
        boolean close = Menu.button("RETURN [" + inventoryKey + "]", x + 660, y + 574, 280, true);
        if (dragged >= 0 && Math.abs(mouse.x() - dragX) + Math.abs(mouse.y() - dragY) > 6) {
            Menu.panel((int) mouse.x() + 12, (int) mouse.y() + 12, 60, 60);
            thumbnail(dragged, (int) mouse.x() + 14, (int) mouse.y() + 14, 56, WHITE);
        }
        return close;
    }

    private void drawSelected(Progression p, int x, int y, boolean empty) {
        Menu.frame(x, y, 280, 456, Menu.color(27, 37, 52), Menu.LINE);
        Menu.text("ITEM DETAILS", x + 16, y + 14, 12, Menu.MUTED);
        if (empty) {
            Menu.wrapped(
                    "Select an item from your collection to inspect it here.",
                    x + 20,
                    y + 132,
                    240,
                    17,
                    Menu.MUTED);
            return;
        }
        Menu.slot(x + 94, y + 46, 92, false, equipped(p, selected));
        thumbnail(
                selected,
                x + 100,
                y + 52,
                80,
                owned(p, selected) ? WHITE : Menu.color(110, 115, 120));
        Menu.wrapped(name(p, selected), x + 18, y + 154, 244, 21, tierColor(tier(selected)));
        boolean wearing = equipped(p, selected), have = owned(p, selected);
        Menu.text(
                wearing ? "CURRENTLY EQUIPPED" : have ? "IN YOUR COLLECTION" : "NOT YET OWNED",
                x + 18,
                y + 215,
                12,
                wearing ? Menu.TEAL : Menu.GOLD);
        if (selected < 16) {
            int current = selected < 4 ? p.damage() : p.defense();
            int next =
                    selected < 4
                            ? p.weaponDamage(selected)
                            : current
                                    + p.armorBonus(slot(selected), tier(selected))
                                    - p.armorBonus(slot(selected), p.armorTier(slot(selected)));
            EquipmentComparison.draw(
                    selected < 4 ? "POWER" : "DEFENSE", current, next, x + 18, y + 250, 244);
        } else Menu.text("Restores up to 60 health.", x + 18, y + 252, 14, Menu.TEXT);
        String action =
                !have
                        ? "NOT OWNED"
                        : selected == 16
                                ? "DRINK POTION"
                                : wearing
                                        ? selected == 0 ? "STARTER EQUIPPED" : "REMOVE"
                                        : "EQUIP [ENTER]";
        boolean usable =
                have
                        && !(wearing && selected == 0)
                        && (selected != 16 || p.health() < p.maxHealth());
        if (Menu.button(action, x + 18, y + 302, 244, usable)) {
            if (wearing) remove(p, selected);
            else equip(p, selected);
        }
        String help =
                !have
                        ? "Forge this piece with your class mentor in the city. Requires level "
                                + Progression.requiredLevel(tier(selected))
                                + "."
                        : selected == 16
                                ? "Buy replacement potions from Torvald in Market Square."
                                : "Double-click or drag to a matching equipment slot. Right-click"
                                        + " an equipped slot to remove it.";
        Menu.wrapped(help, x + 18, y + 368, 244, 12, Menu.MUTED);
    }

    private final CharacterStatsScreen characterStats = new CharacterStatsScreen();

    private void drawStatsSheet(Progression p, int sx, int sy, int sw, int sh) {
        characterStats.draw(p, sx, sy, sw, sh);
    }

    public void toggleStatsSheet() {
        this.viewDossier = !this.viewDossier;
    }

    public void setViewDossier(boolean view) {
        this.viewDossier = view;
    }

    public boolean isViewDossier() {
        return viewDossier;
    }

    @Override
    public void close() {
        for (RenderTexture t : thumbnails) if (t != null) UnloadRenderTexture(t);
        UnloadRenderTexture(preview);
        UnloadShader(shader);
    }
}
