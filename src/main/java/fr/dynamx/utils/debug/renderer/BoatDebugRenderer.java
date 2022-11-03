package fr.dynamx.utils.debug.renderer;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.common.contentpack.parts.PartFloat;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.vehicles.BoatEntity;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import fr.dynamx.utils.optimization.MutableBoundingBox;
import fr.dynamx.utils.optimization.TransformPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import fr.dynamx.utils.physics.DynamXPhysicsHelper;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
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
            //if(carEntity.physicsPosition.y <= 40)
            {
                int i = 0;
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder bufferbuilder = tessellator.getBuffer();
                for (PartFloat f : entity.getPackInfo().getPartsByType(PartFloat.class)) {
                    Vector3f p = f.getPosition();

                    /*BoatEntity.BoatPhysicsHandler a = (BoatEntity.BoatPhysicsHandler) entity.physicsHandler;
                    Vector3f surfPos = Vector3fPool.get(a.getPosition().x, 40, a.getPosition().z);
                    p = DynamXPhysicsHelper.calculateBuoyantCenter((PhysicsRigidBody) a.getCollisionObject(), surfPos, Vector3fPool.get(0, 1, 0));
                    Vector3f center = a.getCollisionObject().getTransform(TransformPool.get()).transformVector(p, Vector3fPool.get());
                    p = center.subtract(a.getCollisionObject().getTransform(TransformPool.get()).getTranslation());*/

                    if (BoatEntity.ntmdrag[i + 5] != null) {
                        bufferbuilder.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
                        bufferbuilder.pos(p.x, p.y, p.z).color(1f, 0, 1, 1f).endVertex();
                        bufferbuilder.pos(p.x + BoatEntity.ntmdrag[i + 5].x + 0.0001, p.y + BoatEntity.ntmdrag[i + 5].y, p.z + BoatEntity.ntmdrag[i + 5].z + 0.0001).color(1f, 0, 0, 1f).endVertex();
                        tessellator.draw();
                    }
                    if (BoatEntity.ntm[i] != null) {
                        bufferbuilder.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
                        bufferbuilder.pos(p.x, p.y, p.z).color(1f, 0, 1, 1f).endVertex();
                        bufferbuilder.pos(p.x + BoatEntity.ntm[i].x + 0.0001, p.y + BoatEntity.ntm[i].y, p.z + BoatEntity.ntm[i].z + 0.0001).color(1f, 0, 1, 1f).endVertex();
                        tessellator.draw();
                    }
                    if (BoatEntity.ntmdrag[i] != null) {
                        bufferbuilder.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
                        bufferbuilder.pos(p.x, p.y, p.z).color(1f, 1, 0, 1f).endVertex();
                        bufferbuilder.pos(p.x + BoatEntity.ntmdrag[i].x + 0.0001, p.y + BoatEntity.ntmdrag[i].y, p.z + BoatEntity.ntmdrag[i].z + 0.0001).color(1f, 1, 0, 1f).endVertex();
                        tessellator.draw();
                    }
                    i++;
                }
                //System.out.println(DynamXMain.physicsWorld.getDynamicsWorld().getGravity(Vector3fPool.get())+" "+packInfo.getEmptyMass());
            }
            GlStateManager.enableCull();
            GlStateManager.popMatrix();

            MutableBoundingBox box = new MutableBoundingBox();
            //Render floats
            for (PartFloat wheel : entity.getPackInfo().getPartsByType(PartFloat.class)) {
                box.setTo(wheel.box);
                box.offset(wheel.getPosition());
                RenderGlobal.drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                        0, 0, 1, 1);
            }
        }
    }

    public static class WaterLevelDebug implements DebugRenderer<BaseVehicleEntity<?>> {
        @Override
        public boolean shouldRender(BaseVehicleEntity<?> entity) {
            return DynamXDebugOptions.WHEELS.isActive();
        }

        @Override
        public void render(BaseVehicleEntity<?> entity, double x, double y, double z, float partialTicks) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, -y - ClientEventHandler.MC.player.posY, 0);
            GlStateManager.glBegin(GL11.GL_QUADS);
            GlStateManager.color(0, 0, 1, 0.2f);
            ClientDebugSystem.fillFaceBox((float) -20, (float) (39.9999999999f), (float) -20, (float) +20, (float) (40.000000001f), (float) +20);
            GlStateManager.glEnd();
            GlStateManager.translate(0, y + ClientEventHandler.MC.player.posY, 0);
            GlStateManager.rotate(180, 1, 0, 0);
            GlStateManager.translate(0, -y - ClientEventHandler.MC.player.posY, 0);
            GlStateManager.glBegin(GL11.GL_QUADS);
            GlStateManager.color(0, 0, 1, 0.2f);
            ClientDebugSystem.fillFaceBox((float) -20, (float) (39.9999999999f), (float) -20, (float) +20, (float) (40.000000001f), (float) +20);
            GlStateManager.glEnd();
            GlStateManager.popMatrix();
        }

        @Override
        public boolean hasEntityRotation(BaseVehicleEntity<?> entity) {
            return false;
        }
    }
}
