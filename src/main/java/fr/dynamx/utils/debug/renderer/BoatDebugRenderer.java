package fr.dynamx.utils.debug.renderer;

import com.jme3.math.Vector3f;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.contentpack.parts.PartFloat;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.vehicles.BoatEntity;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class BoatDebugRenderer {
    public static <T extends PhysicsEntity<?>> void addAll(RenderPhysicsEntity<T> to) {
        to.addDebugRenderers(new WaterLevelDebug(), new FloatsDebug(), new VehicleDebugRenderer.SteeringWheelDebug(), new VehicleDebugRenderer.SeatDebug(), new VehicleDebugRenderer.PlayerCollisionsDebug(), new VehicleDebugRenderer.NetworkDebug());
    }

    public static class FloatsDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.WHEELS.isActive();
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, double x, double y, double z, float partialTicks) {
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.disableLighting();
            GlStateManager.color(0, 0, 1, 0.2f);
            GlStateManager.disableCull();
            int i = 0;
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            BoatEntity.BoatPhysicsHandler<?> physicsHandler = (BoatEntity.BoatPhysicsHandler<?>) entity.physicsHandler;
            for (PartFloat f : physicsHandler.floatList) {
                for (Vector3f floater : f.listFloaters) {
                    DynamXRenderUtils.drawBoundingBox(floater.subtract(f.size/2, f.size/2, f.size/2),
                            floater.add(f.size/2, f.size/2, f.size/2), 0, 1, 0, 1);

                    Vector3f buoyForce = physicsHandler.buoyForces.get(i++);
                    bufferbuilder.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
                    bufferbuilder.pos(floater.x, floater.y, floater.z).color(1f, 0, 1, 1f).endVertex();
                    bufferbuilder.pos(floater.x + buoyForce.x + 0.0001, floater.y + buoyForce.y, floater.z + buoyForce.z + 0.0001).color(1f, 0, 0, 1f).endVertex();
                    tessellator.draw();
                }
            }
            GlStateManager.enableCull();
            GlStateManager.popMatrix();
        }
    }

    public static class WaterLevelDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.WHEELS.isActive();
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, double x, double y, double z, float partialTicks) {

        }

        @Override
        public boolean hasEntityRotation(BaseVehicleEntity<?> entity) {
            return false;
        }
    }
}
