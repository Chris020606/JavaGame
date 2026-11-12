import static com.raylib.Raylib.*;

/** Explains real level gains on a shared scale; gold is the extra capacity at the next level. */
public final class ProgressionSummary {
    public static void draw(Progression p, int x, int y, int width) {
        Menu.panel(x, y, width, 228);
        Menu.text("YOUR WARDEN / LEVEL " + p.characterLevel(), x + 14, y + 12, 16, Menu.GOLD);
        Menu.text(
                "Defeat enemies and finish objectives to earn XP.", x + 14, y + 42, 11, Menu.MUTED);
        Menu.resourceBar(
                x + 14, y + 65, width - 28, 24, p.xp(), p.xpToNextLevel(), Menu.TEAL, "XP");
        row("MAX HEALTH", p.maxHealth(), p.nextLevelHealthGain(), x + 14, y + 104, width - 28);
        row("ATTACK POWER", p.damage(), p.nextLevelDamageGain(), x + 14, y + 160, width - 28);
    }

    private static void row(String label, int current, int gain, int x, int y, int width) {
        Menu.text(label + "  " + current, x, y, 12, Menu.TEXT);
        Menu.text("+" + gain + " NEXT LEVEL", x + width - 130, y, 11, Menu.GOLD);
        int bar = (width - 12) / 2;
        float maximum = current + gain;
        Menu.bar(x, y + 25, bar, current, maximum, Menu.TEAL);
        Menu.bar(x + bar + 12, y + 25, bar, maximum, maximum, Menu.GOLD);
        Menu.text("NOW", x, y + 37, 9, Menu.MUTED);
        Menu.text("LEVEL UP", x + bar + 12, y + 37, 9, Menu.MUTED);
    }

    private ProgressionSummary() {}
}
