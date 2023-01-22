package fr.dynamx.common.core.mixin;

import fr.dynamx.client.renders.RenderPhysicsEntity;
import fr.dynamx.client.renders.model.renderer.ObjModelRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.PhysicsEntity;
import fr.dynamx.utils.client.ClientDynamXUtils;
import fr.dynamx.utils.optimization.GlQuaternionPool;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Quaternion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;


@Mixin(value = RenderManager.class, remap = MixinChunk.REMAP)
public abstract class MixinRenderManager {

    @Shadow @Nullable public abstract <T extends Entity> Render<T> getEntityRenderObject(Entity entityIn);

    @Inject(method = "renderEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/Render;doRender(Lnet/minecraft/entity/Entity;DDDFF)V"))
    private void preDoRenderEntities(Entity entityIn, double x, double y, double z, float yaw, float partialTicks, boolean p_188391_10_, CallbackInfo ci) {
        //if(MinecraftForgeClient.getRenderPass() ==0 ) {
           // GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
            //GL11.glStencilMask(0xFF);
       // }

    }

    @Inject(method = "renderEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/Render;doRender(Lnet/minecraft/entity/Entity;DDDFF)V",
            shift = At.Shift.AFTER))
    private void postDoRenderEntities(Entity entityIn, double x, double y, double z, float yaw, float partialTicks, boolean p_188391_10_, CallbackInfo ci) {
        if(MinecraftForgeClient.getRenderPass() == 0) {
            if (entityIn instanceof BaseVehicleEntity) {
                GlQuaternionPool.openPool();
                BaseVehicleEntity<?> physicsEntity = (BaseVehicleEntity<?>) entityIn;
                ObjModelRenderer model = DynamXContext.getObjModelRegistry().getModel(physicsEntity.getPackInfo().getModel());

                GlStateManager.pushMatrix();
                GL11.glStencilFunc(GL11.GL_NOTEQUAL, 1, 0xFF);
                GL11.glStencilMask(0xFF);
                GlStateManager.translate((float) x, (float) y, (float) z);
                Quaternion q = ClientDynamXUtils.computeInterpolatedGlQuaternion(physicsEntity.prevRenderRotation, physicsEntity.renderRotation, partialTicks);
                GlStateManager.rotate(q);

                model.renderGroups("AntiWater1", (byte) 0);
                model.renderGroups("AntiWater2", (byte) 0);

                GlStateManager.popMatrix();
                GlQuaternionPool.closePool();

            }
            GL11.glStencilMask(0x00);
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        }
    }
}
