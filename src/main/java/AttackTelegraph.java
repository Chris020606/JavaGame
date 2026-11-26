/** Attack warnings complete at the actual damage frame, not the end of the recovery clip. */
final class AttackTelegraph {
    static float progress(float remaining, float duration) {
        return Math.max(0, Math.min(1, 1 - (remaining - .25f) / Math.max(.01f, duration - .25f)));
    }

    private AttackTelegraph() {}
}
