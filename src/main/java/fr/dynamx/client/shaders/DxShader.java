package fr.dynamx.client.shaders;

import com.jme3.math.Vector3f;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

/**
 * Based on BetterLight
 */
public class DxShader {

    private static DxShader currentShader = null;
    private static int currentProgram = -1;
    private final int program;

    public DxShader(ResourceLocation shader, IResourceManager resourceManager) {
        program = ShaderManager.loadProgram(
                String.format("%s:shaders/%s.vsh", shader.getNamespace(), shader.getPath()),
                String.format("%s:shaders/%s.fsh", shader.getNamespace(), shader.getPath()),
                resourceManager
        );
    }

    public static DxShader getCurrentShader() {
        return currentShader;
    }

    public static void stopShader() {
        if (currentProgram != 0) {
            GL20.glUseProgram(0);
            currentProgram = 0;
            currentShader = null;
        }
    }

    public static int getCurrentProgram() {
        return currentProgram;
    }

    public int getProgram() {
        return program;
    }

    public static boolean isCurrentShader(DxShader shader) {
        return shader != null && currentProgram == shader.program;
    }

    public void useShader() {
        if (!isCurrentShader(this)) {
            OpenGlHelper.glUseProgram(program);
            currentProgram = program;
            currentShader = this;
        }
    }

    public void setUniform(String uniform, int value) {
        if (isCurrentShader(this)) {
            GL20.glUniform1i(GL20.glGetUniformLocation(currentProgram, uniform), value);
        }
    }

    public void setUniform(String uniform, boolean value) {
        if (isCurrentShader(this)) {
            GL20.glUniform1i(GL20.glGetUniformLocation(currentProgram, uniform), value ? 1 : 0);
        }
    }

    public void setUniform(String uniform, float value) {
        if (isCurrentShader(this)) {
            GL20.glUniform1f(GL20.glGetUniformLocation(currentProgram, uniform), value);
        }
    }

    public void setUniform(String uniform, int v1, int v2) {
        if (isCurrentShader(this)) {
            GL20.glUniform2i(GL20.glGetUniformLocation(currentProgram, uniform), v1, v2);
        }
    }

    public void setUniform(String uniform, int v1, int v2, int v3) {
        if (isCurrentShader(this)) {
            GL20.glUniform3i(GL20.glGetUniformLocation(currentProgram, uniform), v1, v2, v3);
        }
    }

    public void setUniform(String uniform, double v1, double v2, double v3) {
        setUniform(uniform, (float) v1, (float) v2, (float) v3);
    }

    public void setUniform(String uniform, float v1, float v2) {
        if (isCurrentShader(this)) {
            GL20.glUniform2f(GL20.glGetUniformLocation(currentProgram, uniform), v1, v2);
        }
    }

    public void setUniform(String uniform, float v1, float v2, float v3) {
        if (isCurrentShader(this)) {
            GL20.glUniform3f(GL20.glGetUniformLocation(currentProgram, uniform), v1, v2, v3);
        }
    }

    public void setUniform(String uniform, float v1, float v2, float v3, float v4) {
        if (isCurrentShader(this)) {
            GL20.glUniform4f(GL20.glGetUniformLocation(currentProgram, uniform), v1, v2, v3, v4);
        }
    }

    public void setUniform(String uniform, FloatBuffer floatBuffer) {
        if (isCurrentShader(this)) {
            GL20.glUniformMatrix4(GL20.glGetUniformLocation(currentProgram, uniform), false, floatBuffer);
        }
    }

    public void setUniform(String uniform, Vector3f vec) {
        setUniform(uniform, vec.x, vec.y, vec.z);
    }

    public void setUniform(String uniform, Vec3d vec) {
        setUniform(uniform, vec.x, vec.y, vec.z);
    }

    public void setUniform(String uniform, BlockPos vec) {
        setUniform(uniform, vec.getX(), vec.getY(), vec.getZ());
    }

}