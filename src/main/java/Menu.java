import static com.raylib.Raylib.*;

public class Menu {
    public static final Color INK = color(12, 17, 28),
            PANEL = color(25, 34, 50),
            LINE = color(116, 103, 76),
            TEXT = color(246, 231, 198),
            MUTED = color(174, 178, 155),
            GOLD = color(224, 177, 87),
            TEAL = color(99, 188, 156),
            RED = color(213, 102, 84);

    public static Color color(int r, int g, int b) {
        return new Color().r((byte) r).g((byte) g).b((byte) b).a((byte) 255);
    }

    private static Font font;

    private static Font font() {
        if (font == null) {
            font =
                    LoadFontEx(
                            assets.AssetPaths.path("resources/outpost/interface.ttf"),
                            96,
                            (int[]) null,
                            0);
            SetTextureFilter(font.texture(), TEXTURE_FILTER_BILINEAR);
        }
        return font;
    }

    public static int measure(String text, int size) {
        return Math.round(MeasureTextEx(font(), text, size * 1.15f, .5f).x());
    }

    public static void center(String text, int x, int y, int size, Color color) {
        text(text, x - measure(text, size) / 2, y, size, color);
    }

    public static void close() {
        if (font != null) {
            UnloadFont(font);
            font = null;
        }
    }

    public static void panel(int x, int y, int w, int h) {
        frame(x, y, w, h, Fade(PANEL, .97f), LINE);
        Color rivet = Fade(GOLD, .65f);
        DrawRectangle(x + 7, y + 7, 2, 2, rivet);
        DrawRectangle(x + w - 9, y + 7, 2, 2, rivet);
        DrawRectangle(x + 7, y + h - 9, 2, 2, rivet);
        DrawRectangle(x + w - 9, y + h - 9, 2, 2, rivet);
    }

    public static void frame(int x, int y, int w, int h, Color fill, Color edge) {
        DrawRectangle(x + 3, y + 4, w, h, Fade(INK, .65f));
        DrawRectangle(x, y, w, h, INK);
        DrawRectangleGradientV(x + 2, y + 2, w - 4, h - 4, fill, Fade(fill, .78f));
        DrawRectangleLines(x, y, w, h, edge);
        DrawRectangleLines(x + 3, y + 3, w - 6, h - 6, Fade(edge, .22f));
        DrawLine(x + 2, y + 2, x + w - 3, y + 2, Fade(TEXT, .16f));
        DrawLine(x + 2, y + 2, x + 2, y + h - 3, Fade(TEXT, .13f));
        DrawLine(x + 3, y + h - 3, x + w - 3, y + h - 3, Fade(INK, .7f));
    }

    public static void slot(int x, int y, int size, boolean selected, boolean equipped) {
        frame(x, y, size, size, color(23, 30, 29), selected ? GOLD : equipped ? TEAL : LINE);
        DrawLine(x + 3, y + 3, x + size - 4, y + 3, INK);
        DrawLine(x + 3, y + 3, x + 3, y + size - 4, INK);
        if (selected) DrawRectangleLines(x - 2, y - 2, size + 4, size + 4, Fade(GOLD, .65f));
    }

    public static void ember(int x, int y, int size, float amount, Color color) {
        DrawPoly(new Vector2().x(x + size * .5f).y(y + size * .5f), 4, size * .52f, 0, LINE);
        if (amount > 0)
            DrawPoly(
                    new Vector2().x(x + size * .5f).y(y + size * .5f),
                    4,
                    size * .37f * (float) Math.sqrt(Math.min(1, amount)),
                    0,
                    color);
        DrawRectangle(x + size / 2 - 1, y + size / 4, 2, 2, Fade(TEXT, Math.min(1, amount) * .65f));
    }

    public static void resourceBar(
            int x,
            int y,
            int width,
            int height,
            float current,
            float maximum,
            Color fill,
            String label) {
        resourceBar(x, y, width, height, current, current, maximum, fill, label);
    }

    public static void resourceBar(
            int x,
            int y,
            int width,
            int height,
            float current,
            float ghost,
            float maximum,
            Color fill,
            String label) {
        frame(x, y, width, height, INK, GOLD);
        float m = Math.max(1, maximum);
        int ghostAmount =
                Math.round((width - 6) * Math.max(0, Math.min(1, Math.max(current, ghost) / m)));
        if (ghostAmount > 0 && ghost > current) {
            DrawRectangleGradientV(
                    x + 3, y + 3, ghostAmount, height - 6, Fade(GOLD, .65f), Fade(GOLD, .40f));
        }
        int amount = Math.round((width - 6) * Math.max(0, Math.min(1, current / m)));
        if (amount > 0) {
            DrawRectangleGradientV(x + 3, y + 3, amount, height - 6, fill, Fade(fill, .66f));
            DrawLine(x + 4, y + 4, x + Math.max(4, amount), y + 4, Fade(TEXT, .35f));
        }
        text(label, x + 10, y + (height - 16) / 2, 13, TEXT);
        String value = Math.round(current) + " / " + Math.round(maximum);
        text(value, x + width - measure(value, 14) - 10, y + (height - 17) / 2, 14, TEXT);
    }

    public static void text(String text, int x, int y, int size, Color color) {
        DrawTextEx(font(), text, new Vector2().x(x).y(y), size * 1.15f, .5f, color);
    }

    public static void wrapped(String text, int x, int y, int width, int size, Color color) {
        String line = "";
        int row = y;
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (measure(next, size) > width && !line.isEmpty()) {
                text(line, x, row, size, color);
                row += size + 5;
                line = word;
            } else line = next;
        }
        if (!line.isEmpty()) text(line, x, row, size, color);
    }

    public static int wrappedHeight(String text, int width, int size) {
        if (text == null || text.isEmpty()) return 0;
        String line = "";
        int lines = 0;
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (measure(next, size) > width && !line.isEmpty()) {
                lines++;
                line = word;
            } else {
                line = next;
            }
        }
        if (!line.isEmpty()) lines++;
        return lines * (size + 5);
    }

    /** Small vector pictograms remain crisp at both display scales. */
    private static final Vector2 iconFrom = new Vector2(), iconTo = new Vector2();

    private static void iconLine(int x1, int y1, int x2, int y2, Color color) {
        DrawLineEx(iconFrom.x(x1).y(y1), iconTo.x(x2).y(y2), 2, color);
    }

    public static void icon(String kind, int x, int y, int size, Color color) {
        int u = Math.max(1, size / 8), cx = x + size / 2, cy = y + size / 2;
        switch (kind) {
            case "charge" -> {
                iconLine(cx, y, x + u, cy + u, color);
                iconLine(x + u, cy + u, cx, cy + u, color);
                iconLine(cx, cy + u, cx, y + size, color);
                iconLine(cx, y + size, x + size - u, cy - u, color);
                iconLine(x + size - u, cy - u, cx, cy - u, color);
                iconLine(cx, cy - u, cx, y, color);
            }
            case "lunge" -> {
                for (int i = 0; i < 2; i++) {
                    int dx = x + i * size / 3;
                    iconLine(dx, y + u, dx + size / 2, cy, color);
                    iconLine(dx + size / 2, cy, dx, y + size - u, color);
                }
            }
            case "crystal" -> {
                iconLine(cx, y, x + size, cy, color);
                iconLine(x + size, cy, cx, y + size, color);
                iconLine(cx, y + size, x, cy, color);
                iconLine(x, cy, cx, y, color);
                iconLine(cx, y, cx, y + size, color);
            }
            case "potion" -> {
                DrawRectangleLines(cx - u, y, 2 * u, 2 * u, color);
                DrawRectangleLines(x + u, y + 2 * u, size - 2 * u, size - 2 * u, color);
                DrawRectangle(cx - u, y, 2 * u, u, color);
                DrawRectangle(x + 2 * u, cy, size - 4 * u, size / 2 - u, color);
            }
            case "dodge" -> {
                iconLine(x, cy, cx, y, color);
                iconLine(cx, y, x + size, cy, color);
                iconLine(x + size, cy, cx, y + size, color);
                iconLine(cx, y + size, x, cy, color);
                iconLine(x + 2 * u, cy, x + size - 2 * u, cy, color);
            }
            case "lock" -> {
                DrawRectangleLines(x + u, cy, size - 2 * u, size / 2, color);
                DrawRectangleLines(x + 2 * u, y, size - 4 * u, size / 2, color);
            }
            case "head" -> {
                DrawRectangleLines(x, y + u, size, size - 2 * u, color);
                iconLine(x, cy, x + size, cy, color);
                iconLine(cx, cy, cx, y + size, color);
            }
            case "chest" -> {
                DrawRectangleLines(x + u, y + u, size - 2 * u, size - u, color);
                iconLine(x, cy, x + u, y + u, color);
                iconLine(x + size, cy, x + size - u, y + u, color);
            }
            case "legs" -> {
                DrawRectangleLines(x + u, y, size - 2 * u, size, color);
                iconLine(cx, cy, cx, y + size, color);
            }
            case "boots" -> {
                // Two matching boot silhouettes: cuff, shaft, toe and sole.
                for (int offset : new int[] {0, size / 2}) {
                    int bx = x + offset;
                    iconLine(bx + u, y + u, bx + 3 * u, y + u, color);
                    iconLine(bx + u, y + u, bx + u, y + 6 * u, color);
                    iconLine(bx + 3 * u, y + u, bx + 3 * u, y + 4 * u, color);
                    iconLine(bx + 3 * u, y + 4 * u, bx + 4 * u, y + 5 * u, color);
                    iconLine(bx + 4 * u, y + 5 * u, bx + 4 * u, y + 6 * u, color);
                    iconLine(bx + u, y + 6 * u, bx + 4 * u, y + 6 * u, color);
                }
            }
            case "crossbow" -> {
                iconLine(cx, y, cx, y + size, color);
                iconLine(x, y + u, cx, cy, color);
                iconLine(cx, cy, x + size, y + u, color);
                iconLine(x, y + u, x + size, y + u, color);
                DrawRectangle(cx - u, cy, 2 * u, size / 2, color);
            }
            case "bow" -> {
                iconLine(x + u, y, cx + 2 * u, cy, color);
                iconLine(cx + 2 * u, cy, x + u, y + size, color);
                iconLine(x + u, y, x + u, y + size, color);
                iconLine(x, cy, x + size, cy, color);
                iconLine(x + size, cy, x + size - u, cy - u, color);
                iconLine(x + size, cy, x + size - u, cy + u, color);
            }
            case "staff" -> {
                DrawRectangle(cx - u / 2, cy, u, size / 2, color);
                DrawCircle(cx, y + 2 * u, 2 * u, color);
                DrawCircle(cx, y + 2 * u, u, INK);
                iconLine(cx - 3 * u, y + 2 * u, cx + 3 * u, y + 2 * u, color);
                iconLine(cx, y, cx, y + 4 * u, color);
            }
            case "heart" -> {
                DrawRectangle(cx - u, y, u * 2, size, color);
                DrawRectangle(x, cy - u, size, u * 2, color);
            }
            case "sword" -> {
                iconLine(x + u, y + size - u, x + size - u, y + u, color);
                iconLine(x + 2 * u, y + size - u, x + size - u, y + u, color);
                iconLine(x + u, y + size - 2 * u, x + size - u, y + u, color);
                iconLine(x + 2 * u, y + size - u, x + size - u, y + 2 * u, color);
                iconLine(x, y + size - 3 * u, x + 3 * u, y + size, color);
            }
            case "bag" -> {
                DrawRectangleLines(x + u, y + 3 * u, size - 2 * u, size - 3 * u, color);
                DrawRectangleLines(cx - 2 * u, y, u * 4, u * 3, color);
                iconLine(x + u, cy + u, x + size - u, cy + u, color);
            }
            case "save" -> {
                DrawRectangleLines(x, y, size, size, color);
                DrawRectangle(x + u, y, u * 4, u * 3, color);
                DrawRectangleLines(x + u, cy + u, size - 2 * u, size / 2 - u, color);
            }
            case "book" -> {
                DrawRectangleLines(x, y + u, size, size - 2 * u, color);
                iconLine(cx, y + u, cx, y + size - u, color);
                iconLine(x + u, y + 3 * u, cx - u, y + 3 * u, color);
                iconLine(cx + u, y + 3 * u, x + size - u, y + 3 * u, color);
            }
            case "gear" -> {
                DrawCircleLines(cx, cy, size / 3f, color);
                DrawCircle(cx, cy, u, color);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    iconLine(
                            cx + (int) (Math.cos(a) * size * .3),
                            cy + (int) (Math.sin(a) * size * .3),
                            cx + (int) (Math.cos(a) * size * .5),
                            cy + (int) (Math.sin(a) * size * .5),
                            color);
                }
            }
            case "shield" -> {
                iconLine(x, y + u, x + size, y + u, color);
                iconLine(x, y + u, x + u, y + size - 2 * u, color);
                iconLine(x + size, y + u, x + size - u, y + size - 2 * u, color);
                iconLine(x + u, y + size - 2 * u, cx, y + size, color);
                iconLine(cx, y + size, x + size - u, y + size - 2 * u, color);
            }
            case "crown" -> {
                iconLine(x, y + u, x + u, y + size - u, color);
                iconLine(x + size, y + u, x + size - u, y + size - u, color);
                iconLine(x, y + u, cx, cy, color);
                iconLine(cx, cy, cx, y, color);
                iconLine(cx, y, x + size, cy, color);
                iconLine(x + u, y + size - u, x + size - u, y + size - u, color);
            }
            default -> {
                iconLine(x, cy, x + size, cy, color);
                iconLine(x + size, cy, cx, y + u, color);
                iconLine(x + size, cy, cx, y + size - u, color);
            }
        }
    }

    private static String buttonIcon(String label) {
        if (label.contains("POTION") || label.contains("Heal")) return "potion";
        if (label.contains("SAVE")) return "save";
        if (label.contains("INVENTORY") || label.contains("GEAR")) return "bag";
        if (label.contains("SETTINGS")) return "gear";
        if (label.contains("CHRONICLE")) return "book";
        if (label.contains("UPGRADE")) return "shield";
        if (label.contains("NEW GAME") || label.contains("BEGIN")) return "sword";
        if (label.contains("CONTINUE")
                || label.contains("RETURN")
                || label.contains("RESUME")
                || label.contains("LOAD GAME")) return "arrow";
        return null;
    }

    public static void bar(int x, int y, int w, float value, float max, Color color) {
        bar(x, y, w, value, value, max, color);
    }

    public static void bar(int x, int y, int w, float value, float ghost, float max, Color color) {
        DrawRectangle(x - 1, y - 1, w + 2, 10, INK);
        DrawRectangle(x, y, w, 8, LINE);
        float m = Math.max(1, max);
        int ghostFill = (int) (w * Math.max(0, Math.min(1, Math.max(value, ghost) / m)));
        if (ghostFill > 0 && ghost > value) {
            DrawRectangle(x, y, ghostFill, 8, Fade(GOLD, .65f));
        }
        int fill = (int) (w * Math.max(0, Math.min(1, value / m)));
        DrawRectangle(x, y, fill, 8, color);
        DrawLine(x, y, x + fill, y, Fade(TEXT, .3f));
    }

    public static boolean primaryButton(String label, int x, int y, int width) {
        Vector2 mouse = GetMousePosition();
        boolean hover =
                mouse.x() >= x && mouse.x() <= x + width && mouse.y() >= y && mouse.y() <= y + 44;
        frame(x, y, width, 44, hover ? color(239, 198, 113) : color(195, 151, 72), GOLD);
        icon("sword", x + 16, y + 12, 20, INK);
        center(label, x + width / 2 + 8, y + 12, 19, INK);
        return hover && IsMouseButtonPressed(MOUSE_BUTTON_LEFT);
    }

    public static boolean button(String label, int x, int y, int w, boolean enabled) {
        Vector2 mouse = GetMousePosition();
        boolean hover =
                mouse.x() >= x && mouse.x() <= x + w && mouse.y() >= y && mouse.y() <= y + 44;
        frame(
                x,
                y,
                w,
                44,
                enabled ? (hover ? color(56, 73, 99) : color(33, 46, 68)) : color(24, 29, 39),
                enabled ? (hover ? GOLD : LINE) : LINE);
        DrawLine(x + 4, y + 40, x + w - 5, y + 40, enabled ? Fade(GOLD, .45f) : INK);
        Color foreground = enabled ? TEXT : MUTED;
        String kind = w >= 140 ? buttonIcon(label) : null;
        if (kind != null) icon(kind, x + 12, y + 14, 16, foreground);
        int inset = kind == null ? 0 : 24;
        int textSize = 18;
        while (textSize > 12 && measure(label, textSize) > w - inset - 16) textSize--;
        text(
                label,
                x + inset + (w - inset - measure(label, textSize)) / 2,
                y + 13,
                textSize,
                foreground);
        return enabled && hover && IsMouseButtonPressed(MOUSE_BUTTON_LEFT);
    }
}
