package fr.hermes.api.mc.events;

import fr.hermes.api.events.HmEvent;
import fr.hermes.api.events.HmEventPhase;
import fr.hermes.api.events.HmEventResult;
import fr.hermes.api.events.ListenableHmEvent;
import fr.hermes.api.mc.entities.HmClientPlayerEntity;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmLivingEntity;
import fr.hermes.api.mc.utils.HmRayTraceResult;
import net.minecraft.client.audio.SoundManager;
import org.joml.Vector3f;

public final class HmMcClientEvents {
    /**
     * Callback for client tick events.
     */
    @FunctionalInterface
    public interface ClientTickCallback {
        void onClientTick(HmEventPhase phase);
    }

    /**
     * Called every client tick.
     */
    public static final HmEvent<ClientTickCallback> TICK = ListenableHmEvent.create(event ->
            (phase) -> event.forEach(l -> l.onClientTick(phase))
    );

    /**
     * Callback for sound system setup events.
     */
    @FunctionalInterface
    public interface SoundSetupCallback {
        void onSoundSetup(SoundManager soundManager);
    }

    /**
     * Called when a sound system setup event is triggered.
     */
    public static final HmEvent<SoundSetupCallback> SOUND_SETUP = ListenableHmEvent.create(event ->
            (soundManager) -> event.forEach(l -> l.onSoundSetup(soundManager))
    );

    /**
     * Callback for sound system load events.
     */
    @FunctionalInterface
    public interface SoundSystemLoadCallback {
        void onSoundSystemLoad();
    }

    /**
     * Called when a sound system load event is triggered.
     */
    public static final HmEvent<SoundSystemLoadCallback> SOUND_SYSTEM_LOAD = ListenableHmEvent.create(event ->
            () -> event.forEach(l -> l.onSoundSystemLoad())
    );

    /**
     * Callback for draw block highlight events.
     */
    @FunctionalInterface
    public interface DrawBlockHighlightCallback {
        void onDrawBlockHighlight(HmClientPlayerEntity player, HmRayTraceResult target, float partialTicks);
    }

    /**
     * Called when a block highlight is rendered.
     */
    public static final HmEvent<DrawBlockHighlightCallback> DRAW_BLOCK_HIGHLIGHT = ListenableHmEvent.create(event ->
            (player, target, partialTicks) -> event.forEach(l -> l.onDrawBlockHighlight(player, target, partialTicks))
    );

    /**
     * Callback for camera setup events.
     */
    @FunctionalInterface
    public interface CameraSetupCallback {
        HmEventResult<Vector3f> onCameraSetup(HmEntity entity, float yaw, float pitch, float roll, float partialTicks);
    }

    /**
     * Called when a camera setup is triggered.
     */
    public static final HmEvent<CameraSetupCallback> CAMERA_SETUP = ListenableHmEvent.create(event ->
            (entity, yaw, pitch, roll, partialTicks) -> event.processWithResult(l -> l.onCameraSetup(entity, yaw, pitch, roll, partialTicks))
    );

    /**
     * Callback for render world last events.
     */
    @FunctionalInterface
    public interface RenderWorldLastCallback {
        void onRenderWorldLast(float partialTicks);
    }

    /**
     * Called when the world is rendered.
     */
    public static final HmEvent<RenderWorldLastCallback> RENDER_WORLD_LAST = ListenableHmEvent.create(event ->
            (partialTicks) -> event.forEach(l -> l.onRenderWorldLast(partialTicks))
    );

    /**
     * Callback for entity render events.
     */
    @FunctionalInterface
    public interface PlayerRenderCallback {
        HmEventResult.Type onPlayerRender(HmClientPlayerEntity player, float partialTicks, boolean renderShadow);
    }
    
    /**
     * Called when a player is rendered.
     */
    public static final HmEvent<PlayerRenderCallback> PLAYER_RENDER_PRE = ListenableHmEvent.create(event ->
            (player, partialTicks, renderShadow) -> event.process(l -> l.onPlayerRender(player, partialTicks, renderShadow))
    );

    /**
     * Callback for entity render events.
     */
    @FunctionalInterface
    public interface LivingEntityRenderCallback {
        HmEventResult.Type onLivingEntityRender(HmLivingEntity entity, float partialTicks, boolean renderShadow);
    }

    /**
     * Called when a player is rendered.
     */
    public static final HmEvent<LivingEntityRenderCallback> LIVING_ENTITY_RENDER_PRE = ListenableHmEvent.create(event ->
            (entity, partialTicks, renderShadow) -> event.process(l -> l.onLivingEntityRender(entity, partialTicks, renderShadow))
    );
}
