package Entities;

import java.util.HashMap;

import static com.raylib.Raylib.*;

public abstract class Entity {

    private Model    model;
    private ModelAnimation animations;
    private ModelSkeleton modelSkeleton;
    private boolean hasAnimation = false;
    private int animationIndex = 0;
    private float currentFrame;
    private int posedAnimation,posedFromAnimation,transitionFrom;
    private float posedFrame,posedFromFrame,posedBlend=1,transitionFrame,transitionTime;
    private boolean displayedPose;
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
        beginPoseTransition();
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
        currentFrame = AnimationFrames.sample(progress,clip.keyframeCount());
        poseForDisplay(animationIndex,currentFrame);
    }

    protected void advancePose(float dt) { transitionTime=Math.max(0,transitionTime-Math.max(0,dt)); }
    protected void beginPoseTransition() { if(displayedPose) { transitionFrom=posedAnimation;transitionFrame=posedFrame;transitionTime=.09f; } }
    protected void poseStill(int index,float frame) { transitionTime=0;poseForDisplay(index,frame); }
    protected void poseForDisplay(int index,float frame) {
        ModelAnimation clip=new ModelAnimation(animations).position(index);
        posedAnimation=index;posedFrame=Math.max(0,Math.min(clip.keyframeCount()-1,frame));displayedPose=true;
        posedBlend=transitionTime>0?1-transitionTime/.09f:1;posedFromAnimation=transitionFrom;posedFromFrame=transitionFrame;
        applyPose(model);
    }
    private void applyPose(Model target) {
        ModelAnimation clip=new ModelAnimation(animations).position(posedAnimation);
        if(posedBlend<1) {
            ModelAnimation from=new ModelAnimation(animations).position(posedFromAnimation);
            UpdateModelAnimationEx(target,from,Math.max(0,Math.min(from.keyframeCount()-1,posedFromFrame)),clip,posedFrame,posedBlend);
        } else UpdateModelAnimation(target,clip,posedFrame);
    }
    protected void blendLocomotion(int index,float frames,float amount) {
        if(amount<=0)return;
        posedFromAnimation=posedAnimation;posedFromFrame=posedFrame;
        posedAnimation=index;posedFrame=AnimationFrames.looping(frames,new ModelAnimation(animations).position(index).keyframeCount());posedBlend=Math.min(1,amount);
        applyPose(model);
    }
    public void copyPoseTo(Model equipment) {
        applyPose(equipment);
    }
    public int posedFrame() { return (int)posedFrame; }
    public float sampledFrame() { return posedFrame; }
    public ModelAnimation getAnimation() {
        return animations;
    }

    public void loadAnimations(String path) {

        animationIndices.clear(); hasAnimation=false;displayedPose=false;transitionTime=0;posedBlend=1;animationIndex=0; restartAnimation();
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

        if (animation.keyframeCount() <= 0) return;
        advancePose(dt);animationTimer=(animationTimer+Math.max(0,dt)*animationFPS)%animation.keyframeCount();currentFrame=AnimationFrames.looping(animationTimer,animation.keyframeCount());
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
