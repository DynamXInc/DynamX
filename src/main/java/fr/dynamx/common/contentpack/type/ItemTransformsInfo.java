package fr.dynamx.common.contentpack.type;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores item transforms for different views ({@link net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType}s)
 */
@RegisteredSubInfoType(name = "ItemTransforms", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.ITEMS,
        SubInfoTypeRegistries.ARMORS, SubInfoTypeRegistries.BLOCKS, SubInfoTypeRegistries.HELICOPTER})
public class ItemTransformsInfo extends SubInfoType<AbstractItemObject<?, ?>> implements ISubInfoTypeOwner<ItemTransformsInfo> {
    private final Map<ItemCameraTransforms.TransformType, ViewTransformsInfo> viewTransforms = new HashMap<>();

    public ItemTransformsInfo(ISubInfoTypeOwner<AbstractItemObject<?, ?>> owner) {
        super(owner);
    }

    public ItemTransformsInfo(AbstractItemObject<?, ?> owner, float itemScale, Vector3f itemTranslate, Vector3f itemRotate) {
        this((ISubInfoTypeOwner<AbstractItemObject<?, ?>>) owner);
        for(ViewTransformsInfo.EnumViewType type : ViewTransformsInfo.EnumViewType.values()) {
            ViewTransformsInfo viewTransformsInfo = new ViewTransformsInfo(this, type, itemScale, itemTranslate, itemRotate);
            viewTransformsInfo.appendTo(this);
        }
    }

    @Override
    public String getName() {
        return "ItemTransformsInfo";
    }

    @Override
    public void appendTo(AbstractItemObject<?, ?> owner) {
        owner.setItemTransformsInfo(this);
    }

    @Override
    public void addSubProperty(ISubInfoType<ItemTransformsInfo> property) {
        throw new IllegalStateException("Cannot add sub property to a light");
    }

    @Override
    public List<ISubInfoType<ItemTransformsInfo>> getSubProperties() {
        return Collections.emptyList();
    }

    public void addViewTransforms(ItemCameraTransforms.TransformType viewType, ViewTransformsInfo viewTransformsInfo) {
        viewTransforms.put(viewType, viewTransformsInfo);
    }

    @Nullable
    public ViewTransformsInfo getViewTransforms(ItemCameraTransforms.TransformType viewType) {
        return viewTransforms.get(viewType);
    }
}
