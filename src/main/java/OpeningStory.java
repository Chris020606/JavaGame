import static com.raylib.Raylib.*;

/** A deliberate, readable prologue before the player takes control of a new warden. */
public final class OpeningStory {
    public static final String[][] PAGES = {
        {
            "THE FALL OF THE REALM",
            "The outer gates were breached from within.",
            "Corrupt conspirators and outlaw syndicates bought the gate wardens and shattered the"
                + " ancient warding seals in pursuit of forbidden plunder. But their betrayal"
                + " unleashed the Hollow Frost: an ancient winter of souls that devoured the"
                + " capital and turned outlying towns to ash and ice. Surviving families fled into"
                + " the freezing valleys, hunted by frost raiders and undead legions."
        },
        {
            "THE LAST SANCTUARY",
            "Aethelgard still stands against the cold.",
            "Behind these stone ramparts, our survivors keep a fragile hope alive. Beyond them,"
                + " displaced refugees hide in ruined waystations and frostbitten camps across the"
                + " valley. You are an Outpost Warden. Your watch begins by braving the snows and"
                + " bringing our people safely home."
        },
        {
            "THE PRIMORDIAL BEACON",
            "The realm's last living solar ember.",
            "The Primordial Beacon carries Wardens into the wilds and guides rescued survivors back"
                    + " through the blizzard. Its solar fire is our sole defense against the Hollow"
                    + " Frost. Guard the Beacon with your life on every expedition: if its light is"
                    + " extinguished, the frost claims the sanctuary forever."
        },
        {
            "YOUR FIRST WATCH",
            "No Warden steps beyond the gates unprepared.",
            "Find your class mentor in the Citadel to claim your first weapon. Then report to"
                + " Commander Sean at the southern expedition gate. Search for lost citizens,"
                + " defend the Beacon against the frost hordes, and begin our fight to reclaim the"
                + " frozen realm."
        }
    };

    public static void draw(int page) {
        int w = GetScreenWidth(), h = GetScreenHeight();
        DrawRectangle(0, 0, w, h, Fade(Menu.INK, .92f));
        int width = Math.min(760, w - 48), x = (w - width) / 2, y = Math.max(24, (h - 400) / 2);
        Menu.text("OUTPOST / " + (page + 1) + " OF " + PAGES.length, x, y, 13, Menu.TEAL);
        Menu.text(PAGES[page][0], x, y + 48, 30, Menu.GOLD);
        Menu.wrapped(PAGES[page][1], x, y + 110, width, 23, Menu.TEXT);
        Menu.wrapped(PAGES[page][2], x, y + 176, width, 20, Menu.TEXT);
        Menu.text(
                page == PAGES.length - 1 ? "ENTER / BEGIN YOUR WATCH" : "ENTER / CONTINUE",
                x,
                y + 350,
                15,
                Menu.GOLD);
    }

    private OpeningStory() {}
}
