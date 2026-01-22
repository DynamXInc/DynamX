package fr.hermes.api.mc.events;

import fr.hermes.api.events.HmEvent;
import fr.hermes.api.events.ListenableHmEvent;
import fr.hermes.api.mc.world.HmWorld;

/**
 * Registry of Minecraft World events
 */
public final class HmWorldEvents {
    /**
     * Callback for world load events.
     */
    @FunctionalInterface
    public interface WorldLoadCallback {
        void onWorldLoad(HmWorld world);
    }

    /**
     * Callback for world unload events.
     */
    @FunctionalInterface
    public interface WorldUnloadCallback {
        void onWorldUnload(HmWorld world);
    }

    /**
     * Called when a world is loaded.
     */
    public static final HmEvent<WorldLoadCallback> LOAD = ListenableHmEvent.create(event ->
            world -> event.forEach(l -> l.onWorldLoad(world))
    );

    /**
     * Called when a world is unloaded.
     */
    public static final HmEvent<WorldUnloadCallback> UNLOAD = ListenableHmEvent.create(event ->
            world -> event.forEach(l -> l.onWorldUnload(world))
    );
}
