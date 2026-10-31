import static com.raylib.Raylib.*;

/** Shared layout coordinates keep mouse targets aligned with the drawn rows. */
public final class WorkshopScreen {
    public static final int WIDTH=940,HEIGHT=600,CONTENT=244,ROW_TOP=140,ROW_HEIGHT=52;
    private int tab;
    private static final String[] TABS={"TRAINING","EQUIPMENT FORGE","SUPPLIES"};
    public boolean draw(Progression p,String closeKey,GameAudio audio) {
        int x=(GetScreenWidth()-WIDTH)/2,y=(GetScreenHeight()-HEIGHT)/2;
        Menu.panel(x,y,WIDTH,HEIGHT);
        Menu.frame(x+8,y+8,212,HEIGHT-16,Menu.color(27,34,30),Menu.LINE);
        Menu.text("THE WORKSHOP",x+28,y+28,28,Menu.TEXT);
        Menu.text("REST. REFINE. RETURN.",x+28,y+72,12,Menu.MUTED);
        // Resolve navigation before drawing so highlight and content agree on the click frame.
        if(IsMouseButtonPressed(MOUSE_BUTTON_LEFT)) {
            Vector2 mouse=GetMousePosition();
            for(int i=0;i<TABS.length;i++)if(mouse.x()>=x+24 && mouse.x()<x+208 && mouse.y()>=y+122+i*60 && mouse.y()<y+166+i*60)tab=i;
        }
        for(int i=0;i<TABS.length;i++) {
            int top=y+122+i*60;
            if(tab==i) { DrawRectangle(x+8,top-4,3,52,Menu.TEAL);DrawRectangle(x+18,top-4,192,52,Menu.color(64,67,47)); }
            Menu.button(TABS[i],x+24,top,184,true);
        }
        Menu.text("YOUR WARDEN",x+28,y+340,13,Menu.GOLD);
        String[] stats={p.maxHealth()+"  HEALTH",p.damage()+"  DAMAGE",p.maxStamina()+"  STAMINA",p.defense()+"  DEFENSE"};
        for(int i=0;i<stats.length;i++)Menu.text(stats[i],x+28,y+374+i*31,17,i==2?Menu.TEAL:Menu.TEXT);
        Menu.text(TABS[tab],x+CONTENT,y+28,28,Menu.TEXT);
        Menu.text(p.crystals()+" CRYSTALS",x+720,y+32,20,Menu.GOLD);
        Menu.text(tab==0?"Build your strengths. Every upgrade stays with this campaign.":tab==1?"Forge equipment here, then equip it in your inventory.":"Leave the sanctuary prepared for the next encounter.",x+CONTENT,y+76,16,Menu.MUTED);
        DrawLine(x+CONTENT,y+112,x+916,y+112,Menu.LINE);
        if(tab==0)drawTraining(p,audio,x,y);
        else if(tab==1)drawForge(p,audio,x,y);
        else drawSupplies(p,audio,x,y);
        DrawLine(x+CONTENT,y+531,x+916,y+531,Menu.LINE);
        Menu.text("TIME PAUSED / Purchases are saved with your campaign.",x+CONTENT,y+551,14,Menu.MUTED);
        return Menu.button("RETURN ["+closeKey+"]",x+24,y+536,184,true);
    }
    private void drawTraining(Progression p,GameAudio audio,int x,int y) {
        Menu.text("ATTRIBUTE",x+CONTENT+12,y+121,11,Menu.MUTED);
        Menu.text("LEVEL",x+430,y+121,11,Menu.MUTED);Menu.text("NEXT UPGRADE",x+515,y+121,11,Menu.MUTED);
        for(int i=0;i<Progression.TRAINING.length;i++) {
            var upgrade=Progression.TRAINING[i];int top=y+ROW_TOP+i*ROW_HEIGHT;
            Menu.frame(x+CONTENT,top,672,48,i%2==0?Menu.color(39,47,39):Menu.color(31,38,33),Menu.LINE);
            String icon=switch(upgrade) {case VITALITY -> "heart";case POWER -> "sword";case ARMOR -> "shield";case BASE -> "crown";default -> "gear";};
            Menu.icon(icon,x+CONTENT+12,top+14,20,Menu.TEAL);
            Menu.text(p.upgradeLabel(upgrade),x+CONTENT+43,top+15,16,Menu.TEXT);
            boolean max=p.level(upgrade)>=p.maxLevel(upgrade);
            Menu.text(p.level(upgrade)+" / "+p.maxLevel(upgrade),x+430,top+13,14,Menu.GOLD);
            Menu.bar(x+430,top+35,58,p.level(upgrade),p.maxLevel(upgrade),Menu.TEAL);
            Menu.text(max?"MASTERED":p.upgradeEffect(upgrade),x+515,top+16,14,Menu.MUTED);
            if(Menu.button(max?"MAX LEVEL":"UPGRADE / "+p.cost(upgrade),x+758,top+2,150,!max && p.crystals()>=p.cost(upgrade)) && p.purchase(upgrade))audio.play("upgrade");
        }
    }
    private void drawForge(Progression p,GameAudio audio,int x,int y) {
        for(int tier=1;tier<=3;tier++) {
            int left=x+CONTENT+(tier-1)*226;
            Menu.frame(left,y+136,214,374,Menu.color(39,47,39),Menu.LINE);
            var color=tier==3?Menu.GOLD:tier==2?Menu.TEAL:Menu.TEXT;
            Menu.text("TIER 0"+tier,left+16,y+151,15,color);
            Menu.icon(p.heroClass()==HeroClass.ARCHER?"bow":p.heroClass()==HeroClass.MAGE?"staff":"sword",left+16,y+184,32,color);
            Menu.wrapped(p.weaponName(tier),left+16,y+234,182,19,Menu.TEXT);
            Menu.text("+"+p.weaponBonus(tier)+" damage",left+16,y+284,15,Menu.MUTED);
            if(Menu.button(tier<=p.weaponUnlocked()?"OWNED":"FORGE / "+p.weaponPrice(tier),left+12,y+313,190,tier>p.weaponUnlocked() && p.crystals()>=p.weaponPrice(tier)) && p.buyWeapon(tier))audio.play("upgrade");
            DrawLine(left+16,y+372,left+198,y+372,Menu.LINE);
            Menu.text(tier==1?"IRON ARMOR":tier==2?"RUNIC ARMOR":"DAWNSTEEL ARMOR",left+16,y+387,17,color);
            Menu.text("4 pieces / +"+(tier*7)+" defense",left+16,y+421,14,Menu.MUTED);
            if(Menu.button(tier<=p.armorUnlocked()?"OWNED":"FORGE / "+p.armorPrice(tier),left+12,y+455,190,tier>p.armorUnlocked() && p.crystals()>=p.armorPrice(tier)) && p.buyArmor(tier))audio.play("upgrade");
        }
    }
    private void drawSupplies(Progression p,GameAudio audio,int x,int y) {
        Menu.icon("potion",x+CONTENT+36,y+164,54,Menu.RED);
        Menu.text("HEALING POTIONS",x+CONTENT+120,y+164,26,Menu.TEXT);
        Menu.text("Restore up to 60 HP. Carry up to five.",x+CONTENT+120,y+211,18,Menu.MUTED);
        for(int i=0;i<5;i++) {
            Menu.slot(x+CONTENT+120+i*70,y+266,54,false,i<p.potions());
            Menu.icon("potion",x+CONTENT+137+i*70,y+286,20,i<p.potions()?Menu.RED:Menu.LINE);
        }
        if(Menu.button("RESTOCK / 30 CRYSTALS",x+CONTENT+120,y+368,340,p.crystals()>=30 && p.potions()<5) && p.buyPotion())audio.play("upgrade");
    }
}
