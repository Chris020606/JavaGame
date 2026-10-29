/** Warned ground magic and swept expanding rings. Each hazard can strike the hero once. */
public final class BossHazard {
    public enum Kind { SIGIL, SHOCKWAVE }
    public final Kind kind;
    public final float x,z,damage;
    public float delay,age,radius,previousRadius;
    private boolean struck;
    public BossHazard(Kind kind,float x,float z,float delay,float damage) {
        this.kind=kind;this.x=x;this.z=z;this.delay=delay;this.damage=damage;
        radius=kind==Kind.SIGIL?1.65f:.5f;previousRadius=radius;
    }
    public void update(float dt) {
        previousRadius=radius;
        float active=Math.max(0,dt-Math.max(0,delay));
        delay-=dt;age+=active;
        if(kind==Kind.SHOCKWAVE) radius=.5f+age*6;
    }
    public boolean hits(float px,float pz) {
        if(struck || delay>0 || expired()) return false;
        float d=(float)Math.hypot(px-x,pz-z);
        boolean hit=kind==Kind.SIGIL?d<=radius:d>=previousRadius-.45f && d<=radius+.45f;
        if(hit) struck=true;
        return hit;
    }
    public boolean expired() { return kind==Kind.SIGIL?age>.45f:radius>10; }
}
