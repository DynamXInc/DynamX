package fr.dynamx.common.core.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First step for our stencil test
 * In this step we execute the basic things to use our stencil buffer. Then, we make everything pass the stencil test
 * @see MixinRenderManager for the second step
 */
@Mixin(value = EntityRenderer.class, remap = MixinChunk.REMAP)
public abstract class MixinWater {

    @Inject(method = "renderWorldPass(IFJ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V"))
    private void preRenderEntities(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        //Before minecraft's opaque pass
        if (MinecraftForgeClient.getRenderPass() == 0) {
            //Clears the buffer
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
            //Enables the stencil test
            GL11.glEnable(GL11.GL_STENCIL_TEST);
            //We always want all the entities to pass the test
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
            //Disables writing to the buffer
            GL11.glStencilMask(0x00);
        }
    }
}
