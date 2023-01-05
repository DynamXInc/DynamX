package fr.dynamx.common.network.sync.vars;

import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.common.physics.utils.SynchronizedRigidBodyTransform;

import java.util.Map;

/**
 * The {@link SynchronizedVariable} responsible to sync the pos and the rotation of rigid bodies attached to the entity
 */
public abstract class AttachedBodySynchronizedVariable<T extends PhysicsEntity<?>> {
    /**
     * Handles sync of attached bodies, via the {@link AttachedBodySynchronizer}
     */
    public interface AttachedBodySynchronizer {
        /**
         * @return A map giving one {@link SynchronizedRigidBodyTransform} for each joint (the key is the jointId) <br>
         * Keep this map in a field of this module
         */
        Map<Byte, SynchronizedRigidBodyTransform> getTransforms();

        /**
         * Alters the physics state on the object attached to the given joint <br>
         * Fired when receiving a sync and physics are enabled on this side
         */
        void setPhysicsTransform(byte jointId, RigidBodyTransform transform);
    }
}
