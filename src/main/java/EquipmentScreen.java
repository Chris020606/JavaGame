import Entities.*;
import assets.AssetPaths;
import static com.raylib.Raylib.*;
import static com.raylib.Colors.WHITE;

/** Select/compare equipment, then equip explicitly, double-click, or drag to a labeled slot. */
public final class EquipmentScreen implements AutoCloseable {
    private final RenderTexture preview=LoadRenderTexture(640,800);
    private final RenderTexture[] thumbnails=new RenderTexture[16];
    private final Shader shader=LoadShader(AssetPaths.path("resources/outpost/daylight.vs"),AssetPaths.path("resources/outpost/preview.fs"));
    private final Camera3D camera=new Camera3D()._position(new Vector3().x(3).y(2.9f).z(5.5f)).target(new Vector3().y(1.3f)).up(new Vector3().y(1)).fovy(40).projection(CAMERA_PERSPECTIVE);
    private HeroClass thumbnailClass;
    private Progression owner;
    private int selected,dragged=-1,filter,lastClicked=-1;
    private double lastClick;
    private float yaw,dragX,dragY;
    private boolean orbit;
    private static Color tierColor(int tier) { return tier==3?Menu.GOLD:tier==2?Menu.TEAL:tier==1?Menu.color(182,201,220):Menu.MUTED; }
    public void prepare(WorldAssets assets,Player player,Progression progress) {
        if(owner!=progress) { owner=progress;selected=progress.weaponTier();dragged=lastClicked=-1;filter=0; }
        Vector3 position=player.getPosition();float facing=player.getRotation().y();
        player.setPosition(new Vector3());player.getRotation().y(yaw);player.poseForInventory();assets.previewPass(shader);
        BeginTextureMode(preview);ClearBackground(Menu.INK);DrawRectangleGradientV(0,0,640,800,Menu.color(61,69,57),Menu.color(24,32,31));BeginMode3D(camera);
        DrawCylinder(new Vector3().y(-.03f),.95f,.95f,.06f,32,Menu.color(93,83,59));
        assets.preparePlayer(player);assets.drawPlayerBody(player,progress,WHITE);
        assets.drawPlayerWeapon(player,progress.heroClass().ordinal(),progress.weaponTier());assets.drawArmor(player,progress);
        EndMode3D();EndTextureMode();
        if(thumbnailClass!=progress.heroClass()) {
            for(int item=0;item<16;item++) {
                if(thumbnails[item]==null)thumbnails[item]=LoadRenderTexture(128,128);
                float target=item<4?(progress.heroClass()==HeroClass.ARCHER?0:.85f):slot(item)==ArmorSlot.HEAD?1.85f:slot(item)==ArmorSlot.CHEST?.85f:slot(item)==ArmorSlot.LEGS?.34f:.12f;
                float distance=item<4?4.3f:slot(item)==ArmorSlot.HEAD?2.7f:slot(item)==ArmorSlot.CHEST?2.8f:1.2f;
                Camera3D itemCamera=new Camera3D()._position(new Vector3().x(distance*.5f).y(target+distance*.25f).z(distance)).target(new Vector3().y(target)).up(new Vector3().y(1)).fovy(34).projection(CAMERA_PERSPECTIVE);
                BeginTextureMode(thumbnails[item]);ClearBackground(new Color().a((byte)0));BeginMode3D(itemCamera);
                assets.drawInventoryItem(player,progress,item);EndMode3D();EndTextureMode();
            }
            thumbnailClass=progress.heroClass();
        }
        assets.endPreview();player.setPosition(position);player.getRotation().y(facing);
    }
    private static boolean inside(Vector2 m,int x,int y,int w,int h) { return m.x()>=x && m.x()<x+w && m.y()>=y && m.y()<y+h; }
    private static int tier(int item) { return item<4?item:1+(item-4)/4; }
    private static ArmorSlot slot(int item) { return ArmorSlot.values()[(item-4)%4]; }
    private static boolean owned(Progression p,int item) { return item<4?item<=p.weaponUnlocked():item<16?tier(item)<=p.armorUnlocked():p.potions()>0; }
    private static boolean equipped(Progression p,int item) { return item<4?p.weaponTier()==item:item<16 && p.armorTier(slot(item))==tier(item); }
    private static String name(Progression p,int item) { return item<4?p.weaponName(item):item<16?p.armorName(slot(item),tier(item)):"Healing potions"; }
    private static void equip(Progression p,int item) { if(item<4)p.equipWeapon(item);else if(item<16)p.equipArmor(slot(item),tier(item));else p.usePotion(); }
    private static void remove(Progression p,int item) { if(item<4)p.equipWeapon(0);else if(item<16)p.equipArmor(slot(item),0); }
    private void thumbnail(int item,int x,int y,int size,Color tint) {
        if(item==16) { Menu.icon("potion",x+size/4,y+size/4,size/2,Menu.RED);return; }
        DrawTexturePro(thumbnails[item].texture(),new Rectangle().width(128).height(-128),new Rectangle().x(x).y(y).width(size).height(size),new Vector2(),0,tint);
    }
    private java.util.List<Integer> visible() {
        java.util.ArrayList<Integer> ids=new java.util.ArrayList<>();for(int i=0;i<17;i++)if(filter==0 || filter==1 && i<4 || filter==2 && i>=4 && i<16)ids.add(i);return ids;
    }
    public boolean draw(Progression p,String inventoryKey) {
        int x=(GetScreenWidth()-940)/2,y=(GetScreenHeight()-600)/2;Vector2 mouse=GetMousePosition();Menu.panel(x,y,940,600);
        Menu.icon("bag",x+24,y+19,24,Menu.GOLD);Menu.text("THE WARDEN'S PACK",x+60,y+16,27,Menu.TEXT);Menu.text(p.crystals()+" CRYSTALS",x+720,y+24,19,Menu.GOLD);
        Menu.text(p.heroClass().label+" / "+p.difficulty().label+" / EQUIPMENT",x+24,y+62,13,Menu.MUTED);
        Menu.frame(x+20,y+86,258,338,Menu.INK,Menu.LINE);
        DrawTexturePro(preview.texture(),new Rectangle().width(640).height(-800),new Rectangle().x(x+24).y(y+90).width(250).height(330),new Vector2(),0,WHITE);
        Menu.text("DRAG TO TURN / SCROLL TO ZOOM",x+30,y+432,11,Menu.MUTED);Menu.text(p.weaponName(p.weaponTier()),x+24,y+452,14,tierColor(p.weaponTier()));
        if(IsMouseButtonPressed(MOUSE_BUTTON_LEFT) && inside(mouse,x+24,y+90,250,330))orbit=true;
        if(orbit && IsMouseButtonDown(MOUSE_BUTTON_LEFT))yaw+=GetMouseDelta().x()*.8f;
        if(inside(mouse,x+24,y+90,250,330))camera.fovy(Math.max(28,Math.min(52,camera.fovy()-GetMouseWheelMove()*2)));
        if(IsMouseButtonReleased(MOUSE_BUTTON_LEFT))orbit=false;
        String[] filters={"BACKPACK","WEAPONS","ARMOR"};
        for(int i=0;i<3;i++) {
            int left=x+400+i*172;boolean hit=inside(mouse,left,y+56,166,25);
            if(hit && IsMouseButtonPressed(MOUSE_BUTTON_LEFT)) { filter=i;selected=visible().get(0);dragged=lastClicked=-1; }
            Menu.frame(left,y+56,166,25,filter==i?Menu.color(70,75,55):Menu.PANEL,filter==i?Menu.GOLD:Menu.LINE);Menu.center(filters[i],left+83,y+62,12,filter==i?Menu.GOLD:Menu.TEXT);
        }
        var ids=visible();int pos=Math.max(0,ids.indexOf(selected)),move=IsKeyPressed(KEY_RIGHT)?1:IsKeyPressed(KEY_LEFT)?-1:IsKeyPressed(KEY_DOWN)?6:IsKeyPressed(KEY_UP)?-6:0;
        if(move!=0)selected=ids.get(Math.max(0,Math.min(ids.size()-1,pos+move)));
        if(IsKeyPressed(KEY_ENTER) && owned(p,selected))equip(p,selected);
        if(IsKeyPressed(KEY_BACKSPACE) || IsKeyPressed(KEY_DELETE))remove(p,selected);
        for(int s=0;s<5;s++) {
            int sx=x+300,sy=y+90+s*70;Menu.slot(sx,sy,64,false,true);int level=s==4?p.weaponTier():p.armorTier(ArmorSlot.values()[s]);
            Menu.center(s==4?"WEAPON":ArmorSlot.values()[s].name(),sx+32,sy+4,9,Menu.MUTED);
            if(level>0 || s==4)thumbnail(s==4?level:4+(level-1)*4+s,sx+10,sy+12,44,WHITE);
            else Menu.icon(ArmorSlot.values()[s].icon(),sx+20,sy+22,24,Menu.LINE);
            Menu.text("T"+level,sx+4,sy+48,11,tierColor(level));
            if(IsMouseButtonPressed(MOUSE_BUTTON_RIGHT) && inside(mouse,sx,sy,64,64)) { if(s==4)p.equipWeapon(0);else p.equipArmor(ArmorSlot.values()[s],0); }
            if(IsMouseButtonReleased(MOUSE_BUTTON_LEFT) && dragged>=0 && inside(mouse,sx,sy,64,64) && (Math.abs(mouse.x()-dragX)+Math.abs(mouse.y()-dragY)>6))
                if(s==4 && dragged<4 || s<4 && dragged>=4 && dragged<16 && slot(dragged).ordinal()==s)equip(p,dragged);
        }
        int hover=-1;
        for(int cell=0;cell<18;cell++) {
            int left=x+400+(cell%6)*75,top=y+90+(cell/6)*76;
            int cellItem=cell<ids.size()?ids.get(cell):-1;Menu.slot(left,top,66,cellItem==selected,cellItem>=0 && equipped(p,cellItem));
            if(cell>=ids.size())continue;int item=ids.get(cell);boolean available=owned(p,item);
            thumbnail(item,left+4,top+1,58,available?WHITE:Menu.color(90,95,100));
            if(available) {
                Menu.text(item==16?"x"+p.potions():"T"+tier(item),left+5,top+49,11,tierColor(tier(item)));
                DrawRectangle(left+5,top+61,56,2,tierColor(tier(item)));
            } else { Menu.icon("lock",left+47,top+45,13,Menu.MUTED); }

            if(equipped(p,item)) { DrawRectangleLines(left+1,top+1,64,64,Menu.TEAL);DrawRectangle(left+49,top+3,14,3,Menu.TEAL); }
            if(selected==item)DrawRectangleLines(left,top,66,66,Menu.GOLD);
            if(inside(mouse,left,top,66,66)) {
                hover=item;
                if(IsMouseButtonPressed(MOUSE_BUTTON_LEFT)) {
                    selected=item;
                    if(available && lastClicked==item && GetTime()-lastClick<.35) { equip(p,item);dragged=-1;lastClicked=-1; }
                    else { if(available) { dragged=item;dragX=mouse.x();dragY=mouse.y(); }lastClicked=item;lastClick=GetTime(); }
                }
            }
        }
        if(IsMouseButtonReleased(MOUSE_BUTTON_LEFT))dragged=-1;
        Menu.frame(x+400,y+330,510,130,Menu.color(39,46,37),Menu.LINE);Menu.text(name(p,selected),x+416,y+340,23,tierColor(tier(selected)));
        int delta=selected<4?p.weaponBonus(selected)-p.weaponBonus(p.weaponTier()):selected<16?p.armorBonus(slot(selected),tier(selected))-p.armorBonus(slot(selected),p.armorTier(slot(selected))):0;
        String compare=selected<4?"DAMAGE  "+p.damage()+"  >  "+(p.damage()+delta):selected<16?"DEFENSE  "+p.defense()+"  >  "+(p.defense()+delta):"Restores up to 60 HP / "+p.potions()+" stored";
        Menu.text(compare+(selected<16?String.format("  (%+d)",delta):""),x+416,y+377,17,delta<0?Menu.RED:delta>0?Menu.TEAL:Menu.TEXT);
        boolean wearing=equipped(p,selected);String action=!owned(p,selected)?"LOCKED / VISIT FORGE":selected==16?(p.health()<p.maxHealth()?"USE POTION":"HEALTH FULL"):wearing&&selected==0?"STARTER EQUIPPED":wearing?"UNEQUIP":"EQUIP [ENTER]";
        if(Menu.button(action,x+416,y+408,245,owned(p,selected) && !(wearing && selected==0) && (selected!=16 || p.health()<p.maxHealth()))) { if(wearing)remove(p,selected);else equip(p,selected); }
        Menu.text(wearing?"EQUIPPED":owned(p,selected)?"OWNED":"NOT OWNED",x+708,y+421,15,wearing?Menu.TEAL:Menu.MUTED);
        Menu.text("HEALTH "+p.maxHealth()+"   DAMAGE "+p.damage(),x+24,y+480,18,Menu.TEXT);Menu.text("STAMINA "+p.maxStamina()+"   DEFENSE "+p.defense(),x+24,y+510,18,Menu.TEAL);
        Menu.text(String.format("SPEED %.1f   RECOVERY %d/s",p.movementSpeed(),p.staminaRecovery()),x+24,y+540,16,Menu.MUTED);
        if(Menu.button("USE POTION / "+p.potions(),x+400,y+485,220,p.potions()>0 && p.health()<p.maxHealth()))p.usePotion();
        if(Menu.button("BEST GEAR",x+635,y+485,130,true)) { p.equipWeapon(p.weaponUnlocked());for(ArmorSlot slot:ArmorSlot.values())p.equipArmor(slot,p.armorUnlocked()); }
        boolean close=Menu.button("RETURN ["+inventoryKey+"]",x+780,y+485,130,true);
        Menu.wrapped("Select to compare. Double-click, Enter, or drag to equip. Right-click a slot to remove armor.",x+400,y+545,510,13,Menu.MUTED);
        if(dragged>=0 && Math.abs(mouse.x()-dragX)+Math.abs(mouse.y()-dragY)>6) { Menu.panel((int)mouse.x()+12,(int)mouse.y()+12,60,60);thumbnail(dragged,(int)mouse.x()+14,(int)mouse.y()+14,56,WHITE); }
        else if(hover>=0 && hover!=selected) {
            int hx=Math.min(GetScreenWidth()-280,(int)mouse.x()+16),hy=Math.max(12,(int)mouse.y()-42);Menu.panel(hx,hy,260,34);Menu.text(name(p,hover),hx+10,hy+9,15,tierColor(tier(hover)));
        }
        return close;
    }
    @Override public void close() { for(RenderTexture t:thumbnails)if(t!=null)UnloadRenderTexture(t);UnloadRenderTexture(preview);UnloadShader(shader); }
}
