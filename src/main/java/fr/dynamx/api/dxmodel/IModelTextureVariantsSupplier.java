package fr.dynamx.api.dxmodel;

import fr.dynamx.common.objloader.data.ObjObjectData;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.client.renders.model.texture.TextureVariantData;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Matches an {@link ObjObjectData} with its available textures
 *
 * @see fr.dynamx.client.renders.model.renderer.ObjModelRenderer
 */
public interface IModelTextureVariantsSupplier {
    default IModelTextureVariants getMainObjectVariants() { return getTextureVariantsFor(null); }

    /**
     * @return An id to {@link TextureVariantData} map that can be applied to the given {@link ObjObjectData} <br>
     * Return null to always apply default texture
     */
    @Nullable
    IModelTextureVariants getTextureVariantsFor(ObjObjectRenderer objObjectRenderer);

    default String getMainObjectVariantName(byte variantId) {
        IModelTextureVariants variants = getMainObjectVariants();
        return variants != null ? variants.getVariant(variantId).getName() : "default";
    }

    /**
     * @return True if this supplier has varying textures (more textures than the default texture) <br>
     * If you return false, and this model is registered twice, this texture supplier can be replaced by the other one
     */
    default boolean hasVaryingTextures() {
        return false;
    }

    /**
     * @return The pack owning the corresponding model, used for error messages
     */
    String getPackName();

    default boolean canRenderPart(String partName) {
        return true;
    }

    interface IModelTextureVariants {
        TextureVariantData getDefaultVariant();
        TextureVariantData getVariant(byte variantId);
        Map<Byte, TextureVariantData> getTextureVariants();
    }
}
