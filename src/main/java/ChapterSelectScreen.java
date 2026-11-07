import static com.raylib.Raylib.*;

/** Zone selection is a view; Game owns starting and saving the selected expedition. */
public final class ChapterSelectScreen {
    public static final int NO_SELECTION = -1;
    public static final int BACK = -2;

    /** Returns a chapter index, BACK, or NO_SELECTION; the coordinator performs travel. */
    public int draw(int unlocked, int current) {
        int x = (GetScreenWidth() - 740) / 2, y = (GetScreenHeight() - 570) / 2;
        Menu.panel(x, y, 740, 570);
        Menu.text("WORLD MAP / THE VALLEY", x + 24, y + 22, 28, Menu.GOLD);
        Menu.wrapped(
                "Revisit unlocked chapters with your current equipment. Entering a chapter restarts"
                        + " its encounters and replaces the active checkpoint.",
                x + 24,
                y + 66,
                680,
                16,
                Menu.TEXT);
        // A connected route communicates chapter order; circles are destinations, not decoration.
        DrawLine(x + 40, y + 145, x + 40, y + 145 + 3 * 86, Menu.LINE);
        for (int i = 0; i < 4; i++) {
            var c = Campaign.CHAPTERS[i];
            int top = y + 130 + i * 86;
            DrawCircle(x + 40, top + 15, 13, i <= unlocked ? Menu.GOLD : Menu.LINE);
            DrawCircleLines(
                    x + 40,
                    top + 15,
                    i == current ? 20 : 16,
                    i <= unlocked ? Menu.GOLD : Menu.MUTED);
            Menu.center(Integer.toString(i + 1), x + 40, top + 9, 12, Menu.INK);
            Menu.text(c.title(), x + 72, top, 21, i <= unlocked ? Menu.TEXT : Menu.MUTED);
            Menu.text(
                    c.bossOnly()
                            ? "BOSS ENCOUNTER"
                            : c.waves()
                                    + " WAVES / "
                                    + (i == 2 ? "MOUNTAIN PASS" : "MEADOW DEFENSE"),
                    x + 72,
                    top + 32,
                    13,
                    Menu.MUTED);
            if (Menu.button(
                    i > unlocked ? "LOCKED" : i == current ? "RESTART" : "TRAVEL",
                    x + 540,
                    top - 4,
                    174,
                    i <= unlocked)) return i;
        }
        if (Menu.button("BACK", x + 24, y + 506, 220, true)) return BACK;
        return NO_SELECTION;
    }
}
