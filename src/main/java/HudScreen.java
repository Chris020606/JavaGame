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
        int healthY=y-72;
        Menu.center(state.mastery(),GetScreenWidth()/2,y-104,11,state.empowered()?Menu.GOLD:Menu.MUTED);
        Menu.icon("heart",x,healthY+1,14,Menu.RED);
        int pip=layout.cell()<56?14:17;
        for(int i=0;i<12;i++)Menu.ember(x+22+i*(pip+2),healthY,pip,p.health()/p.maxHealth()*12-i,Menu.RED);
        String hp=Math.round(p.health())+" / "+p.maxHealth();Menu.text(hp,x+span-Menu.measure(hp,12),healthY+2,12,Menu.TEXT);
        int staminaY=y-43;
        for(int i=0;i<10;i++) {
            int width=(span-9*3)/10,left=x+i*(width+3);
            DrawRectangle(left,staminaY,width,7,Menu.INK);
            float fill=Math.max(0,Math.min(1,player.discipline().stamina()/p.maxStamina()*10-i));
            DrawRectangle(left+1,staminaY+1,(int)((width-2)*fill),5,player.discipline().stamina()<24?Menu.RED:Menu.TEAL);
        }
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
        String timing=state.charge()>0?"RELEASE IN GOLD":player.discipline().riposteReady()?"RIPOSTE READY":state.combo()>0 && state.comboWindow()>0 && state.cooldown()<=.22f?"CHAIN NOW":player.isAttacking()?"RECOVER":"STAMINA "+Math.round(player.discipline().stamina())+" / "+p.maxStamina();
        Menu.center(timing,GetScreenWidth()/2,y-29,10,Menu.GOLD);
        float maximum=state.charge()>0?1.4f:state.duration();
        Menu.bar(x,y-14,span,state.charge()>0?state.charge():maximum-state.cooldown(),maximum,state.charge()>0?Menu.GOLD:Menu.TEAL);
        if(state.charge()>0)DrawRectangle(x+(int)(span*.75f/1.4f),y-15,(int)(span*.3f/1.4f),10,Fade(Menu.GOLD,.65f));
        else if(player.isAttacking())DrawRectangle(x+(int)(span*state.impact()/maximum),y-17,2,14,Menu.TEXT);
        if(hover>=0) {
            int tx=Math.max(8,Math.min(GetScreenWidth()-338,GetScreenWidth()/2-165)),ty=y-158;
            Menu.panel(tx,ty,330,48);Menu.icon(icons[hover],tx+12,ty+12,22,Menu.GOLD);
            String help=switch(hover) {case 0 -> "Strike toward your facing";case 1 -> "Hold to charge, release in gold";case 2 -> "Time your block before a gold hit";case 3 -> "Close the gap with a lunge";case 4 -> "Restore up to 60 health";default -> "Inspect and change equipment";};
            Menu.text(labels[hover]+" / "+controls.label(ACTIONS[hover]),tx+44,ty+7,13,Menu.GOLD);
            Menu.text(help,tx+44,ty+27,11,Menu.TEXT);
        }
    }
}
