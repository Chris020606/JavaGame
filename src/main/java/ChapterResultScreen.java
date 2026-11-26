import static com.raylib.Raylib.*;

/** Presentation of earned campaign results. Does not grant rewards or advance the chapter. */
final class ChapterResultScreen {
    static void draw(Progression progress, int chapter, boolean victory, int x, int y) {
        Campaign.Chapter completed = Campaign.CHAPTERS[chapter];
        Menu.icon("crown", x, y + 2, 30, Menu.GOLD);
        Menu.fittedText(
                victory ? "THE VALLEY SEES DAWN" : "CHAPTER SECURED",
                x + 46,
                y + 2,
                594,
                30,
                Menu.GOLD);
        Menu.text(completed.title(), x, y + 54, 22, Menu.TEXT);
        Menu.wrapped(completed.aftermath(), x, y + 88, 640, 16, Menu.MUTED);

        int integrity =
                Math.round(100 * progress.baseHealth() / Math.max(1, progress.maxBaseHealth()));
        card(
                x,
                y + 160,
                "shield",
                "BEACON INTEGRITY",
                Math.max(0, Math.min(100, integrity)) + "%",
                "The light held",
                Menu.TEAL);
        card(
                x + 218,
                y + 160,
                "heart",
                "CITIZENS HOME",
                progress.rescuedSurvivors() + " / " + CitadelNpc.RESCUED_SURVIVORS.length,
                "Sheltered in Aethelgard",
                Menu.TEAL);
        card(
                x + 436,
                y + 160,
                "crown",
                "CHAPTER FEATS",
                progress.chapterFeats().completed(chapter) + " / 3",
                "+" + progress.chapterFeats().awarded(chapter) + " bonus crystals",
                Menu.GOLD);

        if (!victory) {
            Campaign.Chapter next = Campaign.CHAPTERS[Math.min(3, chapter + 1)];
            Menu.text("NEXT DESTINATION", x, y + 254, 11, Menu.MUTED);
            Menu.fittedText(
                    next.title()
                            + (next.bossOnly() ? " / BOSS DUEL" : " / " + next.waves() + " WAVES"),
                    x,
                    y + 275,
                    640,
                    19,
                    Menu.TEXT);
        } else {
            Menu.text("THE LONG NIGHT IS OVER", x, y + 254, 18, Menu.TEXT);
            Menu.fittedText(
                    progress.kills()
                            + " enemies defeated / "
                            + progress.crystals()
                            + " crystals carried home",
                    x,
                    y + 280,
                    640,
                    14,
                    Menu.MUTED);
        }
        Menu.text(
                completed.bossOnly()
                        ? "BOSS REWARD / +120 CRYSTALS / +300 XP"
                        : "Your next expedition is ready.",
                x,
                y + 316,
                14,
                Menu.TEAL);
    }

    private static void card(
            int x, int y, String icon, String title, String value, String caption, Color accent) {
        Menu.frame(x, y, 204, 78, Menu.color(17, 27, 39), Fade(Menu.LINE, .65f));
        Menu.icon(icon, x + 12, y + 12, 17, accent);
        Menu.text(title, x + 37, y + 12, 10, Menu.MUTED);
        Menu.text(value, x + 12, y + 33, 22, accent);
        Menu.fittedText(caption, x + 67, y + 44, 126, 10, Menu.TEXT);
    }
}
