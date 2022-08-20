package fr.dynamx.api.obj;

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
    /**
     * @return An id to {@link TextureVariantData} map that can be applied to the given {@link ObjObjectData} <br>
     * Return null to always apply default texture
     */
    @Nullable
    default Map<Byte, TextureVariantData> getTextureVariantsFor(ObjObjectRenderer objObjectRenderer) {
        return null;
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
}
