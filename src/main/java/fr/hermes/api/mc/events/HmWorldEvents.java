package fr.hermes.api.mc.events;

import fr.hermes.api.events.HmEvent;
import fr.hermes.api.events.ListenableHmEvent;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.world.HmChunk;
import fr.hermes.api.mc.world.HmWorld;
import org.joml.Vector3f;

import java.util.List;

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
     * Called when a world is loaded.
     */
    public static final HmEvent<WorldLoadCallback> LOAD = ListenableHmEvent.create(event ->
            world -> event.forEach(l -> l.onWorldLoad(world))
    );

    /**
     * Callback for world unload events.
     */
    @FunctionalInterface
    public interface WorldUnloadCallback {
        void onWorldUnload(HmWorld world);
    }

    /**
     * Called when a world is unloaded.
     */
    public static final HmEvent<WorldUnloadCallback> UNLOAD = ListenableHmEvent.create(event ->
            world -> event.forEach(l -> l.onWorldUnload(world))
    );

    /** 
     * Callback for chunk load events.
     */
    @FunctionalInterface
    public interface ChunkLoadCallback {
        void onChunkLoad(HmChunk chunk);
    }

    /**
     * Called when a chunk is loaded.
     */
    public static final HmEvent<ChunkLoadCallback> CHUNK_LOAD = ListenableHmEvent.create(event ->
            chunk -> event.forEach(l -> l.onChunkLoad(chunk))
    );

    /**
     * Callback for chunk unload events.
     */
    @FunctionalInterface
    public interface ChunkUnloadCallback {
        void onChunkUnload(HmChunk chunk);
    }

    /**
     * Called when a chunk is unloaded.
     */
    public static final HmEvent<ChunkUnloadCallback> CHUNK_UNLOAD = ListenableHmEvent.create(event ->
            chunk -> event.forEach(l -> l.onChunkUnload(chunk))
    );

    /**
     * Callback for explosion detonate events.
     */
    @FunctionalInterface
    public interface ExplosionDetonateCallback {
        void onExplosionDetonate(HmWorld world, Vector3f pos, List<HmEntity> affectedEntities);
    }

    /**
     * Called when an explosion is detonated.
     */
    public static final HmEvent<ExplosionDetonateCallback> EXPLOSION_DETONATE = ListenableHmEvent.create(event ->
            (world, pos, affectedEntities) -> event.forEach(l -> l.onExplosionDetonate(world, pos, affectedEntities))
    );
}
