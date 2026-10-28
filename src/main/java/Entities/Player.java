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

        if (moveLength > 0.0f) {
            moveX /= moveLength;
            moveZ /= moveLength;
            position.x(Math.max(-22, Math.min(22, position.x() + moveX * movementSpeed * dt * (isAttacking() ? .4f : 1))));

            position.z(Math.max(-22, Math.min(22, position.z() + moveZ * movementSpeed * dt * (isAttacking() ? .4f : 1))));
            float angle = (float)Math.toDegrees(Math.atan2(moveX, moveZ));

            if (!isAttacking()) setRotation(new Vector3().x(0).y(angle).z(0));}

        setPosition(position);

        attackTime = Math.max(0, attackTime - dt);
        if (attackTime > 0) {
            setAnimation(attackClip);
        } else if (moving) {
            setAnimation(PlayerAnimation.WALK);
        } else {
            setAnimation(PlayerAnimation.MELEE_IDLE);
        }

        if (attackTime > 0) playAnimationProgress(1-attackTime/attackDuration);
        else updateAnimation(dt);
    }

    public void attack(boolean hasWeapon) { attack(1,false,.5f); }

    public void attack(int combo,boolean heavy,float duration) {
        attackDuration=duration; attackTime=duration;
        attackClip=heavy ? PlayerAnimation.ATTACK_CHOP : combo==2 ? PlayerAnimation.ATTACK_DIAGONAL : PlayerAnimation.ATTACK_HORIZONTAL;
        if (weaponStyle==1) attackClip=PlayerAnimation.BOW_RELEASE;
        if (weaponStyle==2) attackClip=PlayerAnimation.MAGIC_CAST;
        setAnimation(attackClip);
        restartAnimation(); playAnimationProgress(0);
    }

    public boolean isAttacking() { return attackTime > 0; }

    public void resetCombat() {
        attackTime = 0;
        setAnimation(PlayerAnimation.MELEE_IDLE);
        restartAnimation();
        updateAnimation(0);
    }

    public void movement(float dt) {

    }

}
