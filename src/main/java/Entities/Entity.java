package Entities;

import java.util.HashMap;

import static com.raylib.Raylib.*;

public abstract class Entity {

    private Model    model;
    private ModelAnimation animations;
    private ModelSkeleton modelSkeleton;
    private boolean hasAnimation = false;
    private int animationIndex = 0;
    private int currentFrame = 0;
    private int posedAnimation,posedFrame;
    private int[] animationCount = new int[1];
    private float animationTimer = 0.0f;
    private float animationFPS = 24.0f;

    private boolean hasSkeleton;

    private float  scale = 1.0f;
    private Vector3  rotation = new Vector3();
    private Vector3  position = new Vector3();
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

    private final HashMap<String, Integer> animationIndices = new HashMap<>();

    protected Integer getAnimationIndex(String name) {
        // raylib stores ModelAnimation.name in 32 bytes including its terminator.
        // The imported horizontal-slice name is 32 characters and loses its final letter.
        Integer index=animationIndices.get(name);
        return index!=null?index:animationIndices.get(name.substring(0,Math.min(31,name.length())));
    }

    public void setAnimation(int index) {
        if (!hasAnimation) {
            System.out.println("No animation");
            return;
        }

        if (index < 0 || index >= animationCount[0]) {
            System.out.println("Invalid index");
            return;
        }

        if (animationIndex == index) {
            return;
        }
        animationIndex = index;
        currentFrame = 0;
        animationTimer = 0.0f;
    }

    public void restartAnimation() {
        currentFrame = 0;
        animationTimer = 0;
    }

    public void playAnimationProgress(float progress) {
        if (!hasAnimation || model == null) return;
        ModelAnimation clip = new ModelAnimation(animations).position(animationIndex);
        currentFrame = Math.min(clip.keyframeCount()-1, (int)(Math.max(0, progress)*clip.keyframeCount()));
        poseForDisplay(animationIndex,currentFrame);
    }

    protected void poseForDisplay(int index,int frame) {
        posedAnimation=index;posedFrame=frame;
        UpdateModelAnimation(model,new ModelAnimation(animations).position(index),frame);
    }
    public void copyPoseTo(Model equipment) {
        UpdateModelAnimation(equipment,new ModelAnimation(animations).position(posedAnimation),posedFrame);
    }
    public int posedFrame() { return posedFrame; }
    public ModelAnimation getAnimation() {
        return animations;
    }

    public void loadAnimations(String path) {

        animationIndices.clear(); hasAnimation=false; animationIndex=0; restartAnimation();
        animations = LoadModelAnimations(path, animationCount);

        for (int i = 0; i < animationCount[0]; i++) {
            hasAnimation = true;

            ModelAnimation animation = new ModelAnimation(animations).position(i);

            String name = animation.name().getString();

            animationIndices.put(name, i);

            System.out.println(i + ": " + name + " | Frames: " + animation.keyframeCount());
        }
    }

    public void updateAnimation(float dt) {

        if (!hasAnimation || animations == null || model == null) {
            return;
        }

        ModelAnimation animation = new ModelAnimation(animations).position(animationIndex);

        animationTimer += dt;
        float frameTime = 1.0f / animationFPS;

        if (animation.keyframeCount() <= 0) return;
        while (animationTimer >= frameTime) {
            animationTimer -= frameTime;
            currentFrame++;
            if (currentFrame >= animation.keyframeCount()) {
                currentFrame = 0;
            }
        }

        poseForDisplay(animationIndex,currentFrame);
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
        if (fps > 0 && Float.isFinite(fps)) animationFPS = fps;
    }

    public ModelSkeleton getModelSkeleton() {
        return modelSkeleton;
    }

    public void dispose() {
        if (animations != null) { UnloadModelAnimations(animations, animationCount[0]); animations = null; }
        if (model != null) { UnloadModel(model); model = null; }
    }

    public void update(float dt) {

    }


}
