package fr.dynamx.utils.debug.renderer;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.IShapeInfo;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;

import java.util.List;

/**
 * A debug renderer for a {@link fr.dynamx.client.renders.RenderPhysicsEntity}
 *
 * @param <T> The entity type
 * @see VehicleDebugRenderer
 * @see BoatDebugRenderer
 */
public interface DebugRenderer<T extends PhysicsEntity<?>> {
    /**
     * @return True to render at this frame
     */
    boolean shouldRender(T entity);

    /**
     * Return true to automatically render with the entity's rotation, optimized to keep the same transform for each DebugRenderer
     *
     * @return True by default
     */
    default boolean hasEntityRotation(T entity) {
        return true;
    }

    /**
     * Renders this debug
     */
    void render(T entity, RenderPhysicsEntity<T> renderer, double x, double y, double z, float partialTicks);

    /**
     * Entity collisions shapes render
     */
    class ShapesDebug implements DebugRenderer<PhysicsEntity<?>> {
        @Override
        public boolean shouldRender(PhysicsEntity<?> entity) {
            return entity instanceof PackPhysicsEntity && DynamXDebugOptions.PLAYER_TO_OBJECT_COLLISION_DEBUG.isActive();
        }

        @Override
        public void render(PhysicsEntity<?> entity, RenderPhysicsEntity<PhysicsEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            for (IShapeInfo shapeInfo : ((PackPhysicsEntity<?, ?>) entity).getPackInfo().getShapes()) {
                RenderGlobal.drawBoundingBox(
                        (shapeInfo.getPosition().x - shapeInfo.getSize().x),
                        (shapeInfo.getPosition().y - shapeInfo.getSize().y),
                        (shapeInfo.getPosition().z - shapeInfo.getSize().z),
                        (shapeInfo.getPosition().x + shapeInfo.getSize().x),
                        (shapeInfo.getPosition().y + shapeInfo.getSize().y),
                        (shapeInfo.getPosition().z + shapeInfo.getSize().z),
                        0, 1, 1, 1);
            }
        }
    }

    /**
     * Center of mass render
     */
    class CenterOfMassDebug implements DebugRenderer<PhysicsEntity<?>> {
        @Override
        public boolean shouldRender(PhysicsEntity<?> entity) {
            return entity instanceof PackPhysicsEntity && DynamXDebugOptions.CENTER_OF_MASS.isActive();
        }

        @Override
        public void render(PhysicsEntity<?> entity, RenderPhysicsEntity<PhysicsEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            RenderGlobal.drawBoundingBox(-((PackPhysicsEntity<?, ?>) entity).getPackInfo().getCenterOfMass().x - 0.05f, -((PackPhysicsEntity<?, ?>) entity).getPackInfo().getCenterOfMass().y - 0.05f,
                    -((PackPhysicsEntity<?, ?>) entity).getPackInfo().getCenterOfMass().z - 0.05f, -((PackPhysicsEntity<?, ?>) entity).getPackInfo().getCenterOfMass().x + 0.05f,
                    -((PackPhysicsEntity<?, ?>) entity).getPackInfo().getCenterOfMass().y + 0.05f, -((PackPhysicsEntity<?, ?>) entity).getPackInfo().getCenterOfMass().z + 0.05f,
                    1, 0, 1, 1);
        }
    }

}
