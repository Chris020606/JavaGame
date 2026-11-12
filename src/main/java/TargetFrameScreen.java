import static com.raylib.Raylib.*;

import Entities.Enemy;

/** A focused target has a readable health frame and cast warning, independent of world labels. */
public final class TargetFrameScreen {
    private Enemy lastTarget;
    private float targetGhostHealth = -1;
    private float targetGhostDelay = 0;

    public void draw(Enemy target, float distance, String focusKey) {
        draw(target, distance, focusKey, false);
    }

    public void draw(Enemy target, float distance, String focusKey, boolean practice) {
        if (target == null || GetScreenWidth() < 1000) return;
        float dt = Math.max(0, Math.min(.1f, GetFrameTime()));
        if (target != lastTarget || targetGhostHealth < 0 || target.health >= targetGhostHealth) {
            targetGhostHealth = target.health;
            targetGhostDelay = 0;
            lastTarget = target;
        } else {
            targetGhostDelay += dt;
            if (targetGhostDelay >= 0.28f) {
                targetGhostHealth =
                        Math.max(
                                target.health,
                                targetGhostHealth
                                        - Math.max(25f, (targetGhostHealth - target.health) * 4.5f)
                                                * dt);
            }
        }

        int x = GetScreenWidth() - 290;
        int y = GetScreenHeight() - 222;
        Menu.panel(x, y, 274, 120);
        String badge = practice ? "" : target.boss() ? "[BOSS] " : target.elite ? "[ELITE] " : "";
        String name =
                practice
                        ? "PRACTICE PARTNER"
                        : target.boss()
                                ? (target.king ? "HOLLOW KING" : "ASH WARDEN")
                                : target.label();
        Menu.text(
                badge + name,
                x + 12,
                y + 10,
                15,
                target.boss() || target.elite ? Menu.GOLD : Menu.TEXT);
        // Training uses inflated health internally; never present it as a real enemy stat.
        String classification = practice ? "TRAINING" : (target.undead ? "[UNDEAD]" : "[HUMANOID]");
        Menu.text(
                practice
                        ? "SAFE PRACTICE / " + Math.round(distance) + "m"
                        : classification
                                + " "
                                + Math.round(target.health)
                                + " / "
                                + Math.round(target.maxHealth)
                                + " HP / "
                                + Math.round(distance)
                                + "m",
                x + 12,
                y + 34,
                11,
                Menu.MUTED);
        Menu.bar(x + 12, y + 55, 250, target.health, targetGhostHealth, target.maxHealth, Menu.RED);
        if (target.attackTime > .25f) {
            // Damage resolves with .25 seconds remaining; display time until impact, not clip end.
            String move =
                    target.healingTarget != null
                            ? "HEALING WARD [INTERRUPTIBLE]"
                            : target.boss()
                                    ? target.moveLabel()
                                            + (target.bossMove.parryable
                                                    ? " [PARRYABLE]"
                                                    : " [UNBLOCKABLE]")
                                    : target.ranged() ? "RANGED SHOT" : "MELEE STRIKE";
            Color castColor =
                    target.healingTarget != null
                            ? Menu.TEAL
                            : target.boss() && !target.bossMove.parryable ? Menu.RED : Menu.GOLD;
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
