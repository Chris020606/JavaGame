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
    private boolean pointerAim;
    void pointerAim(boolean enabled) { pointerAim=enabled; }
    private final Settings settings=new Settings();
    private Tutorial tutorial;
    private SaveGame.Snapshot practiceReturn;
    private GameState practiceReturnState;
    private boolean practiceReturnPlaytest;
    private Tutorial.Step preparedStep;
    private final CombatMastery mastery=new CombatMastery();
    private float hitPause,hitMarker;
    private boolean activeHeavy,activeCharged;
    private final ArrayList<Enemy> shotTargets=new ArrayList<>();
    Tutorial tutorial() { return tutorial; }
    void beginTutorial() {
        if(tutorial!=null)return;
        practiceReturnState=state;practiceReturnPlaytest=playtest;
        practiceReturn=new SaveGame.Snapshot(player.getName(),player.getPosition().x(),player.getPosition().z(),player.getRotation().y(),progress,director.snapshot(),java.util.List.copyOf(enemies),Arena.HIGHLANDS,player.discipline().stamina(),dayNight.phase());
        tutorial=new Tutorial();preparedStep=null;playtest=false;start();storyTime=0;
    }
    void finishTutorial() {
        if(tutorial==null)return;
        tutorial=null;settings.tutorialSeen=true;persistSettings();
        restoreSnapshot(practiceReturn,practiceReturnPlaytest);state=practiceReturnState;practiceReturn=null;
        notifyPlayer("PRACTICE COMPLETE / Your adventure is ready.");
    }
    private void prepareTutorial() {
        if(tutorial==null || preparedStep==tutorial.step())return;
        preparedStep=tutorial.step();enemies.clear();heroShots.clear();projectiles.clear();
        player.discipline().reset();
        if(preparedStep==Tutorial.Step.STRIKE || preparedStep==Tutorial.Step.CHARGE || preparedStep==Tutorial.Step.PARRY) {
            Enemy partner=new Enemy(player.getPosition().x()+2.2f,player.getPosition().z(),1,0,.5f,Difficulty.EASY);
            partner.health=9999;enemies.add(partner);player.aimAt(partner.x,partner.z);
        }
        if(preparedStep==Tutorial.Step.HAVEN)progress.damagePlayer(25);
        audio.play("upgrade");
    }
    private void connected(Enemy enemy,boolean heavy,boolean charged) {
        hitMarker=.16f;if(progress.heroClass()==HeroClass.WARRIOR)hitPause=Math.max(hitPause,heavy?.045f:.025f);
        if(!heavy)mastery.lightHit(progress.heroClass());
        if(tutorial!=null)tutorial.hit(charged);
    }
    private final Path settingsPath=AppData.directory().resolve("settings.properties"), savePath=AppData.directory().resolve("save.properties");
    private final Path playtestSavePath=AppData.directory().resolve("playtest-save.properties");
    private boolean playtest,maxPlaytestTraining=true;
    private int playtestChapter,playtestTime;
    private final DayNightCycle dayNight=new DayNightCycle();
    private static final float[] PLAYTEST_TIMES={.36f,.5f,.73f,0};
    private static final String[] TIME_LABELS={"MORNING","NOON","SUNSET","MIDNIGHT"};
    void dayPhase(float phase) { dayNight.phase(phase); }
    float dayPhase() { return dayNight.phase(); }
    private GameState playtestReturn=GameState.MAIN_MENU;
    private GameState settingsReturn=GameState.MAIN_MENU;
    private Controls.Action rebinding;
    private String nameDraft="", menuMessage="";
    private final ArrayList<Projectile> projectiles=new ArrayList<>();
    private Player player;
    private GameState journalReturn=GameState.MAIN_MENU;
    private GameState blessingReturn=GameState.PLAYING;
    private float storyTime;
    private Difficulty selectedDifficulty=Difficulty.NORMAL;
    private float lastHeroX,lastHeroZ;
    private final HudScreen hudScreen=new HudScreen();
    private boolean hudCharging,uiMouseCaptured;
    private final WorkshopScreen workshopScreen=new WorkshopScreen();
    private final EquipmentScreen equipmentScreen=new EquipmentScreen();
    private final HeroPortrait heroPortrait=new HeroPortrait();
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
    private final ArrayList<Enemy> fallen=new ArrayList<>();
    private float outcomeTime;
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
    private boolean photoMode;
    void photoMode(boolean enabled) { photoMode=enabled; }
    private final Camera2D uiCamera = new Camera2D().zoom(1);

    public Game() { this(()->IsMouseButtonPressed(MOUSE_BUTTON_LEFT));pointerAim=true; }
    Game(java.util.function.BooleanSupplier mouseAttack) {
        this.mouseAttack=mouseAttack;
        settings.load(settingsPath);selectedDifficulty=settings.difficulty; menuMessage=settings.warning;
        Scene scene = sceneManager.createScene("Outpost");
        sceneManager.setOpenScene(scene);
        Player knight = new Player();
        knight.setName(settings.playerName);
        knight.setControls(settings.controls);
        knight.setPosition(new Vector3().x(0).y(0).z(4));
        knight.setModel(LoadModel(AssetPaths.path("resources/outpost/hero_warrior.glb")));
        knight.loadAnimations(AssetPaths.path("resources/outpost/hero_warrior.glb"));
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
            String path=AssetPaths.path(selectedClass==HeroClass.WARRIOR ? "resources/outpost/hero_warrior.glb" :
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
        applyHeroModel(); storyTime=12;hudCharging=false;mastery.reset();hitPause=hitMarker=0;
        progress = playtest?Progression.playtest(selectedClass,selectedDifficulty,maxPlaytestTraining):new Progression(selectedClass,selectedDifficulty); player.setMovementSpeed(selectedClass.speed); inventory=false; fallen.clear();outcomeTime=0;heroShots.clear(); enemies.clear(); projectiles.clear(); wave = 0; nextWave = 5;
        attackCooldown = slashTime = noticeTime = heavyCharge = 0; bossHazards.clear(); workshop = false;
        player.getPosition().x(0).y(0).z(4);
        player.resetCombat();
        director.reset();director.difficulty(progress.difficulty());
        if(playtest) { director.restore(new WaveDirector.State(Campaign.CHAPTERS[playtestChapter].first()-1,0,0,5,0,0,21));wave=director.wave();inventory=true; }
        dayNight.phase(playtest?PLAYTEST_TIMES[playtestTime]:DayNightCycle.initial(0));
        applyProgression();lastHeroX=0;lastHeroZ=4; feedback.clear();vfx.clear(); combo=0;
        comboWindow=inputBuffer=attackElapsed=hurtFlash=0; impactPending=false;
        player.setName(settings.playerName); menuMessage="";
        state = GameState.PLAYING;
    }
    private void applyProgression() { player.setCrossbow(progress.heroClass()==HeroClass.ARCHER && progress.weaponTier()>=2);player.setWeaponTier(progress.weaponTier());player.setMovementSpeed(progress.movementSpeed());player.setDodgeRecovery(1.5f-.15f*progress.expedition().rank(Expedition.Blessing.WAYFARER));player.discipline().configure(progress.maxStamina(),progress.staminaRecovery()); }
    private void aimCursor() {
        if(!pointerAim || IsMouseButtonDown(MOUSE_BUTTON_RIGHT))return;
        Ray ray=GetScreenToWorldRay(GetMousePosition(),sceneManager.getOpenScene().getCameraController().getCamera());
        if(Math.abs(ray.direction().y())<.0001f)return;
        float t=(1.1f-ray._position().y())/ray.direction().y();
        if(t>0)player.aimAt(ray._position().x()+ray.direction().x()*t,ray._position().z()+ray.direction().z()*t);
    }
    private boolean nearBase() { return Math.hypot(player.getPosition().x(), player.getPosition().z()) < 6; }
    private void notifyPlayer(String message) { notice = message; noticeTime = 3; }

    public void update(float dt) {
        dt = Math.max(0, Math.min(dt, .05f));applyProgression();
        if(!IsMouseButtonDown(MOUSE_BUTTON_LEFT))uiMouseCaptured=false;
        else if(inventory || workshop || state!=GameState.PLAYING)uiMouseCaptured=true;
        hitMarker=Math.max(0,hitMarker-dt);
        if(state==GameState.PLAYING && !inventory && !workshop) {
            mastery.tick(dt);
            if(hitPause>0) { float held=Math.min(dt,hitPause);hitPause-=held;dt-=held; }
        }
        prepareTutorial();
        final float presentationStep=dt;
        boolean outcome=state==GameState.LEVEL_COMPLETE || state==GameState.VICTORY || state==GameState.GAME_OVER;
        if((state==GameState.PLAYING || outcome) && !inventory && !workshop) {
            fallen.removeIf(e->{e.deathTime+=presentationStep;return e.deathTime>CorpsePresentation.lifetime(e.boss());});
            if(outcome) { outcomeTime+=dt;noticeTime=Math.max(0,noticeTime-dt);feedback.update(dt);vfx.update(dt);player.poseForOutcome(state!=GameState.GAME_OVER,outcomeTime); }
        }
        boolean titleAudio=state==GameState.PLAYTEST_SETUP && playtestReturn==GameState.MAIN_MENU || state==GameState.MAIN_MENU || state==GameState.LOGIN || state==GameState.JOURNAL && journalReturn==GameState.MAIN_MENU || state==GameState.SETTING_MENU && settingsReturn==GameState.MAIN_MENU;
        boolean battle=state==GameState.PLAYING && !inventory && !workshop && !enemies.isEmpty();
        Enemy musicBoss=enemies.stream().filter(e->e.boss() && e.health>0).findFirst().orElse(null);
        audio.update(dt,chapterIndex(),titleAudio,battle,battle && musicBoss!=null,battle && musicBoss!=null && musicBoss.enraged());
        if(state==GameState.RELIC_CHOICE) {
            if(IsKeyPressed(KEY_ESCAPE))state=blessingReturn;
            else if(settings.controls.pressed(SAVE))saveCurrent();
            else for(int i=0;i<progress.expedition().offers().size();i++)if(IsKeyPressed(KEY_ONE+i)) { chooseBlessing(i);break; }
            return;
        }
        if((state==GameState.PLAYING || state==GameState.LEVEL_COMPLETE) && settings.controls.pressed(INTERACT) && nearBase() && enemies.isEmpty() && director.remaining()==0 && progress.expedition().embers()>0 && !progress.expedition().offers().isEmpty()) {
            blessingReturn=state;state=GameState.RELIC_CHOICE;return;
        }
        if(state==GameState.PLAYTEST_SETUP) {
            if(IsKeyPressed(KEY_ESCAPE))state=playtestReturn;
            else if(IsKeyPressed(KEY_ENTER))startPlaytest();
            return;
        }
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
        if (tutorial==null && settings.controls.pressed(LOAD)) { loadActive(); return; }
        if (IsKeyPressed(KEY_ENTER)) {
            if (state == GameState.MAIN_MENU) { requestNewGame(); return; }
            if (state == GameState.GAME_OVER) { start(); return; }
            if (state == GameState.PAUSED) { state = GameState.PLAYING; return; }
        }
        if (state != GameState.PLAYING) return;
        HudLayout belt=new HudLayout(GetScreenWidth(),GetScreenHeight());Vector2 pointer=GetMousePosition();
        int hovered=pointerAim && !inventory && !workshop?belt.hit(pointer.x(),pointer.y()):-1;
        int beltClick=hovered>=0 && IsMouseButtonPressed(MOUSE_BUTTON_LEFT)?hovered:-1;
        if(beltClick==1)hudCharging=true;
        if(!IsMouseButtonDown(MOUSE_BUTTON_LEFT))hudCharging=false;
        boolean overBelt=pointerAim && !inventory && !workshop && belt.contains(pointer.x(),pointer.y()) || hudCharging || uiMouseCaptured;
        boolean heavyDown=settings.controls.down(HEAVY) || hudCharging;
        if(beltClick==2)player.requestParry();
        if (settings.controls.pressed(INVENTORY) || beltClick==5) { inventory=!inventory; workshop=false;if(tutorial!=null && inventory)tutorial.inventory(); }
        if (inventory) { vfx.blade().clear();return; }
        if (settings.controls.pressed(POTION) || beltClick==4) {
            notifyPlayer(progress.usePotion()?"Potion used / +60 HP":"Need missing health and a potion.");
        }
        if (settings.controls.pressed(WORKSHOP)) {
            if (nearBase()) workshop = !workshop;
            else notifyPlayer("Return to the outpost to upgrade.");
        }
        if (workshop) { vfx.blade().clear();return; }
        noticeTime = Math.max(0, noticeTime - dt); storyTime=Math.max(0,storyTime-dt);
        attackCooldown = Math.max(0, attackCooldown - dt);
        slashTime = Math.max(0, slashTime - dt);
        hurtFlash=Math.max(0,hurtFlash-dt);
        comboWindow=Math.max(0,comboWindow-dt);
        inputBuffer=Math.max(0,inputBuffer-dt);
        feedback.update(dt);vfx.update(dt);dayNight.update(dt);
        boolean wasDodging=player.isDodging();float beforeMoveX=player.getPosition().x(),beforeMoveZ=player.getPosition().z();
        if(!overBelt && (IsMouseButtonDown(MOUSE_BUTTON_LEFT) || heavyDown))aimCursor();
        sceneManager.getOpenScene().updatesEntities(dt);
        if(progress.heroClass()==HeroClass.WARRIOR && player.isAttacking() && dt>0) {
            float elapsed=activeAttack.duration()-player.attackRemaining();
            if(elapsed>=activeAttack.impact()-.07f && elapsed<=activeAttack.impact()+(activeHeavy?.20f:.14f))
                assets.sampleBlade(player,progress.weaponTier(),vfx.blade());
        }
        if(!wasDodging && player.isDodging()) { audio.play("dash");if(tutorial!=null)tutorial.dodge(); }
        if(tutorial!=null) {
            tutorial.movement((float)Math.hypot(player.getPosition().x()-beforeMoveX,player.getPosition().z()-beforeMoveZ));
            tutorial.healing(dt,nearBase());
        }
        float px = player.getPosition().x(), pz = player.getPosition().z();
        FieldEvent event=progress.fieldEvent();FieldEvent.Status previousEventStatus=event.status();event.update(dt,px,pz,settings.controls.down(INTERACT));
        if(event.claim()) { progress.rewardObjective();audio.play("upgrade");vfx.impact(event.x(),event.z(),Menu.GOLD,true);notifyPlayer("OBJECTIVE COMPLETE / +65 CRYSTALS / +1 EMBER BLESSING / SUPPLIES");saveCurrent(); }
        float heroVx=Math.max(-15,Math.min(15,(px-lastHeroX)/Math.max(.001f,dt))),heroVz=Math.max(-15,Math.min(15,(pz-lastHeroZ)/Math.max(.001f,dt)));
        lastHeroX=px;lastHeroZ=pz;
        if (nearBase()) progress.heal(dt);
        // Impact occurs part-way through the swing, once only, in the locked facing arc.
        if (impactPending) {
            attackElapsed+=dt;
            if (attackElapsed>=activeAttack.impact()) { resolvePlayerHit(px,pz); impactPending=false; }
        }
        boolean finishingChapter=director.levelCleared(0) && enemies.stream().noneMatch(e->e.health>0);
        if(finishingChapter) { inputBuffer=0;heavyCharge=0;player.setCharging(false); }
        if(player.isDodging()) { inputBuffer=0;comboWindow=0;combo=0;heavyCharge=0;player.setCharging(false); }
        if(!finishingChapter && heavyDown && attackCooldown<=0 && !player.isDodging() && player.discipline().recovery()<=0) {
            heavyCharge=Math.min(1.4f,heavyCharge+dt);player.setCharging(true);
        } else if(heavyCharge>0 && !heavyDown) {
            releasedCharge=heavyCharge;heavyCharge=0;player.setCharging(false);
            bufferedHeavy=true;bufferedLunge=false;inputBuffer=.22f;
        }
        boolean mousePressed=mouseAttack.getAsBoolean() && !overBelt;
        boolean mouseHeld=pointerAim && !overBelt && IsMouseButtonDown(MOUSE_BUTTON_LEFT) && !IsMouseButtonDown(MOUSE_BUTTON_RIGHT) && attackCooldown<=0;
        boolean lightInput=settings.controls.pressed(SLASH)||mousePressed||mouseHeld||beltClick==0;
        if(!overBelt && (mousePressed || mouseHeld || heavyCharge>0))aimCursor();
        if(lightInput && !finishingChapter && heavyCharge<=0) {
            bufferedHeavy=bufferedLunge=false;
            bufferedTimed=(!mouseHeld || mousePressed) && combo>0 && comboWindow>0 && attackCooldown<=.22f;
            inputBuffer=.22f;
        }
        if(!finishingChapter && (settings.controls.pressed(LUNGE) || beltClick==3) && heavyCharge<=0) {
            bufferedLunge=true;bufferedHeavy=false;inputBuffer=.22f;
        }
        if(inputBuffer>0 && attackCooldown<=0 && !player.isDodging() && heavyCharge<=0 && player.discipline().recovery()<=0) {
            float cost=bufferedLunge?CombatDiscipline.LUNGE:bufferedHeavy?CombatDiscipline.HEAVY:CombatDiscipline.LIGHT;
            inputBuffer=0;
            if(!player.discipline().spend(cost)) notifyPlayer("LOW STAMINA / Breathe before attacking");
            else {
                combo=bufferedLunge||bufferedHeavy?0:bufferedTimed?combo%3+1:1;
                activeAttack=bufferedLunge?Combat.lunge(progress.heroClass()):progress.heroClass().attack(combo,bufferedHeavy);
                if(!bufferedLunge && progress.heroClass()==HeroClass.ARCHER && progress.weaponTier()>=2)
                    activeAttack=new Combat.Attack(bufferedHeavy?1.1f:.84f,bufferedHeavy?.20f:.16f,activeAttack.range(),activeAttack.halfAngle(),activeAttack.multiplier(),activeAttack.label());
                if(bufferedHeavy && releasedCharge>.35f)
                    activeAttack=new Combat.Attack(activeAttack.duration(),progress.heroClass()==HeroClass.WARRIOR?.18f:.12f,activeAttack.range(),activeAttack.halfAngle(),activeAttack.multiplier(),activeAttack.label());
                if(bufferedHeavy)activeAttack=Combat.scale(activeAttack,1+.12f*progress.expedition().rank(Expedition.Blessing.KINDLING),activeAttack.label());
                if(bufferedHeavy) {
                    boolean perfect=releasedCharge>=.75f && releasedCharge<=1.05f;
                    float bonus=perfect?1.6f:releasedCharge>1.2f?1.05f:1+releasedCharge*.25f;
                    activeAttack=Combat.scale(activeAttack,bonus,perfect?"PERFECT CHARGE":activeAttack.label());
                } else if(combo>1) activeAttack=Combat.scale(activeAttack,1.12f,"TIMED CHAIN "+combo+" / 3");
                float riposte=player.discipline().consumeRiposte();
                if(riposte>1) activeAttack=Combat.scale(activeAttack,riposte,"RIPOSTE");
                float classBonus=mastery.attackBonus(progress.heroClass(),bufferedHeavy);
                if(classBonus>1)activeAttack=Combat.scale(activeAttack,classBonus,"EMPOWERED "+activeAttack.label());
                attackCooldown=activeAttack.duration();comboWindow=attackCooldown+.23f;
                attackElapsed=0;impactPending=true;activeHeavy=bufferedHeavy;activeCharged=bufferedHeavy && releasedCharge>=.75f;
                vfx.beginMelee(activeHeavy);
                if(bufferedLunge) { player.lunge(activeAttack.duration(),activeAttack.impact());audio.play("dash"); }
                else player.attack(combo,bufferedHeavy,activeAttack.duration(),activeAttack.impact(),bufferedHeavy?releasedCharge:0);
            }
        }
        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            if (enemy.health <= 0) {
                iterator.remove(); progress.rewardEnemy(enemy.boss()); audio.play(enemy.boss()?"boss-fall":"kill");
                enemy.attackTime=enemy.chargeTime=enemy.stun=enemy.hitFlash=0;enemy.healingTarget=null;enemy.deathTime=0;
                enemy.corpseSettled=false;
                if(fallen.size()>=48)fallen.remove(0);fallen.add(enemy);
                vfx.impact(enemy.x,enemy.z,enemy.boss()?Menu.GOLD:Menu.TEAL,enemy.boss());
                feedback.add(enemy.x,enemy.z,"+"+enemy.reward()+" crystals",Menu.TEAL);
                if (enemy.boss()) notifyPlayer(chapter().title()+" DEFEATED / "+progress.weaponName(progress.weaponUnlocked())+" in inventory");
                continue;
            }
            if(tutorial!=null) { enemy.health=9999;if(tutorial.step()!=Tutorial.Step.PARRY) { enemy.stun=.2f;enemy.attackTime=0;enemy.impactPending=false; } }
            boolean huntsPlayer=tutorial!=null || enemy.chooseHero(px,pz,dt);
            boolean huntsTraveler=!enemy.boss() && event.vulnerable() && enemy.distance(event.x(),event.z())<9 && (!huntsPlayer || enemy.distance(event.x(),event.z())<enemy.distance(px,pz)*.75f);
            if(huntsTraveler)huntsPlayer=false;
            float tx=huntsTraveler?event.x():huntsPlayer?px:0,tz=huntsTraveler?event.z():huntsPlayer?pz:0,separationX=0,separationZ=0;
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
                } else if(enemy.targetsTraveler && event.vulnerable() && enemy.distance(event.x(),event.z())<enemy.meleeRange()+.1f) {
                    event.damage(enemy.siegeDamage(wave));vfx.impact(event.x(),event.z(),Menu.RED,false);audio.play("hurt");
                } else if (!enemy.targetsPlayer && !enemy.targetsTraveler && enemy.distance(0,0)<enemy.meleeRange()+.1f) {
                    float before=progress.baseHealth(); progress.damageBase(enemy.siegeDamage(wave));
                    feedback.add(0,0,"OUTPOST -"+Math.round(before-progress.baseHealth()),Menu.RED); audio.play("hurt");
                }
            }
            if (enemy.stun<=0 && enemy.attackTime<=0 && enemy.cooldown<=0 && enemy.distance(tx,tz)<enemy.attackRange() && enemy.facingTarget(tx,tz) && (!huntsPlayer || SquadTactics.canJoinAssault(enemy,enemies)))
            {
                if(enemy.ranged() && !SquadTactics.clearShot(enemy,tx,tz,enemies)) { enemy.sideStepTime=.65f;continue; }
                float lead=enemy.ranged() && huntsPlayer?Math.min(.6f,enemy.distance(px,pz)/10)*(.45f+(1-enemy.difficulty.cadence)) : 0;
                enemy.beginAttack(Math.max(-22,Math.min(22,tx+heroVx*lead)),Math.max(-22,Math.min(22,tz+heroVz*lead)),huntsPlayer);
                enemy.targetsTraveler=huntsTraveler;
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
            shotTargets.clear();shotTargets.addAll(enemies);
            shotTargets.sort(java.util.Comparator.comparingDouble(e -> (e.x-shot.previousX)*(e.x-shot.previousX)+(e.z-shot.previousZ)*(e.z-shot.previousZ)));
            for (Enemy enemy:shotTargets) if (shot.hit(enemy)) {
                float damage=Math.min(enemy.health,shot.damage);
                enemy.hit(damage,shot.previousX,shot.previousZ,.25f);vfx.impact(enemy.x,enemy.z,shot.magic?Menu.TEAL:Menu.GOLD,false);
                feedback.add(enemy.x,enemy.z,"-"+Math.round(damage),Menu.GOLD); audio.play("hit");connected(enemy,shot.heavy,shot.charged);
                if (!shot.magic) { stopped=true;break; }
            }
            if (stopped || shot.remaining<=0) shots.remove();
        }
        separateEnemies(dt);
        for (Iterator<Projectile> shots=projectiles.iterator();shots.hasNext();) {
            Projectile shot=shots.next(); shot.update(dt);
            boolean hit=shot.travelerTarget?event.vulnerable() && shot.hits(event.x(),event.z(),.8f):shot.playerTarget?shot.hits(px,pz,.65f):shot.hits(0,0,1.8f);
            if (hit) {
                if(shot.travelerTarget) { event.damage(shot.damage);vfx.impact(event.x(),event.z(),Menu.RED,false);audio.play("hurt"); }
                else if(shot.playerTarget) strikeHero(shot.damage,shot.x-shot.vx*.2f,shot.z-shot.vz*.2f,true,null);
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
        if(event.status()==FieldEvent.Status.FAILED && previousEventStatus!=FieldEvent.Status.FAILED)notifyPlayer("OPTIONAL OBJECTIVE LOST / The sanctuary still needs you.");
        if (progress.defeated()) { state = GameState.GAME_OVER;outcomeTime=0;player.resetCombat();impactPending=false;heavyCharge=0;player.poseForOutcome(false,0); return; }
        if(wave>0 && director.total()>0 && director.remaining()==0 && enemies.isEmpty() && progress.expedition().waveCleared(wave))notifyPlayer("EMBER BLESSING EARNED / Return to the sanctuary and press "+settings.controls.label(INTERACT));
        if(tutorial!=null)return;
        WaveDirector.Spawn spawn=director.update(dt,enemies.size());
        if (director.wave()!=wave) { wave=director.wave();progress.fieldEvent().begin(wave);audio.play("wave"); notifyPlayer(chapter().bossOnly()?chapter().title()+" / BOSS DUEL":"WAVE "+(wave-chapter().first()+1)+" / "+WaveFormation.label(wave)); }
        if (spawn!=null) enemies.add(new Enemy(spawn.x(),spawn.z(),spawn.wave(),spawn.variant(),random.nextFloat(),progress.difficulty()));
        nextWave=director.rest();
        if(director.levelCleared(enemies.size()) && !player.isAttacking()) {
            heroShots.clear();projectiles.clear();bossHazards.clear();heavyCharge=0;player.resetCombat();impactPending=false;attackCooldown=0;
            state=wave>=10?GameState.VICTORY:GameState.LEVEL_COMPLETE;
            outcomeTime=0;audio.play("victory");player.poseForOutcome(true,0);
            vfx.impact(player.getPosition().x(),player.getPosition().z(),Menu.GOLD,true);
            saveCurrent();
        }
    }
    private void strikeHero(float damage,float sourceX,float sourceZ,boolean parryable,Enemy attacker) {
        float px=player.getPosition().x(),pz=player.getPosition().z();
        if(player.invulnerable()) { mastery.evade(progress.heroClass());feedback.add(px,pz,progress.heroClass()==HeroClass.ARCHER?"EVADE / FOCUS":"EVADE",Menu.TEAL);return; }
        if(parryable && Combat.inArc(px,pz,player.getRotation().y(),sourceX,sourceZ,1000,75) && player.discipline().deflect()) {
            if(attacker!=null) { attacker.stun=1.1f;attacker.attackTime=attacker.chargeTime=0;attacker.impactPending=false;attacker.cooldown=1.3f; }
            mastery.parry(progress.heroClass());if(tutorial!=null)tutorial.parry();
            player.discipline().stamina(player.discipline().stamina()+10*progress.expedition().rank(Expedition.Blessing.SECOND_WIND));
            vfx.impact(px,pz,Menu.TEAL,true);feedback.add(px,pz,"PARRY / RIPOSTE",Menu.GOLD);audio.play("parry");return;
        }
        if(tutorial!=null) { feedback.add(px,pz,"SAFE PRACTICE / TRY AGAIN",Menu.TEAL);return; }
        float before=progress.health();progress.damagePlayer(damage);progress.fieldEvent().interrupt();vfx.impact(px,pz,Menu.RED,false);
        feedback.add(px,pz,"-"+Math.round(before-progress.health()),Menu.RED);hurtFlash=.25f;audio.play("hurt");
    }
    private void continueCampaign() {
        if(state==GameState.VICTORY) { state=GameState.MAIN_MENU;return; }
        director.advanceLevel(); storyTime=12; inventory=workshop=false;fallen.clear();outcomeTime=0;
        player.getPosition().x(0).y(0).z(4);player.resetCombat();
        state=GameState.PLAYING;saveCurrent();
    }
    private void resolvePlayerHit(float px,float pz) {
        slashTime=.16f;
        audio.play(progress.heroClass()==HeroClass.WARRIOR?"swing":progress.heroClass()==HeroClass.ARCHER?"bow":"magic");
        if (progress.heroClass()!=HeroClass.WARRIOR) {
            Vector3 origin=player.projectileOrigin();
            HeroShot shot=new HeroShot(origin.x(),origin.z(),player.getRotation().y(),activeAttack,Math.round(progress.damage()*(1+.10f*progress.expedition().rank(Expedition.Blessing.HUNTERS_MARK))),progress.heroClass()==HeroClass.MAGE);
            shot.startHeight(origin.y());shot.heavy=activeHeavy;shot.charged=activeCharged;heroShots.add(shot);
            return;
        }
        boolean hit=false;
        for (Enemy enemy:enemies) if (enemy.health>0 && Combat.inArc(px,pz,player.getRotation().y(),enemy.x,enemy.z,activeAttack.range(),activeAttack.halfAngle())) {
            float damage=Math.min(enemy.health,Math.round(progress.damage()*activeAttack.multiplier()));
            enemy.hit(damage,px,pz,activeAttack.multiplier()>1?.8f:.35f);vfx.impact(enemy.x,enemy.z,Menu.GOLD,activeAttack.multiplier()>1);
            feedback.add(enemy.x,enemy.z,"-"+Math.round(damage),Menu.GOLD);connected(enemy,activeHeavy,activeCharged); hit=true;
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
        // UI buttons can open inventory later in this render. Show it next frame,
        // after its offscreen previews have been prepared outside the 2D overlay.
        boolean inventoryPrepared=inventory;
        if(inventoryPrepared) equipmentScreen.prepare(assets,player,progress);
        if(state==GameState.LOGIN)heroPortrait.prepare(assets,selectedClass);
        // Jaylib 6.0.1-0's SDL backend restores a framebuffer-pixel projection
        // after 3D. Scale the overlay to window pixels, matching GetMousePosition.
        // Recompute each frame for resizing and moving between Retina/1x displays.
        uiCamera.zoom((float)GetRenderWidth() / Math.max(1, GetScreenWidth()));
        BeginMode2D(uiCamera);
        try {
            if(photoMode)return;
            drawEnemyHealth();
            if (state == GameState.MAIN_MENU) { drawMainMenu(); return; }
            if (state == GameState.LOGIN) { drawNameEntry(); return; }
            if(state==GameState.PLAYTEST_SETUP) { drawPlaytestSetup();return; }
            if (state == GameState.SETTING_MENU) { drawSettings(); return; }
            if(state==GameState.RELIC_CHOICE) { drawBlessings();return; }
            feedback.draw(sceneManager.getOpenScene().getCameraController().getCamera());
            if (hurtFlash>0) {
                Color tint=Fade(Menu.RED,hurtFlash*1.5f);
                DrawRectangle(0,0,GetScreenWidth(),9,tint); DrawRectangle(0,GetScreenHeight()-9,GetScreenWidth(),9,tint);
                DrawRectangle(0,0,9,GetScreenHeight(),tint); DrawRectangle(GetScreenWidth()-9,0,9,GetScreenHeight(),tint);
            }
            drawHud();
            if(tutorial!=null && !inventory && !workshop)drawTutorial();
            if(tutorial==null && storyTime>0 && state==GameState.PLAYING && !inventory && !workshop) {
                Menu.panel(24,190,270,132);
                Menu.text("CHAPTER "+(chapterIndex()+1)+" / "+chapter().title(),38,200,14,Menu.GOLD);
                Menu.wrapped(chapter().story(),38,228,240,16,Menu.TEXT);
            }
            if((state==GameState.LEVEL_COMPLETE || state==GameState.VICTORY) && !inventory) drawChapterEnd();
            if(state==GameState.JOURNAL) drawJournal();
            if (workshop) drawWorkshop();
            if (inventory && inventoryPrepared) drawInventory();
            if (state == GameState.PAUSED || state == GameState.GAME_OVER) drawOverlay();
        } finally {
            EndMode2D();
        }
    }
    private Vector3 v(float x, float y, float z) { return new Vector3().x(x).y(y).z(z); }
    private static Color skyColor(DayNightCycle.Rgb c) { return Menu.color(Math.round(c.r()*255),Math.round(c.g()*255),Math.round(c.b()*255)); }
    private void drawWorld() {
        float time=(float)GetTime();Camera3D camera=sceneManager.getOpenScene().getCameraController().getCamera();
        assets.view(player,camera);
        DayNightCycle.Frame light=dayNight.frame();
        renderer.beginShadow(time,light);assets.pass(renderer.shadowShader,true);
        assets.drawEnvironment(chapterIndex(),time);drawActors();renderer.endShadow();
        renderer.beginScene();
        DrawRectangleGradientV(0,0,renderer.width(),renderer.height(),skyColor(light.top()),skyColor(light.horizon()));
        BeginMode3D(camera);
        Enemy boss=enemies.stream().filter(e->e.boss() && e.health>0).findFirst().orElse(null);
        assets.drawSky(camera,light);
        assets.litPass(renderer,camera,time,boss,light);assets.drawEnvironment(chapterIndex(),time);drawActors();
        if (director.warning()) {
            DrawCylinderWires(v(director.x(),.08f,director.z()),1.4f,1.4f,.15f,24,Menu.RED);
            DrawLine3D(v(director.x(),0,director.z()),v(director.x(),3,director.z()),Menu.GOLD);
        }
        vfx.begin();
        for(HeroShot shot:heroShots)vfx.projectile(camera,shot.x,shot.z,shot.vx,shot.vz,shot.magic,shot.magic?Menu.TEAL:Menu.GOLD,time,shot.height());
        if(progress.heroClass()==HeroClass.WARRIOR)vfx.drawBlade();
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
        for(Enemy enemy:fallen)assets.drawEnemy(enemy);
        assets.drawFieldEvent(progress.fieldEvent(),(float)GetTime());
    }
    private void drawEnemyHealth() {
        for (Enemy enemy : enemies) {
            Vector2 screen = GetWorldToScreen(v(enemy.x, enemy.boss()?(enemy.king?6.0f:5.2f):2.1f, enemy.z), sceneManager.getOpenScene().getCameraController().getCamera());
            Menu.bar((int)screen.x()-22, (int)screen.y(), 44, enemy.health, enemy.maxHealth, enemy.boss()||enemy.variant==2?Menu.GOLD:Menu.RED);
            String label=tutorial!=null?enemy.attackTime>.25f?"PARRY THE GOLD STRIKE":"PRACTICE PARTNER":enemy.attackTime>.25f?"! ATTACK":enemy.boss()?(wave==10?"HOLLOW KING":wave==5?"ASH WARDEN":"WARLORD"):enemy.label();
            Menu.text(label,(int)screen.x()-Menu.measure(label,10)/2,(int)screen.y()-14,10,tutorial!=null?Menu.GOLD:enemy.attackTime>.25f?Menu.RED:Menu.TEXT);
        }
    }
    private void drawHud() {
        int w=GetScreenWidth(),h=GetScreenHeight();
        boolean compact=w<1000;int identity=compact?150:218;
        Menu.panel(16,20,identity,70);
        Menu.icon(progress.heroClass()==HeroClass.WARRIOR?"sword":progress.heroClass()==HeroClass.ARCHER?"bow":"staff",28,34,22,Menu.GOLD);
        Menu.text(progress.heroClass().label.toUpperCase(),60,31,14,Menu.TEXT);Menu.text(player.getName(),60,49,11,Menu.MUTED);
        Menu.text((compact?"":(playtest?"PLAYTEST":progress.difficulty().label.toUpperCase())+" / ")+"LV "+progress.characterLevel()+" / DEF "+progress.defense(),28,64,11,Menu.MUTED);
        int top=w/2-130;
        Menu.panel(top,20,260,82);
        Menu.center(tutorial!=null?"TUTORIAL / SAFE PRACTICE":"LEVEL "+(chapterIndex()+1)+" / "+chapter().title(),w/2,29,14,Menu.GOLD);
        int left=chapter().waves()-chapter().cleared(wave,director.remaining(),enemies.size(),director.total());
        Menu.center(tutorial!=null?"LESSON "+Math.min(8,tutorial.step().ordinal()+1)+" OF 8":chapter().bossOnly()?"BOSS ONLY  /  "+enemies.size()+" HOSTILES":"WAVE "+Math.max(0,wave-chapter().first()+1)+" / "+chapter().waves()+"   -   "+left+" LEFT",w/2,54,14,Menu.TEXT);
        Menu.center(tutorial!=null?"No raids / Your campaign is safe":state==GameState.LEVEL_COMPLETE || state==GameState.VICTORY?"THE SANCTUARY ENDURES":director.remaining()>0?director.remaining()+" reinforcements approaching":enemies.isEmpty()?"Next raid in "+(int)Math.ceil(nextWave)+"s":"Protect the sanctuary",w/2,77,11,Menu.MUTED);
        Menu.panel(w-156,20,140,62);Menu.icon("crystal",w-144,38,20,Menu.GOLD);
        Menu.text("CRYSTALS",w-114,29,10,Menu.MUTED);Menu.text(String.valueOf(progress.crystals()),w-114,46,20,Menu.GOLD);
        if(w>=1000)drawMinimap(w-184,102);
        if(state==GameState.PLAYING && !inventory && !workshop)drawCombatCues();
        int sy=compact?h-178:112,sw=compact?110:208;
        Menu.panel(16,sy,sw,56);Menu.text("SANCTUARY",28,sy+8,compact?10:13,Menu.GOLD);
        Menu.text((int)progress.baseHealth()+" / "+progress.maxBaseHealth(),28,sy+26,12,Menu.TEXT);
        Menu.bar(28,sy+44,sw-24,progress.baseHealth(),progress.maxBaseHealth(),Menu.GOLD);
        hudScreen.draw(progress,player,settings.controls,new HudScreen.CombatState(attackCooldown,activeAttack.duration(),activeAttack.impact(),heavyCharge,combo,comboWindow,mastery.label(progress.heroClass()),mastery.stacks()>0));
        if(!compact) {
            Menu.text(settings.controls.label(DODGE)+" DODGE / "+settings.controls.label(SPRINT)+" SPRINT",w-220,h-75,12,Menu.TEXT);
            Menu.text(settings.controls.label(WORKSHOP)+" WORKSHOP / ESC PAUSE",w-220,h-49,11,Menu.MUTED);
            Menu.text(settings.controls.label(SAVE)+" SAVE / "+settings.controls.label(INTERACT)+" INTERACT",w-220,h-27,10,Menu.MUTED);
        }
        if(nearBase()) Menu.text("+ HEALING HAVEN / +"+(3+progress.baseLevel())+" HP/s",28,174,11,Menu.TEAL);
        if(progress.expedition().embers()>0) {
            if(compact)Menu.text(progress.expedition().embers()+" EMBERS / "+settings.controls.label(INTERACT),28,110,11,Menu.GOLD);
            else {
                int by=h<740?h-202:h-250;
                Menu.panel(16,by,218,54);Menu.text(progress.expedition().embers()+" EMBER BLESSING"+(progress.expedition().embers()>1?"S":""),28,by+9,14,Menu.GOLD);
                Menu.text(settings.controls.label(INTERACT)+" AT SANCTUARY / CLEAR FIELD",28,by+32,10,Menu.TEAL);
            }
        }
        FieldEvent event=progress.fieldEvent();
        if(event.visible() && state==GameState.PLAYING && !inventory && !workshop) {
            Menu.panel(24,338,270,128);Menu.text("OPTIONAL / "+event.title(),38,350,16,Menu.GOLD);
            Menu.wrapped("["+settings.controls.label(INTERACT)+"] "+event.instruction(),38,380,244,14,Menu.TEXT);
            Menu.text((int)Math.ceil(event.time())+"s / "+(event.vulnerable()?"INTEGRITY "+(int)event.health():"+65 CRYSTALS + BLESSING"),38,443,12,Menu.TEAL);
            Vector2 marker=GetWorldToScreen(v(event.x(),2.8f,event.z()),sceneManager.getOpenScene().getCameraController().getCamera());
            Menu.text(event.kind()==FieldEvent.Kind.SURVIVOR?"SURVIVOR":"EMBER OBJECTIVE",(int)marker.x()-50,(int)marker.y(),12,Menu.TEAL);
        }
        for(Enemy enemy:enemies) if(enemy.boss() && enemy.health>0) {
            Menu.panel(w/2-170,114,340,72);
            Menu.center((wave==10?"HOLLOW KING":wave==5?"ASH WARDEN":"WARLORD")+"  /  "+(int)enemy.health+" HP",w/2,121,18,Menu.GOLD);
            Menu.bar(w/2-154,151,308,enemy.health,enemy.maxHealth,Menu.RED);
            Menu.center((enemy.enraged()?"ENRAGED / ":"")+enemy.moveLabel()+(enemy.bossMove.parryable?" / PARRY OR EVADE":" / UNBLOCKABLE"),w/2,164,12,Menu.GOLD);
        }
        if(noticeTime>0 && state==GameState.PLAYING) {
            int nw=compact?250:280,ny=compact?h-270:198;
            Menu.panel((w-nw)/2,ny,nw,74);
            Menu.wrapped(notice,w/2-nw/2+12,ny+10,nw-24,12,Menu.TEXT);
        }
    }
    private void drawTutorial() {
        int x=24,y=190,width=Math.min(330,GetScreenWidth()/3);
        Menu.panel(x,y,width,296);
        Menu.text("TRAINING / "+Math.min(8,tutorial.step().ordinal()+1)+" OF 8",x+16,y+12,13,Menu.TEAL);
        Menu.text(tutorial.title(),x+16,y+40,19,Menu.GOLD);
        Menu.wrapped(tutorial.instruction(settings.controls,progress.heroClass()),x+16,y+78,width-32,16,Menu.TEXT);
        Menu.bar(x+16,y+234,width-32,tutorial.step().ordinal(),7,Menu.TEAL);
        if(Menu.button(tutorial.complete()?"FINISH PRACTICE":"SKIP / RETURN",x+16,y+246,width-32,true))finishTutorial();
    }
    private void drawCombatCues() {
        if(pointerAim && !IsMouseButtonDown(MOUSE_BUTTON_RIGHT)) {
            Vector2 mouse=GetMousePosition();int x=(int)mouse.x(),y=(int)mouse.y();Color color=hitMarker>0?Menu.GOLD:Fade(Menu.TEXT,.65f);
            int r=hitMarker>0?10:6;DrawCircleLines(x,y,r,color);
            DrawLine(x-r-4,y,x-r,y,color);DrawLine(x+r,y,x+r+4,y,color);
            DrawLine(x,y-r-4,x,y-r,color);DrawLine(x,y+r,x,y+r+4,color);
        }
        if(hitMarker>0)Menu.center("HIT",GetScreenWidth()/2,GetScreenHeight()/2+35,13,Menu.GOLD);
        if(player.isAttacking()) {
            String phase=attackElapsed<activeAttack.impact()?"WINDUP":player.attackRemaining()>=activeAttack.duration()*.48f?"CONTACT":player.attackRemaining()>.12f?"RECOVER / DODGE TO DISENGAGE":"CHAIN WINDOW";
            Menu.center(phase,GetScreenWidth()/2,GetScreenHeight()-211,11,Menu.MUTED);
        }
    }
    private void drawMinimap(int x,int y) {
        Menu.panel(x,y,160,160);
        Menu.text("RADAR",x+12,y+10,12,Menu.MUTED);
        Menu.text(dayNight.frame().label()+" / "+dayNight.frame().clock(),x+12,y+143,11,Menu.GOLD);
        int cx=x+80,cy=y+90;
        DrawCircleLines(cx,cy,59,Menu.LINE);
        DrawLine(cx-59,cy,cx+59,cy,Menu.LINE); DrawLine(cx,cy-59,cx,cy+59,Menu.LINE);
        DrawCircle(cx,cy,5,Menu.TEAL);
        if(progress.fieldEvent().visible())DrawCircle(cx+(int)(progress.fieldEvent().x()*2.5f),cy+(int)(progress.fieldEvent().z()*2.5f),4,Menu.TEAL);
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
        if(Menu.button("PLAYTEST / ALL GEAR",x,y+402,280,true))openPlaytestSetup();
        if(Menu.button("TUTORIAL / SAFE PRACTICE",x+300,y+402,320,true))beginTutorial();
        Menu.text(menuMessage.isEmpty()?"Hold right mouse to orbit. ESC pauses.":menuMessage, x, y+462, 15, Menu.MUTED);
    }
    private void drawInventory() { dim();if(equipmentScreen.draw(progress,settings.controls.label(INVENTORY))) inventory=false; }
    private void chooseBlessing(int index) {
        var offers=progress.expedition().offers();if(index<0 || index>=offers.size())return;
        Expedition.Blessing choice=offers.get(index);
        if(progress.expedition().choose(choice)) {
            audio.play("upgrade");state=blessingReturn;notifyPlayer(choice.title.toUpperCase()+" / RANK "+progress.expedition().rank(choice));saveCurrent();
        }
    }
    private void drawBlessings() {
        dim();int x=(GetScreenWidth()-900)/2,y=(GetScreenHeight()-510)/2;
        Menu.panel(x-20,y-20,940,550);Menu.text("CHOOSE YOUR EMBER BLESSING",x+20,y+12,32,Menu.GOLD);
        Menu.text("Shape your expedition / "+progress.expedition().embers()+" ember"+(progress.expedition().embers()==1?"":"s")+" to spend",x+20,y+60,18,Menu.TEAL);
        var offers=progress.expedition().offers();
        for(int i=0;i<offers.size();i++) {
            var b=offers.get(i);int left=x+20+i*292;Menu.panel(left,y+108,272,300);
            Menu.icon(b==Expedition.Blessing.BLOODROOT?"heart":b==Expedition.Blessing.BULWARK?"crown":"sword",left+24,y+128,34,Menu.TEAL);
            Menu.text(b.title,left+22,y+182,25,Menu.GOLD);Menu.text("RANK "+progress.expedition().rank(b)+" > "+(progress.expedition().rank(b)+1)+" / 3",left+22,y+220,16,Menu.MUTED);
            Menu.wrapped(b.description,left+22,y+254,228,18,Menu.TEXT);
            if(Menu.button("["+(i+1)+"] CLAIM",left+20,y+350,232,true)) { chooseBlessing(i);break; }
        }
        Menu.text("Blessings persist with your save. Leave now to decide later.",x+20,y+440,17,Menu.MUTED);
        if(Menu.button("RETURN [ESC]",x+620,y+466,252,true))state=blessingReturn;
    }
    private void drawWorkshop() {
        dim();if(workshopScreen.draw(progress,settings.controls.label(WORKSHOP),audio))workshop=false;
    }
    private void drawOverlay() {
        if(state==GameState.GAME_OVER && outcomeTime<1.4f) {
            int center=GetScreenWidth()/2;Menu.panel(center-235,186,470,74);
            Menu.center(progress.health()<=0?"WARDEN FALLEN":"THE SANCTUARY HAS FALLEN",center,199,27,Menu.RED);
            Menu.center("The ember waits for your return.",center,238,16,Menu.TEXT);return;
        }
        dim(); int x=(GetScreenWidth()-440)/2,y=(GetScreenHeight()-440)/2;
        boolean lost=state==GameState.GAME_OVER;
        Menu.text(lost?(progress.health()<=0?"WARDEN FALLEN":"OUTPOST FALLEN"):"DEFENSE PAUSED",x,y,36,Menu.TEXT);
        Menu.text(lost?"Wave "+wave+" / "+progress.kills()+" defeated":player.getName()+" / Wave "+wave,x,y+54,20,Menu.MUTED);
        if (Menu.button(lost?"TRY AGAIN":"RESUME",x,y+100,440,true)) { if (lost) start(); else state=GameState.PLAYING; }
        if (Menu.button(playtest?"SAVE PLAYTEST":"SAVE GAME",x,y+154,212,!lost && tutorial==null)) saveCurrent();
        if (Menu.button(playtest?"LOAD PLAYTEST":"LOAD GAME",x+228,y+154,212,tutorial==null && Files.exists(playtest?playtestSavePath:savePath)))loadActive();
        if (Menu.button("SETTINGS",x,y+208,212,true)) openSettings();
        if (Menu.button("CHRONICLE",x+228,y+208,212,true)) { journalReturn=state;state=GameState.JOURNAL; }
        if (Menu.button("SAVE & MAIN MENU",x,y+262,440,!lost)) { if (saveCurrent()) state=GameState.MAIN_MENU; }
        if (lost && Menu.button("MAIN MENU",x,y+262,440,true)) state=GameState.MAIN_MENU;
        if(playtest) { if(Menu.button("PLAYTEST TOOLS / CLASS + CHAPTER",x,y+318,440,true))openPlaytestSetup(); }
        else if(!lost && Menu.button(tutorial!=null?"LEAVE PRACTICE":"TUTORIAL / SAFE PRACTICE",x,y+318,440,true)) { if(tutorial!=null)finishTutorial();else beginTutorial(); }
        Menu.text(playtest?"Separate playtest save / Campaign save is preserved.":"Closing the game also saves an active defense.",x,y+380,15,Menu.MUTED);
    }
    private void drawChapterEnd() {
        if(chapter().bossOnly() && outcomeTime<1.6f) {
            int center=GetScreenWidth()/2;
            Menu.panel(center-270,186,540,80);Menu.center(chapter().title().toUpperCase()+" DEFEATED",center,201,27,Menu.GOLD);
            Menu.center("The ember endures. Congratulations, warden!",center,239,16,Menu.TEXT);
            return;
        }
        dim();int x=(GetScreenWidth()-660)/2,y=(GetScreenHeight()-480)/2;
        for(int i=0;i<48;i++) {
            float t=outcomeTime+i*.17f,life=(t%3)/3;
            int cx=(int)((i*149.7f+Math.sin(t*2)*32)%GetScreenWidth()),cy=(int)(GetScreenHeight()*(1-life));
            DrawCircle(cx,cy,2+i%3,Fade(i%2==0?Menu.GOLD:Menu.TEAL,(1-life)*.65f));
        }
        Menu.panel(x-24,y-24,708,510);
        Menu.icon("crown",x,y,32,Menu.GOLD);
        Menu.text(state==GameState.VICTORY?"THE VALLEY SEES DAWN":"CONGRATULATIONS, WARDEN!",x+48,y,30,Menu.GOLD);
        Menu.text(chapter().title(),x,y+60,25,Menu.TEXT);
        Menu.wrapped(chapter().aftermath(),x,y+107,640,20,Menu.TEXT);
        if(state!=GameState.VICTORY) {
            Campaign.Chapter next=Campaign.CHAPTERS[Math.min(3,chapterIndex()+1)];
            Menu.text("NEXT / "+next.title()+(next.bossOnly()?" / BOSS DUEL":" / "+next.waves()+" WAVES"),x,y+205,21,Menu.TEAL);
            Menu.wrapped(next.story(),x,y+244,640,18,Menu.MUTED);
        } else Menu.text(progress.kills()+" enemies defeated / The sanctuary endures.",x,y+230,19,Menu.TEAL);
        Menu.text(chapter().bossOnly()?"BOSS DEFEATED / "+progress.weaponName(progress.weaponUnlocked())+" unlocked":"CHAPTER COMPLETE / The ember burns brighter.",x,y+310,17,Menu.TEAL);
        if(Menu.button(state==GameState.VICTORY?"MAIN MENU [ENTER]":"CONTINUE [ENTER]",x,y+350,380,true)) continueCampaign();
        if(state!=GameState.VICTORY && Menu.button("INVENTORY",x+400,y+350,240,true)) inventory=true;
        Menu.text(playtest?"Your playtest has been saved separately.":"Your campaign progress has been saved.",x,y+424,16,Menu.MUTED);
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
        playtest=false;
        nameDraft=settings.playerName.equals("Warden")?"":settings.playerName;
        menuMessage=""; state=GameState.LOGIN;
        while (GetCharPressed()!=0) { }
    }
    private void confirmName() {
        settings.playerName=Settings.cleanName(nameDraft);settings.difficulty=selectedDifficulty; persistSettings(); start();
        if(pointerAim && !settings.tutorialSeen)beginTutorial();
    }
    void startPlaytest() { playtest=true;start(); }
    boolean isPlaytest() { return playtest; }
    private void openPlaytestSetup() { playtestReturn=state;state=GameState.PLAYTEST_SETUP; }
    private void drawPlaytestSetup() {
        dim();int x=(GetScreenWidth()-800)/2,y=(GetScreenHeight()-600)/2;
        Menu.panel(x,y,800,600);Menu.text("PLAYTEST / EVERYTHING UNLOCKED",x+24,y+20,30,Menu.GOLD);
        Menu.text("100,000 crystals / all gear / 5 potions / separate save",x+24,y+69,18,Menu.TEAL);
        Menu.text("CLASS",x+24,y+93,12,Menu.MUTED);
        for(HeroClass hero:HeroClass.values())if(Menu.button((selectedClass==hero?"> ":"")+hero.label,x+24+hero.ordinal()*256,y+112,240,true))selectedClass=hero;
        Menu.text("DIFFICULTY",x+24,y+181,12,Menu.MUTED);
        for(Difficulty d:Difficulty.values())if(Menu.button((selectedDifficulty==d?"> ":"")+d.label,x+24+d.ordinal()*194,y+202,176,true))selectedDifficulty=d;
        Menu.text("JUMP TO CHAPTER",x+24,y+270,12,Menu.MUTED);
        for(int i=0;i<4;i++)if(Menu.button((playtestChapter==i?"> ":"")+(i+1)+" / "+Campaign.CHAPTERS[i].title(),x+24+(i%2)*388,y+292+(i/2)*56,364,true))playtestChapter=i;
        if(Menu.button(maxPlaytestTraining?"TRAINING / MAXED":"TRAINING / STARTER STATS",x+24,y+420,364,true))maxPlaytestTraining=!maxPlaytestTraining;
        if(Menu.button("TIME / "+TIME_LABELS[playtestTime],x+412,y+420,364,true))playtestTime=(playtestTime+1)%4;
        if(Menu.button("START / RESET PLAYTEST [ENTER]",x+24,y+490,364,true))startPlaytest();
        if(Menu.button("BACK [ESC]",x+412,y+490,176,true))state=playtestReturn;
        if(Menu.button("LOAD PLAYTEST",x+608,y+490,168,Files.exists(playtestSavePath)))loadFrom(playtestSavePath,true);
        Menu.text("Tab changes equipment. Pause > Playtest Tools switches class/chapter or resets the kit.",x+24,y+556,15,Menu.MUTED);
    }
    private void drawNameEntry() {
        dim();int x=(GetScreenWidth()-940)/2,y=(GetScreenHeight()-600)/2;
        Menu.panel(x,y,940,600);
        Color accent=selectedClass==HeroClass.WARRIOR?Menu.GOLD:selectedClass==HeroClass.ARCHER?Menu.TEAL:Menu.color(182,149,237);
        heroPortrait.draw(x+610,y+65,306,408);
        Menu.text("THE LAST EMBER",x+24,y+22,16,accent);
        Menu.text("CHOOSE YOUR WARDEN",x+24,y+53,35,Menu.TEXT);
        Menu.text("A valley without dawn is waiting for your light.",x+24,y+102,18,Menu.MUTED);
        Menu.panel(x+24,y+137,560,58);
        Menu.text(nameDraft.isEmpty()?"Your legend starts with a name":nameDraft+((int)(GetTime()*2)%2==0?"_":""),x+40,y+153,24,nameDraft.isEmpty()?Menu.MUTED:Menu.TEXT);
        for(HeroClass hero:HeroClass.values())if(Menu.button((selectedClass==hero?"> ":"")+hero.label,x+24+hero.ordinal()*190,y+216,180,true))selectedClass=hero;
        Menu.text(switch(selectedClass) { case WARRIOR -> "Stand against the dark. Make every swing count.";case ARCHER -> "Find the opening. Let the next arrow bring dawn.";case MAGE -> "Shape the ember. Turn the cold into light.";},x+24,y+278,18,accent);
        Menu.text("CHOOSE YOUR CHALLENGE",x+24,y+326,13,Menu.GOLD);
        for(Difficulty choice:Difficulty.values())if(Menu.button((choice==selectedDifficulty?"> ":"")+choice.label,x+24+choice.ordinal()*142,y+350,134,true))selectedDifficulty=choice;
        Menu.wrapped(selectedDifficulty.description,x+24,y+411,548,17,Menu.MUTED);
        if(Menu.button("LIGHT THE BEACON [ENTER]",x+24,y+482,286,true))confirmName();
        if(Menu.button("CLEAR NAME",x+326,y+482,118,true))nameDraft="";
        if(Menu.button("BACK",x+462,y+482,122,true))state=GameState.MAIN_MENU;
        Menu.center(selectedClass.label.toUpperCase(),x+763,y+489,28,accent);
        Menu.center(selectedClass.health+" HEALTH / "+(selectedClass==HeroClass.WARRIOR?"CLOSE COMBAT":selectedClass==HeroClass.ARCHER?"PRECISION":"ARCANE POWER"),x+763,y+530,13,Menu.MUTED);
        Menu.text("One ember. One last watch. Your story begins here.",x+24,y+556,17,accent);
        for(int i=0;i<9;i++) {
            float rise=((float)GetTime()*.18f+i*.13f)%1;
            DrawCircle(x+630+i*29+(int)(Math.sin(GetTime()+i)*5),y+460-(int)(rise*365),1.5f,Fade(accent,(1-rise)*.6f));
        }
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
        if(tutorial!=null) { notifyPlayer("Practice stays separate / Your campaign save is safe.");return false; }
        if (progress.defeated()) return false;
        try {
            SaveGame.write(playtest?playtestSavePath:savePath,new SaveGame.Snapshot(player.getName(),player.getPosition().x(),player.getPosition().z(),player.getRotation().y(),progress,director.snapshot(),enemies.stream().filter(e->e.health>0).toList(),Arena.HIGHLANDS,player.discipline().stamina(),dayNight.phase()));
            menuMessage=playtest?"Playtest saved separately.":"Game saved."; notifyPlayer(menuMessage); return true;
        } catch (IOException e) { menuMessage="Save failed. Previous save was kept.";notifyPlayer(menuMessage);System.err.println(e.getMessage());return false; }
    }
    private boolean loadActive() { return loadFrom(playtest?playtestSavePath:savePath,playtest); }
    boolean loadCurrent() { return loadFrom(savePath,false); }
    private boolean loadFrom(Path path,boolean sandbox) {
        try {
            SaveGame.Snapshot saved=SaveGame.read(path);
            restoreSnapshot(saved,sandbox);menuMessage="Game loaded. Press Enter to resume.";return true;
        } catch (IOException e) { menuMessage="Could not load game. Current defense was kept.";notifyPlayer(menuMessage);System.err.println(e.getMessage());return false; }
    }
    private void restoreSnapshot(SaveGame.Snapshot saved,boolean sandbox) {
            playtest=sandbox;
            progress=saved.progression();selectedDifficulty=progress.difficulty();director.difficulty(progress.difficulty());director.restore(saved.wave());fallen.clear();outcomeTime=0;enemies.clear();enemies.addAll(saved.enemies());projectiles.clear();heroShots.clear();inventory=false;
            selectedClass=progress.heroClass();
            if(playtest) { playtestChapter=director.chapterIndex();maxPlaytestTraining=progress.level(Progression.Upgrade.VITALITY)==10; }
            applyHeroModel();
            player.setName(saved.name());settings.playerName=saved.name();player.getPosition().x(saved.x()).z(saved.z());player.getRotation().y(saved.facing());
            applyProgression();player.resetCombat();player.discipline().stamina(saved.stamina());dayNight.phase(saved.dayPhase());lastHeroX=saved.x();lastHeroZ=saved.z();heavyCharge=0;bossHazards.clear();feedback.clear();vfx.clear();wave=director.wave();nextWave=director.rest();workshop=false;
            combo=0;comboWindow=inputBuffer=attackElapsed=hurtFlash=slashTime=noticeTime=0;attackCooldown=.3f;impactPending=false;
            sceneManager.getOpenScene().getCameraController().update(0);
            state=director.levelCleared(enemies.size())?(wave>=10?GameState.VICTORY:GameState.LEVEL_COMPLETE):GameState.PAUSED;
            if(state==GameState.LEVEL_COMPLETE || state==GameState.VICTORY)outcomeTime=3;

        hudCharging=false;mastery.reset();hitPause=hitMarker=0;
    }
    WeaponTrail weaponTrail() { return vfx.blade(); }
    Combat.Attack currentAttack() { return activeAttack; }
    java.util.List<BossHazard> getBossHazards() { return java.util.Collections.unmodifiableList(bossHazards); }
    boolean weaponMaterialsValid() { return assets.weaponMaterialsValid(); }
    boolean shadowAtlasValid() { return renderer.shadowAtlasValid(); }
    java.util.List<Enemy> getEnemies() { return java.util.Collections.unmodifiableList(enemies); }
    Progression getProgression() { return progress; }
    GameAudio audio() { return audio; }
    boolean isWorkshopOpen() { return workshop; }
    boolean isInventoryOpen() { return inventory; }
    int fallenCount() { return fallen.size(); }
    public boolean shouldQuit() { return quitRequested; }
    public void dispose() { persistSettings(); if (state==GameState.RELIC_CHOICE||state==GameState.PLAYING||state==GameState.PAUSED||state==GameState.LEVEL_COMPLETE||state==GameState.VICTORY||state==GameState.JOURNAL&&journalReturn==GameState.PAUSED||state==GameState.SETTING_MENU&&settingsReturn==GameState.PAUSED||state==GameState.PLAYTEST_SETUP&&playtestReturn==GameState.PAUSED) saveCurrent(); audio.close();vfx.close(); assets.close();renderer.close();equipmentScreen.close();heroPortrait.close(); player.dispose(); Menu.close(); }
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
