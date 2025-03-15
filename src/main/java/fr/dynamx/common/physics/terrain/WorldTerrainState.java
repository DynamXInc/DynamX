package fr.dynamx.common.physics.terrain;

import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.common.physics.terrain.chunk.ChunkState;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.ChunkGraph;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks the loaded physic terrain chunks and their usage by physic entities
 */
public class WorldTerrainState {
    /**
     * Loaded terrain used by physic entities <br>
     * Maps the chunk pos to the number of using entities
     */
    @Getter
    private final Map<VerticalChunkPos, Short> loadedTerrain = new HashMap<>();
    /**
     * Not in-use chunks, to unload from physics world
     */
    @Getter
    private final Set<VerticalChunkPos> unloadQueue = new HashSet<>();
    /**
     * Chunks that has been unloaded, waiting for the entities in it to be unloaded <br>
     * This avoids unloading the physics terrain before the physic entities are removed and saved
     */
    private final Set<VerticalChunkPos> pendingForInvalidation = new HashSet<>();

    /**
     * @return True of an entity uses this chunk
     */
    public boolean isLoadedAnywhere(VerticalChunkPos pos) {
        return loadedTerrain.containsKey(pos);
    }

    /**
     * Adds an entity to the given chunk, so we know we shouldn't unload it
     *
     * @param world The physics world
     * @param pos   The chunk pos
     */
    public void addSubscriber(IPhysicsWorld world, VerticalChunkPos pos) {
        if (!world.isCallingFromPhysicsThread()) {
            throw new IllegalStateException("Not calling from physics thread ! With " + pos + " : " + world.getTerrainManager().getTicket(pos));
        }
        if (loadedTerrain.containsKey(pos)) {
            loadedTerrain.put(pos, (short) (loadedTerrain.get(pos) + 1));
        } else {
            loadedTerrain.put(pos, (short) 1);
        }
        unloadQueue.remove(pos);
        pendingForInvalidation.remove(pos);
    }

    /**
     * Removes an entity from the given chunk <br>
     * Adds the chunk to the unloadQueue if no one is using it
     *
     * @param world The physics world
     * @param pos   The chunk pos
     */
    public void removeSubscriber(IPhysicsWorld world, VerticalChunkPos pos) {
        if (!world.isCallingFromPhysicsThread()) {
            throw new IllegalStateException("Not calling from physics thread ! With " + pos + " : " + world.getTerrainManager().getTicket(pos));
        }
        if (!loadedTerrain.containsKey(pos)) {
            //Can be called by entities in already unloaded chunks (subscription blocked)
            return;
        }
        short subscriberCount = loadedTerrain.get(pos);
        loadedTerrain.put(pos, (short) (subscriberCount - 1));
        if (subscriberCount <= 1) {
            unloadQueue.add(pos);
        }
    }

    /**
     * Unloads chunks in unloadQueue
     *
     * @param terrain The physics terrain containing the tracked chunks
     */
    public void tick(PhysicsWorldTerrain terrain) {
        //Detect and remove unused chunks
        if (unloadQueue.isEmpty()) {
            return;
        }
        unloadQueue.removeIf((pos) -> {
            ChunkLoadingTicket ticket = terrain.getTicket(pos);
            loadedTerrain.remove(pos);
            // Sometimes, the chunk can be unsubscribed even before it has been loaded
            if (ticket.getStatus() == ChunkState.LOADED) {
                if (ticket.getCollisions() == null) {
                    throw new IllegalStateException("Cannot remove null collisions of " + ticket);
                }
                ticket.getCollisions().removeFromBulletWorld();
                if (ticket.getCollisions().getChunkState().areComputedElementsAdded() || ticket.getCollisions().getChunkState().arePersistentElementsAdded()) {
                    throw new IllegalStateException("Elements still added ! " + ticket.getCollisions() + " wtf " + ticket);
                }
            }
            if (pendingForInvalidation.contains(pos)) {
                onChunkUnload(terrain, pos);
                pendingForInvalidation.remove(pos);
            }
            return true;
        });
    }

    /**
     * Unloads the chunk data from memory if it's unused. <br>
     * It the chunk is still used, it marks it as unloaded, so when it's not used anymore, it will be unloaded from memory
     *
     * @param terrain The physics terrain containing the tracked chunks
     * @param pos     The chunk pos
     */
    public void onChunkUnload(PhysicsWorldTerrain terrain, VerticalChunkPos pos) {
        if (isLoadedAnywhere(pos)) {
            pendingForInvalidation.add(pos);
        } else {
            ChunkLoadingTicket ticket = terrain.removeTicket(pos); //Will cancel current loading processes
            if (terrain.isDebug()) {
                ChunkGraph.addToGrah(ticket.getPos(), ChunkGraph.ChunkActions.CHK_UNLOAD, ChunkGraph.ActionLocation.MAIN, ticket.getCollisions(), "Ticket " + ticket);
            }
            ticket.setUnloaded(); //will prevent concurrent loadings
            terrain.getCache().invalidate(ticket, false, false);
        }
    }

    /**
     * Unloads all chunks
     *
     * @param physicsWorld The physics world
     */
    public void onWorldUnload(IPhysicsWorld physicsWorld) {
        if (!physicsWorld.isCallingFromPhysicsThread()) {
            throw new IllegalStateException("Not calling from physics thread ! When unloading.");
        }
        //Clear loaded chunks
        for (VerticalChunkPos pos : loadedTerrain.keySet()) {
            ChunkLoadingTicket ticket = physicsWorld.getTerrainManager().getTicket(pos);
            if (ticket.getCollisions() == null)
                DynamXMain.log.warn("[World Unload] Cannot remove null collisions of " + ticket);
            else
                ticket.getCollisions().removeFromBulletWorld();
        }
        //Clear lists
        loadedTerrain.clear();
        unloadQueue.clear();
    }
}
