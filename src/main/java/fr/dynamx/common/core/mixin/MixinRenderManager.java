package fr.dynamx.common.core.mixin;

import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.renderer.entity.RenderManager;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Second step for our stencil test
 * In this step we render the mask meshes and hide them. Then, we write the meshes bits to the stencil buffer so that
 * we can make them fail the test in the third and final step
 * @see MixinRenderGlobal for the final step
 */
@Mixin(value = RenderManager.class, remap = DynamXConstants.REMAP)
public abstract class MixinRenderManager {

        /* todo Yanis
    @Inject(method = "renderEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/Render;doRender(Lnet/minecraft/entity/Entity;DDDFF)V",
                    shift = At.Shift.AFTER))
    private void postDoRenderEntities(Entity entityIn, double x, double y, double z, float yaw, float partialTicks, boolean p_188391_10_, CallbackInfo ci) {

        if (MinecraftForgeClient.getRenderPass() == 0) {
            if (entityIn instanceof BaseVehicleEntity && ((BaseVehicleEntity<?>) entityIn).getPackInfo() != null) {
                GlQuaternionPool.openPool();
                QuaternionPool.openPool();
                BaseVehicleEntity<?> physicsEntity = (BaseVehicleEntity<?>) entityIn;
                DxModelRenderer model = DynamXContext.getDxModelRegistry().getModel(physicsEntity.getPackInfo().getModel());
                //Applies a color mask to mask the following meshes
                GL11.glColorMask(false, false, false, false);
                //We want our meshes bits to replace the already contained stencil bits
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
                //Following meshes always pass the stencil test
                GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
                //Enables our meshes bits to be written in the stencil buffer
                GL11.glStencilMask(0xFF);
                //Disables the depth mask because we don't need it
                GL11.glDepthMask(false);

                GlStateManager.pushMatrix();
                GlStateManager.translate((float) x, (float) y, (float) z);
                Quaternion q = ClientDynamXUtils.computeInterpolatedGlQuaternion(physicsEntity.prevRenderRotation, physicsEntity.renderRotation, partialTicks);
                GlStateManager.rotate(q);

                model.renderGroups("AntiWater1", (byte) 0, false);
                model.renderGroups("AntiWater2", (byte) 0, false);

                GlStateManager.popMatrix();
                QuaternionPool.closePool();
                GlQuaternionPool.closePool();

                //Removes the color mask to see everything
                GL11.glColorMask(true, true, true, true);
                //Enables back the depth mask
                GL11.glDepthMask(true);
                //Default opengl stencil operation
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
                //We always want to see everything else
                GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
                //Disables writing to the stencil buffer
                GL11.glStencilMask(0x00);
            }
        }
    }*/
}
