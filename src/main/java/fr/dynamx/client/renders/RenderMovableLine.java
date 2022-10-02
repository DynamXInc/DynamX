package fr.dynamx.client.renders;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.common.entities.modules.MovableModule;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.maths.DynamXGeometry;
import fr.dynamx.utils.optimization.Vector3fPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.util.Map;

public class RenderMovableLine {

    public static void renderLine(float partialTicks) {
        for (Map.Entry<Integer, Integer> entry : DynamXContext.getPlayerPickingObjects().entrySet()) {
            Integer playerEntityID = entry.getKey();
            Integer physicsEntityID = entry.getValue();
            /* Prevent line rendering for other players */
            if (playerEntityID == Minecraft.getMinecraft().player.getEntityId()) {
                EntityPlayer player = (EntityPlayer) Minecraft.getMinecraft().world.getEntityByID(playerEntityID);
                PhysicsEntity<?> physicsEntity = (PhysicsEntity<?>) Minecraft.getMinecraft().world.getEntityByID(physicsEntityID);
                if (player == null || physicsEntity == null) {
                    continue;
                }
                MovableModule movableModule = physicsEntity.getModuleByType(MovableModule.class);
                if (movableModule == null) {
                    continue;
                }
                PhysicsRigidBody hitBody = movableModule.pickObjects.hitBody;
                if(hitBody == null){
                    continue;
                }

                GlStateManager.pushMatrix();
                GlStateManager.disableLighting();
                GlStateManager.disableTexture2D();

                Vector3f physicsLocation = ClientDebugSystem.getInterpolatedTranslation(hitBody, partialTicks);
                Quaternion physicsRotation = ClientDebugSystem.getInterpolatedRotation(hitBody, partialTicks);
                DynamXRenderUtils.glTranslate(physicsLocation);

                Vector3f firstPersonOffset = Vector3fPool.get(0, 0, 0.35f);

                float interYaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;
                float interPitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
                Vector3f firstPersonOffsetRot = DynamXGeometry.getRotatedPoint(firstPersonOffset, interPitch, interYaw, 0);

                Vector3f target = Vector3fPool.get(DynamXUtils.getCameraTranslation(Minecraft.getMinecraft(), partialTicks)).add(firstPersonOffsetRot);
                target.subtractLocal(physicsLocation.x, physicsLocation.y - player.getEyeHeight(), physicsLocation.z);

                Vector3f pivot = DynamXGeometry.rotateVectorByQuaternion(movableModule.pickObjects.getLocalPickPosition(), physicsRotation);

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
}
