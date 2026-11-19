import static com.raylib.Raylib.*;

/** A deliberate, readable prologue before the player takes control of a new warden. */
public final class OpeningStory {
    public static final String[][] PAGES = {
        {
            "THE LAST LIGHT",
            "Corrupt wardens broke the seals. The Hollow Frost took the realm.",
            "Aethelgard is our last refuge. Claim your weapon, rescue the missing, and defend the"
                    + " Beacon that brings them home."
        }
    };

    public static void draw(int page) {
        int w = GetScreenWidth(), h = GetScreenHeight();
        DrawRectangle(0, 0, w, h, Fade(Menu.INK, .92f));
        int width = Math.min(700, w - 48), x = (w - width) / 2, y = Math.max(24, (h - 300) / 2);
        Menu.text("AETHELGARD", x, y, 13, Menu.TEAL);
        Menu.text(PAGES[page][0], x, y + 48, 30, Menu.GOLD);
        Menu.wrapped(PAGES[page][1], x, y + 100, width, 21, Menu.TEXT);
        Menu.wrapped(PAGES[page][2], x, y + 168, width, 20, Menu.TEXT);
        Menu.text(
                page == PAGES.length - 1 ? "ENTER / BEGIN YOUR WATCH" : "ENTER / CONTINUE",
                x,
                y + 274,
                15,
                Menu.GOLD);
    }

    private OpeningStory() {}
}
