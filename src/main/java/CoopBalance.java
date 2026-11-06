import Entities.Difficulty;

/** Tuned for two active heroes: more bodies, tougher bosses, unchanged readable attack timing. */
public final class CoopBalance {
    public static int count(int wave, Difficulty difficulty) {
        return Campaign.chapter(wave).bossOnly()
                ? 1
                : Math.min(54, Math.round(difficulty.count(Campaign.enemies(wave), false) * 1.4f));
    }

    public static float healthScale(boolean boss) {
        return boss ? 2f : 1.45f;
    }

    public static final float BASE_HEALTH = 520,
            PLAYER_DAMAGE_FACTOR = .75f,
            HIT_GRACE = .7f,
            VOLLEY_INTERVAL = .65f;

    private CoopBalance() {}
}
