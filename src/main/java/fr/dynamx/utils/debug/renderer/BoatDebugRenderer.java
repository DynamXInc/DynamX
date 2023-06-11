package fr.dynamx.utils.debug.renderer;

import com.jme3.math.Vector3f;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.contentpack.parts.PartFloat;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.physics.entities.BoatPhysicsHandler;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class BoatDebugRenderer {
    public static <T extends PhysicsEntity<?>> void addAll(RenderPhysicsEntity<T> to) {
        to.addDebugRenderers(new FloatsDebug(), new VehicleDebugRenderer.SteeringWheelDebug(), new VehicleDebugRenderer.SeatDebug(), new VehicleDebugRenderer.PlayerCollisionsDebug(), new VehicleDebugRenderer.NetworkDebug());
    }

    public static class FloatsDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.WHEELS.isActive();
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, RenderPhysicsEntity<BaseVehicleEntity<?>> renderer, double x, double y, double z, float partialTicks) {
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
            BoatPhysicsHandler<?> physicsHandler = (BoatPhysicsHandler<?>) entity.physicsHandler;
            for (PartFloat f : physicsHandler.floatList) {
                for (Vector3f floater : f.childFloatsPos) {
                    DynamXRenderUtils.drawBoundingBox(floater.subtract(f.size/2, f.getScale().y/2, f.size/2),
                            floater.add(f.size/2, f.getScale().y/2, f.size/2), 0, 1, 0, 1);
                    drawForce(tessellator, bufferbuilder, floater, physicsHandler.buoyForces.get(i), 1,0,0);
                    drawForce(tessellator, bufferbuilder, floater, physicsHandler.dragForces.get(i), 1,1,0);
                    i++;
                }
            }
            GlStateManager.enableCull();
            GlStateManager.popMatrix();
        }
        private void drawForce(Tessellator tessellator, BufferBuilder bufferBuilder, Vector3f pos, Vector3f force, float red, float green, float blue){
            bufferBuilder.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            bufferBuilder.pos(pos.x, pos.y, pos.z).color(1f, 0, 1, 1f).endVertex();
            bufferBuilder.pos(pos.x + force.x + 0.0001, pos.y + force.y, pos.z + force.z + 0.0001).color(red, green, blue, 1f).endVertex();
            tessellator.draw();
        }

    }
}
