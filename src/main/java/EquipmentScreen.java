import Entities.*;
import assets.AssetPaths;
import static com.raylib.Raylib.*;
import static com.raylib.Colors.WHITE;

/** Equipment grid with click/drag equipping and an animated-rig 3D paper doll. */
public final class EquipmentScreen implements AutoCloseable {
    private final RenderTexture preview=LoadRenderTexture(640,800);
    private final Shader shader=LoadShader(AssetPaths.path("resources/outpost/daylight.vs"),AssetPaths.path("resources/outpost/preview.fs"));
    private final Camera3D camera=new Camera3D()._position(new Vector3().x(3).y(2.9f).z(5.5f)).target(new Vector3().y(1.3f)).up(new Vector3().y(1)).fovy(40).projection(CAMERA_PERSPECTIVE);
    private int selected,dragged=-1;
    private float yaw;
    private boolean orbit;
    private static Color tierColor(int tier) { return tier==3?Menu.GOLD:tier==2?Menu.TEAL:tier==1?Menu.color(182,201,220):Menu.MUTED; }
    public void prepare(WorldAssets assets,Player player,Progression progress) {
        Vector3 position=player.getPosition();float facing=player.getRotation().y();
        BeginTextureMode(preview);ClearBackground(Menu.color(20,30,39));BeginMode3D(camera);
        DrawCylinder(new Vector3().y(-.03f),.95f,.95f,.06f,32,Menu.color(42,61,67));
        player.setPosition(new Vector3());player.getRotation().y(yaw);player.poseForInventory();
        assets.previewPass(shader);assets.preparePlayer(player);
        assets.drawPlayerBody(player,progress,WHITE);
        assets.drawPlayerWeapon(player,progress.heroClass().ordinal(),progress.weaponTier());assets.drawArmor(player,progress);
        assets.endPreview();player.setPosition(position);player.getRotation().y(facing);
        EndMode3D();EndTextureMode();
    }
    private static boolean inside(Vector2 mouse,int x,int y,int w,int h) { return mouse.x()>=x && mouse.x()<x+w && mouse.y()>=y && mouse.y()<y+h; }
    private static int tier(int item) { return item<4?item:1+(item-4)/4; }
    private static ArmorSlot slot(int item) { return ArmorSlot.values()[(item-4)%4]; }
    private static boolean owned(Progression p,int item) { return item<4?item<=p.weaponUnlocked():item<16?tier(item)<=p.armorUnlocked():item==16 && p.potions()>0; }
    private static boolean equipped(Progression p,int item) { return item<4?p.weaponTier()==item:item<16 && p.armorTier(slot(item))==tier(item); }
    private static String icon(Progression p,int item) { return item<4?switch(p.heroClass()) {case WARRIOR -> "sword";case ARCHER -> "bow";case MAGE -> "staff";}:item<16?slot(item).icon():"heart"; }
    private static String name(Progression p,int item) { return item<4?p.weaponName(item):item<16?p.armorName(slot(item),tier(item)):"Healing potions"; }
    private static void equip(Progression p,int item) { if(item<4) p.equipWeapon(item);else if(item<16) p.equipArmor(slot(item),tier(item));else p.usePotion(); }
    public boolean draw(Progression p,String inventoryKey) {
        int x=(GetScreenWidth()-940)/2,y=(GetScreenHeight()-600)/2;
        Vector2 mouse=GetMousePosition();Menu.panel(x,y,940,600);
        Menu.text("WARDEN / EQUIPMENT",x+24,y+16,29,Menu.TEXT);
        Menu.text(p.crystals()+" CRYSTALS",x+720,y+24,19,Menu.GOLD);
        Menu.text(p.heroClass().label+"  /  "+p.difficulty().label,x+24,y+62,17,Menu.MUTED);
        DrawTexturePro(preview.texture(),new Rectangle().width(640).height(-800),new Rectangle().x(x+24).y(y+90).width(250).height(330),new Vector2(),0,WHITE);
        Menu.text("DRAG TO ROTATE",x+70,y+432,13,Menu.MUTED);
        Menu.text(p.weaponName(p.weaponTier()),x+24,y+452,14,tierColor(p.weaponTier()));
        if(IsMouseButtonPressed(MOUSE_BUTTON_LEFT) && inside(mouse,x+24,y+90,250,330)) orbit=true;
        if(orbit && IsMouseButtonDown(MOUSE_BUTTON_LEFT)) yaw+=GetMouseDelta().x()*.8f;
        if(IsMouseButtonReleased(MOUSE_BUTTON_LEFT)) orbit=false;
        for(int s=0;s<5;s++) {
            int sx=x+300,sy=y+90+s*70;
            Menu.panel(sx,sy,64,64);String kind=s==4?icon(p,p.weaponTier()):ArmorSlot.values()[s].icon();
            int level=s==4?p.weaponTier():p.armorTier(ArmorSlot.values()[s]);
            Menu.icon(kind,sx+20,sy+14,26,tierColor(level));Menu.text("T"+level,sx+24,sy+44,12,Menu.MUTED);
            if(IsMouseButtonPressed(MOUSE_BUTTON_RIGHT) && inside(mouse,sx,sy,64,64)) { if(s==4)p.equipWeapon(0);else p.equipArmor(ArmorSlot.values()[s],0); }
            if(IsMouseButtonReleased(MOUSE_BUTTON_LEFT) && dragged>=0 && inside(mouse,sx,sy,64,64)) {
                if(s==4 && dragged<4 || s<4 && dragged>=4 && dragged<16 && slot(dragged).ordinal()==s) equip(p,dragged);
            }
        }
        for(int item=0;item<18;item++) {
            int left=x+400+(item%6)*75,top=y+90+(item/6)*76;
            boolean available=item<17 && owned(p,item);Color color=item==17?Menu.LINE:tierColor(tier(item));
            DrawRectangleRounded(new Rectangle().x(left).y(top).width(66).height(66),.13f,6,Menu.color(37,50,59));
            if(item<17) {
                Menu.icon(icon(p,item),left+19,top+14,28,available?color:Menu.color(74,83,91));
                Menu.text(item==16?String.valueOf(p.potions()):available?"T"+tier(item):"LOCK",left+8,top+47,12,available?color:Menu.MUTED);
                if(equipped(p,item)) DrawRectangleLines(left+1,top+1,64,64,Menu.TEAL);
                if(selected==item) DrawRectangleLines(left,top,66,66,Menu.GOLD);
                if(IsMouseButtonPressed(MOUSE_BUTTON_LEFT) && inside(mouse,left,top,66,66)) { selected=item;if(available)dragged=item; }
                if(IsMouseButtonReleased(MOUSE_BUTTON_LEFT) && dragged==item && inside(mouse,left,top,66,66)) equip(p,item);
            }
        }
        if(IsMouseButtonReleased(MOUSE_BUTTON_LEFT)) dragged=-1;
        Menu.panel(x+400,y+330,510,130);
        Menu.text(name(p,selected),x+416,y+340,24,tierColor(tier(selected)));
        String comparison=selected<4?"Damage bonus +"+p.weaponBonus(selected)+" / Change "+String.format("%+d",p.weaponBonus(selected)-p.weaponBonus(p.weaponTier())):
            selected<16?"Defense +"+p.armorBonus(slot(selected),tier(selected))+" / Change "+String.format("%+d",p.armorBonus(slot(selected),tier(selected))-p.armorBonus(slot(selected),p.armorTier(slot(selected)))):"Restores up to 60 HP / "+p.potions()+" stored";
        Menu.text(comparison,x+416,y+378,17,Menu.TEXT);
        Menu.text(owned(p,selected)?"Click to equip, or drag into its matching slot.":"Not owned. Visit the workshop's equipment forge.",x+416,y+410,16,Menu.MUTED);
        Menu.text("HEALTH "+p.maxHealth()+"   DAMAGE "+p.damage(),x+24,y+480,18,Menu.TEXT);
        Menu.text("STAMINA "+p.maxStamina()+"   DEFENSE "+p.defense(),x+24,y+510,18,Menu.TEAL);
        Menu.text(String.format("SPEED %.1f   RECOVERY %d/s",p.movementSpeed(),p.staminaRecovery()),x+24,y+540,16,Menu.MUTED);
        if(Menu.button("USE POTION / "+p.potions(),x+400,y+485,235,p.potions()>0 && p.health()<p.maxHealth())) p.usePotion();
        boolean close=Menu.button("RETURN ["+inventoryKey+" / ESC]",x+655,y+485,255,true);
        Menu.text("Right-click equipped armor to remove it. Equipment and potion storage are saved.",x+400,y+548,13,Menu.MUTED);
        if(dragged>=0) { Menu.panel((int)mouse.x()+12,(int)mouse.y()+12,56,56);Menu.icon(icon(p,dragged),(int)mouse.x()+26,(int)mouse.y()+25,28,tierColor(tier(dragged))); }
        return close;
    }
    @Override public void close() { UnloadRenderTexture(preview);UnloadShader(shader); }
}
