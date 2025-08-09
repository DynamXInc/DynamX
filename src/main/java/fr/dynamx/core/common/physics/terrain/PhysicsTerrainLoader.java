package fr.dynamx.core.common.physics.terrain;

import fr.aym.acslib.services.impl.thrload.DynamXThreadedModLoader;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.physics.terrain.chunk.ChunkCollisions;
import fr.dynamx.core.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.core.common.physics.terrain.chunk.DebugChunkCollisions;
import fr.dynamx.core.utils.VerticalChunkPos;
import fr.dynamx.core.utils.debug.ChunkGraph;
import fr.dynamx.core.utils.debug.Profiler;
import fr.dynamx.core.utils.optimization.Vector3fPool;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Async loader for quicker loading of {@link ChunkCollisions}
 */
public class PhysicsTerrainLoader {
    private static final ThreadFactory factory = new DynamXThreadedModLoader.DefaultThreadFactory("DnxTerrainLoader");
    private final ExecutorService POOL = Executors.newFixedThreadPool(1, factory);
    private final PhysicsWorldTerrain manager;
    private final Profiler profiler = new Profiler();

    protected PhysicsTerrainLoader(PhysicsWorldTerrain manager) {
        this.manager = manager;
    }

    /**
     * Appends a chunk to the loading queue
     */
    public void asyncLoadChunk(ChunkLoadingTicket.Snap chunk) {
        //System.out.println("Async load. status "+POOL.isTerminated()+" "+chunk.getTicket()+" "+chunk.getSnapIndex());
        if (!POOL.isTerminated()) {
            POOL.submit(() -> loadChunk(chunk));
        }
    }

    /**
     * Stops any thread and clears queues
     */
    public void onWorldUnload() {
        POOL.shutdownNow();
    }

    private void loadChunk(ChunkLoadingTicket.Snap chk) {
        VerticalChunkPos lookingAt = chk.getTicket().getPos();
        try {
            if (chk.isValid()) { //If loading ticket is still valid
                profiler.start(Profiler.Profiles.TERRAIN_LOADER_TICK);
                ChunkCollisions collision = manager.isDebug() ? new DebugChunkCollisions(manager.getWorld(), lookingAt) : new ChunkCollisions(manager.getWorld(), lookingAt);
                if (manager.isDebug()) {
                    ChunkGraph.addToGrah(lookingAt, ChunkGraph.ChunkActions.LOAD_ASYNC, ChunkGraph.ActionLocation.LOADER, collision, "Ticket " + chk.getTicket() + " " + chk.isValid());
                }
                if (!chk.isValid()) {
                    if (manager.isDebug()) {
                        DynamXMain.log.warn("Aborting load at {}", lookingAt);
                    }
                    profiler.end(Profiler.Profiles.TERRAIN_LOADER_TICK);
                    return;
                }
                chk.getTicket().incrStatusIndex(); //Invalidate other loading processes
                Vector3fPool.openPool();
                collision.loadCollisionsAsync(manager, manager.getCache(), chk.getTicket(), Vector3fPool.get(lookingAt.x * 16, lookingAt.y * 16, lookingAt.z * 16)).exceptionally(e -> {
                    DynamXMain.log.fatal("Failed to async-load chunk {}", chk.getTicket(), e);
                    return null;
                });
                Vector3fPool.closePool();
                profiler.end(Profiler.Profiles.TERRAIN_LOADER_TICK);
                profiler.update();
            }
            if (profiler.isActive()) { //Profiling
                List<String> st = profiler.getData();
                if (!st.isEmpty()) {
                    profiler.printData("Terrain thread");
                    profiler.reset();
                }
            }
        } catch (Exception e1) {
            DynamXMain.log.fatal("Chunk error at {}", lookingAt, e1);
        }
    }
}
