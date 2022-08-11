package fr.dynamx.api.obj;

import fr.dynamx.common.objloader.ObjObjectData;
import fr.dynamx.client.renders.model.ObjObjectRenderer;
import fr.dynamx.client.renders.model.texture.TextureData;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Matches an {@link ObjObjectData} with its available textures
 *
 * @see TessellatorModelClient
 */
public interface IModelTextureSupplier {
    /**
     * @return A id to {@link TextureData} map that can be applied to the given {@link ObjObjectData} <br>
     * Return null to always apply default texture
     */
    @Nullable
    default Map<Byte, TextureData> getTexturesFor(ObjObjectRenderer objObjectRenderer) {
        return null;
    }

    /**
     * @return True if this supplier has custom textures (more textures than the default texture) <br>
     * If you return false, and this model is registered twice, this texture supplier can be replaced by the other one
     */
    default boolean hasCustomTextures() {
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
