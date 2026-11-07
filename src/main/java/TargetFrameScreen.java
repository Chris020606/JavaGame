import static com.raylib.Raylib.*;

import Entities.Enemy;

/** A focused target has a readable health frame and cast warning, independent of world labels. */
public final class TargetFrameScreen {
    public void draw(Enemy target, float distance, String focusKey) {
        if (target == null || GetScreenWidth() < 1000) return;
        int x = GetScreenWidth() - 290;
        int y = GetScreenHeight() - 222;
        Menu.panel(x, y, 274, 120);
        String name = target.boss() ? (target.king ? "HOLLOW KING" : "ASH WARDEN") : target.label();
        Menu.text(name, x + 12, y + 10, 16, target.boss() || target.elite ? Menu.GOLD : Menu.TEXT);
        Menu.text(
                Math.round(target.health)
                        + " / "
                        + Math.round(target.maxHealth)
                        + " HP  /  "
                        + Math.round(distance)
                        + "m",
                x + 12,
                y + 34,
                12,
                Menu.MUTED);
        Menu.bar(x + 12, y + 55, 250, target.health, target.maxHealth, Menu.RED);
        if (target.attackTime > .25f) {
            // Damage resolves with .25 seconds remaining; display time until impact, not clip end.
            String move =
                    target.healingTarget != null
                            ? "HEALING WARD"
                            : target.boss()
                                    ? target.moveLabel()
                                    : target.ranged() ? "RANGED SHOT" : "MELEE STRIKE";
            Color castColor = target.boss() && !target.bossMove.parryable ? Menu.RED : Menu.GOLD;
            Menu.text(
                    move + " / " + String.format("%.1fs", target.attackTime - .25f),
                    x + 12,
                    y + 76,
                    11,
                    castColor);
            Menu.bar(
                    x + 12,
                    y + 101,
                    250,
                    target.attackDuration() - target.attackTime,
                    Math.max(.01f, target.attackDuration() - .25f),
                    castColor);
        } else Menu.text(focusKey + " / CYCLE TARGET", x + 12, y + 82, 12, Menu.MUTED);
    }
}
