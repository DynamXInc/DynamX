package fr.dynamx.api.contentpack.registry;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.common.contentpack.loader.InfoLoader;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfoBuilder;
import fr.dynamx.common.contentpack.type.objects.*;
import fr.dynamx.common.contentpack.type.vehicle.EngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;

public enum SubInfoTypeRegistries {
    PACKS(DynamXObjectLoaders.PACKS, PackInfo.class),
    WHEELED_VEHICLES(DynamXObjectLoaders.WHEELED_VEHICLES, ModularVehicleInfoBuilder.class),
    TRAILERS(DynamXObjectLoaders.TRAILERS, ModularVehicleInfoBuilder.class),
    BOATS(DynamXObjectLoaders.BOATS, ModularVehicleInfoBuilder.class),
    ITEMS(DynamXObjectLoaders.ITEMS, ItemObject.class),
    ARMORS(DynamXObjectLoaders.ARMORS, ArmorObject.class),
    BLOCKS_AND_PROPS(DynamXObjectLoaders.BLOCKS, BlockObject.class),
    PROPS(DynamXObjectLoaders.PROPS, PropObject.class),
    WHEELS(DynamXObjectLoaders.WHEELS, PartWheelInfo.class),
    ENGINES(DynamXObjectLoaders.ENGINES, EngineInfo.class);

    private final InfoLoader<?, ?> infoLoader;
    private final Class<? extends ISubInfoTypeOwner<?>> infoOwnerType;

    SubInfoTypeRegistries(InfoLoader<?, ?> infoLoader, Class<?> infoOwnerType) {
        if (!ISubInfoTypeOwner.class.isAssignableFrom(infoOwnerType))
            throw new IllegalArgumentException(infoOwnerType + " does not implements ISubInfoTypeOwner !");
        this.infoLoader = infoLoader;
        this.infoOwnerType = (Class<ISubInfoTypeOwner<?>>) infoOwnerType;
    }

    public InfoLoader<?, ?> getInfoLoader() {
        return infoLoader;
    }

    public Class<? extends ISubInfoTypeOwner<?>> getInfoOwnerType() {
        return infoOwnerType;
    }

    static {
        for (SubInfoTypeRegistries value : values()) {
            DynamXObjectLoaders.getLoaders().add(value.getInfoLoader());
        }
    }
}
