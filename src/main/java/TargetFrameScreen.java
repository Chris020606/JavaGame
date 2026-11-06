import Entities.Enemy;
import Entities.Player;
import java.util.List;
import static com.raylib.Raylib.*;

/** Focused enemy identity, health and cast time, separate from the player's action belt. */
public final class TargetFrameScreen {
    public void draw(Player player,HeroClass hero,int chapter,List<Enemy> enemies) {
        if(GetScreenWidth()<1000)return;
        float x=player.getPosition().x(),z=player.getPosition().z();
        Enemy target=RangedAim.target(x,z,player.getRotation().y(),hero==HeroClass.WARRIOR?8:hero.attack(1,false).range(),enemies,e->ArenaNavigation.projectileFraction(chapter,x,z,e.x,e.z)>=1);
        if(target==null)return;
        int left=GetScreenWidth()-290,top=GetScreenHeight()-218;
        Menu.panel(left,top,274,116);
        Menu.text(target.label(),left+12,top+10,16,target.boss()?Menu.GOLD:Menu.TEXT);
        Menu.text(Math.round(target.health)+" / "+Math.round(target.maxHealth)+" HP",left+12,top+34,12,Menu.MUTED);
        Menu.bar(left+12,top+55,250,target.health,target.maxHealth,Menu.RED);
        String action=target.attackTime>.25f?(target.boss()?target.moveLabel():"ATTACK")+" / "+String.format("%.1fs",Math.max(0,target.attackTime-.25f)):target.stun>0?"STAGGERED":"WATCH FOR AN OPENING";
        Menu.text(action,left+12,top+77,11,target.attackTime>.25f?Menu.GOLD:Menu.MUTED);
        if(target.attackTime>.25f)Menu.bar(left+12,top+98,250,target.attackDuration()-target.attackTime,target.attackDuration()-.25f,Menu.GOLD);
    }
}
