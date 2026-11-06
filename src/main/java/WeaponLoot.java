import java.util.Properties;
import java.util.random.RandomGenerator;

/** Persistent best-found weapon enchantments. Weaker duplicates never replace better gear. */
public final class WeaponLoot {
    public enum Trait { KEEN, FLOWING, BALANCED }
    public record Roll(int tier,Trait trait,int power) {
        public int damage() { return trait==Trait.FLOWING?power:power*2; }
        public int recovery() { return trait==Trait.KEEN?0:power; }
        public String label() { return trait.name()+" +"+power; }
    }
    private final Roll[] best=new Roll[4];
    public Roll get(int tier) { return best[tier]; }
    public Roll roll(int tier,RandomGenerator random) {
        Roll found=new Roll(tier,Trait.values()[random.nextInt(3)],1+random.nextInt(4));
        Roll current=best[tier];
        if(current==null || found.damage()+found.recovery()>current.damage()+current.recovery())best[tier]=found;
        return found;
    }
    public void store(Properties p) {
        for(int i=0;i<4;i++)if(best[i]!=null) { p.setProperty("loot."+i+".trait",best[i].trait().name());p.setProperty("loot."+i+".power",""+best[i].power()); }
    }
    public static WeaponLoot restore(Properties p) {
        WeaponLoot loot=new WeaponLoot();
        for(int i=0;i<4;i++)if(p.containsKey("loot."+i+".trait"))loot.best[i]=new Roll(i,Trait.valueOf(p.getProperty("loot."+i+".trait")),SaveGame.integer(p,"loot."+i+".power",1,4));
        return loot;
    }
}
