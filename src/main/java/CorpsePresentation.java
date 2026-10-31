/** Fallen characters stay on the ground, then fade in place after the fight. */
public final class CorpsePresentation {
    private CorpsePresentation() { }
    public static float lifetime(boolean boss) { return boss?18:12; }
    public static float opacity(float age,boolean boss) { return Math.max(0,Math.min(1,(lifetime(boss)-Math.max(0,age))/2)); }
    public static float height() { return 0; }
    public static float support(float minimum,float scale) { return Math.max(0,-minimum)*scale; }
}
