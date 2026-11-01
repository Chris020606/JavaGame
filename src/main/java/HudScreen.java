import Config.Controls;
import Entities.Player;
import static Config.Controls.Action.*;
import static com.raylib.Raylib.*;

/** Outpost's ember vitals and six-slot action belt. Geometry is shared with input. */
public final class HudScreen {
    public record CombatState(float cooldown,float duration,float impact,float charge,int combo,float comboWindow,String mastery,boolean empowered) { }
    private static final Controls.Action[] ACTIONS={SLASH,HEAVY,PARRY,LUNGE,POTION,INVENTORY};
    public void draw(Progression p,Player player,Controls controls,CombatState state) {
        HudLayout layout=new HudLayout(GetScreenWidth(),GetScreenHeight());int x=layout.x(),y=layout.y(),span=layout.span();
        Menu.frame(x-8,y-114,span+16,76,Menu.color(24,31,30),Menu.LINE);
        int healthY=y-104;
        DrawRectangle(x-8,y-140,span+16,23,Fade(Menu.INK,.85f));
        Menu.center(state.mastery(),GetScreenWidth()/2,y-132,11,state.empowered()?Menu.GOLD:Menu.MUTED);
        int labelSize=layout.cell()<56?12:14;
        Menu.text("HEALTH",x,healthY,labelSize,Menu.MUTED);
        String hp=Math.round(p.health())+" / "+p.maxHealth();
        Menu.text(hp,x+span-Menu.measure(hp,14),healthY,14,Menu.TEXT);
        int barX=x,barY=healthY+15,barW=span,barH=14;
        Menu.frame(barX,barY,barW,barH,Menu.INK,Menu.LINE);
        int healthWidth=(int)((barW-4)*Math.max(0,Math.min(1,p.health()/p.maxHealth())));
        if(healthWidth>0)DrawRectangle(barX+2,barY+2,healthWidth,barH-4,p.health()/p.maxHealth()<.25f?Menu.RED:Menu.color(202,111,91));
        int staminaY=y-69;
        Menu.text("STAMINA",x,staminaY,labelSize,Menu.MUTED);
        String stamina=Math.round(player.discipline().stamina())+" / "+p.maxStamina();
        Menu.text(stamina,x+span-Menu.measure(stamina,14),staminaY,14,Menu.TEXT);
        int staminaBarY=staminaY+17;
        Menu.frame(x,staminaBarY,span,12,Menu.INK,Menu.LINE);
        int staminaWidth=(int)((span-4)*Math.max(0,Math.min(1,player.discipline().stamina()/p.maxStamina())));
        if(staminaWidth>0)DrawRectangle(x+2,staminaBarY+2,staminaWidth,8,player.discipline().stamina()<24?Menu.GOLD:Menu.TEAL);
        Menu.frame(x-8,y-8,span+16,78,Menu.PANEL,Menu.LINE);
        Vector2 mouse=GetMousePosition();int hover=layout.hit(mouse.x(),mouse.y());
        String weapon=p.heroClass()==HeroClass.WARRIOR?"sword":p.heroClass()==HeroClass.ARCHER?"bow":"staff";
        String[] icons={weapon,"crown","shield","sword","potion","bag"};
        String[] labels={p.heroClass()==HeroClass.WARRIOR?"STRIKE":p.heroClass()==HeroClass.ARCHER?"SHOOT":"CAST","CHARGE","PARRY","LUNGE","HEAL","PACK"};
        for(int i=0;i<HudLayout.SLOTS;i++) {
            int sx=layout.slotX(i),size=layout.cell();boolean active=i==0&&player.isAttacking() || i==1&&state.charge()>0 || i==2&&player.discipline().parrying();
            Menu.slot(sx,y,size,hover==i,active);
            if((i==0 || i==3) && state.cooldown()>0)DrawRectangle(sx+3,y+3,size-6,(int)((size-6)*Math.min(1,state.cooldown()/state.duration())),Fade(Menu.INK,.55f));
            Menu.text(controls.label(ACTIONS[i]),sx+5,y+4,9,Menu.MUTED);
            Menu.icon(icons[i],sx+size/2-12,y+18,24,i==4?Menu.RED:active?Menu.GOLD:Menu.TEXT);
            if(i==4)Menu.text(""+p.potions(),sx+size-15,y+size-18,13,Menu.GOLD);
            Menu.center(labels[i],sx+size/2,y+size+3,9,Menu.MUTED);
        }
        String timing=state.charge()>0?"HEAVY CHARGE / RELEASE IN GOLD":player.discipline().riposteReady()?"RIPOSTE READY":state.combo()>0 && state.comboWindow()>0 && state.cooldown()<=.22f?"CHAIN NOW":player.isAttacking()?"ATTACK RECOVERY":null;
        boolean actionMeter=state.charge()>0;
        if(timing!=null)Menu.center(timing,GetScreenWidth()/2,y-39,10,Menu.GOLD);
        if(actionMeter) {
            float maximum=state.charge()>0?1.4f:state.duration();
            Menu.bar(x,y-26,span,state.charge()>0?state.charge():maximum-state.cooldown(),maximum,state.charge()>0?Menu.GOLD:Menu.TEAL);
            if(state.charge()>0)DrawRectangle(x+(int)(span*.75f/1.4f),y-27,(int)(span*.3f/1.4f),10,Fade(Menu.GOLD,.65f));
            else if(player.isAttacking())DrawRectangle(x+(int)(span*state.impact()/maximum),y-29,2,14,Menu.TEXT);
        }
        if(hover>=0) {
            int tx=Math.max(8,Math.min(GetScreenWidth()-338,GetScreenWidth()/2-165)),ty=y-174;
            Menu.panel(tx,ty,330,48);Menu.icon(icons[hover],tx+12,ty+12,22,Menu.GOLD);
            String help=switch(hover) {case 0 -> "Strike toward your facing";case 1 -> p.heroClass()==HeroClass.MAGE && p.weaponTier()>=3?"Charged Dawnweaver shots mend allies":"Hold to charge, release in gold";case 2 -> "Time your block before a gold hit";case 3 -> "Close the gap with a lunge";case 4 -> "Restore up to 60 health";default -> "Inspect and change equipment";};
            Menu.text(labels[hover]+" / "+controls.label(ACTIONS[hover]),tx+44,ty+7,13,Menu.GOLD);
            Menu.text(help,tx+44,ty+27,11,Menu.TEXT);
        }
    }
}
