import java.util.Properties;

/**
 * Optional chapter goals. Rewards are paid once on chapter completion, never for replaying a save.
 */
public final class ChapterFeats {
    public enum Action {
        CHARGED_HIT,
        PARRY,
        EVADE,
        OBJECTIVE
    }

    public static final int REWARD = 15;
    private final int[][] actions = new int[4][4];
    private final int[] paid = new int[4];

    public void record(int chapter, Action action) {
        actions[chapter][action.ordinal()] = Math.min(99, actions[chapter][action.ordinal()] + 1);
    }

    public int goal(int chapter, int feat) {
        return feat < 2 ? 2 : Campaign.CHAPTERS[chapter].bossOnly() ? 3 : 1;
    }

    public int progress(int chapter, int feat) {
        int action = feat == 0 ? 0 : feat == 1 ? 1 : Campaign.CHAPTERS[chapter].bossOnly() ? 2 : 3;
        return Math.min(goal(chapter, feat), actions[chapter][action]);
    }

    public String title(int chapter, int feat) {
        return feat == 0
                ? "CHARGED RESOLVE"
                : feat == 1
                        ? "PERFECT COUNTER"
                        : Campaign.CHAPTERS[chapter].bossOnly()
                                ? "DANCE WITH DEATH"
                                : "FIELD WARDEN";
    }

    public String description(int chapter, int feat) {
        return feat == 0
                ? "Land 2 charged hits on enemies."
                : feat == 1
                        ? "Deflect 2 enemy attacks with a successful parry."
                        : Campaign.CHAPTERS[chapter].bossOnly()
                                ? "Evade 3 incoming hits during a dodge."
                                : "Complete an optional field objective.";
    }

    public boolean complete(int chapter, int feat) {
        return progress(chapter, feat) >= goal(chapter, feat);
    }

    public boolean paid(int chapter, int feat) {
        return (paid[chapter] & (1 << feat)) != 0;
    }

    public int completed(int chapter) {
        int result = 0;
        for (int i = 0; i < 3; i++) if (complete(chapter, i)) result++;
        return result;
    }

    public int awarded(int chapter) {
        return Integer.bitCount(paid[chapter]) * REWARD;
    }

    public int claim(int chapter) {
        int reward = 0;
        for (int i = 0; i < 3; i++)
            if (complete(chapter, i) && !paid(chapter, i)) {
                paid[chapter] |= 1 << i;
                reward += REWARD;
            }
        return reward;
    }

    public void store(Properties p) {
        for (int c = 0; c < 4; c++) {
            p.setProperty("feats." + c + ".paid", paid[c] + "");
            for (int a = 0; a < 4; a++) p.setProperty("feats." + c + "." + a, actions[c][a] + "");
        }
    }

    public static ChapterFeats restore(Properties p) {
        ChapterFeats result = new ChapterFeats();
        for (int c = 0; c < 4; c++) {
            String k = "feats." + c + ".";
            if (!p.containsKey(k + "paid")) continue;
            result.paid[c] = SaveGame.integer(p, k + "paid", 0, 7);
            for (int a = 0; a < 4; a++) result.actions[c][a] = SaveGame.integer(p, k + a, 0, 99);
            for (int f = 0; f < 3; f++)
                if (result.paid(c, f) && !result.complete(c, f))
                    throw new IllegalArgumentException("Unearned chapter feat");
        }
        return result;
    }
}
