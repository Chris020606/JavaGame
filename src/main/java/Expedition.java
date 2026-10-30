import java.util.*;

/** Persistent, optional build choices earned by clearing waves and field objectives. */
public final class Expedition {
    public enum Blessing {
        KINDLING("Kindling", "Heavy attacks deal +12% damage per rank."),
        BLOODROOT("Bloodroot", "Defeating an enemy restores 3 health per rank."),
        SECOND_WIND("Second Wind", "A successful parry restores 10 extra stamina per rank."),
        WAYFARER("Wayfarer", "Dodge cooldown is 0.15 seconds shorter per rank."),
        BULWARK("Bulwark", "The sanctuary takes 12% less damage per rank."),
        HUNTERS_MARK("Hunter's Mark", "Projectiles deal +10% damage per rank.");
        public final String title,description;
        Blessing(String title,String description) { this.title=title;this.description=description; }
    }
    private final int[] ranks=new int[Blessing.values().length];
    private int embers,lastWave,choices,objectives;
    public int rank(Blessing blessing) { return ranks[blessing.ordinal()]; }
    public int embers() { return embers; }
    public int objectives() { return objectives; }
    public int choices() { return choices; }
    public boolean waveCleared(int wave) {
        if(wave<=lastWave || wave<1 || wave>10)return false;
        lastWave=wave;
        if(Campaign.chapter(wave).bossOnly())return false;
        embers++;return true;
    }
    public void objectiveCompleted() { objectives++;embers++; }
    public List<Blessing> offers() {
        if(embers<=0)return List.of();
        ArrayList<Blessing> available=new ArrayList<>();
        // Rotate the draft so every choice can lead to a different build.
        Blessing[] all=Blessing.values();
        for(int i=0;i<all.length && available.size()<3;i++) {
            Blessing b=all[Math.floorMod(i+choices*2,all.length)];if(rank(b)<3)available.add(b);
        }
        return List.copyOf(available);
    }
    public boolean choose(Blessing blessing) {
        if(!offers().contains(blessing))return false;
        ranks[blessing.ordinal()]++;embers--;choices++;return true;
    }
    public void store(Properties p) {
        p.setProperty("expedition.embers",embers+"");p.setProperty("expedition.lastWave",lastWave+"");
        p.setProperty("expedition.choices",choices+"");p.setProperty("expedition.objectives",objectives+"");
        for(Blessing b:Blessing.values())p.setProperty("blessing."+b.name(),rank(b)+"");
    }
    public static Expedition restore(Properties p) {
        Expedition e=new Expedition();
        e.embers=read(p,"expedition.embers",0,30);e.lastWave=read(p,"expedition.lastWave",0,10);
        e.choices=read(p,"expedition.choices",0,18);e.objectives=read(p,"expedition.objectives",0,8);
        int ranks=0;for(Blessing b:Blessing.values()) { e.ranks[b.ordinal()]=read(p,"blessing."+b.name(),0,3);ranks+=e.rank(b); }
        if(ranks!=e.choices)throw new IllegalArgumentException("Inconsistent blessing ranks");
        return e;
    }
    private static int read(Properties p,String key,int min,int max) { return p.containsKey(key)?SaveGame.integer(p,key,min,max):0; }
}
