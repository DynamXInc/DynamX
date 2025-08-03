package fr.dynamx.client.renders.shader.uniforms;

import org.lwjgl.opengl.GL20;

public class Uniform4f extends UniformBase{

    private float v1, v2, v3, v4;

    public Uniform4f(String name) {
        super(name);
    }

    public UniformBase set(float v1, float v2, float v3, float v4) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.v4 = v4;
        return this;
    }

    @Override
    void upload(int location) {
        GL20.glUniform4f(GL20.glGetUniformLocation(location, name), v1, v2, v3, v4);
    }
}
