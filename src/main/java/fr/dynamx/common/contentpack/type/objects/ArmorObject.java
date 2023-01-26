package fr.dynamx.common.contentpack.type.objects;

import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.IPackFilePropertyFixer;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.obj.IModelTextureVariantsSupplier;
import fr.dynamx.client.renders.model.ModelObjArmor;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.common.contentpack.loader.ObjectLoader;
import fr.dynamx.common.contentpack.type.MaterialVariantsInfo;
import fr.dynamx.common.items.DynamXItemArmor;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Armor object, for "armor_" files
 */
public class ArmorObject<T extends ArmorObject<?>> extends AbstractItemObject<T, T> implements IModelTextureVariantsSupplier {
    @IPackFilePropertyFixer.PackFilePropertyFixer(registries = SubInfoTypeRegistries.ARMORS)
    public static final IPackFilePropertyFixer PROPERTY_FIXER = (object, key, value) -> {
        if ("Textures".equals(key))
            return new IPackFilePropertyFixer.FixResult("MaterialVariants", true, true);
        return null;
    };

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
    @Deprecated
    @PackFileProperty(configNames = "Textures", required = false, type = DefinitionType.DynamXDefinitionTypes.STRING_ARRAY_2D)
    protected String[][] texturesArray;

    @SideOnly(Side.CLIENT)
    protected ModelObjArmor objArmor;

    public ArmorObject(String packName, String fileName) {
        super(packName, fileName);
        this.itemScale = 0.7f; //default value
    }

    public void initArmorModel() {
        objArmor = new ModelObjArmor(this, DynamXContext.getObjModelRegistry().getModel(getModel()));
    }

    @SideOnly(Side.CLIENT)
    public ModelObjArmor getObjArmor() {
        return objArmor;
    }

    public MaterialVariantsInfo<?> getVariants() {
        return getSubPropertyByType(MaterialVariantsInfo.class);
    }

    @Override
    public IModelTextureVariants getTextureVariantsFor(ObjObjectRenderer objObjectRenderer) {
        return getVariants();
    }

    @Override
    public boolean hasVaryingTextures() {
        return getVariants() != null;
    }

    public int getMaxTextureMetadata() {
        return getVariants() != null ? getVariants().getVariantsMap().size() : 1;
    }

    @Override
    protected IInfoOwner<T> createOwner(ObjectLoader<T, ?> loader) {
        throw new IllegalArgumentException("Call createOwners !");
    }

    @Override
    public IInfoOwner<T>[] createOwners(ObjectLoader<T, ?> loader) {
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
        if (owners.isEmpty())
            DynamXErrorManager.addPackError(getPackName(), "armor_error", ErrorLevel.FATAL, getName(), "No configured items for this armor");
        this.owners = owners.toArray(new IInfoOwner[0]);
        return this.owners;
    }

    @Override
    public boolean postLoad(boolean hot) {
        if (hot && FMLCommonHandler.instance().getSide().isClient())
            initArmorModel();
        if (texturesArray != null)
            new MaterialVariantsInfo(this, texturesArray).appendTo(this);
        return super.postLoad(hot);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderItem3D(ItemStack item, ItemCameraTransforms.TransformType renderType) {
        EntityEquipmentSlot slot = ((DynamXItemArmor<?>) item.getItem()).armorType;
        getObjArmor().setActivePart(slot, getMaxTextureMetadata() > 1 ? (byte) item.getMetadata() : 0);
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
        if (itemMeta == 0 || getVariants() == null)
            return super.getTranslationKey(item, itemMeta) + "_" + slot.getName();
        return super.getTranslationKey(item, itemMeta) + "_" + slot.getName() + "_" + getVariants().getVariant((byte) itemMeta).getName();
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
        if (itemMeta == 0 || getVariants() == null)
            return prefix + " " + super.getTranslatedName(item, itemMeta);
        return prefix + " " + super.getTranslatedName(item, itemMeta) + "_" + getVariants().getVariant((byte) itemMeta).getName();
    }

    /**
     * List of owned {@link ISubInfoType}s
     */
    protected final List<ISubInfoType<T>> subProperties = new ArrayList<>();

    /**
     * Adds an {@link ISubInfoType}
     */
    public void addSubProperty(ISubInfoType<T> property) {
        subProperties.add(property);
    }

    /**
     * @return The list of owned {@link ISubInfoType}s
     */
    public List<ISubInfoType<T>> getSubProperties() {
        return subProperties;
    }

    @Override
    public String toString() {
        return "ArmorObject named " + getFullName();
    }
}
