package fr.hermes.forge.events;

import fr.hermes.api.mc.entities.HmServerPlayerEntity;
import fr.hermes.api.mc.events.HmPlayerEvents;
import fr.hermes.api.mc.events.HmWorldEvents;
import fr.hermes.api.mc.world.HmWorld;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

// TODO REGISTER
public class HmForgeCommonEventHandler {
    @SubscribeEvent
    public void playerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        HmPlayerEvents.JOIN.invoker().onPlayerJoin((HmServerPlayerEntity) event.player);
    }

    @SubscribeEvent
    public void playerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        HmPlayerEvents.LEAVE.invoker().onPlayerLeave((HmServerPlayerEntity) event.player);
    }

    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        HmWorldEvents.LOAD.invoker().onWorldLoad((HmWorld) event.getWorld());
    }

    @SubscribeEvent
    public void worldUnload(WorldEvent.Unload event) {
        HmWorldEvents.UNLOAD.invoker().onWorldUnload((HmWorld) event.getWorld());
    }
}
