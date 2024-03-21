package fr.dynamx.common.contentpack.type.objects;

import com.jme3.math.Vector3f;
import fr.aym.acslib.api.services.error.ErrorLevel;
import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.IPartContainer;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.object.part.IDrawablePart;
import fr.dynamx.api.contentpack.object.render.Enum3DRenderLocation;
import fr.dynamx.api.contentpack.object.render.IModelPackObject;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.common.contentpack.type.ItemTransformsInfo;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import fr.dynamx.common.contentpack.type.ViewTransformsInfo;
import fr.dynamx.common.items.DynamXItemRegistry;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.DynamXReflection;
import fr.dynamx.utils.errors.DynamXErrorManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.*;

public abstract class AbstractItemObject<T extends AbstractItemObject<?, ?>, A extends ISubInfoTypeOwner<?>> extends ObjectInfo<T>
        implements IModelPackObject, IPartContainer<A>
{
    @Getter
    @Setter
    @PackFileProperty(configNames = {"CreativeTabName", "CreativeTab", "TabName"}, required = false, defaultValue = "CreativeTab of DynamX", description = "common.creativetabname")
    protected String creativeTabName;
    @Getter
    @Setter
    @PackFileProperty(configNames = "Model", type = DefinitionType.DynamXDefinitionTypes.DYNX_RESOURCE_LOCATION, description = "common.model", defaultValue = "obj/name_of_vehicle/name_of_model.obj")
    protected ResourceLocation model;

    /**
     * @deprecated Replaced by {@link fr.dynamx.common.contentpack.type.ViewTransformsInfo}
     */
    @Getter
    @PackFileProperty(configNames = "ItemScale", required = false, description = "common.itemscale", defaultValue = "0.9")
    protected float itemScale = getBaseItemScale();
    /**
     * @deprecated Replaced by {@link fr.dynamx.common.contentpack.type.ViewTransformsInfo}
     */
    @Getter
    @Deprecated
    @PackFileProperty(configNames = "ItemTranslate", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0 0")
    protected Vector3f itemTranslate = null;
    /**
     * @deprecated Replaced by {@link fr.dynamx.common.contentpack.type.ViewTransformsInfo}
     */
    @Getter
    @Deprecated
    @PackFileProperty(configNames = "ItemRotate", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F, required = false, defaultValue = "0 0 0")
    protected Vector3f itemRotate = null;

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

    @Getter
    private final List<IDrawablePart<?>> drawableParts = new ArrayList<>();

    /**
     * The list of all rendered parts for this object <br>
     * A rendered part will not be rendered with the main part of the 3D model (see {@link fr.dynamx.client.renders.model.renderer.DxModelRenderer#renderDefaultParts(byte, boolean)}) <br>
     * The {@link BasePart}s using rendered parts are responsible to render the part at the right location (by implementing {@link IDrawablePart})
     */
    @Getter
    private final List<String> renderedParts = new ArrayList<>();

    @Getter
    @Setter
    private ItemTransformsInfo itemTransformsInfo;

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
        if (part instanceof IDrawablePart)
            addDrawablePart((IDrawablePart<?>) part);
    }

    @Override
    public void addSubProperty(ISubInfoType<A> property) {
        subProperties.add(property);
        if (property instanceof IDrawablePart)
            addDrawablePart((IDrawablePart<?>) property);
    }

    @Override
    public boolean postLoad(boolean hot) {
        if(FMLCommonHandler.instance().getSide().isClient() && (itemScale != getBaseItemScale() || itemTranslate != null || itemRotate != null)) {
            if(itemTransformsInfo != null) {
                DynamXErrorManager.addPackError(getPackName(), "mixed_item_transforms_info", ErrorLevel.HIGH, getName(), "You can't mix old item transforms and ItemTransforms block !");
            } else {
                itemTransformsInfo = new ItemTransformsInfo(this, itemScale, itemTranslate, itemRotate);
            }
        }
        subProperties.forEach(subInfoType -> subInfoType.postLoad((A) this, hot));
        parts.forEach(part -> part.postLoad((A) this, hot));
        // Build the scene graph
        if (FMLCommonHandler.instance().getSide().isClient()) {
            getSceneGraph();
        }
        return super.postLoad(hot);
    }

    @Override
    public Enum3DRenderLocation get3DItemRenderLocation() {
        return item3DRenderLocation;
    }

    /**
     * Adds a {@link IDrawablePart} to this object <br>
     * The part will not be rendered with the main parts of the 3D model (see {@link fr.dynamx.client.renders.model.renderer.DxModelRenderer#renderDefaultParts(byte, boolean)})
     *
     * @param part The part to add
     */
    public void addDrawablePart(IDrawablePart<?> part) {
        String[] names = part.getRenderedParts();
        if (names.length > 0)
            renderedParts.addAll(Arrays.asList(names));
        drawableParts.add(part);
    }

    @Override
    public boolean canRenderPart(String partName) {
        return !renderedParts.contains(partName);
    }

    @Override
    public ViewTransformsInfo getViewTransformsInfo(ItemCameraTransforms.TransformType viewType) {
        return itemTransformsInfo != null ? itemTransformsInfo.getViewTransforms(viewType) : null;
    }

    /**
     * @return The default value for itemScale (applied to the item when getViewTransformsInfo returns null)
     */
    public float getBaseItemScale() {
        return 0.9f;
    }
}
