/** Inputs are intentions only: the host owns position, health, damage and rewards. */
public record CoopInput(long sequence, float moveX, float moveZ, float aim, int buttons) {
    public static final int LIGHT = 1,
            HEAVY = 2,
            DODGE = 4,
            PARRY = 8,
            INTERACT = 16,
            POTION = 32,
            READY = 64,
            PAUSE = 128,
            VITALITY = 256,
            POWER = 512,
            ENDURANCE = 1024,
            SPRINT = 2048;
    public static final CoopInput NONE = new CoopInput(0, 0, 0, 0, 0);

    public CoopInput {
        if (sequence < 0
                || !Float.isFinite(moveX)
                || !Float.isFinite(moveZ)
                || Math.abs(moveX) > 1.01f
                || Math.abs(moveZ) > 1.01f
                || !Float.isFinite(aim)
                || Math.abs(aim) > 3600
                || (buttons & ~4095) != 0)
            throw new IllegalArgumentException("Invalid co-op input");
    }
}
