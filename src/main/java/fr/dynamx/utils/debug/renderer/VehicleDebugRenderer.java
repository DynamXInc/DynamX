package fr.dynamx.utils.debug.renderer;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.api.network.sync.SimulationHolder;
import fr.dynamx.api.physics.IRotatedCollisionHandler;
import fr.dynamx.client.network.ClientPhysicsEntitySynchronizer;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.parts.*;
import fr.dynamx.common.contentpack.type.vehicle.FrictionPoint;
import fr.dynamx.common.contentpack.type.vehicle.SteeringWheelInfo;
import fr.dynamx.common.contentpack.type.vehicle.TrailerAttachInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.vehicles.DoorEntity;
import fr.dynamx.common.entities.vehicles.HelicopterEntity;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.BoundingBoxPool;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Quaternion;

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
                new WheelDebug(),
                new FrictionPointsDebug(),
                new SteeringWheelDebug(),
                new TrailerPointsDebug(),
                new PlayerCollisionsDebug(),
                new NetworkDebug(),
                new DoorPointsDebug(),
                new PropsContainerDebug(),
                new RotorDebug());
        if (hasSeats)
            to.addDebugRenderers(new SeatDebug());
    }

    /**
     * Wheels render
     */
    public static class WheelDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.WHEELS.isActive();
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, RenderPhysicsEntity<BaseVehicleEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            MutableBoundingBox box = new MutableBoundingBox();
            //Render wheels
            for (PartWheel wheel : entity.getPackInfo().getPartsByType(PartWheel.class)) {
                wheel.getBox(box);
                box.offset(wheel.getPosition());
                if (!wheel.isDrivingWheel())
                    RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                            1, 0, 0, 1);
                else
                    RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                            0, 1, 0, 1);
            }
        }
    }
    /**
     * Rotors render
     */
    public static class RotorDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            if (entity instanceof HelicopterEntity) {
                return DynamXDebugOptions.ROTORS.isActive();
            }
            return false;
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, double x, double y, double z, float partialTicks) {
            MutableBoundingBox box = new MutableBoundingBox(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
            //Render wheels
            for (PartRotor rotor : entity.getPackInfo().getPartsByType(PartRotor.class)) {
                box.offset(rotor.getPosition());
                if (!rotor.isMainRotor())
                    RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                            204F/255, 123F/255, 0, 1);
                else
                    RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                            0, 1, 0, 1);
            }
        }
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

            /*GlStateManager.pushMatrix();
            GlStateManager.disableLighting();
            GlStateManager.disableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.translate(-entity.posX, -entity.posY, -entity.posZ);
            byte[][] cache = PhysicsEntityTerrainLoader.loadMatrice;
            int xc = entity.chunkCoordX*16;
            int yc = entity.chunkCoordY*16;
            int zc = entity.chunkCoordZ*16;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 49; j++) {
                    byte state = cache[i][j];
                    if(state != -1) {
                        int dx = (j % 7) - 3;
                        int dz = (j / 7) - 3;
                        float r = state == 3 ? 1 : 0;
                        float g = state == 1 ? 1 : 0;
                        float b = state == 2 ? 1 : 0;
                        ClientDebugSystem.drawAABBDebug(new float[]{xc + dx * 16, yc + i * 16 - 16, zc + dz * 16, xc + dx * 16 + 16, yc + i * 16, zc + dz * 16 + 16, r, g, b});
                    }
                }
            }
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();*/
        }
    }

    /**
     * Steering wheel render
     */
    public static class SteeringWheelDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.WHEELS.isActive() && entity.getPackInfo().getSubPropertyByType(SteeringWheelInfo.class) != null;
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, RenderPhysicsEntity<BaseVehicleEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            /* Rendering the steering wheel debug */
            SteeringWheelInfo info = entity.getPackInfo().getSubPropertyByType(SteeringWheelInfo.class);
            GlStateManager.pushMatrix();
            Vector3f center = info.getSteeringWheelPosition();
            //Translation to the steering wheel rotation point (and render pos)
            GlStateManager.translate(center.x, center.y, center.z);

            //Apply steering wheel base rotation
            if (info.getSteeringWheelBaseRotation() != null)
                GlStateManager.rotate(GlQuaternionPool.get(info.getSteeringWheelBaseRotation()));
            //Rotate the steering wheel
            //wtf useless in debug GlStateManager.rotate(-(entity.prevVisualProperties[0] + (entity.visualProperties[0] - entity.prevVisualProperties[0]) * partialTicks), 0F, 0F, 1F);

            //Render it
            RenderGlobal.drawBoundingBox(-0.25f, -0.25f, -0.1f, 0.25f, 0.25f, 0.1f,
                    0.5f, 1, 0.5f, 1);
            GlStateManager.popMatrix();
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
     * Door attach point render
     */
    public static class DoorPointsDebug implements DebugRenderer<PackPhysicsEntity<?, ?>> {
        @Override
        public boolean shouldRender(PackPhysicsEntity<?, ?> entity) {
            if (!DynamXDebugOptions.DOOR_ATTACH_POINTS.isActive())
                return false; //avoid following checks
            return entity instanceof IModuleContainer.IDoorContainer && ((IModuleContainer.IDoorContainer) entity).getDoors() != null;
        }

        @Override
        public void render(PackPhysicsEntity<?, ?> entity, RenderPhysicsEntity<PackPhysicsEntity<?, ?>> renderer, double x, double y, double z, float partialTicks) {
            Vector3f point = new Vector3f();
            if (entity instanceof BaseVehicleEntity) {
                for (PartDoor door : ((BaseVehicleEntity<?>) entity).getPackInfo().getPartsByType(PartDoor.class)) {
                    point = door.getCarAttachPoint();
                    RenderGlobal.drawBoundingBox(point.x - 0, point.y - 0.05f,
                            point.z - 0.05f, point.x + 0.05f, point.y + 0.05f, point.z + 0.05f,
                            1f, 1, 1, 1);

                    MutableBoundingBox box = new MutableBoundingBox();
                    door.getBox(box);
                    box.offset(door.getPosition());

                    RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                            0, 0, 1, 1);

                }

            } else if (entity instanceof DoorEntity) {
                if (((DoorEntity<?>) entity).getPackInfo() != null) {
                    point = ((DoorEntity<?>) entity).getPackInfo().getDoorAttachPoint();
                }

                RenderGlobal.drawBoundingBox(point.x - 0, point.y - 0.05f,
                        point.z - 0.05f, point.x + 0.05f, point.y + 0.05f, point.z + 0.05f,
                        0.5f, 0, 1, 1);
            }
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
                    MutableBoundingBox rotatedSize = DynamXContext.getCollisionHandler().rotateBB(Vector3fPool.get(0, 0, 0), container.getBox(), entity.physicsRotation);
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
                Vector3f serverPos = ((ClientPhysicsEntitySynchronizer)entity.getSynchronizer()).getServerPos();
                if(serverPos != null) {
                    GlStateManager.translate(- pos.x + serverPos.x, - pos.y + serverPos.y, - pos.z + serverPos.z);
                    Quaternion q = GlQuaternionPool.get(((ClientPhysicsEntitySynchronizer<? extends PhysicsEntity<?>>) entity.getSynchronizer()).getServerRotation());
                    GlStateManager.rotate(q);
                    GlStateManager.color(entity.getSynchronizer().getSimulationHolder() == SimulationHolder.DRIVER ? 0.9f : 0.1f, 0.1f, 0.8f, 0.3f);
                    renderer.renderMain(entity, partialTicks);
                    renderer.renderParts(entity, partialTicks);
                    GlStateManager.color(1, 1, 1, 1);
                }
            }
            GlStateManager.popMatrix();
            /* End of Aymeric's network debug*/
        }
    }

    /**
     * Renders seats
     */
    public static class SeatDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.SEATS_AND_STORAGE.isActive();
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, RenderPhysicsEntity<BaseVehicleEntity<?>> renderer, double x, double y, double z, float partialTicks) {
            MutableBoundingBox box = new MutableBoundingBox();
            for (PartSeat seat : entity.getPackInfo().getPartsByType(PartSeat.class)) {
                seat.getBox(box);
                box.offset(seat.getPosition());
                if (!seat.isDriver())
                    RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                            1, 0, 0, 1);
                else
                    RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                            0, 1, 0, 1);
            }
            for (PartStorage storage : entity.getPackInfo().getPartsByType(PartStorage.class)) {
                storage.getBox(box);
                box.offset(storage.getPosition());
                RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                        1, 0.7f, 0, 1);
            }
        }
    }
}
