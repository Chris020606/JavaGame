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
        if (target == null || GetScreenWidth() < 1000) {
            lastTarget = null;
            targetGhostHealth = -1;
            return;
        }
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
        String badge = practice ? "" : target.boss() ? "BOSS / " : "";
        String name =
                practice
                        ? "PRACTICE PARTNER"
                        : target.boss()
                                ? (target.king ? "HOLLOW KING" : "ASH WARDEN")
                                : target.label();
        Color heading = target.boss() || target.elite ? Menu.GOLD : Menu.TEXT;
        Menu.fittedText(badge + name, x + 12, y + 9, 206, 17, heading);
        String range = Math.round(distance) + "m";
        Menu.text(range, x + 261 - Menu.measure(range, 13), y + 12, 13, Menu.MUTED);
        // Training partners use artificial health internally.
        Menu.text(
                practice ? "Safe practice" : target.undead ? "Undead" : "Humanoid",
                x + 12,
                y + 34,
                13,
                Menu.MUTED);
        if (!practice) {
            String hp = Math.round(target.health) + " / " + Math.round(target.maxHealth) + " HP";
            Menu.text(hp, x + 261 - Menu.measure(hp, 13), y + 34, 13, Menu.TEXT);
        }
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
                                    : (target.variant == 4
                                                    ? "HEX BOLT"
                                                    : target.ranged() ? "ARROW" : "STRIKE")
                                            + " / "
                                            + (target.targetsPlayer
                                                    ? "AT YOU"
                                                    : target.targetsTraveler
                                                            ? "AT OBJECTIVE"
                                                            : "AT BEACON");
            Color castColor =
                    target.healingTarget != null
                            ? Menu.TEAL
                            : target.boss() && !target.bossMove.parryable ? Menu.RED : Menu.GOLD;
            int tenths = (int) Math.ceil((target.attackTime - .25f) * 10);
            String timer = tenths / 10 + "." + tenths % 10 + "s";
            Menu.text(timer, x + 261 - Menu.measure(timer, 13), y + 74, 13, castColor);
            Menu.fittedText(move, x + 12, y + 74, 208, 14, castColor);
            Menu.bar(
                    x + 12,
                    y + 101,
                    250,
                    AttackTelegraph.progress(target.attackTime, target.attackDuration()),
                    1,
                    castColor);
        } else
            Menu.fittedText(
                    target.boss() && target.enraged()
                            ? "ENRAGED / " + focusKey + " TO CYCLE"
                            : focusKey + " / CYCLE TARGET",
                    x + 12,
                    y + 82,
                    250,
                    12,
                    target.boss() && target.enraged() ? Menu.RED : Menu.MUTED);
    }
}
