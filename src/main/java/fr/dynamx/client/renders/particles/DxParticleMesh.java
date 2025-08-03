package fr.dynamx.client.renders.particles;

import fr.dynamx.client.particles.DxParticleManager;
import fr.dynamx.client.renders.mesh.DxIndexBuffer;
import fr.dynamx.client.renders.mesh.GLMesh;
import fr.dynamx.client.renders.model.texture.MaterialTexture;
import fr.dynamx.utils.DynamXConstants;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class DxParticleMesh extends GLMesh {

    private static final float[] vertexBufferData = {
            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f,
            -0.5f, 0.5f, 0.0f,
            0.5f, 0.5f, 0.0f,
    };

    private static final int[] indexBufferData = {
            0, 1, 2,
            2, 1, 3
    };

    @Getter
    private MaterialTexture texture;

    public DxParticleMesh(String textureName) {
        super(GL11.GL_TRIANGLE_STRIP, 4);

        super.setPositions(vertexBufferData);
        DxIndexBuffer indices = super.createIndices(indexBufferData.length);
        for (int indexBufferDatum : indexBufferData) {
            indices.put(indexBufferDatum);
        }
        indices.position(0);

        super.setCenters(new float[DxParticleManager.MAX_PARTICLES * 4]);
        getCenters().setStream();
        super.setColors(new float[DxParticleManager.MAX_PARTICLES * 4]);
        getColors().setStream();

        setInstancing(true);

        texture = new MaterialTexture(new ResourceLocation(DynamXConstants.ID, "textures/particles/" + textureName), "default");
        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();

        texture.loadTexture(textureManager);
        texture.uploadTexture(textureManager);
    }
}