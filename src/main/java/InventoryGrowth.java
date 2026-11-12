import static com.raylib.Raylib.*;

/** Inventory progression: current value and the exact next-level increase share one scale. */
public final class InventoryGrowth {
    public static void draw(Progression p, int x, int y, int width) {
        Menu.panel(x, y, width, 140);
        Menu.text("LEVEL " + p.characterLevel() + " / NEXT LEVEL", x + 12, y + 9, 12, Menu.GOLD);
        row("HEALTH", p.maxHealth(), p.nextLevelHealthGain(), x + 12, y + 34, width - 24);
        row("POWER", p.damage(), p.nextLevelDamageGain(), x + 12, y + 72, width - 24);
        Menu.text(
                p.xp() + " / " + p.xpToNextLevel() + " XP     [C] ALL STATS",
                x + 12,
                y + 117,
                11,
                Menu.TEAL);
    }

    private static void row(String label, int current, int gain, int x, int y, int width) {
        Menu.text(label + " " + current, x, y, 12, Menu.TEXT);
        Menu.text("+" + gain + " NEXT", x + width - 78, y, 11, Menu.GOLD);
        int half = (width - 10) / 2;
        Menu.bar(x, y + 22, half, current, current + gain, Menu.TEAL);
        Menu.bar(x + half + 10, y + 22, half, current + gain, current + gain, Menu.GOLD);
    }

    private InventoryGrowth() {}
}
