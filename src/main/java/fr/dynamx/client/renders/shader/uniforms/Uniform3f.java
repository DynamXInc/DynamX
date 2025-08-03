package fr.dynamx.client.renders.shader.uniforms;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;

public class Uniform3f extends UniformBase{

    private float v1, v2, v3;

    public Uniform3f(String name) {
        super(name);
    }

    public UniformBase set(float v1, float v2, float v3) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        return this;
    }

    public UniformBase set(Vector3f vec){
        this.v1 = vec.x;
        this.v2 = vec.y;
        this.v3 = vec.z;
        return this;
    }

    @Override
    void upload(int location) {
        GL20.glUniform3f(GL20.glGetUniformLocation(location, name), v1, v2, v3);
    }
}
