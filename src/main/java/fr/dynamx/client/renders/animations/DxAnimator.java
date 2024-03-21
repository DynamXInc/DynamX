package fr.dynamx.client.renders.animations;

import com.modularmods.mcgltf.animation.InterpolatedChannel;
import fr.dynamx.client.renders.model.renderer.GltfModelRenderer;
import fr.dynamx.common.blocks.TEDynamXBlock;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Class that manages animations for a model
 *
 * @see TEDynamXBlock
 */
public class DxAnimator {
    /**
     * Time between two frames (Animations should play at 60 FPS)
     */
    public static final float TIME_STEP = 1.0f / 60f;
    /**
     * Is any animation playing
     */
    protected boolean isAnimationPlaying;

    /**
     * Should the animator pause
     */
    @Getter
    @Setter
    @Accessors(fluent = true)
    private boolean shouldPause;

    /**
     * Map of all the animations of a model (key: animation name, value: list of channels)
     * This map should be filled before playing any animation
     */
    @Nullable
    @Setter
    public HashMap<String, List<InterpolatedChannel>> modelAnimations;

    /**
     * Queue of animations to play
     */
    @Getter
    protected final Queue<DxAnimation> animationQueue = new LinkedList<>();

    @Getter
    @Setter
    private EnumBlendPose blendPose = EnumBlendPose.NONE;

    /**
     * Plays all the animations of the queue
     */
    public void update(GltfModelRenderer modelRenderer, float partialTicks) {
        if (shouldPause) return;


        if (animationQueue.isEmpty()) {
            if (modelRenderer.animation != null) {
                /*modelRenderer.animation.resetAnimation();
                modelRenderer.resetNodeTransforms(1);
                modelRenderer.animation = null;*/
            }
            isAnimationPlaying = false;
            return;
        }

        //System.out.println(animationQueue);
        DxAnimation currentAnimation = animationQueue.peek();
        currentAnimation.playAnimation(modelRenderer,this, partialTicks);
    }

    /**
     * Adds an animation to the queue and returns a DxAnimation object
     *
     * @param animationName Name of the animation to add
     */
    public DxAnimation addAnimation(String animationName, DxAnimation.EnumAnimType type) {
        if (modelAnimations == null) throw new IllegalStateException("Model animations map is null," +
                " you should call the fillModelAnimations method before playing any animation");
        List<InterpolatedChannel> list = modelAnimations.containsKey(animationName) ?
                modelAnimations.get(animationName) : new ArrayList<>();
        DxAnimation animation = new DxAnimation(animationName, list, type);
        animationQueue.add(animation);
        return animation;
    }

    /**
     * Returns true if any animation is playing
     */
    public boolean isAnyAnimationPlaying() {
        return isAnimationPlaying;
    }

    /**
     * Plays the next animation in the queue
     */
    public void playNextAnimation() {
        animationQueue.poll();
    }

    /**
     * Returns the name of the animation that is currently playing
     */
    @Nullable
    public DxAnimation getPlayingAnimation() {
        return animationQueue.peek();
    }

    public enum EnumBlendPose{
        NONE, START, END, START_END
    }

}
