package fr.dynamx.client.renders;

import fr.dynamx.client.renders.shader.uniforms.*;
import fr.dynamx.common.DynamXContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.optifine.shaders.Shaders;

import java.nio.FloatBuffer;

public abstract class IShaderUniformsHandler {
    public final UniformBase uniformDiffuseMap = new Uniform1i("diffuseMap").set(0);
    public final UniformBase uniformLightMap = new Uniform1i("lightMap").set(1);

    public final Uniform3f uniformCameraPosition = new Uniform3f("playerPosition");
    public final Uniform3f uniformChunkPos = new Uniform3f("chunkPos");
    public final Uniform1i lightCount = new Uniform1i("lightCount");


    public final UniformM4 uniformViewMatrix = new UniformM4("viewMatrix");
    public final UniformM4 uniformViewMatrixInverse = new UniformM4("viewMatrixInverse");


    protected abstract int getShaderProgram();

    public void tick() {
        if (DynamXContext.optifineShadersOn != Shaders.shaderPackLoaded) {
            DynamXContext.optifineShadersOn = Shaders.shaderPackLoaded;

            if (!DynamXContext.optifineShadersOn) {
                DynamXContext.shaderUniformsHandler = new VanillaShaderUniformsHandler();
            } else {
                DynamXContext.shaderUniformsHandler = new OptifineShaderUniformsHandler();
            }
        }
    }

    public void uploadCommonUniforms(EntityPlayer player, int shaderProgram) {
        if (player == null) return;
        //   uniformDiffuseMap.sendValueTo(getShaderProgram());

        float x = (float) (player.lastTickPosX + (player.posX - player.lastTickPosX) * Minecraft.getMinecraft().getRenderPartialTicks());
        float y = (float) (player.lastTickPosY + (player.posY - player.lastTickPosY) * Minecraft.getMinecraft().getRenderPartialTicks());
        float z = (float) (player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * Minecraft.getMinecraft().getRenderPartialTicks());

        uniformCameraPosition.set(x, y, z).sendValueTo(shaderProgram);

        uniformViewMatrixInverse.set((FloatBuffer) GlobalMatrices.invViewMatrixBuffer.position(0)).sendValueTo(shaderProgram);
        uniformViewMatrix.set((FloatBuffer) GlobalMatrices.viewMatrixBuffer.position(0)).sendValueTo(shaderProgram);
    }

    public void uploadEntityUniforms(Entity entity) {

    }


    public void uploadTileEntityUniforms(TileEntity entity) {

    }

    public void uploadTerrainUniforms(BlockPos pos) {
    }

}
