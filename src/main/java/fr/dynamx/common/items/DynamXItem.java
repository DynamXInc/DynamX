package fr.dynamx.common.items;

import dz.betterlights.dynamx.LightCasterPartSync;
import dz.betterlights.lighting.lightcasters.EntityLightCaster;
import dz.betterlights.lighting.lightcasters.LightCaster;
import dz.betterlights.network.EnumPacketType;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.api.network.EnumPacketTarget;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.parts.LightObject;
import fr.dynamx.common.contentpack.parts.PartLightSource;
import fr.dynamx.common.contentpack.parts.lights.SpotLightObject;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.entities.modules.AbstractLightsModule;
import fr.dynamx.common.items.lights.ItemLightContainer;
import fr.dynamx.common.network.lights.PacketSyncItemInstanceUUID;
import fr.dynamx.common.network.lights.PacketSyncPartLights;
import fr.dynamx.common.objloader.data.DxModelData;
import fr.dynamx.common.objloader.data.ObjModelData;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXUtils;
import fr.dynamx.utils.RegistryNameSetter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DynamXItem<T extends AbstractItemObject<?, ?>> extends Item implements IDynamXItem<T>, IResourcesOwner {
    protected T itemInfo;

    public static Map<UUID, AbstractLightsModule.ItemLightsModule> itemInstanceLights = new HashMap<>();


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
     * @param model     The obj model of the block "namespace:resourceName.obj"
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
        if(worldIn.isRemote){
            return super.onItemRightClick(worldIn, playerIn, handIn);
        }
        if(!stack.hasTagCompound()){
            stack.setTagCompound(new NBTTagCompound());
        }
        DynamXItem<?> item = (DynamXItem<?>) stack.getItem();

        UUID instanceUUID = stack.getTagCompound().getUniqueId("InstanceUUID");
        if (!itemInstanceLights.containsKey(instanceUUID)) {
            UUID id = UUID.randomUUID();
            if(FMLCommonHandler.instance().getSide().isServer()) {
                DynamXContext.getNetwork().getVanillaNetwork().sendPacket(new PacketSyncItemInstanceUUID(id), EnumPacketTarget.ALL, null);
            }
            AbstractLightsModule.ItemLightsModule instanceLight = new AbstractLightsModule.ItemLightsModule(this, getInfo(), id);
            stack.getTagCompound().setUniqueId("InstanceUUID", id);
            itemInstanceLights.put(id, instanceLight);
        }
        if (!stack.getTagCompound().hasKey("LightLists")) {
            instanceUUID = stack.getTagCompound().getUniqueId("InstanceUUID");
            NBTTagList list = new NBTTagList();
            AbstractLightsModule.ItemLightsModule instanceLight = itemInstanceLights.get(instanceUUID);
            instanceLight.getLightCasterPartSyncs().keySet().forEach(lightCasterContainer -> {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setInteger("LightId", lightCasterContainer);
                list.appendTag(compound);
            });
            stack.getTagCompound().setTag("LightLists", list);
        } else {
            AbstractLightsModule.ItemLightsModule instanceLight = itemInstanceLights.get(instanceUUID);
            if(instanceLight == null || !stack.getTagCompound().hasKey("LightLists")){
                return new ActionResult<>(EnumActionResult.PASS, stack);
            }
            NBTTagList list = stack.getTagCompound().getTagList("LightLists", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound compound = list.getCompoundTagAt(i);
                int id = compound.getInteger("LightId");
                instanceLight.setLightOn(id, !instanceLight.isLightOn(id));
            }
        }

        return new ActionResult<>(EnumActionResult.PASS, stack);
    }


    @Nullable
    public static AbstractLightsModule.ItemLightsModule getLightContainer(ItemStack stack){
        if(!stack.hasTagCompound()){
            return null;
        }
        if(!stack.getTagCompound().hasUniqueId("InstanceUUID")){
            return null;
        }

        return itemInstanceLights.get(stack.getTagCompound().getUniqueId("InstanceUUID"));
    }
}
