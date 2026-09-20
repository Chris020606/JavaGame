package Entities;

import static com.raylib.Raylib.*;

public abstract class Entity {

    private Model    model;
    private ModelAnimation animations;
    private ModelSkeleton modelSkeleton;
    private boolean hasAnimation = false;
    private int animationIndex = 0;
    private int currentFrame = 0;
    private int[] animationCount = new int[1];
    private float animationTimer = 0.0f;
    private float animationFPS = 24.0f;

    private boolean hasSkeleton;

    private float  scale = 1.0f;
    private Vector3  rotation = new Vector3();
    private Vector3  position;
    private Material material;

    public Model getModel() {
        return model;
    }

    public Material getMaterial() {
        return material;
    }

    public Vector3 getPosition() {
        return position;
    }

    public Vector3 getRotation() {
        return rotation;
    }

    public float getScale() {
        return scale;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void setPosition(Vector3 position) {
        this.position = position;
    }

    public void setRotation(Vector3 rotation) {
        this.rotation = rotation;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public void setAnimation(int index) {
        if (!hasAnimation) {
            return;
        }

        if (index < 0 || index >= animationCount[0]) {
            return;
        }

        // Don't restart the animation every frame
        if (animationIndex == index) {
            return;
        }
        animationIndex = index;
        currentFrame = 0;
        animationTimer = 0.0f;
    }

    public ModelAnimation getAnimation() {
        return animations;
    }

    public void loadAnimations(String path) {

        animations = LoadModelAnimations(path, animationCount);

        if (animationCount[0] > 0) {
            hasAnimation = true;
            System.out.println("Loaded " + animationCount[0] + " animations");
        }
        else {
            System.out.println("WARNING: No animations loaded from: " + path);
        }
    }

    public void updateAnimation(float dt) {

        if (!hasAnimation || animations == null || model == null) {
            return;
        }

        ModelAnimation animation = new ModelAnimation(animations).position(animationIndex);

        animationTimer += dt;
        float frameTime = 1.0f / animationFPS;

        if (animationTimer >= frameTime) {
            animationTimer -= frameTime;
            currentFrame++;
            if (currentFrame >= animation.keyframeCount()) {
                currentFrame = 0;
            }
        }

        UpdateModelAnimation(model, animation, currentFrame);
    }

    public int getAnimationIndex() {
        return animationIndex;
    }

    public int getAnimationCount() {
        return animationCount[0];
    }

    public boolean hasAnimation() {
        return hasAnimation;

    }

    public void setAnimationFPS(float fps) {
        animationFPS = fps;
    }

    public ModelSkeleton getModelSkeleton() {
        return modelSkeleton;
    }

    public void update(float dt) {

    }


}
