import java.util.List;

/** Immutable host snapshots; networking and rendering never mutate the simulation. */
public record CoopState(
        long tick,
        Phase phase,
        int chapter,
        int wave,
        int queued,
        float rest,
        float baseHealth,
        float baseMax,
        int kills,
        int difficulty,
        boolean paused,
        List<Hero> heroes,
        List<Mob> enemies,
        List<Bolt> bolts,
        List<Danger> dangers) {
    public enum Phase {
        LOBBY,
        PLAYING,
        INTERMISSION,
        DEFEAT,
        VICTORY
    }

    public record Hero(
            int slot,
            String name,
            int hero,
            boolean connected,
            boolean ready,
            boolean paused,
            float x,
            float z,
            float facing,
            float health,
            float maxHealth,
            float stamina,
            float maxStamina,
            float vx,
            float vz,
            float attack,
            float duration,
            float impact,
            boolean heavy,
            int combo,
            float charge,
            float dodge,
            float parry,
            float hurt,
            float revive,
            float repair,
            int potions,
            int credits,
            int vitality,
            int power,
            int endurance) {}

    public record Mob(
            int id,
            int wave,
            int variant,
            float personality,
            float x,
            float z,
            float facing,
            float health,
            float maxHealth,
            float attack,
            float duration,
            float attackX,
            float attackZ,
            int move,
            float charge,
            float stun,
            float clock,
            float death) {}

    public record Bolt(
            int id, float x, float z, float vx, float vz, boolean magic, boolean hostile) {}

    public record Danger(
            int id, int kind, float x, float z, float delay, float radius, float age) {}
}
