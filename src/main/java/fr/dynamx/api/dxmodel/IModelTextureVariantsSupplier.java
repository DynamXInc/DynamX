package fr.dynamx.api.dxmodel;

import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.core.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.core.client.renders.model.texture.TextureVariantData;
import fr.dynamx.core.common.objloader.data.ObjObjectData;
import fr.dynamx.core.client.renders.model.renderer.ObjModelRenderer;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Matches an {@link ObjObjectData} with its available textures
 *
 * @see ObjModelRenderer
 */
public interface IModelTextureVariantsSupplier extends INamedObject {
    default IModelTextureVariants getMainObjectVariants() {
        return getTextureVariantsFor(null);
    }

    /**
     * @return An id to {@link TextureVariantData} map that can be applied to the given {@link ObjObjectData} <br>
     * Return null to always apply default texture
     */
    @Nullable
    IModelTextureVariants getTextureVariantsFor(ObjObjectRenderer objObjectRenderer);

    default String getMainObjectVariantName(byte variantId) {
        return getMainObjectVariantNameOrDefault(variantId, "default");
    }

    default String getMainObjectVariantNameOrDefault(byte variantId, String notFoundVariantName) {
        IModelTextureVariants variants = getMainObjectVariants();
        TextureVariantData variant = variants != null ? variants.getVariant(variantId) : null;
        return variant != null ? variant.getName() : notFoundVariantName;
    }

    /**
     * @return True if this supplier has varying textures (more textures than the default texture) <br>
     * If you return false, and this model is registered twice, this texture supplier can be replaced by the other one
     */
    default boolean hasTextureVariants() {
        return false;
    }

    /**
     * @return The pack owning the corresponding model, used for error messages
     */
    String getPackName();

    default boolean canRenderPart(String partName) {
        return true;
    }

    byte getMaxVariantId();

    interface IModelTextureVariants {
        TextureVariantData getDefaultVariant();

        TextureVariantData getVariant(byte variantId);

        Map<Byte, TextureVariantData> getTextureVariants();
    }
}
