package fr.dynamx.client.renders;

import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEndGateway;
import net.minecraft.tileentity.TileEntityEndPortal;
import net.minecraft.util.math.BlockPos;
import net.optifine.shaders.Shaders;

public class VanillaShaderUniformsHandler extends IShaderUniformsHandler {

    public int shaderProgramId = -1;


    @Override
    protected int getShaderProgram() {
        return shaderProgramId;
    }

    @Override
    public void uploadCommonUniforms(EntityPlayer player, int shaderProgramId) {
        super.uploadCommonUniforms(player, shaderProgramId);
        uniformDiffuseMap.sendValueTo(shaderProgramId);
        uniformLightMap.sendValueTo(shaderProgramId);
        Shaders.uniform_frameCounter.setProgram(shaderProgramId);
    }

    @Override
    public void uploadEntityUniforms(Entity entity) {
        super.uploadEntityUniforms(entity);

    }

    @Override
    public void uploadTileEntityUniforms(TileEntity entity) {
        super.uploadTileEntityUniforms(entity);

    }

    @Override
    public void uploadTerrainUniforms(BlockPos pos) {
        super.uploadTerrainUniforms(pos);

    }


}