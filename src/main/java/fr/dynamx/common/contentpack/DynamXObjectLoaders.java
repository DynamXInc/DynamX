package fr.dynamx.common.contentpack;

import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.contentpack.loader.*;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.contentpack.type.objects.ItemObject;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.contentpack.type.vehicle.*;
import fr.dynamx.common.items.DynamXItem;
import fr.dynamx.common.items.DynamXItemArmor;
import fr.dynamx.common.items.vehicle.*;
import fr.dynamx.common.items.ItemProps;

import java.util.ArrayList;
import java.util.List;

/**
 * All DynamX {@link InfoLoader}s
 */
public class DynamXObjectLoaders {
    static final List<InfoLoader<?>> LOADERS = new ArrayList<>();

    public static PacksInfoLoader PACKS = new PacksInfoLoader("pack_info", (p, n) -> new PackInfo(p, ContentPackType.NOTSET), new SubInfoTypesRegistry<>());

    public static ObjectLoader<ModularVehicleInfo, ItemCar> WHEELED_VEHICLES = new ObjectLoader<>("vehicle_", (packName, fileName) -> new ModularVehicleInfo(packName, fileName, VehicleValidator.CAR_VALIDATOR), ItemCar::getItemForCar, new SubInfoTypesRegistry<>());
    public static ObjectLoader<ModularVehicleInfo, ItemTrailer> TRAILERS = new ObjectLoader<>("trailer_", (packName, fileName) -> new ModularVehicleInfo(packName, fileName, VehicleValidator.TRAILER_VALIDATOR), ItemTrailer::new, WHEELED_VEHICLES.getSubInfoTypesRegistry());
    public static ObjectLoader<ModularVehicleInfo, ItemBoat> BOATS = new ObjectLoader<>("boat_", (packName, fileName) -> new ModularVehicleInfo(packName, fileName, VehicleValidator.BOAT_VALIDATOR), ItemBoat::new, WHEELED_VEHICLES.getSubInfoTypesRegistry());
    public static ObjectLoader<ModularVehicleInfo, ItemHelicopter> HELICOPTERS = new ObjectLoader<>("helicopter_", ModularVehicleInfo::new, ItemHelicopter::getItemForCar, new SubInfoTypesRegistry<>());
    public static ObjectLoader<ModularVehicleInfo, ItemPlane> PLANE = new ObjectLoader<>("plane_", ModularVehicleInfo::new, ItemPlane::getItemForCar, new SubInfoTypesRegistry<>());
    public static ObjectLoader<BlockObject<?>, DynamXBlock<?>> BLOCKS = new ObjectLoader<>("block", BlockObject::new, new SubInfoTypesRegistry<>());
    public static ObjectLoader<ItemObject<?>, DynamXItem<?>> ITEMS = new ObjectLoader<>("item", ItemObject::new, new SubInfoTypesRegistry<>());
    public static ObjectLoader<ArmorObject<?>, DynamXItemArmor<?>> ARMORS = new ObjectLoader<>("armor", ArmorObject::new, new SubInfoTypesRegistry<>());
    public static ObjectLoader<PropObject<?>, ItemProps<?>> PROPS = new ObjectLoader<>("prop", PropObject::new, new SubInfoTypesRegistry<>());
    public static InfoLoader<PartWheelInfo> WHEELS = new InfoLoader<>("wheel", PartWheelInfo::new, new SubInfoTypesRegistry<>());
    public static InfoLoader<EngineInfo> ENGINES = new InfoLoader<>("engine", EngineInfo::new, new SubInfoTypesRegistry<>());
    public static SoundInfoLoader SOUNDS = new SoundInfoLoader("sounds", SoundListInfo::new);

    static {
        DynamXObjectLoaders.getLoaders().add(DynamXObjectLoaders.SOUNDS);
    }

    public static List<InfoLoader<?>> getLoaders() {
        return LOADERS;
    }
}
