package Entities;

public enum BossMove {
    CLEAVE("ASH CLEAVE",1.2f,true),SLAM("FAULT SLAM",1.65f,false),
    CHARGE("IRON RUSH",1.4f,true),SIGILS("SOUL SIGILS",1.6f,false);
    public final String label;
    public final float duration;
    public final boolean parryable;
    BossMove(String label,float duration,boolean parryable) { this.label=label;this.duration=duration;this.parryable=parryable; }
    public static BossMove choose(int cycle,float distance,boolean king) {
        BossMove next=values()[Math.floorMod(cycle+(king?1:0),4)];
        return next==CLEAVE && distance>4?CHARGE:next;
    }
}
