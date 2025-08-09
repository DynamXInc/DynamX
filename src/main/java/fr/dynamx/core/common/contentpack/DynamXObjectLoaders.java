package fr.dynamx.core.common.contentpack;

import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.core.common.blocks.DynamXBlock;
import fr.dynamx.core.common.contentpack.loader.*;
import fr.dynamx.core.common.contentpack.type.objects.ArmorObject;
import fr.dynamx.core.common.contentpack.type.objects.BlockObject;
import fr.dynamx.core.common.contentpack.type.objects.ItemObject;
import fr.dynamx.core.common.contentpack.type.objects.PropObject;
import fr.dynamx.core.common.contentpack.type.vehicle.*;
import fr.dynamx.core.common.items.DynamXItem;
import fr.dynamx.core.common.items.DynamXItemArmor;
import fr.dynamx.core.common.items.vehicle.ItemBoat;
import fr.dynamx.core.common.items.vehicle.ItemCar;
import fr.dynamx.core.common.items.vehicle.ItemHelicopter;
import fr.dynamx.core.common.items.vehicle.ItemTrailer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * All DynamX {@link InfoLoader}s
 */
public class DynamXObjectLoaders {
    private static final List<InfoList<?>> INFO_LISTS = new ArrayList<>();

    public static PacksInfoLoader PACKS = new PacksInfoLoader("pack_info", (p, n) -> new PackInfo(p, n, ContentPackType.NOTSET), new SubInfoTypesRegistry<>());

    public static ObjectLoader<ModularVehicleInfo, ItemCar> WHEELED_VEHICLES = new ObjectLoader<>("vehicle_", CarInfo::new, new SubInfoTypesRegistry<>());
    public static ObjectLoader<ModularVehicleInfo, ItemTrailer> TRAILERS = new ObjectLoader<>("trailer_", (packName, fileName) -> new ModularVehicleInfo(packName, fileName, VehicleValidator.TRAILER_VALIDATOR), WHEELED_VEHICLES.getDefaultSubInfoTypesRegistry());
    public static ObjectLoader<ModularVehicleInfo, ItemBoat> BOATS = new ObjectLoader<>("boat_", (packName, fileName) -> new ModularVehicleInfo(packName, fileName, VehicleValidator.BOAT_VALIDATOR), WHEELED_VEHICLES.getDefaultSubInfoTypesRegistry());
    public static ObjectLoader<ModularVehicleInfo, ItemHelicopter> HELICOPTERS = new ObjectLoader<>("helicopter_", (packName, fileName) -> new ModularVehicleInfo(packName, fileName, VehicleValidator.HELICOPTER_VALIDATOR), new SubInfoTypesRegistry<>());
    public static ObjectLoader<BlockObject<?>, DynamXBlock<?>> BLOCKS = new ObjectLoader<>("block", BlockObject::new, new SubInfoTypesRegistry<>());
    public static ObjectLoader<ItemObject<?>, DynamXItem<?>> ITEMS = new ObjectLoader<>("item", ItemObject::new, new SubInfoTypesRegistry<>());
    public static ObjectLoader<ArmorObject<?>, DynamXItemArmor<?>> ARMORS = new ObjectLoader<>("armor", ArmorObject::new, new SubInfoTypesRegistry<>());
    public static PropsLoader<PropObject<?>> PROPS = new PropsLoader<>();
    public static InfoLoader<PartWheelInfo> WHEELS = new InfoLoader<>("wheel", PartWheelInfo::new, new SubInfoTypesRegistry<>());
    public static LateInfoLoader<BaseEngineInfo> ENGINES = new LateInfoLoader<>("engine", ((pack, name, clazz) -> {
        if(Objects.equals(clazz, CarEngineInfo.class.toString()))
            return new CarEngineInfo(pack, name);
        else if(Objects.equals(clazz, BaseEngineInfo.class.toString()))
            return new BaseEngineInfo(pack, name);
        else if(Objects.equals(clazz, BoatEngineInfo.class.toString()))
            return new BoatEngineInfo(pack, name);
        else
            throw new IllegalArgumentException("Unknown engine class: " + clazz);
    }), new SubInfoTypesRegistry<>());
    public static SoundInfoLoader SOUNDS = new SoundInfoLoader("sounds", SoundListInfo::new);

    static {
        DynamXObjectLoaders.getInfoLists().add(DynamXObjectLoaders.SOUNDS);
    }

    public static List<InfoList<?>> getInfoLists() {
        return INFO_LISTS;
    }

    public static List<InfoLoader<?>> getInfoLoaders() {
        return Collections.unmodifiableList(INFO_LISTS.stream().filter(l -> l instanceof InfoLoader).map(l -> (InfoLoader<?>) l).collect(Collectors.toList()));
    }
}
