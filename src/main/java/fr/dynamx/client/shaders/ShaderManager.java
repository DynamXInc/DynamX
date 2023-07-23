package fr.dynamx.client.shaders;

import fr.dynamx.common.DynamXMain;
import fr.dynamx.utils.DynamXConstants;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.client.resource.VanillaResourceType;
import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.nio.ByteBuffer;
import java.util.function.Predicate;

/**
 * Based on BetterLight
 */
public class ShaderManager implements ISelectiveResourceReloadListener {

    public static DxShader sceneGrid;

    public static void init(IResourceManager manager) {
        sceneGrid = new DxShader(new ResourceLocation(DynamXConstants.ID, "scenegrid"), manager);
    }

    public static int loadProgram(String vsh, String fsh, IResourceManager manager) {
        int vertexShader = createShader(vsh, OpenGlHelper.GL_VERTEX_SHADER, manager);
        int fragmentShader = createShader(fsh, OpenGlHelper.GL_FRAGMENT_SHADER, manager);
        int program = OpenGlHelper.glCreateProgram();
        OpenGlHelper.glAttachShader(program, vertexShader);
        OpenGlHelper.glAttachShader(program, fragmentShader);
        OpenGlHelper.glLinkProgram(program);
        String s = GL20.glGetProgramInfoLog(program, 32768);
        if (!s.isEmpty())
            System.out.println("GL LOG: " + s);
        else
            System.out.println("Successfully loaded the shader");
        return program;
    }

    public static int createShader(String filename, int shaderType, IResourceManager manager) {
        int shader = OpenGlHelper.glCreateShader(shaderType);
        if (shader == 0)
            return 0;
        try {
            byte[] byteArray = IOUtils.toByteArray(new BufferedInputStream(manager.getResource(new ResourceLocation(filename)).getInputStream()));
            ByteBuffer buffer = BufferUtils.createByteBuffer(byteArray.length);
            buffer.put(byteArray);
            buffer.position(0);
            OpenGlHelper.glShaderSource(shader, buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        OpenGlHelper.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, OpenGlHelper.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            DynamXMain.log.error("Error creating shader: " + getLogInfo(shader));
            return 0;
        }

        return shader;
    }

    public static String getLogInfo(int obj) {
        return ARBShaderObjects.glGetInfoLogARB(obj, ARBShaderObjects.glGetObjectParameteriARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB));
    }

    @Override
    public void onResourceManagerReload(@Nullable IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
        if (resourcePredicate.test(VanillaResourceType.MODELS)) {
            init(resourceManager);
        }
    }
}
