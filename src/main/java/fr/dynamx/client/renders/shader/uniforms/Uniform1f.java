package fr.dynamx.client.renders.shader.uniforms;

import org.lwjgl.opengl.GL20;

public class Uniform1f extends UniformBase{

    private float v1;

    public Uniform1f(String name) {
        super(name);
    }

    public UniformBase set(float v1) {
        this.v1 = v1;
        return this;
    }

    @Override
    void upload(int location) {
        GL20.glUniform1f(GL20.glGetUniformLocation(location, name), v1);
    }
}
