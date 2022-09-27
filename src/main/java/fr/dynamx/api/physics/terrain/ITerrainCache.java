package fr.dynamx.api.physics.terrain;

import fr.dynamx.common.physics.terrain.cache.FileTerrainCache;
import fr.dynamx.common.physics.terrain.chunk.ChunkCollisions;
import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.common.physics.terrain.chunk.ChunkTerrain;
import fr.dynamx.common.physics.terrain.element.TerrainElementType;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.Profiler;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * Manages terrain loading and caching, planned for removed RemoteTerrainCache. See {@link FileTerrainCache} for implementation
 *
 * @see ITerrainManager
 */
public interface ITerrainCache {
    /**
     * Marks the chunk's data to be removed from this cache
     *
     * @param changed     True if the data has changed and should be deleted, false if we just don't need the data to be loaded in the cache anymore
     * @param syncChanges If chunk should be invalidated for clients
     */
    void invalidate(VerticalChunkPos pos, boolean changed, boolean syncChanges);

    /**
     * Marks the chunk's data to be removed from this cache
     *
     * @param changed     True if the data has changed and should be deleted, false if we just don't need the data to be loaded in the cache anymore
     * @param syncChanges If chunk should be invalidated for clients
     */
    void invalidate(ChunkLoadingTicket pos, boolean changed, boolean syncChanges);

    /**
     * Clears all cached data, called on world unload
     */
    void clear();

    /**
     * Loads some chunk's data from the cache, on client implementations, it is an function call that retrieves the chunk data from the server <br>
     * It is not async on server side
     *
     * @param ticket      The chunk to load
     * @param terrainType The terrain type to load
     */
    default CompletableFuture<ChunkTerrain> asyncLoad(ChunkLoadingTicket ticket, TerrainElementType terrainType) {
        CompletableFuture<ChunkTerrain> future = new CompletableFuture<>();
        future.complete(load(ticket, Profiler.get()));
        return future;
    }

    /**
     * Ticks this cache (updates save operation, and sends updates to clients)
     */
    void tick();

    /**
     * Adds a chunk to the cache
     */
    void addChunkToSave(ChunkLoadingTicket loadingTicket, ChunkCollisions collisions);

    /**
     * Loads some chunk's data from the cache, it is a synchronous function that can load files from the disk
     *
     * @param ticket   The chunk to load
     * @param profiler The profiler, to time things
     * @return The chunk data, or null if not present in the cache
     */
    @Nullable
    ChunkTerrain load(ChunkLoadingTicket ticket, Profiler profiler);

    /**
     * If remote, then the locally computed collisions will never be trusted
     */
    boolean isRemoteCache();
}
