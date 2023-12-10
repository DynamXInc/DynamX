package fr.dynamx.client.renders.model.renderer;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.dxmodel.IModelTextureVariantsSupplier;
import fr.dynamx.client.renders.model.texture.MaterialTexture;
import fr.dynamx.client.renders.model.texture.TextureVariantData;
import fr.dynamx.common.objloader.data.Material;
import fr.dynamx.common.objloader.data.ObjObjectData;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.client.DynamXRenderUtils;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import javax.annotation.Nullable;
import javax.vecmath.Vector4f;
import java.util.HashMap;
import java.util.Map;

import static fr.dynamx.common.DynamXMain.log;

public class ObjObjectRenderer {
    private final Map<Byte, VariantRenderData> modelRenderData = new HashMap<>();
    @Getter
    private final ObjObjectData objObjectData;
    @Getter
    @Setter
    private Vector4f objectColor = new Vector4f(1, 1, 1, 1);

    public ObjObjectRenderer(ObjObjectData objObjectData) {
        this.objObjectData = objObjectData;
    }

    public void uploadVAO() {
        if (modelRenderData.isEmpty()) //Add default render data
            modelRenderData.put((byte) 0, new VariantRenderData(null, null));
        if (objObjectData.getMaterials().isEmpty()) {
            objObjectData.clearData();
            return;
        }
        for (Map.Entry<Byte, VariantRenderData> entry : modelRenderData.entrySet()) {
            if (entry.getValue().vaoId == -1) {
                int vaoID = DynamXRenderUtils.genVertexArrays();
                DynamXRenderUtils.bindVertexArray(vaoID);
                entry.getValue().vaoId = vaoID;

                entry.getValue().ebo = setupIndicesBuffer(getObjObjectData().getIndices());
                entry.getValue().vboPositions = setupArraysPointers(EnumGLPointer.VERTEX, getObjObjectData().getVerticesPos());
                entry.getValue().vboNormals = setupArraysPointers(EnumGLPointer.NORMAL, getObjObjectData().getVerticesNormals());
                entry.getValue().vboTexCoords = setupArraysPointers(EnumGLPointer.TEX_COORDS, getObjObjectData().getTextureCoords());

                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                DynamXRenderUtils.bindVertexArray(0);
            }
        }
        objObjectData.clearData();
    }

    public void clearVAO() {
        if (!modelRenderData.isEmpty()) {
            modelRenderData.forEach((textureID, renderData) -> {
                if (renderData.vaoId != -1)
                    OpenGlHelper.glDeleteBuffers(renderData.vaoId);
                if (renderData.ebo != -1)
                    OpenGlHelper.glDeleteBuffers(renderData.ebo);
                if (renderData.vboPositions != -1)
                    OpenGlHelper.glDeleteBuffers(renderData.vboPositions);
                if (renderData.vboNormals != -1)
                    OpenGlHelper.glDeleteBuffers(renderData.vboNormals);
            });
            modelRenderData.clear();
        }
    }

    public void setTextureVariants(ObjModelRenderer model, IModelTextureVariantsSupplier.IModelTextureVariants variants) {
        for (TextureVariantData variant : variants.getTextureVariants().values()) {
            boolean usesVariant = variant.getId() == 0 || model.getMaterials().containsKey(variant.getName());
            if (!usesVariant) { //search variant in used textures
                for (String materialName : objObjectData.getMaterialForEachVertex()) {
                    Material material = model.getMaterials().get(materialName);
                    if (material != null && material.diffuseTexture.containsKey(variant.getName())) {
                        usesVariant = true;
                        break;
                    }
                }
            }
            if (usesVariant)
                modelRenderData.put(variant.getId(), new VariantRenderData(variants.getDefaultVariant(), variant));
        }
    }

    public void render(ObjModelRenderer model, byte textureVariantID) {
        if (modelRenderData.isEmpty()) {
            log.error("Default texture variant not loaded for model " + model.getLocation() + ". Trying to upload the vaos now.");
            uploadVAO();
        }
        if (modelRenderData.containsKey(textureVariantID))
            renderVAO(model, modelRenderData.get(textureVariantID));
        else if (modelRenderData.containsKey((byte) 0))
            renderVAO(model, modelRenderData.get((byte) 0));
        else
            throw new IllegalStateException("Default texture variant not loaded for model " + model.getLocation());
    }

    private Material bindMaterial(ObjModelRenderer model, String materialName, @Nullable String baseVariantName, @Nullable String variantName) {
        if (variantName != null && materialName.equals(baseVariantName))
            materialName = variantName;
        Material material = model.getMaterials().get(materialName);
        if (material == null && baseVariantName != null)
            material = model.getMaterials().get(baseVariantName);
        if (!isMaterialValid(model, material))
            return null;
        // For compatibility with sub-textures directly in materials
        MaterialTexture materialMultipleTextures = material.diffuseTexture.containsKey(variantName) ? material.diffuseTexture.get(variantName) : material.diffuseTexture.get("default");
        if (materialMultipleTextures != null)
            bindTexture(materialMultipleTextures.getGlTextureId());
        else
            log.error("Failed to load Default texture of " + objObjectData.getName() + " in " + model.getLocation() + " in material " + material.getName());
        return material;
    }

    int i = 0;

    private void renderVAO(ObjModelRenderer model, VariantRenderData renderData) {
        if (renderData.vaoId == -1)
            return;
        DynamXRenderUtils.bindVertexArray(renderData.vaoId);
        GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GlStateManager.glEnableClientState(GL11.GL_NORMAL_ARRAY);

        for (Map.Entry<String, Material.IndexPair> pair : getObjObjectData().getMaterials().entrySet()) {
            Material material = bindMaterial(model, pair.getKey(), renderData.getBaseVariant(), renderData.getVariant());
            if (material == null) {
                continue;
            }

            if (renderData.ebo != -1)
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, renderData.ebo);

            if (material.transparency != 1) {
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
            if (MinecraftForgeClient.getRenderPass() == -1 ||
                    (material.transparency != 1 && MinecraftForgeClient.getRenderPass() == 1) ||
                    (material.transparency == 1 && MinecraftForgeClient.getRenderPass() == 0)) {
                GlStateManager.color(
                        material.ambientColor.x * objectColor.x,
                        material.ambientColor.y * objectColor.y,
                        material.ambientColor.z * objectColor.z,
                        material.transparency * objectColor.w);
                Material.IndexPair indexPair = pair.getValue();
                GL11.glDrawElements(GL11.GL_TRIANGLES, (indexPair.getFinalIndex() - indexPair.getStartIndex()), GL11.GL_UNSIGNED_INT,
                        4L * indexPair.getStartIndex());
            }
            objectColor.set(1, 1, 1, 1);

            if (renderData.ebo != -1)
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        }
        GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GlStateManager.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        DynamXRenderUtils.bindVertexArray(0);
    }

    private void bindTexture(int id) {
        GlStateManager.bindTexture(id);
    }

    private int setupIndicesBuffer(int[] indices) {
        int vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, DynamXUtils.createIntBuffer(indices), GL15.GL_STATIC_DRAW);
        return vboId;
    }

    private int setupArraysPointers(EnumGLPointer glPointer, float[] data) {
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
        return vboId;
    }

    private boolean isMaterialValid(ObjModelRenderer model, Material material) {
        if (material == null)
            return false;
        if (material.getName().equals("none")) //BlockBench uses "none" materials, this is a bug
        {
            if (!model.hasNoneMaterials) {
                DynamXErrorManager.addError(model.getTextureVariants() != null ? model.getTextureVariants().getPackName() : "Non-pack model", DynamXErrorManager.MODEL_ERRORS, "obj_none_material", ErrorLevel.LOW, model.getLocation().getModelPath().toString(), objObjectData.getName());
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
    @RequiredArgsConstructor
    public static class VariantRenderData {
        private final TextureVariantData baseVariant;
        private final TextureVariantData variant;
        private int vaoId = -1;
        private int vboPositions = -1;
        private int vboNormals = -1;
        private int vboTexCoords = -1;
        private int ebo = -1;

        public String getBaseVariant() {
            return baseVariant != null ? baseVariant.getName() : null;
        }

        public String getVariant() {
            return variant != null ? variant.getName() : null;
        }
    }
}
