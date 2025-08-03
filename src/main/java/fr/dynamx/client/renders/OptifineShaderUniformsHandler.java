package fr.dynamx.client.renders;

import fr.dynamx.client.renders.shader.uniforms.Uniform1i;
import fr.dynamx.client.renders.shader.uniforms.UniformM4;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.joml.Vector4f;

public class OptifineShaderUniformsHandler extends IShaderUniformsHandler {

    public int shaderProgramId = -1;


    public final Uniform1i uniformNoiseTexture = new Uniform1i("noiseTexture");


    public OptifineShaderUniformsHandler() {


    }

    @Override
    protected int getShaderProgram() {
        return shaderProgramId;
    }




}
