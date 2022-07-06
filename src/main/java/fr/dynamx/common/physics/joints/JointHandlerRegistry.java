package fr.dynamx.common.physics.joints;

import fr.dynamx.common.entities.PhysicsEntity;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of {@link JointHandler}, required to sync joints and load them from world saves
 */
public class JointHandlerRegistry
{
    private static final Map<ResourceLocation, JointHandler<?, ?, ?>> HANDLERS = new HashMap<>();

    /**
     * Registers a joint handler
     *
     * @param handler the handler of a joint, responsible for creating and destroying the joint
     */
    public static void register(JointHandler<?, ?, ?> handler) {
        if(HANDLERS.containsKey(handler.getType()))
            throw new IllegalArgumentException("JointHandler "+handler.getType()+" already registered !");
        HANDLERS.put(handler.getType(), handler);
    }

    /**
     * @return The joint handler matching with the given name
     * @throws IllegalArgumentException If no handler was found
     */
    public static JointHandler<?, ?, ?> getHandler(ResourceLocation name) {
        if(!HANDLERS.containsKey(name))
            throw new IllegalArgumentException("JointHandler "+name+" does not exists");
        return HANDLERS.get(name);
    }

    /**
     * @return The joint handler matching with the given name, or null if no handler was found
     */
    @Nullable
    public static JointHandler<?, ?, ?> getHandlerUnsafe(ResourceLocation name) {
        return HANDLERS.containsKey(name) ? HANDLERS.get(name) : null;
    }

    /**
     * Creates a joint in the given entity
     *
     * @param name The {@link JointHandler} registry name
     * @param entity The affected entity
     * @param jointID The local id of the joint, useful if you have multiple joints on this JointHandler <br> Should be unique for each joint
     */
    public static void createJointWithSelf(ResourceLocation name, PhysicsEntity<?> entity, byte jointID) {
        getHandler(name).createJoint(entity, entity, jointID);
    }

    /**
     * Creates a joint between the two given entities
     *
     * @param name The {@link JointHandler} registry name
     * @param entity1 The main entity
     * @param entity2 The other entity
     * @param jointID The local id of the joint, useful if you have multiple joints on this JointHandler <br> Should be unique for each joint
     */
    public static void createJointWithOther(ResourceLocation name, PhysicsEntity<?> entity1, PhysicsEntity<?> entity2, byte jointID) {
        getHandler(name).createJoint(entity1, entity2, jointID);
    }
}
