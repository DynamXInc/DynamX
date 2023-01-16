package fr.dynamx.api.network.sync;

import fr.dynamx.common.physics.utils.RigidBodyTransform;
import fr.dynamx.common.physics.utils.SynchronizedRigidBodyTransform;

import java.util.Map;

/**
 * Handles sync of attached bodies, via the {@link fr.dynamx.common.network.sync.variables.EntityTransformsVariable}
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
