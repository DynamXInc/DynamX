package fr.hermes.forge.events;

import fr.dynamx.core.utils.optimization.Vector3fPool;
import fr.hermes.api.events.HmEventPhase;
import fr.hermes.api.events.HmEventResult;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import fr.hermes.api.mc.entities.HmServerPlayerEntity;
import fr.hermes.api.mc.events.HmPlayerEvents;
import fr.hermes.api.mc.events.HmWorldEvents;
import fr.hermes.api.mc.items.HmItemStack;
import fr.hermes.api.mc.world.HmChunk;
import fr.hermes.api.mc.world.HmWorld;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.joml.Vector3i;

import java.util.List;
import java.util.Vector;

// TODO REGISTER
public class HmForgeCommonEventHandler {
    @SubscribeEvent
    public void playerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        HmPlayerEvents.SERVER_JOIN.invoker().onPlayerJoin((HmServerPlayerEntity) event.player);
    }

    @SubscribeEvent
    public void playerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        HmPlayerEvents.SERVER_LEAVE.invoker().onPlayerLeave((HmServerPlayerEntity) event.player);
    }

    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        HmWorldEvents.LOAD.invoker().onWorldLoad((HmWorld) event.getWorld());
    }

    @SubscribeEvent
    public void worldUnload(WorldEvent.Unload event) {
        HmWorldEvents.UNLOAD.invoker().onWorldUnload((HmWorld) event.getWorld());
    }

    @SubscribeEvent
    public void chunkLoad(ChunkEvent.Load event) {
        HmWorldEvents.CHUNK_LOAD.invoker().onChunkLoad((HmChunk) event.getChunk());
    }

    @SubscribeEvent
    public void chunkUnload(ChunkEvent.Unload event) {
        HmWorldEvents.CHUNK_UNLOAD.invoker().onChunkUnload((HmChunk) event.getChunk());
    }

    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent event) {
        HmPlayerEvents.TICK.invoker().onPlayerTick((HmPlayerEntity) event.player, event.phase == TickEvent.Phase.START ? HmEventPhase.PRE :  HmEventPhase.POST);
    }

    @SubscribeEvent
    public void playerStartTracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
        HmPlayerEvents.START_TRACKING.invoker().onPlayerStartTracking((HmServerPlayerEntity) event.getEntityPlayer(), (HmEntity) event.getTarget());
    }

    @SubscribeEvent
    public void explosionDetonate(ExplosionEvent.Detonate event) {
        HmWorldEvents.EXPLOSION_DETONATE.invoker().onExplosionDetonate((HmWorld) event.getWorld(), 
            Vector3fPool.get(event.getExplosion().getPosition().x, event.getExplosion().getPosition().y, event.getExplosion().getPosition().z),
            (List) event.getAffectedEntities());
    }

    @SubscribeEvent
    public void playerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        HmPlayerEvents.RIGHT_CLICK_BLOCK.invoker().onPlayerRightClickBlock((HmPlayerEntity) event.getEntityPlayer(),
            event.getHand(), event.getPos(), Vector3fPool.get(event.getHitVec().x, event.getHitVec().y, event.getHitVec().z));
    }

    @SubscribeEvent
    public void playerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        HmPlayerEvents.RIGHT_CLICK_ITEM.invoker().onPlayerRightClickItem((HmPlayerEntity) event.getEntityPlayer(), 
            event.getHand(), (HmItemStack) (Object) event.getItemStack());
    }

    @SubscribeEvent
    public void playerRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        HmEventResult<Void> result = HmPlayerEvents.RIGHT_CLICK_ENTITY.invoker().onPlayerRightClickEntity((HmPlayerEntity) event.getEntityPlayer(), 
            event.getHand(), (HmEntity) event.getTarget());
        if (result.shouldStopPropagation()) {
            event.setCanceled(true);
            event.setCancellationResult(result.isSuccess() ? EnumActionResult.SUCCESS : EnumActionResult.FAIL);
        }
    }
}
