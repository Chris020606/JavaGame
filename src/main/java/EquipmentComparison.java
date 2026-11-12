import static com.raylib.Raylib.*;

/** Two values on one scale make a selected item's effect readable before equipping. */
public final class EquipmentComparison {
    public static void draw(String stat, int equipped, int selected, int x, int y, int width) {
        int half = (width - 20) / 2;
        int delta = selected - equipped;
        Color change = delta > 0 ? Menu.TEAL : delta < 0 ? Menu.RED : Menu.MUTED;
        Menu.text("CURRENT " + stat + "  " + equipped, x, y, 11, Menu.MUTED);
        Menu.text(
                "SELECTED  " + selected + String.format("  (%+d)", delta),
                x + half + 20,
                y,
                11,
                change);
        float maximum = Math.max(1, Math.max(equipped, selected));
        Menu.bar(x, y + 20, half, equipped, maximum, Menu.MUTED);
        Menu.bar(x + half + 20, y + 20, half, selected, maximum, change);
    }

    private EquipmentComparison() {}
}
