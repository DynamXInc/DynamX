package fr.dynamx.common.core.mixin;

import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import fr.dynamx.utils.optimization.QuaternionPool;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Quaternion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = RenderManager.class, remap = MixinChunk.REMAP)
public abstract class MixinRenderManager {
    @Inject(method = "renderEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/Render;doRender(Lnet/minecraft/entity/Entity;DDDFF)V",
                    shift = At.Shift.AFTER))
    private void postDoRenderEntities(Entity entityIn, double x, double y, double z, float yaw, float partialTicks, boolean p_188391_10_, CallbackInfo ci) {
        if (MinecraftForgeClient.getRenderPass() == 0) {
            if (entityIn instanceof BaseVehicleEntity && ((BaseVehicleEntity<?>) entityIn).getPackInfo() != null) {
                GlQuaternionPool.openPool();
                QuaternionPool.openPool();
                BaseVehicleEntity<?> physicsEntity = (BaseVehicleEntity<?>) entityIn;
                ObjModelRenderer model = DynamXContext.getObjModelRegistry().getModel(physicsEntity.getPackInfo().getModel());
                GL11.glEnable(GL11.GL_STENCIL_TEST);
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
                GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
                GL11.glStencilMask(0xFF);
                GL11.glColorMask(false, false, false, false);
                GL11.glDepthMask(false);

                GlStateManager.pushMatrix();
                GlStateManager.translate((float) x, (float) y, (float) z);
                Quaternion q = ClientDynamXUtils.computeInterpolatedGlQuaternion(physicsEntity.prevRenderRotation, physicsEntity.renderRotation, partialTicks);
                GlStateManager.rotate(q);

                model.renderGroups("AntiWater1", (byte) 0);
                model.renderGroups("AntiWater2", (byte) 0);

                GlStateManager.popMatrix();
                QuaternionPool.closePool();
                GlQuaternionPool.closePool();

                GL11.glColorMask(true, true, true, true);
                GL11.glDepthMask(true);
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
                GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
                GL11.glStencilMask(0x00);
            }
        }
    }
}
