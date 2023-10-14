package fr.dynamx.common.contentpack.type.objects;

import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.events.CreatePackItemEvent;
import fr.dynamx.client.renders.model.renderer.ObjObjectRenderer;
import fr.dynamx.common.contentpack.loader.InfoList;
import fr.dynamx.common.items.DynamXItem;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ItemObject<T extends ItemObject<?>> extends AbstractItemObject<T, T> {
    /**
     * List of owned {@link ISubInfoType}s
     */
    protected final List<ISubInfoType<T>> subProperties = new ArrayList<>();

    @PackFileProperty(configNames = "MaxItemStackSize", required = false, defaultValue = "1")
    protected int maxItemStackSize = 1;

    public ItemObject(String packName, String fileName) {
        super(packName, fileName);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected IInfoOwner<T> createOwner(InfoList<T> loader) {
        CreatePackItemEvent.SimpleItem<T, ?> event = new CreatePackItemEvent.SimpleItem(loader, this);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isOverridden()) {
            return event.getObjectItem();
        } else {
            return new DynamXItem(this);
        }
    }

    @Override
    public void addSubProperty(ISubInfoType<T> property) {
        subProperties.add(property);
    }

    @Override
    public int getMaxItemStackSize() {
        return maxItemStackSize;
    }

    public void setMaxItemStackSize(int maxItemStackSize) {
        this.maxItemStackSize = maxItemStackSize;
    }

    @Override
    public List<ISubInfoType<T>> getSubProperties() {
        return subProperties;
    }

    @Override
    public String toString() {
        return "ItemObject named " + getFullName();
    }

    @Nullable
    @Override
    public IModelTextureVariants getTextureVariantsFor(ObjObjectRenderer objObjectRenderer) {
        // variants not supported on items
        return null;
    }
}
