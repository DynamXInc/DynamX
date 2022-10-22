package fr.dynamx.api.events;

import fr.dynamx.api.contentpack.object.IInfoOwner;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfoBuilder;
import fr.dynamx.common.contentpack.loader.ObjectLoader;
import fr.dynamx.common.contentpack.type.ObjectInfo;
import fr.dynamx.common.contentpack.type.objects.AbstractItemObject;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.contentpack.type.objects.ItemObject;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.items.DynamXItem;
import fr.dynamx.common.items.ItemModularEntity;
import fr.dynamx.common.items.ItemProps;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nullable;

/**
 * @see CreatePackItemEvent
 */
public abstract class CreatePackItemEvent<B extends ObjectInfo<?>, C extends IInfoOwner<?>> extends Event {
    /**
     *  The loader of this object, depends on the type of the object (item, block, armor, trailer, wheeled vehicle, moto...)
     */
    @Getter
    private final ObjectLoader<B, C, ?> loader;
    /**
     * The {@link ObjectInfo} of the item to create
     */
    @Getter
    private final B objectInfo;
    @Nullable
    @Getter
    @Setter
    private C spawnItem;

    public CreatePackItemEvent(ObjectLoader<B, C, ?> loader, B objectInfo) {
        this.loader = loader;
        this.objectInfo = objectInfo;
    }

    /**
     * @return True if the spawn item has been replaced
     */
    public boolean isOverridden() {
        return spawnItem != null;
    }

    /**
     * Called by the ContentPackSystem when creating the item of a {@link ModularVehicleInfo} <br>
     * Set your own item to prevent default behavior <br>
     * You can cancel the event to avoid other addons to modify your behavior
     */
    @Cancelable
    public static class CreateVehicleItemEvent<B extends ObjectInfo<?>, C extends IInfoOwner<?>> extends CreatePackItemEvent<B,C> {
        public CreateVehicleItemEvent(ObjectLoader<B,C ,ModularVehicleInfoBuilder> loader, B objectInfo) {
            super(loader, objectInfo);
        }
    }

    /**
     * Called by the ContentPackSystem when creating the item of a {@link AbstractItemObject} <br>
     * Set your own item to prevent default behavior <br>
     * You can cancel the event to avoid other addons to modify your behavior
     */
    @Cancelable
    public static class CreateSimpleItemEvent<B extends ObjectInfo<?>, C extends IInfoOwner<?>> extends CreatePackItemEvent<B,C> {
        public CreateSimpleItemEvent(ObjectLoader<B,C, ISubInfoTypeOwner.Empty> loader, B objectInfo) {
            super(loader, objectInfo);
        }
    }

    /**
     * Called by the ContentPackSystem when creating the block of a {@link fr.dynamx.common.contentpack.type.objects.BlockObject} <br>
     * Set your own item to prevent default behavior <br>
     * You can cancel the event to avoid other addons to modify your behavior
     */
    @Cancelable
    public static class CreateSimpleBlockEvent extends CreatePackItemEvent<BlockObject<?>, DynamXBlock<BlockObject<?>>> {
        public CreateSimpleBlockEvent(ObjectLoader<BlockObject<?>, DynamXBlock<BlockObject<?>>, BlockObject<?>> loader, BlockObject<?> objectInfo) {
            super(loader, objectInfo);
        }
    }

    /**
     * Called by the ContentPackSystem when creating the item of a {@link PropObject} <br>
     * Set your own item to prevent default behavior <br>
     * You can cancel the event to avoid other addons to modify your behavior
     */
    @Cancelable
    public static class CreatePropsItemEvent<B extends ObjectInfo<?>, C extends IInfoOwner<?>> extends CreatePackItemEvent<B,C> {

        public CreatePropsItemEvent(ObjectLoader<B, C, ?> loader, B objectInfo) {
            super(loader, objectInfo);
        }
    }
}