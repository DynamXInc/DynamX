package fr.dynamx.common.contentpack.loader;

import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.api.contentpack.object.IInfoOwner;
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
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;

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
 * @param <U> The object type if it's an {@link ISubInfoTypeOwner} and you use a {@link SubInfoTypesRegistry}, or {@link fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner.Empty}
 * @see ObjectInfo
 * @see BuildableInfoLoader
 */
public class ObjectLoader<T extends ObjectInfo<?>, C extends IInfoOwner<?>, U extends ISubInfoTypeOwner<?>> extends InfoLoader<T, U> {
    /**
     * All {@link IInfoOwner}s associated with our objects
     */
    public final List<IInfoOwner<T>> owners = new ArrayList<>();
    /**
     * Builtin java objects added by mods, register once and remembered for hot reloads
     */
    protected final List<T> builtinObjects = new ArrayList<>();

    /**
     * @param prefix       The prefix used to detect associated .dnx files
     * @param assetCreator A function matching an object packName and name with its object class
     */
    public ObjectLoader(String prefix, BiFunction<String, String, T> assetCreator, @Nullable SubInfoTypesRegistry<U> infoTypesRegistry) {
        super(prefix, assetCreator, infoTypesRegistry);
    }

    @Override
    public void clear(boolean hot) {
        super.clear(hot);
        //DO NOT CLEAR OWNERS, ITEMS ARE REUSED !
        for (T b : builtinObjects) {
            if (hot) //If it's an hot reload, reload builtin items
                loadItems(b, true);
            else //Else add the info in the map, but DO NOT create an object owner, it already exists !
                super.loadItems(b, false);
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
        T info = assetCreator.apply(modName, objectName);
        owners.add((IInfoOwner<T>) owner);
        builtinObjects.add(info);
        if(DynamXObjectLoaders.PACKS.findPackInfoByPackName(modName) == null)
            DynamXObjectLoaders.PACKS.addInfo(modName, new PackInfo(modName, ContentPackType.BUILTIN));
        //System.out.println("Builtin pack info added is " + DynamXObjectLoaders.PACKS.findPackInfoByPackName(modName)+" with name " + modName);
        //System.out.println("All info " + DynamXObjectLoaders.PACKS.getInfos());
        return info;
    }

    /**
     * Puts the info into infos map, and updates other references to this objects (in {@link IInfoOwner}s for example)
     * <br>
     * Also generates owner of the info
     */
    @Override
    @SuppressWarnings("unchecked")
    public void loadItems(T info, boolean hot) {
        super.loadItems(info, hot);
        if (!hot) {
            boolean client = FMLCommonHandler.instance().getSide().isClient();
            Object[] tabItem = new Object[1];
            if (info instanceof AbstractItemObject) {
                String creativeTabName = ((AbstractItemObject<?>) info).getCreativeTabName();
                if (creativeTabName != null && !creativeTabName.equalsIgnoreCase("None")) {
                    if (DynamXItemRegistry.creativeTabs.stream().noneMatch(p -> DynamXReflection.getCreativeTabName(p).equals(creativeTabName))) {
                        DynamXMain.log.info("Creative Tab : " + creativeTabName + " not found. Creating a new one");
                        DynamXItemRegistry.creativeTabs.add(new CreativeTabs(creativeTabName) {
                            @Override
                            public ItemStack createIcon() {
                                if (tabItem[0] != null) {
                                    return tabItem[0] instanceof Item ? new ItemStack((Item) tabItem[0]) : new ItemStack((Block) tabItem[0]);
                                }
                                return new ItemStack(Items.APPLE);
                            }
                        });
                    }
                }
            }
            C[] obj = (C[]) ((ObjectInfo<T>) info).createOwners(this);
            if (obj.length > 0) {
                tabItem[0] = obj[0];
            }
            for (C ob : obj) {
                owners.add((IInfoOwner<T>) ob);
                if (client) {
                    if (ob instanceof IResourcesOwner && ((IResourcesOwner) ob).createTranslation()) {
                        for (int metadata = 0; metadata < ((IResourcesOwner) ob).getMaxMeta(); metadata++) {
                            ContentPackUtils.addMissingLangFile(DynamXMain.resDir, (IInfoOwner<T>) ob, metadata);
                        }
                    }
                }
            }
        } else {
            boolean found = false;
            for (IInfoOwner<T> blockImporter : owners) {
                if (blockImporter.getInfo().getFullName().equalsIgnoreCase(info.getFullName())) {
                    blockImporter.setInfo(info);
                    found = true;
                    //Don't break, multiple items can have the same infos
                }
            }
            if (!found) {
                log.error("Cannot hotswap " + info.getFullName() + " in " + owners);
            }
        }
    }
}
