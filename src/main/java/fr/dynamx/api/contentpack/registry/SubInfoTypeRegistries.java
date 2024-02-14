package fr.dynamx.api.contentpack.registry;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.common.contentpack.loader.InfoList;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.contentpack.type.objects.ItemObject;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.contentpack.type.vehicle.BaseEngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.CarEngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import lombok.Getter;

public enum SubInfoTypeRegistries {
    PACKS(DynamXObjectLoaders.PACKS, PackInfo.class),
    WHEELED_VEHICLES(DynamXObjectLoaders.WHEELED_VEHICLES, ModularVehicleInfo.class),
    TRAILERS(DynamXObjectLoaders.TRAILERS, ModularVehicleInfo.class),
    BOATS(DynamXObjectLoaders.BOATS, ModularVehicleInfo.class),
    HELICOPTER(DynamXObjectLoaders.HELICOPTERS, ModularVehicleInfo.class),
    ITEMS(DynamXObjectLoaders.ITEMS, ItemObject.class),
    ARMORS(DynamXObjectLoaders.ARMORS, ArmorObject.class),
    BLOCKS(DynamXObjectLoaders.BLOCKS, BlockObject.class),
    PROPS(DynamXObjectLoaders.PROPS, PropObject.class),
    WHEELS(DynamXObjectLoaders.WHEELS, PartWheelInfo.class),
    CAR_ENGINES(DynamXObjectLoaders.ENGINES, CarEngineInfo.class),
    HELICOPTER_ENGINES(DynamXObjectLoaders.ENGINES, BaseEngineInfo.class);

    @Getter
    private final InfoList<?> infoList;
    @Getter
    private final Class<? extends ISubInfoTypeOwner<?>> infoOwnerType;

    SubInfoTypeRegistries(InfoList<?> infoList, Class<?> infoOwnerType) {
        if (!ISubInfoTypeOwner.class.isAssignableFrom(infoOwnerType))
            throw new IllegalArgumentException(infoOwnerType + " does not implements ISubInfoTypeOwner !");
        this.infoList = infoList;
        this.infoOwnerType = (Class<ISubInfoTypeOwner<?>>) infoOwnerType;
    }

    static {
        for (SubInfoTypeRegistries value : values()) {
            DynamXObjectLoaders.getInfoLists().add(value.getInfoList());
        }
    }
}
