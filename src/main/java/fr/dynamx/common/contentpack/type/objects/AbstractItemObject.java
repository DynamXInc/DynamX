package fr.dynamx.common.contentpack.type.objects;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.render.IObjPackObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXReflection;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractItemObject<T extends AbstractItemObject<?, ?>, A extends ISubInfoTypeOwner<?>> extends ObjectInfo<T>
        implements IObjPackObject, IPartContainer<A> {

    @PackFileProperty(configNames = {"CreativeTabName", "CreativeTab", "TabName"}, required = false, defaultValue = "CreativeTab of DynamX", description = "common.creativetabname")
    protected String creativeTabName;
    @PackFileProperty(configNames = "Model", type = DefinitionType.DynamXDefinitionTypes.DYNX_RESOURCE_LOCATION, description = "common.model")
    protected ResourceLocation model;
    @PackFileProperty(configNames = "ItemScale", required = false, description = "common.itemscale", defaultValue = "0.9")
    protected float itemScale = 0.9f;
    @PackFileProperty(configNames = "ItemTranslate", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0 0")
    protected Vector3f itemTranslate = new Vector3f(0, 0, 0);
    @PackFileProperty(configNames = "ItemRotate", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0 0")
    protected Vector3f itemRotate = new Vector3f(0, 0, 0);
    @PackFileProperty(configNames = "Item3DRenderLocation", required = false, description = "common.item3D", defaultValue = "all")
    protected Enum3DRenderLocation item3DRenderLocation = Enum3DRenderLocation.ALL;
    @PackFileProperty(configNames = "IconText", required = false, description = "common.icontext", defaultValue = "Block for blocks, Prop for props")
    protected String itemIcon;

    private final List<BasePart<A>> parts = new ArrayList<>();
    private final List<ISubInfoType<A>> subProperties = new ArrayList<>();

    public AbstractItemObject(String packName, String fileName) {
        super(packName, fileName);
    }

    public String getCreativeTabName() {
        return creativeTabName;
    }

    public CreativeTabs getCreativeTab(CreativeTabs defaultCreativeTab) {
        if (creativeTabName != null)
            return !creativeTabName.equalsIgnoreCase("None") ?
                    DynamXItemRegistry.creativeTabs.stream().filter(p -> DynamXReflection.getCreativeTabName(p).equals(creativeTabName)).findFirst().orElse(defaultCreativeTab) : null;
        return defaultCreativeTab;
    }

    @Override
    public ResourceLocation getModel() {
        return model;
    }

    public void setModel(ResourceLocation model) {
        this.model = model;
    }

    public Vector3f getItemTranslate() {
        return itemTranslate;
    }

    public Vector3f getItemRotate() {
        return itemRotate;
    }

    @Override
    public float getItemScale() {
        return itemScale;
    }

    public void setItemScale(float itemScale) {
        this.itemScale = itemScale;
    }

    @Override
    public Enum3DRenderLocation get3DItemRenderLocation() {
        return item3DRenderLocation;
    }

    public void setItem3DRenderLocation(Enum3DRenderLocation item3DRenderLocation) {
        this.item3DRenderLocation = item3DRenderLocation;
    }

    @Override
    public String getTranslationKey(IInfoOwner<T> item, int itemMeta) {
        return "item." + DynamXConstants.ID + "." + super.getTranslationKey(item, itemMeta);
    }

    public void setItemIcon(String itemIcon) {
        this.itemIcon = itemIcon;
    }

    @Nullable
    @Override
    public String getItemIcon() {
        return itemIcon;
    }

    public int getMaxItemStackSize() {
        return 1;
    }

    @Override
    public List<BasePart<A>> getAllParts() {
        return parts;
    }

    @Override
    public void addPart(BasePart<A> tBasePart) {
        parts.add(tBasePart);
    }

    @Override
    public void addSubProperty(ISubInfoType<A> property) {
        subProperties.add(property);
    }

    @Override
    public List<ISubInfoType<A>> getSubProperties() {
        return subProperties;
    }
}
