import static com.raylib.Raylib.*;

/** A slider owns its pointer capture, so dragging outside the track still reaches either end. */
final class SettingsSlider {
    private boolean held;

    float draw(String label, int x, int y, int width, float value) {
        Vector2 mouse = GetMousePosition();
        float result =
                update(
                        value,
                        mouse.x(),
                        mouse.y(),
                        IsMouseButtonPressed(MOUSE_BUTTON_LEFT),
                        IsMouseButtonDown(MOUSE_BUTTON_LEFT),
                        x,
                        y,
                        width);
        Menu.text(label, x, y, 14, Menu.TEXT);
        String percent = Math.round(result * 100) + "%";
        Menu.text(percent, x + width - Menu.measure(percent, 13), y, 13, Menu.GOLD);
        DrawRectangle(x, y + 28, width, 5, Menu.color(48, 58, 70));
        DrawRectangle(x, y + 28, Math.round(width * result), 5, Menu.GOLD);
        int thumb = x + Math.round(width * result);
        DrawRectangle(thumb - 4, y + 23, 8, 15, held ? Menu.TEXT : Menu.GOLD);
        return result;
    }

    float update(
            float value,
            float mouseX,
            float mouseY,
            boolean pressed,
            boolean down,
            int x,
            int y,
            int width) {
        if (!down) held = false;
        if (pressed
                && mouseX >= x - 6
                && mouseX <= x + width + 6
                && mouseY >= y + 17
                && mouseY <= y + 44) held = true;
        return held && down ? Math.max(0, Math.min(1, (mouseX - x) / Math.max(1, width))) : value;
    }
}
