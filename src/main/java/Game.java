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
    private final java.util.function.BooleanSupplier mouseAttack;
    private final Settings settings=new Settings();
    private final Path settingsPath=AppData.directory().resolve("settings.properties"), savePath=AppData.directory().resolve("save.properties");
    private GameState settingsReturn=GameState.MAIN_MENU;
    private Controls.Action rebinding;
    private String nameDraft="", menuMessage="";
    private final ArrayList<Projectile> projectiles=new ArrayList<>();
    private Player player;
    private GameState journalReturn=GameState.MAIN_MENU;
    private float storyTime;
    private HeroClass selectedClass=HeroClass.WARRIOR;
    private boolean inventory;
    private final ArrayList<HeroShot> heroShots=new ArrayList<>();
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

    public Game() { this(()->IsMouseButtonPressed(MOUSE_BUTTON_LEFT)); }
    Game(java.util.function.BooleanSupplier mouseAttack) {
        this.mouseAttack=mouseAttack;
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

    private HeroClass modelClass=HeroClass.WARRIOR;
    private void applyHeroModel() {
        if (modelClass!=selectedClass) {
            player.dispose();
            String path=AssetPaths.path(selectedClass==HeroClass.WARRIOR ? "resources/knight.glb" :
                selectedClass==HeroClass.ARCHER ? "resources/outpost/hero_archer.glb" : "resources/outpost/hero_mage.glb");
            player.setModel(LoadModel(path)); player.loadAnimations(path);
            if (player.getModel().meshCount()==0 || player.getAnimationCount()==0)
                throw new IllegalStateException("Missing hero model or animations: "+path);
            for (int i=0;i<player.getAnimationCount();i++)
                if (!IsModelAnimationValid(player.getModel(),new ModelAnimation(player.getAnimation()).position(i)))
                    throw new IllegalStateException("Hero animation skeleton mismatch: "+path);
            modelClass=selectedClass;
        }
        player.setWeaponStyle(selectedClass.ordinal());
        player.setMovementSpeed(selectedClass.speed);
    }
    Arena arena() { return Arena.HIGHLANDS; }
    Campaign.Chapter chapter() { return director.chapter(); }
    int chapterIndex() { return director.chapterIndex(); }
    private void start() {
        applyHeroModel(); storyTime=12;
        progress = new Progression(selectedClass); player.setMovementSpeed(selectedClass.speed); inventory=false; heroShots.clear(); enemies.clear(); projectiles.clear(); wave = 0; nextWave = 5;
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
        if(state==GameState.JOURNAL) {
            if(IsKeyPressed(KEY_ESCAPE)||IsKeyPressed(KEY_ENTER)) state=journalReturn;
            return;
        }
        if(state==GameState.LEVEL_COMPLETE || state==GameState.VICTORY) {
            if(settings.controls.pressed(SAVE)) saveCurrent();
            if(settings.controls.pressed(INVENTORY) && state==GameState.LEVEL_COMPLETE) { inventory=!inventory; return; }
            if(inventory && IsKeyPressed(KEY_ESCAPE)) { inventory=false;return; }
            if(IsKeyPressed(KEY_ENTER) && !inventory) { continueCampaign();return; }
            if(IsKeyPressed(KEY_ESCAPE) && !inventory) { saveCurrent();state=GameState.MAIN_MENU;return; }
            return;
        }
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
            if (inventory) inventory=false;
            else if (workshop) workshop = false;
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
        if (settings.controls.pressed(INVENTORY)) { inventory=!inventory; workshop=false; }
        if (inventory) return;
        if (settings.controls.pressed(POTION)) {
            notifyPlayer(progress.usePotion()?"Potion used / +60 HP":"Need missing health and a potion.");
        }
        if (settings.controls.pressed(WORKSHOP)) {
            if (nearBase()) workshop = !workshop;
            else notifyPlayer("Return to the outpost to upgrade.");
        }
        if (workshop) return;
        noticeTime = Math.max(0, noticeTime - dt); storyTime=Math.max(0,storyTime-dt);
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
        if (settings.controls.pressed(SLASH) || mouseAttack.getAsBoolean() || settings.controls.pressed(HEAVY)) {
            inputBuffer=.2f; bufferedHeavy=settings.controls.pressed(HEAVY);
        }
        if (inputBuffer>0 && attackCooldown<=0 && !player.isDodging()) {
            combo=bufferedHeavy?0:comboWindow>0?combo%3+1:1;
            activeAttack=progress.heroClass().attack(combo,bufferedHeavy);
            attackCooldown=activeAttack.duration(); comboWindow=attackCooldown+.75f;
            attackElapsed=0; impactPending=true; inputBuffer=0;
            player.attack(combo,bufferedHeavy,activeAttack.duration()); audio.play("swing");
        }
        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            if (enemy.health <= 0) {
                iterator.remove(); progress.rewardEnemy(enemy.boss()); audio.play("kill");
                feedback.add(enemy.x,enemy.z,"+"+enemy.reward()+" crystals",Menu.TEAL);
                if (enemy.boss()) notifyPlayer(chapter().title()+" DEFEATED / "+progress.weaponName(progress.weaponUnlocked())+" in inventory");
                continue;
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
                else if (enemy.targetsPlayer && !player.isDodging() && Combat.inArc(enemy.x,enemy.z,enemy.facing,px,pz,enemy.meleeRange(),65)) {
                    float before=progress.health(); progress.damagePlayer(enemy.meleeDamage(wave));
                    feedback.add(px,pz,"-"+Math.round(before-progress.health()),Menu.RED); hurtFlash=.25f; audio.play("hurt");
                } else if (!enemy.targetsPlayer && enemy.distance(0,0)<enemy.meleeRange()+.1f) {
                    float before=progress.baseHealth(); progress.damageBase(enemy.siegeDamage(wave));
                    feedback.add(0,0,"OUTPOST -"+Math.round(before-progress.baseHealth()),Menu.RED); audio.play("hurt");
                }
            }
            if (enemy.stun<=0 && enemy.attackTime<=0 && enemy.cooldown<=0 && enemy.distance(huntsPlayer?px:0,huntsPlayer?pz:0)<enemy.attackRange())
                enemy.beginAttack(huntsPlayer?px:0,huntsPlayer?pz:0,huntsPlayer);
        }
        for (Iterator<HeroShot> shots=heroShots.iterator();shots.hasNext();) {
            HeroShot shot=shots.next(); shot.update(dt);
            boolean stopped=false;
            // Nearest intersections first, regardless of enemy spawn order.
            var targets=new ArrayList<>(enemies);
            targets.sort(java.util.Comparator.comparingDouble(e -> Math.hypot(e.x-shot.previousX,e.z-shot.previousZ)));
            for (Enemy enemy:targets) if (shot.hit(enemy)) {
                float damage=Math.min(enemy.health,shot.damage);
                enemy.hit(damage,shot.previousX,shot.previousZ,.25f);
                feedback.add(enemy.x,enemy.z,"-"+Math.round(damage),Menu.GOLD); audio.play("hit");
                if (!shot.magic) { stopped=true;break; }
            }
            if (stopped || shot.remaining<=0) shots.remove();
        }
        separateEnemies(dt);
        for (Iterator<Projectile> shots=projectiles.iterator();shots.hasNext();) {
            Projectile shot=shots.next(); shot.update(dt);
            boolean hit=shot.playerTarget?shot.hits(px,pz,.65f):shot.hits(0,0,1.8f);
            if (hit) {
                float before=shot.playerTarget?progress.health():progress.baseHealth();
                if (shot.playerTarget) { if(!player.isDodging()) { progress.damagePlayer(shot.damage); hurtFlash=.25f; } }
                else progress.damageBase(shot.damage);
                float lost=before-(shot.playerTarget?progress.health():progress.baseHealth());
                feedback.add(shot.playerTarget?px:0,shot.playerTarget?pz:0,(shot.playerTarget?"-":"OUTPOST -")+Math.round(lost),Menu.RED);
                audio.play("hurt");
            }
            if (hit || shot.life<=0) shots.remove();
        }
        if (progress.defeated()) { state = GameState.GAME_OVER; return; }
        WaveDirector.Spawn spawn=director.update(dt,enemies.size());
        if (director.wave()!=wave) { wave=director.wave(); audio.play("wave"); notifyPlayer(chapter().bossOnly()?chapter().title()+" / BOSS DUEL":"WAVE "+(wave-chapter().first()+1)+" OF "+chapter().waves()+" / "+chapter().title()); }
        if (spawn!=null) enemies.add(new Enemy(spawn.x(),spawn.z(),spawn.wave(),spawn.variant(),random.nextFloat()));
        nextWave=director.rest();
        if(director.levelCleared(enemies.size())) {
            heroShots.clear();projectiles.clear();player.resetCombat();impactPending=false;attackCooldown=0;
            state=wave>=10?GameState.VICTORY:GameState.LEVEL_COMPLETE;
            saveCurrent();
        }
    }
    private void continueCampaign() {
        if(state==GameState.VICTORY) { state=GameState.MAIN_MENU;return; }
        director.advanceLevel(); storyTime=12; inventory=workshop=false;
        player.getPosition().x(0).y(0).z(4);player.resetCombat();
        state=GameState.PLAYING;saveCurrent();
    }
    private void resolvePlayerHit(float px,float pz) {
        slashTime=.16f;
        if (progress.heroClass()!=HeroClass.WARRIOR) {
            heroShots.add(new HeroShot(px,pz,player.getRotation().y(),activeAttack,progress.damage(),progress.heroClass()==HeroClass.MAGE));
            return;
        }
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
            if(storyTime>0 && state==GameState.PLAYING && !inventory && !workshop) {
                Menu.panel(24,170,270,132);
                Menu.text("CHAPTER "+(chapterIndex()+1)+" / "+chapter().title(),38,180,14,Menu.GOLD);
                Menu.wrapped(chapter().story(),38,208,240,16,Menu.TEXT);
            }
            if((state==GameState.LEVEL_COMPLETE || state==GameState.VICTORY) && !inventory) drawChapterEnd();
            if(state==GameState.JOURNAL) drawJournal();
            if (workshop) drawWorkshop();
            if (inventory) drawInventory();
            if (state == GameState.PAUSED || state == GameState.GAME_OVER) drawOverlay();
        } finally {
            EndMode2D();
        }
    }
    private Vector3 v(float x, float y, float z) { return new Vector3().x(x).y(y).z(z); }
    private void drawWorld() {
        DrawRectangleGradientV(0,0,GetRenderWidth(),GetRenderHeight(),Menu.color(91,150,182),Menu.color(213,226,212));
        BeginMode3D(sceneManager.getOpenScene().getCameraController().getCamera());
        assets.drawEnvironment(chapterIndex(),(float)GetTime());
        if (director.warning()) {
            DrawCylinderWires(v(director.x(),.08f,director.z()),1.4f,1.4f,.15f,24,Menu.RED);
            DrawLine3D(v(director.x(),0,director.z()),v(director.x(),3,director.z()),Menu.GOLD);
        }
        DrawModelEx(player.getModel(), player.getPosition(), v(0, 1, 0), player.getRotation().y(), v(1, 1, 1), hurtFlash>0?Menu.RED:WHITE);
        assets.drawPlayerWeapon(player,progress.heroClass().ordinal(),progress.weaponTier());
        for (HeroShot shot:heroShots) {
            if (shot.magic) DrawSphere(v(shot.x,1,shot.z),.2f,Menu.TEAL);
            else DrawLine3D(v(shot.x,1,shot.z),v(shot.x-shot.vx*.04f,1,shot.z-shot.vz*.04f),Menu.GOLD);
        }
        if (player.isAttacking()) drawArc(player.getPosition().x(),player.getPosition().z(),player.getRotation().y(),activeAttack.range(),activeAttack.halfAngle(),slashTime>0?Menu.GOLD:Menu.TEAL);
        for (Enemy enemy:enemies) if (enemy.attackTime>.25f)
            drawArc(enemy.x,enemy.z,enemy.facing,enemy.ranged()?8:enemy.meleeRange(),enemy.ranged()?8:65,enemy.ranged()?Menu.GOLD:Menu.RED);
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
            Menu.bar((int)screen.x()-22, (int)screen.y(), 44, enemy.health, enemy.maxHealth, enemy.boss()||enemy.variant==2?Menu.GOLD:Menu.RED);
            String label=enemy.attackTime>.25f?"! ATTACK":enemy.boss()?(wave==10?"HOLLOW KING":wave==5?"ASH WARDEN":"WARLORD"):enemy.label();
            Menu.text(label,(int)screen.x()-Menu.measure(label,10)/2,(int)screen.y()-14,10,enemy.attackTime>.25f?Menu.RED:Menu.TEXT);
        }
    }
    private void drawHud() {
        int w=GetScreenWidth(),h=GetScreenHeight();
        // A quiet header leaves the centre of the world unobstructed.
        Menu.panel(24,20,264,104);
        Menu.text(progress.heroClass().label.toUpperCase()+"  /  "+player.getName(),40,29,20,Menu.TEXT);
        Menu.text("LEVEL "+progress.characterLevel()+"     ARMOR "+progress.armorLevel(),40,55,13,Menu.MUTED);
        Menu.bar(40,80,232,progress.health(),progress.maxHealth(),Menu.TEAL);
        Menu.text((int)progress.health()+" / "+progress.maxHealth()+" HP",40,94,15,Menu.TEXT);
        int top=w/2-145;
        Menu.panel(top,20,290,82);
        Menu.center("LEVEL "+(chapterIndex()+1)+" / "+chapter().title(),w/2,29,17,Menu.GOLD);
        int left=chapter().waves()-chapter().cleared(wave,director.remaining(),enemies.size(),director.total());
        Menu.center(chapter().bossOnly()?"BOSS ONLY  /  "+enemies.size()+" HOSTILES":"WAVE "+Math.max(0,wave-chapter().first()+1)+" / "+chapter().waves()+"   -   "+left+" LEFT",w/2,54,17,Menu.TEXT);
        Menu.center(director.remaining()>0?director.remaining()+" reinforcements approaching":enemies.isEmpty()?"Next raid in "+(int)Math.ceil(nextWave)+"s":"Protect the sanctuary",w/2,77,13,Menu.MUTED);
        Menu.panel(w-200,20,176,62);
        Menu.text("CRYSTALS",w-182,27,12,Menu.MUTED);
        Menu.text(String.valueOf(progress.crystals()),w-182,43,29,Menu.GOLD);
        drawMinimap(w-184,102);
        Menu.panel(24,h-106,242,82);
        Menu.text("SANCTUARY",40,h-98,18,Menu.GOLD);
        Menu.text((int)progress.baseHealth()+" / "+progress.maxBaseHealth()+" integrity",40,h-72,15,Menu.TEXT);
        Menu.bar(40,h-43,210,progress.baseHealth(),progress.maxBaseHealth(),Menu.GOLD);
        int bx=w/2-200;
        Menu.panel(bx,h-116,400,92);
        Menu.text("COMBAT",bx+16,h-108,12,Menu.MUTED);
        String[] keys={settings.controls.label(SLASH),settings.controls.label(HEAVY),settings.controls.label(POTION),settings.controls.label(INVENTORY)};
        String[] actions={"ATTACK","HEAVY","HEAL "+progress.potions(),"GEAR"};
        for(int i=0;i<4;i++) {
            int x=bx+12+i*96;
            DrawRectangleRounded(new Rectangle().x(x).y(h-86).width(88).height(48),.18f,6,Menu.color(43,58,64));
            Menu.icon(i==0?"sword":i==1?"crown":i==2?"heart":"bag",x+8,h-80,15,Menu.TEAL);
            Menu.center(keys[i],x+52,h-83,16,Menu.TEXT);
            Menu.center(actions[i],x+44,h-60,12,i==2?Menu.TEAL:Menu.MUTED);
        }
        Menu.bar(bx+12,h-32,376,activeAttack.duration()-attackCooldown,activeAttack.duration(),Menu.TEAL);
        Menu.text(settings.controls.label(WORKSHOP)+"  WORKSHOP    "+settings.controls.label(SAVE)+"  SAVE    ESC  PAUSE",w-265,h-57,13,Menu.TEXT);
        Menu.text(settings.controls.label(SPRINT)+" SPRINT / "+settings.controls.label(DODGE)+" DODGE"+(player.dodgeCooldown()>0?" "+String.format("%.1fs",player.dodgeCooldown()):" READY"),w-265,h-34,12,Menu.MUTED);
        if(nearBase()) Menu.text("+ HEALING HAVEN / +"+(3+progress.baseLevel())+" HP/s",40,134,14,Menu.TEAL);
        for(Enemy enemy:enemies) if(enemy.boss() && enemy.health>0) {
            Menu.panel(w/2-170,114,340,55);
            Menu.center((wave==10?"HOLLOW KING":wave==5?"ASH WARDEN":"WARLORD")+"  /  "+(int)enemy.health+" HP",w/2,121,18,Menu.GOLD);
            Menu.bar(w/2-154,151,308,enemy.health,enemy.maxHealth,Menu.RED);
        }
        if(noticeTime>0) {
            int nw=Math.min(w-80,Menu.measure(notice,18)+40);
            Menu.panel((w-nw)/2,184,nw,38);
            Menu.center(notice,w/2,192,18,Menu.TEXT);
        }
    }
    private void drawMinimap(int x,int y) {
        Menu.panel(x,y,160,160);
        Menu.text("RADAR",x+12,y+10,12,Menu.MUTED);
        int cx=x+80,cy=y+90;
        DrawCircleLines(cx,cy,59,Menu.LINE);
        DrawLine(cx-59,cy,cx+59,cy,Menu.LINE); DrawLine(cx,cy-59,cx,cy+59,Menu.LINE);
        DrawCircle(cx,cy,5,Menu.TEAL);
        for (Enemy e:enemies) DrawCircle(cx+(int)(e.x*2.5f),cy+(int)(e.z*2.5f),3,e.boss()||e.variant==2?Menu.GOLD:Menu.RED);
        if (director.warning()) DrawCircleLines(cx+(int)(director.x()*2.5f),cy+(int)(director.z()*2.5f),6,Menu.GOLD);
        DrawCircle(cx+(int)(player.getPosition().x()*2.5f),cy+(int)(player.getPosition().z()*2.5f),4,Menu.TEXT);
    }
    private void dim() { DrawRectangle(0, 0, GetScreenWidth(), GetScreenHeight(), Fade(Menu.INK, .86f)); }
    private void drawMainMenu() {
        dim(); int x = (GetScreenWidth()-640)/2, y = (GetScreenHeight()-420)/2;
        Menu.text("THE LAST EMBER", x, y, 18, Menu.TEAL);
        Menu.text("OUTPOST", x, y+42, 72, Menu.TEXT);
        Menu.text("Guard the light. Bring back the dawn.", x, y+132, 28, Menu.GOLD);
        Menu.text("Four chapters. Two sieges. Two final duels.", x, y+193, 20, Menu.MUTED);
        Menu.text("Welcome, "+settings.playerName+". Your next defense awaits.", x, y+224, 20, Menu.MUTED);
        if (Menu.button("NEW GAME [ENTER]", x, y+285, 280, true)) requestNewGame();
        if (Menu.button("LOAD GAME",x+300,y+285,160,Files.exists(savePath))) loadCurrent();
        if (Menu.button("QUIT", x+480, y+285, 140, true)) quitRequested = true;
        if (Menu.button("SETTINGS",x,y+344,200,true)) openSettings();
        if(Menu.button("CHRONICLE / THE LAST EMBER",x+220,y+344,400,true)) { journalReturn=state;state=GameState.JOURNAL; }
        Menu.text(menuMessage.isEmpty()?"Hold right mouse to orbit. ESC pauses.":menuMessage, x, y+401, 15, Menu.MUTED);
    }
    private void drawInventory() {
        dim();int x=(GetScreenWidth()-620)/2,y=(GetScreenHeight()-520)/2;
        Menu.text("INVENTORY / "+progress.heroClass().label,x,y,30,Menu.TEXT);
        Menu.text("Equipped: "+progress.weaponName(progress.weaponTier()),x,y+48,20,Menu.TEAL);
        Menu.text("Damage "+progress.damage()+" / Armor "+progress.defense()+" / Crystals "+progress.crystals(),x,y+82,18,Menu.GOLD);
        Menu.text("Defeat Warlords every 5 waves to unlock weapons.",x,y+118,17,Menu.MUTED);
        for (int tier=0;tier<=3;tier++) {
            int left=x+(tier%2)*315,top=y+155+(tier/2)*86;
            boolean unlocked=tier<=progress.weaponUnlocked();
            String label=unlocked?progress.weaponName(tier):"LOCKED / TIER "+tier;
            if (Menu.button((tier==progress.weaponTier()?"> ":"")+label,left,top,300,unlocked)) progress.equipWeapon(tier);
            Menu.text("+"+progress.weaponBonus(tier)+" damage"+(unlocked?"":" / defeat another Warlord"),left+4,top+51,14,Menu.MUTED);
        }
        Menu.text("Potions: "+progress.potions()+" / 5 / Restore up to 60 HP",x,y+340,18,Menu.TEXT);
        if (Menu.button("USE POTION",x,y+378,270,progress.potions()>0 && progress.health()<progress.maxHealth())) progress.usePotion();
        Menu.text("Workshop: 30 crystals per potion",x,y+440,17,Menu.MUTED);
        if (Menu.button("RETURN ["+settings.controls.label(INVENTORY)+" / ESC]",x,y+477,350,true)) inventory=false;
    }
    private void drawWorkshop() {
        dim(); int width = Math.min(900, GetScreenWidth()-60), x = (GetScreenWidth()-width)/2, y = (GetScreenHeight()-430)/2;
        Menu.text("THE WORKSHOP", x, y, 34, Menu.TEXT);
        Menu.text(progress.crystals() + " crystals  /  Time paused while upgrading", x, y+48, 18, Menu.GOLD);
        int card = (width-24)/3;
        String[] names = {"OUTPOST", "ARMOR", "CHARACTER"};
        String[] descriptions = {"+100 base HP; full repair", "+2 damage reduction", "+10 damage; +25 max HP"};
        for (Progression.Upgrade type : Progression.Upgrade.values()) {
            int i = type.ordinal(), left = x+i*(card+12);
            Menu.panel(left, y+94, card, 196);
            Menu.icon(i==0?"shield":i==1?"shield":"sword",left+16,y+116,20,Menu.GOLD);
            Menu.text(names[i], left+48, y+112, 22, Menu.TEXT);
            Menu.text("LEVEL " + progress.level(type) + " > " + (progress.level(type)+1), left+16, y+147, 16, Menu.TEAL);
            Menu.text(descriptions[i], left+16, y+181, 14, Menu.MUTED);
            if (Menu.button("UPGRADE / " + progress.cost(type), left+16, y+225, card-32, progress.crystals() >= progress.cost(type)) && progress.purchase(type)) audio.play("upgrade");
        }
        if (Menu.button("POTION / 30 crystals ("+progress.potions()+" / 5)",x,y+308,400,progress.crystals()>=30 && progress.potions()<5) && progress.buyPotion()) audio.play("upgrade");
        if (Menu.button("RETURN ["+settings.controls.label(WORKSHOP)+" / ESC]", x, y+372, 350, true)) workshop = false;
    }
    private void drawOverlay() {
        dim(); int x=(GetScreenWidth()-440)/2,y=(GetScreenHeight()-440)/2;
        boolean lost=state==GameState.GAME_OVER;
        Menu.text(lost?"OUTPOST FALLEN":"DEFENSE PAUSED",x,y,36,Menu.TEXT);
        Menu.text(lost?"Wave "+wave+" / "+progress.kills()+" defeated":player.getName()+" / Wave "+wave,x,y+54,20,Menu.MUTED);
        if (Menu.button(lost?"TRY AGAIN":"RESUME",x,y+100,440,true)) { if (lost) start(); else state=GameState.PLAYING; }
        if (Menu.button("SAVE GAME",x,y+154,212,!lost)) saveCurrent();
        if (Menu.button("LOAD GAME",x+228,y+154,212,Files.exists(savePath))) loadCurrent();
        if (Menu.button("SETTINGS",x,y+208,212,true)) openSettings();
        if (Menu.button("CHRONICLE",x+228,y+208,212,true)) { journalReturn=state;state=GameState.JOURNAL; }
        if (Menu.button("SAVE & MAIN MENU",x,y+262,440,!lost)) { if (saveCurrent()) state=GameState.MAIN_MENU; }
        if (lost && Menu.button("MAIN MENU",x,y+262,440,true)) state=GameState.MAIN_MENU;
        Menu.text(menuMessage,x,y+325,15,Menu.GOLD);
        Menu.text("Closing the game also saves an active defense.",x,y+356,15,Menu.MUTED);
    }
    private void drawChapterEnd() {
        dim();int x=(GetScreenWidth()-660)/2,y=(GetScreenHeight()-480)/2;
        Menu.panel(x-24,y-24,708,510);
        Menu.icon("crown",x,y,32,Menu.GOLD);
        Menu.text(state==GameState.VICTORY?"THE VALLEY SEES DAWN":"CHAPTER COMPLETE",x+48,y,30,Menu.GOLD);
        Menu.text(chapter().title(),x,y+60,25,Menu.TEXT);
        Menu.wrapped(chapter().aftermath(),x,y+107,640,20,Menu.TEXT);
        if(state!=GameState.VICTORY) {
            Campaign.Chapter next=Campaign.CHAPTERS[Math.min(3,chapterIndex()+1)];
            Menu.text("NEXT / "+next.title()+(next.bossOnly()?" / BOSS DUEL":" / "+next.waves()+" WAVES"),x,y+205,21,Menu.TEAL);
            Menu.wrapped(next.story(),x,y+244,640,18,Menu.MUTED);
        } else Menu.text(progress.kills()+" enemies defeated / The sanctuary endures.",x,y+230,19,Menu.TEAL);
        if(Menu.button(state==GameState.VICTORY?"MAIN MENU [ENTER]":"CONTINUE [ENTER]",x,y+350,380,true)) continueCampaign();
        if(state!=GameState.VICTORY && Menu.button("INVENTORY",x+400,y+350,240,true)) inventory=true;
        Menu.text("Your campaign progress has been saved.",x,y+424,16,Menu.MUTED);
        if(state!=GameState.VICTORY && Menu.button("MAIN MENU [ESC]",x+440,y+416,200,true)) { saveCurrent();state=GameState.MAIN_MENU; }
    }
    private void drawJournal() {
        dim();int x=(GetScreenWidth()-760)/2,y=(GetScreenHeight()-610)/2;
        Menu.icon("book",x,y,30,Menu.GOLD);Menu.text("CHRONICLE / THE LAST EMBER",x+46,y,30,Menu.TEXT);
        for(int i=0;i<Campaign.CHAPTERS.length;i++) {
            Campaign.Chapter c=Campaign.CHAPTERS[i];int top=y+66+i*112;
            Menu.panel(x,top,760,100);
            Menu.text((i+1)+" / "+c.title(),x+16,top+9,21,Menu.GOLD);
            Menu.text(c.bossOnly()?"BOSS DUEL":c.waves()+" DEFENSE WAVES",x+530,top+12,15,Menu.TEAL);
            Menu.wrapped(c.story(),x+16,top+43,718,16,Menu.TEXT);
        }
        if(Menu.button("RETURN [ENTER / ESC]",x,y+540,360,true)) state=journalReturn;
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
        dim(); int x=(GetScreenWidth()-560)/2,y=(GetScreenHeight()-450)/2;
        Menu.text("NAME YOUR WARDEN",x,y,34,Menu.TEXT);
        Menu.text("Letters, numbers, spaces, - or _ (up to 16)",x,y+55,18,Menu.MUTED);
        Menu.panel(x,y+100,560,60);
        Menu.text(nameDraft.isEmpty()?"Type a name...":nameDraft+"_",x+16,y+118,24,nameDraft.isEmpty()?Menu.MUTED:Menu.TEXT);
        for (HeroClass hero:HeroClass.values()) {
            if (Menu.button((selectedClass==hero?"> ":"")+hero.label,x+hero.ordinal()*190,y+185,180,true)) selectedClass=hero;
        }
        Menu.text(selectedClass.description,x,y+248,18,Menu.TEAL);
        if (Menu.button("BEGIN [ENTER]",x,y+300,270,true)) confirmName();
        if (Menu.button("CLEAR",x+290,y+300,130,true)) nameDraft="";
        if (Menu.button("BACK",x+440,y+300,120,true)) state=GameState.MAIN_MENU;
        Menu.text("THE LAST EMBER / A FOUR-CHAPTER CAMPAIGN",x,y+365,15,Menu.GOLD);
        Menu.wrapped("An ember beneath the sanctuary is the valley's last warmth. Stand against the cold. Wake the dawn.",x,y+393,540,17,Menu.MUTED);
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
        dim(); int x=(GetScreenWidth()-820)/2,y=(GetScreenHeight()-650)/2;
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
            if (Menu.button(action.label+" / "+(rebinding==action?"PRESS KEY":settings.controls.label(action)),x+(i%2)*420,y+210+(i/2)*46,400,rebinding==null)) {
                rebinding=action;menuMessage="Press a key for "+action.label+" (ESC cancels)."; while(GetKeyPressed()!=0) { }
            }
        }
        Menu.text(menuMessage,x,y+542,15,Menu.GOLD);
        if (Menu.button("BACK / SAVE",x,y+580,260,rebinding==null)) closeSettings();
        if (Menu.button("RESTORE DEFAULTS",x+280,y+580,280,rebinding==null)) {
            settings.controls.reset();settings.volume=.6f;settings.sensitivity=.15f;settings.muted=false;settings.fullscreen=false;applySettings();menuMessage="Defaults restored.";
        }
    }
    boolean saveCurrent() {
        if (progress.defeated()) return false;
        try {
            SaveGame.write(savePath,new SaveGame.Snapshot(player.getName(),player.getPosition().x(),player.getPosition().z(),player.getRotation().y(),progress,director.snapshot(),enemies.stream().filter(e->e.health>0).toList(),Arena.HIGHLANDS));
            menuMessage="Game saved."; notifyPlayer(menuMessage); return true;
        } catch (IOException e) { menuMessage="Save failed. Previous save was kept.";notifyPlayer(menuMessage);System.err.println(e.getMessage());return false; }
    }
    boolean loadCurrent() {
        try {
            SaveGame.Snapshot saved=SaveGame.read(savePath);
            progress=saved.progression();director.restore(saved.wave());enemies.clear();enemies.addAll(saved.enemies());projectiles.clear();heroShots.clear();inventory=false;
            selectedClass=progress.heroClass();applyHeroModel();
            player.setName(saved.name());settings.playerName=saved.name();player.getPosition().x(saved.x()).z(saved.z());player.getRotation().y(saved.facing());
            player.resetCombat();feedback.clear();wave=director.wave();nextWave=director.rest();workshop=false;
            combo=0;comboWindow=inputBuffer=attackElapsed=hurtFlash=slashTime=noticeTime=0;attackCooldown=.3f;impactPending=false;
            sceneManager.getOpenScene().getCameraController().update(0);
            state=director.levelCleared(enemies.size())?(wave>=10?GameState.VICTORY:GameState.LEVEL_COMPLETE):GameState.PAUSED;menuMessage="Game loaded. Press Enter to resume.";return true;
        } catch (IOException e) { menuMessage="Could not load game. Current defense was kept.";notifyPlayer(menuMessage);System.err.println(e.getMessage());return false; }
    }
    java.util.List<Enemy> getEnemies() { return java.util.Collections.unmodifiableList(enemies); }
    Progression getProgression() { return progress; }
    boolean isWorkshopOpen() { return workshop; }
    public boolean shouldQuit() { return quitRequested; }
    public void dispose() { persistSettings(); if (state==GameState.PLAYING||state==GameState.PAUSED||state==GameState.LEVEL_COMPLETE||state==GameState.VICTORY||state==GameState.JOURNAL&&journalReturn==GameState.PAUSED||state==GameState.SETTING_MENU&&settingsReturn==GameState.PAUSED) saveCurrent(); audio.close(); assets.close(); player.dispose(); Menu.close(); }
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
