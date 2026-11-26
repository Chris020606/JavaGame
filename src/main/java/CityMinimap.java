import static com.raylib.Raylib.*;

/** North-up city map: coordinates match the real city, independent of camera rotation. */
public final class CityMinimap {
    private static final CityNavigation navigation = new CityNavigation();
    private static Progression owner;

    public static CitadelGuide.Destination destination(Progression p, CitadelHub hub) {
        syncOwner(p);
        CitadelGuide.Destination target = navigation.destination(p);
        CitadelNpc npc = p.hasWeapon() ? navigation.tracked() : CitadelGuide.mentor(p.heroClass());
        if (npc == null || npc == CitadelNpc.COMMANDER) return target;
        return new CitadelGuide.Destination(
                target.title(), target.instruction(), hub.npcX(npc), hub.npcZ(npc));
    }

    static boolean visit(CitadelNpc citizen, Progression p) {
        syncOwner(p);
        return navigation.visit(citizen, p);
    }

    private static void syncOwner(Progression p) {
        if (owner != p) {
            owner = p;
            navigation.clear();
        }
    }

    public static void draw(CitadelHub hub, Progression p, float px, float pz) {
        destination(p, hub);
        int x = GetScreenWidth() - 184, y = 82;
        Menu.panel(x, y, 160, 160);
        Menu.text("AETHELGARD", x + 12, y + 10, 11, Menu.GOLD);
        int cx = x + 80, cy = y + 88;
        DrawCircle(cx, cy, 59, Fade(Menu.INK, .6f));
        DrawCircleLines(cx, cy, 59, Menu.LINE);
        DrawCircleLines(cx, cy, 15, Fade(Menu.TEAL, .8f));
        DrawLine(cx - 59, cy, cx + 59, cy, Fade(Menu.LINE, .45f));
        DrawLine(cx, cy - 59, cx, cy + 59, Fade(Menu.LINE, .45f));
        Menu.center("N", cx, cy - 69, 10, Menu.GOLD);
        Menu.center("S", cx, cy + 61, 9, Menu.MUTED);
        Menu.center("W", cx - 69, cy - 5, 9, Menu.MUTED);
        Menu.center("E", cx + 69, cy - 5, 9, Menu.MUTED);
        CitadelNpc hovered = null;
        double closest = 8;
        Vector2 pointer = GetMousePosition();
        for (CitadelNpc npc : CitadelNpc.values()) {
            if (npc.isSurvivor()) continue;
            if (npc == CitadelNpc.CARAVAN_TRADER) {
                DrawPoly(
                        new Vector2().x(mapX(x, hub.npcX(npc))).y(mapY(y, hub.npcZ(npc))),
                        4,
                        5,
                        45,
                        Menu.GOLD);
            }
            double distance =
                    Math.hypot(
                            pointer.x() - mapX(x, hub.npcX(npc)),
                            pointer.y() - mapY(y, hub.npcZ(npc)));
            if (distance < closest) {
                hovered = npc;
                closest = distance;
            }
            DrawCircle(
                    mapX(x, hub.npcX(npc)),
                    mapY(y, hub.npcZ(npc)),
                    3,
                    npc.mentorClass() == p.heroClass() ? Menu.GOLD : Menu.MUTED);
        }
        if (hovered != null && IsMouseButtonPressed(MOUSE_BUTTON_LEFT)) navigation.track(hovered);
        if (IsMouseButtonPressed(MOUSE_BUTTON_RIGHT)
                && Math.hypot(pointer.x() - cx, pointer.y() - cy) <= 59) navigation.clear();
        if (navigation.tracked() != null && p.hasWeapon()) {
            CitadelGuide.Destination target = destination(p, hub);
            int tx = mapX(x, target.x()), tz = mapY(y, target.z());
            DrawLine(mapX(x, px), mapY(y, pz), tx, tz, Fade(Menu.GOLD, .6f));
            DrawCircleLines(tx, tz, 7, Menu.GOLD);
        }
        DrawCircle(mapX(x, CitadelHub.PORTAL_X), mapY(y, CitadelHub.PORTAL_Z), 4, Menu.TEAL);
        DrawCircle(mapX(x, px), mapY(y, pz), 4, Menu.TEXT);
        Menu.text("CLICK TO TRACK", x + 12, y + 143, 10, Menu.GOLD);
        if (hovered != null) {
            int tx = x - 280, ty = y + 164;
            Menu.panel(tx, ty, 440, 84);
            Menu.text(hovered.npcName(), tx + 12, ty + 10, 15, Menu.GOLD);
            String service =
                    hovered == CitadelNpc.CARAVAN_TRADER
                            ? "Provisions and permanent caravan upgrades"
                            : hovered == CitadelNpc.COMMANDER
                                    ? "Expedition gatekeeper"
                                    : "Class weapons and individual armor pieces";
            Menu.text(service, tx + 12, ty + 36, 12, Menu.TEXT);
            Menu.text(
                    "LEFT CLICK: TRACK / RIGHT CLICK RADAR: CLEAR",
                    tx + 12,
                    ty + 61,
                    10,
                    Menu.TEAL);
        }
    }

    private static int mapX(int x, float world) {
        return x + 80 + Math.round(world * .95f);
    }

    private static int mapY(int y, float world) {
        return y + 88 + Math.round((world + 7) * .95f);
    }

    private CityMinimap() {}
}
