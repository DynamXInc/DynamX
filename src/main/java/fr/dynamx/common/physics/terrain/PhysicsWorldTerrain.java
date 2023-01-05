package fr.dynamx.common.physics.terrain;

import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.api.physics.terrain.DynamXTerrainApi;
import fr.dynamx.api.physics.terrain.ITerrainCache;
import fr.dynamx.api.physics.terrain.ITerrainManager;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.handlers.TaskScheduler;
import fr.dynamx.common.physics.terrain.cache.FileTerrainCache;
import fr.dynamx.common.physics.terrain.cache.RemoteTerrainCache;
import fr.dynamx.common.physics.terrain.cache.TerrainFile;
import fr.dynamx.common.physics.terrain.chunk.ChunkCollisions;
import fr.dynamx.common.physics.terrain.chunk.ChunkLoadingTicket;
import fr.dynamx.common.physics.terrain.chunk.ChunkState;
import fr.dynamx.common.physics.terrain.chunk.DebugChunkCollisions;
import fr.dynamx.common.physics.terrain.element.TerrainElementType;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.ChunkGraph;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.event.world.ChunkEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static fr.dynamx.utils.debug.Profiler.Profiles.*;

/**
 * Main implementation of {@link ITerrainManager}
 */
public class PhysicsWorldTerrain implements ITerrainManager {
    /**
     * The physics world using this terrain
     */
    private final IPhysicsWorld physicsWorld;
    /**
     * The Minecraft world associated to this terrain
     */
    private final World world;
    /**
     * The async terrain loaded
     */
    private final PhysicsTerrainLoader terrainLoader = new PhysicsTerrainLoader(this);
    /**
     * The terrain cache
     */
    private final ITerrainCache terrainCache;

    /**
     * All loaded, cached collisions
     */
    private final ChunkLoadingTicketMap chunkTickets = new ChunkLoadingTicketMap();
    /**
     * The async loaded chunks, ready to be added to the physics world
     */
    private final LinkedBlockingQueue<ChunkLoadingTicket.AsyncLoadedChunk> asyncLoadedQueue = new LinkedBlockingQueue<>();
    private final List<VerticalChunkPos> remove = new ArrayList<>();

    /**
     * todo doc
     */
    private final ConcurrentHashMap<VerticalChunkPos, Byte> scheduledChunkReload = new ConcurrentHashMap<>();

    private final WorldTerrainState terrainState = new WorldTerrainState();

    private final boolean isDebug;

    public PhysicsWorldTerrain(IPhysicsWorld physicsWorld, World world, boolean isRemoteWorld) {
        this.physicsWorld = physicsWorld;
        this.world = world;
        this.terrainCache = isRemoteWorld ? new RemoteTerrainCache(world) : new FileTerrainCache(world);
        this.isDebug = DynamXConfig.enableDebugTerrainManager;
    }

    @Override
    public ChunkLoadingTicket getTicket(VerticalChunkPos pos) {
        ChunkLoadingTicket t = chunkTickets.get(pos);
        if (t != null)
            return t;
        t = new ChunkLoadingTicket(pos);
        chunkTickets.put(pos, t);
        return t;
    }

    /**
     * @return True if this chunk is loaded in the given world
     */
    private boolean isChunkLoaded(World world, int x, int z) {
        return world.isRemote ? world.isChunkGeneratedAt(x, z) : ((ChunkProviderServer) world.getChunkProvider()).chunkExists(x, z);
    }

    @Override
    public void subscribeToChunk(VerticalChunkPos pos, ChunkLoadingTicket.TicketPriority priority, Profiler profiler) {
        profiler.start(GET_T0);
        if (priority == ChunkLoadingTicket.TicketPriority.NONE || !isChunkLoaded(world, pos.x, pos.z)) {
            profiler.end(GET_T0);
            return; //Not loaded in minecraft ? don't load
        }
        profiler.end(GET_T0);
        profiler.start(GET_T1);
        ChunkLoadingTicket ticket = getTicket(pos);
        profiler.end(GET_T1);
        // Second condition : the full terrain is already loaded (HIGH and MEDIUM priorities), or we only request the LOW terrain
        if (ticket.getStatus() == ChunkState.LOADED && (ticket.getPriority() != ChunkLoadingTicket.TicketPriority.LOW || priority == ChunkLoadingTicket.TicketPriority.LOW)) { //Loaded ? Add it to the world
            if (priority.ordinal() > ticket.getPriority().ordinal()) {
                ticket.setPriority(priority);
            }
            profiler.start(GET_T2);
            if (!terrainState.isLoadedAnywhere(ticket.getPos())) { //Not already added to the world
                profiler.end(GET_T2);
                profiler.start(ADD_T3);
                addUsedChunk(ticket, true);
                profiler.end(ADD_T3);
            } else
                profiler.end(GET_T2);
            //Set chunk used after loading it : this will avoid weird sync errors
            subscribeToChunk(ticket);
        } else if (ticket.getStatus() == ChunkState.NONE) { //Not loaded ? Load it
            if (isDebug) {
                ChunkGraph.addToGrah(ticket.getPos(), ChunkGraph.ChunkActions.INITIATE_LOAD, ChunkGraph.ActionLocation.MAIN, null, ticket + " " + priority);
            }
            ticket.setLoading();
            ticket.setPriority(priority);
            switch (priority) {
                case LOW:
                case MEDIUM:
                    //Set chunk used before loading it : this will avoid weird async errors
                    subscribeToChunk(ticket);
                    asyncLoadChunkCollisions(ticket);
                    break;
                case HIGH:
                    profiler.start(LOAD_NOW);
                    loadChunkCollisionsNow(ticket, profiler);
                    addUsedChunk(ticket, true);
                    //Set chunk used after loading it : this will avoid weird sync errors
                    subscribeToChunk(ticket);
                    profiler.end(LOAD_NOW);
                    break;
            }
        }
        //Loading but in a lower priority ? Or loaded but only with slopes ? Load it
        else if ((ticket.getStatus() == ChunkState.LOADING || ticket.getStatus() == ChunkState.LOADED) && priority.ordinal() > ticket.getPriority().ordinal()) {
            if (isDebug)
                ChunkGraph.addToGrah(ticket.getPos(), ChunkGraph.ChunkActions.OVERRIDE_LOAD, ChunkGraph.ActionLocation.MAIN, null, ticket + " " + priority);
            ticket.setPriority(priority);
            if (ticket.getStatus() == ChunkState.LOADED)
                ticket.setLoading();
            else
                ticket.incrStatusIndex("Higher priority"); //This will cancel current loading operations
            //Set chunk used before loading it : this will avoid weird async errors
            subscribeToChunk(ticket);
            switch (priority) {
                case MEDIUM:
                    asyncLoadChunkCollisions(ticket);
                    break;
                case HIGH:
                    profiler.start(LOAD_NOW);
                    loadChunkCollisionsNow(ticket, Profiler.get());
                    addUsedChunk(ticket, false);
                    profiler.end(LOAD_NOW);
                    break;
            }
        } else {
            //else it's loading
            subscribeToChunk(ticket);
        }
        if (!asyncLoadedQueue.isEmpty()) {
            profiler.start(RCV_ASYNC);
            receiveAsyncLoadedChunks();
            profiler.end(RCV_ASYNC);
        }
    }

    @Override
    public void unsubscribeFromChunk(VerticalChunkPos pos) {
        if (!physicsWorld.isCallingFromPhysicsThread()) {
            physicsWorld.schedule(() -> unsubscribeFromChunk(pos));
        } else {
            terrainState.removeSubscriber(physicsWorld, pos);
        }
    }

    /**
     * Adds the given chunk to the physics world, checking its loaded state
     */
    private void addUsedChunk(ChunkLoadingTicket ticket, boolean checkNotLoaded) {
        Profiler.get().start(ADD_USED);
        if (ticket.getStatus() == ChunkState.LOADED && ticket.getPriority() != ChunkLoadingTicket.TicketPriority.NONE) {
            ChunkCollisions collisions = ticket.getCollisions();
            //terrainState.addSubscriber(physicsWorld, ticket.getPos());
            if (!checkNotLoaded || !terrainState.isLoadedAnywhere(ticket.getPos())) {
                if (ticket.getPriority() == ChunkLoadingTicket.TicketPriority.LOW) {
                    if (!collisions.getChunkState().arePersistentElementsAdded()) {
                        collisions.addToBulletWorld(physicsWorld, TerrainElementType.PERSISTENT_ELEMENTS, Profiler.get());
                    }
                } else {
                    if (collisions.getChunkState().areComputedElementsAdded()) {
                        DynamXMain.log.fatal("[0x2] Chunk is already added " + collisions + " " + collisions.getChunkState() + " " + ticket);
                        ChunkGraph c = ChunkGraph.getAt(ticket.getPos());
                        if (c != null) {
                            System.out.println("Graph will be print :");
                            c.prettyPrint();
                        } else {
                            System.out.println("Graph not found !");
                        }
                        if (!DynamXConfig.ignoreDangerousTerrainErrors) {
                            throw new IllegalStateException("[0x2] Chunk is already added " + collisions + " " + collisions.getChunkState() + " " + ticket);
                        }
                    }
                    collisions.addToBulletWorld(physicsWorld, Profiler.get());
                }
            } else { //If it was already added
                DynamXMain.log.fatal("[0x1] Found an invalid ticket state " + ticket + ". Send your log to Aym' ! 0x2", new IllegalStateException("Chunk " + collisions + " already loaded ! UnloadQueue " + terrainState.getUnloadQueue() + " Loaded " + terrainState.getLoadedTerrain()));
                ChunkGraph c = ChunkGraph.getAt(ticket.getPos());
                if (c != null) {
                    System.out.println("Graph will be print :");
                    c.prettyPrint();
                } else {
                    System.out.println("Graph not found !");
                }
                if (!DynamXConfig.ignoreDangerousTerrainErrors) {
                    throw new IllegalStateException("[0x1] Chunk " + collisions + " already loaded ! UnloadQueue " + terrainState.getUnloadQueue() + " Loaded " + terrainState.getLoadedTerrain());
                }
            }
        } else { //Incorrect ticket state (not loaded)
            DynamXMain.log.fatal("Found an invalid ticket state " + ticket + ". Send your log to Aym' ! 0 x1", new IllegalStateException("Bad ticket state " + ticket));
            ChunkGraph c = ChunkGraph.getAt(ticket.getPos());
            if (c != null) {
                System.out.println("Graph will be print :");
                c.prettyPrint();
            } else {
                System.out.println("Graph not found !");
            }
            if (!DynamXConfig.ignoreDangerousTerrainErrors)
                throw new IllegalStateException("Bad ticket state " + ticket);
        }

        Profiler.get().end(ADD_USED);
    }

    /**
     * Will keep this chunk loaded for the next tick
     * todo doc
     */
    private void subscribeToChunk(ChunkLoadingTicket ticket) {
        terrainState.addSubscriber(physicsWorld, ticket.getPos());
    }

    /**
     * Processes async loaded collisions, and adds them to the physics world
     */
    private void receiveAsyncLoadedChunks() {
        while (!asyncLoadedQueue.isEmpty()) {
            ChunkLoadingTicket.AsyncLoadedChunk toffer = asyncLoadedQueue.poll();
            if (toffer == null) return; //We aren't safe from weird multithreading errors
            if (!toffer.getSnap().isValid()) //The received version isn't valid anymore
            {
                if (DynamXConfig.enableDebugTerrainManager)
                    DynamXMain.log.error("[PWT] Ignored async loaded chunk, new request sent " + toffer.getSnap().getTicket() + " " + toffer.getSnap().getSnapIndex());
                return;
            }
            ChunkCollisions offer = toffer.getCollisionsIn();
            ChunkLoadingTicket ticket = toffer.getSnap().getTicket();
            if (isDebug)
                ChunkGraph.addToGrah(ticket.getPos(), ChunkGraph.ChunkActions.HOTSWAP, ChunkGraph.ActionLocation.MAIN, offer, "ASYNC LOAD Ticket " + ticket + " " + toffer.getSnap().getSnapIndex());
            ticket.incrStatusIndex("Loaded async"); //Invalidate other loading operations
            ticket.setLoaded(terrainState, offer);  //Will remove the previous chunk from loaded terrain
            if (getTerrainState().isLoadedAnywhere(offer.getPos()))
                addUsedChunk(ticket, false); //Add the new chunk to the physics world. FIX : ONLY IF IT'S USED
            ticket.fireLoadedCallback(); //Call this after adding the chunk : the callback may ask for a new load of this ticket
        }
    }

    @Override
    public void tickTerrain() {
        //Tick terrain state
        terrainState.tick(this);
        //Tick cache
        terrainCache.tick();
        if (!asyncLoadedQueue.isEmpty()) {
            Profiler.get().start(RCV_ASYNC);
            Vector3fPool.openPool();
            receiveAsyncLoadedChunks();
            Vector3fPool.closePool();
            Profiler.get().end(RCV_ASYNC);
        }
        //Tick terrain loaders (as the slopes item)
        DynamXTerrainApi.getCustomTerrainLoaders().forEach(l -> l.update(this, Profiler.get()));
        //Process chunk changes
        for (Map.Entry<VerticalChunkPos, Byte> en : scheduledChunkReload.entrySet()) {
            byte state = en.getValue();
            if (state == 1) {
                remove.add(en.getKey());
                scheduledChunkReload.remove(en.getKey());
                onChunkChanged(en.getKey());
            } else
                en.setValue((byte) (state - 1));
        }
        if (!remove.isEmpty()) {
            remove.forEach(scheduledChunkReload::remove);
            remove.clear();
        }
    }

    @Override
    public ChunkCollisions loadChunkCollisionsNow(ChunkLoadingTicket ticket, Profiler profiler) {
        profiler.start(Profiler.Profiles.EMERGENCY_CHUNK_LOAD);
        VerticalChunkPos pos = ticket.getPos();
        ChunkCollisions coll = isDebug ? new DebugChunkCollisions(getWorld(), pos, getPhysicsWorld()) : new ChunkCollisions(getWorld(), pos);
        if (isDebug)
            ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.LOAD_NOW, ChunkGraph.ActionLocation.MAIN, coll, "Ticket " + ticket);
        ticket.incrStatusIndex("Load now"); //When we start to load the chunk, we can consider it's the more up-to-date version, and we are in the physics thread, so very good
        coll.loadCollisionsSync(this, getCache(), ticket, Vector3fPool.get(pos.x * 16, pos.y * 16, pos.z * 16), profiler);
        ticket.fireLoadedCallback(); //Call this after adding the chunk : the callback may ask for a new load of this ticket
        profiler.end(Profiler.Profiles.EMERGENCY_CHUNK_LOAD);
        if (isDebug)
            ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.HOTSWAP, ChunkGraph.ActionLocation.MAIN, coll, "SYNCED LOAD Ticket " + ticket);
        return coll;
    }

    @Override
    public void asyncLoadChunkCollisions(ChunkLoadingTicket ticket) {
        terrainLoader.asyncLoadChunk(ticket.snapshot());
    }

    @Override
    public void offerLoadedChunk(ChunkLoadingTicket.AsyncLoadedChunk chunk) {
        asyncLoadedQueue.add(chunk);
    }

    @Override
    public void onChunkUnload(ChunkEvent.Unload e) {
        TaskScheduler.schedule(new TaskScheduler.ScheduledTask((short) 40) {
            @Override
            public void run() {
                physicsWorld.schedule(() -> {
                    for (int y = 0; y < 16; y++) {
                        VerticalChunkPos pos = new VerticalChunkPos(e.getChunk().x, y, e.getChunk().z);
                        if (chunkTickets.containsKey(pos))
                            terrainState.onChunkUnload(PhysicsWorldTerrain.this, pos);
                    }
                });
            }
        });
    }

    @Override
    public ChunkLoadingTicket removeTicket(VerticalChunkPos pos) {
        return chunkTickets.remove(pos);
    }

    @Override
    public void notifyWillChange() {
        if (!DynamXContext.getPhysicsWorld(world).isCallingFromPhysicsThread())
            DynamXContext.getPhysicsWorld(world).schedule(this::notifyWillChangeInternal);
        else
            notifyWillChangeInternal();
    }

    private void notifyWillChangeInternal() {
        //Receive async loaded chunks, so previous loading results don't interfere with this chunk change
        receiveAsyncLoadedChunks();
    }

    @Override
    public void onChunkChanged(VerticalChunkPos pos) {
        if (!DynamXContext.getPhysicsWorld(world).isCallingFromPhysicsThread())
            DynamXContext.getPhysicsWorld(world).schedule(() -> onChunkChangedInternal(pos));
        else
            onChunkChangedInternal(pos);
    }

    private void onChunkChangedInternal(VerticalChunkPos pos) {
        notifyWillChangeInternal();
        if (chunkTickets.containsKey(pos)) { //If the chunk is loaded
            ChunkLoadingTicket ticket = chunkTickets.get(pos);
            if (isDebug)
                ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.CHK_UPDATE, ChunkGraph.ActionLocation.MAIN, ticket.getCollisions(), "Chunk changed. Ticket " + ticket);
            if (ticket.getStatus() != ChunkState.LOADING) { //If not loading
                terrainCache.invalidate(ticket, true, true); //Invalidate the cache
                //If chunk terrain is loaded
                if (ticket.getStatus() != ChunkState.NONE && ticket.getPriority().ordinal() > ChunkLoadingTicket.TicketPriority.NONE.ordinal()) {
                    //If it's not loading in the main physics thread
                    if (ticket.getStatus() != ChunkState.LOADING || ticket.getPriority() != ChunkLoadingTicket.TicketPriority.HIGH) {
                        ticket.setLoading(); //Reload it, async
                        asyncLoadChunkCollisions(ticket);
                    }
                }
            } else { //If it's loading, it's iznogood
                if (DynamXConfig.enableDebugTerrainManager) {
                    DynamXMain.log.error("This chunk is still loading, wtf " + ticket);
                    ChunkGraph graph = ChunkGraph.getAt(pos);
                    if (graph != null) {
                        System.out.println("Printing graph " + pos);
                        graph.prettyPrint();
                    } else {
                        System.out.println("Graph not found :/");
                    }
                }
            }
        } else {  //Else just notify of the change
            terrainCache.invalidate(pos, true, true);
        }
    }

    @Nullable
    @Override
    public ChunkCollisions getChunkAt(VerticalChunkPos cp) {
        return chunkTickets.containsKey(cp) ? getTicket(cp).getCollisions() : null;
    }

    @Override
    public void onWorldUnload() {
        //Clear loader
        terrainLoader.onWorldUnload();
        //Clear loaded chunks
        terrainState.onWorldUnload(getPhysicsWorld());
        //Clear caches
        chunkTickets.clear();
        terrainCache.clear();
    }

    @Override
    public IPhysicsWorld getPhysicsWorld() {
        return physicsWorld;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public ITerrainCache getCache() {
        return terrainCache;
    }

    @Override
    public WorldTerrainState getTerrainState() {
        return terrainState;
    }

    public boolean isDebug() {
        return isDebug;
    }

    /**
     * Marks the physics terrain dirty and schedule a new computation <br>
     * Don't abuse as it may create some lag
     *
     * @param world The world
     * @param pos   The modified position. The corresponding chunk will be reloaded
     */
    public void onBlockChange(World world, BlockPos pos) {
        scheduledChunkReload.put(new VerticalChunkPos(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4), (byte) 10);
    }
}
