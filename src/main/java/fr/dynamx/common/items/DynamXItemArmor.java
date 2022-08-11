package fr.dynamx.common.items;

import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.render.IObjPackObject;
import fr.dynamx.api.contentpack.object.render.IResourcesOwner;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.RegistryNameSetter;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class DynamXItemArmor<T extends ArmorObject<?>> extends ItemArmor implements IInfoOwner<T>, IResourcesOwner {
    protected final int textureNum;
    protected T armorInfo;

    public DynamXItemArmor(T armorInfo, ArmorMaterial material, EntityEquipmentSlot armorType) {
        super(material, 0, armorType);
        this.armorInfo = armorInfo;
        RegistryNameSetter.setRegistryName(this, DynamXConstants.ID, armorInfo.getFullName().toLowerCase() + "_" + armorType.getName());
        setTranslationKey(DynamXConstants.ID + "." + armorInfo.getFullName().toLowerCase() + "_" + armorType.getName());
        setCreativeTab(armorInfo.getCreativeTab(DynamXItemRegistry.objectTab));
        DynamXItemRegistry.add(this);
        textureNum = armorInfo.getMaxTextureMetadata();
        if (textureNum > 1) {
            setHasSubtypes(true);
            setMaxDamage(0);
        }
    }

    /**
     * Use this constructor to create a custom armor item having the same functionalities as pack armor <br>
     * You can customise custom properties using this.getInfo() <br> <br>
     * NOTE : Registry name and translation key are automatically set and the item is automatically registered into Forge by DynamX,
     * but don't forget to set a creative tab !<br><br>
     *
     * <strong>NOTE : Should be called during addons initialization</strong>
     *
     * @param modid     The mod owning this item used to register the item
     * @param itemName  The name of the item
     * @param model     The obj model of the item, must be under "dynamxmod:models/<model>"
     * @param material  The armor material
     * @param armorType The armor type
     */
    public DynamXItemArmor(String modid, String itemName, ResourceLocation model, ArmorMaterial material, EntityEquipmentSlot armorType) {
        super(material, 0, armorType);
        if (modid.contains("builtin_mod_")) { //Backward-compatibility
            this.armorInfo = (T) DynamXObjectLoaders.ARMORS.addBuiltinObject(this, modid, itemName);
            modid = modid.replace("builtin_mod_", "");
        } else {
            this.armorInfo = (T) DynamXObjectLoaders.ARMORS.addBuiltinObject(this, "dynx." + modid, itemName);
        }
        this.armorInfo.setModel(model);
        armorInfo.setDescription("Builtin " + modid + "'s armor");
        this.textureNum = 1;

        RegistryNameSetter.setRegistryName(this, modid, armorInfo.getFullName().toLowerCase());
        setTranslationKey(armorInfo.getFullName().toLowerCase());
        DynamXItemRegistry.add(this);
    }


    public T getInfo() {
        return armorInfo;
    }

    public void setInfo(T itemInfo) {
        this.armorInfo = itemInfo;
    }

    @Override
    public String getJsonName(int meta) {
        return getInfo().getFullName().toLowerCase() + "_" + armorType.getName() + "_" + getInfo().getTexturesFor(null).get((byte) meta).getName().toLowerCase();
    }

    @Override
    public boolean createJson() {
        return getObjModel().get3DItemRenderLocation() != Enum3DRenderLocation.ALL;
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            for (byte m = 0; m < textureNum; m++) {
                items.add(new ItemStack(this, 1, m));
            }
        }
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        if (stack.getMetadata() != 0 && textureNum > 1) {
            return super.getTranslationKey(stack) + "_" + getInfo().getTexturesFor(null).get((byte) stack.getMetadata()).getName().toLowerCase();
        }
        return super.getTranslationKey(stack);
    }

    @Override
    public int getMaxMeta() {
        return textureNum;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("Description: " + getInfo().getDescription());
        tooltip.add("Pack: " + getInfo().getPackName());
        if (stack.getMetadata() > 0 && textureNum > 1)
            tooltip.add("Texture: " + getInfo().getTexturesFor(null).get((byte) stack.getMetadata()).getName());
        super.addInformation(stack, worldIn, tooltip, flagIn);
        //tooltip.add("Armor slot : "+armorType.getName());
    }

    @Nullable
    @Override
    @SideOnly(Side.CLIENT)
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        return super.getArmorTexture(stack, entity, slot, type);
    }

    @Nullable
    @Override
    @SideOnly(Side.CLIENT)
    public ModelBiped getArmorModel(EntityLivingBase entityLiving, ItemStack itemStack, EntityEquipmentSlot armorSlot, ModelBiped _default) {
        armorInfo.getObjArmor().setActivePart(armorSlot, textureNum > 1 ? (byte) itemStack.getMetadata() : 0);
        //System.out.println("Je te tiens "+armorSlot+" "+armorInfo.getObjArmor()+" "+armorInfo.getObjArmor().bipedBody);
        return armorInfo.getObjArmor();
    }

    @Override
    public IObjPackObject getObjModel() {
        return getInfo();
    }
}
