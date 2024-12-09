package fr.dynamx.client.renders.animations;

import com.modularmods.mcgltf.dynamx.animation.InterpolatedChannel;
import fr.dynamx.client.renders.model.renderer.GltfModelRenderer;
import fr.dynamx.utils.optimization.QuaternionPool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.model.animation.Animation;

import java.util.List;

@RequiredArgsConstructor
@ToString
public class DxAnimation {

    @Getter
    private final String name;
    @ToString.Exclude
    private final List<InterpolatedChannel> channels;
    @Getter
    private final EnumAnimType animType;
    @Getter
    private boolean isPlaying;
    @Getter
    private boolean hasEnded;
    @Setter
    @Accessors(fluent = true)
    private boolean shouldLoop;

    //public float currentTime = 0;
    public float finalTime = 100;

    private final DxAnimation.Timer timer = new DxAnimation.Timer();

    boolean allTimerEnder;
    InterpolatedChannel.TransformType update = null;

    public void playAnimation(GltfModelRenderer modelRenderer, DxAnimator animator, float partialTicks) {
        QuaternionPool.openPool();
        float worldTime = Animation.getWorldTime(Minecraft.getMinecraft().world, partialTicks);
        float tmpDeltaTime = 0;
        if (allTimerEnder) {
            timer.timerEnded = false;
            timer.shouldPlayFinalTransition = false;
            timer.currentTime = 0;
            if (animator != null) {
                animator.isAnimationPlaying = false;
                animator.animationQueue.poll();
            }
            isPlaying = false;
            //System.out.println("END CONFIRMED");
            for (InterpolatedChannel channel : channels) {
                update = channel.update(0);
                GltfModelRenderer.Transform transform = modelRenderer.initialNodeTransforms.get(channel.nodeModel);
                if (update != null)
                    modelRenderer.resetNodeModel(channel.nodeModel, transform, 1);
            }
            return;
        }
        if (timer.timerEnded) {
            //System.out.println("ANIM END");
            allTimerEnder = true;
        } else {
            allTimerEnder = false;
        }
       // System.out.println("===== FRAME =====");
        float endTime = getEndTime(channels.get(0));// + (finalTime / 20);
        //System.out.println("Animation: " + name + " - " + timer.currentTime + " - " + endTime + " " + finalTime + " " + worldTime + " " + (worldTime % endTime));
        tmpDeltaTime = 0.01f; // todo use DxAnimation.time_step ?
        timer.updateTimer(animator, this, endTime, tmpDeltaTime);
        for (InterpolatedChannel channel : channels) {
            isPlaying = true;
            if (shouldLoop) {
                channel.update(timer.currentTime);//worldTime % endTime);
            } else {
                if (!timer.shouldPlayFinalTransition) {
                    update = channel.update(timer.currentTime);//Math.min(timer.currentTime, endTime - (finalTime / 20)));
                    if (update != null)
                        modelRenderer.blendInitialPose(channel.nodeModel, update.copiedValues, update.type, 1);
                }
                /*if ((animator.getBlendPose().equals(DxAnimator.EnumBlendPose.START)
                        || animator.getBlendPose().equals(DxAnimator.EnumBlendPose.START_END)
                        || animator.getBlendPose().equals(DxAnimator.EnumBlendPose.END))) {
                    if (!timer.shouldPlayFinalTransition && (animType.equals(EnumAnimType.START) || animType.equals(EnumAnimType.START_END))) {
                        tmpDeltaTime = partialTicks / (endTime - (finalTime / 20));
                        if (update != null) {
                            //System.out.println(timer.currentTime);
                            System.out.println("Channel " + channel.nodeModel.getName() + " end");
                            modelRenderer.blendInitialPose(channel.nodeModel, update.copiedValues, update.type, 0);
                        }
                    }
                    if (timer.shouldPlayFinalTransition && (animType.equals(EnumAnimType.END) || animType.equals(EnumAnimType.START_END))) {
                        tmpDeltaTime = partialTicks / (finalTime);
                        GltfModelRenderer.Transform transform = modelRenderer.initialNodeTransforms.get(channel.nodeModel);
                        float delta = (timer.currentTime);//todo - (endTime - (finalTime / 20))) / (finalTime / 20);
                        modelRenderer.resetNodeModel(channel.nodeModel, transform, delta);
                    }
                }*/
            }

        }
        QuaternionPool.closePool();
    }

    public void resetAnimation() {
      //  System.out.println("RESANIM " + this.name);
        timer.timerEnded = false;
        timer.shouldPlayFinalTransition = false;
        timer.currentTime = 0;
        for (InterpolatedChannel channel : channels) {
            channel.update(0);
        }
    }

    public void resetModel(GltfModelRenderer gltfModelRenderer, float partialTicks) {
        gltfModelRenderer.resetModel(partialTicks);
    }

    public float getStartTime(InterpolatedChannel channel) {
        return channel.getKeys()[0];
    }

    public float getEndTime(InterpolatedChannel channel) {
        return channel.getKeys()[channel.getKeys().length - 1];
    }

    public float getTotalEndTime() {
        float test = 0;
        for (InterpolatedChannel interpolatedChannel : channels) {
            test += getEndTime(interpolatedChannel);
        }
        return test;
    }

    public float getDuration(InterpolatedChannel channel) {
        return getEndTime(channel) - getStartTime(channel);
    }

    public enum EnumAnimType {
        NORMAL, LOOP, START, END, START_END
    }

    public static class Timer {

        public float currentTime = 0;
        public boolean timerEnded = false;
        public float endTime = 0;

        public boolean shouldPlayFinalTransition = false;


        public void updateTimer(DxAnimator animator, DxAnimation animation, float endTime, float deltaTime) {
            if (!shouldPlayFinalTransition && currentTime < endTime) {// - (animation.finalTime / 20)) {
                currentTime += deltaTime;
            } else if (currentTime >= endTime) {// - (animation.finalTime / 20)) {
                shouldPlayFinalTransition = true;
            }
            if (shouldPlayFinalTransition && currentTime < endTime) {
                currentTime += deltaTime;
            } else if (currentTime >= endTime) {
                timerEnded = true;
            }
        }
    }
}


