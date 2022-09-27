package fr.dynamx.common.physics.utils;

/**
 * Helping class for sync of {@link RigidBodyTransform} via {@link fr.dynamx.common.network.sync.vars.AttachedBodySynchronizedVariable}
 */
public class SynchronizedRigidBodyTransform {
    /**
     * The transform at this tick <br>
     * Synchronized by the {@link fr.dynamx.common.network.sync.vars.AttachedBodySynchronizedVariable}
     */
    private final RigidBodyTransform physicTransform;
    /**
     * The prev render transform
     */
    private final RigidBodyTransform prevTransform;
    /**
     * The current render transform
     */
    private final RigidBodyTransform transform;

    public SynchronizedRigidBodyTransform() {
        this(new RigidBodyTransform());
    }

    /**
     * @param physicTransform The initial transform
     */
    public SynchronizedRigidBodyTransform(RigidBodyTransform physicTransform) {
        this.physicTransform = physicTransform;
        prevTransform = new RigidBodyTransform(physicTransform);
        transform = new RigidBodyTransform(physicTransform);
    }

    /**
     * @return The transform at this tick <br>
     * Synchronized by the {@link fr.dynamx.common.network.sync.vars.AttachedBodySynchronizedVariable}
     */
    public RigidBodyTransform getPhysicTransform() {
        return physicTransform;
    }

    /**
     * @return The current render transform
     */
    public RigidBodyTransform getTransform() {
        return transform;
    }

    /**
     * @return The prev render transform
     */
    public RigidBodyTransform getPrevTransform() {
        return prevTransform;
    }

    /**
     * Updates current and prev transform using the physic transform <br>
     * Fire this in the updatePos method of this module (see {@link fr.dynamx.api.entities.modules.IPhysicsModule.IEntityPosUpdateListener})
     */
    public void updatePos() {
        prevTransform.set(transform);
        transform.set(physicTransform);
    }
}
