package fr.dynamx.common.contentpack.type.objects;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.obj.IModelTextureSupplier;
import fr.dynamx.api.obj.IObjObject;
import fr.dynamx.client.renders.model.ModelObjArmor;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.loader.ObjectLoader;
import fr.dynamx.common.items.DynamXItemArmor;
import fr.dynamx.common.obj.texture.TextureData;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Armor object, for "armor_" files
 */
public class ArmorObject<T extends ArmorObject<T>> extends AbstractItemObject<T> implements IModelTextureSupplier, ISubInfoTypeOwner<ArmorObject<?>> {
    @Getter
    @PackFileProperty(configNames = "ArmorHead", required = false)
    protected String armorHead;
    @Getter
    @PackFileProperty(configNames = "ArmorBody", required = false)
    protected String armorBody;
    @Getter
    @PackFileProperty(configNames = "ArmorArms", required = false)
    protected String[] armorArms;
    @Getter
    @PackFileProperty(configNames = "ArmorLegs", required = false)
    protected String[] armorLegs;
    @Getter
    @PackFileProperty(configNames = "ArmorFoot", required = false)
    protected String[] armorFoot;
    @Getter
    @PackFileProperty(configNames = "Durability", required = false, defaultValue = "5")
    protected int durability = 5; //Leather value
    @Getter
    @PackFileProperty(configNames = "Enchantability", required = false, defaultValue = "15")
    protected int enchantibility = 15; //Leather value
    @Getter
    @PackFileProperty(configNames = "EquipSound", required = false, defaultValue = "item.armor.equip_leather")
    protected SoundEvent sound = SoundEvent.REGISTRY.getObject(new ResourceLocation("item.armor.equip_leather"));
    @Getter
    @PackFileProperty(configNames = "Toughness", required = false, defaultValue = "0")
    protected float toughness = 0; //Leather value
    @Getter
    @PackFileProperty(configNames = "DamageReduction", required = false, defaultValue = "\"1 2 3 1\" (leather)")
    protected int[] reductionAmount = new int[]{1, 2, 3, 1};
    @Getter
    @PackFileProperty(configNames = "Textures", required = false, type = DefinitionType.DynamXDefinitionTypes.STRING_ARRAY_2D)
    protected String[][] texturesArray;

    @SideOnly(Side.CLIENT)
    protected ModelObjArmor objArmor;

    public ArmorObject(String packName, String fileName) {
        super(packName, fileName);
        this.itemScale = 0.7f; //default value
        if (FMLCommonHandler.instance().getSide().isClient())
            objArmor = new ModelObjArmor(this);
    }

    @SideOnly(Side.CLIENT)
    public ModelObjArmor getObjArmor() {
        return objArmor;
    }

    private final Map<Byte, TextureData> textures = new HashMap<>();
    private int maxTextureMetadata;

    @Nullable
    @Override
    public Map<Byte, TextureData> getTexturesFor(IObjObject object) {
        return textures;
    }

    @Override
    public boolean hasCustomTextures() {
        return textures.size() > 1;
    }

    @Override
    protected IInfoOwner<T> createOwner(ObjectLoader<T, ?, ?> loader) {
        throw new IllegalArgumentException("Call createOwners !");
    }

    @Override
    public IInfoOwner<T>[] createOwners(ObjectLoader<T, ?, ?> loader) {
        ItemArmor.ArmorMaterial material = EnumHelper.addArmorMaterial(getFullName(), "", durability, reductionAmount, enchantibility, sound, toughness);
        List<IInfoOwner<T>> owners = new ArrayList<>();
        if (getArmorHead() != null)
            owners.add(new DynamXItemArmor(this, material, EntityEquipmentSlot.HEAD));
        if (getArmorBody() != null || getArmorArms() != null)
            owners.add(new DynamXItemArmor(this, material, EntityEquipmentSlot.CHEST));
        if (getArmorLegs() != null)
            owners.add(new DynamXItemArmor(this, material, EntityEquipmentSlot.LEGS));
        if (getArmorFoot() != null)
            owners.add(new DynamXItemArmor(this, material, EntityEquipmentSlot.FEET));
        if (owners.isEmpty()) {
            //DynamXMain.log.error("Armor " + getFullName() + " has no configured items !");
            DynamXErrorManager.addError(getPackName(), "armor_error", ErrorLevel.FATAL, getName(), "No configured items for this armor");
        }
        this.owners = owners.toArray(new IInfoOwner[0]);
        return this.owners;
    }

    @Override
    public void onComplete(boolean hotReload) {
        if (hotReload && FMLCommonHandler.instance().getSide().isClient())
            getObjArmor().init(DynamXContext.getObjModelRegistry().getModel(getModel()));

        textures.clear();
        textures.put((byte) 0, new TextureData("Default", (byte) 0, getName()));
        if (texturesArray != null) {
            byte id = 1;
            for (String[] info : texturesArray) {
                textures.put(id, new TextureData(info[0], id, info[1] == null ? "dummy" : info[1]));
                id++;
            }
        }
        int texCount = 0;
        for (TextureData data : textures.values()) {
            if (data.isItem())
                texCount++;
        }
        this.maxTextureMetadata = texCount;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderItem3D(ItemStack item, ItemCameraTransforms.TransformType renderType) {
        EntityEquipmentSlot slot = ((DynamXItemArmor<?>) item.getItem()).armorType;
        getObjArmor().setActivePart(slot, maxTextureMetadata > 1 ? (byte) item.getMetadata() : 0);
        //restore default rotations (contained in ModelBiped)
        getObjArmor().setModelAttributes(getObjArmor());
        if (renderType != ItemCameraTransforms.TransformType.GUI)
            GlStateManager.rotate(90, 1, 0, 0);
        switch (slot) {
            case FEET:
                GlStateManager.translate(0, 1.8, -0.15);
                break;
            case LEGS:
                GlStateManager.translate(0, 1.5, -0.15);
                break;
            case CHEST:
                GlStateManager.translate(0, 0.7, -0.15);
                break;
            case HEAD:
                GlStateManager.translate(0, 0.2, -0.15);
                break;
        }
        GlStateManager.rotate(180, 0, 0, 1);
        getObjArmor().render(null, 0, 0, 0, 0, 0, 1);
    }

    @Override
    public String getTranslationKey(IInfoOwner<T> item, int itemMeta) {
        EntityEquipmentSlot slot = ((DynamXItemArmor<T>) item).armorType;
        if (itemMeta == 0)
            return super.getTranslationKey(item, itemMeta) + "_" + slot.getName();
        TextureData textureInfo = textures.get((byte) itemMeta);
        return super.getTranslationKey(item, itemMeta) + "_" + slot.getName() + "_" + textureInfo.getName().toLowerCase();
    }

    @Override
    public String getTranslatedName(IInfoOwner<T> item, int itemMeta) {
        String prefix = "";
        EntityEquipmentSlot slot = ((DynamXItemArmor<T>) item).armorType;
        switch (slot) {
            case FEET:
                prefix = "Chaussures de";
                break;
            case LEGS:
                prefix = "Pantalon de";
                break;
            case CHEST:
                prefix = "T-shirt de";
                break;
            case HEAD:
                prefix = "Casque de";
                break;
        }
        if (itemMeta == 0)
            return prefix + " " + super.getTranslatedName(item, itemMeta);
        TextureData textureInfo = textures.get((byte) itemMeta);
        return prefix + " " + super.getTranslatedName(item, itemMeta) + " " + textureInfo.getName();
    }

    public int getMaxTextureMetadata() {
        return maxTextureMetadata;
    }

    /**
     * List of owned {@link ISubInfoType}s
     */
    protected final List<ISubInfoType<ArmorObject<?>>> subProperties = new ArrayList<>();

    /**
     * Adds an {@link ISubInfoType}
     */
    public void addSubProperty(ISubInfoType<ArmorObject<?>> property) {
        subProperties.add(property);
    }

    /**
     * @return The list of owned {@link ISubInfoType}s
     */
    public List<ISubInfoType<ArmorObject<?>>> getSubProperties() {
        return subProperties;
    }

    @Override
    public String toString() {
        return "ArmorObject named " + getFullName();
    }
}
