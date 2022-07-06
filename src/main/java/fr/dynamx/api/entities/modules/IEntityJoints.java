package fr.dynamx.api.entities.modules;

import com.jme3.bullet.joints.Constraint;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.joints.EntityJoint;
import fr.dynamx.common.physics.joints.EntityJointsHandler;
import fr.dynamx.common.physics.joints.JointHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;

/**
 * Handles creation, synchronisation and destruction of {@link EntityJoint}, via a {@link JointHandler} <br>
 *     A joint is a {@link Constraint} between two entities, for example a car and a trailer <br>
 *         If you want to put joints on your entity, you should return this in the getJointsHandler function of you {@link PhysicsEntity} <br>
 *             NOTE : if you want to override something, first check your {@link JointHandler} if you can do it there
 *
 * @see EntityJointsHandler EntityJointsHandler for the default implementation
 * @see AttachModule
 */
public interface IEntityJoints
{
    /**
     * <strong>This method is fired by the default {@link JointHandler} implementation, in most case you don't have to call this.</strong> <br><br>
     * Creates a joint between this two entities <br>
     * The joint will be saved an re-created until you remove it, one entity dies, or the joint breaks <br>
     * Should be fired on server side
     *
     * @param type The joint handler of the joint
     * @param joint The physical {@link Constraint} associated with the joint
     * @param jointId The local id of the joint, useful if you have multiple joints on this JointHandler <br> Should be unique for each joint
     * @param otherEntity The entity linked to the entity owning *this* {@link EntityJointsHandler} (the other entity must have another EntityJointsHandler, but you don't need to call this function on it) <br>
     *                    Can be the same entity (the entity owning this joints handler)
     * @param <C> The type of the {@link Constraint}
     */
    <C extends Constraint> void addJoint(JointHandler<?, ?, ?> type, C joint, byte jointId, PhysicsEntity<?> otherEntity);

    /**
     * Removes the joint with this entity, if it exists <br>
     *     Should be fired on server side
     *
     * @param jointType The type of the joint (name of the {@link JointHandler})
     * @param jointId The local id of the joint, the same as when the joint was created
     */
    void removeJointWithMe(ResourceLocation jointType, byte jointId);

    /**
     * Removes the joint with otherEntity, if it exists <br>
     *     Should be fired on server side
     *
     * @param otherEntity The other entity linked to this joint
     * @param jointType The type of the joint (name of the {@link JointHandler})
     * @param jointId The local id of the joint, the same as when the joint was created
     */
    void removeJointWith(PhysicsEntity<?> otherEntity, ResourceLocation jointType, byte jointId);

    void removeJointsOfType(ResourceLocation jointType, byte jointId);

    void onRemoveJoint(EntityJoint<?> joint);

    /**
     * Updates the {@link SimulationHolder} of all linked entity, if we own the joint
     */
    void setSimulationHolderOnJointedEntities(SimulationHolder simulationHolder);

    /**
     * Sends all the joints to the target client <br>
     *     Used for player connection
     */
    void sync(EntityPlayerMP target);

    /**
     * @return All joints linked to this entity
     */
    Collection<EntityJoint<?>> getJoints();
}
