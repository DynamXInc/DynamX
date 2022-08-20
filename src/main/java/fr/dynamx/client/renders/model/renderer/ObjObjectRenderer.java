package fr.dynamx.client.renders.model.renderer;

import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.dynamx.client.renders.model.texture.MaterialTexture;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.objloader.Material;
import fr.dynamx.common.objloader.Vertex;
import fr.dynamx.common.objloader.data.ObjObjectData;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.dynamx.common.DynamXMain.log;

public class ObjObjectRenderer {
    // Use THIS instead of GlStateManager, it has weird issues due to last bind texture memory and display lists
    private static int bindTexture;

    private final Map<Byte, Integer> modelDisplayList = new HashMap<>();
    //VariantTexture / VaoID
    private final Map<Byte, Integer> vaos = new HashMap<>();
    private final List<Integer> vbos = new ArrayList<>();
    private boolean isVAOSetup;

    @Getter
    private final ObjObjectData objObjectData;

    public ObjObjectRenderer(ObjObjectData objObjectData) {
        this.objObjectData = objObjectData;
    }

    public void clearDisplayLists() {
        if (!modelDisplayList.isEmpty()) {
            modelDisplayList.forEach((textureID, displayList) -> {
                // If the list was created previously, we free the GPU memory
                GlStateManager.glDeleteLists(displayList, 1);
            });
            modelDisplayList.clear();
        }
    }

    private void compileModel(ObjModelRenderer model, @Nullable TextureVariantData useDefault, @Nullable TextureVariantData textureVariantData) {
        // Create an empty display list
        int id = GlStateManager.glGenLists(1);
        // Start the compilation of the list, this will fill the list with every vertex rendered onwards
        GlStateManager.glNewList(id, GL11.GL_COMPILE);
        //Do immediate render
        renderCPU(model, useDefault != null ? useDefault.getName() : "Default", textureVariantData != null ? textureVariantData.getName() : "Default");
        // Finish the compilation process
        GlStateManager.glEndList();
        modelDisplayList.put(textureVariantData != null ? textureVariantData.getId() : 0, id);

        vaos.put(textureVariantData != null ? textureVariantData.getId() : 0, -1);
    }

    private void setupVAO() {
        if (!isVAOSetup) {
            for (Map.Entry<Byte, Integer> entry : vaos.entrySet()) {
                Byte variantTextureData = entry.getKey();
                Integer tempVaoID = entry.getValue();
                if (tempVaoID == -1) {
                    int vaoID = DynamXRenderUtils.genVertexArrays();
                    DynamXRenderUtils.bindVertexArray(vaoID);
                    vaos.replace(variantTextureData, vaoID);

                    setupIndicesBuffer(getObjObjectData().getMesh().indices);
                    setupArraysPointers(EnumGLPointer.VERTEX, getObjObjectData().getMesh().getVerticesPos());
                    setupArraysPointers(EnumGLPointer.TEX_COORDS, getObjObjectData().getMesh().getTextureCoords());
                    setupArraysPointers(EnumGLPointer.NORMAL, getObjObjectData().getMesh().getVerticesNormals());

                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                    DynamXRenderUtils.bindVertexArray(0);
                }
            }
            isVAOSetup = true;
        }
    }

    public void createList(TextureVariantData useDefault, TextureVariantData textureVariantData, ObjModelRenderer model, boolean logIfNotFound) {
        if (!isMaterialValid(model, objObjectData.getMesh().materialForEachVertex[0]))
            return;
        if (useDefault == null || textureVariantData == null) {
            compileModel(model, null, null);
            return;
        }
        boolean hasVaryingTextures = textureVariantData.getId() == 0; //0 is the default, base texture
        if (!hasVaryingTextures) {
            for (Material material : objObjectData.getMesh().materialForEachVertex) {
                if (material.diffuseTexture.containsKey(textureVariantData.getName())) {
                    hasVaryingTextures = true;
                    break;
                }
            }
        }
        if (hasVaryingTextures) {
            compileModel(model, useDefault, textureVariantData);
        } else {
            if (logIfNotFound)
                log.error("Failed to find custom texture for skin " + textureVariantData.getName() + " of " + model.getLocation() + " in part " + objObjectData.getName());
            modelDisplayList.put(textureVariantData.getId(), modelDisplayList.get(useDefault.getId()));
        }
    }

    public void createDefaultList(ObjModelRenderer model) {
        createList(null, null, model, false);
    }

    public void render(ObjModelRenderer model, byte textureVariantID) {
        if (objObjectData.getMesh().materialForEachVertex.length == 0 || !isMaterialValid(model, objObjectData.getMesh().materialForEachVertex[0]))
            return;
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        if (OpenGlHelper.useVbo()) {
            setupVAO();
            if (model.getTextureVariants() == null) {
                return;
            }
            Map<Byte, TextureVariantData> textureVariant = model.getTextureVariants().getTextureVariantsFor(this);
            if (textureVariant == null) {
                return;
            }
            if (vaos.containsKey(textureVariantID)) {
                renderVAO(model, vaos.get(textureVariantID), textureVariant.get(textureVariantID).getName());
            }

        } else {
            if (!modelDisplayList.containsKey(textureVariantID)) {
                GlStateManager.color(1, 0, 0);
                GlStateManager.callList(modelDisplayList.get((byte) 0));
                GlStateManager.color(1, 1, 1);
            } else {
                GlStateManager.callList(modelDisplayList.get(textureVariantID));
            }
            GlStateManager.disableBlend();
            GlStateManager.bindTexture(Minecraft.getMinecraft().getTextureMapBlocks().getGlTextureId());
        }
    }

    private void renderVAO(ObjModelRenderer model, int vaoID, String variantTextureName) {
        DynamXRenderUtils.bindVertexArray(vaoID);
        GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GlStateManager.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        List<Material> materialsList = getObjObjectData().getMesh().materialsList;
        for (int i = 0, materialsListSize = materialsList.size(); i < materialsListSize; i++) {
            Material material = materialsList.get(i);
            drawTexture(model, material, "Default", variantTextureName, true);
            Material.IndexPair indexPair = material.indexPairPerObject.get(getObjObjectData().getName());
            GL11.glDrawElements(GL11.GL_TRIANGLES, (indexPair.getFinalIndex() - indexPair.getStartIndex()), GL11.GL_UNSIGNED_INT,
                    4L * indexPair.getStartIndex());
        }
        GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GlStateManager.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        DynamXRenderUtils.bindVertexArray(0);
    }


    /**
     * Model is assumed to be not empty
     */
    private void renderCPU(ObjModelRenderer model, String useDefault, String textureName) {
        //Reset bind texture
        startDrawing();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder renderer = tess.getBuffer();
        Material bind = objObjectData.getMesh().materialForEachVertex[0];
        drawTexture(model, bind, "Default", textureName, false);

        int[] indices = objObjectData.getMesh().indices;
        Vertex[] vertices = objObjectData.getMesh().vertices;

        boolean begining = true, drawing = false;
        for (int i = 0; i < indices.length; i += 3) {
            int i0 = indices[i];
            int i1 = indices[i + 1];
            int i2 = indices[i + 2];
            Vertex v0 = vertices[i0];
            Vertex v1 = vertices[i1];
            Vertex v2 = vertices[i2];

            Material materialToBind = objObjectData.getMesh().materialForEachVertex[i / 3];
            if (isMaterialValid(model, materialToBind) && materialToBind != bind) {
                bind = materialToBind;
                if (drawing)
                    tess.draw();
                drawTexture(model, materialToBind, "Default", textureName, false);
                begining = true;
            }
            if (begining) {
                begining = false;
                renderer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_NORMAL);
                drawing = true;
            }
            renderer.pos(v0.getPos().x, v0.getPos().y, v0.getPos().z).tex(v0.getTexCoords().x, 1f - v0.getTexCoords().y).normal(v0.getNormal().x, v0.getNormal().y, v0.getNormal().z).endVertex();
            renderer.pos(v1.getPos().x, v1.getPos().y, v1.getPos().z).tex(v1.getTexCoords().x, 1f - v1.getTexCoords().y).normal(v1.getNormal().x, v1.getNormal().y, v1.getNormal().z).endVertex();
            renderer.pos(v2.getPos().x, v2.getPos().y, v2.getPos().z).tex(v2.getTexCoords().x, 1f - v2.getTexCoords().y).normal(v2.getNormal().x, v2.getNormal().y, v2.getNormal().z).endVertex();
        }
        if (drawing)
            tess.draw();
        bindTexture(Minecraft.getMinecraft().getTextureMapBlocks().getGlTextureId());
    }

    private void drawTexture(ObjModelRenderer model, Material material, String useDefault, String textureName, boolean areVbosEnabled) {
        String bindName = getExistingTexture(material, textureName, useDefault);
        MaterialTexture materialMultipleTextures = material.diffuseTexture.get(bindName);
        if (materialMultipleTextures != null) {
            if (areVbosEnabled) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(materialMultipleTextures.getPath());
            } else {
                bindTexture(materialMultipleTextures.getGlTextureId());
            }
        } else {
            log.error("Failed to load Default texture of " + objObjectData.getName() + " in " + model.getLocation() + " in material " + material.getName());
        }
    }

    private String getExistingTexture(Material material, String first, String second) {
        return material.diffuseTexture.containsKey(first) ? first : material.diffuseTexture.containsKey(second) ? second : "Default";
    }

    /**
     * Binds the texture, if not already bound
     */
    private static void bindTexture(int id) {
        if (id != bindTexture) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            bindTexture = id;
        }
    }

    /**
     * Resets memory of last bind texture
     */
    private static void startDrawing() {
        bindTexture = -1;
    }

    private void setupIndicesBuffer(int[] indices) {
        int vboId = GL15.glGenBuffers();
        vbos.add(vboId);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, DynamXUtils.createIntBuffer(indices), GL15.GL_STATIC_DRAW);
    }

    private void setupArraysPointers(EnumGLPointer glPointer, float[] data) {
        int vboId = GL15.glGenBuffers();
        vbos.add(vboId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, DynamXUtils.createFloatBuffer(data), GL15.GL_STATIC_DRAW);
        switch (glPointer) {
            case VERTEX:
                GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0);
                break;
            case TEX_COORDS:
                GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 0, 0);
                break;
            case NORMAL:
                GL11.glNormalPointer(GL11.GL_FLOAT, 0, 0L);
                break;
        }
    }

    private boolean isMaterialValid(ObjModelRenderer model, Material material) {
        if (material == null)
            return false;
        if (material.getName().equals("none")) //BlockBench uses "none" materials, this is a bug
        {
            if (!model.hasNoneMaterials) {
                log.error("Invalid object " + objObjectData.getName() + " in model " + model.getLocation() + " : uses 'none' material of BlockBench");
                DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.MODEL, model.getTextureVariants() != null ? model.getTextureVariants().getPackName() : "Non-pack model", model.getLocation().toString(),
                        "Invalid object " + objObjectData.getName() + " : uses 'none' material of BlockBench", ErrorTrackingService.TrackedErrorLevel.LOW);
            }
            model.hasNoneMaterials = true;
            return false;
        }
        return true;
    }

    enum EnumGLPointer {
        VERTEX, TEX_COORDS, NORMAL
    }
}
