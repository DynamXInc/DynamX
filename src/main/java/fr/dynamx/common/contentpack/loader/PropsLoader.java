package fr.dynamx.common.contentpack.loader;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.ContentPackLoader;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.common.items.ItemProps;
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

import java.util.ArrayList;
import java.util.List;

import static fr.dynamx.common.DynamXMain.log;

/**
 * Loader of props. This loader is different from {@link ObjectLoader} because props aren't loaded from files, but from their block owner.
 *
 * @param <T> The objects class
 * @see ObjectInfo
 */
public class PropsLoader<T extends PropObject<?>> extends InfoList<T> {
    /**
     * All {@link IDynamXItem}s associated with our objects
     */
    public final List<IDynamXItem<T>> owners = new ArrayList<>();
    /**
     * Builtin java objects added by mods, register once and remembered for hot reloads
     */
    protected final List<T> builtinObjects = new ArrayList<>();

    public PropsLoader() {
        super(new SubInfoTypesRegistry<>());
    }

    @Override
    public String getName() {
        return "props";
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
     * @param modName The name of the mod adding this object
     * @param object  The builtin object to add
     * @throws IllegalStateException If you call this after the start of packs loading (see ContentPackLoader.isPackLoadingStarted)
     * @see fr.dynamx.api.contentpack.DynamXAddon
     */
    public void addBuiltinObject(String modName, T object) {
        if (ContentPackLoader.isPackLoadingStarted())
            throw new IllegalStateException("You should register your builtin objects before packs loading. Use the addon init callback.");
        builtinObjects.add(object);
        if (DynamXObjectLoaders.PACKS.findPackLocations(modName).isEmpty())
            DynamXObjectLoaders.PACKS.loadItems(PackInfo.forAddon(modName), false);
    }

    @Override
    public void postLoad(boolean hot) {
        updateItems(this, owners, builtinObjects, hot);
    }

    /**
     * @return Maps a built info with the right item, if initialized, or returns null
     */
    public ItemProps<?> getItem(T from) {
        return from.getItems().length == 1 ? (ItemProps<?>) from.getItems()[0] : null;
    }
}
