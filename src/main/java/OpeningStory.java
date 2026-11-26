import static com.raylib.Raylib.*;

/** A deliberate, readable prologue before the player takes control of a new warden. */
public final class OpeningStory {
    public static final String[][] PAGES = {
        {
            "The last light",
            "The gate wardens sold their keys. When the seals broke, the Hollow Frost swept through"
                    + " the kingdom.",
            "Some families are still beyond the walls. Find them. Keep the Beacon burning long"
                    + " enough to bring them home."
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
                page == PAGES.length - 1 ? "Begin your watch [Enter]" : "Continue [Enter]",
                x,
                y + 274,
                15,
                Menu.GOLD);
    }

    private OpeningStory() {}
}
