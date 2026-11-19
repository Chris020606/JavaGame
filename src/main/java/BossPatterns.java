import Entities.Difficulty;
import Entities.Enemy;

import java.util.ArrayList;
import java.util.List;

/** Locked ground targets leave a readable escape route; no marker follows a dodging player. */
public final class BossPatterns {
    public static List<BossHazard> sigils(Enemy boss, float targetX, float targetZ, int wave) {
        boolean easy = boss.difficulty == Difficulty.EASY;
        boolean secondPhase = boss.enraged() && !easy;
        int count = easy ? 3 : boss.king ? (secondPhase ? 7 : 5) : (secondPhase ? 5 : 3);
        float warning = Math.max(easy ? 1.15f : .9f, boss.attackDuration() - .25f);
        float interval = easy ? .26f : .18f;
        List<BossHazard> result = new ArrayList<>();
        float angle = (float) Math.atan2(targetZ - boss.z, targetX - boss.x);
        for (int i = 0; i < count; i++) {
            float x = targetX, z = targetZ;
            if (boss.king && i > 0) {
                // An incomplete crown leaves one sector open for escape.
                double a = angle + (i - 1) * Math.PI * 2 / count;
                x += (float) Math.cos(a) * 4.2f;
                z += (float) Math.sin(a) * 4.2f;
            } else if (!boss.king) {
                int offset = i == 0 ? 0 : (i + 1) / 2 * (i % 2 == 1 ? 1 : -1);
                x += (float) Math.cos(angle) * offset * 3.4f;
                z += (float) Math.sin(angle) * offset * 3.4f;
            }
            result.add(
                    new BossHazard(
                            BossHazard.Kind.SIGIL,
                            Math.max(-20, Math.min(20, x)),
                            Math.max(-20, Math.min(20, z)),
                            warning + i * interval,
                            boss.meleeDamage(wave) * .7f));
        }
        return result;
    }

    public static List<BossHazard> shockwaves(Enemy boss, int wave) {
        List<BossHazard> result = new ArrayList<>();
        int count = boss.enraged() && boss.difficulty != Difficulty.EASY ? (boss.king ? 3 : 2) : 1;
        for (int i = 0; i < count; i++)
            result.add(
                    new BossHazard(
                            BossHazard.Kind.SHOCKWAVE,
                            boss.x,
                            boss.z,
                            .3f + i * .85f,
                            boss.meleeDamage(wave) * (i == 0 ? .65f : .5f)));
        return result;
    }

    private BossPatterns() {}
}
