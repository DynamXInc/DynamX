package fr.dynamx.common.physics.terrain.chunk;

import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.physics.terrain.WorldTerrainState;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.ChunkGraph;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the status of a {@link ChunkCollisions}. This ticket is unique for each {@link VerticalChunkPos}. <br>
 * The status can be one of {@link ChunkState}. <br>
 * The {@link fr.dynamx.api.physics.terrain.ITerrainManager} will handle the loading of this chunk, checking for the {@link TicketPriority} of this ticket and its current state.
 */
public class ChunkLoadingTicket implements VerticalChunkPos.VerticalChunkPosContainer
{
    private final VerticalChunkPos pos;

    private ChunkCollisions collisions;
    private CompletableFuture<ChunkCollisions> loadedCallback;

    private TicketPriority priority = TicketPriority.NONE;
    private ChunkState status = ChunkState.NONE;

    private int statusIndex;

    public ChunkLoadingTicket(VerticalChunkPos pos) {
        this.pos = pos;
    }

    /**
     * Sets the loading priority of the chunk
     */
    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    /**
     * @return The loading priority of this chunk
     */
    public TicketPriority getPriority() {
        return priority;
    }

    /**
     * @return The status of this chunk
     */
    public ChunkState getStatus() {
        return status;
    }

    /**
     * @return The status index of this ticket
     * @see Snap
     */
    public int getStatusIndex() {
        return statusIndex;
    }

    //private final List<String> indexChangers = new ArrayList<>();

    /**
     * Increments the status index of this ticket, stopping all running loading operations based on snapshots.
     * @see Snap
     */
    public void incrStatusIndex(String from) {
       //indexChangers.add(from);
        statusIndex++;
    }

    /*public List<String> getIndexChangers() {
        return indexChangers;
    }*/

    /**
     * @return The position of this ticket. It's the unique identifier of this ticket.
     */
    public VerticalChunkPos getPos() {
        return pos;
    }

    /**
     * @return A new snapshot of this ticket
     */
    public Snap snapshot() {
        return new Snap(statusIndex);
    }

    /**
     * @return The last loaded collisions data of this chunk
     */
    @Nullable
    public ChunkCollisions getCollisions() {
        return collisions;
    }

    /**
     * Marks this chunk as loading and cancels any previous loading operation <br>
     * Also creates a new loaded callback
     */
    public void setLoading() {
        incrStatusIndex("Set loading");
        this.loadedCallback = new CompletableFuture<>();
        this.status = ChunkState.LOADING;
        if(DynamXConfig.enableDebugTerrainManager)
            ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.SET_LOADING, ChunkGraph.ActionLocation.UNKNOWN, null, ""+this);
    }

    /**
     * @return The loaded callback of the chunk, where further operations can be added
     */
    public CompletableFuture<ChunkCollisions> getLoadedCallback() {
        return loadedCallback;
    }

    /**
     * Marks this chunk as loaded an updates contained chunk collisions <br>
     * <strong>Note : fireLoadedCallback is not called by this function</strong>
     */
    public void setLoaded(WorldTerrainState terrainState, ChunkCollisions collisions) {
        setCollisions(terrainState, collisions);
        this.status = ChunkState.LOADED;
        if(DynamXConfig.enableDebugTerrainManager)
            ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.SET_LOADED, ChunkGraph.ActionLocation.UNKNOWN, null, this+" "+(collisions != null ? collisions.getElements().getElements().size()+" / "+collisions.getElements().getPersistentElements().size() : "null coll"));
    }

    /**
     * Fires the loading callback of this chunk <br>
     * It should be called separately from the setLoaded method, as it may be followed by a new loading operation on this chunk (async loading for clients)
     */
    public void fireLoadedCallback() {
        if(loadedCallback != null)
            loadedCallback.complete(collisions);
    }

    /**
     * Marks this chunks as unloaded, and clears the contained data
     */
    public void setUnloaded(WorldTerrainState terrainState) {
        if(DynamXConfig.enableDebugTerrainManager)
            ChunkGraph.addToGrah(getPos(), ChunkGraph.ChunkActions.DESTROY, ChunkGraph.ActionLocation.MAIN, getCollisions());
        incrStatusIndex("Set unloaded");
        if(loadedCallback != null) //complete with old collisions
            loadedCallback.complete(collisions);
        setCollisions(terrainState, null);
        status = ChunkState.NONE;
        priority = ChunkLoadingTicket.TicketPriority.NONE;
    }

    private void setCollisions(WorldTerrainState terrainState, ChunkCollisions collisions) {
        if(collisions != this.collisions) {
            if(this.collisions != null) {
                this.collisions.removeFromBulletWorld(DynamXContext.getPhysicsWorld());
                if(this.collisions.getChunkState().areComputedElementsAdded()|| this.collisions.getChunkState().arePersistentElementsAdded()) {
                    throw new IllegalStateException("Elements still added ! "+this.collisions+" wtf "+this);
                }
                this.collisions.reset();
            }
        }
        this.collisions = collisions;
    }

    @Override
    public boolean equals(Object o) {
        return posEquals(o);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }

    @Override
    public String toString() {
        return "ChunkLoadingTicket{" +
                "pos=" + pos +
                ", collisions=" + collisions +
                ", priority=" + priority +
                ", status=" + status +
                ", statusIndex=" + statusIndex +
                '}';
    }

    /**
     * Chunk loading priorities
     */
    public enum TicketPriority
    {
        /**
         * No loading
         */
        NONE,
        /**
         * Only load slopes (async)
         */
        LOW,
        /**
         * Async load complete terrain
         */
        MEDIUM,
        /**
         * Load complete terrain (in the physics thread)
         */
        HIGH
    }

    /**
     * A snapshot of this ticket, typically taken when beginning a loading of this chunk <br>
     * If the status index of the chunk is different, then another loading was started so this snapshot, and the associated loading operation, can be discarded
     */
    public class Snap implements VerticalChunkPos.VerticalChunkPosContainer
    {
        private final int snapIndex;

        public Snap(int snapIndex) {
            this.snapIndex = snapIndex;
        }

        /**
         * @return True if this snapshot is still valid
         */
        public boolean isValid() {
            /*if(snapIndex != ChunkLoadingTicket.this.statusIndex) {
                System.out.println("Snapshot not valid anymore ! Index "+this.snapIndex+" of "+this.getTicket());
            }*/
            return snapIndex == ChunkLoadingTicket.this.statusIndex;
        }

        /**
         * @return The ticket associated with this snapshot
         */
        public ChunkLoadingTicket getTicket() {
            return ChunkLoadingTicket.this;
        }

        /**
         * @return The status index of the ticket when the snapshot was taken
         */
        public int getSnapIndex() {
            return snapIndex;
        }

        @Override
        public boolean equals(Object o) {
            return posEquals(o);
        }

        @Override
        public int hashCode() {
            return ChunkLoadingTicket.this.hashCode();
        }

        @Override
        public VerticalChunkPos getPos() {
            return getTicket().getPos();
        }
    }

    /**
     * Wrapper class for data of an async loaded chunk
     */
    public static class AsyncLoadedChunk
    {
        private final Snap snap;
        private final ChunkCollisions collisionsIn;

        public AsyncLoadedChunk(Snap snap, ChunkCollisions collisions) {
            this.snap = snap;
            this.collisionsIn = collisions;
        }

        /**
         * @return The loaded collisions
         */
        public ChunkCollisions getCollisionsIn() {
            return collisionsIn;
        }

        /**
         * @return The snapshot associated with this loading operation
         */
        public Snap getSnap() {
            return snap;
        }
    }
}
