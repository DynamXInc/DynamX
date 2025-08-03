package fr.dynamx.client.renders.shader.uniforms;

import org.lwjgl.opengl.GL20;

public class Uniform2f extends UniformBase{

    private float v1, v2;

    public Uniform2f(String name) {
        super(name);
    }

    public UniformBase set(float v1, float v2) {
        this.v1 = v1;
        this.v2 = v2;
        return this;
    }

    @Override
    void upload(int location) {
        GL20.glUniform2f(GL20.glGetUniformLocation(location, name), v1, v2);
    }
}
