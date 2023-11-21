package fr.dynamx.utils.debug.renderer;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.physics.IRotatedCollisionHandler;
import fr.dynamx.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.scene.EntityRenderContext;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.PartPropsContainer;
import fr.dynamx.common.contentpack.type.vehicle.FrictionPoint;
import fr.dynamx.common.contentpack.type.vehicle.TrailerAttachInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.BoundingBoxPool;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * Contains all {@link DebugRenderer}s for vehicles
 *
 * @see BoatDebugRenderer
 */
public class VehicleDebugRenderer {
    /**
     * Adds all debug renderers for a wheeled vehicle
     *
     * @param hasSeats True to add seats debug
     */
    public static <T extends PhysicsEntity<?>> void addAll(RenderPhysicsEntity<T> to, boolean hasSeats) {
        to.addDebugRenderers(
                new FrictionPointsDebug(),
                new TrailerPointsDebug(),
                new PlayerCollisionsDebug(),
                new NetworkDebug(),
                new PropsContainerDebug());
        if (hasSeats)
            to.addDebugRenderers(new DebugRenderer.StoragesDebug());
    }

    /**
     * {@link fr.dynamx.common.contentpack.type.vehicle.FrictionPoint}s render
     */
    public static class FrictionPointsDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.FRICTION_POINTS.isActive();
        }

        @Override
        public boolean hasEntityRotation(BaseVehicleEntity<?> entity) {
            return false;
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, RenderPhysicsEntity<BaseVehicleEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            if (!entity.getPackInfo().getFrictionPoints().isEmpty()) {
                float horizSpeed = Vector3fPool.get((float) entity.motionX, 0, (float) entity.motionZ).length();
                for (FrictionPoint f : entity.getPackInfo().getFrictionPoints()) {
                    Vector3f pushDown = new Vector3f((float) -entity.motionX, -horizSpeed, (float) -entity.motionZ);
                    pushDown.multLocal(f.getIntensity());
                    Vector3f pos = f.getPosition();
                    pos = DynamXGeometry.rotateVectorByQuaternion(pos, entity.renderRotation);

                    GlStateManager.color(1, 0, 0, 1);
                    DynamXRenderUtils.drawBoundingBox(Vector3fPool.get(pos).addLocal(-0.04f, -0.04f, -0.04f), Vector3fPool.get(pos).addLocal(0.04f, 0.04f, 0.04f), 0, 1, 0, 1);
                    GlStateManager.glBegin(GL11.GL_LINES);
                    GlStateManager.glVertex3f(pos.x, pos.y, pos.z);
                    pos.addLocal(pushDown);
                    GlStateManager.glVertex3f(pos.x, pos.y, pos.z);
                    GlStateManager.glEnd();
                }
            }
        }
    }

    /**
     * Trailer attach point render
     */
    public static class TrailerPointsDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.TRAILER_ATTACH_POINTS.isActive() && entity.getPackInfo().getSubPropertyByType(TrailerAttachInfo.class) != null;
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, RenderPhysicsEntity<BaseVehicleEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            Vector3f p1 = entity.getPackInfo().getSubPropertyByType(TrailerAttachInfo.class).getAttachPoint();
            RenderGlobal.drawBoundingBox(p1.x - 0, p1.y - 0.05f,
                    p1.z - 0.05f, p1.x + 0.05f, p1.y + 0.05f, p1.z + 0.05f,
                    0.5f, 0, 1, 1);
        }
    }

    /**
     * Player collision debug : shows un-rotated and rotated boxes of player and vehicle
     *
     * @see IRotatedCollisionHandler
     */
    public static class PlayerCollisionsDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        public static AxisAlignedBB lastTemp;
        //Used for rotation debug
        public static Vec3d pos;
        public static Vector3f motion;
        public static Vector3f rotatedmotion;
        public static Vector3f realmotionrot;
        public static Vector3f realmotion;

        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.PLAYER_COLLISIONS.isActive();
        }

        @Override
        public boolean hasEntityRotation(BaseVehicleEntity<?> entity) {
            return false;
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, RenderPhysicsEntity<BaseVehicleEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            /* Start of Aymeric's collision debug */
            GlStateManager.pushMatrix();
            GlStateManager.translate(-entity.posX, -entity.posY, -entity.posZ);

            try {
                for (MutableBoundingBox bb : entity.getCollisionBoxes()) {

                    RenderGlobal.drawBoundingBox(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, 1, 1, 0, 1);
                }
            } catch (ConcurrentModificationException e) {
                e.printStackTrace();
            }

            if (lastTemp != null) {
                RenderGlobal.drawSelectionBoundingBox(lastTemp, 0, 1, 1, 1);
            }
            //if(RotatedCollisionHandler.pos != null)
            //GlStateManager.translate(RotatedCollisionHandler.pos.x, RotatedCollisionHandler.pos.y, RotatedCollisionHandler.pos.z);
            if (motion != null) {
                RenderGlobal.drawBoundingBox(pos.x, pos.y, pos.z,
                        motion.x * 10 + pos.x, motion.y * 10 + pos.y,
                        motion.z * 10 + pos.z, 1, 0, 0, 1);
            }
            if (rotatedmotion != null) {
                RenderGlobal.drawBoundingBox(pos.x, pos.y, pos.z,
                        rotatedmotion.x * 10 + pos.x, rotatedmotion.y * 10 + pos.y,
                        rotatedmotion.z * 10 + pos.z, 0, 1, 0, 1);
            }
            if (realmotionrot != null) {
                RenderGlobal.drawBoundingBox(pos.x, pos.y, pos.z,
                        realmotionrot.x * 10 + pos.x, realmotionrot.y * 10 + pos.y,
                        realmotionrot.z * 10 + pos.z, 1, 0, 1, 1);
            }
            if (realmotion != null) {
                RenderGlobal.drawBoundingBox(pos.x, pos.y, pos.z,
                        realmotion.x * 10 + pos.x, realmotion.y * 10 + pos.y,
                        realmotion.z * 10 + pos.z, 0, 0, 1, 1);
            }

            BoundingBoxPool.getPool().openSubPool();
            DynamXContext.getPlayerToCollision().forEach((player, playerPhysicsHandler) -> {
                if (playerPhysicsHandler.getBodyIn() != null) {
                    BoundingBox bb = playerPhysicsHandler.getBodyIn().boundingBox(BoundingBoxPool.get());
                    Vector3f min = bb.getMin(Vector3fPool.get());
                    Vector3f max = bb.getMax(Vector3fPool.get());
                    RenderGlobal.drawBoundingBox(min.x, min.y, min.z, max.x, max.y, max.z, 0.2f, 0.5f, 0.7f, 1);
                }
            });
            BoundingBoxPool.getPool().closeSubPool();
            GlStateManager.popMatrix();
            /* End of Aymeric's collision debug*/
        }
    }

    /**
     * Player collision debug : shows un-rotated and rotated boxes of player and vehicle
     *
     * @see IRotatedCollisionHandler
     */
    public static class PropsContainerDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.PROPS_CONTAINERS.isActive();
        }

        @Override
        public boolean hasEntityRotation(BaseVehicleEntity<?> entity) {
            return false;
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, RenderPhysicsEntity<BaseVehicleEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            List<PartPropsContainer> containers = entity.getPackInfo().getPartsByType(PartPropsContainer.class);
            if (!containers.isEmpty()) {
                for (PartPropsContainer container : containers) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(-entity.posX, -entity.posY, -entity.posZ);

                    Vector3f pos = DynamXGeometry.rotateVectorByQuaternion(container.getPosition(), entity.physicsRotation);
                    MutableBoundingBox rotatedSize = DynamXContext.getCollisionHandler().rotateBB(Vector3fPool.get(0, 0, 0), container.getBoundingBox(), entity.physicsRotation);
                    rotatedSize = rotatedSize.offset(pos);
                    rotatedSize = rotatedSize.offset(entity.physicsPosition);
                    DynamXRenderUtils.drawBoundingBox(rotatedSize.toBB(), 1, 0, 0, 1);

                    GlStateManager.popMatrix();
                }
            }
        }
    }

    /**
     * Network debug : render previous entity states
     */
    public static class NetworkDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.FULL_NETWORK_DEBUG.isActive() && entity.getSynchronizer() instanceof ClientPhysicsEntitySynchronizer;
        }

        @Override
        public boolean hasEntityRotation(BaseVehicleEntity<?> entity) {
            return false;
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, RenderPhysicsEntity<BaseVehicleEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            /* Start of Aymeric's network debug */
            GlStateManager.pushMatrix();
            {
                Vector3f pos = entity.physicsPosition;
                Vector3f serverPos = ((ClientPhysicsEntitySynchronizer) entity.getSynchronizer()).getServerPos();
                if (serverPos != null) {
                    GlStateManager.color(entity.getSynchronizer().getSimulationHolder() == SimulationHolder.DRIVER ? 0.9f : 0.1f, 0.1f, 0.8f, 0.3f);
                    EntityRenderContext context = renderer.getRenderContext(entity);
                    if (context != null)
                        renderer.renderEntity(entity, context.setRenderParams(-pos.x + serverPos.x, -pos.y + serverPos.y, -pos.z + serverPos.z, partialTicks, true));
                    GlStateManager.color(1, 1, 1, 1);
                }
            }
            GlStateManager.popMatrix();
            /* End of Aymeric's network debug*/
        }
    }
}
