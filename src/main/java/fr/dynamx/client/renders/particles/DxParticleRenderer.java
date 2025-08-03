package fr.dynamx.client.renders.particles;

import fr.dynamx.client.particles.BlendMode;
import fr.dynamx.client.particles.DxParticleSystem;
import lombok.Getter;
import net.minecraft.client.renderer.GlStateManager;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class DxParticleRenderer {

    @Getter
    private final DxParticleMesh particleMesh;

    public DxParticleRenderer(String textureName) {
        this.particleMesh = new DxParticleMesh(textureName);
    }

    public void render(DxParticleSystem particleSystem) {
        if (particleSystem.getParticles().isEmpty()) {
            return;
        }

        prepareRenderState(particleSystem.getBlendMode());

        particleMesh.setInstanceCount(particleSystem.getParticles().size());

        particleMesh.render();
        particleMesh.getCenters().updateDataStore();
        particleMesh.getColors().updateDataStore();

        restoreRenderState();
    }

    private void prepareRenderState(BlendMode blendMode) {
        GlStateManager.bindTexture(particleMesh.getTexture().getGlTextureId());
        GL20.glUniform1i(GL20.glGetUniformLocation(Shaders.ProgramTextured.getId(), "dxParticles"), 1);
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        blendMode.apply();
        GL11.glDepthMask(false);
    }

    private void restoreRenderState() {
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL20.glUniform1i(GL20.glGetUniformLocation(Shaders.ProgramTextured.getId(), "dxParticles"), 0);
    }

    public void clean() {
        particleMesh.cleanUp();
    }
}
