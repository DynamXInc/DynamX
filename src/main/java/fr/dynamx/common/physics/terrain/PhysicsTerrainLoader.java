package fr.dynamx.common.physics.terrain;

import fr.aym.acslib.services.impl.thrload.DynamXThreadedModLoader;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.physics.terrain.chunk.ChunkCollisions;
import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.common.physics.terrain.chunk.DebugChunkCollisions;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.ChunkGraph;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.BoundingBoxPool;
import fr.dynamx.utils.optimization.Vector3fPool;

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
                //long start = System.currentTimeMillis();
                //System.out.println("ASYNC LOADING ZOFIPZ?QCOFPSF? "+lookingAt+" "+chk.getTicket().getPriority());
                profiler.start(Profiler.Profiles.TERRAIN_LOADER_TICK);
                Vector3fPool.openPool();
                BoundingBoxPool.getPool().openSubPool();
                ChunkCollisions collision = manager.isDebug() ? new DebugChunkCollisions(manager.getWorld(), lookingAt, manager.getPhysicsWorld()) : new ChunkCollisions(manager.getWorld(), lookingAt);
                if (manager.isDebug())
                    ChunkGraph.addToGrah(lookingAt, ChunkGraph.ChunkActions.LOAD_ASYNC, ChunkGraph.ActionLocation.LOADER, collision, "Ticket " + chk.getTicket() + " " + chk.isValid());
                if (!chk.isValid()) {
                    if (manager.isDebug())
                        DynamXMain.log.warn("Aborting load at " + lookingAt);
                    Vector3fPool.closePool();
                    BoundingBoxPool.getPool().closeSubPool();
                    profiler.end(Profiler.Profiles.TERRAIN_LOADER_TICK);
                    return;
                }
                chk.getTicket().incrStatusIndex("Loading async"); //Invalidate other loading processes
                collision.loadCollisionsAsync(manager, manager.getCache(), chk.getTicket(), Vector3fPool.get(lookingAt.x * 16, lookingAt.y * 16, lookingAt.z * 16)).exceptionally(e -> {
                    DynamXMain.log.fatal("Failed to async-load chunk " + chk.getTicket(), e);
                    return null;
                });
                Vector3fPool.closePool();
                BoundingBoxPool.getPool().closeSubPool();
                profiler.end(Profiler.Profiles.TERRAIN_LOADER_TICK);
                profiler.update();

                //System.out.println("ASYNC LOADED C "+lookingAt+" "+(System.currentTimeMillis()-start)+" ms");
            } else {
                //System.err.println("C NOT VALID "+lookingAt+" "+chk.getTicket()+" "+chk.getSnapIndex());
            }
            if (profiler.isActive()) //Profiling
            {
                List<String> st = profiler.getData();
                if (!st.isEmpty()) {
                    profiler.printData("Terrain thread");
                    profiler.reset();
                }
            }
        } catch (Exception e1) {
            DynamXMain.log.fatal("Chunk error at " + lookingAt, e1);
        }
    }
}
