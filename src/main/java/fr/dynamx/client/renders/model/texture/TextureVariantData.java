package fr.dynamx.client.renders.model.texture;

import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nullable;

/**
 * Properties of a texture used in obj models
 *
 */
@ToString
public class TextureVariantData {
    /**
     * The name of the texture, matching with the name in mtl files
     */
    @Getter
    private final String name;
    /**
     * The id of this texture, unique in one vehicle
     */
    @Getter
    private final byte id;

    /**
     * This texture will have an associated item
     *
     * @param name     The name of the texture, matching with the name in mtl files
     * @param id       The id of this texture, should be unique in one vehicle
     */
    public TextureVariantData(String name, byte id) {
        this.name = name.toLowerCase();
        this.id = id;
    }
}
