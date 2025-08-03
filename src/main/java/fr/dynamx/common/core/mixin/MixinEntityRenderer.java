package fr.dynamx.common.core.mixin;

import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.client.MinecraftForgeClient;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First step for our stencil test
 * In this step we execute the basic things to use our stencil buffer. Then, we make everything pass the stencil test
 *
 * @see MixinRenderManager for the second step
 */
@Mixin(value = EntityRenderer.class, remap = DynamXConstants.REMAP)
public abstract class MixinEntityRenderer {
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
        ClientEventHandler.resetBigEntities();
    }

     @Inject(method = "renderWorldPass", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleManager;renderParticles(Lnet/minecraft/entity/Entity;F)V", shift = At.Shift.AFTER)
    )
    private void renderParticles(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        if (DynamXContext.getParticleManager() == null) {
            return;
        }
        int currentProgram = GlStateManager.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        Shaders.useProgram(Shaders.ProgramTextured);


        DynamXContext.getParticleManager().update(0.01667f);
        DynamXContext.getParticleManager().render();



        OpenGlHelper.glUseProgram(currentProgram);

    }
}
