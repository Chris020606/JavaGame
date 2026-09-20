package Entities;


import Scene.CameraController;

import static com.raylib.Raylib.*;

enum PlayerAnimation {
    Jump_Full_Long,
    Jump_Full_Short,
    Jump_Idle,
    Jump_Land,
    Jump_Start,
    Running_A,
    Running_B,
    T_Pose,
    Walking_A,
    Walking_B,
    Walking_C
}

public class Player extends Entity {

    private String name;
    private float movementSpeed = 5.0f;
    private boolean sex; // true = male, false = female
    private int level;
    private int health;


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

        if (IsKeyDown(KEY_W)) {
            moveX += forwardX;
            moveZ += forwardZ;
            moving = true;
        }

        if (IsKeyDown(KEY_S)) {
            moveX -= forwardX;
            moveZ -= forwardZ;
            moving = true;
        }

        if (IsKeyDown(KEY_D)) {
            moveX += rightX;
            moveZ += rightZ;
            moving = true;
        }

        if (IsKeyDown(KEY_A)) {
            moveX -= rightX;
            moveZ -= rightZ;
            moving = true;
        }

        float moveLength = (float)Math.sqrt(moveX * moveX + moveZ * moveZ);

        if (moveLength > 0.0f) {
            moveX /= moveLength;
            moveZ /= moveLength;
            position.x(position.x() + moveX * movementSpeed * dt);

            position.z( position.z() + moveZ * movementSpeed * dt);
            float angle = (float)Math.toDegrees(Math.atan2(moveX, moveZ));

            setRotation(new Vector3()
                .x(0)
                .y(angle)
                .z(0));
        }

        setPosition(position);

        if (moving) {
            setAnimation(9);
        } else {
            setAnimation(2);
        }

        updateAnimation(dt);
    }
}
