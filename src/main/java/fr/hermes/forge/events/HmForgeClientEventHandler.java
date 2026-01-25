package fr.hermes.forge.events;

import fr.hermes.api.events.HmEventPhase;
import fr.hermes.api.events.HmEventResult;
import fr.hermes.api.mc.entities.HmClientPlayerEntity;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmLivingEntity;
import fr.hermes.api.mc.events.HmMcClientEvents;
import fr.hermes.api.mc.events.HmPlayerEvents;
import fr.hermes.api.mc.utils.HmRayTraceResult;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.client.event.sound.SoundSetupEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.joml.Vector3f;

// TODO REGISTER
public class HmForgeClientEventHandler {
    @SubscribeEvent
    public void clientConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        HmPlayerEvents.CLIENT_CONNECTED_TO_SERVER.invoker().onClientConnectedToServer();
    }

    @SubscribeEvent
    public void clientDisconnectedFromServer(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        HmPlayerEvents.CLIENT_DISCONNECTED_FROM_SERVER.invoker().onClientDisconnectedFromServer();
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        HmMcClientEvents.TICK.invoker().onClientTick(event.phase == TickEvent.Phase.START ? HmEventPhase.PRE : HmEventPhase.POST);
    }

    @SubscribeEvent
    public void soundSystemSetup(SoundSetupEvent event) {
        HmMcClientEvents.SOUND_SETUP.invoker().onSoundSetup(event.getManager());
    }

    @SubscribeEvent
    public void soundSystemLoad(SoundLoadEvent event) {
        HmMcClientEvents.SOUND_SYSTEM_LOAD.invoker().onSoundSystemLoad();
    }

    @SubscribeEvent
    public void drawBlockHighlight(DrawBlockHighlightEvent event) {
        HmMcClientEvents.DRAW_BLOCK_HIGHLIGHT.invoker().onDrawBlockHighlight((HmClientPlayerEntity) event.getPlayer(), (HmRayTraceResult) event.getTarget(), event.getPartialTicks());
    }

    @SubscribeEvent
    public void cameraSetup(EntityViewRenderEvent.CameraSetup event) {
        HmEventResult<Vector3f> result = HmMcClientEvents.CAMERA_SETUP.invoker().onCameraSetup((HmEntity) event.getEntity(), event.getYaw(), event.getPitch(), event.getRoll(), (float) event.getRenderPartialTicks());
        if (result.hasResult()) {
            event.setYaw(result.getResult().x);
            event.setPitch(result.getResult().y);
            event.setRoll(result.getResult().z);
        }
    }

    @SubscribeEvent
    public void renderWorldLast(RenderWorldLastEvent event) {
        HmMcClientEvents.RENDER_WORLD_LAST.invoker().onRenderWorldLast(event.getPartialTicks());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void playerRender(RenderPlayerEvent.Pre event) {
        HmEventResult.Type result = HmMcClientEvents.PLAYER_RENDER_PRE.invoker().onPlayerRender((HmClientPlayerEntity) event.getEntity(), event.getPartialRenderTick());
        if (result.isCancelled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void livingEntityRender(RenderLivingEvent.Pre event) {
        HmEventResult.Type result = HmMcClientEvents.LIVING_ENTITY_RENDER_PRE.invoker().onLivingEntityRender((HmLivingEntity) event.getEntity(), event.getPartialRenderTick());
        if (result.isCancelled()) {
            event.setCanceled(true);
        }
    }
}
