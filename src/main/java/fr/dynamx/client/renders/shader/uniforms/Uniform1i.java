package fr.dynamx.client.renders.shader.uniforms;

import org.lwjgl.opengl.GL20;

public class Uniform1i extends UniformBase{

    private int v1;

    public Uniform1i(String name) {
        super(name);
    }

    public UniformBase set(int v1) {
        this.v1 = v1;
        return this;
    }
    public UniformBase set(boolean bol) {
        this.v1 = bol ? 1 : 0;
        return this;
    }

    @Override
    void upload(int location) {
        GL20.glUniform1i(GL20.glGetUniformLocation(location, name), v1);
    }
}
