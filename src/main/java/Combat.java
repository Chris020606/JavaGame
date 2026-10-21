/** Deterministic combat geometry and timing, independent of rendering/input. */
public final class Combat {
    private Combat() {}
    public static boolean inArc(float px, float pz, float facing, float x, float z, float range, float halfAngle) {
        double dx=x-px, dz=z-pz, distance=Math.hypot(dx,dz);
        if (distance > range) return false;
        if (distance < .001) return true;
        double angle=Math.toRadians(facing);
        return (dx*Math.sin(angle)+dz*Math.cos(angle))/distance >= Math.cos(Math.toRadians(halfAngle));
    }
    public record Attack(float duration, float impact, float range, float halfAngle, float multiplier, String label) {}
    public static Attack attack(int combo, boolean heavy) {
        if (heavy) return new Attack(.9f,.38f,3.5f,65,1.8f,"HEAVY CLEAVE");
        return combo==3 ? new Attack(.65f,.26f,3.3f,85,1.4f,"FINISHER  3 / 3")
            : new Attack(.5f,.18f,3.0f,65,1,"SLASH  " + combo + " / 3");
    }
}
