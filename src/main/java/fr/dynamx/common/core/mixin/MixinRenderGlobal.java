package fr.dynamx.common.core.mixin;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Final step for our stencil test
 * In this test we make all the bits inside the stencil buffer fail the test.
 * Then, we disable the test
 */
@Mixin(value = RenderGlobal.class)
public abstract class MixinRenderGlobal {

    @Inject(method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;)V"))
    private void preRenderTranslucent(BlockRenderLayer blockLayerIn, double partialTicks, int pass, Entity entityIn, CallbackInfoReturnable<Integer> cir) {
        if (blockLayerIn.equals(BlockRenderLayer.TRANSLUCENT)) {
            //We want only the stencil bits = 0xFF to fail the test
            GL11.glStencilFunc(GL11.GL_NOTEQUAL, 1, 0xFF);
        }
    }

    @Inject(method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;)V",
                    shift = At.Shift.AFTER))
    private void postRenderTranslucent(BlockRenderLayer blockLayerIn, double partialTicks, int pass, Entity entityIn, CallbackInfoReturnable<Integer> cir) {
        if (blockLayerIn.equals(BlockRenderLayer.TRANSLUCENT)) {
            //Everything after water rendering pass the test again
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
            //Enables writing to the stencil buffer in order to clear it on the next frame
            GL11.glStencilMask(0xFF);
            //Disables the stencil buffer for this frame
            GL11.glDisable(GL11.GL_STENCIL_TEST);
        }
    }
}
