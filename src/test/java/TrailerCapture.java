import Entities.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bytedeco.javacpp.BytePointer;
import static com.raylib.Raylib.*;

/** Films actual game simulation directly from its framebuffer, never the user's desktop. */
public final class TrailerCapture {
    private static final int WIDTH=1600,HEIGHT=900,FPS=24;
    private static final Set<Integer> held=new HashSet<>(),taps=new HashSet<>();
    private static final AtomicBoolean attack=new AtomicBoolean();
    private static Path profile;
    private static int activeFrame;
    private static PrintWriter cues;
    record Shot(String id,int seconds,HeroClass hero,int wave,String mode,String title,String subtitle,String music) { }
    private static final Shot[] SHOTS={
        new Shot("01-ember",8,HeroClass.WARRIOR,0,"scenic","OUTPOST","Guard the last light. Bring back the dawn.","title"),
        new Shot("02-valley",25,HeroClass.WARRIOR,1,"scenic","ONE VALLEY. ONE LAST FIRE.","A living sky above a sanctuary worth defending.","meadow"),
        new Shot("03-warrior",30,HeroClass.WARRIOR,3,"combat","COMMIT TO EVERY STRIKE","Chain your attacks. Watch your stamina. Read the enemy.","battle"),
        new Shot("04-counter",18,HeroClass.WARRIOR,5,"counter","TURN DEFENSE INTO OPPORTUNITY","Gold attacks can be parried. Unblockable attacks demand a dodge.","boss"),
        new Shot("05-archer",25,HeroClass.ARCHER,6,"combat","HOLD YOUR DISTANCE","Bow, crossbow, reposition, reload. Make every opening count.","frost"),
        new Shot("06-mage",25,HeroClass.MAGE,8,"combat","LET THE EMBER ANSWER","Piercing magic and charged surges against the dead.","necropolis"),
        new Shot("07-supplies",16,HeroClass.ARCHER,1,"supplies","LEAVE THE SAFETY OF THE FIRE","Recover supplies before the opportunity is lost.","meadow"),
        new Shot("08-blessings",18,HeroClass.WARRIOR,0,"blessings","SHAPE YOUR EXPEDITION","Earn ember blessings. Choose the strengths your build needs.","title"),
        new Shot("09-survivor",25,HeroClass.WARRIOR,2,"survivor","SOMEONE IS STILL OUT THERE","Find a survivor. Stay close. Bring them home alive.","cemetery"),
        new Shot("10-waystone",25,HeroClass.MAGE,3,"beacon","DEFEND MORE THAN YOURSELF","Wake a waystone, draw the enemy, and hold until it answers.","battle"),
        new Shot("11-equipment",18,HeroClass.ARCHER,0,"equipment","MAKE THE WARDEN YOUR OWN","Visible weapons, fitted armor, and a build you can change.","title"),
        new Shot("12-pass",15,HeroClass.MAGE,6,"scenic","FOUR CHAPTERS. A ROAD TO DAWN.","Meadow, cemetery, frozen pass, and the Hollow Necropolis.","frost"),
        new Shot("13-warden",25,HeroClass.WARRIOR,5,"boss","THE ASH WARDEN","Learn the cleave, evade the slam, and survive the rush.","boss"),
        new Shot("14-king",25,HeroClass.MAGE,10,"boss","THE HOLLOW KING","Enrage changes the rhythm. Soul sigils change the battlefield.","enrage"),
        new Shot("15-dawn",12,HeroClass.WARRIOR,10,"victory","BRING BACK THE DAWN","The final blow. The fallen king. The valley remembers warmth.","boss"),
        new Shot("16-return",15,HeroClass.WARRIOR,0,"ending","OUTPOST","Try the latest development build and the all-gear playtest.","title")
    };
    private static void event(int type,int a,int b) {
        try(AutomationEvent e=new AutomationEvent().type(type)) { e.params(0,a).params(1,b);PlayAutomationEvent(e); }
    }
    private static void hold(int key,boolean down) { if(down?held.add(key):held.remove(key))event(down?2:1,key,0); }
    private static void tap(int key) { hold(key,true);taps.add(key); }
    private static void release() { for(int key:new ArrayList<>(held))hold(key,false);taps.clear();attack.set(false); }
    private static void steer(Game game,float tx,float tz,boolean sprint) {
        Player player=game.getPlayer();Camera3D c=game.getSceneManager().getOpenScene().getCameraController().getCamera();
        float fx=c.target().x()-c._position().x(),fz=c.target().z()-c._position().z(),len=(float)Math.hypot(fx,fz);fx/=len;fz/=len;
        float dx=tx-player.getPosition().x(),dz=tz-player.getPosition().z(),d=(float)Math.hypot(dx,dz);
        float forward=dx*fx+dz*fz,right=-dx*fz+dz*fx;
        hold(KEY_W,d>.8f && forward>.55f);hold(KEY_S,d>.8f && forward<-.55f);
        hold(KEY_D,d>.8f && right>.55f);hold(KEY_A,d>.8f && right<-.55f);hold(KEY_LEFT_SHIFT,sprint && d>6);
    }
    private static void setup(Game game,Shot shot) throws Exception {
        release();game.photoMode(false);
        Progression p=Progression.playtest(shot.hero,Difficulty.NORMAL,false);
        // All equipment/training shown is obtainable in the shipped all-gear sandbox.
        for(int i=0;i<5;i++)p.purchase(Progression.Upgrade.VITALITY);
        for(int i=0;i<9;i++)p.purchase(Progression.Upgrade.BASE);
        if(shot.mode.equals("beacon"))for(int i=0;i<2;i++)p.purchase(Progression.Upgrade.POWER);
        if(shot.mode.equals("boss") || shot.mode.equals("counter"))p.equipWeapon(0);
        else if(shot.mode.equals("victory"))p.equipWeapon(3);
        else p.equipWeapon(shot.hero==HeroClass.ARCHER?2:3);
        ArrayList<Enemy> enemies=new ArrayList<>();
        boolean fight=Set.of("combat","boss","counter","victory","survivor","beacon","supplies").contains(shot.mode);
        if(fight) {
            if(Campaign.chapter(shot.wave).bossOnly()) {
                Enemy boss=new Enemy(1,11,shot.wave,5,.5f);if(shot.mode.equals("victory"))boss.health=80;
                if(shot.mode.equals("counter"))boss.health=boss.maxHealth*.55f;enemies.add(boss);
            } else {
                int count=shot.mode.equals("combat")?6:shot.mode.equals("beacon")?5:3;
                for(int i=0;i<count;i++) {
                    double a=i*Math.PI*2/count;
                    float x=(float)Math.sin(a)*(shot.mode.equals("combat")?11:15),z=(float)Math.cos(a)*11;
                    enemies.add(new Enemy(x,z,shot.wave,WaveFormation.variant(shot.wave,i),.2f+(i%6)*.12f));
                }
            }
        }
        float px=0,pz=5;
        if(Set.of("survivor","beacon","supplies").contains(shot.mode)) {
            p.fieldEvent().begin(shot.wave);px=p.fieldEvent().x()+1;pz=p.fieldEvent().z()+1;
            for(int i=0;i<enemies.size();i++) { enemies.get(i).x=p.fieldEvent().x()-7+i*3;enemies.get(i).z=p.fieldEvent().z()-10; }
        }
        if(shot.mode.equals("blessings")) { p.expedition().waveCleared(1);p.expedition().waveCleared(2); }
        int total=enemies.size();WaveDirector.State waves=new WaveDirector.State(shot.wave,0,total,8,0,21,0);
        SaveGame.write(profile.resolve("save.properties"),new SaveGame.Snapshot("Ember",px,pz,0,p,waves,enemies,Arena.HIGHLANDS,100,DayNightCycle.initial(Campaign.index(shot.wave))));
        if(!game.loadCurrent())throw new IllegalStateException("Trailer setup did not load: "+shot.id);
        game.setState(GameState.PLAYING);game.audio().configure(0,false);
        if(!fight)game.setState(GameState.PAUSED);
        if(shot.mode.equals("blessings"))game.setState(GameState.PLAYING);
        camera(game,shot,0);game.photoMode(Set.of("scenic","ending","boss","counter","victory").contains(shot.mode));
    }
    private static void direct(Game game,Shot shot,int frame,int frames) {
        for(int key:new ArrayList<>(taps))hold(key,false);taps.clear();
        for(int key=32;key<=348;key++) {
            if(IsKeyDown(key)&&!held.contains(key))event(1,key,0);
            else if(!IsKeyDown(key)&&held.contains(key))event(2,key,0);
        }
        float t=frame/(float)FPS;
        if(shot.mode.equals("scenic") || shot.mode.equals("ending")) {
            game.dayPhase(shot.id.startsWith("02")?.30f+.45f*frame/frames:shot.id.startsWith("12")?.43f+.48f*frame/frames:.36f);
            return;
        }
        if(shot.mode.equals("blessings")) {
            if(frame==FPS/2 || frame==FPS*10)tap(KEY_G);
            if(frame==FPS*7)tap(KEY_TWO);if(frame==FPS*15)tap(KEY_ONE);return;
        }
        if(shot.mode.equals("equipment")) {
            if(frame==0) { game.setState(GameState.PLAYING);tap(KEY_TAB); }
            int x=(WIDTH-940)/2,y=(HEIGHT-600)/2;
            if(frame==FPS*4 || frame==FPS*10) {
                int item=frame==FPS*4?1:8;event(7,x+400+(item%6)*75+30,y+90+(item/6)*76+30);event(6,0,0);
            }
            if(frame==FPS*4+1 || frame==FPS*10+1) { event(5,0,0);tap(KEY_ENTER); }
            return;
        }
        if(game.getState()==GameState.LEVEL_COMPLETE && !shot.mode.equals("victory")) {
            // Hold genuine results briefly; subsequent footage shows the real aftermath.
            release();return;
        }
        if(game.getState()!=GameState.PLAYING)return;
        Player player=game.getPlayer();float px=player.getPosition().x(),pz=player.getPosition().z();
        FieldEvent field=game.getProgression().fieldEvent();
        boolean objective=Set.of("survivor","beacon","supplies").contains(shot.mode) && field.visible();
        if(objective) {
            if(field.status()==FieldEvent.Status.WAITING || shot.mode.equals("supplies")) {
                steer(game,field.x(),field.z(),false);hold(KEY_G,true);
                if(shot.mode.equals("supplies"))return;
            } else if(shot.mode.equals("survivor")) {
                float d=Math.max(.1f,(float)Math.hypot(field.x(),field.z()));steer(game,field.x()-field.x()/d*3,field.z()-field.z()/d*3,false);
            }
        } else hold(KEY_G,false);
        Enemy nearest=null;float distance=Float.MAX_VALUE,bestScore=Float.MAX_VALUE;
        for(Enemy e:game.getEnemies())if(e.health>0) {
            float score=objective && shot.mode.equals("beacon")?e.distance(field.x(),field.z())-(e.variant==2?2:0):e.distance(px,pz);
            if(score<bestScore) { nearest=e;distance=e.distance(px,pz);bestScore=score; }
        }
        if(nearest==null) { if(!objective)steer(game,0,4,false);return; }
        float desired=shot.hero==HeroClass.WARRIOR?1.7f:shot.hero==HeroClass.ARCHER?5:5.5f;
        if(!objective || shot.mode.equals("beacon")) {
            float dx=nearest.x-px,dz=nearest.z-pz,len=Math.max(.1f,distance);
            float tx=distance>desired?nearest.x-dx/len*desired:px-dx/len*(desired-distance),tz=distance>desired?nearest.z-dz/len*desired:pz-dz/len*(desired-distance);
            if(objective && shot.mode.equals("beacon") && Math.hypot(tx-field.x(),tz-field.z())>4) { tx=field.x();tz=field.z(); }
            steer(game,tx,tz,distance>9);
        }
        player.aimAt(nearest.x,nearest.z);
        // Give ranged movement a real opening instead of filming repeated attacks
        // while the director lets melee enemies crowd the hero at the sanctuary.
        if(shot.mode.equals("combat") && shot.hero!=HeroClass.WARRIOR && distance<desired*.72f) {
            hold(KEY_Q,false);
            if(frame%30==0)tap(KEY_LEFT_CONTROL);
            return;
        }
        if(game.getProgression().health()<game.getProgression().maxHealth()*.52f && frame%24==0)tap(KEY_H);
        boolean danger=nearest.attackTime>.15f && nearest.attackTime<.42f && distance<nearest.meleeRange()+1;
        if(danger) {
            if(!nearest.boss() || nearest.bossMove.parryable) { if(frame%5==0)tap(KEY_F); }
            else if(frame%8==0)tap(KEY_LEFT_CONTROL);
        }
        if(shot.mode.equals("counter")) { if(!danger && frame%38==0)attack.set(true);return; }
        if(distance<(shot.hero==HeroClass.WARRIOR?2.8f:9)) {
            int cycle=frame%(FPS*4);
            boolean heavy=cycle<FPS*.8f && !player.isAttacking();hold(KEY_Q,heavy);
            if(!heavy && frame%14==0)attack.set(true);
            if(shot.hero==HeroClass.WARRIOR && distance>2.4f && frame%(FPS*5)==0)tap(KEY_R);
        } else hold(KEY_Q,false);
    }
    private static void camera(Game game,Shot shot,float t) {
        Camera3D c=game.getSceneManager().getOpenScene().getCameraController().getCamera();
        boolean scenic=shot.mode.equals("scenic") || shot.mode.equals("ending");
        if(scenic) {
            float angle=-.7f+t*.026f,r=shot.id.startsWith("12")?38:31;
            c._position().x((float)Math.sin(angle)*r).y(10+(float)Math.sin(t*.08f)*2).z((float)Math.cos(angle)*r);
            c.target().x(0).y(1.6f).z(0);
        } else {
            boolean duel=shot.mode.equals("boss") || shot.mode.equals("counter") || shot.mode.equals("victory");
            float px=game.getPlayer().getPosition().x(),pz=game.getPlayer().getPosition().z(),angle=(duel?-2.65f:.38f)+(float)Math.sin(t*.15f)*.10f;
            c._position().x(px+(float)Math.sin(angle)*12).y(8.2f).z(pz+(float)Math.cos(angle)*12);c.target().x(px*.65f).y(.8f).z(pz*.65f);
        }
    }
    public static void main(String[] args) throws Exception {
        boolean preview=Arrays.asList(args).contains("--preview");int from=0;Set<Integer> only=new HashSet<>();
        for(String arg:args) {
            if(arg.startsWith("--from="))from=Integer.parseInt(arg.substring(7));
            if(arg.startsWith("--only="))for(String id:arg.substring(7).split(","))only.add(Integer.parseInt(id));
        }
        Path out=Path.of(preview?"build/marketing-preview":"build/marketing").toAbsolutePath();Files.createDirectories(out.resolve("raw"));
        profile=Files.createTempDirectory(Path.of("build"),"trailer-profile-").toAbsolutePath();System.setProperty("outpost.dataDir",profile.toString());
        Settings settings=new Settings();settings.playerName="Ember";settings.volume=0;settings.sensitivity=0;settings.save(profile.resolve("settings.properties"));
        SetTraceLogLevel(LOG_WARNING);SetConfigFlags(FLAG_MSAA_4X_HINT);InitWindow(WIDTH,HEIGHT,"Outpost | Capturing actual gameplay");SetExitKey(KEY_NULL);SetTargetFPS(0);
        Game game=null;
        try(PrintWriter manifest=new PrintWriter(Files.newBufferedWriter(out.resolve("shots.tsv")))) {
            manifest.println("id\tseconds\ttitle\tsubtitle\tmusic");
            for(Shot shot:SHOTS)manifest.printf(Locale.ROOT,"%s\t%d\t%s\t%s\t%s%n",shot.id,preview?Math.min(3,shot.seconds):shot.seconds,shot.title,shot.subtitle,shot.music);
            manifest.flush();game=new Game(()->attack.getAndSet(false));game.getSceneManager().getOpenScene().getCameraController().setSensitivity(0);
            game.audio().observeCues(name->{if(cues!=null && activeFrame>=0)cues.printf(Locale.ROOT,"%.6f\t%s%n",activeFrame/(float)FPS,name);});
            for(int index=from;index<SHOTS.length;index++) {
                if(!only.isEmpty() && !only.contains(index))continue;
                Shot shot=SHOTS[index];activeFrame=-1;setup(game,shot);int frames=(preview?Math.min(3,shot.seconds):shot.seconds)*FPS;
                Path clip=out.resolve("raw/"+shot.id+".mp4");
                Process encoder=new ProcessBuilder(System.getenv().getOrDefault("OUTPOST_FFMPEG","ffmpeg"),"-hide_banner","-loglevel","error","-y","-f","rawvideo","-pix_fmt","rgba","-s",WIDTH+"x"+HEIGHT,"-r",FPS+"","-i","pipe:0","-an","-vf","scale=1920:1080:flags=lanczos","-c:v","libx264","-preset","veryfast","-crf","19","-pix_fmt","yuv420p","-movflags","+faststart",clip.toString()).redirectError(out.resolve("raw/"+shot.id+"-encoder.log").toFile()).start();
                int defeats=0,playing=0;
                try(OutputStream pixels=new BufferedOutputStream(encoder.getOutputStream(),4*1024*1024);PrintWriter cueFile=new PrintWriter(Files.newBufferedWriter(out.resolve("raw/"+shot.id+"-cues.tsv")))) {
                    cues=cueFile;byte[] buffer=new byte[WIDTH*HEIGHT*4];
                    for(int f=0;f<frames;f++) {
                        if(WindowShouldClose())throw new IOException("Trailer capture window was closed");
                        activeFrame=f;direct(game,shot,f,frames);game.update(1f/FPS);camera(game,shot,f/(float)FPS);
                        BeginDrawing();ClearBackground(Menu.INK);game.render();EndDrawing();
                        Image image=LoadImageFromScreen();
                        try {
                            if(image.width()!=WIDTH || image.height()!=HEIGHT)throw new IOException("Capture dimensions changed");
                            ImageFormat(image,PIXELFORMAT_UNCOMPRESSED_R8G8B8A8);new BytePointer(image.data()).get(buffer);pixels.write(buffer);
                        } finally { UnloadImage(image); }
                        if(game.getState()==GameState.GAME_OVER)defeats++;if(game.getState()==GameState.PLAYING)playing++;
                    }
                    cues=null;
                }
                if(encoder.waitFor()!=0)throw new IOException("Video encoding failed: "+shot.id);
                Files.writeString(out.resolve("raw/"+shot.id+"-quality.txt"),"frames="+frames+"\nplaying="+playing+"\nplayerDefeat="+defeats+"\nendState="+game.getState()+"\nhero="+game.getProgression().heroClass()+"\nobjective="+game.getProgression().fieldEvent().status()+"\nblessings="+game.getProgression().expedition().choices()+"\n");
                System.out.println("CAPTURED "+shot.id+" / "+frames+" frames / player defeat frames "+defeats);release();
            }
        } finally { cues=null;if(game!=null)game.dispose();CloseWindow(); }
    }
}
