import static com.raylib.Raylib.*;

/** Compact character summary: numbers explain stats, while only experience uses a progress bar. */
public final class InventoryGrowth {
    public static void draw(Progression p, int x, int y, int width) {
        Menu.panel(x, y, width, 140);
        Menu.text("LEVEL " + p.characterLevel(), x + 12, y + 10, 13, Menu.GOLD);
        Menu.text(
                "NEXT LEVEL",
                x + width - Menu.measure("NEXT LEVEL", 10) - 12,
                y + 12,
                10,
                Menu.MUTED);
        int cardWidth = (width - 34) / 2;
        stat(
                "HEALTH",
                p.maxHealth(),
                p.nextLevelHealthGain(),
                x + 12,
                y + 34,
                cardWidth,
                Menu.TEAL);
        stat(
                "POWER",
                p.damage(),
                p.nextLevelDamageGain(),
                x + 22 + cardWidth,
                y + 34,
                cardWidth,
                Menu.GOLD);
        String xp = p.xp() + " / " + p.xpToNextLevel() + " XP";
        Menu.text(xp, x + 12, y + 109, 11, Menu.MUTED);
        Menu.text(
                "[C] STATS",
                x + width - Menu.measure("[C] STATS", 10) - 12,
                y + 110,
                10,
                Menu.TEXT);
        int barWidth = width - 24;
        DrawRectangle(x + 12, y + 128, barWidth, 4, Menu.color(44, 54, 65));
        float ratio = Math.max(0, Math.min(1, (float) p.xp() / Math.max(1, p.xpToNextLevel())));
        DrawRectangle(x + 12, y + 128, Math.round(barWidth * ratio), 4, Menu.TEAL);
    }

    private static void stat(
            String label, int value, int gain, int x, int y, int width, Color accent) {
        DrawRectangle(x, y, width, 64, Menu.color(18, 27, 40));
        DrawRectangle(x, y, 2, 64, Fade(accent, .7f));
        Menu.text(label, x + 10, y + 7, 10, Menu.MUTED);
        int size = 23;
        while (size > 12 && Menu.measure("" + value, size) > width - 18) size--;
        Menu.text("" + value, x + 10, y + 23, size, Menu.TEXT);
        Menu.text(gain > 0 ? "+" + gain + " next level" : "Level cap", x + 10, y + 50, 9, accent);
    }

    private InventoryGrowth() {}
}
