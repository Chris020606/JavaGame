import static com.raylib.Raylib.*;

public class Main {
    public static void main(String[] args) throws java.io.IOException {
        boolean smokeTest = java.util.Arrays.asList(args).contains("--smoke-test");
        if (smokeTest && System.getProperty("outpost.dataDir")==null)
            System.setProperty("outpost.dataDir",java.nio.file.Files.createTempDirectory("outpost-smoke-").toString());
        boolean hidden=java.util.Arrays.asList(args).contains("--hidden");
        SetConfigFlags((hidden?FLAG_WINDOW_HIDDEN:0) | FLAG_WINDOW_RESIZABLE | FLAG_WINDOW_HIGHDPI | FLAG_MSAA_4X_HINT);
        InitWindow(1280, 800, "Outpost | Fight. Gather. Fortify.");
        SetWindowMinSize(1000, 680);
        SetExitKey(KEY_NULL);
        SetTargetFPS(60);
        Game game = null;
        try {
            game = new Game();
            if(java.util.Arrays.asList(args).contains("--playtest"))game.startPlaytest();
            int frames = 0;
            while (!WindowShouldClose() && !game.shouldQuit()) {
                game.update(GetFrameTime());
                BeginDrawing();
                ClearBackground(Menu.INK);
                game.render();
                EndDrawing();
                if (smokeTest && ++frames >= 120) break;
            }
        } finally {
            if (game != null) game.dispose();
            CloseWindow();
        }
    }
}
