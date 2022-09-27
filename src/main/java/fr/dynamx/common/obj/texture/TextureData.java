package fr.dynamx.common.obj.texture;

import fr.dynamx.api.obj.IModelTextureSupplier;
import fr.dynamx.common.contentpack.type.PartWheelInfo;
import fr.dynamx.common.obj.eximpl.QuickObjObject;

import javax.annotation.Nullable;

/**
 * Properties of a texture used in obj models
 *
 * @see IModelTextureSupplier
 * @see fr.dynamx.common.contentpack.ModularVehicleInfo
 * @see PartWheelInfo
 * @see QuickObjObject
 */
public class TextureData {
    private final String name;
    private final byte id;
    @Nullable
    private final String iconName;

    /**
     * This texture will not have an associated item
     *
     * @param name The name of the texture, matching with the name in mtl files
     * @param id   The id of this texture, should be unique in one vehicle
     */
    public TextureData(String name, byte id) {
        this(name, id, null);
    }

    /**
     * This texture will have an associated item
     *
     * @param name     The name of the texture, matching with the name in mtl files
     * @param id       The id of this texture, should be unique in one vehicle
     * @param iconName The name if the texture of the item
     */
    public TextureData(String name, byte id, String iconName) {
        this.name = name;
        this.id = id;
        this.iconName = iconName;
    }

    /**
     * @return The name of the texture, matching with the name in mtl files
     */
    public String getName() {
        return name;
    }

    /**
     * @return The id of this texture, unique in one vehicle
     */
    public byte getId() {
        return id;
    }

    /**
     * @return If this texture must have an associated item, used for multi-skins vehicles
     */
    public boolean isItem() {
        return iconName != null;
    }

    /**
     * @return The associated item texture name, or null
     */
    @Nullable
    public String getIconName() {
        return iconName;
    }

    @Override
    public String toString() {
        return "TextureData{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", iconName='" + iconName + '\'' +
                '}';
    }
}
