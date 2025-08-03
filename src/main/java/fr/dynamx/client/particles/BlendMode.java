package fr.dynamx.client.particles;

import org.lwjgl.opengl.GL11;

public enum BlendMode {
    ALPHA(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA),
    ADDITIVE(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

    private final int sFactor;
    private final int dFactor;

    BlendMode(int sFactor, int dFactor) {
        this.sFactor = sFactor;
        this.dFactor = dFactor;
    }

    public void apply() {
        GL11.glBlendFunc(sFactor, dFactor);
    }
}
