/** Stable forward-facing spark spread; presentation only, independent of damage and knockback. */
final class ImpactDirection {
    static double angle(float sourceX, float sourceZ, float hitX, float hitZ, int spark) {
        double heading = Math.atan2(hitZ - sourceZ, hitX - sourceX);
        double fan = ((spark * .61803398875) % 1 - .5) * 1.8;
        return heading + fan;
    }

    private ImpactDirection() {}
}
