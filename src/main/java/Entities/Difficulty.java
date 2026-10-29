package Entities;

public enum Difficulty {
    EASY("Easy",.75f,.65f,.90f,.80f,1.20f,1,"Forgiving damage and smaller raids"),
    NORMAL("Normal",1,1,1,1,1,2,"The intended campaign balance"),
    HARD("Hard",1.25f,1.35f,1.08f,1.15f,.85f,3,"Stronger squads and shorter recoveries"),
    EXTREME("Extreme",1.6f,1.75f,1.16f,1.35f,.70f,4,"Relentless pressure; master your defenses");
    public final String label,description;
    public final float health,damage,speed,numbers,cadence;
    public final int attackers;
    Difficulty(String label,float health,float damage,float speed,float numbers,float cadence,int attackers,String description) {
        this.label=label;this.health=health;this.damage=damage;this.speed=speed;this.numbers=numbers;this.cadence=cadence;this.attackers=attackers;this.description=description;
    }
    public int count(int normal,boolean boss) { return boss?1:Math.max(1,Math.min(36,Math.round(normal*numbers))); }
}
