package Entities;


import static com.raylib.Raylib.*;

public class Player extends Entity {

    private String name;
    private float movementSpeed = 5.0f;
    private boolean sex; // true = male, false = female
    private int level;
    private int health;

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

    @Override
    public void update(float dt) {
        Vector3 position = getPosition();

        boolean moving = false;

        if (IsKeyDown(KEY_W)) {
            position.z(position.z() - movementSpeed * dt);
            moving = true;
        }

        if (IsKeyDown(KEY_S)) {
            position.z(position.z() + movementSpeed * dt);
            moving = true;
        }

        if (IsKeyDown(KEY_A)) {
            position.x(position.x() - movementSpeed * dt);
            moving = true;
        }

        if (IsKeyDown(KEY_D)) {
            position.x(position.x() + movementSpeed * dt);
            moving = true;
        }

        setPosition(position);

        if (moving) {
            setAnimation(9);
        } else {
            setAnimation(7);
        }

        updateAnimation(dt);
    }
}
