import Entities.Player;
import Scene.SceneManager;
import com.raylib.Raylib;

import static com.raylib.Colors.*;
import static com.raylib.Raylib.*;

public class Main {
    public static void main(String args[]) {

        final int screenWidth = 1200;
        final int screenHeight = 1200;
        SetConfigFlags(FLAG_WINDOW_RESIZABLE | FLAG_MSAA_4X_HINT | FLAG_WINDOW_HIGHDPI);
        InitWindow(screenWidth, screenHeight, "Demo");
        SetTargetFPS(60);

        Game game = new Game();

        while (!WindowShouldClose()) {
            game.update(GetFrameTime());

            BeginDrawing();
            ClearBackground(Fade(BLUE, 0.3f));
            game.render();
            EndDrawing();

        }
        CloseWindow();
    }
}
