package fr.dynamx.common.core.mixin;

import fr.dynamx.client.renders.GlobalMatrices;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.renderer.EntityRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

@Mixin(value = EntityRenderer.class, remap = DynamXConstants.REMAP)
public abstract class MixinBetterLightEntityRenderer {

    private static final float[] tmpModelViewInverse = new float[16];
    private static final float[] tmpModelView = new float[16];

    private static final float[] tmpProjectionInverse = new float[16];
    private static final float[] tmpProjection = new float[16];

    @Inject(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;orientCamera(F)V",
            shift = At.Shift.AFTER))
    public void postModelView(float partialTicks, int pass, CallbackInfo ci) {
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, (FloatBuffer) GlobalMatrices.viewMatrixBuffer.position(0));
        GlobalMatrices.viewMatrix.set((FloatBuffer) GlobalMatrices.viewMatrixBuffer.position(0));
        GlobalMatrices.viewMatrix.invert(GlobalMatrices.invViewMatrix);
        GlobalMatrices.invViewMatrix.get((FloatBuffer) GlobalMatrices.invViewMatrixBuffer.position(0));

        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, (FloatBuffer) GlobalMatrices.projectionMatrixBuffer.position(0));
        GlobalMatrices.projectionMatrix.set((FloatBuffer) GlobalMatrices.projectionMatrixBuffer.position(0));
        GlobalMatrices.projectionMatrix.invert(GlobalMatrices.invProjectionMatrix);
        GlobalMatrices.invProjectionMatrix.get((FloatBuffer) GlobalMatrices.invProjectionMatrixBuffer.position(0));
    }



}
