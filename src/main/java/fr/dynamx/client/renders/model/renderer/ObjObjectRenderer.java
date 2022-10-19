package fr.dynamx.client.renders.model.renderer;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.client.handlers.ClientEventHandler;
import fr.dynamx.client.renders.model.texture.MaterialTexture;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import fr.dynamx.common.objloader.data.Material;
import fr.dynamx.common.objloader.data.ObjObjectData;
import fr.dynamx.common.objloader.data.Vertex;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import javax.annotation.Nullable;
import javax.vecmath.Vector4f;
import java.util.*;

import static fr.dynamx.common.DynamXMain.log;

public class ObjObjectRenderer {
    // Use THIS instead of GlStateManager, it has weird issues due to last bind texture memory and display lists
    private static int bindTexture;

    private final Map<Byte, VariantRenderData> modelDisplayList = new HashMap<>();
    @Getter
    private final ObjObjectData objObjectData;
    private boolean isVAOSetup;
    @Getter
    @Setter
    private Vector4f objectColor = new Vector4f(1, 1, 1, 1);

    public ObjObjectRenderer(ObjObjectData objObjectData) {
        this.objObjectData = objObjectData;
    }

    public void clearDisplayLists() {
        if (!modelDisplayList.isEmpty()) {
            modelDisplayList.forEach((textureID, displayList) -> {
                // If the list was created previously, we free the GPU memory
                GlStateManager.glDeleteLists(displayList.displayListId, 1);
            });
            modelDisplayList.clear();
        }
    }

    private void compileModel(ObjModelRenderer model, @Nullable TextureVariantData baseVariant, @Nullable TextureVariantData textureVariantData) {
        // Create an empty display list
        int id = GlStateManager.glGenLists(1);
        // Start the compilation of the list, this will fill the list with every vertex rendered onwards
        GlStateManager.glNewList(id, GL11.GL_COMPILE);
        //Do immediate render
        renderCPU(model, baseVariant != null ? baseVariant.getName() : null, textureVariantData != null ? textureVariantData.getName() : null);
        // Finish the compilation process
        GlStateManager.glEndList();
        modelDisplayList.put(textureVariantData != null ? textureVariantData.getId() : 0, new VariantRenderData(baseVariant, textureVariantData, id, -1));
    }

    private void setupVAO() {
        if (!isVAOSetup) {
            for (Map.Entry<Byte, VariantRenderData> entry : modelDisplayList.entrySet()) {
                if (entry.getValue().vaoId == -1) {
                    int vaoID = DynamXRenderUtils.genVertexArrays();
                    DynamXRenderUtils.bindVertexArray(vaoID);
                    entry.getValue().vaoId = vaoID;

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

    public void createList(ObjModelRenderer model, @Nullable TextureVariantData baseVariant, @Nullable TextureVariantData variant, boolean logIfNotFound) {
        if (!isMaterialValid(model, model.getMaterials().get(objObjectData.getMesh().materialForEachVertex[0])))
            return;
        if (baseVariant == null || variant == null || baseVariant == variant) { //Default model
            compileModel(model, null, null);
            return;
        }
        boolean hasVaryingTextures = model.getMaterials().containsKey(variant.getName());
        if (!hasVaryingTextures) {
            for (String materialName : objObjectData.getMesh().materialForEachVertex) {
                Material material = model.getMaterials().get(materialName);
                if (material != null && material.diffuseTexture.containsKey(variant.getName())) {
                    hasVaryingTextures = true;
                    break;
                }
            }
        }
        if (hasVaryingTextures)
            compileModel(model, baseVariant, variant);
        else {
            if (logIfNotFound)
                log.error("Failed to find custom texture for skin " + variant + " of " + model.getLocation() + " in part " + objObjectData.getName());
            modelDisplayList.put(variant.getId(), modelDisplayList.get(baseVariant.getId()));
        }
    }

    public void render(ObjModelRenderer model, byte textureVariantID) {
        if (objObjectData.getMesh().materials.isEmpty())
            return;
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        if (OpenGlHelper.useVbo()) {
            setupVAO();
            if(!modelDisplayList.containsKey(textureVariantID)) {
                if(!modelDisplayList.containsKey((byte) 0))
                    return;
                renderVAO(model, modelDisplayList.get((byte) 0));
            } else
                renderVAO(model, modelDisplayList.get(textureVariantID));
        } else {
            if (!modelDisplayList.containsKey(textureVariantID)) {
                GlStateManager.color(1, 0, 0);
                GlStateManager.callList(modelDisplayList.get((byte) 0).displayListId);
                GlStateManager.color(1, 1, 1);
            } else
                GlStateManager.callList(modelDisplayList.get(textureVariantID).displayListId);
            GlStateManager.disableBlend();
            GlStateManager.bindTexture(ClientEventHandler.MC.getTextureMapBlocks().getGlTextureId());
        }
    }

    private Material bindMaterial(ObjModelRenderer model, String materialName, @Nullable String baseVariantName, @Nullable String variantName, boolean areVbosEnabled) {
        if(variantName != null && materialName.equals(baseVariantName))
            materialName = variantName;
        Material material = model.getMaterials().get(materialName);
        if(material == null && baseVariantName != null)
            material = model.getMaterials().get(baseVariantName);
        if (!isMaterialValid(model, material))
            return null;
        // For compatibility with sub-textures directly in materials
        MaterialTexture materialMultipleTextures = material.diffuseTexture.containsKey(variantName) ? material.diffuseTexture.get(variantName) : material.diffuseTexture.get("default");
        if (materialMultipleTextures != null) {
            if (areVbosEnabled)
                ClientEventHandler.MC.getTextureManager().bindTexture(materialMultipleTextures.getPath());
            else
                bindTexture(materialMultipleTextures.getGlTextureId());
        } else
            log.error("Failed to load Default texture of " + objObjectData.getName() + " in " + model.getLocation() + " in material " + material.getName());
        return material;
    }

    private void renderVAO(ObjModelRenderer model, VariantRenderData renderData) {
        DynamXRenderUtils.bindVertexArray(renderData.vaoId);
        GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GlStateManager.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        for(Map.Entry<String, Material.IndexPair> pair : getObjObjectData().getMesh().materials.entrySet()) {
            Material material = bindMaterial(model, pair.getKey(), renderData.getBaseVariant(), renderData.getVariant(), true);
            if(material != null) {
                GlStateManager.color(material.diffuseColor.x * objectColor.x, material.diffuseColor.y * objectColor.y, material.diffuseColor.z * objectColor.z,
                        material.transparency * objectColor.w);
                Material.IndexPair indexPair = pair.getValue();
                GL11.glDrawElements(GL11.GL_TRIANGLES, (indexPair.getFinalIndex() - indexPair.getStartIndex()), GL11.GL_UNSIGNED_INT,
                       4L * indexPair.getStartIndex());
                objectColor.set(1, 1, 1, 1);
            }
        }
        GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GlStateManager.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        DynamXRenderUtils.bindVertexArray(0);
    }


    /**
     * Model is assumed to be not empty
     */
    private void renderCPU(ObjModelRenderer model, @Nullable String baseMaterial, @Nullable String textureName) {
        //Reset bind texture
        startDrawing();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder renderer = tess.getBuffer();
        String[] materialForEachVertex = objObjectData.getMesh().materialForEachVertex;
        Material bind = bindMaterial(model, materialForEachVertex[0], baseMaterial, textureName, false);
        if(bind == null)
            return;
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

            //Material materialToBind = objObjectData.getMesh().materialForEachVertex[i / 3];
            Material materialToBind = bindMaterial(model, materialForEachVertex[i / 3], baseMaterial, textureName, false);
            if (materialToBind != null && materialToBind != bind) {
                bind = materialToBind;
                if (drawing)
                    tess.draw();
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
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, DynamXUtils.createIntBuffer(indices), GL15.GL_STATIC_DRAW);
    }

    private void setupArraysPointers(EnumGLPointer glPointer, float[] data) {
        int vboId = GL15.glGenBuffers();
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
                DynamXErrorManager.addError(model.getTextureVariants() != null ? model.getTextureVariants().getPackName() : "Non-pack model", DynamXErrorManager.MODEL_ERRORS, "obj_none_material", ErrorLevel.LOW, model.getLocation().toString(), objObjectData.getName());
            }
            model.hasNoneMaterials = true;
            return false;
        }
        return true;
    }

    enum EnumGLPointer {
        VERTEX, TEX_COORDS, NORMAL
    }

    @Override
    public String toString() {
        return "ObjObjectRenderer{" +
                "objObjectData=" + objObjectData +
                '}';
    }

     @ToString
    public static class VariantRenderData {
        private final TextureVariantData baseVariant;
        private final TextureVariantData variant;
        private final int displayListId;
        private int vaoId;

        public VariantRenderData(TextureVariantData baseVariant, TextureVariantData variant, int displayListId, int vaoId) {
            this.baseVariant = baseVariant;
            this.variant = variant;
            this.displayListId = displayListId;
            this.vaoId = vaoId;
        }

        public String getBaseVariant() {
            return baseVariant != null ? baseVariant.getName() : null;
        }
        public String getVariant() {
            return variant != null ? variant.getName() : null;
        }
    }
}
