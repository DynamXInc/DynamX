package fr.dynamx.common.physics.terrain.chunk;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import fr.dynamx.api.events.PhysicsEvent;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.api.physics.terrain.ITerrainCache;
import fr.dynamx.api.physics.terrain.ITerrainElement;
import fr.dynamx.api.physics.terrain.ITerrainManager;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.terrain.computing.TerrainCollisionsCalculator;
import fr.dynamx.common.physics.terrain.cache.TerrainFile;
import fr.dynamx.common.physics.terrain.element.TerrainElementType;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.DynamXReflection;
import fr.dynamx.utils.VerticalChunkPos;
import fr.dynamx.utils.debug.ChunkGraph;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.optimization.BoundingBoxPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The collision of a chunk, see {@link ITerrainManager}
 *
 * @see ChunkLoadingTicket
 */
public class ChunkCollisions implements VerticalChunkPos.VerticalChunkPosContainer
{
    private final VerticalChunkPos myPos;
    private final World mcWorld;

    /**
     * State of this chunk, avoids inconsistencies due to threaded loading
     */
    private EnumChunkCollisionsState state = EnumChunkCollisionsState.INVALID;

    /**
     * The loaded elements of this chunk
     */
    private final ChunkTerrain elements = new ChunkTerrain();

    /**
     * Max size of this chunk, used for large and fat slopes <br>
     * WIP
     */
    private int[] maxSize = ITerrainElement.DEFAULT_SIZE;

    public ChunkCollisions(World mcWorld, VerticalChunkPos pos)
    {
        this.myPos = pos;
        this.mcWorld = mcWorld;
        if(DynamXConfig.enableDebugTerrainManager)
            ChunkGraph.addToGrah(pos, ChunkGraph.ChunkActions.CREATE_INSTANCE, ChunkGraph.ActionLocation.UNKNOWN, this);
        if(state != EnumChunkCollisionsState.INVALID)
            reset();
        setChunkState(EnumChunkCollisionsState.INITIALIZED);
    }

    /**
     * @return Max size of this chunk, used for large and fat slopes
     * TODO use
     */
    public int[] getMaxSize() {
        return maxSize;
    }

    /**
     * @return The state of this chunk, avoids inconsistencies due to threaded loading
     */
    public EnumChunkCollisionsState getChunkState() {
        return state;
    }

    /**
     * Sets the state of this chunk, avoids inconsistencies due to threaded loading
     */
    public void setChunkState(EnumChunkCollisionsState state) {
        if(state != this.state)
            MinecraftForge.EVENT_BUS.post(new PhysicsEvent.ChunkCollisionsStateEvent(DynamXContext.getPhysicsWorld(), this, state));
        this.state = state;
    }

    /**
     * Sets the new chunk state of this chunk according to the terrainType to add and the previous state of this chunk
     */
    protected void transitionToAddedState(TerrainElementType terrainType) {
        boolean stateValid = state == EnumChunkCollisionsState.COMPUTED || (terrainType == TerrainElementType.PERSISTENT_ELEMENTS && state == EnumChunkCollisionsState.ADDED_COMPUTED) ||
                (terrainType == TerrainElementType.COMPUTED_TERRAIN && state == EnumChunkCollisionsState.ADDED_PERSISTENT);
        if (stateValid) {
            switch (terrainType) {
                case ALL:
                    setChunkState(EnumChunkCollisionsState.ADDED_ALL);
                case COMPUTED_TERRAIN:
                    if(state == EnumChunkCollisionsState.ADDED_PERSISTENT)
                        setChunkState(EnumChunkCollisionsState.ADDED_ALL);
                    else
                        setChunkState(EnumChunkCollisionsState.ADDED_COMPUTED);
                case PERSISTENT_ELEMENTS:
                    if(state == EnumChunkCollisionsState.ADDED_COMPUTED)
                        setChunkState(EnumChunkCollisionsState.ADDED_ALL);
                    else
                        setChunkState(EnumChunkCollisionsState.ADDED_PERSISTENT);
            }
        }
        else {
            throw new IllegalStateException("Invalid transition of " + getPos() + " from " + state + " to ADDED with terrain type "+terrainType);
        }
    }

    /**
     * Sets the new chunk state of this chunk according to the terrainType to remove and the previous state of this chunk
     */
    protected void transitionToRemovedState(TerrainElementType terrainType) {
        boolean stateValid = state == EnumChunkCollisionsState.ADDED_ALL || (terrainType == TerrainElementType.PERSISTENT_ELEMENTS && state == EnumChunkCollisionsState.ADDED_PERSISTENT) ||
                (terrainType == TerrainElementType.COMPUTED_TERRAIN && state == EnumChunkCollisionsState.ADDED_COMPUTED);
        if (stateValid) {
            switch (terrainType) {
                case ALL:
                    setChunkState(EnumChunkCollisionsState.COMPUTED);
                case COMPUTED_TERRAIN:
                    if(state == EnumChunkCollisionsState.ADDED_COMPUTED)
                        setChunkState(EnumChunkCollisionsState.COMPUTED);
                    else
                        setChunkState(EnumChunkCollisionsState.ADDED_PERSISTENT);
                case PERSISTENT_ELEMENTS:
                    if(state == EnumChunkCollisionsState.ADDED_PERSISTENT)
                        setChunkState(EnumChunkCollisionsState.COMPUTED);
                    else
                        setChunkState(EnumChunkCollisionsState.ADDED_COMPUTED);
            }
        }
        else {
            throw new IllegalStateException("Invalid transition of " + getPos() + " from " + state + " to COMPUTED/ADDED with terrain type "+terrainType);
        }
    }

    /**
     * Clears this chunk
     */
    public void reset()
    {
        if(DynamXConfig.enableDebugTerrainManager)
            ChunkGraph.addToGrah(getPos(), ChunkGraph.ChunkActions.RESET, ChunkGraph.ActionLocation.UNKNOWN, this, "Cur state "+state);
        boolean debug = DynamXConfig.enableDebugTerrainManager && DynamXConfig.chunkDebugPoses.contains(getPos());
        if(state == EnumChunkCollisionsState.INVALID)
            DynamXMain.log.warn("[CHUNK DEBUG] Already re-setted chunk "+this);
        setChunkState(EnumChunkCollisionsState.INVALID);
        elements.getElements().forEach(ITerrainElement::clear);
        elements.getPersistentElements().forEach(ITerrainElement::clear);
        if(debug)
            DynamXMain.log.info("[CHUNK DEBUG] Resetting chunk "+ getPos()+" with "+elements);
        elements.getElements().clear(); //Don't remove persistent elements, it may be re-used
    }

    /**
     * Adds this chunk to the physics world. If all elements are already added, an error is thrown. <br>
     * At the end, all elements of the chunk will be added (see {@link TerrainElementType}).
     *
     * @param profiler The current profiler, can be null
     */
    public void addToBulletWorld(IPhysicsWorld physicsWorld, @Nullable Profiler profiler) {
        Vector3fPool.openPool();
        if(!getChunkState().areComputedElementsAdded() && !getChunkState().arePersistentElementsAdded())
            addToBulletWorld(physicsWorld, TerrainElementType.ALL, profiler);
        else if(!getChunkState().areComputedElementsAdded())
            addToBulletWorld(physicsWorld, TerrainElementType.COMPUTED_TERRAIN, profiler);
        else if(!getChunkState().arePersistentElementsAdded())
            addToBulletWorld(physicsWorld, TerrainElementType.PERSISTENT_ELEMENTS, profiler);
        else if(false) //FIXME should not be called
             throw new IllegalStateException("Chunk is already added "+this+" "+getChunkState());
        Vector3fPool.closePool();
    }

    /**
     * Add the collisions to the bullet world, the body must have been computed previously, can't be added twice
     *
     * @param terrainType The elements to add
     * @param profiler The current profiler, can be null
     */
    public void addToBulletWorld(IPhysicsWorld physicsWorld, TerrainElementType terrainType, @Nullable Profiler profiler)
    {
        transitionToAddedState(terrainType);
        if(profiler != null)
            profiler.start(Profiler.Profiles.ADD_DEBUG_TO_WORLD);
        AtomicInteger addedBodys = new AtomicInteger();
        List<ITerrainElement> elements = this.elements.getElements(terrainType);
        if(!elements.isEmpty()) //Body may be empty if the chunk is empty
        {
            int x = myPos.x*16+8;
            int y = myPos.y*16;
            int z = myPos.z*16+8;
            Vector3f pos = Vector3fPool.get(x, y, z);
            final Vector3f min = Vector3fPool.get(pos);
            final Vector3f max = Vector3fPool.get(pos);
            Matrix3f mat = new Matrix3f();

            elements.forEach(body -> {
                if(body.getBody() != null) {
                    physicsWorld.addCollisionObject(body.getBody());
                    body.addDebugToWorld(mcWorld, pos);
                    addedBodys.getAndIncrement();
                }

                //For debug : get the biggest bb containing all
                /*BoundingBox b = body.getCollisionShape().boundingBox(pos, mat, BoundingBoxPool.get());
                Vector3f v = b.getMin(Vector3fPool.get());
                Vector3f w = b.getMax(Vector3fPool.get());
                min.set(Math.min(v.x, min.x), Math.min(v.y, min.y), Math.min(v.z, min.z));
                max.set(Math.max(w.x, max.x), Math.max(w.y, max.y), Math.max(w.z, max.z));
                todo fix by using my size, but do a real computation for compound box :c */
            });

            //(mcWorld.isRemote ? DynamXDebugOptions.CLIENT_CHUNK_BOXES : DynamXDebugOptions.CHUNK_BOXES).getDataIn().put(new BlockPos(myPos.x, myPos.y, myPos.z), new float[]{min.x, min.y, min.z, max.x, max.y, max.z, 0, 0, 1});
            updateNearEntities();
        }
        if(DynamXConfig.enableDebugTerrainManager)
            ChunkGraph.addToGrah(getPos(), ChunkGraph.ChunkActions.ADD_TO_WORLD, ChunkGraph.ActionLocation.MAIN, this, "Type "+terrainType+"./// Amount of components "+this.elements.getElements().size()+" "+this.elements.getElements(terrainType).size()+" added "+addedBodys);
        if(profiler != null)
            profiler.end(Profiler.Profiles.ADD_DEBUG_TO_WORLD);
    }

    /**
     * Removes this chunk from the physics world. <br>
     * At the end, all elements of the chunk will be removed from the physics world (see {@link TerrainElementType}).
     *
     */
    public void removeFromBulletWorld(IPhysicsWorld physicsWorld) {
        if(getChunkState().areComputedElementsAdded() && getChunkState().arePersistentElementsAdded())
            removeFromBulletWorld(physicsWorld, TerrainElementType.ALL);
        else if(getChunkState().areComputedElementsAdded())
            removeFromBulletWorld(physicsWorld, TerrainElementType.COMPUTED_TERRAIN);
        else if(getChunkState().arePersistentElementsAdded())
            removeFromBulletWorld(physicsWorld, TerrainElementType.PERSISTENT_ELEMENTS);
    }

    /**
     * Removes the collisions from the bullet world. Can't be removed twice.
     *
     * @param terrainType The elements to remove
     */
    public void removeFromBulletWorld(IPhysicsWorld physicsWorld, TerrainElementType terrainType)
    {
        transitionToRemovedState(terrainType);
        if(DynamXConfig.enableDebugTerrainManager)
            ChunkGraph.addToGrah(getPos(), ChunkGraph.ChunkActions.REMOVE_FROM_WORLD, ChunkGraph.ActionLocation.MAIN, this, "Type "+terrainType);
        List<ITerrainElement> elements = this.elements.getElements(terrainType);
        if(!elements.isEmpty()) //Body may be empty if the chunk is empty
        {
            Vector3fPool.openPool();
            BoundingBoxPool.getPool().openSubPool();

            elements.forEach(body -> {
                physicsWorld.removeCollisionObject(body.getBody());
                body.removeDebugFromWorld(mcWorld);
            });

            (mcWorld.isRemote ? DynamXDebugOptions.CLIENT_CHUNK_BOXES : DynamXDebugOptions.CHUNK_BOXES).getDataIn().remove(new BlockPos(myPos.x, myPos.y, myPos.z));
            updateNearEntities();

            BoundingBoxPool.getPool().closeSubPool();
            Vector3fPool.closePool();
        }
    }

    /**
     * Common code for loadCollisionsSync and loadCollisionsAsync <br>
     * Checks the state of this chunk and determines the element type to load
     *
     * @param synchronizedLoading True if the chunk is loaded in this thread, used for debug
     */
    private TerrainElementType initCollisionsLoading(ChunkLoadingTicket ticket, boolean synchronizedLoading) {
        if (state != EnumChunkCollisionsState.INITIALIZED)
            throw new IllegalStateException("Invalid transition from " + state + " to COMPUTING");
        if (ticket.getStatus() == ChunkState.LOADED)
            throw new IllegalStateException("Chunk " + this + " is already loaded into terrain manager ! " + ticket);
        if (DynamXConfig.enableDebugTerrainManager)
            ChunkGraph.addToGrah(getPos(), ChunkGraph.ChunkActions.LOAD_INTERNAL, synchronizedLoading ? ChunkGraph.ActionLocation.MAIN : ChunkGraph.ActionLocation.LOADER, this, "Ticket: " + ticket);
        reset();
        setChunkState(EnumChunkCollisionsState.COMPUTING);
        return ticket.getPriority() == ChunkLoadingTicket.TicketPriority.LOW ? TerrainElementType.PERSISTENT_ELEMENTS : TerrainElementType.ALL;
    }

    /**
     * Computes all collisions of the chunk, doing everything in this thread (no async tasks, event on client side)
     *
     * @param manager The terrain manager loading this chunk
     * @param cache The terrain cache to try to load ths chunk from
     * @param ticket The chunk loading ticket, holding priority and previous chunk information
     * @param pos The in-bullet world pos of the chunk, apply this offset to the created collision
     * @param profiler The current profiler
     */
    public void loadCollisionsSync(ITerrainManager manager, ITerrainCache cache, ChunkLoadingTicket ticket, Vector3f pos, Profiler profiler) {
        TerrainElementType needType = initCollisionsLoading(ticket, true);
        //System.out.println("Load, have "+elements+" "+this);
        ChunkTerrain cached = cache.load(ticket, profiler);
        localLoadCollisions(cached, cache, needType, ticket, pos, profiler);
        if(cached == null && cache.isRemoteCache())
        {
            cache.asyncLoad(ticket, TerrainElementType.PERSISTENT_ELEMENTS).thenAccept(elements -> {
                if(DynamXConfig.enableDebugTerrainManager)
                    System.out.println("Post-adding slopes to "+this+" ! Are "+elements);
                if(elements != null) //If there are loaded persistent elements
                    addPersistentElements(manager, elements.getPersistentElements());
                if(DynamXConfig.enableDebugTerrainManager)
                    ChunkGraph.addToGrah(ticket.getPos(), ChunkGraph.ChunkActions.ASYNC_COMPLETE_FUTURE_EXEC, ChunkGraph.ActionLocation.MAIN, this, "Post adder. "+ticket+" "+elements.getPersistentElements());
            }).exceptionally(e -> {
                DynamXMain.log.error("Failed to post-add persistent elements to chunk "+ticket, e);
                return null;
            });
        }
    }

    /**
     * Computes all collisions of the chunk, loading data from cache in an async way (allowing the client to gather chunk data from the server)
     *
     * @param manager The terrain manager loading this chunk
     * @param cache The terrain cache to try to load ths chunk from
     * @param ticket The chunk loading ticket, holding priority and previous chunk information
     * @param pos The in-bullet world pos of the chunk, apply this offset to the created collision
     */
    public CompletableFuture<Void> loadCollisionsAsync(ITerrainManager manager, ITerrainCache cache, ChunkLoadingTicket ticket, Vector3f pos) {
        TerrainElementType needType = initCollisionsLoading(ticket, false);
        //System.out.println("Load, have "+elements+" "+this);
        final Vector3f fpos = Vector3fPool.getPermanentVector(pos);
        final ChunkLoadingTicket.Snap c = ticket.snapshot();
        return cache.asyncLoad(ticket, needType).thenAccept((terrainElements) -> { //Nb : the FileTerrainCache is not async, only the RemoteTerrainCache is
            Profiler localProfiler = Profiler.get(); //it's async,so in another thread
            localLoadCollisions(terrainElements, cache, needType, ticket, fpos, localProfiler);
            //TODOOLD CHECK VALIDITY OF THE INITIAL SNAPSHOT ? No
            if(!c.isValid())
            {
                if(DynamXConfig.enableDebugTerrainManager) {
                    System.err.println("[COLL] Ignored async loaded chunk, new request sent " + c.getTicket() + " " + c.getSnapIndex() + " Got " + terrainElements);
                    ChunkGraph.addToGrah(ticket.getPos(), ChunkGraph.ChunkActions.ASYNC_COMPLETE_FUTURE_EXEC, ChunkGraph.ActionLocation.LOADER, this, "IGNORED async load. " + ticket + " " + elements + " snapid " + c.getSnapIndex() + " Got " + terrainElements);
                }
                return;
            }
            manager.offerLoadedChunk(new ChunkLoadingTicket.AsyncLoadedChunk(c, this)); //Allows to add the chunk to the bullet world - now done with completable futures :)
            if(DynamXConfig.enableDebugTerrainManager)
                ChunkGraph.addToGrah(ticket.getPos(), ChunkGraph.ChunkActions.ASYNC_COMPLETE_FUTURE_EXEC, ChunkGraph.ActionLocation.LOADER, this, "Normal async load. "+ticket+" "+elements+" snapid "+c.getSnapIndex()+" Got "+terrainElements);
            //System.out.println("Have load "+elements);
        });
    }

    /**
     * Computes collisions bodies, and terrain elements if elements is null or empty <br>
     * Returns all collisions of this chunk in this.elements
     *
     * @param cachedElements The elements of this chunk, will be added to chunk's elements list
     * @param cache The terrain cache to try to load ths chunk from
     * @param ticket The chunk loading ticket, holding priority and previous chunk information
     * @param pos The in-bullet world pos of the chunk, apply this offset to the returned body
     * @param profiler The current profiler
     */
    private void localLoadCollisions(@Nullable ChunkTerrain cachedElements, ITerrainCache cache, TerrainElementType type, ChunkLoadingTicket ticket, Vector3f pos, Profiler profiler)
    {
        profiler.start(Profiler.Profiles.CHUNK_SHAPE_COMPUTE);

        Vector3fPool.openPool();
        QuaternionPool.openPool();

        boolean debug = DynamXConfig.enableDebugTerrainManager && (DynamXConfig.chunkDebugPoses.contains(getPos()) || TerrainFile.ULTIMATEDEBUG);
        if(debug)
            DynamXMain.log.info("[CHUNK DEBUG] Computing collisions of chunk "+this+" with "+this.elements+" before, and take from "+ticket+" at "+System.currentTimeMillis());
        if(DynamXConfig.enableDebugTerrainManager)
            ChunkGraph.addToGrah(getPos(), ChunkGraph.ChunkActions.LOAD_INTERNAL_DOING, ChunkGraph.ActionLocation.UNKNOWN, this, "Previous : "+ticket+" "+type+" "+cachedElements);

        boolean shouldSave = false;
        if(type != TerrainElementType.PERSISTENT_ELEMENTS) {
            if (cachedElements == null || cachedElements.getElements().isEmpty()) {
                this.elements.getElements().addAll(TerrainCollisionsCalculator.computeCollisionFaces(myPos, mcWorld, profiler, false));
                ChunkGraph.addToGrah(getPos(), ChunkGraph.ChunkActions.LOAD_INTERNAL_DOING, ChunkGraph.ActionLocation.UNKNOWN, this, "DONE WITH  "+this.elements.getElements().size());
                if (!this.elements.getElements().isEmpty())
                    shouldSave = true;
                if (debug)
                    DynamXMain.log.info("[CHUNK DEBUG] Choice 1. Elements after : " + this.elements);
            } else {
                this.elements.getElements().addAll(cachedElements.getElements());
                if (debug)
                    DynamXMain.log.info("[CHUNK DEBUG] Choice 2. Elements after : " + this.elements.getElements(type));
            }
        }
        if (cachedElements != null)
            this.elements.getPersistentElements().addAll(cachedElements.getPersistentElements());
        //if (this.elements.getPersistentElements().isEmpty() && ticket.getCollisions() != null) //if previous slopes were not added, add them !
          //  addPersistentElements(ticket.getCollisions().getElements().getPersistentElements());

        //if(this.elements.getElements().isEmpty() && myPos.y == 0 && type != TerrainElementType.PERSISTENT_ELEMENTS)
          //  System.err.println("EMPTY AT "+myPos);

        maxSize = ITerrainElement.DEFAULT_SIZE;
        this.elements.getElements().forEach(element -> {
            try {
                PhysicsRigidBody b = element.build(pos);
                if(b == null && debug)
                    DynamXMain.log.info("[CHUNK DEBUG] Body of "+element+" is null");
                else if(debug)
                    System.out.println("Add "+element+" "+element.getBody());
            } catch (Exception e) {
                DynamXMain.log.error("Failed to add "+element+" in "+this, e);
                //Mark dirty for refresh
                DynamXContext.getPhysicsWorld().getTerrainManager().onChunkChanged(getPos());
            }
        });
        this.elements.getPersistentElements().forEach(element -> {
            PhysicsRigidBody b = element.build(pos);
            if(b == null)
                DynamXMain.log.warn("[CHUNK DEBUG] Body of "+element+" (persistent) is null");
            else if(debug)
                System.out.println("Add persistent "+element+" "+element.getBody());
            if(element.getMaxSize()[0] > maxSize[0] || element.getMaxSize()[1] > maxSize[1] || element.getMaxSize()[2] > maxSize[2])
                maxSize = element.getMaxSize().clone();
        });
        if(shouldSave) //save AFTER shapes have been built !
            cache.addChunkToSave(ticket, this);

        QuaternionPool.closePool();
        Vector3fPool.closePool();
        setChunkState(EnumChunkCollisionsState.COMPUTED);
        profiler.end(Profiler.Profiles.CHUNK_SHAPE_COMPUTE);
    }

    /**
     * Adds the given persistent elements to this chunk, building the elements, updating the cache and replacing the chunk in world (if loaded) <br>
     * Should be done in the physics thread
     *
     * @param manager The terrain manager
     * @param elementList The elements to add
     */
    public void addPersistentElements(ITerrainManager manager, Collection<ITerrainElement.IPersistentTerrainElement> elementList)
    {
        boolean debug = DynamXConfig.enableDebugTerrainManager && DynamXConfig.chunkDebugPoses.contains(getPos());
        if(debug)
            DynamXMain.log.info("[CHUNK DEBUG] Adding elements to "+ getPos()+" : "+elementList);

        manager.getCache().invalidate(manager.getTicket(getPos()), true, true);
        boolean added = state == EnumChunkCollisionsState.ADDED_ALL || state == EnumChunkCollisionsState.ADDED_PERSISTENT;
        if(added)
            removeFromBulletWorld(manager.getPhysicsWorld(), TerrainElementType.PERSISTENT_ELEMENTS);
        elements.getPersistentElements().addAll(elementList);
        for(ITerrainElement element : elementList)
        {
            element.build(Vector3fPool.get(myPos.x * 16, myPos.y * 16, myPos.z * 16));
        }
        if(added)
            addToBulletWorld(manager.getPhysicsWorld(), TerrainElementType.PERSISTENT_ELEMENTS, null);
        manager.getCache().addChunkToSave(manager.getTicket(getPos()), this);
    }

    /**
     * Removes the given persistent terrain elements, updating the cache and replacing the chunk in world (if loaded) <br>
     * Should be done in the physics thread
     *
     * @param manager The terrain manager
     * @param elements The elements to remove
     */
    public void removePersistentElements(ITerrainManager manager, Iterable<ITerrainElement.IPersistentTerrainElement> elements) {
        boolean debug = DynamXConfig.enableDebugTerrainManager && DynamXConfig.chunkDebugPoses.contains(getPos());
        if(debug)
            DynamXMain.log.info("[CHUNK DEBUG] Removing elements from "+ getPos()+" : "+elements);

        boolean added = state == EnumChunkCollisionsState.ADDED_ALL || state == EnumChunkCollisionsState.ADDED_PERSISTENT;
        manager.getCache().invalidate(manager.getTicket(getPos()), true, true);
        if(added)
            removeFromBulletWorld(manager.getPhysicsWorld(), TerrainElementType.PERSISTENT_ELEMENTS);
        for(ITerrainElement e : elements)
        {
            this.elements.getPersistentElements().remove(e);
        }
        if(added)
            addToBulletWorld(manager.getPhysicsWorld(), TerrainElementType.PERSISTENT_ELEMENTS, null);
        manager.getCache().addChunkToSave(manager.getTicket(getPos()), this);
    }

    /**
     * Called when the chunk is updated, to wake neighbor {@link PhysicsEntity} from sleeping
     */
    private void updateNearEntities()
    {
        DynamXMain.proxy.scheduleTask(mcWorld, () -> {
            try {
                for(int x=-1;x<=1;x++)
                {
                    for(int z=-1;z<=1;z++)
                    {
                        if(((boolean) DynamXReflection.worldIsChunkLoaded.invoke(mcWorld, myPos.x+x, myPos.z+z, false))) {
                            Chunk chk = mcWorld.getChunk(myPos.x + x, myPos.z + z);
                            for (int y = -1; y <= 1; y++) {
                                if (myPos.y + y >= 0 && myPos.y + y < 16) {
                                    chk.getEntityLists()[myPos.y + y].forEach(e -> {
                                        if (e instanceof PhysicsEntity)
                                            ((PhysicsEntity<?>) e).forcePhysicsActivation();
                                    });
                                }
                            }
                        }
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                DynamXMain.log.error("Failed to update entities around "+ getPos()+" after chunk change", e);
            }
        });
    }

    /**
     * @return An unmodifiable copy of chunk elements list
     */
    public ChunkTerrain getElements() {
        return elements.unmodifiableCopy();
    }

    @Override
    public int hashCode()
    {
        return myPos.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return posEquals(o);
    }

    @Override
    public String toString() {
        return "ChunkCollisions[x="+myPos.x+";y="+myPos.y+";z="+myPos.z+"]";
    }

    @Override
    public VerticalChunkPos getPos()
    {
        return myPos;
    }
}