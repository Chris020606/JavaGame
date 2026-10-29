package Entities;

import Scene.CameraController;
import Config.Controls;
import static Config.Controls.Action.*;
import static com.raylib.Raylib.*;


public class Player extends Entity {

    private Controls controls=new Controls();
    public void setControls(Controls controls) { this.controls=controls; }
    private String name;
    private int weaponStyle;
    public void setWeaponStyle(int style) { weaponStyle=style; }
    private final CombatDiscipline discipline=new CombatDiscipline();
    private boolean charging,lungeAttack;
    public CombatDiscipline discipline() { return discipline; }
    public void setCharging(boolean value) { charging=value; }
    public boolean invulnerable() { return dodgeTime>.10f && dodgeTime<.31f; }
    public void lunge(float duration) { attack(1,false,duration);lungeAttack=true;if(weaponStyle==0) attackClip=PlayerAnimation.ATTACK_STAB;setAnimation(attackClip); }
    private float velocityX,velocityZ,dodgeTime,dodgeCooldown,dodgeX,dodgeZ;
    private boolean sprinting;
    private PlayerAnimation dodgeClip=PlayerAnimation.DODGE_FORWARD;
    public boolean isDodging() { return dodgeTime>0; }
    public float dodgeCooldown() { return dodgeCooldown; }
    public boolean isSprinting() { return sprinting; }
    private float movementSpeed = 5.0f;
    public void setMovementSpeed(float speed) { movementSpeed=speed; }
    private boolean sex; // true = male, false = female
    private int level;
    private int health = 100;
    private float attackTime, attackDuration=.5f;
    private PlayerAnimation attackClip=PlayerAnimation.ATTACK_CHOP;

    private CameraController cameraController;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isSex() {
        return sex;
    }

    public void setSex(boolean sex) {
        this.sex = sex;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getHealth() {
        return health;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setCameraController(CameraController cameraController) {
        this.cameraController = cameraController;
    }

    public void setAnimation(PlayerAnimation animation) {

        Integer index = getAnimationIndex(animation.getAnimationName());

        if (index == null) {
            // Some imported models omit optional animation clips.
            return;
        }

        setAnimation(index);
    }

    @Override
    public void update(float dt) {
        Vector3 position = getPosition();

        boolean moving = false;

        Camera3D camera = cameraController.getCamera();

        float forwardX = camera.target().x() - camera._position().x();
        float forwardZ = camera.target().z() - camera._position().z();

        float length = (float)Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);

        if (length > 0.0f) {
            forwardX /= length;
            forwardZ /= length;
        }

        float rightX = -forwardZ;
        float rightZ = forwardX;

        float moveX = 0.0f;
        float moveZ = 0.0f;

        if (controls.down(FORWARD)) {
            moveX += forwardX;
            moveZ += forwardZ;
            moving = true;
        }

        if (controls.down(BACK)) {
            moveX -= forwardX;
            moveZ -= forwardZ;
            moving = true;
        }

        if (controls.down(RIGHT)) {
            moveX += rightX;
            moveZ += rightZ;
            moving = true;
        }

        if (controls.down(LEFT)) {
            moveX -= rightX;
            moveZ -= rightZ;
            moving = true;
        }

        float moveLength = (float)Math.sqrt(moveX * moveX + moveZ * moveZ);

        discipline.tick(dt,sprinting);
        dodgeCooldown=Math.max(0,dodgeCooldown-dt);
        if(controls.pressed(PARRY) && !isAttacking() && !isDodging() && !charging) discipline.beginParry();
        if(moveLength>0) { moveX/=moveLength;moveZ/=moveLength; }
        if(controls.pressed(DODGE) && dodgeCooldown<=0 && (!isAttacking() || attackTime<attackDuration*.48f) && discipline.recovery()<=0 && discipline.spend(CombatDiscipline.DODGE)) {
            attackTime=0;charging=false;
            double facing=Math.toRadians(getRotation().y());
            dodgeX=moveLength>0?moveX:(float)Math.sin(facing);
            dodgeZ=moveLength>0?moveZ:(float)Math.cos(facing);
            float forward=dodgeX*(float)Math.sin(facing)+dodgeZ*(float)Math.cos(facing);
            float side=dodgeX*(float)Math.cos(facing)-dodgeZ*(float)Math.sin(facing);
            dodgeClip=forward>.5f?PlayerAnimation.DODGE_FORWARD:forward<-.5f?PlayerAnimation.DODGE_BACKWARD:
                side>0?PlayerAnimation.DODGE_RIGHT:PlayerAnimation.DODGE_LEFT;
            dodgeTime=.34f;dodgeCooldown=1.5f;restartAnimation();
        }
        sprinting=controls.down(SPRINT) && moving && !isAttacking() && !isDodging() && !charging && discipline.recovery()<=0 && discipline.canSprint();
        float speed=movementSpeed*(sprinting?1.55f:1)*(isAttacking()?.3f:charging||discipline.recovery()>0?.15f:1);
        float blend=Math.min(1,dt*18);
        velocityX+=(moveX*speed-velocityX)*blend;
        velocityZ+=(moveZ*speed-velocityZ)*blend;
        if(isDodging()) {
            position.x(position.x()+dodgeX*10*dt).z(position.z()+dodgeZ*10*dt);
            dodgeTime=Math.max(0,dodgeTime-dt);
        } else {
            position.x(position.x()+velocityX*dt).z(position.z()+velocityZ*dt);
            if(moving && !isAttacking()) {
                float target=(float)Math.toDegrees(Math.atan2(moveX,moveZ));
                float delta=(target-getRotation().y()+540)%360-180;
                getRotation().y((float)Math.IEEEremainder(getRotation().y()+Math.max(-720*dt,Math.min(720*dt,delta)),360));
            }
            // Small committed step gives sword swings weight, without changing the hit arc.
            if((weaponStyle==0 || lungeAttack) && attackTime>0) {
                float phase=1-attackTime/attackDuration;
                if(phase>.22f && phase<.5f) {
                    double a=Math.toRadians(getRotation().y());
                    position.x(position.x()+(float)Math.sin(a)*(lungeAttack?8:1.6f)*dt).z(position.z()+(float)Math.cos(a)*(lungeAttack?8:1.6f)*dt);
                }
            }
        }
        position.x(Math.max(-22,Math.min(22,position.x()))).z(Math.max(-22,Math.min(22,position.z())));
        setPosition(position);

        attackTime = Math.max(0, attackTime - dt);
        if (isDodging()) {
            setAnimation(dodgeClip);
        } else if (attackTime > 0) {
            setAnimation(weaponStyle==1 && 1-attackTime/attackDuration<.28f?PlayerAnimation.BOW_DRAW:attackClip);
        } else if(discipline.recovery()>0 || charging) {
            setAnimation(PlayerAnimation.BLOCK_IDLE);
        } else if (moving) {
            setAnimation(weaponStyle==1?PlayerAnimation.BOW_RUN:sprinting?PlayerAnimation.RUN:PlayerAnimation.WALK);
        } else {
            setAnimation(weaponStyle==1?PlayerAnimation.BOW_IDLE:PlayerAnimation.MELEE_IDLE);
        }

        if(isDodging()) playAnimationProgress(1-dodgeTime/.34f);
        else if (attackTime > 0) {
            float phase=1-attackTime/attackDuration;
            // Deliberate windup, fast contact, and a full recovery pose.
            float pose=phase<.36f?phase/.36f*.42f:phase<.58f?.42f+(phase-.36f)/.22f*.30f:.72f+(phase-.58f)/.42f*.28f;
            playAnimationProgress(weaponStyle==0?pose:weaponStyle==1?(phase<.28f?phase/.28f:(phase-.28f)/.72f):phase);
        }
        else updateAnimation(dt);
    }

    public void attack(boolean hasWeapon) { attack(1,false,.5f); }

    public void attack(int combo,boolean heavy,float duration) {
        lungeAttack=false;charging=false;attackDuration=duration; attackTime=duration;
        attackClip=heavy ? PlayerAnimation.ATTACK_JUMP_CHOP : combo==3 ? PlayerAnimation.ATTACK_CHOP : combo==2 ? PlayerAnimation.ATTACK_DIAGONAL : PlayerAnimation.ATTACK_HORIZONTAL;
        if (weaponStyle==1) attackClip=PlayerAnimation.BOW_RELEASE;
        if (weaponStyle==2) attackClip=PlayerAnimation.MAGIC_CAST;
        setAnimation(attackClip);
        restartAnimation(); playAnimationProgress(0);
    }

    public float attackRemaining() { return attackTime; }
    public boolean isAttacking() { return attackTime > 0; }

    public void resetCombat() {
        attackTime = dodgeTime = dodgeCooldown = velocityX = velocityZ = 0; sprinting=charging=lungeAttack=false;discipline.reset();
        setAnimation(weaponStyle==1?PlayerAnimation.BOW_IDLE:PlayerAnimation.MELEE_IDLE);
        restartAnimation();
        updateAnimation(0);
    }

    public void movement(float dt) {

    }

}
