package fr.dynamx.api.physics;

import com.jme3.bullet.PhysicsSoftSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsVehicle;
import fr.dynamx.api.events.PhysicsEvent;
import fr.dynamx.api.physics.terrain.ITerrainManager;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.terrain.PhysicsWorldTerrain;
import fr.dynamx.common.physics.utils.PhysicsWorldOperation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

/**
 * Where all physics happen
 */
public interface IPhysicsWorld {
    /**
     * Appends an operation on this PhysicsWorld <br>
     * See other add and remove methods <br>
     * Thread-safe method
     *
     * @param operation The {@link PhysicsWorldOperation} operation to execute
     */
    void addOperation(PhysicsWorldOperation<?> operation);

    /**
     * Adds a collision object to the physics world, used for non-vehicle objects <br>
     * The world terrain around will not be loaded, see addBulletEntity <br>
     * <strong>Note : </strong> The user object of the collision object must be a {@link BulletShapeType} <br>
     * Thread-safe method
     */
    default void addCollisionObject(PhysicsCollisionObject obj) {
        if (obj.getUserObject() instanceof BulletShapeType)
            addOperation(new PhysicsWorldOperation<>(PhysicsWorldOperation.PhysicsWorldOperationType.ADD_OBJECT, obj));
        else
            throw new IllegalArgumentException("User object of a collision object must be a BulletShapeType !");
    }

    /**
     * Removes a collision object from the physics world, used for non-vehicle objects <br>
     * The world terrain around will not be unloaded, see removeBulletEntity <br>
     * Thread-safe method
     */
    default void removeCollisionObject(PhysicsCollisionObject obj) {
        addOperation(new PhysicsWorldOperation<>(PhysicsWorldOperation.PhysicsWorldOperationType.REMOVE_OBJECT, obj));
    }

    /**
     * Adds an entity to the physics world : the terrain around the entity will be loaded <br>
     * Does not add the collision box of the entity, see addCollisionObject <br>
     * Thread-safe method
     */
    default void addBulletEntity(PhysicsEntity<?> e) {
        e.isRegistered = PhysicsEntity.EnumEntityPhysicsRegistryState.REGISTERING;
        addOperation(new PhysicsWorldOperation<>(PhysicsWorldOperation.PhysicsWorldOperationType.ADD_ENTITY, e));
        MinecraftForge.EVENT_BUS.post(new PhysicsEvent.PhysicsEntityAdded(e, this));
    }

    /**
     * Removes an entity from the world <br>
     * Does not remove the collision box of the entity, see removeCollisionObject <br>
     * Thread-safe method
     */
    default void removeBulletEntity(PhysicsEntity<?> e) {
        addOperation(new PhysicsWorldOperation<>(PhysicsWorldOperation.PhysicsWorldOperationType.REMOVE_ENTITY, e));
        e.isRegistered = PhysicsEntity.EnumEntityPhysicsRegistryState.NOT_REGISTERED;
        MinecraftForge.EVENT_BUS.post(new PhysicsEvent.PhysicsEntityRemoved(e, this));
    }

    /**
     * Adds a vehicle to the physics world <br>
     * The world terrain around will not be loaded (use addBulletEntity for this) <br>
     * <strong>Note : </strong> The user object of the collision object must be a {@link BulletShapeType} <br>
     * Thread-safe method
     */
    default void addVehicle(PhysicsVehicle vehicle) {
        if (vehicle.getUserObject() instanceof BulletShapeType)
            addOperation(new PhysicsWorldOperation<>(PhysicsWorldOperation.PhysicsWorldOperationType.ADD_VEHICLE, vehicle));
        else
            throw new IllegalArgumentException("User object of a vehicle must be a BulletShapeType !");
    }

    /**
     * Removes a vehicle from the physics world <br>
     * The world terrain around will not be unloaded (use removeBulletEntity for this) <br>
     * Thread-safe method
     */
    default void removeVehicle(PhysicsVehicle vehicle) {
        addOperation(new PhysicsWorldOperation<>(PhysicsWorldOperation.PhysicsWorldOperationType.REMOVE_VEHICLE, vehicle));
    }

    /**
     * Adds a joint to the physics world <br>
     * Thread-safe method
     */
    default void addJoint(PhysicsJoint joint) {
        addOperation(new PhysicsWorldOperation<>(PhysicsWorldOperation.PhysicsWorldOperationType.ADD_CONSTRAINT, joint));
    }

    /**
     * Removes a joint from the physics world <br>
     * It should be called when one of the entities using the joint is killed <br>
     * Thread-safe method
     */
    default void removeJoint(PhysicsJoint joint) {
        addOperation(new PhysicsWorldOperation<>(PhysicsWorldOperation.PhysicsWorldOperationType.REMOVE_CONSTRAINT, joint));
    }

    /**
     * @return The bullet's dynamic world
     */
    PhysicsSoftSpace getDynamicsWorld();

    /**
     * @return The total number of physics entity inside the world <br>
     * If 0 then there is no terrain loaded
     */
    int getLoadedEntityCount();

    /**
     * Removes all entities, vehicles and collisions from the physics world, called when world is unloading
     */
    void clearAll();

    /**
     * Processes all simulation things, called once per tick
     *
     * @param deltaTime The time elapsed since the last call of the function, in seconds (typically one tick ie 0.05 secs) <br>
     *                  This time will be subdivided into smaller intervals of getTimeSubdivision() to call getDynamicsWorld().stepSimulation as many times as needed
     */
    void stepSimulation(float deltaTime);

    /**
     * Schedules a tasks that will be executed in the physics thread, before the next simulation step
     */
    void schedule(Runnable r);

    /**
     * @return The {@link ITerrainManager} used by this world
     */
    PhysicsWorldTerrain getTerrainManager();

    /**
     * @return True if this {@link IPhysicsWorld} is simulating this minecraft world
     */
    boolean ownsWorld(World mcWorld);

    /**
     * Called on minecraft tick start
     */
    void tickStart();

    /**
     * Called on minecraft tick end
     */
    void tickEnd();

    /**
     * @return The thread simulating the physics
     */
    Thread getPhysicsThread();

    /**
     * @return True if the function is called from the physics thread (see getPhysicsThread())
     */
    default boolean isCallingFromPhysicsThread() {
        return Thread.currentThread() == getPhysicsThread();
    }
}
