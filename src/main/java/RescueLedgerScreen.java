import static com.raylib.Raylib.*;

/** The rescue roster reflects persisted campaign progress; it grants no duplicate rewards. */
public final class RescueLedgerScreen {
    private static Progression owner;
    private static int selected = -1;

    public static boolean draw(Progression p) {
        if (owner != p) {
            owner = p;
            selected = -1;
        }
        int width = Math.min(900, GetScreenWidth() - 40), x = (GetScreenWidth() - width) / 2;
        int y = Math.max(16, (GetScreenHeight() - 590) / 2);
        DrawRectangle(0, 0, GetScreenWidth(), GetScreenHeight(), Fade(Menu.INK, .9f));
        Menu.panel(x, y, width, 590);
        Menu.text("THE PEOPLE OF AETHELGARD", x + 24, y + 22, 27, Menu.GOLD);
        int rescued = Math.min(CitadelNpc.RESCUED_SURVIVORS.length, p.rescuedSurvivors());
        Menu.text(
                rescued + " / " + CitadelNpc.RESCUED_SURVIVORS.length + " PEOPLE HOME",
                x + 24,
                y + 66,
                14,
                Menu.TEAL);
        Menu.bar(
                x + 24,
                y + 96,
                width - 48,
                rescued,
                CitadelNpc.RESCUED_SURVIVORS.length,
                Menu.TEAL);
        for (int i = 0; i < CitadelNpc.RESCUED_SURVIVORS.length; i++) {
            CitadelNpc npc = CitadelNpc.RESCUED_SURVIVORS[i];
            int col = i % 2, row = i / 2;
            int cardWidth = (width - 60) / 2,
                    cx = x + 24 + col * (cardWidth + 12),
                    cy = y + 122 + row * 109;
            boolean home = i < rescued;
            Menu.frame(
                    cx,
                    cy,
                    cardWidth,
                    98,
                    Menu.color(25, 36, 50),
                    selected == i ? Menu.GOLD : home ? Menu.TEAL : Menu.LINE);
            Vector2 mouse = GetMousePosition();
            if (home
                    && IsMouseButtonPressed(MOUSE_BUTTON_LEFT)
                    && mouse.x() >= cx
                    && mouse.x() < cx + cardWidth
                    && mouse.y() >= cy
                    && mouse.y() < cy + 98) selected = i;
            Menu.icon(
                    home ? "crown" : "shield", cx + 14, cy + 14, 22, home ? Menu.TEAL : Menu.MUTED);
            Menu.text(npc.npcName(), cx + 46, cy + 14, 16, home ? Menu.TEXT : Menu.MUTED);
            Menu.text(
                    home ? "SAFE IN THE CITY" : "STILL MISSING",
                    cx + 14,
                    cy + 46,
                    12,
                    home ? Menu.TEAL : Menu.GOLD);
            Menu.text(
                    home
                            ? "Select to hear their story."
                            : "Search the expedition's survivor markers.",
                    cx + 14,
                    cy + 72,
                    11,
                    Menu.MUTED);
        }
        String detail =
                selected >= 0 && selected < rescued
                        ? CitadelNpc.RESCUED_SURVIVORS[selected].lore()
                        : "Find survivors during expeditions. Interact and escort them into the"
                                + " Beacon's light. Select a returned citizen above to hear what"
                                + " coming home means to them.";
        Menu.wrapped(detail, x + 24, y + 468, width - 48, 13, Menu.TEXT);
        return Menu.button("RETURN TO CITY [ESC]", x + 24, y + 532, width - 48, true);
    }

    private RescueLedgerScreen() {}
}
