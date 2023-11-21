package fr.dynamx.common.contentpack.type.objects;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXReflection;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractItemObject<T extends AbstractItemObject<?, ?>, A extends ISubInfoTypeOwner<?>> extends ObjectInfo<T>
        implements IModelPackObject, IPartContainer<A> {

    @Getter
    @Setter
    @PackFileProperty(configNames = {"CreativeTabName", "CreativeTab", "TabName"}, required = false, defaultValue = "CreativeTab of DynamX", description = "common.creativetabname")
    protected String creativeTabName;
    @Getter
    @Setter
    @PackFileProperty(configNames = "Model", type = DefinitionType.DynamXDefinitionTypes.DYNX_RESOURCE_LOCATION, description = "common.model", defaultValue = "obj/name_of_vehicle/name_of_model.obj")
    protected ResourceLocation model;
    @Getter
    @Setter
    @PackFileProperty(configNames = "ItemScale", required = false, description = "common.itemscale", defaultValue = "0.9")
    protected float itemScale = 0.9f;
    @Getter
    @PackFileProperty(configNames = "ItemTranslate", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0 0")
    protected Vector3f itemTranslate = new Vector3f(0, 0, 0);
    @Getter
    @PackFileProperty(configNames = "ItemRotate", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0 0")
    protected Vector3f itemRotate = new Vector3f(0, 0, 0);
    @Getter
    @Setter
    @PackFileProperty(configNames = "Item3DRenderLocation", required = false, description = "common.item3D", defaultValue = "all")
    protected Enum3DRenderLocation item3DRenderLocation = Enum3DRenderLocation.ALL;
    @Getter
    @Setter
    @PackFileProperty(configNames = "IconText", required = false, description = "common.icontext", defaultValue = "Block for blocks, Prop for props")
    protected String itemIcon;

    @Getter
    private final Map<Class<?>, Byte> partIds = new HashMap<>();
    private final List<BasePart<A>> parts = new ArrayList<>();
    /**
     * List of owned {@link ISubInfoType}s
     */
    @Getter
    private final List<ISubInfoType<A>> subProperties = new ArrayList<>();

    public AbstractItemObject(String packName, String fileName) {
        super(packName, fileName);
    }

    public CreativeTabs getCreativeTab(CreativeTabs defaultCreativeTab) {
        if (creativeTabName != null)
            return !creativeTabName.equalsIgnoreCase("None") ?
                    DynamXItemRegistry.creativeTabs.stream().filter(p -> DynamXReflection.getCreativeTabName(p).equals(creativeTabName)).findFirst().orElse(defaultCreativeTab) : null;
        return defaultCreativeTab;
    }

    @Override
    public String getTranslationKey(IDynamXItem<T> item, int itemMeta) {
        return "item." + DynamXConstants.ID + "." + super.getTranslationKey(item, itemMeta);
    }

    public int getMaxItemStackSize() {
        return 1;
    }

    @Override
    public List<BasePart<A>> getAllParts() {
        return parts;
    }

    @Override
    public void addPart(BasePart<A> part) {
        byte id = (byte) (partIds.getOrDefault(part.getIdClass(), (byte) -1) + 1);
        part.setId(id);
        partIds.put(part.getIdClass(), id);
        parts.add(part);
    }

    @Override
    public void addSubProperty(ISubInfoType<A> property) {
        subProperties.add(property);
    }

    @Override
    public boolean postLoad(boolean hot) {
        subProperties.forEach(subInfoType -> subInfoType.postLoad((A) this, hot));
        return super.postLoad(hot);
    }
}
