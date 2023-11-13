package fr.dynamx.common.contentpack.loader;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.utils.DynamXReflection;
import fr.dynamx.utils.client.ContentPackUtils;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ProgressManager;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.dynamx.common.DynamXMain.log;

/**
 * List of {@link ISubInfoTypeOwner}s with utility methods to load them
 *
 * @param <T> The objects class
 * @see INamedObject
 * @see ObjectLoader
 */
@Getter
public abstract class InfoList<T extends ISubInfoTypeOwner<?>> {
    /**
     * Loaded objects, identified by their full name
     */
    protected final Map<String, T> infos = new HashMap<>();
    /**
     * SubInfoTypesRegistry for this object
     */
    protected final SubInfoTypesRegistry<T> defaultSubInfoTypesRegistry;

    /**
     * @param defaultSubInfoTypesRegistry
     */
    public InfoList(@Nullable SubInfoTypesRegistry<T> defaultSubInfoTypesRegistry) {
        this.defaultSubInfoTypesRegistry = defaultSubInfoTypesRegistry;
    }

    /**
     * @return The name of the listed objects, used for logging and errors
     */
    public abstract String getName();

    /**
     * Clears infos, used for hot reload
     *
     * @param hot If it's a hot reload
     */
    public void clear(boolean hot) {
        infos.clear();
    }

    /**
     * Puts the info into infos map, and updates other references to these objects (in {@link IDynamXItem}s for example)
     */
    public void loadItems(T info, boolean hot) {
        info.onComplete(hot);
        infos.put(info.getFullName(), info);
    }

    /**
     * Post-loads the objects (shape generation...), and creates the IInfoOwners
     *
     * @param hot True if it's a hot reload
     */
    public abstract void postLoad(boolean hot);

    /**
     * @return The info from the info's full name, or null
     */
    @Nullable
    public T findInfo(String infoFullName) {
        return infos.get(infoFullName);
    }

    public boolean hasSubInfoTypesRegistry() {
        return defaultSubInfoTypesRegistry != null;
    }

    /**
     * Creates and update {@link IDynamXItem}s depending on the loading phase (hot or not).
     *
     * @param infoList
     * @param owners
     * @param builtinObjects
     * @param hot
     * @param <T>
     * @param <C>
     */
    public static <T extends ObjectInfo<?> & ISubInfoTypeOwner<?>, C extends IDynamXItem<?>> void updateItems(InfoList<T> infoList, List<IDynamXItem<T>> owners, List<T> builtinObjects, boolean hot) {
        Map<String, T> infos = infoList.getInfos();
        ProgressManager.ProgressBar bar1 = ProgressManager.push("Post-loading " + infoList.getName(), infos.size());
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
                if (!builtinObjects.contains(info)) {
                    C[] obj = (C[]) ((ObjectInfo<T>) info).createItems(infoList);
                    if (obj.length > 0) {
                        tabItem[0] = obj[0];
                    }
                    for (C ob : obj) {
                        owners.add((IDynamXItem<T>) ob);
                        if (client) {
                            if (ob instanceof IResourcesOwner && ((IResourcesOwner) ob).createTranslation()) {
                                for (int metadata = 0; metadata < ((IResourcesOwner) ob).getMaxMeta(); metadata++) {
                                    String translationKey = info.getTranslationKey((IDynamXItem) ob, metadata) + ".name";
                                    String translationValue = info.getTranslatedName((IDynamXItem) ob, metadata);
                                    ContentPackUtils.addMissingLangFile(DynamXMain.resourcesDirectory, info.getPackName(), translationKey, translationValue);
                                }
                            }
                        }
                    }
                }
            } else if (!builtinObjects.contains(info)) { //Refresh infos objects contained in created info owners
                boolean found = false;
                for (IDynamXItem<T> owner : owners) {
                    if (owner.getInfo().getFullName().equalsIgnoreCase(info.getFullName())) {
                        if(!found) {
                            T oldInfo = owner.getInfo();
                            info.setItems(oldInfo.getItems());
                            found = true;
                        }
                        owner.setInfo(info);
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
}
