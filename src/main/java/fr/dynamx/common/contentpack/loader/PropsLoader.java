package fr.dynamx.common.contentpack.loader;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IInfoOwner;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static fr.dynamx.common.DynamXMain.log;

/**
 * Loader of props. This loader is different from {@link ObjectLoader} because props aren't loaded from files, but from their block owner.
 *
 * @param <T> The objects class
 * @see ObjectInfo
 */
public class PropsLoader<T extends PropObject<?>> extends InfoList<T> {
    /**
     * All {@link IInfoOwner}s associated with our objects
     */
    public final List<IInfoOwner<T>> owners = new ArrayList<>();
    /**
     * Builtin java objects added by mods, register once and remembered for hot reloads
     */
    protected final List<T> builtinObjects = new ArrayList<>();
    private final Function<T, ItemProps<?>> itemCreator;

    public PropsLoader(@Nullable SubInfoTypesRegistry<T> infoTypesRegistry) {
        this(null, infoTypesRegistry);
    }

    public PropsLoader(Function<T, ItemProps<?>> itemCreator, @Nullable SubInfoTypesRegistry<T> infoTypesRegistry) {
        super(infoTypesRegistry);
        this.itemCreator = itemCreator;
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
        //TODO SIMPLIFY AND PUT IN COMMON WITH OBJECT LOADER ?
        ProgressManager.ProgressBar bar1 = ProgressManager.push("Post-loading props", infos.size());
        System.out.println("Registry: props: " + getInfos());
        for (T info : infos.values()) {
            bar1.step(info.getFullName());
            try {
                if (!info.postLoad(hot))
                    continue;
            } catch (Exception e) {
                DynamXErrorManager.addError(info.getPackName(), DynamXErrorManager.PACKS_ERRORS, "complete_object_error", ErrorLevel.FATAL, info.getName(), null, e);
                continue;
            }
            if (!hot) {
                boolean client = FMLCommonHandler.instance().getSide().isClient();
                Object[] tabItem = new Object[1];
                if (info instanceof AbstractItemObject) {
                    String creativeTabName = ((AbstractItemObject<?, ?>) info).getCreativeTabName();
                    if (creativeTabName != null && !creativeTabName.equalsIgnoreCase("None")) {
                        if (DynamXItemRegistry.creativeTabs.stream().noneMatch(p -> DynamXReflection.getCreativeTabName(p).equals(creativeTabName))) {
                            CreativeTabs tab = new CreativeTabs(creativeTabName) {
                                @Override
                                public ItemStack createIcon() {
                                    if (tabItem[0] != null) {
                                        return tabItem[0] instanceof Item ? new ItemStack((Item) tabItem[0]) : new ItemStack((Block) tabItem[0]);
                                    }
                                    return new ItemStack(Items.APPLE);
                                }
                            };
                            DynamXItemRegistry.creativeTabs.add(tab);
                            if (client)
                                ContentPackUtils.addMissingLangFile(DynamXMain.resourcesDirectory, info.getPackName(), tab.getTranslationKey(), tab.getTabLabel());
                        }
                    }
                }
                System.out.println("Test:" + info + " // " + builtinObjects);
                IInfoOwner<?>[] obj = ((ObjectInfo<T>) info).createOwners(this);
                if (obj.length > 0) {
                    tabItem[0] = obj[0];
                }
                for (IInfoOwner<?> ob : obj) {
                    owners.add((IInfoOwner<T>) ob);
                    if (client) {
                        if (ob != null && ((ItemProps<?>) ob).createTranslation()) {
                            for (int metadata = 0; metadata < ((IResourcesOwner) ob).getMaxMeta(); metadata++) {
                                String translationKey = info.getTranslationKey((IInfoOwner) ob, metadata) + ".name";
                                String translationValue = info.getTranslatedName((IInfoOwner) ob, metadata);
                                ContentPackUtils.addMissingLangFile(DynamXMain.resourcesDirectory, info.getPackName(), translationKey, translationValue);
                            }
                        }
                    }
                }
            } else { //Refresh infos objects contained in created info owners
                boolean found = false;
                for (IInfoOwner<T> owner : owners) {
                    if (owner.getInfo().getFullName().equalsIgnoreCase(info.getFullName())) {
                        owner.setInfo(info);
                        found = true;
                        //Don't break, multiple items can have the same infos
                    }
                }
                if (!found) {
                    log.error("Cannot hotswap " + info.getFullName() + " in " + owners);
                }
            }
        }
        ProgressManager.pop(bar1);
    }

    /**
     * FIXME USE THIS INSTEAD OF CREATE OWNER ... METHODS
     *
     * @return Maps a built info with the right item
     */
    public ItemProps<?> getItem(T from) {
        return itemCreator.apply(from);
    }
}
