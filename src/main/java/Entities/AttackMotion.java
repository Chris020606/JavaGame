package Entities;

/** The visible contact frame follows the damage clock, independent of clip length. */
public final class AttackMotion {
    private AttackMotion() { }
    public static float unit(float value) { return Math.max(0,Math.min(1,value)); }
    public static float smooth(float value) { float t=unit(value);return t*t*(3-2*t); }
    public static float contact(float elapsed,float impact,float duration,float pose,float start) {
        if(elapsed<=impact) { float t=unit(elapsed/Math.max(.001f,impact));return start+(pose-start)*t*t; }
        float t=unit((elapsed-impact)/Math.max(.001f,duration-impact));return pose+(1-pose)*(1-(1-t)*(1-t));
    }
    public static float recovery(float elapsed,float impact,float duration) {
        float start=Math.min(duration-.06f,impact+.12f);
        return smooth((elapsed-start)/Math.max(.06f,duration-start));
    }
}
