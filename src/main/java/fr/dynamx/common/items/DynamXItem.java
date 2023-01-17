package fr.dynamx.common.items;

import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.render.IObjPackObject;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.RegistryNameSetter;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class DynamXItem<T extends AbstractItemObject<T, ?>> extends Item implements IInfoOwner<T>, IResourcesOwner {
    protected T itemInfo;

    /**
     * Use the other constructor to create custom blocks and easily set BlockObject's properties
     */
    public DynamXItem(T itemInfo) {
        this.setInfo(itemInfo);
        RegistryNameSetter.setRegistryName(this, DynamXConstants.ID, itemInfo.getFullName().toLowerCase());
        setTranslationKey(DynamXConstants.ID + "." + itemInfo.getFullName().toLowerCase());
        setCreativeTab(itemInfo.getCreativeTab(DynamXItemRegistry.objectTab));
        setMaxStackSize(itemInfo.getMaxItemStackSize());
        DynamXItemRegistry.add(this);
    }

    /**
     * Use this constructor to create a custom item having the same functionalities as pack item <br>
     * You can customise item properties using this.getInfo() <br> <br>
     * NOTE : Registry name and translation key are automatically set and the item is automatically registered into Forge by DynamX,
     * but don't forget to set a creative tab !<br><br>
     *
     * <strong>NOTE : Should be called during addons initialization</strong>
     *
     * @param modid    The mod owning this item used to register the item
     * @param itemName The name of the item
     * @param model     The obj model of the block "namespace:resourceName.obj". The default namespace is "dynamxmod". The model file must be under "namespace:models/resourceName.obj"
     */
    public DynamXItem(String modid, String itemName, String model) {
        if (modid.contains("builtin_mod_")) { //Backward-compatibility
            itemInfo = (T) DynamXObjectLoaders.ITEMS.addBuiltinObject(this, modid, itemName);
            modid = modid.replace("builtin_mod_", "");
        } else {
            itemInfo = (T) DynamXObjectLoaders.ITEMS.addBuiltinObject(this, "dynx." + modid, itemName);
        }
        itemInfo.setModel(RegistryNameSetter.getDynamXModelResourceLocation(model));
        itemInfo.setDescription("Builtin " + modid + "'s item");

        RegistryNameSetter.setRegistryName(this, modid, itemInfo.getFullName().toLowerCase());
        setTranslationKey(itemInfo.getFullName().toLowerCase());
        setMaxStackSize(itemInfo.getMaxItemStackSize());
        DynamXItemRegistry.add(this);
    }

    public T getInfo() {
        return itemInfo;
    }

    public void setInfo(T itemInfo) {
        this.itemInfo = itemInfo;
    }

    @Override
    public String toString() {
        return "DynamXItem{" +
                "itemInfo=" + getInfo().getFullName() +
                '}';
    }

    @Override
    public String getJsonName(int meta) {
        return getInfo().getFullName().toLowerCase();
    }

    @Override
    public IObjPackObject getObjModel() {
        return getInfo();
    }

    @Override
    public boolean createJson() {
        return IResourcesOwner.super.createJson() || itemInfo.get3DItemRenderLocation() != Enum3DRenderLocation.ALL;
    }
}
