package fr.dynamx.core.common.contentpack.loader;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.core.common.blocks.DynamXBlock;
import fr.dynamx.core.common.contentpack.ContentPackLoader;
import fr.dynamx.core.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.core.common.contentpack.PackInfo;
import fr.dynamx.core.common.contentpack.type.ObjectInfo;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Automatic loader of specific info objects
 *
 * @param <T> The objects class
 * @param <C> The owners class
 * @see ObjectInfo
 */
public class ObjectLoader<T extends ObjectInfo<?> & ISubInfoTypeOwner<?>, C extends IDynamXItem<?>> extends InfoLoader<T> {
    /**
     * All {@link IDynamXItem}s associated with our objects
     */
    public final List<IDynamXItem<T>> owners = new ArrayList<>();
    /**
     * Builtin java objects added by mods, register once and remembered for hot reloads
     */
    protected final List<T> builtinObjects = new ArrayList<>();

    /**
     * @param prefix       The prefix used to detect associated .dnx files
     * @param assetCreator A function matching an object packName and name with its object class
     */
    public ObjectLoader(String prefix, BiFunction<String, String, T> assetCreator, @Nullable SubInfoTypesRegistry<T> infoTypesRegistry) {
        super(prefix, assetCreator, infoTypesRegistry);
    }

    @Override
    public void clear(boolean hot) {
        super.clear(hot);
        //DO NOT CLEAR OWNERS, ITEMS ARE REUSED !
        for (T b : builtinObjects) {
            loadItems(b, hot);
        }
    }

    /**
     * Registers a builtin object, ie added from mods <br>
     * The object will have the same properties as if it was added in a pack,
     * and it is automatically reused when packs are reloaded <br> <br>
     * NOTE : Should be called during addons initialization
     *
     * @param modName    The name of the mod adding this object
     * @param objectName The name of the object
     * @return The object, to use in classes extending DynamX ones (example : {@link DynamXBlock})
     * @throws IllegalStateException If you call this after the start of packs loading (see ContentPackLoader.isPackLoadingStarted)
     * @see fr.dynamx.api.contentpack.DynamXAddon
     */
    @SuppressWarnings("unchecked")
    public T addBuiltinObject(C owner, String modName, String objectName) {
        if (ContentPackLoader.isPackLoadingStarted())
            throw new IllegalStateException("You should register your builtin objects before packs loading. Use the addon init callback.");
        T info = assetCreator.create(modName, objectName, null);
        owners.add((IDynamXItem<T>) owner);
        builtinObjects.add(info);
        if (DynamXObjectLoaders.PACKS.findPackLocations(modName).isEmpty())
            DynamXObjectLoaders.PACKS.loadItems(PackInfo.forAddon(modName), false);
        return info;
    }

    @Override
    public void postLoad(boolean hot) {
        super.postLoad(hot);
        updateItems(this, owners, builtinObjects, hot);
    }

    /**
     * @return Maps a built info with the right item, if initialized, or returns null <br>
     * Note for armors, directly call ArmorObject.getOwners()
     */
    public C getItem(T from) {
        return from.getItems().length == 1 ? (C) from.getItems()[0] : null;
    }
}
