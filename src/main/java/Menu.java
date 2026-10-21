import static com.raylib.Raylib.*;

public class Menu {
    public static final Color INK = color(14, 22, 30), PANEL = color(24, 36, 46),
        LINE = color(53, 73, 83), TEXT = color(235, 239, 229), MUTED = color(153, 174, 178),
        GOLD = color(235, 190, 101), TEAL = color(86, 200, 174), RED = color(224, 105, 100);
    public static Color color(int r, int g, int b) {
        return new Color().r((byte)r).g((byte)g).b((byte)b).a((byte)255);
    }
    public static void panel(int x, int y, int w, int h) {
        DrawRectangle(x, y, w, h, PANEL);
        DrawRectangleLines(x, y, w, h, LINE);
    }
    public static void text(String text, int x, int y, int size, Color color) {
        DrawText(text, x, y, size, color);
    }
    public static void bar(int x, int y, int w, float value, float max, Color color) {
        DrawRectangle(x, y, w, 8, LINE);
        DrawRectangle(x, y, (int)(w * Math.max(0, Math.min(1, value / max))), 8, color);
    }
    public static boolean button(String label, int x, int y, int w, boolean enabled) {
        Vector2 mouse = GetMousePosition();
        boolean hover = mouse.x() >= x && mouse.x() <= x+w && mouse.y() >= y && mouse.y() <= y+44;
        DrawRectangle(x, y, w, 44, enabled ? (hover ? TEAL : GOLD) : LINE);
        text(label, x + (w - MeasureText(label, 18))/2, y+13, 18, enabled ? INK : MUTED);
        return enabled && hover && IsMouseButtonPressed(MOUSE_BUTTON_LEFT);
    }
}
