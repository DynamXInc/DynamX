package fr.dynamx.common.items;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.capability.itemdata.DynamXItemData;
import fr.dynamx.common.capability.itemdata.DynamXItemDataProvider;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.entities.modules.AbstractLightsModule;
import fr.dynamx.common.network.lights.PacketSyncItemLight;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.RegistryNameSetter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.util.UUID;

public class DynamXItem<T extends AbstractItemObject<?, ?>> extends Item implements IDynamXItem<T>, IResourcesOwner {
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
     * @param model    The obj model of the block "namespace:resourceName.obj"
     */
    public DynamXItem(String modid, String itemName, ResourceLocation model) {
        if (modid.contains("builtin_mod_")) { //Backward-compatibility
            itemInfo = (T) DynamXObjectLoaders.ITEMS.addBuiltinObject(this, modid, itemName);
            modid = modid.replace("builtin_mod_", "");
        } else {
            itemInfo = (T) DynamXObjectLoaders.ITEMS.addBuiltinObject(this, "dynx." + modid, itemName);
        }
        itemInfo.setModel(model);
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
    public IModelPackObject getDxModel() {
        return getInfo();
    }

    @Override
    public boolean createJson() {
        return IResourcesOwner.super.createJson() || itemInfo.get3DItemRenderLocation() != Enum3DRenderLocation.ALL;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);

        if (worldIn.isRemote) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        DynamXItemData capability = stack.getCapability(DynamXItemDataProvider.DYNAMX_ITEM_DATA_CAPABILITY, null);
        if (capability == null) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        NBTTagCompound tagCompound = stack.getTagCompound();
        AbstractLightsModule.ItemLightsModule itemModule = capability.itemModule;
        boolean isServer = FMLCommonHandler.instance().getSide().isServer();

        if (itemModule == null) {
            UUID newId = UUID.randomUUID();
            itemModule = new AbstractLightsModule.ItemLightsModule(this, getInfo(), newId, playerIn);
            if (isServer) {
                DynamXContext.getNetwork().getVanillaNetwork().sendPacket(new PacketSyncItemLight(newId, playerIn), EnumPacketTarget.ALL, null);
            }
            tagCompound.setUniqueId("InstanceUUID", newId);
            DynamXItemData.itemInstanceLights.put(newId, itemModule);
            capability.itemModule = itemModule;
        }

        UUID uuid = tagCompound.getUniqueId("InstanceUUID");
        for (Integer id : itemModule.getLightCasterPartSyncs().keySet()) {
            itemModule.setLightOn(id, !itemModule.isLightOn(id));
            if (isServer) {
                DynamXContext.getNetwork().getVanillaNetwork().sendPacket(new PacketSyncItemLight(uuid, !itemModule.isLightOn(id)), EnumPacketTarget.ALL, null);
            }
        }

        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    @Override
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);
        if (worldIn.isRemote) {
            return;
        }
        DynamXItemData capability = stack.getCapability(DynamXItemDataProvider.DYNAMX_ITEM_DATA_CAPABILITY, null);
        if (capability == null) {
            return;
        }
        AbstractLightsModule.ItemLightsModule instanceLight = capability.itemModule;
        if (instanceLight == null) {
            return;
        }
        if (isSelected) {
            return;
        }
        DynamXItemData.setLightOn(instanceLight, false);
    }

    @Nullable
    public static AbstractLightsModule.ItemLightsModule getLightContainer(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return null;
        }
        if (!stack.getTagCompound().hasUniqueId("InstanceUUID")) {
            return null;
        }

        return DynamXItemData.itemInstanceLights.get(stack.getTagCompound().getUniqueId("InstanceUUID"));
    }
}
