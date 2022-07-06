package fr.dynamx.client.renders;

import com.jme3.math.Vector3f;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.QuaternionPool;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Quaternion;

public class RenderMovableLine {

    public static void renderLine(float partialTicks) {
        DynamXContext.getPlayerPickingObjects().forEach((playerEntityID, physicsEntityID) -> {
            /* Prevent line rendering for other players */
            if (playerEntityID == Minecraft.getMinecraft().player.getEntityId()) {
                EntityPlayer player = (EntityPlayer) Minecraft.getMinecraft().world.getEntityByID(playerEntityID);
                PhysicsEntity<?> physicsEntity = (PhysicsEntity<?>) Minecraft.getMinecraft().world.getEntityByID(physicsEntityID);
                if (player != null && physicsEntity != null) {
                    MovableModule movableModule = physicsEntity.getModuleByType(MovableModule.class);
                    if (movableModule != null) {

                        GlStateManager.pushMatrix();
                        GlStateManager.disableLighting();
                        GlStateManager.disableTexture2D();

                        float physicsEntityX = (float) (physicsEntity.prevPosX + (physicsEntity.posX - physicsEntity.prevPosX) * partialTicks);
                        float physicsEntityY = (float) (physicsEntity.prevPosY + (physicsEntity.posY - physicsEntity.prevPosY) * partialTicks);
                        float physicsEntityZ = (float) (physicsEntity.prevPosZ + (physicsEntity.posZ - physicsEntity.prevPosZ) * partialTicks);

                        GlStateManager.translate(physicsEntityX, physicsEntityY, physicsEntityZ);

                        Vector3f firstPersonOffset = Vector3fPool.get(0, 0, 0.35f);

                        float interYaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;
                        float interPitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
                        Vector3f firstPersonOffsetRot = DynamXGeometry.getRotatedPoint(firstPersonOffset, interPitch, interYaw, 0);


                        Vector3f target = Vector3fPool.get(DynamXUtils.getCameraTranslation(Minecraft.getMinecraft(), partialTicks)).add(firstPersonOffsetRot);
                        target.subtractLocal(physicsEntityX, physicsEntityY - player.getEyeHeight(), physicsEntityZ);

                        Quaternion rotQuat = ClientDynamXUtils.computeInterpolatedGlQuaternion(
                                physicsEntity.prevRenderRotation,
                                physicsEntity.renderRotation,
                                partialTicks);

                        Vector3f pivot = DynamXGeometry.rotateVectorByQuaternion(movableModule.pickObjects.getLocalPickPosition(), QuaternionPool.get(rotQuat.x, rotQuat.y, rotQuat.z, rotQuat.w));

                        GlStateManager.glBegin(GL11.GL_LINE_STRIP);
                        GlStateManager.glVertex3f(target.x, target.y, target.z);
                        GlStateManager.glVertex3f(pivot.x, pivot.y, pivot.z);
                        GlStateManager.glEnd();

                        GlStateManager.enableLighting();
                        GlStateManager.enableTexture2D();
                        GlStateManager.popMatrix();
                    }
                }
            }

        });
    }
}
