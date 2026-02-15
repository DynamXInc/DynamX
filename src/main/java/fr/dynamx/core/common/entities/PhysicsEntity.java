package fr.dynamx.core.common.entities;

import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
// FIXME THIS SHOULD NOT RELY ON BULLET ANYMORE
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.network.sync.SynchronizedEntityVariable;
import fr.dynamx.api.network.sync.SynchronizedEntityVariableRegistry;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.api.physics.entities.EntityPhysicsState;
import fr.dynamx.core.common.DynamXContext;
import fr.dynamx.core.common.DynamXMain;
import fr.dynamx.core.common.network.sync.PhysicsEntitySynchronizer;
import fr.dynamx.core.common.network.sync.variables.EntityPosVariable;
import fr.dynamx.core.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.core.common.physics.joints.EntityJointsHandler;
import fr.dynamx.core.common.physics.player.WalkingOnPlayerController;
import fr.dynamx.core.common.physics.terrain.PhysicsEntityTerrainLoader;
import fr.dynamx.core.utils.DynamXConstants;
import fr.dynamx.core.utils.DynamXUtils;
import fr.dynamx.core.utils.PhysicsEntityException;
import fr.dynamx.core.utils.debug.Profiler;
import fr.dynamx.core.utils.maths.DynamXGeometry;
import fr.dynamx.core.utils.optimization.MutableBoundingBox;
import fr.dynamx.core.utils.optimization.QuaternionPool;
import fr.dynamx.core.utils.optimization.Vector3fPool;
import fr.hermes.api.mc.entities.HmEntityLogic;
import fr.hermes.api.mc.items.HmItemStack;
import fr.hermes.api.mc.world.HmWorld;
import fr.hermes.forge.JmeVector3fPool;
import fr.hermes.api.mc.entities.HmEntity;
import fr.hermes.api.mc.utils.HmOrientation;
import fr.hermes.api.mc.entities.HmPlayerEntity;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.nbt.NBTTagCompound;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Base class for all entities using bullet to simulate their physics
 *
 * @param <T> The physics handler type
 */
@SynchronizedEntityVariable.SynchronizedPhysicsModule(modid = DynamXConstants.ID)
public abstract class PhysicsEntity<T extends AbstractEntityPhysicsHandler<?, ?>> implements IDynamXObject, HmEntityLogic {
    @Getter
    protected final HmEntity mcEntity;

    /**
     * Entity network
     * -- GETTER --
     *
     * @return The entity network
     */
    @Getter
    private final PhysicsEntitySynchronizer<? extends PhysicsEntity<T>> synchronizer;

    /**
     * The entity physics handler
     */
    @Nullable
    @Getter
    public T physicsHandler;

    /**
     * The entity physics position
     */
    public final Vector3f physicsPosition = new Vector3f();

    /**
     * The entity physics rotation <br>
     * <strong>If you are rendering something, use renderRotation !</strong>
     */
    public final Quaternion physicsRotation = new Quaternion();

    /**
     * Rotation for render <br>
     * <strong>If you are not rendering something, use physicsRotation !</strong>
     */
    public final Quaternion renderRotation = new Quaternion();

    /**
     * Prev render rotation
     */
    public final Quaternion prevRenderRotation = new Quaternion();

    /**
     * Entity initialization state
     */
    public EnumEntityInitState initialized = EnumEntityInitState.NOT_INITIALIZED;

    /**
     * State of the entity inside the physics engine
     */
    public EnumEntityPhysicsRegistryState isRegistered = EnumEntityPhysicsRegistryState.NOT_REGISTERED;

    /**
     * Map of players walking on the top of this entity
     *
     * @see WalkingOnPlayerController
     */
    public final Map<HmPlayerEntity, WalkingOnPlayerController> walkingOnPlayers = new HashMap<>();

    /**
     * Permits the render of large entities that you are riding
     */
    public boolean wasRendered = false;

    /**
     * Cache to avoid many heavy calculus of the entity box
     */
    private MutableBoundingBox entityBoxCache;

    /**
     * True if the entity uses the physics world <br>
     * I.e. it's physics handler should not be null
     */
    private final boolean usesPhysicsWorld;

    @SynchronizedEntityVariable(name = "pos")
    public final EntityPosVariable synchronizedPosition = new EntityPosVariable(this);

    /**
     * -- GETTER --
     *
     * @return The terrain loader of this entity
     */
    @Getter
    private final PhysicsEntityTerrainLoader terrainCache = new PhysicsEntityTerrainLoader(this);

    public PhysicsEntity(HmEntity mcEntity) {
        this.mcEntity = mcEntity;
        mcEntity.hm$setNoClip(true);
        mcEntity.hm$setPreventEntitySpawning(true);
        mcEntity.hm$setIgnoreFrustumCheck(true);

        // Network Init
        synchronizer = DynamXMain.getProxy().getNetHandlerForEntity(this);
        usesPhysicsWorld = DynamXContext.usesPhysicsWorld(mcEntity.hm$getWorld());
    }

    public PhysicsEntity(HmEntity mcEntity, Vector3f pos, float spawnRotationAngle) {
        this(mcEntity);
        mcEntity.hm$setPosition(pos.x, pos.y, pos.z);
        mcEntity.hm$setRotationYaw(spawnRotationAngle);
    }

    // TODO reorganise methods

    @Override
    public Boolean isInRangeToRenderDist(double range) {
        double d = getBoundingBox().getAverageEdgeLength() * 4.0D * 64.0D;
        return range < d * d;
    }

    public void registerSynchronizedVariables() {
        SynchronizedEntityVariableRegistry.addVarsOf(this.getSynchronizer(), this);
    }

    /**
     * Checks if the entity has been initialized and initializes it if required
     */
    protected void checkEntityInit() {
        switch (initialized) {
            case NOT_INITIALIZED:
                physicsPosition.set((float) mcEntity.hm$getPosX(), (float) mcEntity.hm$getPosY(), (float) mcEntity.hm$getPosZ());
                if (physicsRotation.equals(Quaternion.IDENTITY)) {
                    physicsRotation.set(DynamXGeometry.rotationYawToQuaternion(mcEntity.hm$getRotationYaw()));
                }
                if (!initEntityProperties()) {
                    mcEntity.hm$setDead();
                    return;
                }
            case ONLY_ENTITY_PROPERTIES:
                initPhysicsEntity(usesPhysicsWorld);
                // Will refresh simulation holders on joint entities
                getSynchronizer().setSimulationHolder(getSynchronizer().getSimulationHolder(), getSynchronizer().getSimulationPlayerHolder());
                registerSynchronizedVariables();
                //TODO RESTORE EVENTS MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.Init(world.isRemote ? Side.CLIENT : Side.SERVER, this, usesPhysicsWorld));
                initialized = EnumEntityInitState.ALL;
                break;
        }
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        // Fix: since initEntityProperties was added here, checkEntityInit wasn't called anymore on client, and so physicsRotation wasn't correct
        if (physicsRotation.equals(Quaternion.IDENTITY)) {
            physicsRotation.set(DynamXGeometry.rotationYawToQuaternion(mcEntity.hm$getRotationYaw()));
        }
        if (!initEntityProperties()) {
            mcEntity.hm$setDead();
            return;
        }
        initialized = EnumEntityInitState.ONLY_ENTITY_PROPERTIES;
    }

    @Override
    public void writeToNbt(NBTTagCompound compound) {
        DynamXUtils.writeQuaternionNBT(compound, physicsRotation);
    }

    @Override
    public void readFromNbt(NBTTagCompound compound) {
        //Force init here, we have all the info needed
        QuaternionPool.openPool();
        physicsPosition.set((float) mcEntity.hm$getPosX(), (float) mcEntity.hm$getPosY(), (float) mcEntity.hm$getPosZ());
        physicsRotation.set(DynamXUtils.readQuaternionNBT(compound));
        QuaternionPool.closePool();
        if (!initEntityProperties()) {
            mcEntity.hm$setDead();
            return;
        }
        initialized = EnumEntityInitState.ONLY_ENTITY_PROPERTIES;
    }

    @Override
    public void onSetDead() {

    }

    @Override
    public void onAddPassenger(HmEntity passenger) {

    }

    @Override
    public void onRemovePassenger(HmEntity passenger) {

    }

    @Override
    public boolean updatePassenger(HmEntity passenger) {
        return false;
    }

    @Override
    public boolean updatePassengerRotation(HmEntity passenger) {
        return false;
    }

    @Override
    public HmEntity getControllingPassenger() {
        return null;
    }

    @Override
    public HmItemStack getPickedResult() {
        return null;
    }

    @Override
    public Boolean canFitPassenger(HmEntity passenger) {
        return false;
    }

    @Override
    public int getBrightnessForRender() {
        return -1;
    }

    /**
     * Fired by the minecraft entity update method
     */
    protected void mcThreadUpdate() {
        //Init
        checkEntityInit();

        //Prepare/request physics update
        if (usesPhysicsWorld()) {
            if (physicsHandler.getPhysicsState() == EntityPhysicsState.FROZEN) {
                physicsHandler.setPhysicsState(EntityPhysicsState.UNFREEZE);
            } else {
                physicsHandler.setPhysicsState(EntityPhysicsState.ENABLE);
            }
            if (isRegistered == EnumEntityPhysicsRegistryState.NOT_REGISTERED) {
                DynamXContext.getPhysicsWorld(mcEntity.hm$getWorld()).addBulletEntity(this);
            }
        }

        //Tick physics if we don't use a physics world
        if (!usesPhysicsWorld) {
            getSynchronizer().onPrePhysicsTick(Profiler.get());
            getSynchronizer().onPostPhysicsTick(Profiler.get());
        }

        //Update visual pos
        updateMinecraftPos(Vector3fPool.get(physicsPosition.x, physicsPosition.y, physicsPosition.z),
                DynamXUtils.toQuaternion(physicsRotation)); //TODO SHOULD USE POOL

        //Post the update event
        /* TODO RESTORE EVENTS PhysicsEntityEvent.Update update;
        if (world.isRemote) {
            update = new PhysicsEntityEvent.ClientUpdate(this,
                    PhysicsEntityEvent.UpdateType.POST_ENTITY_UPDATE,
                    isRegistered == EnumEntityPhysicsRegistryState.REGISTERED && usesPhysicsWorld);
        } else {
            update = new PhysicsEntityEvent.ServerUpdate(this, PhysicsEntityEvent.UpdateType.POST_ENTITY_UPDATE,
                    isRegistered == EnumEntityPhysicsRegistryState.REGISTERED && usesPhysicsWorld);
        }
        MinecraftForge.EVENT_BUS.post(update);*/
    }

    /**
     * Called in minecraft thread to update vanilla position and rotation fields, also used for render and updating "prev" fields
     */
    protected void updateMinecraftPos(org.joml.Vector3f physicsPosition, Quaternionf physicsRotation) {
        prevRenderRotation.set(renderRotation);
        renderRotation.set(physicsRotation.x, physicsRotation.y, physicsRotation.z, physicsRotation.w);

        onMove();
    }

    /**
     * Fired on entity move to move walking players
     */
    protected void onMove() {
        //if (x != 0 || y != 0 || z != 0)
        entityBoxCache = null; //The entity box has changed, mark it as dirty
        //TODO WIP
        Vector3f motion = JmeVector3fPool.get((float) mcEntity.hm$getMotionX(), (float) mcEntity.hm$getMotionY(), (float) mcEntity.hm$getMotionZ());
        for (Map.Entry<HmPlayerEntity, WalkingOnPlayerController> e : walkingOnPlayers.entrySet()) {
            HmPlayerEntity entity = e.getKey();
            HmOrientation f = e.getValue().face;
            {
                Vector3f vh = DynamXGeometry.rotateVectorByQuaternion(motion, physicsRotation);
                float projVehicMotion = JmeVector3fPool.get(vh.x, vh.y, vh.z).dot(JmeVector3fPool.get(f.getDirectionVec().x, f.getDirectionVec().y, f.getDirectionVec().z));
                if (projVehicMotion != 0) //We push the player
                {
                    e.getValue().applyOffset();
                }
            }
        }
    }

    /**
     * Called before ticking the physics world (can be in an external thread) <br>
     * Here we give the "input" to the physics world, i.e. the controls, the forces, etc <br>
     * Wrapper for events and profiling, please override preUpdatePhysics
     *
     * @param profiler        The current profiler
     * @param simulatePhysics If physics should be simulated in this update <br> If false, the physics handler may be null
     */
    public final void prePhysicsUpdateWrapper(Profiler profiler, boolean simulatePhysics) {
        profiler.start(Profiler.Profiles.PHY2);

        simulatePhysics = simulatePhysics && isRegistered == EnumEntityPhysicsRegistryState.REGISTERED;
        preUpdatePhysics(simulatePhysics);

        //TODO EVENTS MinecraftForge.EVENT_BUS.post(world.isRemote ? new PhysicsEntityEvent.ClientUpdate(this, PhysicsEntityEvent.UpdateType.PRE_PHYSICS_UPDATE, simulatePhysics) :
                //new PhysicsEntityEvent.ServerUpdate(this, PhysicsEntityEvent.UpdateType.PRE_PHYSICS_UPDATE, simulatePhysics));
        profiler.end(Profiler.Profiles.PHY2);
    }

    /**
     * Called before ticking the physics world (can be in an external thread) <br>
     * Here we give the "input" to the physics world, i.e. the controls, the forces, etc
     *
     * @param simulatingPhysics If physics should be simulated in this update <br> If false, the physics handler may be null
     */
    public void preUpdatePhysics(boolean simulatingPhysics) {
        if (simulatingPhysics) {
            physicsHandler.update();
        }
    }

    /**
     * Called after ticking the physics world (can be in an external thread) <br>
     * Here we get the results of the "input" : the new position, the new rotation, etc <br>
     * Wrapper for events and profiling, please override postUpdatePhysics
     *
     * @param profiler        The current profiler
     * @param simulatePhysics If physics should be simulated in this update <br> If false, the physics handler may be null
     */
    public final void postUpdatePhysicsWrapper(Profiler profiler, boolean simulatePhysics) {
        profiler.start(Profiler.Profiles.PHY2P);

        simulatePhysics = simulatePhysics && isRegistered == EnumEntityPhysicsRegistryState.REGISTERED;
        postUpdatePhysics(simulatePhysics);

        // TODO EVENTS MinecraftForge.EVENT_BUS.post(world.isRemote ? new PhysicsEntityEvent.ClientUpdate(this, PhysicsEntityEvent.UpdateType.POST_PHYSICS_UPDATE, simulatePhysics) :
                //new PhysicsEntityEvent.ServerUpdate(this, PhysicsEntityEvent.UpdateType.POST_PHYSICS_UPDATE, simulatePhysics));
        profiler.end(Profiler.Profiles.PHY2P);
    }


    /**
     * Called after ticking the physics world (can be in an external thread) <br>
     * Here we get the results of the "input" : the new position, the new rotation, etc
     *
     * @param simulatingPhysics If physics should be simulated in this update <br> If false, the physics handler may be null
     */
    public void postUpdatePhysics(boolean simulatingPhysics) {
        if (simulatingPhysics) {
            physicsHandler.postUpdate();
        }
    }

    /**
     * Inits the entity, for example pack properties <br>
     * Fired on the first update of the entity
     *
     * @return False to kill the entity (failed init)
     */
    public abstract boolean initEntityProperties();

    /**
     * Inits the entity physics handler <br>
     * Fired on the first update of the entity, only if this side uses physics
     *
     * @param usePhysics True if the entity is registered in a running physics world
     */
    public abstract void initPhysicsEntity(boolean usePhysics);

    /**
     * Forces activation, called when the collisions of the chunk of the entity changes
     */
    public void forcePhysicsActivation() {
        if (physicsHandler != null) {
            physicsHandler.activate();
        }
    }

    /**
     * @return the number of ticks between each sync of this entity from server to client, if on dedicated server <br>
     * This SHOULD return the same value on client and server sides
     */
    public abstract int getSyncTickRate();

    public boolean usesPhysicsWorld() {
        return usesPhysicsWorld && physicsHandler != null;
    }

    //Vanilla functions

    /**
     * Minecraft's entity update
     */
    @Override
    public void onUpdate() {
        try {
            mcThreadUpdate();
        } catch (Exception ex) {
            throw new PhysicsEntityException(this, "mcThreadUpdate", ex);
        }
    }

    @Override
    public MutableBoundingBox getBoundingBox() {
        if (entityBoxCache != null) {
            return entityBoxCache;
        }
        if (physicsPosition.length() == 0) {
            physicsPosition.set((float) mcEntity.hm$getPosX(), (float) mcEntity.hm$getPosY(), (float) mcEntity.hm$getPosZ());
        }
        JmeVector3fPool.openPool();
        if (physicsHandler != null) {
            Vector3f min = JmeVector3fPool.get();
            Vector3f max = JmeVector3fPool.get();
            BoundingBox boundingBox = physicsHandler.getBoundingBox();
            boundingBox.getMin(min);
            boundingBox.getMax(max);
            entityBoxCache = new MutableBoundingBox(min.x, min.y, min.z, max.x, max.y, max.z);
        } else {
            List<MutableBoundingBox> boxes = getCollisionBoxes(); //Get PartShape boxes
            if (boxes.isEmpty()) { //If there is no boxes, create a default one
                Vector3f min = JmeVector3fPool.get(mcEntity.hm$getPosX(), mcEntity.hm$getPosY(), mcEntity.hm$getPosZ()).subtractLocal(2, 1, 2);
                Vector3f max = JmeVector3fPool.get(mcEntity.hm$getPosX(), mcEntity.hm$getPosY(), mcEntity.hm$getPosZ()).addLocal(2, 2, 2);
                entityBoxCache = new MutableBoundingBox(min.x, min.y, min.z, max.x, max.y, max.z);
            } else {
                MutableBoundingBox container;
                if (boxes.size() == 1) { //If there is one, no more calculus to do !
                    container = boxes.get(0);
                } else {
                    container = new MutableBoundingBox(boxes.get(0));
                    for (int i = 1; i < boxes.size(); i++) { //Else create a bigger box containing all the boxes
                        container.growTo(boxes.get(i));
                    }
                }
                //The container box corresponding to an unrotated entity, so rotate it !
                container = DynamXContext.getCollisionHandler().rotateBB(physicsPosition, container, physicsRotation);
                container.grow(0.5, 0.0, 0.5); //Grow it to avoid little glitches on the corners of the car
                entityBoxCache = container;
            }
        }
        JmeVector3fPool.closePool();
        return entityBoxCache;
    }

    @Override
    public void onRemovedFromWorld() {
        IPhysicsWorld physicsWorld = DynamXContext.getPhysicsWorld(mcEntity.hm$getWorld());
        if (usesPhysicsWorld && physicsWorld != null) //onRemovedFromWorld may be called before physicsWorld is loaded (in case of failing to load from nbt)
        {
            physicsWorld.removeBulletEntity(this);
            terrainCache.onRemoved(physicsWorld.getTerrainManager());
        }
        if (physicsHandler != null) {
            physicsHandler.removeFromWorld();
        }
    }

    @Override
    public String getName() {
        return "DynamXEntity." + mcEntity.hm$getEntityId();
    }

    /**
     * @return The entity joint handler, null by default
     */
    @Nullable
    public EntityJointsHandler getJointsHandler() {
        return null;
    }

    /**
     * @return The module of the specified type
     */
    public abstract <D extends IPhysicsModule<?>> D getModuleByType(Class<D> moduleClass);

    /**
     * @return True if this entity has this module
     */
    public boolean hasModuleOfType(Class<? extends IPhysicsModule<?>> moduleClass) {
        return getModuleByType(moduleClass) != null;
    }

    /**
     * Method called when the entity's rigidbody enter in collision with something else
     */
    public void onCollisionEnter(PhysicsCollisionEvent collisionEvent, BulletShapeType<?> entityA, BulletShapeType<?> entityB) {
    }

    /**
     * @return True if the player should have the motion of this entity when walking on the top of any collision box
     * @see WalkingOnPlayerController
     */
    public boolean canPlayerStandOnTop() {
        return false;
    }

    @Override
    public Quaternion getCollidableRotation() {
        return physicsRotation;
    }

    @Override
    public Vector3f getCollisionOffset() {
        return JmeVector3fPool.get();
    }

    public HmWorld getWorld() {
        return mcEntity.hm$getWorld();
    }

    public UUID getUniqueId() {
        return mcEntity.hm$getUniqueID();
    }

    public int getEntityId() {
        return mcEntity.hm$getEntityId();
    }

    public int getTicksExisted() {
        return mcEntity.hm$getTicksExisted();
    }

    public boolean hasPassenger(HmEntity entity) {
        return mcEntity.hm$getPassengers().contains(entity);
    }

    /* FIXME DOES IT BREAKS SOMETHING TO REMOVE THIS?? @Override
    public void setLocationAndAngles(double x, double y, double z, float yaw, float pitch) {
        QuaternionPool.openPool();
        physicsRotation.set(DynamXGeometry.rotationYawToQuaternion(yaw));
        QuaternionPool.closePool();
        super.setLocationAndAngles(x, y, z, yaw, pitch);
    }*/

    public enum EnumEntityInitState {
        NOT_INITIALIZED, ONLY_ENTITY_PROPERTIES, ALL
    }

    public enum EnumEntityPhysicsRegistryState {
        NOT_REGISTERED, REGISTERING, REGISTERED
    }

    public abstract PhysicsEntitiesFactory createEntityFactory();
}
