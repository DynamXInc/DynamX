package fr.dynamx.api.physics.terrain;

import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.physics.terrain.WorldTerrainState;
import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.common.physics.terrain.chunk.ChunkCollisions;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.Profiler;
import net.minecraft.world.World;
import net.minecraftforge.event.world.ChunkEvent;

import javax.annotation.Nullable;

/**
 * A TerrainManager is responsible for loading the world collisions (blocs and non-physic, non-player) entities <br>
 *     Interface used for protection system
 */
public interface ITerrainManager
{
    /**
     * Injects chunks loaded in other threads and unloads unused chunks from bullet's world (chunks not used the last tick)
     */
    void tickTerrain();

    /**
     * Loads the collisions of this chunk in the current thread, so it may provoke a little freeze
     * @param ticket The chunk's ticket
     * @param profiler The current profiler
     */
    ChunkCollisions loadChunkCollisionsNow(ChunkLoadingTicket ticket, Profiler profiler);

    /**
     * Asks for the threaded terrain loader to load this chunk (in another thread) and then hotswap it
     * @param ticket The chunk's ticket
     */
    void asyncLoadChunkCollisions(ChunkLoadingTicket ticket);

    /**
     * Called on ChunkEvent.Unload, removes any collision linked to it
     */
    void onChunkUnload(ChunkEvent.Unload e);

    /**
     * Notifies that a chunk will change, and that all async loaded chunks should be received and put in the world now <br>
     * <strong>This SHOULD be called at each {@link ChunkCollisions} modification to avoid loading conflicts</strong>
     */
    void notifyWillChange();

    /**
     * Called on chunk modification (block change), invalidates the collision saved on disk (if exists) and loads the new collision in the {@link ITerrainManager} (while it's loading, the old collision stays active)
     * @param pos The collision chunk's pos
     */
    void onChunkChanged(VerticalChunkPos pos);

    /**
     * Called on WorldEvent.Unload to unload all terrain
     */
    void onWorldUnload();

    /**
     * @return The terrain cache
     */
    ITerrainCache getCache();

    /**
     * @param pos The position of the chunk
     * @return The chunk collisions at this position, or null if not loaded
     */
    @Nullable
    ChunkCollisions getChunkAt(VerticalChunkPos pos);

    /**
     * @param pos The position of the chunk
     * @return The chunk ticket associated with this position (unique). If none exists, it will create one.
     */
    ChunkLoadingTicket getTicket(VerticalChunkPos pos);

    /**
     * Sets the {@link ChunkCollisions} of this ticket and refreshes the physic terrain <br>
     * Fired by the async chunk loader
     * @param ticket The chunk's ticket
     * @param collisions The new chunk's collisions
     */
    void offerLoadedChunk(ChunkLoadingTicket.AsyncLoadedChunk chunk);

    /**
     * Will load the chunk at the given position, depending on the requested priority
     *
     * @param pos The position of the chunk
     * @param priority The priority of loading
     * @param pro
     * @see IPhysicsTerrainLoader
     */
    void subscribeToChunk(VerticalChunkPos pos, ChunkLoadingTicket.TicketPriority priority, Profiler pro);

    /**
     * @return The world associated to this terrain manager
     */
    World getWorld();

    /**
     * @return The physics world associated to this terrain manager
     */
    IPhysicsWorld getPhysicsWorld();

    //TODO DOC
    WorldTerrainState getTerrainState();

    void unsubscribeFromChunk(VerticalChunkPos pos);

    ChunkLoadingTicket removeTicket(VerticalChunkPos pos);
}
