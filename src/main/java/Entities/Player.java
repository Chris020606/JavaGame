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
    private boolean crossbow;
    private boolean twoHanded;
    public void setWeaponTier(int tier) { twoHanded=weaponStyle==0 && tier>0; }
    public void setCrossbow(boolean value) { crossbow=value; }
    private PlayerAnimation rangedIdle() { return crossbow?PlayerAnimation.CROSSBOW_IDLE:PlayerAnimation.BOW_IDLE; }
    public void setWeaponStyle(int style) { weaponStyle=style; }
    private final CombatDiscipline discipline=new CombatDiscipline();
    private boolean charging,lungeAttack;
    public CombatDiscipline discipline() { return discipline; }
    private float chargePoseTime,aimLock,locomotionClock;
    private float lastOutcomeTime;
    private float visualLift;
    private boolean fallenPose;
    public Vector3 renderPosition() { return new Vector3().x(getPosition().x()).y(getPosition().y()+visualLift).z(getPosition().z()); }
    public void setCharging(boolean value) { if(value&&!charging)chargePoseTime=0;charging=value; }
    public void aimAt(float x,float z) {
        if(!isAttacking() && !isDodging() && Math.hypot(x-getPosition().x(),z-getPosition().z())>.2f) {
            getRotation().y((float)Math.toDegrees(Math.atan2(x-getPosition().x(),z-getPosition().z())));aimLock=.2f;
        }
    }
    private boolean requestedParry;
    public void requestParry() { requestedParry=true; }
    public void parry() { if(!isAttacking() && !isDodging() && !charging)discipline.beginParry(); }
    public boolean invulnerable() { return dodgeTime>.10f && dodgeTime<.31f; }
    public void lunge(float duration) { lunge(duration,.25f); }
    public void lunge(float duration,float impact) {
        attack(1,false,duration,impact,0);lungeAttack=true;
        if(weaponStyle==0) { attackClip=twoHanded?PlayerAnimation.TWO_HANDED_STAB:PlayerAnimation.ATTACK_STAB;setAnimation(attackClip);restartAnimation();playAnimationProgress(0); }
    }
    private float velocityX,velocityZ,dodgeTime,dodgeCooldown,dodgeX,dodgeZ;
    private float dodgeRecovery=1.5f;
    public void setDodgeRecovery(float seconds) { dodgeRecovery=seconds; }
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
    private float attackTime, attackDuration=.5f,attackImpact=.18f,attackStartPose;
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
            throw new IllegalStateException("Missing required player animation: "+animation.getAnimationName());
        }

        setAnimation(index);
    }

    @Override
    public void update(float dt) {
        Vector3 position = getPosition();
        aimLock=Math.max(0,aimLock-dt);locomotionClock+=dt;
        if(charging)chargePoseTime+=dt;

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
        if(controls.pressed(PARRY) || requestedParry)parry();
        requestedParry=false;
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
            dodgeTime=.34f;dodgeCooldown=dodgeRecovery;restartAnimation();
        }
        sprinting=controls.down(SPRINT) && moving && !isAttacking() && !isDodging() && !charging && discipline.recovery()<=0 && discipline.canSprint();
        float recover=AttackMotion.recovery(attackDuration-attackTime,attackImpact,attackDuration);
        if(weaponStyle==1 && crossbow && isAttacking())recover=AttackMotion.smooth((1-attackTime/attackDuration-.88f)/.12f);
        float speed=movementSpeed*(sprinting?1.55f:1)*(isAttacking()?recover:charging||discipline.recovery()>0?.18f:1);
        if(isAttacking() && recover<=0)velocityX=velocityZ=0;
        float blend=Math.min(1,dt*18);
        velocityX+=(moveX*speed-velocityX)*blend;
        velocityZ+=(moveZ*speed-velocityZ)*blend;
        if(isDodging()) {
            position.x(position.x()+dodgeX*10*dt).z(position.z()+dodgeZ*10*dt);
            dodgeTime=Math.max(0,dodgeTime-dt);
        } else {
            position.x(position.x()+velocityX*dt).z(position.z()+velocityZ*dt);
            if(moving && !isAttacking() && !charging && aimLock<=0) {
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
        PlayerAnimation locomotion=locomotion(moveX,moveZ,sprinting);
        if (isDodging()) {
            setAnimation(dodgeClip);
        } else if (attackTime > 0) {
            poseAttack(dt,moving?locomotion:null);return;
        } else if(charging) {
            setAnimation(weaponStyle==1?(crossbow?PlayerAnimation.CROSSBOW_IDLE:PlayerAnimation.BOW_DRAW):weaponStyle==2?PlayerAnimation.MAGIC_RAISE:twoHanded?PlayerAnimation.TWO_HANDED_CHOP:PlayerAnimation.ATTACK_CHOP);
            advancePose(dt);playAnimationProgress(weaponStyle==1?(crossbow?0:AttackMotion.smooth(chargePoseTime/.7f)):weaponStyle==2?AttackMotion.smooth(chargePoseTime/.65f):.28f*AttackMotion.smooth(chargePoseTime/.7f));return;
        } else if(discipline.recovery()>0) {
            setAnimation(weaponStyle==0?PlayerAnimation.BLOCK_IDLE:weaponStyle==1?rangedIdle():PlayerAnimation.MAGIC_RAISE);
        } else if (moving) {
            setAnimation(locomotion);
        } else {
            setAnimation(weaponStyle==1?rangedIdle():weaponStyle==2?PlayerAnimation.MAGIC_RAISE:PlayerAnimation.MELEE_IDLE);
        }

        if(isDodging()) { advancePose(dt);playAnimationProgress(1-dodgeTime/.34f); }
        else if(!moving && weaponStyle==2) { advancePose(dt);playAnimationProgress(0); }
        else updateAnimation(dt*(sprinting?1.35f:1));
    }

    private PlayerAnimation locomotion(float dx,float dz,boolean sprint) {
        double angle=Math.toRadians(getRotation().y());float forward=dx*(float)Math.sin(angle)+dz*(float)Math.cos(angle),side=dx*(float)Math.cos(angle)-dz*(float)Math.sin(angle);
        if(forward<-.55f)return PlayerAnimation.WALK_BACKWARD;
        if(Math.abs(side)>.7f)return side>0?PlayerAnimation.RUN_STRAFE_RIGHT:PlayerAnimation.RUN_STRAFE_LEFT;
        return weaponStyle==1&&!crossbow?PlayerAnimation.BOW_RUN:PlayerAnimation.RUN;
    }
    private void poseAttack(float dt,PlayerAnimation moving) {
        float elapsed=attackDuration-attackTime,after=Math.max(0,elapsed-attackImpact),progress;
        PlayerAnimation clip=attackClip;
        if(weaponStyle==1 && crossbow) {
            if(elapsed<attackImpact) { clip=PlayerAnimation.CROSSBOW_IDLE;progress=0; }
            else if(after<.13f) { clip=PlayerAnimation.CROSSBOW_SHOOT;progress=after/.13f*.55f; }
            else { clip=PlayerAnimation.CROSSBOW_RELOAD;progress=(after-.13f)/Math.max(.1f,attackDuration-attackImpact-.13f); }
        } else if(weaponStyle==1) {
            clip=elapsed<attackImpact?PlayerAnimation.BOW_DRAW:PlayerAnimation.BOW_RELEASE;
            progress=elapsed<attackImpact?AttackMotion.contact(elapsed,attackImpact,attackDuration,1,attackStartPose):after/Math.max(.1f,attackDuration-attackImpact)*.45f;
        } else if(weaponStyle==2) {
            clip=elapsed<attackImpact?PlayerAnimation.MAGIC_RAISE:PlayerAnimation.MAGIC_SHOOT;
            progress=elapsed<attackImpact?AttackMotion.contact(elapsed,attackImpact,attackDuration,1,attackStartPose):after/Math.max(.1f,attackDuration-attackImpact)*.65f;
        } else progress=AttackMotion.contact(elapsed,attackImpact,attackDuration,twoHanded && attackClip==PlayerAnimation.TWO_HANDED_CHOP?.54f:.44f,attackStartPose);
        setAnimation(clip);advancePose(dt);playAnimationProgress(progress);
        float weight=AttackMotion.recovery(elapsed,attackImpact,attackDuration);
        if(crossbow && weaponStyle==1)weight=AttackMotion.smooth((elapsed/attackDuration-.88f)/.12f);
        if(moving!=null)blendLocomotion(getAnimationIndex(moving.getAnimationName()),locomotionClock*70,weight);
    }

    public void attack(boolean hasWeapon) { attack(1,false,.5f); }

    public void attack(int combo,boolean heavy,float duration) {
        attack(combo,heavy,duration,duration*.36f,0);
    }
    public void attack(int combo,boolean heavy,float duration,float impact,float heldCharge) {
        lungeAttack=false;charging=false;attackDuration=duration; attackTime=duration;
        attackImpact=impact;attackStartPose=heavy&&heldCharge>.3f?(weaponStyle==1||weaponStyle==2?1:.28f):0;
        velocityX=velocityZ=0;
        attackClip=twoHanded?(heavy||combo==2?PlayerAnimation.TWO_HANDED_CHOP:combo==3?PlayerAnimation.TWO_HANDED_FINISHER:PlayerAnimation.TWO_HANDED_SLICE):heavy?PlayerAnimation.ATTACK_CHOP:combo==3?PlayerAnimation.ATTACK_STAB:combo==2?PlayerAnimation.ATTACK_DIAGONAL:PlayerAnimation.ATTACK_HORIZONTAL;
        if (weaponStyle==1) attackClip=crossbow?PlayerAnimation.CROSSBOW_SHOOT:PlayerAnimation.BOW_RELEASE;
        if (weaponStyle==2) attackClip=PlayerAnimation.MAGIC_CAST;
        beginPoseTransition();setAnimation(weaponStyle==1?(crossbow?PlayerAnimation.CROSSBOW_IDLE:PlayerAnimation.BOW_DRAW):weaponStyle==2?PlayerAnimation.MAGIC_RAISE:attackClip);
        restartAnimation();playAnimationProgress(attackStartPose);
    }

    public float attackRemaining() { return attackTime; }
    public boolean isDrawingBow() { return weaponStyle==1 && !crossbow && (charging || isAttacking() && attackDuration-attackTime<attackImpact); }
    public Vector3 handPosition(String name) {
        Model model=getModel();
        int i=boneIndex(name);if(i>=0) {
            Vector3 p=new Transform(model.currentPose()).position(i).translation();double a=Math.toRadians(getRotation().y());
            return new Vector3().x(getPosition().x()+(float)(p.x()*Math.cos(a)+p.z()*Math.sin(a))).y(getPosition().y()+p.y()).z(getPosition().z()+(float)(p.z()*Math.cos(a)-p.x()*Math.sin(a)));
        }
        return new Vector3().x(getPosition().x()).y(1.1f).z(getPosition().z());
    }
    public Vector3 projectileOrigin() {
        Vector3 hand=handPosition("handslot.r");double angle=Math.toRadians(getRotation().y());
        return new Vector3().x(getPosition().x()+(float)Math.sin(angle)*.6f).y(hand.y()).z(getPosition().z()+(float)Math.cos(angle)*.6f);
    }
    public boolean isReloading() { return crossbow && weaponStyle==1 && attackTime>0 && attackDuration-attackTime>=attackImpact+.13f; }
    public void poseForOutcome(boolean victory,float elapsed) {
        setAnimation(victory?PlayerAnimation.CHEER:PlayerAnimation.DEATH);
        advancePose(Math.max(0,elapsed-lastOutcomeTime));lastOutcomeTime=elapsed;
        playAnimationProgress(victory?(elapsed%2.4f)/2.4f:elapsed/1.25f);
        if(!victory && elapsed>.8f)blendLocomotion(getAnimationIndex(PlayerAnimation.GROUND_REST.getAnimationName()),0,AttackMotion.smooth((elapsed-.8f)/.4f));
        if(victory)visualLift=0;
        else if(!fallenPose || elapsed<1.3f)visualLift=Math.max(0,-SkinBounds.vertical(getModel())[0]);
        fallenPose=!victory;
    }
    public boolean isAttacking() { return attackTime > 0; }

    public void poseForInventory() {
        Integer index=getAnimationIndex((weaponStyle==1?rangedIdle():PlayerAnimation.UNARMED_IDLE).getAnimationName());
        if(index!=null)poseStill(index,0);
    }
    public void resetCombat() {
        attackTime = dodgeTime = dodgeCooldown = velocityX = velocityZ = aimLock = chargePoseTime = lastOutcomeTime = 0; sprinting=charging=lungeAttack=false;discipline.reset();
        visualLift=0;fallenPose=false;requestedParry=false;
        setAnimation(weaponStyle==1?rangedIdle():PlayerAnimation.MELEE_IDLE);
        restartAnimation();
        updateAnimation(0);
    }

    public void movement(float dt) {

    }

}
