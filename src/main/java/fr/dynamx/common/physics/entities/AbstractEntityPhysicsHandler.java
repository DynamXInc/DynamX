package fr.dynamx.common.physics.entities;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.api.physics.entities.EntityPhysicsState;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;

import javax.annotation.Nullable;

/**
 * Physics handler of {@link PhysicsEntity}, supporting all types of {@link PhysicsCollisionObject} <br>
 * The physics handler is the bridge between the minecraft entity and the physics engine
 *
 * @param <T> The entity type
 * @param <P> The {@link PhysicsCollisionObject} type
 * @see EntityPhysicsHandler
 */
public abstract class AbstractEntityPhysicsHandler<T extends PhysicsEntity<?>, P extends PhysicsCollisionObject> {
    /**
     * The entity owning this physics handler
     */
    protected final T handledEntity;
    /**
     * The {@link PhysicsCollisionObject} managed by this physics handler
     */
    protected final P collisionObject;
    /**
     * The position
     */
    protected final Vector3f physicsPosition;
    /**
     * The rotation
     */
    protected final Quaternion physicsRotation;
    /**
     * The initial spawn yaw angle
     */
    protected final float spawnRotationAngle;
    /**
     * The activation state of the {@link PhysicsCollisionObject}
     */
    protected boolean isBodyActive;

    /**
     * The physics state of the object
     */
    private EntityPhysicsState physicsState = EntityPhysicsState.UNFREEZE;

    public AbstractEntityPhysicsHandler(T entity) {
        this.handledEntity = entity;
        this.spawnRotationAngle = entity.rotationYaw;
        this.physicsPosition = new Vector3f((float) entity.posX, (float) entity.posY, (float) entity.posZ);
        this.physicsRotation = new Quaternion(DynamXGeometry.rotationYawToQuaternion(spawnRotationAngle));

        collisionObject = createShape(physicsPosition, physicsRotation, spawnRotationAngle);
    }

    /**
     * Creates the {@link PhysicsCollisionObject} of this physics handler
     *
     * @param position      The spawn position
     * @param rotation      The spawn rotation
     * @param spawnRotation The spawn rotation yaw
     * @return The final collision object
     */
    protected abstract P createShape(Vector3f position, Quaternion rotation, float spawnRotation);

    /**
     * Adds the collision object to the physics world
     */
    public void addToWorld() {
        DynamXContext.getPhysicsWorld().addCollisionObject(collisionObject);
    }

    /**
     * Removes the collision object from the physics world
     */
    public void removePhysicsEntity() {
        if (collisionObject != null) {
            DynamXContext.getPhysicsWorld().removeCollisionObject(collisionObject);
        }
    }

    /**
     * Copy the current physics state from the collision object state
     */
    public void update() {
    }

    //TODO DOC
    public void postUpdate() {
        collisionObject.getPhysicsLocation(physicsPosition);
        collisionObject.getPhysicsRotation(physicsRotation);
        isBodyActive = collisionObject.isActive();
    }

    /**
     * If force is true, then entity sleeping will be disabled, used for driven vehicles <br>
     * Note : sleeping is an optimization so use this wisely
     */
    public void setForceActivation(boolean force) {
    }

    /**
     * Activates the entity physics <br>
     * If the rigid body is sleeping, it will stop sleeping <br>
     * Note : sleeping is an optimization so use this wisely
     */
    public void activate() {
        getCollisionObject().activate(true);
    }

    /**
     * @return The activation state of the {@link PhysicsCollisionObject}
     */
    public boolean isBodyActive() {
        return isBodyActive;
    }

    /**
     * @return The entity owning this physics handler
     */
    public T getHandledEntity() {
        return handledEntity;
    }

    /**
     * @deprecated will be removed, replaced by getHandledEntity
     */
    @Deprecated
    public T getPhysicsEntity() {
        return handledEntity;
    }

    /**
     * @return The {@link PhysicsCollisionObject} managed by this physics handler
     */
    public P getCollisionObject() {
        return collisionObject;
    }


    /**
     * @return The initial spawn yaw angle
     */
    public float getSpawnRotationAngle() {
        return spawnRotationAngle;
    }

    /**
     * @return The physics state of the object
     */
    public EntityPhysicsState getPhysicsState() {
        return physicsState;
    }

    /**
     * Sets the physics state of the object
     */
    public void setPhysicsState(EntityPhysicsState physicsState) {
        this.physicsState = physicsState;
    }

    /**
     * @return the rotation in the physics world
     */
    public Quaternion getRotation() {
        return this.physicsRotation;
    }

    /**
     * @return The in-world position of this entity
     */
    public Vector3f getPosition() {
        Vector3f mass = getCenterOfMass();
        if (mass == null) {
            return physicsPosition;
        }
        Vector3f vec = Vector3fPool.get(physicsPosition);
        vec.addLocal(DynamXGeometry.rotateVectorByQuaternion(mass, getRotation()));
        return vec;
    }

    /**
     * @return The center of mass of the object, or null if (0, 0, 0)
     */
    @Nullable
    public Vector3f getCenterOfMass() {
        return null;
    }

    /**
     * Sets the physics position, the physics rotation and the velocities <br>
     * Used for network sync
     */
    public void updatePhysicsState(Vector3f pos, Quaternion rotation, Vector3f linearVel, Vector3f rotationalVel) {
        Vector3f mass = getCenterOfMass();
        if (mass != null) {
            pos.addLocal(DynamXGeometry.rotateVectorByQuaternion(mass, rotation).multLocal(-1));
        }

        setPhysicsPosition(pos);
        setPhysicsRotation(rotation);
        setLinearVelocity(linearVel);
        setAngularVelocity(rotationalVel);
    }

    /**
     * Adjusts the physics position and velocity to match the given pos, and sets the physics rotation and velocities
     */
    public void updatePhysicsStateFromNet(Vector3f pos, Quaternion rotation, Vector3f linearVel, Vector3f rotationalVel) {
        Vector3f mass = getCenterOfMass();
        if (mass != null) {
            pos.addLocal(DynamXGeometry.rotateVectorByQuaternion(mass, rotation).multLocal(-1));
        }

        linearVel.addLocal(pos.subtract(physicsPosition));
        setLinearVelocity(linearVel);

        setPhysicsRotation(rotation);
        setAngularVelocity(rotationalVel);
    }

    /**
     * Sets the position in the physics world
     */
    public abstract void setPhysicsPosition(Vector3f position);

    /**
     * Sets the rotation in the physics world
     */
    public abstract void setPhysicsRotation(Quaternion rotation);

    /**
     * @return the velocity in the physics world
     */
    public abstract Vector3f getLinearVelocity();

    /**
     * @return the angular/rotational velocity in the physics world
     */
    public abstract Vector3f getAngularVelocity();

    /**
     * Sets the velocity in the physics world
     */
    public abstract void setLinearVelocity(Vector3f velocity);

    /**
     * Sets the angular/rotational velocity in the physics world
     */
    public abstract void setAngularVelocity(Vector3f velocity);

    /**
     * Applies a force on this entity <br>
     * Does not affect the entity if it's not moving (lets the entity sleep)
     */
    public abstract void applyForce(Vector3f at, Vector3f force);

    /**
     * Applies a torque on this entity <br>
     * Does not affect the entity if it's not moving (lets the entity sleep)
     */
    public abstract void applyTorque(Vector3f force);

    /**
     * Applies an impulse on this entity <br>
     * Will prevent the entity from sleeping
     */
    public abstract void applyImpulse(Vector3f at, Vector3f force);

    /**
     * Applies a torque impulse on this entity <br>
     * Will prevent the entity from sleeping
     */
    public abstract void applyTorqueImpulse(Vector3f force);

    /**
     * Freezes the object : disables the physics by setting the collision object as kinematic <br>
     * Used internally by the {@link fr.dynamx.api.physics.IPhysicsWorld} <br>
     * Depends on the {@link EntityPhysicsState}
     *
     * @param freeze The frozen state of this object
     */
    public abstract void setFreezePhysics(boolean freeze);
}
