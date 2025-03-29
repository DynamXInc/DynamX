package fr.dynamx.client.renders.model.renderer;

import com.modularmods.mcgltf.dynamx.MCglTF;
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
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;

import javax.annotation.Nullable;
import javax.vecmath.Vector4f;
import java.util.HashMap;
import java.util.Map;

import static fr.dynamx.common.DynamXMain.log;

public class ObjObjectRenderer {
    public static final int COLOR_MAP_INDEX = GL13.GL_TEXTURE0;
    public static final int NORMAL_MAP_INDEX = GL13.GL_TEXTURE2;
    public static final int SPECULAR_MAP_INDEX = GL13.GL_TEXTURE3;

    @Getter
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
                if (renderData.vboTexCoords != -1)
                    OpenGlHelper.glDeleteBuffers(renderData.vboTexCoords);
            });
            modelRenderData.clear();
        }
    }

    public void setTextureVariants(ObjModelRenderer model, IModelTextureVariantsSupplier.IModelTextureVariants variants) {
        for (TextureVariantData variant : variants.getTextureVariants().values()) {
            // The variant may, or may not, be added as a separated Material/as a texture in the material
            // Let's check that
            // Base/default variant should always be added.
            // 1. If the variant exists as a Material, add it.
            boolean usesVariant = variant.getId() == 0 || model.getMaterials().containsKey(variant.getName());
            if (!usesVariant) {
                // 2. If a material has a texture with the same name as the variant
                // This obj object is actually using this  variant
                for (String materialName : objObjectData.getMaterialForEachVertex()) {
                    Material material = model.getMaterials().get(materialName);
                    if (material != null && material.diffuseTexture.containsKey(variant.getName())) {
                        usesVariant = true;
                        break;
                    }
                }
            }
            // If the variant is used, let's add it and create a vao for it
            if (usesVariant) {
                modelRenderData.put(variant.getId(), new VariantRenderData(variants.getDefaultVariant(), variant));
            }
        }
    }

    public void render(ObjModelRenderer model, byte textureVariantID, boolean forceVanillaRender) {
        if (modelRenderData.isEmpty()) {
            log.error("Default texture variant not loaded for model " + model.getLocation() + ". Trying to upload the vaos now.");
            uploadVAO();
        }

        if (modelRenderData.containsKey(textureVariantID)) {
            renderVAO(model, modelRenderData.get(textureVariantID), forceVanillaRender);
        } else if (modelRenderData.containsKey((byte) 0)) {
            renderVAO(model, modelRenderData.get((byte) 0), forceVanillaRender);
        } else {
            throw new IllegalStateException("Default texture variant not loaded for model " + model.getLocation());
        }

        if (!forceVanillaRender) {
            GlStateManager.setActiveTexture(NORMAL_MAP_INDEX);
            GlStateManager.bindTexture(MCglTF.getInstance().getDefaultNormalMap());
            GlStateManager.setActiveTexture(SPECULAR_MAP_INDEX);
            GlStateManager.bindTexture(MCglTF.getInstance().getDefaultSpecularMap());
            GlStateManager.setActiveTexture(COLOR_MAP_INDEX);
            GlStateManager.bindTexture(MCglTF.getInstance().getDefaultColorMap());
        }
    }

    /**
     * Binds the desired material
     *
     * @param model              The obj model containing this obj object, and the associated materials
     * @param materialName       The material to bind
     * @param baseVariantName    The base/default variant of the model
     * @param variantName        The variant to bind. If the materialName is the same as the baseVariantName, the materialName will be replaced by the variantName (if a material with this name exists).
     *                           It will also load the texture matching this variantName, if any (or load the default one).
     * @param forceVanillaRender True will prevent from applying PBR/specular textures
     * @return The bound material, or null if no matching material was found
     */
    private Material bindMaterial(ObjModelRenderer model, String materialName, @Nullable String baseVariantName, @Nullable String variantName, boolean forceVanillaRender) {
        if (variantName != null && materialName.equals(baseVariantName)) {
            materialName = variantName;
        }

        Material material = model.getMaterials().get(materialName);
        if (material == null && baseVariantName != null) {
            material = model.getMaterials().get(baseVariantName);
        }

        if (!isMaterialValid(model, material)) {
            return null;
        }

        MaterialTexture diffuseTexture = material.diffuseTexture.getOrDefault(variantName, material.diffuseTexture.get("default"));
        bindTextureIfAny(diffuseTexture, COLOR_MAP_INDEX);

        if (diffuseTexture == null) {
            log.error("Failed to load texture of {} in {} in material {}", objObjectData.getName(), model.getLocation(), material.getName());
        }

        if (!forceVanillaRender) {
            MaterialTexture normalTexture = material.normalTexture.getOrDefault(variantName, material.normalTexture.get("default"));
            bindTextureIfAny(normalTexture, NORMAL_MAP_INDEX);

            MaterialTexture specularTexture = material.specularTexture.getOrDefault(variantName, material.specularTexture.get("default"));
            bindTextureIfAny(specularTexture, SPECULAR_MAP_INDEX);
        }

        return material;
    }

    private void bindTextureIfAny(@Nullable MaterialTexture texture, int textureIndex) {
        if (texture == null) {
            return;
        }
        GlStateManager.setActiveTexture(textureIndex);
        GlStateManager.bindTexture(texture.getGlTextureId());
    }

    private void renderVAO(ObjModelRenderer model, VariantRenderData renderData, boolean forceVanillaRender) {
        if (renderData.vaoId == -1)
            return;
        DynamXRenderUtils.bindVertexArray(renderData.vaoId);
        GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GlStateManager.glEnableClientState(GL11.GL_NORMAL_ARRAY);

        for (Map.Entry<String, Material.IndexPair> pair : getObjObjectData().getMaterials().entrySet()) {
            Material material = bindMaterial(model, pair.getKey(), renderData.getBaseVariant(), renderData.getVariant(), forceVanillaRender);
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
