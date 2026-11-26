package Entities;

/** Longer boss encounters keep full damage windows; no invulnerability or passive regeneration. */
public final class BossBalance {
    public static float endurance(Difficulty difficulty, boolean king) {
        float scale =
                switch (difficulty) {
                    case EASY -> 2.4f;
                    case NORMAL -> 3.2f;
                    case HARD -> 3.6f;
                    case EXTREME -> 4.0f;
                };
        return scale * (king ? 1.25f : 1);
    }

    public static float recovery(Difficulty difficulty, boolean king, boolean enraged) {
        if (difficulty == Difficulty.EASY) return enraged ? .65f : 1.1f;
        return enraged ? (king ? .36f : .42f) : .85f;
    }

    private BossBalance() {}
}
