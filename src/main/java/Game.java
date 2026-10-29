import Config.Controls;
import static Config.Controls.Action.*;
import java.nio.file.*;
import java.io.IOException;
import assets.AssetPaths;
import Entities.Player;
import Entities.Enemy;
import Entities.BossMove;
import Entities.CombatDiscipline;
import Entities.Difficulty;
import Entities.SquadTactics;
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
    private Difficulty selectedDifficulty=Difficulty.NORMAL;
    private float lastHeroX,lastHeroZ;
    private final WorkshopScreen workshopScreen=new WorkshopScreen();
    private final EquipmentScreen equipmentScreen=new EquipmentScreen();
    private HeroClass selectedClass=HeroClass.WARRIOR;
    private boolean inventory;
    private final ArrayList<HeroShot> heroShots=new ArrayList<>();
    private final WorldAssets assets = new WorldAssets();
    private final ModernRenderer renderer=new ModernRenderer();
    private final ArrayList<BossHazard> bossHazards=new ArrayList<>();
    private GameState state = GameState.MAIN_MENU;
    private SceneManager sceneManager = new SceneManager();
    private Progression progress = new Progression();
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final Random random = new Random();
    private final WaveDirector director = new WaveDirector(random);
    private final GameAudio audio = new GameAudio();
    private final CombatFeedback feedback = new CombatFeedback();
    private final CombatVfx vfx=new CombatVfx();
    private int wave, combo;
    private float comboWindow, inputBuffer, attackElapsed, hurtFlash;
    private boolean bufferedHeavy,bufferedLunge,bufferedTimed,impactPending;
    private float heavyCharge,releasedCharge;
    private Combat.Attack activeAttack = Combat.attack(1,false);
    private float nextWave = 5, attackCooldown, slashTime, noticeTime;
    private String notice = "";
    private boolean workshop;
    private boolean quitRequested;
    private final Camera2D uiCamera = new Camera2D().zoom(1);

    public Game() { this(()->IsMouseButtonPressed(MOUSE_BUTTON_LEFT)); }
    Game(java.util.function.BooleanSupplier mouseAttack) {
        this.mouseAttack=mouseAttack;
        settings.load(settingsPath);selectedDifficulty=settings.difficulty; menuMessage=settings.warning;
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
        progress = new Progression(selectedClass,selectedDifficulty); player.setMovementSpeed(selectedClass.speed); inventory=false; heroShots.clear(); enemies.clear(); projectiles.clear(); wave = 0; nextWave = 5;
        attackCooldown = slashTime = noticeTime = heavyCharge = 0; bossHazards.clear(); workshop = false;
        player.getPosition().x(0).y(0).z(4);
        player.resetCombat();
        director.reset();director.difficulty(progress.difficulty());applyProgression();lastHeroX=0;lastHeroZ=4; feedback.clear();vfx.clear(); combo=0;
        comboWindow=inputBuffer=attackElapsed=hurtFlash=0; impactPending=false;
        player.setName(settings.playerName); menuMessage="";
        state = GameState.PLAYING;
    }
    private void applyProgression() { player.setCrossbow(progress.heroClass()==HeroClass.ARCHER && progress.weaponTier()>=2);player.setMovementSpeed(progress.movementSpeed());player.discipline().configure(progress.maxStamina(),progress.staminaRecovery()); }
    private boolean nearBase() { return Math.hypot(player.getPosition().x(), player.getPosition().z()) < 6; }
    private void notifyPlayer(String message) { notice = message; noticeTime = 3; }

    public void update(float dt) {
        dt = Math.max(0, Math.min(dt, .05f));applyProgression();
        boolean titleAudio=state==GameState.MAIN_MENU || state==GameState.LOGIN || state==GameState.JOURNAL && journalReturn==GameState.MAIN_MENU || state==GameState.SETTING_MENU && settingsReturn==GameState.MAIN_MENU;
        audio.update(dt,chapterIndex(),titleAudio,state==GameState.PLAYING && !inventory && !workshop && !enemies.isEmpty());
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
        feedback.update(dt);vfx.update(dt);
        boolean wasDodging=player.isDodging();
        sceneManager.getOpenScene().updatesEntities(dt);
        if(!wasDodging && player.isDodging())audio.play("dash");
        float px = player.getPosition().x(), pz = player.getPosition().z();
        float heroVx=Math.max(-15,Math.min(15,(px-lastHeroX)/Math.max(.001f,dt))),heroVz=Math.max(-15,Math.min(15,(pz-lastHeroZ)/Math.max(.001f,dt)));
        lastHeroX=px;lastHeroZ=pz;
        if (nearBase()) progress.heal(dt);
        // Impact occurs part-way through the swing, once only, in the locked facing arc.
        if (impactPending) {
            attackElapsed+=dt;
            if (attackElapsed>=activeAttack.impact()) { resolvePlayerHit(px,pz); impactPending=false; }
        }
        if(player.isDodging()) { inputBuffer=0;comboWindow=0;combo=0;heavyCharge=0;player.setCharging(false); }
        if(settings.controls.down(HEAVY) && attackCooldown<=0 && !player.isDodging() && player.discipline().recovery()<=0) {
            heavyCharge=Math.min(1.4f,heavyCharge+dt);player.setCharging(true);
        } else if(heavyCharge>0 && !settings.controls.down(HEAVY)) {
            releasedCharge=heavyCharge;heavyCharge=0;player.setCharging(false);
            bufferedHeavy=true;bufferedLunge=false;inputBuffer=.14f;
        }
        boolean lightInput=settings.controls.pressed(SLASH);lightInput|=mouseAttack.getAsBoolean();
        if(lightInput && heavyCharge<=0) {
            bufferedHeavy=bufferedLunge=false;
            bufferedTimed=combo>0 && comboWindow>0 && attackCooldown<=.14f;
            if(attackCooldown>.14f) { combo=0;comboWindow=0; }
            inputBuffer=.14f;
        }
        if(settings.controls.pressed(LUNGE) && heavyCharge<=0) {
            bufferedLunge=true;bufferedHeavy=false;inputBuffer=.14f;
        }
        if(inputBuffer>0 && attackCooldown<=0 && !player.isDodging() && heavyCharge<=0 && player.discipline().recovery()<=0) {
            float cost=bufferedLunge?CombatDiscipline.LUNGE:bufferedHeavy?CombatDiscipline.HEAVY:CombatDiscipline.LIGHT;
            inputBuffer=0;
            if(!player.discipline().spend(cost)) notifyPlayer("LOW STAMINA / Breathe before attacking");
            else {
                combo=bufferedLunge||bufferedHeavy?0:bufferedTimed?combo%3+1:1;
                activeAttack=bufferedLunge?Combat.lunge(progress.heroClass()):progress.heroClass().attack(combo,bufferedHeavy);
                if(bufferedHeavy) {
                    boolean perfect=releasedCharge>=.75f && releasedCharge<=1.05f;
                    float bonus=perfect?1.6f:releasedCharge>1.2f?1.05f:1+releasedCharge*.25f;
                    activeAttack=Combat.scale(activeAttack,bonus,perfect?"PERFECT CHARGE":activeAttack.label());
                } else if(combo>1) activeAttack=Combat.scale(activeAttack,1.12f,"TIMED CHAIN "+combo+" / 3");
                float riposte=player.discipline().consumeRiposte();
                if(riposte>1) activeAttack=Combat.scale(activeAttack,riposte,"RIPOSTE");
                attackCooldown=activeAttack.duration();comboWindow=attackCooldown+.23f;
                attackElapsed=0;impactPending=true;
                if(bufferedLunge) { player.lunge(activeAttack.duration());audio.play("dash"); }
                else player.attack(combo,bufferedHeavy,activeAttack.duration());
                audio.play(progress.heroClass()==HeroClass.ARCHER?"bow":progress.heroClass()==HeroClass.MAGE?"magic":"swing");
            }
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
            boolean huntsPlayer=enemy.chooseHero(px,pz,dt);
            float tx=huntsPlayer?px:0,tz=huntsPlayer?pz:0,separationX=0,separationZ=0;
            for(Enemy other:enemies) if(other!=enemy && other.health>0) {
                float dx=enemy.x-other.x,dz=enemy.z-other.z,d=(float)Math.hypot(dx,dz);
                if(d>.01f && d<2.5f) { float force=(2.5f-d)/2.5f;separationX+=dx/d*force;separationZ+=dz/d*force; }
            }
            float ex=enemy.x,ez=enemy.z;boolean charging=enemy.chargeTime>0;
            enemy.navigate(tx,tz,separationX,separationZ,dt);
            if(enemy.variant==4 && enemy.healCooldown<=0 && enemy.stun<=0 && enemy.attackTime<=0) {
                Enemy ally=SquadTactics.woundedAlly(enemy,enemies);
                if(ally!=null) { enemy.healingTarget=ally;enemy.attackTime=.9f;enemy.impactPending=true;enemy.healCooldown=8;enemy.cooldown=1.5f;audio.play("ward"); }
            }
            if((charging || enemy.chargeTime>0) && !enemy.chargeHit && Combat.segmentDistance(px,pz,ex,ez,enemy.x,enemy.z)<1.45f) {
                enemy.chargeHit=true;strikeHero(enemy.meleeDamage(wave)*1.15f,ex,ez,true,enemy);
            }
            if (enemy.impactPending && enemy.attackTime<=.25f) {
                enemy.impactPending=false;
                if(enemy.healingTarget!=null) {
                    Enemy ally=enemy.healingTarget;enemy.healingTarget=null;
                    if(ally.health>0 && ally.distance(enemy.x,enemy.z)<7) { ally.health=Math.min(ally.maxHealth,ally.health+ally.maxHealth*.18f);feedback.add(ally.x,ally.z,"WARD +",Menu.TEAL); }
                } else if(enemy.boss()) {
                    switch(enemy.bossMove) {
                        case CHARGE -> enemy.launchCharge();
                        case SLAM -> {
                            audio.play("slam");vfx.impact(enemy.attackX,enemy.attackZ,Menu.GOLD,true);
                            if(Math.hypot(px-enemy.attackX,pz-enemy.attackZ)<=3.4f) strikeHero(enemy.meleeDamage(wave)*1.3f,enemy.x,enemy.z,false,enemy);
                            bossHazards.add(new BossHazard(BossHazard.Kind.SHOCKWAVE,enemy.x,enemy.z,.25f,enemy.meleeDamage(wave)*.7f));
                        }
                        case SIGILS -> { }
                        case CLEAVE -> {
                            if(Combat.inArc(enemy.x,enemy.z,enemy.facing,px,pz,enemy.meleeRange(),100)) strikeHero(enemy.meleeDamage(wave),enemy.x,enemy.z,true,enemy);
                        }
                    }
                } else if (enemy.ranged()) { projectiles.add(new Projectile(enemy,wave));if(enemy.distance(px,pz)<15)audio.play(enemy.variant==4?"magic":"bow"); }
                else if (enemy.targetsPlayer && Combat.inArc(enemy.x,enemy.z,enemy.facing,px,pz,enemy.meleeRange(),65)) {
                    strikeHero(enemy.meleeDamage(wave),enemy.x,enemy.z,true,enemy);
                } else if (!enemy.targetsPlayer && enemy.distance(0,0)<enemy.meleeRange()+.1f) {
                    float before=progress.baseHealth(); progress.damageBase(enemy.siegeDamage(wave));
                    feedback.add(0,0,"OUTPOST -"+Math.round(before-progress.baseHealth()),Menu.RED); audio.play("hurt");
                }
            }
            if (enemy.stun<=0 && enemy.attackTime<=0 && enemy.cooldown<=0 && enemy.distance(tx,tz)<enemy.attackRange() && enemy.facingTarget(tx,tz) && (!huntsPlayer || SquadTactics.canJoinAssault(enemy,enemies)))
            {
                if(enemy.ranged() && !SquadTactics.clearShot(enemy,tx,tz,enemies)) { enemy.sideStepTime=.65f;continue; }
                float lead=enemy.ranged() && huntsPlayer?Math.min(.6f,enemy.distance(px,pz)/10)*(.45f+(1-enemy.difficulty.cadence)) : 0;
                enemy.beginAttack(Math.max(-22,Math.min(22,tx+heroVx*lead)),Math.max(-22,Math.min(22,tz+heroVz*lead)),huntsPlayer);
                if(!enemy.ranged() && enemy.distance(px,pz)<10)audio.play("swing");
                if(enemy.boss() && enemy.bossMove==BossMove.SIGILS) {
                    int count=enemy.enraged()?5:3;
                    for(int i=0;i<count;i++) {
                        double a=(i-1)*Math.PI*2/(count-1);
                        float hx=px+(i==0?0:(float)Math.cos(a)*3),hz=pz+(i==0?0:(float)Math.sin(a)*3);
                        bossHazards.add(new BossHazard(BossHazard.Kind.SIGIL,hx,hz,enemy.attackDuration()-.25f+i*.12f,enemy.meleeDamage(wave)*.75f));
                    }
                }
            }
        }
        for (Iterator<HeroShot> shots=heroShots.iterator();shots.hasNext();) {
            HeroShot shot=shots.next(); shot.update(dt);
            boolean stopped=false;
            // Nearest intersections first, regardless of enemy spawn order.
            var targets=new ArrayList<>(enemies);
            targets.sort(java.util.Comparator.comparingDouble(e -> Math.hypot(e.x-shot.previousX,e.z-shot.previousZ)));
            for (Enemy enemy:targets) if (shot.hit(enemy)) {
                float damage=Math.min(enemy.health,shot.damage);
                enemy.hit(damage,shot.previousX,shot.previousZ,.25f);vfx.impact(enemy.x,enemy.z,shot.magic?Menu.TEAL:Menu.GOLD,false);
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
                if(shot.playerTarget) strikeHero(shot.damage,shot.x-shot.vx*.2f,shot.z-shot.vz*.2f,true,null);
                else {
                    float before=progress.baseHealth();progress.damageBase(shot.damage);
                    feedback.add(0,0,"OUTPOST -"+Math.round(before-progress.baseHealth()),Menu.RED);audio.play("hurt");
                }
            }
            if (hit || shot.life<=0) shots.remove();
        }
        for(Iterator<BossHazard> hazards=bossHazards.iterator();hazards.hasNext();) {
            BossHazard hazard=hazards.next();hazard.update(dt);
            if(hazard.hits(px,pz)) strikeHero(hazard.damage,hazard.x,hazard.z,false,null);
            if(hazard.expired()) hazards.remove();
        }
        if (progress.defeated()) { state = GameState.GAME_OVER; return; }
        WaveDirector.Spawn spawn=director.update(dt,enemies.size());
        if (director.wave()!=wave) { wave=director.wave(); audio.play("wave"); notifyPlayer(chapter().bossOnly()?chapter().title()+" / BOSS DUEL":"WAVE "+(wave-chapter().first()+1)+" OF "+chapter().waves()+" / "+chapter().title()); }
        if (spawn!=null) enemies.add(new Enemy(spawn.x(),spawn.z(),spawn.wave(),spawn.variant(),random.nextFloat(),progress.difficulty()));
        nextWave=director.rest();
        if(director.levelCleared(enemies.size())) {
            heroShots.clear();projectiles.clear();bossHazards.clear();heavyCharge=0;player.resetCombat();impactPending=false;attackCooldown=0;
            state=wave>=10?GameState.VICTORY:GameState.LEVEL_COMPLETE;
            saveCurrent();
        }
    }
    private void strikeHero(float damage,float sourceX,float sourceZ,boolean parryable,Enemy attacker) {
        float px=player.getPosition().x(),pz=player.getPosition().z();
        if(player.invulnerable()) { feedback.add(px,pz,"EVADE",Menu.TEAL);return; }
        if(parryable && Combat.inArc(px,pz,player.getRotation().y(),sourceX,sourceZ,1000,75) && player.discipline().deflect()) {
            if(attacker!=null) { attacker.stun=1.1f;attacker.attackTime=attacker.chargeTime=0;attacker.impactPending=false;attacker.cooldown=1.3f; }
            vfx.impact(px,pz,Menu.TEAL,true);feedback.add(px,pz,"PARRY / RIPOSTE",Menu.GOLD);audio.play("parry");return;
        }
        float before=progress.health();progress.damagePlayer(damage);vfx.impact(px,pz,Menu.RED,false);
        feedback.add(px,pz,"-"+Math.round(before-progress.health()),Menu.RED);hurtFlash=.25f;audio.play("hurt");
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
            enemy.hit(damage,px,pz,activeAttack.multiplier()>1?.8f:.35f);vfx.impact(enemy.x,enemy.z,Menu.GOLD,activeAttack.multiplier()>1);
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
        if(inventory) equipmentScreen.prepare(assets,player,progress);
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
                Menu.panel(24,190,270,132);
                Menu.text("CHAPTER "+(chapterIndex()+1)+" / "+chapter().title(),38,200,14,Menu.GOLD);
                Menu.wrapped(chapter().story(),38,228,240,16,Menu.TEXT);
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
        float time=(float)GetTime();Camera3D camera=sceneManager.getOpenScene().getCameraController().getCamera();
        renderer.beginShadow(time);assets.pass(renderer.shadowShader,true);
        assets.drawEnvironment(chapterIndex(),time);drawActors();renderer.endShadow();
        renderer.beginScene();
        DrawRectangleGradientV(0,0,renderer.width(),renderer.height(),ChapterLook.at(chapterIndex()).skyTop(),ChapterLook.at(chapterIndex()).skyBottom());
        if(chapterIndex()==1 || chapterIndex()==3) DrawCircle(renderer.width()*3/4,renderer.height()/12,renderer.height()/35f,Menu.color(185,191,217));
        BeginMode3D(camera);
        Enemy boss=enemies.stream().filter(e->e.boss() && e.health>0).findFirst().orElse(null);
        assets.litPass(renderer,camera,time,boss);assets.drawEnvironment(chapterIndex(),time);drawActors();
        if(boss!=null) {
            float hx=player.getPosition().x(),hz=player.getPosition().z(),cx=camera._position().x()-hx,cz=camera._position().z()-hz;
            float t=((boss.x-hx)*cx+(boss.z-hz)*cz)/Math.max(.001f,cx*cx+cz*cz);
            if(t>0 && t<1 && Combat.segmentDistance(boss.x,boss.z,hx,hz,camera._position().x(),camera._position().z())<1.5f)
                assets.drawHeroSilhouette(player);
        }
        if (director.warning()) {
            DrawCylinderWires(v(director.x(),.08f,director.z()),1.4f,1.4f,.15f,24,Menu.RED);
            DrawLine3D(v(director.x(),0,director.z()),v(director.x(),3,director.z()),Menu.GOLD);
        }
        vfx.begin();
        for(HeroShot shot:heroShots)vfx.projectile(camera,shot.x,shot.z,shot.vx,shot.vz,shot.magic,shot.magic?Menu.TEAL:Menu.GOLD,time);
        if(player.isAttacking())vfx.slash(player.getPosition().x(),player.getPosition().z(),player.getRotation().y(),activeAttack.range(),activeAttack.halfAngle(),1-player.attackRemaining()/activeAttack.duration(),slashTime>0?Menu.GOLD:Menu.TEAL);
        if(player.discipline().parrying())vfx.shield(camera,player.getPosition().x(),player.getPosition().z(),time,Menu.TEAL);
        if(heavyCharge>0)vfx.charge(player.getPosition().x(),player.getPosition().z(),heavyCharge,heavyCharge>=.75f && heavyCharge<=1.05f?Menu.GOLD:Menu.TEAL);
        for(Enemy enemy:enemies)vfx.enemy(camera,enemy,time);
        for(BossHazard hazard:bossHazards)vfx.hazard(camera,hazard,time);
        for(Projectile shot:projectiles)vfx.projectile(camera,shot.x,shot.z,shot.vx,shot.vz,shot.magic,shot.magic?Menu.color(180,124,235):Menu.GOLD,time);
        vfx.particles(camera);vfx.end();
        EndMode3D();assets.finishLitPass();renderer.present();
    }
    private void drawActors() {
        assets.preparePlayer(player);
        assets.drawPlayerBody(player,progress,hurtFlash>0?Menu.RED:WHITE);
        assets.drawPlayerWeapon(player,progress.heroClass().ordinal(),progress.weaponTier());assets.drawArmor(player,progress);
        for(Enemy enemy:enemies) assets.drawEnemy(enemy);
    }
    private void drawEnemyHealth() {
        for (Enemy enemy : enemies) {
            Vector2 screen = GetWorldToScreen(v(enemy.x, enemy.boss()?(enemy.king?6.0f:5.2f):2.1f, enemy.z), sceneManager.getOpenScene().getCameraController().getCamera());
            Menu.bar((int)screen.x()-22, (int)screen.y(), 44, enemy.health, enemy.maxHealth, enemy.boss()||enemy.variant==2?Menu.GOLD:Menu.RED);
            String label=enemy.attackTime>.25f?"! ATTACK":enemy.boss()?(wave==10?"HOLLOW KING":wave==5?"ASH WARDEN":"WARLORD"):enemy.label();
            Menu.text(label,(int)screen.x()-Menu.measure(label,10)/2,(int)screen.y()-14,10,enemy.attackTime>.25f?Menu.RED:Menu.TEXT);
        }
    }
    private void drawHud() {
        int w=GetScreenWidth(),h=GetScreenHeight();
        // A quiet header leaves the centre of the world unobstructed.
        Menu.panel(24,20,264,134);
        Menu.text(progress.heroClass().label.toUpperCase()+"  /  "+player.getName(),40,29,20,Menu.TEXT);
        Menu.text("LEVEL "+progress.characterLevel()+" / "+progress.difficulty().label.toUpperCase()+" / DEF "+progress.defense(),40,55,13,Menu.MUTED);
        Menu.bar(40,80,232,progress.health(),progress.maxHealth(),Menu.TEAL);
        Menu.text((int)progress.health()+" / "+progress.maxHealth()+" HP",40,94,15,Menu.TEXT);
        Menu.text("STAMINA "+Math.round(player.discipline().stamina())+" / "+progress.maxStamina(),40,120,12,Menu.GOLD);
        Menu.bar(158,124,114,player.discipline().stamina(),progress.maxStamina(),player.discipline().stamina()<24?Menu.RED:Menu.GOLD);
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
        String timing=player.discipline().riposteReady()?"RIPOSTE READY / 1.75x":heavyCharge>0?"RELEASE Q AT .75 - 1.05s / "+String.format("%.2fs",heavyCharge):combo>0 && comboWindow>0 && attackCooldown<=.14f?"CHAIN NOW":"TIME YOUR STRIKES";
        Menu.text(timing,bx+16,h-108,12,Menu.GOLD);
        String[] keys={settings.controls.label(SLASH),settings.controls.label(HEAVY),settings.controls.label(POTION),settings.controls.label(INVENTORY)};
        String[] actions={"STRIKE","CHARGE","HEAL "+progress.potions(),"GEAR"};
        for(int i=0;i<4;i++) {
            int x=bx+12+i*96;
            DrawRectangleRounded(new Rectangle().x(x).y(h-86).width(88).height(48),.18f,6,Menu.color(43,58,64));
            Menu.icon(i==0?"sword":i==1?"crown":i==2?"heart":"bag",x+8,h-80,15,Menu.TEAL);
            Menu.center(keys[i],x+52,h-83,16,Menu.TEXT);
            Menu.center(actions[i],x+44,h-60,12,i==2?Menu.TEAL:Menu.MUTED);
        }
        Menu.bar(bx+12,h-32,376,activeAttack.duration()-attackCooldown,activeAttack.duration(),Menu.TEAL);
        Menu.text(settings.controls.label(PARRY)+" PARRY / "+settings.controls.label(LUNGE)+" LUNGE",w-265,h-80,14,Menu.GOLD);
        Menu.text(settings.controls.label(WORKSHOP)+"  WORKSHOP    "+settings.controls.label(SAVE)+"  SAVE    ESC  PAUSE",w-265,h-57,13,Menu.TEXT);
        Menu.text(settings.controls.label(SPRINT)+" SPRINT / "+settings.controls.label(DODGE)+" DODGE"+(player.discipline().stamina()<22?" TIRED":player.dodgeCooldown()>0?" "+String.format("%.1fs",player.dodgeCooldown()):" READY"),w-265,h-34,12,Menu.MUTED);
        if(nearBase()) Menu.text("+ HEALING HAVEN / +"+(3+progress.baseLevel())+" HP/s",40,160,14,Menu.TEAL);
        for(Enemy enemy:enemies) if(enemy.boss() && enemy.health>0) {
            Menu.panel(w/2-170,114,340,72);
            Menu.center((wave==10?"HOLLOW KING":wave==5?"ASH WARDEN":"WARLORD")+"  /  "+(int)enemy.health+" HP",w/2,121,18,Menu.GOLD);
            Menu.bar(w/2-154,151,308,enemy.health,enemy.maxHealth,Menu.RED);
            Menu.center((enemy.enraged()?"ENRAGED / ":"")+enemy.moveLabel()+(enemy.bossMove.parryable?" / PARRY OR EVADE":" / UNBLOCKABLE"),w/2,164,12,Menu.GOLD);
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
    private void dim() { DrawRectangle(0, 0, GetScreenWidth(), GetScreenHeight(), Fade(Menu.INK, .92f)); }
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
    private void drawInventory() { dim();if(equipmentScreen.draw(progress,settings.controls.label(INVENTORY))) inventory=false; }
    private void drawWorkshop() {
        dim();if(workshopScreen.draw(progress,settings.controls.label(WORKSHOP),audio))workshop=false;
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
        settings.playerName=Settings.cleanName(nameDraft);settings.difficulty=selectedDifficulty; persistSettings(); start();
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
        Menu.text("CAMPAIGN DIFFICULTY",x,y+348,14,Menu.GOLD);
        for(Difficulty choice:Difficulty.values()) if(Menu.button((choice==selectedDifficulty?"> ":"")+choice.label,x+choice.ordinal()*142,y+374,134,true)) selectedDifficulty=choice;
        Menu.text(selectedDifficulty.description,x,y+431,16,Menu.MUTED);
    }
    private void openSettings() { settingsReturn=state; state=GameState.SETTING_MENU; menuMessage=""; rebinding=null; }
    private void closeSettings() { persistSettings(); rebinding=null; state=settingsReturn; }
    private void applySettings() {
        audio.configure(settings.volume,settings.muted);
        sceneManager.getOpenScene().getCameraController().setSensitivity(settings.sensitivity);renderer.quality(settings.highQuality);
        if (IsWindowFullscreen()!=settings.fullscreen) ToggleFullscreen();
    }
    private void persistSettings() {
        try { settings.save(settingsPath); }
        catch (IOException e) { menuMessage="Could not save settings. Check folder permissions."; System.err.println(e.getMessage()); }
    }
    private void drawSettings() {
        dim(); int x=(GetScreenWidth()-820)/2,y=(GetScreenHeight()-650)/2;
        Menu.text("SETTINGS",x,y,34,Menu.TEXT);
        Menu.text("Master audio "+Math.round(settings.volume*100)+"%",x,y+68,20,Menu.TEXT);
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
            if (Menu.button(action.label+" / "+(rebinding==action?"PRESS KEY":settings.controls.label(action)),x+(i%3)*280,y+210+(i/3)*46,260,rebinding==null)) {
                rebinding=action;menuMessage="Press a key for "+action.label+" (ESC cancels)."; while(GetKeyPressed()!=0) { }
            }
        }
        if(Menu.button(settings.highQuality?"GRAPHICS / HIGH - 2K SHADOWS + BLOOM":"GRAPHICS / BALANCED - 1K SHADOWS",x,y+492,820,rebinding==null)) {
            settings.highQuality=!settings.highQuality;applySettings();
        }
        Menu.text(menuMessage,x,y+542,15,Menu.GOLD);
        if (Menu.button("BACK / SAVE",x,y+580,260,rebinding==null)) closeSettings();
        if (Menu.button("RESTORE DEFAULTS",x+280,y+580,280,rebinding==null)) {
            settings.controls.reset();settings.volume=.6f;settings.sensitivity=.15f;settings.muted=false;settings.fullscreen=false;settings.highQuality=true;applySettings();menuMessage="Defaults restored.";
        }
    }
    boolean saveCurrent() {
        if (progress.defeated()) return false;
        try {
            SaveGame.write(savePath,new SaveGame.Snapshot(player.getName(),player.getPosition().x(),player.getPosition().z(),player.getRotation().y(),progress,director.snapshot(),enemies.stream().filter(e->e.health>0).toList(),Arena.HIGHLANDS,player.discipline().stamina()));
            menuMessage="Game saved."; notifyPlayer(menuMessage); return true;
        } catch (IOException e) { menuMessage="Save failed. Previous save was kept.";notifyPlayer(menuMessage);System.err.println(e.getMessage());return false; }
    }
    boolean loadCurrent() {
        try {
            SaveGame.Snapshot saved=SaveGame.read(savePath);
            progress=saved.progression();selectedDifficulty=progress.difficulty();director.difficulty(progress.difficulty());director.restore(saved.wave());enemies.clear();enemies.addAll(saved.enemies());projectiles.clear();heroShots.clear();inventory=false;
            selectedClass=progress.heroClass();applyHeroModel();
            player.setName(saved.name());settings.playerName=saved.name();player.getPosition().x(saved.x()).z(saved.z());player.getRotation().y(saved.facing());
            applyProgression();player.resetCombat();player.discipline().stamina(saved.stamina());lastHeroX=saved.x();lastHeroZ=saved.z();heavyCharge=0;bossHazards.clear();feedback.clear();vfx.clear();wave=director.wave();nextWave=director.rest();workshop=false;
            combo=0;comboWindow=inputBuffer=attackElapsed=hurtFlash=slashTime=noticeTime=0;attackCooldown=.3f;impactPending=false;
            sceneManager.getOpenScene().getCameraController().update(0);
            state=director.levelCleared(enemies.size())?(wave>=10?GameState.VICTORY:GameState.LEVEL_COMPLETE):GameState.PAUSED;menuMessage="Game loaded. Press Enter to resume.";return true;
        } catch (IOException e) { menuMessage="Could not load game. Current defense was kept.";notifyPlayer(menuMessage);System.err.println(e.getMessage());return false; }
    }
    Combat.Attack currentAttack() { return activeAttack; }
    java.util.List<BossHazard> getBossHazards() { return java.util.Collections.unmodifiableList(bossHazards); }
    boolean shadowAtlasValid() { return renderer.shadowAtlasValid(); }
    java.util.List<Enemy> getEnemies() { return java.util.Collections.unmodifiableList(enemies); }
    Progression getProgression() { return progress; }
    GameAudio audio() { return audio; }
    boolean isWorkshopOpen() { return workshop; }
    public boolean shouldQuit() { return quitRequested; }
    public void dispose() { persistSettings(); if (state==GameState.PLAYING||state==GameState.PAUSED||state==GameState.LEVEL_COMPLETE||state==GameState.VICTORY||state==GameState.JOURNAL&&journalReturn==GameState.PAUSED||state==GameState.SETTING_MENU&&settingsReturn==GameState.PAUSED) saveCurrent(); audio.close();vfx.close(); assets.close();renderer.close();equipmentScreen.close(); player.dispose(); Menu.close(); }
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
