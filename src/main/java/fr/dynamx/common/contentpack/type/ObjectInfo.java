package fr.dynamx.common.contentpack.type;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.common.contentpack.loader.InfoList;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

/**
 * Base of every pack object
 *
 * @param <T> The object class
 */
public abstract class ObjectInfo<T extends ObjectInfo<?> & ISubInfoTypeOwner<?>> implements INamedObject {
    private final String packName;
    private final String fileName;
    @PackFileProperty(configNames = "Description", description = "common.description")
    private String description;
    @PackFileProperty(configNames = "Name", description = "common.name")
    private String defaultName;
    /**
     * IDynamXItem are the objects using this info. It is, for example, a block or an item, or all item armors of an ArmorInfo. <br>
     * Null before postLoad step.
     */
    @Getter
    protected IDynamXItem<T>[] items;

    /**
     * @param packName The name of the pack owning the object
     * @param fileName The name of the object
     */
    public ObjectInfo(String packName, String fileName) {
        this.packName = packName;
        this.fileName = fileName;
    }

    @Override
    public String getPackName() {
        return packName;
    }

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public String getFullName() {
        return packName + "." + fileName;
    }

    /**
     * @return The name of the texture file for this object's item, according to the given metadata
     */
    public String getIconFileName(byte metadata) {
        return getName().toLowerCase();
    }

    /**
     * @return A description of the object, can be null if object config is bad
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * IDynamXItems are the objects using this info, can be for example a block or an item, or all armors of an ArmorInfo. <br>
     * Called by createItems() default implementation. <br>
     *
     * @return An IDynamXItems for this object. Null if the object has failed to load
     */
    @Nullable
    protected abstract IDynamXItem<T> createItem(InfoList<T> loader);

    /**
     * Inits the items for this object. <br>
     * IDynamXItems are the objects using this info. It is, for example, a block or an item, or all armors of an ArmorInfo.
     *
     * @param loader The loader of this object
     * @return All IDynamXItems for this object
     */
    @SuppressWarnings("unchecked")
    public IDynamXItem<T>[] createItems(InfoList<T> loader) {
        IDynamXItem<T> owner = createItem(loader);
        if (owner == null)
            return new IDynamXItem[0];
        items = new IDynamXItem[]{owner};
        return items;
    }

    /**
     * Post loads this object <br>
     * Can be used for collision shape generation
     *
     * @param hot If it's a hot reloading (info owners already created)
     * @return False if an error occurred and this object shouldn't be loaded
     */
    public boolean postLoad(boolean hot) {
        return true;
    }

    /**
     * @return The translation key for the given item, used for auto-translation
     */
    public String getTranslationKey(IDynamXItem<T> item, int itemMeta) {
        return getFullName().toLowerCase();
    }

    /**
     * @return The default translation name for the given item, used for auto-translation
     */
    public String getTranslatedName(IDynamXItem<T> item, int itemMeta) {
        return (getDefaultName() == null ? getName() : getDefaultName());
    }

    /**
     * @return The default name of the object in translation, null if object config is bad
     */
    @Nullable
    public String getDefaultName() {
        return defaultName;
    }

    public void setDefaultName(String defaultName) {
        this.defaultName = defaultName;
    }

    public void setItems(IDynamXItem<?>[] items) {
        // @Setter annotation cannot be used because of generic types
        this.items = (IDynamXItem<T>[]) items;
    }
}
