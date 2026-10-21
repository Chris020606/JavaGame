import Config.Controls;
import static Config.Controls.Action.*;
import java.nio.file.*;
import java.io.IOException;
import assets.AssetPaths;
import Entities.Player;
import Entities.Enemy;
import Scene.Scene;
import Scene.SceneManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import static com.raylib.Raylib.*;
import static com.raylib.Colors.WHITE;

public class Game {
    private final Settings settings=new Settings();
    private final Path settingsPath=AppData.directory().resolve("settings.properties"), savePath=AppData.directory().resolve("save.properties");
    private GameState settingsReturn=GameState.MAIN_MENU;
    private Controls.Action rebinding;
    private String nameDraft="", menuMessage="";
    private final ArrayList<Projectile> projectiles=new ArrayList<>();
    private Player player;
    private final WorldAssets assets = new WorldAssets();
    private GameState state = GameState.MAIN_MENU;
    private SceneManager sceneManager = new SceneManager();
    private Progression progress = new Progression();
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final Random random = new Random();
    private final WaveDirector director = new WaveDirector(random);
    private final GameAudio audio = new GameAudio();
    private final CombatFeedback feedback = new CombatFeedback();
    private int wave, combo;
    private float comboWindow, inputBuffer, attackElapsed, hurtFlash;
    private boolean bufferedHeavy, impactPending;
    private Combat.Attack activeAttack = Combat.attack(1,false);
    private float nextWave = 5, attackCooldown, slashTime, noticeTime;
    private String notice = "";
    private boolean workshop;
    private boolean quitRequested;
    private final Camera2D uiCamera = new Camera2D().zoom(1);

    public Game() {
        settings.load(settingsPath); menuMessage=settings.warning;
        Scene scene = sceneManager.createScene("Outpost");
        sceneManager.setOpenScene(scene);
        Player knight = new Player();
        knight.setName(settings.playerName);
        knight.setControls(settings.controls);
        knight.setPosition(new Vector3().x(0).y(0).z(4));
        knight.setModel(LoadModel(AssetPaths.path("resources/knight.glb")));
        knight.loadAnimations(AssetPaths.path("resources/knight.glb"));
        knight.setAnimationFPS(60);
        knight.resetCombat();
        setPlayer(knight);
        scene.getCameraController().update(0);
        applySettings();
    }

    private void start() {
        progress = new Progression(); enemies.clear(); projectiles.clear(); wave = 0; nextWave = 5;
        attackCooldown = slashTime = noticeTime = 0; workshop = false;
        player.getPosition().x(0).y(0).z(4);
        player.resetCombat();
        director.reset(); feedback.clear(); combo=0;
        comboWindow=inputBuffer=attackElapsed=hurtFlash=0; impactPending=false;
        player.setName(settings.playerName); menuMessage="";
        state = GameState.PLAYING;
    }
    private boolean nearBase() { return Math.hypot(player.getPosition().x(), player.getPosition().z()) < 6; }
    private void notifyPlayer(String message) { notice = message; noticeTime = 3; }

    public void update(float dt) {
        dt = Math.max(0, Math.min(dt, .05f));
        if (state==GameState.LOGIN) {
            for (int ch=GetCharPressed();ch!=0;ch=GetCharPressed())
                if (nameDraft.length()<16 && (Character.isLetterOrDigit(ch)&&ch<128 || ch==' '||ch=='_'||ch=='-')) nameDraft+=(char)ch;
            if (IsKeyPressed(KEY_BACKSPACE)&&!nameDraft.isEmpty()) nameDraft=nameDraft.substring(0,nameDraft.length()-1);
            if (IsKeyPressed(KEY_ESCAPE)) state=GameState.MAIN_MENU;
            else if (IsKeyPressed(KEY_ENTER)) confirmName();
            return;
        }
        if (state==GameState.SETTING_MENU) {
            if (rebinding!=null) {
                int key=GetKeyPressed();
                if (key==KEY_ESCAPE) { rebinding=null; menuMessage=""; }
                else if (key!=0) try { settings.controls.bind(rebinding,key); rebinding=null; menuMessage="Binding updated."; }
                    catch (IllegalArgumentException e) { menuMessage=e.getMessage(); }
            } else if (IsKeyPressed(KEY_ESCAPE)) closeSettings();
            return;
        }
        if (settings.controls.pressed(MUTE)) { settings.muted=!settings.muted; applySettings(); persistSettings(); }
        if (IsKeyPressed(KEY_ESCAPE)) {
            if (workshop) workshop = false;
            else if (state == GameState.PLAYING) state = GameState.PAUSED;
            else if (state == GameState.PAUSED) state = GameState.PLAYING;
            return;
        }
        if ((state==GameState.PLAYING||state==GameState.PAUSED)&&settings.controls.pressed(SAVE)) { saveCurrent(); return; }
        if (settings.controls.pressed(LOAD)) { loadCurrent(); return; }
        if (IsKeyPressed(KEY_ENTER)) {
            if (state == GameState.MAIN_MENU) { requestNewGame(); return; }
            if (state == GameState.GAME_OVER) { start(); return; }
            if (state == GameState.PAUSED) { state = GameState.PLAYING; return; }
        }
        if (state != GameState.PLAYING) return;
        if (settings.controls.pressed(WORKSHOP)) {
            if (nearBase()) workshop = !workshop;
            else notifyPlayer("Return to the outpost to upgrade.");
        }
        if (workshop) return;
        noticeTime = Math.max(0, noticeTime - dt);
        attackCooldown = Math.max(0, attackCooldown - dt);
        slashTime = Math.max(0, slashTime - dt);
        hurtFlash=Math.max(0,hurtFlash-dt);
        comboWindow=Math.max(0,comboWindow-dt);
        inputBuffer=Math.max(0,inputBuffer-dt);
        feedback.update(dt);
        sceneManager.getOpenScene().updatesEntities(dt);
        float px = player.getPosition().x(), pz = player.getPosition().z();
        if (nearBase()) progress.heal(dt);
        // Impact occurs part-way through the swing, once only, in the locked facing arc.
        if (impactPending) {
            attackElapsed+=dt;
            if (attackElapsed>=activeAttack.impact()) { resolvePlayerHit(px,pz); impactPending=false; }
        }
        if (settings.controls.pressed(SLASH) || IsMouseButtonPressed(MOUSE_BUTTON_LEFT) || settings.controls.pressed(HEAVY)) {
            inputBuffer=.2f; bufferedHeavy=settings.controls.pressed(HEAVY);
        }
        if (inputBuffer>0 && attackCooldown<=0) {
            combo=bufferedHeavy?0:comboWindow>0?combo%3+1:1;
            activeAttack=Combat.attack(combo,bufferedHeavy);
            attackCooldown=activeAttack.duration(); comboWindow=attackCooldown+.75f;
            attackElapsed=0; impactPending=true; inputBuffer=0;
            player.attack(combo,bufferedHeavy,activeAttack.duration()); audio.play("swing");
        }
        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            if (enemy.health <= 0) {
                iterator.remove(); progress.rewardKill(); audio.play("kill");
                feedback.add(enemy.x,enemy.z,"+15 crystals",Menu.TEAL); continue;
            }
            boolean huntsPlayer=enemy.distance(px,pz)<(enemy.variant==1?11:enemy.variant==2?5:8);
            float tx=huntsPlayer?px:0, tz=huntsPlayer?pz:0;
            // Flank while approaching; converge only when close enough to swing.
            if (huntsPlayer && !enemy.ranged() && enemy.distance(px,pz)>3.5f) {
                double side=enemy.personality*Math.PI*2;
                tx+=(float)Math.cos(side)*1.8f; tz+=(float)Math.sin(side)*1.8f;
            }
            enemy.approach(tx,tz,dt);
            if (enemy.impactPending && enemy.attackTime<=.25f) {
                enemy.impactPending=false;
                if (enemy.ranged()) projectiles.add(new Projectile(enemy,wave));
                else if (enemy.targetsPlayer && Combat.inArc(enemy.x,enemy.z,enemy.facing,px,pz,2.2f,65)) {
                    float before=progress.health(); progress.damagePlayer(8+wave*2+(enemy.variant==2?5:0));
                    feedback.add(px,pz,"-"+Math.round(before-progress.health()),Menu.RED); hurtFlash=.25f; audio.play("hurt");
                } else if (!enemy.targetsPlayer && enemy.distance(0,0)<2.3f) {
                    float before=progress.baseHealth(); progress.damageBase(10+wave*2);
                    feedback.add(0,0,"OUTPOST -"+Math.round(before-progress.baseHealth()),Menu.RED); audio.play("hurt");
                }
            }
            if (enemy.stun<=0 && enemy.attackTime<=0 && enemy.cooldown<=0 && enemy.distance(huntsPlayer?px:0,huntsPlayer?pz:0)<enemy.attackRange())
                enemy.beginAttack(huntsPlayer?px:0,huntsPlayer?pz:0,huntsPlayer);
        }
        separateEnemies(dt);
        for (Iterator<Projectile> shots=projectiles.iterator();shots.hasNext();) {
            Projectile shot=shots.next(); shot.update(dt);
            boolean hit=shot.playerTarget?shot.hits(px,pz,.65f):shot.hits(0,0,1.8f);
            if (hit) {
                float before=shot.playerTarget?progress.health():progress.baseHealth();
                if (shot.playerTarget) { progress.damagePlayer(shot.damage); hurtFlash=.25f; }
                else progress.damageBase(shot.damage);
                float lost=before-(shot.playerTarget?progress.health():progress.baseHealth());
                feedback.add(shot.playerTarget?px:0,shot.playerTarget?pz:0,(shot.playerTarget?"-":"OUTPOST -")+Math.round(lost),Menu.RED);
                audio.play("hurt");
            }
            if (hit || shot.life<=0) shots.remove();
        }
        if (progress.defeated()) { state = GameState.GAME_OVER; return; }
        WaveDirector.Spawn spawn=director.update(dt,enemies.size());
        if (director.wave()!=wave) { wave=director.wave(); audio.play("wave"); notifyPlayer("WAVE "+wave+"  /  Raiders approaching the trails"); }
        if (spawn!=null) enemies.add(new Enemy(spawn.x(),spawn.z(),spawn.wave(),spawn.variant(),random.nextFloat()));
        nextWave=director.rest();
    }
    private void resolvePlayerHit(float px,float pz) {
        slashTime=.16f;
        boolean hit=false;
        for (Enemy enemy:enemies) if (enemy.health>0 && Combat.inArc(px,pz,player.getRotation().y(),enemy.x,enemy.z,activeAttack.range(),activeAttack.halfAngle())) {
            float damage=Math.min(enemy.health,Math.round(progress.damage()*activeAttack.multiplier()));
            enemy.hit(damage,px,pz,activeAttack.multiplier()>1?.8f:.35f);
            feedback.add(enemy.x,enemy.z,"-"+Math.round(damage),Menu.GOLD); hit=true;
        }
        if (hit) audio.play("hit");
    }
    private void separateEnemies(float dt) {
        for (int i=0;i<enemies.size();i++) for (int j=i+1;j<enemies.size();j++) {
            Enemy a=enemies.get(i), b=enemies.get(j);
            float dx=a.x-b.x,dz=a.z-b.z,d=(float)Math.hypot(dx,dz);
            if (d>=1.1f) continue;
            if (d<.001f) { dx=1;dz=0;d=1; }
            float push=Math.min(.055f,dt*1.5f);
            if (a.attackTime<=0) { a.x+=dx/d*push; a.z+=dz/d*push; }
            if (b.attackTime<=0) { b.x-=dx/d*push; b.z-=dz/d*push; }
        }
    }

    public void render() {
        drawWorld();
        // Jaylib 6.0.1-0's SDL backend restores a framebuffer-pixel projection
        // after 3D. Scale the overlay to window pixels, matching GetMousePosition.
        // Recompute each frame for resizing and moving between Retina/1x displays.
        uiCamera.zoom((float)GetRenderWidth() / Math.max(1, GetScreenWidth()));
        BeginMode2D(uiCamera);
        try {
            drawEnemyHealth();
            if (state == GameState.MAIN_MENU) { drawMainMenu(); return; }
            if (state == GameState.LOGIN) { drawNameEntry(); return; }
            if (state == GameState.SETTING_MENU) { drawSettings(); return; }
            feedback.draw(sceneManager.getOpenScene().getCameraController().getCamera());
            if (hurtFlash>0) {
                Color tint=Fade(Menu.RED,hurtFlash*1.5f);
                DrawRectangle(0,0,GetScreenWidth(),9,tint); DrawRectangle(0,GetScreenHeight()-9,GetScreenWidth(),9,tint);
                DrawRectangle(0,0,9,GetScreenHeight(),tint); DrawRectangle(GetScreenWidth()-9,0,9,GetScreenHeight(),tint);
            }
            drawHud();
            if (workshop) drawWorkshop();
            if (state == GameState.PAUSED || state == GameState.GAME_OVER) drawOverlay();
        } finally {
            EndMode2D();
        }
    }
    private Vector3 v(float x, float y, float z) { return new Vector3().x(x).y(y).z(z); }
    private void drawWorld() {
        BeginMode3D(sceneManager.getOpenScene().getCameraController().getCamera());
        DrawPlane(v(0, -.06f, 0), new Vector2().x(68).y(68), Menu.color(57, 79, 65));
        DrawCylinder(v(0, 0, 0), 5.7f, 5.7f, .08f, 48, Menu.color(70, 92, 82));
        assets.drawEnvironment();
        if (director.warning()) {
            DrawCylinderWires(v(director.x(),.08f,director.z()),1.4f,1.4f,.15f,24,Menu.RED);
            DrawLine3D(v(director.x(),0,director.z()),v(director.x(),3,director.z()),Menu.GOLD);
        }
        DrawModelEx(player.getModel(), player.getPosition(), v(0, 1, 0), player.getRotation().y(), v(1, 1, 1), hurtFlash>0?Menu.RED:WHITE);
        assets.drawPlayerWeapon(player);
        if (player.isAttacking()) drawArc(player.getPosition().x(),player.getPosition().z(),player.getRotation().y(),activeAttack.range(),activeAttack.halfAngle(),slashTime>0?Menu.GOLD:Menu.TEAL);
        for (Enemy enemy:enemies) if (enemy.attackTime>.25f)
            drawArc(enemy.x,enemy.z,enemy.facing,enemy.ranged()?8:2.2f,enemy.ranged()?8:65,enemy.ranged()?Menu.GOLD:Menu.RED);
        for (Projectile shot:projectiles) {
            if (shot.magic) DrawSphere(v(shot.x,1,shot.z),.22f,Menu.color(180,124,235));
            else DrawLine3D(v(shot.x,1,shot.z),v(shot.x-shot.vx*.07f,1,shot.z-shot.vz*.07f),Menu.GOLD);
        }
        for (Enemy enemy : enemies) {
            assets.drawEnemy(enemy);
        }
        EndMode3D();
    }
    private void drawArc(float x,float z,float facing,float radius,float halfAngle,Color color) {
        Vector3 last=null;
        for (int i=0;i<=24;i++) {
            double angle=Math.toRadians(facing-halfAngle+2*halfAngle*i/24);
            Vector3 point=v(x+(float)Math.sin(angle)*radius,.12f,z+(float)Math.cos(angle)*radius);
            if (last!=null) DrawLine3D(last,point,color);
            if (i==0 || i==24) DrawLine3D(v(x,.12f,z),point,color);
            last=point;
        }
    }
    private void drawEnemyHealth() {
        for (Enemy enemy : enemies) {
            Vector2 screen = GetWorldToScreen(v(enemy.x, 2.1f, enemy.z), sceneManager.getOpenScene().getCameraController().getCamera());
            Menu.bar((int)screen.x()-22, (int)screen.y(), 44, enemy.health, enemy.maxHealth, enemy.variant==2?Menu.GOLD:Menu.RED);
            String label=enemy.attackTime>.25f?"! ATTACK":enemy.label();
            Menu.text(label,(int)screen.x()-MeasureText(label,10)/2,(int)screen.y()-14,10,enemy.attackTime>.25f?Menu.RED:Menu.TEXT);
        }
    }
    private void drawHud() {
        int w = GetScreenWidth(), h = GetScreenHeight();
        Menu.panel(20, 20, 280, 108);
        Menu.text(player.getName()+" / LV " + progress.characterLevel(), 36, 34, 17, Menu.TEXT);
        Menu.text((int)progress.health() + " / " + progress.maxHealth() + " HP", 36, 62, 16, Menu.MUTED);
        Menu.bar(36, 86, 248, progress.health(), progress.maxHealth(), Menu.TEAL);
        Menu.text("Armor " + progress.armorLevel() + "   /   Damage " + progress.damage(), 36, 104, 14, Menu.MUTED);
        Menu.panel(w-272, 20, 252, 108);
        Menu.text(progress.crystals() + "  CRYSTALS", w-254, 36, 24, Menu.GOLD);
        Menu.text("WAVE " + wave + "   /   " + enemies.size() + " RAIDERS", w-254, 71, 18, Menu.TEXT);
        Menu.text(director.remaining()>0 ? director.remaining()+" incoming / "+director.total()+" total" : enemies.isEmpty() ? "Next wave in " + (int)Math.ceil(nextWave) + "s" : "Eliminate remaining raiders", w-254, 101, 14, Menu.MUTED);
        Menu.panel(20, h-112, 280, 90);
        Menu.text("OUTPOST / LV " + progress.baseLevel(), 36, h-98, 18, Menu.TEXT);
        Menu.text((int)progress.baseHealth() + " / " + progress.maxBaseHealth() + " integrity", 36, h-73, 16, Menu.MUTED);
        Menu.bar(36, h-45, 248, progress.baseHealth(), progress.maxBaseHealth(), Menu.GOLD);
        int cx=w/2-150;
        Menu.panel(cx,h-151,300,65);
        Menu.text(attackCooldown>0?activeAttack.label():"SWORD READY",cx+14,h-140,17,attackCooldown>0?Menu.GOLD:Menu.TEAL);
        Menu.bar(cx+14,h-111,272,activeAttack.duration()-attackCooldown,activeAttack.duration(),Menu.GOLD);
        Menu.text("Move "+settings.controls.label(FORWARD)+settings.controls.label(LEFT)+settings.controls.label(BACK)+settings.controls.label(RIGHT)+" / "+settings.controls.label(SLASH)+" slash / "+settings.controls.label(HEAVY)+" heavy", 320, h-60, 14, Menu.TEXT);
        drawMinimap(w-180,148);
        Menu.text("KILLS "+progress.kills()+"   /   HEAVY "+Math.round(progress.damage()*1.8f)+" DMG",36,131,13,Menu.GOLD);
        Menu.text(!audio.ready()?"Audio unavailable":audio.muted()?settings.controls.label(MUTE)+" / SOUND OFF":settings.controls.label(MUTE)+" / SOUND ON",w-180,320,13,Menu.MUTED);
        Menu.text(settings.controls.label(WORKSHOP)+" upgrades / ESC pause / "+settings.controls.label(SAVE)+" save", 320, h-35, 16, Menu.MUTED);
        if (nearBase()) Menu.text("SAFE HAVEN  /  Healing +" + (3+progress.baseLevel()) + " HP/s", 24, 166, 14, Menu.TEAL);
        if (noticeTime > 0) Menu.text(notice, (w-MeasureText(notice, 20))/2, 196, 20, Menu.GOLD);
    }
    private void drawMinimap(int x,int y) {
        Menu.panel(x,y,160,160);
        Menu.text("APPROACHES",x+12,y+10,12,Menu.MUTED);
        int cx=x+80,cy=y+90;
        DrawCircleLines(cx,cy,59,Menu.LINE);
        DrawLine(cx-59,cy,cx+59,cy,Menu.LINE); DrawLine(cx,cy-59,cx,cy+59,Menu.LINE);
        DrawCircle(cx,cy,5,Menu.TEAL);
        for (Enemy e:enemies) DrawCircle(cx+(int)(e.x*2.5f),cy+(int)(e.z*2.5f),3,e.variant==2?Menu.GOLD:Menu.RED);
        if (director.warning()) DrawCircleLines(cx+(int)(director.x()*2.5f),cy+(int)(director.z()*2.5f),6,Menu.GOLD);
        DrawCircle(cx+(int)(player.getPosition().x()*2.5f),cy+(int)(player.getPosition().z()*2.5f),4,Menu.TEXT);
    }
    private void dim() { DrawRectangle(0, 0, GetScreenWidth(), GetScreenHeight(), Fade(Menu.INK, .86f)); }
    private void drawMainMenu() {
        dim(); int x = (GetScreenWidth()-640)/2, y = (GetScreenHeight()-420)/2;
        Menu.text("A WARDEN'S LAST STAND", x, y, 18, Menu.TEAL);
        Menu.text("OUTPOST", x, y+42, 72, Menu.TEXT);
        Menu.text("Fight. Gather. Fortify.", x, y+132, 28, Menu.GOLD);
        Menu.text("Defeat raiders for crystals. Build a stronger outpost.", x, y+193, 20, Menu.MUTED);
        Menu.text("Welcome, "+settings.playerName+". Your next defense awaits.", x, y+224, 20, Menu.MUTED);
        if (Menu.button("NEW GAME [ENTER]", x, y+285, 280, true)) requestNewGame();
        if (Menu.button("LOAD GAME",x+300,y+285,160,Files.exists(savePath))) loadCurrent();
        if (Menu.button("QUIT", x+480, y+285, 140, true)) quitRequested = true;
        if (Menu.button("SETTINGS",x,y+344,200,true)) openSettings();
        Menu.text("Sound, display, and key bindings",x+220,y+357,17,Menu.MUTED);
        Menu.text(menuMessage.isEmpty()?"Hold right mouse to orbit. ESC pauses.":menuMessage, x, y+401, 15, Menu.MUTED);
    }
    private void drawWorkshop() {
        dim(); int width = Math.min(900, GetScreenWidth()-60), x = (GetScreenWidth()-width)/2, y = (GetScreenHeight()-360)/2;
        Menu.text("THE WORKSHOP", x, y, 34, Menu.TEXT);
        Menu.text(progress.crystals() + " crystals  /  Time paused while upgrading", x, y+48, 18, Menu.GOLD);
        int card = (width-24)/3;
        String[] names = {"OUTPOST", "ARMOR", "CHARACTER"};
        String[] descriptions = {"+100 base HP; full repair", "+2 damage reduction", "+10 damage; +25 max HP"};
        for (Progression.Upgrade type : Progression.Upgrade.values()) {
            int i = type.ordinal(), left = x+i*(card+12);
            Menu.panel(left, y+94, card, 196);
            Menu.text(names[i], left+16, y+112, 22, Menu.TEXT);
            Menu.text("LEVEL " + progress.level(type) + " > " + (progress.level(type)+1), left+16, y+147, 16, Menu.TEAL);
            Menu.text(descriptions[i], left+16, y+181, 14, Menu.MUTED);
            if (Menu.button("UPGRADE / " + progress.cost(type), left+16, y+225, card-32, progress.crystals() >= progress.cost(type)) && progress.purchase(type)) audio.play("upgrade");
        }
        if (Menu.button("RETURN ["+settings.controls.label(WORKSHOP)+" / ESC]", x, y+312, 350, true)) workshop = false;
    }
    private void drawOverlay() {
        dim(); int x=(GetScreenWidth()-440)/2,y=(GetScreenHeight()-440)/2;
        boolean lost=state==GameState.GAME_OVER;
        Menu.text(lost?"OUTPOST FALLEN":"DEFENSE PAUSED",x,y,36,Menu.TEXT);
        Menu.text(lost?"Wave "+wave+" / "+progress.kills()+" defeated":player.getName()+" / Wave "+wave,x,y+54,20,Menu.MUTED);
        if (Menu.button(lost?"TRY AGAIN":"RESUME",x,y+100,440,true)) { if (lost) start(); else state=GameState.PLAYING; }
        if (Menu.button("SAVE GAME",x,y+154,212,!lost)) saveCurrent();
        if (Menu.button("LOAD GAME",x+228,y+154,212,Files.exists(savePath))) loadCurrent();
        if (Menu.button("SETTINGS",x,y+208,440,true)) openSettings();
        if (Menu.button("SAVE & MAIN MENU",x,y+262,440,!lost)) { if (saveCurrent()) state=GameState.MAIN_MENU; }
        if (lost && Menu.button("MAIN MENU",x,y+262,440,true)) state=GameState.MAIN_MENU;
        Menu.text(menuMessage,x,y+325,15,Menu.GOLD);
        Menu.text("Closing the game also saves an active defense.",x,y+356,15,Menu.MUTED);
    }
    private void requestNewGame() {
        nameDraft=settings.playerName.equals("Warden")?"":settings.playerName;
        menuMessage=""; state=GameState.LOGIN;
        while (GetCharPressed()!=0) { }
    }
    private void confirmName() {
        settings.playerName=Settings.cleanName(nameDraft); persistSettings(); start();
    }
    private void drawNameEntry() {
        dim(); int x=(GetScreenWidth()-560)/2,y=(GetScreenHeight()-320)/2;
        Menu.text("NAME YOUR WARDEN",x,y,34,Menu.TEXT);
        Menu.text("Letters, numbers, spaces, - or _ (up to 16)",x,y+55,18,Menu.MUTED);
        Menu.panel(x,y+100,560,60);
        Menu.text(nameDraft.isEmpty()?"Type a name...":nameDraft+"_",x+16,y+118,24,nameDraft.isEmpty()?Menu.MUTED:Menu.TEXT);
        if (Menu.button("BEGIN [ENTER]",x,y+195,270,true)) confirmName();
        if (Menu.button("CLEAR",x+290,y+195,130,true)) nameDraft="";
        if (Menu.button("BACK",x+440,y+195,120,true)) state=GameState.MAIN_MENU;
        Menu.text("Leave blank to play as Warden.",x,y+270,17,Menu.MUTED);
    }
    private void openSettings() { settingsReturn=state; state=GameState.SETTING_MENU; menuMessage=""; rebinding=null; }
    private void closeSettings() { persistSettings(); rebinding=null; state=settingsReturn; }
    private void applySettings() {
        audio.configure(settings.volume,settings.muted);
        sceneManager.getOpenScene().getCameraController().setSensitivity(settings.sensitivity);
        if (IsWindowFullscreen()!=settings.fullscreen) ToggleFullscreen();
    }
    private void persistSettings() {
        try { settings.save(settingsPath); }
        catch (IOException e) { menuMessage="Could not save settings. Check folder permissions."; System.err.println(e.getMessage()); }
    }
    private void drawSettings() {
        dim(); int x=(GetScreenWidth()-820)/2,y=(GetScreenHeight()-580)/2;
        Menu.text("SETTINGS",x,y,34,Menu.TEXT);
        Menu.text("Volume "+Math.round(settings.volume*100)+"%",x,y+68,20,Menu.TEXT);
        if (Menu.button("-",x+240,y+54,50,true)) { settings.volume=Math.max(0,settings.volume-.1f); applySettings(); }
        if (Menu.button("+",x+300,y+54,50,true)) { settings.volume=Math.min(1,settings.volume+.1f); applySettings(); audio.play("hit"); }
        if (Menu.button(settings.muted?"UNMUTE":"MUTE",x+430,y+54,180,true)) { settings.muted=!settings.muted;applySettings(); }
        Menu.text("Camera speed "+Math.round(settings.sensitivity*100),x,y+122,20,Menu.TEXT);
        if (Menu.button("-",x+240,y+108,50,true)) { settings.sensitivity=Math.max(.05f,settings.sensitivity-.05f); applySettings(); }
        if (Menu.button("+",x+300,y+108,50,true)) { settings.sensitivity=Math.min(.5f,settings.sensitivity+.05f); applySettings(); }
        if (Menu.button(settings.fullscreen?"WINDOWED":"FULLSCREEN",x+430,y+108,260,true)) { settings.fullscreen=!settings.fullscreen;applySettings(); }
        Menu.text("Click an action, then press a key. ESC cancels. Mouse buttons stay fixed.",x,y+174,16,Menu.MUTED);
        Controls.Action[] actions=Controls.Action.values();
        for (int i=0;i<actions.length;i++) {
            Controls.Action action=actions[i];
            if (Menu.button(action.label+" / "+(rebinding==action?"PRESS KEY":settings.controls.label(action)),x+(i%2)*420,y+210+(i/2)*49,400,rebinding==null)) {
                rebinding=action;menuMessage="Press a key for "+action.label+" (ESC cancels)."; while(GetKeyPressed()!=0) { }
            }
        }
        Menu.text(menuMessage,x,y+466,15,Menu.GOLD);
        if (Menu.button("BACK / SAVE",x,y+510,260,rebinding==null)) closeSettings();
        if (Menu.button("RESTORE DEFAULTS",x+280,y+510,280,rebinding==null)) {
            settings.controls.reset();settings.volume=.6f;settings.sensitivity=.15f;settings.muted=false;settings.fullscreen=false;applySettings();menuMessage="Defaults restored.";
        }
    }
    boolean saveCurrent() {
        if (progress.defeated()) return false;
        try {
            SaveGame.write(savePath,new SaveGame.Snapshot(player.getName(),player.getPosition().x(),player.getPosition().z(),player.getRotation().y(),progress,director.snapshot(),enemies.stream().filter(e->e.health>0).toList()));
            menuMessage="Game saved."; notifyPlayer(menuMessage); return true;
        } catch (IOException e) { menuMessage="Save failed. Previous save was kept.";notifyPlayer(menuMessage);System.err.println(e.getMessage());return false; }
    }
    boolean loadCurrent() {
        try {
            SaveGame.Snapshot saved=SaveGame.read(savePath);
            progress=saved.progression();director.restore(saved.wave());enemies.clear();enemies.addAll(saved.enemies());projectiles.clear();
            player.setName(saved.name());settings.playerName=saved.name();player.getPosition().x(saved.x()).z(saved.z());player.getRotation().y(saved.facing());
            player.resetCombat();feedback.clear();wave=director.wave();nextWave=director.rest();workshop=false;
            combo=0;comboWindow=inputBuffer=attackElapsed=hurtFlash=slashTime=noticeTime=0;attackCooldown=.3f;impactPending=false;
            sceneManager.getOpenScene().getCameraController().update(0);state=GameState.PAUSED;menuMessage="Game loaded. Press Enter to resume.";return true;
        } catch (IOException e) { menuMessage="Could not load game. Current defense was kept.";notifyPlayer(menuMessage);System.err.println(e.getMessage());return false; }
    }
    java.util.List<Enemy> getEnemies() { return java.util.Collections.unmodifiableList(enemies); }
    Progression getProgression() { return progress; }
    boolean isWorkshopOpen() { return workshop; }
    public boolean shouldQuit() { return quitRequested; }
    public void dispose() { persistSettings(); if (state==GameState.PLAYING||state==GameState.PAUSED||state==GameState.SETTING_MENU&&settingsReturn==GameState.PAUSED) saveCurrent(); audio.close(); assets.close(); player.dispose(); }
    public GameState getState() { return state; }
    public void setState(GameState state) { this.state = state; }
    public SceneManager getSceneManager() { return sceneManager; }
    public void setSceneManager(SceneManager sceneManager) { this.sceneManager = sceneManager; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) {
        this.player = player;
        Scene scene = sceneManager.getOpenScene();
        scene.addEntity(player); scene.setMainCamera(player); player.setCameraController(scene.getCameraController());
    }
}
