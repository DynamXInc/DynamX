package fr.dynamx.common.entities;

import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.modules.IEntityJoints;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.events.PhysicsEntityEvent;
import fr.dynamx.api.network.sync.PhysicsEntityNetHandler;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.entities.EntityPhysicsState;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.common.network.sync.vars.PosSynchronizedVariable;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import fr.dynamx.common.physics.player.WalkingOnPlayerController;
import fr.dynamx.common.physics.terrain.PhysicsEntityTerrainLoader;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.PhysicsEntityException;
import fr.dynamx.utils.debug.Profiler;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for all entities using bullet to simulate their physics
 *
 * @param <T> The physics handler type
 */
public abstract class PhysicsEntity<T extends AbstractEntityPhysicsHandler<?, ?>> extends Entity implements ICollidableObject, IEntityAdditionalSpawnData {
    /**
     * Entity network
     */
    private final PhysicsEntityNetHandler<? extends PhysicsEntity<T>> network;
    /**
     * The entity physics handler
     */
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
     * <p>
     * 0 = not initialized
     * 1 = only entity properties
     * 2 = all (physics included)
     */
    public byte initialized;
    /**
     * State of the entity inside of the physics engine
     * 0 = not registered
     * 1 = registering
     * 2 = registered
     */
    public byte isRegistered;
    /**
     * Map of players walking on the top of this entity
     *
     * @see WalkingOnPlayerController
     */
    public final Map<EntityPlayer, WalkingOnPlayerController> walkingOnPlayers = new HashMap<>();
    /**
     * Permits the render of large entities that you are riding
     */
    public boolean wasRendered = false;
    /**
     * Cache to avoid many heavy calculus of the entity box
     */
    private AxisAlignedBB entityBoxCache;
    /**
     * True if the entity uses the physics world <br>
     * I.e. it's physics handler should not be null
     */
    private final boolean usesPhysicsWorld;

    private final PhysicsEntityTerrainLoader terrainCache = new PhysicsEntityTerrainLoader(this);

    public PhysicsEntity(World world) {
        super(world);

        this.noClip = true;
        this.preventEntitySpawning = true;

        // Network Init
        network = DynamXMain.proxy.getNetHandlerForEntity(this);
        usesPhysicsWorld = DynamXContext.usesPhysicsWorld(world);
    }

    public PhysicsEntity(World world, Vector3f pos, float spawnRotationAngle) {
        this(world);
        setPosition(pos.x, pos.y, pos.z);
        rotationYaw = spawnRotationAngle;
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 0 || pass == 1;
    }

    @Override
    protected void entityInit() {
    }

    /**
     * Adds the {@link fr.dynamx.api.network.sync.SynchronizedVariable} used to synchronize this module <br>
     * The variables must only be added on the side which has the authority over the data (typically the server) <br>
     * Fired on modules initialization and on {@link fr.dynamx.api.network.sync.SimulationHolder} changes
     *
     * @param side             The current side
     * @param simulationHolder The new holder of the simulation of the entity (see {@link SimulationHolder})
     * @return The list of {@link fr.dynamx.api.network.sync.SynchronizedVariable} used to sync the entity, referenced by they registry name (see {@link fr.dynamx.api.network.sync.SynchronizedVariablesRegistry})
     */
    public List<ResourceLocation> getSynchronizedVariables(Side side, SimulationHolder simulationHolder) {
        List<ResourceLocation> variables = new ArrayList<>();
        if (simulationHolder.isPhysicsAuthority(side)) {
            variables.add(PosSynchronizedVariable.NAME);
        }
        return variables;
    }

    /**
     * Captures the state of this entity at this instant <br>
     * Permits interpolation of position and rotation on client side
     */
    public fr.dynamx.common.network.sync.vars.EntityPhysicsState createStateSnapshot() {
        return new fr.dynamx.common.network.sync.vars.EntityPhysicsState(this);
    }

    /**
     * Checks if the entity has been initialized and initializes it if required
     */
    protected void checkEntityInit() {
        if (initialized != 2) {
            if (initialized == 0) {
                physicsPosition.set((float) posX, (float) posY, (float) posZ);
                if(physicsRotation.equals(Quaternion.IDENTITY)) {
                    physicsRotation.set(DynamXGeometry.rotationYawToQuaternion(rotationYaw));
                }
                if (!initEntityProperties()) {
                    setDead();
                    return;
                }
            }
            getNetwork().setSimulationHolder(getNetwork().getDefaultSimulationHolder());
            initPhysicsEntity(usesPhysicsWorld);
            MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.Init(world.isRemote ? Side.CLIENT : Side.SERVER, this, usesPhysicsWorld));
            initialized = 2;
        }
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        DynamXUtils.writeQuaternion(buffer, physicsRotation);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        physicsRotation.set(DynamXUtils.readQuaternion(additionalData));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        DynamXUtils.writeQuaternionNBT(compound, physicsRotation);
        return super.writeToNBT(compound);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        //Force init here, we have all the info needed
        QuaternionPool.openPool();
        physicsPosition.set((float) posX, (float) posY, (float) posZ);
        physicsRotation.set(DynamXUtils.readQuaternionNBT(compound));
        QuaternionPool.closePool();
        if (!initEntityProperties()) {
            setDead();
            return;
        }
        initialized = 1;
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
                this.physicsHandler.setPhysicsState(EntityPhysicsState.ENABLE);
            }
            if (isRegistered == 0) {
                DynamXContext.getPhysicsWorld().addBulletEntity(this);
            }
        }

        //Tick physics if we don't use a physics world
        if (!usesPhysicsWorld) {
            getNetwork().onPrePhysicsTick(Profiler.get());
            getNetwork().onPostPhysicsTick(Profiler.get());
        }

        //Update visual pos
        updateMinecraftPos();

        //Post the update event
        MinecraftForge.EVENT_BUS.post(world.isRemote ? new PhysicsEntityEvent.ClientUpdate(this, PhysicsEntityEvent.UpdateType.POST_ENTITY_UPDATE, isRegistered == 2 && usesPhysicsWorld) :
                new PhysicsEntityEvent.ServerUpdate(this, PhysicsEntityEvent.UpdateType.POST_ENTITY_UPDATE, isRegistered == 2 && usesPhysicsWorld));
    }

    /**
     * Called in minecraft thread to update vanilla position and rotation fields, also used for render and updating "prev" fields
     */
    protected void updateMinecraftPos() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        this.posX = this.physicsPosition.x;
        this.posY = this.physicsPosition.y;
        this.posZ = this.physicsPosition.z;

        this.motionX = (this.posX - this.prevPosX);
        this.motionY = (this.posY - this.prevPosY);
        this.motionZ = (this.posZ - this.prevPosZ);

        onMove(this.motionX, this.motionY, this.motionZ);
        setPosition(posX, posY, posZ);

        prevRenderRotation.set(renderRotation);
        renderRotation.set(physicsRotation);

        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;

        alignRotation(this.renderRotation);
        //System.out.println("From origin quat " + renderRotation + " get " + rotationYaw);
    }

    /**
     * Fired on entity move to move walking players
     *
     * @param x x move
     * @param y y move
     * @param z z move
     */
    public void onMove(double x, double y, double z) {
        //if (x != 0 || y != 0 || z != 0)
        entityBoxCache = null; //The entity box has changed, mark it as dirty
        //WIP
        for (Map.Entry<EntityPlayer, WalkingOnPlayerController> e : walkingOnPlayers.entrySet()) {
            //List<EnumFacing> collisionFaces = e.getValue();
            EntityPlayer entity = e.getKey();
            //for (EnumFacing f : collisionFaces) {
            //if (!collisionFaces.contains(f.getOpposite())) //If not stuck between 2 aabb
            EnumFacing f = e.getValue().face;
            {
                Vector3f vh = DynamXContext.getCollisionHandler().rotate(Vector3fPool.get((float) motionX, (float) motionY, (float) motionZ), physicsRotation);
                float projVehicMotion = Vector3fPool.get(vh.x, vh.y, vh.z).dot(Vector3fPool.get(f.getDirectionVec().getX(), f.getDirectionVec().getY(), f.getDirectionVec().getZ()));
                //if (projVehicMotion != 0)
                //  System.out.println("Collide on face " + f + " " + projVehicMotion);
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

        simulatePhysics = simulatePhysics && isRegistered == 2;
        preUpdatePhysics(simulatePhysics);

        MinecraftForge.EVENT_BUS.post(world.isRemote ? new PhysicsEntityEvent.ClientUpdate(this, PhysicsEntityEvent.UpdateType.PRE_PHYSICS_UPDATE, simulatePhysics) :
                new PhysicsEntityEvent.ServerUpdate(this, PhysicsEntityEvent.UpdateType.PRE_PHYSICS_UPDATE, simulatePhysics));
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

        simulatePhysics = simulatePhysics && isRegistered == 2;
        postUpdatePhysics(simulatePhysics);

        MinecraftForge.EVENT_BUS.post(world.isRemote ? new PhysicsEntityEvent.ClientUpdate(this, PhysicsEntityEvent.UpdateType.POST_PHYSICS_UPDATE, simulatePhysics) :
                new PhysicsEntityEvent.ServerUpdate(this, PhysicsEntityEvent.UpdateType.POST_PHYSICS_UPDATE, simulatePhysics));
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
            this.physicsPosition.set(this.physicsHandler.getPosition());
            this.physicsRotation.set(this.physicsHandler.getRotation());
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
     * @return The entity network
     */
    public PhysicsEntityNetHandler<? extends PhysicsEntity<T>> getNetwork() {
        return network;
    }

    /**
     * @return The terrain loader of this entity
     */
    public PhysicsEntityTerrainLoader getTerrainCache() {
        return terrainCache;
    }

    /**
     * Computes yaw and pitch from the given quaternion
     */
    private void alignRotation(Quaternion localQuat) {
        Vector3f rotatedForwardDirection = Vector3fPool.get();
        rotatedForwardDirection = localQuat.mult(DynamXGeometry.FORWARD_DIRECTION, rotatedForwardDirection);

        rotationPitch = DynamXGeometry.getPitchFromRotationVector(rotatedForwardDirection) % 360;

        rotationYaw = DynamXGeometry.getYawFromRotationVector(rotatedForwardDirection) % 360;
        if (rotationYaw - prevRotationYaw > 270)
            prevRotationYaw += 360;
        else if (prevRotationYaw - rotationYaw > 270)
            prevRotationYaw -= 360;
    }

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
     * Minecraft entity update
     */
    @Override
    public void onUpdate() {
        Vector3fPool.openPool();
        double d1 = this.prevPosX;
        double d2 = this.prevPosY;
        double d3 = this.prevPosZ;
        super.onUpdate();
        this.prevPosX = d1;
        this.prevPosY = d2;
        this.prevPosZ = d3;
        try {
            mcThreadUpdate();
        } catch (Exception ex) {
            throw new PhysicsEntityException(this, "mcThreadUpdate", ex);
        }
        Vector3fPool.closePool();
    }

    @Override
    public float getEyeHeight() {
        return 0;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
    } //Avoid vanilla sync

    @Override
    public boolean isInRangeToRenderDist(double range) {
        double d = getEntityBoundingBox().getAverageEdgeLength() * 4.0D;
        d *= 64.0D;
        return range < d * d;
    }

    @Override
    public AxisAlignedBB getEntityBoundingBox() {
        if (entityBoxCache == null) {
            if (physicsPosition.length() == 0) {
                physicsPosition.set(new Vector3f((float) posX, (float) posY, (float) posZ));
            }
            Vector3fPool.openPool();
            if (physicsHandler != null) {
                Vector3f min = Vector3fPool.get();
                Vector3f max = Vector3fPool.get();
                BoundingBox boundingBox = physicsHandler.getBoundingBox();
                boundingBox.getMin(min);
                boundingBox.getMax(max);
                entityBoxCache = new AxisAlignedBB(min.x, min.y, min.z, max.x, max.y, max.z);
            } else {
                List<MutableBoundingBox> boxes = getCollisionBoxes(); //Get PartShape boxes
                if (boxes.isEmpty()) { //If there is no boxes, create a default one
                    entityBoxCache = new AxisAlignedBB(getPositionVector().x - 2, getPositionVector().y - 1, getPositionVector().z - 2,
                            getPositionVector().x + 2, getPositionVector().y + 2, getPositionVector().z + 2);
                } else {
                    MutableBoundingBox container;
                    if (boxes.size() == 1) { //If there is one, no more calculus to do !
                        container = boxes.get(0);
                    } else {
                        container = new MutableBoundingBox(boxes.get(0));
                        for (int i = 1; i < boxes.size(); i++) { //Else create a bigger box containing all of the boxes
                            container.growTo(boxes.get(i));
                        }
                    }
                    //The container box corresponding to an unrotated entity, so rotate it !
                    container = DynamXContext.getCollisionHandler().rotateBB(physicsPosition, container, physicsRotation);
                    container.grow(0.5, 0.0, 0.5); //Grow it to avoid little glitches on the corners of the car
                    entityBoxCache = container.toBB();
                }
            }
            Vector3fPool.closePool();
        }
        return entityBoxCache;
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (usesPhysicsWorld && DynamXContext.getPhysicsWorld() != null) //onRemovedFromWorld may be called before physicsWorld is loaded (in case of failing to load from nbt)
        {
            DynamXContext.getPhysicsWorld().removeBulletEntity(this);
            terrainCache.onRemoved(DynamXContext.getPhysicsWorld().getTerrainManager());
        }
        if (physicsHandler != null)
            physicsHandler.removePhysicsEntity();
    }

    @Override
    public String getName() {
        return "DynamXEntity." + getEntityId();
    }

    @Override
    public boolean attackEntityFrom(DamageSource damageSource, float amount) {
        if (!MinecraftForge.EVENT_BUS.post(new PhysicsEntityEvent.Attacked(this, damageSource.getTrueSource(), damageSource))) {
            if (damageSource.isExplosion()) {
                return false;
            }
            //FIXME BUG WITH BULLET, IF WE DISABLE SLEEP, THEN ACTIVE THEN ENABLE SLEEP, NO BUG, ELSE STAYS NOT ACTIVATED
            /*if(physicsHandler != null) {
                System.out.println("ENTITY INFO " + physicsHandler+" "+isRegistered+" "+physicsPosition+" "+physicsHandler.getPosition()+" "+physicsHandler.getPhysicsState()+" "+physicsHandler.isBodyActive()+" "+physicsHandler.getCollisionObject());
                System.out.println("m is "+physicsHandler.getCollisionObject().getCollisionShape()+" "+physicsHandler.getCollisionObject().getPhysicsLocation(Vector3fPool.get()));
                ((PhysicsRigidBody)physicsHandler.getCollisionObject()).setSleepingThresholds(0.8f, 1);
                ((PhysicsRigidBody)physicsHandler.getCollisionObject()).setEnableSleep(false);
                ((PhysicsRigidBody)physicsHandler.getCollisionObject()).activate(true);
                System.out.println("But "+((PhysicsRigidBody)physicsHandler.getCollisionObject()).isActive()+" "+((PhysicsRigidBody)physicsHandler.getCollisionObject()).getGravity(Vector3fPool.get())+" "+((PhysicsRigidBody)physicsHandler.getCollisionObject()).getMass());
                ((PhysicsRigidBody)physicsHandler.getCollisionObject()).setEnableSleep(true);
                return false;
            }
            if(true) {
                return false;
            }*/
            if (!this.world.isRemote && !this.isDead && damageSource.getImmediateSource() instanceof EntityPlayer && damageSource.getTrueSource().getRidingEntity() != this
                    && (((EntityPlayer) damageSource.getImmediateSource()).capabilities.isCreativeMode
                    || ((EntityPlayer) damageSource.getImmediateSource()).getHeldItemMainhand().getItem().equals(DynamXItemRegistry.ITEM_WRENCH))) {
                setDead();
                return true;
            }
        }
        return false;
    }

    /**
     * @return Entity physics handler
     */
    public T getPhysicsHandler() {
        return physicsHandler;
    }

    /**
     * @return The entity joint handler, null by default
     */
    @Nullable
    public IEntityJoints getJointsHandler() {
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
        return Vector3fPool.get();
    }

    @Override
    public void setLocationAndAngles(double x, double y, double z, float yaw, float pitch) {
        QuaternionPool.openPool();
        physicsRotation.set(DynamXGeometry.rotationYawToQuaternion(yaw));
        QuaternionPool.closePool();
        super.setLocationAndAngles(x, y, z, yaw, pitch);
    }
}
