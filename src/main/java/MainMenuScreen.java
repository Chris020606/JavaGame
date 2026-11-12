import static com.raylib.Raylib.*;

/** Draws the title menu and reports intent. It does not load files or change game state. */
public final class MainMenuScreen {
    public enum Action {
        NONE,
        PLAY_TOGETHER,
        NEW_GAME,
        LOAD_GAME,
        QUIT,
        SETTINGS,
        CHRONICLE,
        PLAYER_HISTORY
    }

    public Action draw(String playerName, boolean hasSave, String message) {
        int x = (GetScreenWidth() - 640) / 2;
        int y = (GetScreenHeight() - 420) / 2;
        Action action = Action.NONE;
        // Keep actions on one quiet surface, with the title outside the button panel.
        DrawRectangleGradientV(
                0, 0, GetScreenWidth(), GetScreenHeight(), Fade(Menu.INK, .35f), Menu.INK);
        Menu.panel(x - 30, y + 162, 700, 346);
        DrawLine(x, y + 268, x + 620, y + 268, Menu.GOLD);
        Menu.text("THE LAST LIGHT OF THE VALLEY", x, y, 18, Menu.TEAL);
        Menu.text("OUTPOST", x, y + 42, 72, Menu.TEXT);
        Menu.text("v" + BuildInfo.VERSION, x + 325, y + 96, 12, Menu.GOLD);
        Menu.text("Guard the light. Bring back the dawn.", x, y + 132, 24, Menu.GOLD);
        if (Menu.button("PLAY TOGETHER", x, y + 184, 620, true)) action = Action.PLAY_TOGETHER;
        Menu.text("Welcome back, " + playerName, x, y + 244, 16, Menu.TEXT);
        if (Menu.primaryButton("NEW GAME", x, y + 285, 300)) action = Action.NEW_GAME;
        if (Menu.button("CONTINUE", x + 320, y + 285, 300, hasSave)) action = Action.LOAD_GAME;
        if (Menu.button("QUIT", x + 320, y + 402, 300, true)) action = Action.QUIT;
        if (Menu.button("SETTINGS", x + 320, y + 344, 300, true)) action = Action.SETTINGS;
        if (Menu.button("CHRONICLE", x, y + 402, 300, true)) action = Action.CHRONICLE;
        if (Menu.button("PLAYERS & SAVES", x, y + 344, 300, true)) action = Action.PLAYER_HISTORY;
        if (!message.isEmpty()) Menu.text(message, x, y + 462, 15, Menu.GOLD);
        return action;
    }
}
