package fr.dynamx.common.contentpack.loader;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.utils.DynamXReflection;
import fr.dynamx.utils.client.ContentPackUtils;
import fr.dynamx.utils.errors.DynamXErrorManager;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ProgressManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static fr.dynamx.common.DynamXMain.log;

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
     * @return The object, to use in classes extending DynamX ones (example : {@link fr.dynamx.common.blocks.DynamXBlock})
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
