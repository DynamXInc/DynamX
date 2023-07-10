package com.modularmods.mcgltf;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.io.Buffers;
import de.javagl.jgltf.model.io.GltfModelReader;
import fr.dynamx.common.DynamXMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.fml.client.SplashProgress;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Mod(modid = MCglTF.MODID, clientSideOnly = true, useMetadata = true)
public class MCglTF {

    public static final String MODID = "mcgltf-dynamx";
    public static final String RESOURCE_LOCATION = "resourceLocation";

    public static final Logger logger = LogManager.getLogger(MODID);

    private static MCglTF INSTANCE;

    private int glProgramSkinnig = -1;
    private final int defaultColorMap;
    private final int defaultNormalMap;

    private final Map<ResourceLocation, Supplier<ByteBuffer>> loadedBufferResources = new HashMap<ResourceLocation, Supplier<ByteBuffer>>();
    private final Map<ResourceLocation, Supplier<ByteBuffer>> loadedImageResources = new HashMap<ResourceLocation, Supplier<ByteBuffer>>();
    private final List<IGltfModelReceiver> gltfModelReceivers = new ArrayList<IGltfModelReceiver>();
    private final List<Runnable> gltfRenderData = new ArrayList<Runnable>();

    public MCglTF() {
        INSTANCE = this;

        GL11.glPushAttrib(GL11.GL_TEXTURE_BIT);

        defaultColorMap = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, defaultColorMap);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 2, 2, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, Buffers.create(new byte[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}));
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);

        defaultNormalMap = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, defaultNormalMap);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 2, 2, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, Buffers.create(new byte[]{-128, -128, -1, -1, -128, -128, -1, -1, -128, -128, -1, -1, -128, -128, -1, -1}));
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);

        GL11.glPopAttrib();
    }

    public int getGlProgramSkinnig() {
        return glProgramSkinnig;
    }

    public int getDefaultColorMap() {
        return defaultColorMap;
    }

    public int getDefaultNormalMap() {
        return defaultNormalMap;
    }

    public int getDefaultSpecularMap() {
        return 0;
    }

    public ByteBuffer getBufferResource(ResourceLocation location) {
        Supplier<ByteBuffer> supplier;
        synchronized (loadedBufferResources) {
            supplier = loadedBufferResources.get(location);
            if (supplier == null) {
                supplier = new Supplier<ByteBuffer>() {
                    ByteBuffer bufferData;

                    @Override
                    public synchronized ByteBuffer get() {
                        if (bufferData == null) {
                            try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location)) {
                                bufferData = Buffers.create(IOUtils.toByteArray(new BufferedInputStream(resource.getInputStream())));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        return bufferData;
                    }

                };
                loadedBufferResources.put(location, supplier);
            }
        }
        return supplier.get();
    }

    public ByteBuffer getImageResource(ResourceLocation location) {
        Supplier<ByteBuffer> supplier;
        synchronized (loadedImageResources) {
            supplier = loadedImageResources.get(location);
            if (supplier == null) {
                supplier = new Supplier<ByteBuffer>() {
                    ByteBuffer bufferData;

                    @Override
                    public synchronized ByteBuffer get() {
                        if (bufferData == null) {
                            try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location)) {
                                bufferData = Buffers.create(IOUtils.toByteArray(new BufferedInputStream(resource.getInputStream())));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        return bufferData;
                    }

                };
                loadedImageResources.put(location, supplier);
            }
        }
        return supplier.get();
    }

    public void addGltfModelReceiver(IGltfModelReceiver receiver) {
        gltfModelReceivers.add(receiver);
    }

    public boolean removeGltfModelReceiver(IGltfModelReceiver receiver) {
        return gltfModelReceivers.remove(receiver);
    }

    public boolean isShaderModActive() {
        return true;
    }

    public static MCglTF getInstance() {
        return INSTANCE;
    }

    private void createSkinningProgramGL43() {
        int glShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(glShader,
                "#version 430\r\n"
                        + "layout(location = 0) in vec4 joint;"
                        + "layout(location = 1) in vec4 weight;"
                        + "layout(location = 2) in vec3 position;"
                        + "layout(location = 3) in vec3 normal;"
                        + "layout(location = 4) in vec4 tangent;"
                        + "layout(std430, binding = 0) readonly buffer jointMatrixBuffer {mat4 jointMatrices[];};"
                        + "out vec3 outPosition;"
                        + "out vec3 outNormal;"
                        + "out vec4 outTangent;"
                        + "void main() {"
                        + "mat4 skinMatrix ="
                        + " weight.x * jointMatrices[int(joint.x)] +"
                        + " weight.y * jointMatrices[int(joint.y)] +"
                        + " weight.z * jointMatrices[int(joint.z)] +"
                        + " weight.w * jointMatrices[int(joint.w)];"
                        + "outPosition = (skinMatrix * vec4(position, 1.0)).xyz;"
                        + "mat3 upperLeft = mat3(skinMatrix);"
                        + "outNormal = upperLeft * normal;"
                        + "outTangent.xyz = upperLeft * tangent.xyz;"
                        + "outTangent.w = tangent.w;"
                        + "}");
        GL20.glCompileShader(glShader);

        glProgramSkinnig = GL20.glCreateProgram();
        GL20.glAttachShader(glProgramSkinnig, glShader);
        GL20.glDeleteShader(glShader);
        GL30.glTransformFeedbackVaryings(glProgramSkinnig, new CharSequence[]{"outPosition", "outNormal", "outTangent"}, GL30.GL_SEPARATE_ATTRIBS);
        GL20.glLinkProgram(glProgramSkinnig);
    }

    private void createSkinningProgramGL33() {
        int glShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(glShader,
                "#version 330\r\n"
                        + "layout(location = 0) in vec4 joint;"
                        + "layout(location = 1) in vec4 weight;"
                        + "layout(location = 2) in vec3 position;"
                        + "layout(location = 3) in vec3 normal;"
                        + "layout(location = 4) in vec4 tangent;"
                        + "uniform samplerBuffer jointMatrices;"
                        + "out vec3 outPosition;"
                        + "out vec3 outNormal;"
                        + "out vec4 outTangent;"
                        + "void main() {"
                        + "int jx = int(joint.x) * 4;"
                        + "int jy = int(joint.y) * 4;"
                        + "int jz = int(joint.z) * 4;"
                        + "int jw = int(joint.w) * 4;"
                        + "mat4 skinMatrix ="
                        + " weight.x * mat4(texelFetch(jointMatrices, jx), texelFetch(jointMatrices, jx + 1), texelFetch(jointMatrices, jx + 2), texelFetch(jointMatrices, jx + 3)) +"
                        + " weight.y * mat4(texelFetch(jointMatrices, jy), texelFetch(jointMatrices, jy + 1), texelFetch(jointMatrices, jy + 2), texelFetch(jointMatrices, jy + 3)) +"
                        + " weight.z * mat4(texelFetch(jointMatrices, jz), texelFetch(jointMatrices, jz + 1), texelFetch(jointMatrices, jz + 2), texelFetch(jointMatrices, jz + 3)) +"
                        + " weight.w * mat4(texelFetch(jointMatrices, jw), texelFetch(jointMatrices, jw + 1), texelFetch(jointMatrices, jw + 2), texelFetch(jointMatrices, jw + 3));"
                        + "outPosition = (skinMatrix * vec4(position, 1.0)).xyz;"
                        + "mat3 upperLeft = mat3(skinMatrix);"
                        + "outNormal = upperLeft * normal;"
                        + "outTangent.xyz = upperLeft * tangent.xyz;"
                        + "outTangent.w = tangent.w;"
                        + "}");
        GL20.glCompileShader(glShader);

        glProgramSkinnig = GL20.glCreateProgram();
        GL20.glAttachShader(glProgramSkinnig, glShader);
        GL20.glDeleteShader(glShader);
        GL30.glTransformFeedbackVaryings(glProgramSkinnig, new CharSequence[]{"outPosition", "outNormal", "outTangent"}, GL30.GL_SEPARATE_ATTRIBS);
        GL20.glLinkProgram(glProgramSkinnig);
    }

    public void createShaderSkinningProgram() {
        ContextCapabilities capabilities = GLContext.getCapabilities();
        if (capabilities.OpenGL43) {
            createSkinningProgramGL43();
        } else if (capabilities.OpenGL40) {
            createSkinningProgramGL33();
        } else if (capabilities.OpenGL33) {
            createSkinningProgramGL33();
        }
    }

    public void reloadModels() {
        ContextCapabilities capabilities = GLContext.getCapabilities();
        gltfRenderData.forEach(Runnable::run);
        gltfRenderData.clear();
        Map<ResourceLocation, MutablePair<GltfModel, List<IGltfModelReceiver>>> lookup = new HashMap<>();
        gltfModelReceivers.forEach((receiver) -> {
            ResourceLocation modelLocation = receiver.getModelLocation();
            MutablePair<GltfModel, List<IGltfModelReceiver>> receivers = lookup.get(modelLocation);
            if (receivers == null) {
                receivers = MutablePair.of(null, new ArrayList<>());
                lookup.put(modelLocation, receivers);
            }
            receivers.getRight().add(receiver);
        });
        lookup.entrySet().parallelStream().forEach((entry) -> {
            try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(entry.getKey())) {
                long start = System.currentTimeMillis();
                DynamXMain.log.info("Loading gltf model " + entry.getKey());
                entry.getValue().setLeft(new GltfModelReader().readWithoutReferences(new BufferedInputStream(resource.getInputStream()), entry.getKey()));
                DynamXMain.log.info("Loaded gltf model " + entry.getKey() +" in " + (System.currentTimeMillis() - start) + "ms");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        lookup.forEach((modelLocation, receivers) -> {
            Iterator<IGltfModelReceiver> iterator = receivers.getRight().iterator();
            do {
                IGltfModelReceiver receiver = iterator.next();
                if (receiver.isReceiveSharedModel(receivers.getLeft(), gltfRenderData)) {
                    RenderedGltfModel renderedModel = new RenderedGltfModel(gltfRenderData, receivers.getLeft());
                    if (capabilities.OpenGL43) {
                        renderedModel = new RenderedGltfModel(gltfRenderData, receivers.getLeft());
                    } else if (capabilities.OpenGL40) {
                        renderedModel = new RenderedGltfModelGL40(gltfRenderData, receivers.getLeft());
                    }  else if (capabilities.OpenGL33) {
                        renderedModel = new RenderedGltfModelGL33(gltfRenderData, receivers.getLeft());
                    } else if (capabilities.OpenGL30) {
                        renderedModel = new RenderedGltfModelGL30(gltfRenderData, receivers.getLeft());
                    } else if (capabilities.OpenGL20) {
                        renderedModel = new RenderedGltfModelGL20(gltfRenderData, receivers.getLeft());
                    }
                    receiver.onReceiveSharedModel(renderedModel);
                    while (iterator.hasNext()) {
                        receiver = iterator.next();
                        if (receiver.isReceiveSharedModel(receivers.getLeft(), gltfRenderData)) {
                            receiver.onReceiveSharedModel(renderedModel);
                        }
                    }
                    return;
                }
            }
            while (iterator.hasNext());
        });

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        if (capabilities.OpenGL43) {
            GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            GL30.glBindVertexArray(0);
            GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, 0);
            loadedBufferResources.clear();
            loadedImageResources.clear();
        } else if (capabilities.OpenGL40) {
            GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0);
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
            GL30.glBindVertexArray(0);
            GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, 0);
            loadedBufferResources.clear();
            loadedImageResources.clear();
        } else if (capabilities.OpenGL33) {
            GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0);
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
            GL30.glBindVertexArray(0);
            loadedBufferResources.clear();
            loadedImageResources.clear();
        } else if (capabilities.OpenGL30) {
            GL30.glBindVertexArray(0);
            loadedBufferResources.clear();
            loadedImageResources.clear();
        } else if (capabilities.OpenGL20) {
            loadedBufferResources.clear();
            loadedImageResources.clear();
        }
    }

    public enum EnumRenderedModelGLProfile {
        AUTO,
        GL43,
        GL40,
        GL33,
        GL30,
        GL20
    }

    public EnumRenderedModelGLProfile getRenderedModelGLProfile() {
        return CompatibilityConfig.RenderedModelGLProfile;
    }

    @Config(modid = MCglTF.MODID, category = "compatibility")
    static class CompatibilityConfig {
        @Config.RequiresMcRestart
        @Config.Comment({"Set maximum version of OpenGL to enable some optimizations for rendering glTF model.",
                "The AUTO means it will select maximum OpenGL version available based on your hardware. The GL43 is highest it may select.",
                "The lower OpenGL version you set, the more negative impact on performance you will probably get.",
                "The GL30 is a special profile which essentially the GL33 and above but replace hardware(GPU) skinning with software(CPU) skinning. This will trade a lots of CPU performance for a few GPU performance increase."})
        public static EnumRenderedModelGLProfile RenderedModelGLProfile = EnumRenderedModelGLProfile.AUTO;
    }

}