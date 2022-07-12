package fr.dynamx.common.physics.terrain;

import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.common.physics.terrain.chunk.ChunkState;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.ChunkGraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WorldTerrainState //TODO INTERFACE
{
    //TODO DOC
    private final Map<VerticalChunkPos, Short> loadedTerrain = new HashMap<>();
    private final Set<VerticalChunkPos> unloadQueue = new HashSet<>();
    private final Set<VerticalChunkPos> pendingForInvalidation = new HashSet<>();

    public boolean isLoadedAnywhere(VerticalChunkPos pos) {
        return loadedTerrain.containsKey(pos);
    }

    public void addSubscriber(IPhysicsWorld world, VerticalChunkPos pos) {
        if (!world.isCallingFromPhysicsThread()) {
            throw new IllegalStateException("Not calling from physics thread ! With " + pos + " : " + world.getTerrainManager().getTicket(pos));
        }
        if (loadedTerrain.containsKey(pos))
            loadedTerrain.put(pos, (short) (loadedTerrain.get(pos) + 1));
        else
            loadedTerrain.put(pos, (short) 1);
        unloadQueue.remove(pos);
        pendingForInvalidation.remove(pos);
    }

    public void removeSubscriber(IPhysicsWorld world, VerticalChunkPos pos) {
        if (!world.isCallingFromPhysicsThread()) {
            throw new IllegalStateException("Not calling from physics thread ! With " + pos + " : " + world.getTerrainManager().getTicket(pos));
        }
        if (!loadedTerrain.containsKey(pos)) {
            //Entities in unloaded chunks (subscription blocked)
            return;
        }
        short val = loadedTerrain.get(pos);
        loadedTerrain.put(pos, (short) (val - 1));
        if (val <= 1)
            unloadQueue.add(pos);
    }

    public void tick(PhysicsWorldTerrain terrain) {
        //Detect and remove unused chunks
        if (!unloadQueue.isEmpty()) {
            unloadQueue.removeIf((pos) -> {
                ChunkLoadingTicket ticket = terrain.getTicket(pos);
                if (ticket.getStatus() == ChunkState.LOADING) {
                    //System.out.println("Keep while loading " + ticket);
                    //TODO SOLVE THIS : SHOULD NOT BE CALLED FOR CHUNKS LOADING WITH ENTITIES NEAR

                    //ticket.incrStatusIndex("Unloaded_no entity"); //Invalidate loading
                    //28/07/21 : WTF WHY, KEEP LOADED WHILE LOADING, THEN CHECK IF IT SHOULD BE REMOVED
                    return false; //But mark as used to avoid possible errors. It will be removed later.
                    //Will be removed when loading is finished
                } else {
                    if (ticket.getCollisions() == null) {
                        throw new IllegalStateException("Cannot remove null collisions of " + ticket);
                    }
                    ticket.getCollisions().removeFromBulletWorld(terrain.getPhysicsWorld());
                    if (ticket.getCollisions().getChunkState().areComputedElementsAdded() || ticket.getCollisions().getChunkState().arePersistentElementsAdded()) {
                        throw new IllegalStateException("Elements still added ! " + ticket.getCollisions() + " wtf " + ticket);
                    }
                    loadedTerrain.remove(pos);
                    if(pendingForInvalidation.contains(pos)) {
                        onChunkUnload(terrain, pos);
                        pendingForInvalidation.remove(pos);
                    }
                    return true;
                }
            });
        }
    }

    public void onChunkUnload(PhysicsWorldTerrain terrain, VerticalChunkPos pos) {
        if(isLoadedAnywhere(pos)) {
            pendingForInvalidation.add(pos);
        } else {
            ChunkLoadingTicket ticket = terrain.removeTicket(pos); //Will cancel current loading processes
            if (terrain.isDebug())
                ChunkGraph.addToGrah(ticket.getPos(), ChunkGraph.ChunkActions.CHK_UNLOAD, ChunkGraph.ActionLocation.MAIN, ticket.getCollisions(), "Ticket " + ticket);
            ticket.setUnloaded(this); //will prevent loadings
            terrain.getCache().invalidate(ticket, false, false);
            //unload the collision.
        }
    }

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
                ticket.getCollisions().removeFromBulletWorld(physicsWorld);
        }
        //Clear lists
        loadedTerrain.clear();
        unloadQueue.clear();
    }

    public Map<VerticalChunkPos, Short> getLoadedTerrain() {
        return loadedTerrain;
    }

    public Set<VerticalChunkPos> getUnloadQueue() {
        return unloadQueue;
    }
}
