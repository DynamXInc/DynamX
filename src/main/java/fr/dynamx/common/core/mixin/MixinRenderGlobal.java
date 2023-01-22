package fr.dynamx.common.core.mixin;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = RenderGlobal.class, remap = MixinChunk.REMAP)
public abstract class MixinRenderGlobal {

    @Inject(method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;)V"))
    private void preRenderTranslucent(BlockRenderLayer blockLayerIn, double partialTicks, int pass, Entity entityIn, CallbackInfoReturnable<Integer> cir) {
        if (blockLayerIn.equals(BlockRenderLayer.TRANSLUCENT)) {
            GL11.glStencilFunc(GL11.GL_NOTEQUAL, 1, 0xFF);
            //GL11.glStencilMask(0x00);
        }

    }

    @Inject(method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;)V",
                    shift = At.Shift.AFTER))
    private void postRenderTranslucent(BlockRenderLayer blockLayerIn, double partialTicks, int pass, Entity entityIn, CallbackInfoReturnable<Integer> cir) {
        if (blockLayerIn.equals(BlockRenderLayer.TRANSLUCENT)) {
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
            GL11.glStencilMask(0x00);
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
        }

    }
}
