package fr.dynamx.client.renders.shader.uniforms;

import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

public class UniformM4 extends UniformBase {

    private FloatBuffer matrix;

    public UniformM4(String name) {
        super(name);
    }

    public UniformBase set(FloatBuffer matrix) {
        this.matrix = matrix;
        return this;
    }

    @Override
    void upload(int location) {
        if(matrix != null)
            GL20.glUniformMatrix4(GL20.glGetUniformLocation(location, name), false, matrix);
    }
}
