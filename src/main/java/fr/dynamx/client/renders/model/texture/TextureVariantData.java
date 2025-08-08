package fr.dynamx.client.renders.model.texture;

import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * Properties of a texture used in obj models
 *
 */
@Getter
@ToString
public class TextureVariantData {
    /**
     * The name of the texture, matching with the name in mtl files
     */
    private final String name;
    /**
     * The id of this texture, unique in one vehicle
     */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextureVariantData that = (TextureVariantData) o;
        return id == that.id && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }
}
