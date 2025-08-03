package fr.dynamx.common.core.mixin;

import fr.dynamx.client.renders.OptifineShaderUniformsHandler;
import fr.dynamx.client.renders.VanillaShaderUniformsHandler;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.client.DynamXRenderUtils;
import net.minecraft.client.Minecraft;
import net.optifine.shaders.Program;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = Shaders.class, remap = DynamXConstants.REMAP)
public class MixinOptifineShaders {

    @Inject(method = "setupProgram", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/ARBShaderObjects;glLinkProgramARB(I)V", shift = At.Shift.BEFORE),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private static void setupProgram(Program program, String vShaderPath, String gShaderPath, String fShaderPath, CallbackInfo ci, int programId) {

        GL20.glBindAttribLocation(programId, DynamXContext.centerAttribLocation, "centers");
        GL20.glBindAttribLocation(programId, DynamXContext.colorAttribLocation, "colors");

        DynamXRenderUtils.checkForOglError();

    }

    @Inject(method = "useProgram", at = @At(value = "TAIL"))
    private static void useProgram(Program program, CallbackInfo ci) {
        if (program.getId() == 0) {
            return;
        }
        String name = program.getName();
        if (!DynamXContext.optifineShadersOn) {
            return;
        }
        if (DynamXContext.shaderUniformsHandler instanceof VanillaShaderUniformsHandler) {
            return;
        }

        ((OptifineShaderUniformsHandler) DynamXContext.shaderUniformsHandler).shaderProgramId = -1;
        if (name.equalsIgnoreCase("gbuffers_terrain") || name.equalsIgnoreCase("gbuffers_entities")
                || name.equalsIgnoreCase("gbuffers_entities_glowing")
                || name.equalsIgnoreCase("gbuffers_block") || name.equalsIgnoreCase("gbuffers_weather")
                || name.equalsIgnoreCase("gbuffers_water")
                || name.equalsIgnoreCase("gbuffers_textured")
                || name.equalsIgnoreCase("gbuffers_hand")
                || name.equalsIgnoreCase("composite") || name.equalsIgnoreCase("composite2")
                || name.equalsIgnoreCase("composite5") || name.equalsIgnoreCase("shadow") || name.equalsIgnoreCase("deferred12")) {

            ((OptifineShaderUniformsHandler) DynamXContext.shaderUniformsHandler).shaderProgramId = program.getId();

            DynamXContext.getShaderUniformsHandler().uploadCommonUniforms(Minecraft.getMinecraft().player, program.getId());



        }
        Shaders.checkGLError("post uniform shaders");
    }
}
