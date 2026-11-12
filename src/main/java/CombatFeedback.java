import static com.raylib.Raylib.*;

import java.util.ArrayList;

/** Short-lived world anchored damage/reward labels. Simulation time pauses with combat. */
public final class CombatFeedback {
    private static final class Popup {
        final float x, z;
        final String text;
        final Color color;
        final float lane;
        final boolean crit;
        final float initialTime;
        final int critWidth, largeWidth, smallWidth;
        float time;

        Popup(float x, float z, String text, Color color, float lane) {
            this(x, z, text, color, lane, false);
        }

        Popup(float x, float z, String text, Color color, float lane, boolean crit) {
            this.x = x;
            this.z = z;
            this.text = text;
            this.color = color;
            this.lane = lane;
            this.crit = crit;
            this.initialTime = crit ? 1.35f : 1.05f;
            this.time = this.initialTime;
            this.critWidth = Menu.measure(text, 32);
            this.largeWidth = Menu.measure(text, 26);
            this.smallWidth = Menu.measure(text, 21);
        }
    }

    private final ArrayList<Popup> labels = new ArrayList<>();
    private int serial;
    private final Vector3 anchor = new Vector3();

    public void add(float x, float z, String text, Color color) {
        add(x, z, text, color, false);
    }

    public void add(float x, float z, String text, Color color, boolean crit) {
        if (labels.size() >= 64) labels.remove(0);
        labels.add(new Popup(x, z, text, color, (serial++ % 3 - 1) * .28f, crit));
    }

    public void update(float dt) {
        labels.removeIf(p -> {
            p.time -= dt;
            return p.time <= 0;
        });
    }

    public void clear() {
        labels.clear();
        serial = 0;
    }

    public void draw(Camera3D camera) {
        for (Popup p : labels) {
            float age = p.initialTime - p.time;
            float alpha = Math.min(1.0f, p.time * 2.8f);
            if (p.crit) {
                // WoW-style crit arc: leaps higher and arches slightly outward
                float yUp = 2.4f + age * 2.2f - (age * age * 0.9f);
                float xDrift = p.x + p.lane * (1.0f + age * 0.9f);
                Vector3 world = anchor.x(xDrift).y(yUp).z(p.z);
                Vector2 point = GetWorldToScreen(world, camera);
                int size = age < .18f ? 32 : age < .45f ? 26 : 22;
                int width = age < .18f ? p.critWidth : age < .45f ? p.largeWidth : p.smallWidth;
                int x = (int) point.x() - width / 2, y = (int) point.y();
                Menu.text(p.text, x + 2, y + 2, size, Fade(Menu.INK, alpha));
                Menu.text(p.text, x - 1, y - 1, size, Fade(Menu.GOLD, alpha * 0.65f));
                Menu.text(p.text, x, y, size, Fade(p.color, alpha));
            } else {
                Vector3 world = anchor.x(p.x + p.lane).y(2.4f + age * 1.3f).z(p.z);
                Vector2 point = GetWorldToScreen(world, camera);
                int size = age < .12f ? 26 : 21;
                int x = (int) point.x() - (age < .12f ? p.largeWidth : p.smallWidth) / 2,
                        y = (int) point.y();
                Menu.text(p.text, x + 2, y + 2, size, Fade(Menu.INK, alpha));
                Menu.text(p.text, x, y, size, Fade(p.color, alpha));
            }
        }
    }
}
