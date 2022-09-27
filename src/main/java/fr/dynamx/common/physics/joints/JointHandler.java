package fr.dynamx.common.physics.joints;

import com.jme3.bullet.joints.Constraint;
import fr.dynamx.api.entities.modules.AttachModule;
import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;
import net.minecraft.util.ResourceLocation;

/**
 * Handles joint creation, re-creation (on entity load, server->client sync...) and destruction <br>
 * Works with an {@link AttachModule} <br>
 * Should be registered in the {@link JointHandlerRegistry} <br>
 *
 * @param <A> The class of the entity owning the joint (main entity) (the entity that can be driven, for example the car)
 * @param <B> The class of the second entity, not driveable (other entity) (for example the trailer or the door)
 * @param <D> The class of the {@link AttachModule}, contained in each entity and returned by the getModulesByType function of each entity
 */
public class JointHandler<A extends PhysicsEntity<?>, B extends PhysicsEntity<?>, D extends AttachModule<B> & IPhysicsModule<?>> {
    private final ResourceLocation type;
    private final Class<A> entity1;
    private final Class<B> entity2;
    private final Class<D> attachModule;

    /**
     * @param type         The registry name of this JointHandler, should be unique
     * @param entity1      The class of the entity owning the joint (the entity that can be driven, for example the car)
     * @param entity2      The class of the second entity, not driveable (for example the trailer or the door)
     * @param attachModule The class of the {@link AttachModule}, contained in each entity and returned by the getModulesByType function of each entity
     */
    public JointHandler(ResourceLocation type, Class<A> entity1, Class<B> entity2, Class<D> attachModule) {
        this.type = type;
        this.entity1 = entity1;
        this.entity2 = entity2;
        this.attachModule = attachModule;
    }

    /**
     * @return The registry name of this JointHandler, unique
     */
    public ResourceLocation getType() {
        return type;
    }

    /**
     * Tries to create a new joint between the two entities <br>
     * The two entities must have a module of the type of D, which will create the joint instance <br>
     * In the base implementation, the joint won't be created if one of the AttachModule D refuses the connection <br>
     * Should be fired on server side
     *
     * @param entity1 The first entity of the joint (the order doesn't matter)
     * @param entity2 The second entity of the joint (the order doesn't matter)
     * @param jointId The local id of the joint, useful if you have multiple joints on this JointHandler <br> Should be unique for each joint
     */
    public boolean createJoint(PhysicsEntity<?> entity1, PhysicsEntity<?> entity2, byte jointId) {
        A main = findEntity(getEntity1(), entity1, entity2);
        B attached = findEntity(getEntity2(), entity1, entity2);
        if (main == attached) {
            main = (A) entity1;
            attached = (B) entity2;
        }
        if (isValidEntity(main, attached, jointId) && isValidEntity(attached, main, jointId)) {
            //Use null as constraint if we are not using physics
            //The allows to sync the joint to the client even if we are not simulating it here
            Constraint joint = DynamXContext.usesPhysicsWorld(entity1.world) ? main.getModuleByType(getAttachModuleClass()).createJoint(attached, jointId) : null;
            main.getJointsHandler().addJoint(this, joint, jointId, attached);
            return true;
        } else {
            DynamXMain.log.warn("[Joint System] Failed to (re-)attach " + entity1 + " with " + entity2);
        }
        return false;
    }

    /**
     * Fired when joint is removed from entity <br>
     * Transmits the information to the {@link AttachModule}
     */
    public void onDestroy(EntityJoint<?> joint, PhysicsEntity<?> entity) {
        entity.getModuleByType(getAttachModuleClass()).onJointDestroyed(joint);
    }

    /**
     * @return True if this entity owns the joint (ie if it's the entity of type A)
     */
    public boolean isJointOwner(EntityJoint<?> joint, PhysicsEntity<?> entity) {
        return getEntity1().isAssignableFrom(entity.getClass());
    }

    /**
     * @return The class of the entity owning the joint (the entity that can be driven, for example the car)
     */
    protected Class<A> getEntity1() {
        return entity1;
    }

    /**
     * @return The class of the second entity, not driveable (for example the trailer or the door) <br>
     */
    protected Class<B> getEntity2() {
        return entity2;
    }

    /**
     * @return The class of the {@link AttachModule}, contained in each entity and returned by the getModulesByType function of each entity
     */
    protected Class<D> getAttachModuleClass() {
        return attachModule;
    }

    /**
     * Fired before creating a joint
     *
     * @param jointId The local id of the joint being created
     * @return True only if the entity is not null and has an AttachModule of the type of D, an the attach module accepts a new connection with the jointId
     */
    public boolean isValidEntity(PhysicsEntity<?> entity, PhysicsEntity<?> otherEntity, byte jointId) {
        return entity != null && entity.getModuleByType(getAttachModuleClass()) != null && entity.getModuleByType(getAttachModuleClass()).canCreateJoint(otherEntity, jointId);
    }

    /**
     * @return The entity that is assignable from toFind (ie they have the same class or superclass), between e1 and e2, or null
     */
    @SuppressWarnings("unchecked")
    public static <T extends PhysicsEntity<?>> T findEntity(Class<T> toFind, PhysicsEntity<?> e1, PhysicsEntity<?> e2) {
        return e1 == null ? (e2 == null ? null : toFind.isAssignableFrom(e2.getClass()) ? (T) e2 : null) : toFind.isAssignableFrom(e1.getClass()) ? (T) e1 : (e2 == null ? null : toFind.isAssignableFrom(e2.getClass()) ? (T) e2 : null);
    }
}