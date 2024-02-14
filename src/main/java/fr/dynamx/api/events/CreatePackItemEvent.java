package fr.dynamx.api.events;

import fr.dynamx.api.contentpack.object.IDynamXItem;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.common.contentpack.loader.InfoList;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nullable;

/**
 * @see CreatePackItemEvent
 */
public abstract class CreatePackItemEvent<B extends ObjectInfo<?> & ISubInfoTypeOwner<?>, C extends IDynamXItem<B>> extends Event {
    /**
     *  The loader of this object, depends on the type of the object (item, block, armor, trailer, wheeled vehicle, moto...)
     */
    @Getter
    private final InfoList<B> loader;
    /**
     * The {@link ObjectInfo} of the item to create
     */
    @Getter
    private final B objectInfo;
    /**
     * The item to use, set it to override the default behavior
     */
    @Getter
    @Setter
    @Nullable
    private C objectItem;

    public CreatePackItemEvent(InfoList<B> loader, B objectInfo) {
        this.loader = loader;
        this.objectInfo = objectInfo;
    }

    /**
     * @return True if the spawn item has been replaced
     */
    public boolean isOverridden() {
        return objectItem != null;
    }

    /**
     * Called by the ContentPackSystem when creating the item of a {@link ModularVehicleInfo} <br>
     * Set your own item to prevent default behavior <br>
     * You can cancel the event to avoid other addons to modify your behavior
     */
    @Cancelable
    public static class VehicleItem<B extends ObjectInfo<?> & ISubInfoTypeOwner<?>, C extends IDynamXItem<B>> extends CreatePackItemEvent<B,C> {
        public VehicleItem(InfoList<B> loader, B objectInfo) {
            super(loader, objectInfo);
        }
    }

    /**
     * Called by the ContentPackSystem when creating the item of a {@link AbstractItemObject} <br>
     * Set your own item to prevent default behavior <br>
     * You can cancel the event to avoid other addons to modify your behavior
     */
    @Cancelable
    public static class SimpleItem<B extends ObjectInfo<?> & ISubInfoTypeOwner<?>, C extends IDynamXItem<B>> extends CreatePackItemEvent<B,C> {
        public SimpleItem(InfoList<B> loader, B objectInfo) {
            super(loader, objectInfo);
        }
    }

    /**
     * Called by the ContentPackSystem when creating the block of a {@link fr.dynamx.common.contentpack.type.objects.BlockObject} <br>
     * Set your own item to prevent default behavior <br>
     * You can cancel the event to avoid other addons to modify your behavior
     */
    @Cancelable
    public static class SimpleBlock<B extends ObjectInfo<?> & ISubInfoTypeOwner<?>, C extends IDynamXItem<B>> extends CreatePackItemEvent<B, C> {
        public SimpleBlock(InfoList<B> loader, B objectInfo) {
            super(loader, objectInfo);
        }
    }

    /**
     * Called by the ContentPackSystem when creating the item of a {@link PropObject} <br>
     * Set your own item to prevent default behavior <br>
     * You can cancel the event to avoid other addons to modify your behavior
     */
    @Cancelable
    public static class PropsItem<B extends ObjectInfo<?> & ISubInfoTypeOwner<?>, C extends IDynamXItem<B>> extends CreatePackItemEvent<B,C> {

        public PropsItem(InfoList<B> loader, B objectInfo) {
            super(loader, objectInfo);
        }
    }
}