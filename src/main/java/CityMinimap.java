import static com.raylib.Raylib.*;

/** North-up city map: coordinates match the real city, independent of camera rotation. */
public final class CityMinimap {
    public static void draw(Progression p, float px, float pz) {
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
        Vector2 pointer = GetMousePosition();
        for (CitadelNpc npc : CitadelNpc.values()) {
            if (npc.isSurvivor()) continue;
            if (npc == CitadelNpc.CARAVAN_TRADER) {
                DrawPoly(
                        new Vector2().x(mapX(x, npc.x())).y(mapY(y, npc.z())), 4, 5, 45, Menu.GOLD);
            }
            if (Math.hypot(pointer.x() - mapX(x, npc.x()), pointer.y() - mapY(y, npc.z())) < 7)
                hovered = npc;
            DrawCircle(
                    mapX(x, npc.x()),
                    mapY(y, npc.z()),
                    3,
                    npc.mentorClass() == p.heroClass() ? Menu.GOLD : Menu.MUTED);
        }
        DrawCircle(mapX(x, CitadelHub.PORTAL_X), mapY(y, CitadelHub.PORTAL_Z), 4, Menu.TEAL);
        DrawCircle(mapX(x, px), mapY(y, pz), 4, Menu.TEXT);
        Menu.text("MENTOR / SHOP / GATE", x + 8, y + 143, 9, Menu.GOLD);
        if (hovered != null) {
            int tx = x - 280, ty = y + 164;
            Menu.panel(tx, ty, 440, 64);
            Menu.text(hovered.npcName(), tx + 12, ty + 10, 15, Menu.GOLD);
            String service =
                    hovered == CitadelNpc.CARAVAN_TRADER
                            ? "Provisions and permanent caravan upgrades"
                            : hovered == CitadelNpc.COMMANDER
                                    ? "Expedition gatekeeper"
                                    : "Class weapons and individual armor pieces";
            Menu.text(service, tx + 12, ty + 36, 12, Menu.TEXT);
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
