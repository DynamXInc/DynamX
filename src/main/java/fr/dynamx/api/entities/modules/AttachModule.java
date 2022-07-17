package fr.dynamx.api.entities.modules;

import com.jme3.bullet.joints.Constraint;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.JointHandler;

/**
 * The basis of any attach module, permitting to add joints between entities, or between different parts of one entity <br>
 * You can distinguish the entity owning the joint (main entity) (for example the car) and the other entity (for example the trailer) <br>
 * The owning entity is the entity imposing the movement, the driveable one in most cases <br> <br>
 *
 * <strong>Do not implement this, but {@link AttachToOtherModule} or {@link AttachToSelfModule}</strong>
 *
 * @param <S> The entity which is NOT owning this joint (example : the trailer)
 * @see AttachToSelfModule
 * @see AttachToOtherModule
 * @see fr.dynamx.api.entities.modules.IEntityJoints
 * @see JointHandler
 */
public interface AttachModule<S extends PhysicsEntity<?>> {
    /**
     * Creates a joint between the main entity (owning this instance of the attach module) and the other entity
     *
     * @param otherEntity The entity to link with this module
     * @param jointId     The local id of the joint, useful if you have multiple joints on one {@link JointHandler} <br> Should be unique for each joint
     * @return The created joint, that will be automatically added to the associated {@link fr.dynamx.api.entities.modules.IEntityJoints} and to the physics world
     */
    Constraint createJoint(S otherEntity, byte jointId);

    /**
     * You can prevent duplicated joints here <br>
     * Note the {@link fr.dynamx.api.entities.modules.IEntityJoints} will throw an error if you try to add a duplicated joint
     *
     * @param withEntity The other entity involved in the joint being created
     * @param jointId    The local id of the joint being created
     * @return If the joint can be created
     */
    boolean canCreateJoint(PhysicsEntity<?> withEntity, byte jointId);

    /**
     * Called when a joint is removed from this entity <br>
     * This is called on the two entities owning the joint (except if it is a {@link AttachToSelfModule})
     *
     * @param joint The removed joint, containing the involved entities and the joint id
     */
    void onJointDestroyed(EntityJoint<?> joint);

    /**
     * An {@link AttachModule} made to attach different parts of one entity <br>
     * The other entity will always be the same as the main entity
     */
    interface AttachToSelfModule extends AttachModule<PhysicsEntity<?>> {
        /**
         * @deprecated Implement the other createJoint method
         */
        @Override
        @Deprecated
        default Constraint createJoint(PhysicsEntity<?> otherEntity, byte jointId) {
            return createJoint(jointId);
        }

        /**
         * Creates a joint in this entity
         *
         * @param jointId The local id of the joint, useful if you have multiple joints on one {@link JointHandler} <br> Should be unique for each joint
         * @return The created joint, that will be automatically added to the associated {@link fr.dynamx.api.entities.modules.IEntityJoints} and to the physics world
         */
        Constraint createJoint(byte jointId);
    }

    /**
     * An {@link AttachModule} to link two entities
     *
     * @param <S> The entity which is NOT owning this joint (example : the trailer)
     */
    interface AttachToOtherModule<S extends PhysicsEntity<?>> extends AttachModule<S> {
    }
}
