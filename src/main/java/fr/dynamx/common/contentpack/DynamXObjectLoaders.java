package fr.dynamx.common.contentpack;

import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.contentpack.loader.*;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.contentpack.type.objects.ItemObject;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.contentpack.type.vehicle.EngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.contentpack.type.vehicle.PartWheelInfo;
import fr.dynamx.common.contentpack.type.vehicle.SoundListInfo;
import fr.dynamx.common.items.DynamXItem;
import fr.dynamx.common.items.DynamXItemArmor;
import fr.dynamx.common.items.ItemHelicopter;
import fr.dynamx.common.items.ItemProps;
import fr.dynamx.common.items.vehicle.ItemBoat;
import fr.dynamx.common.items.vehicle.ItemCar;
import fr.dynamx.common.items.vehicle.ItemTrailer;

import java.util.ArrayList;
import java.util.List;

/**
 * All DynamX {@link InfoLoader}s
 */
public class DynamXObjectLoaders {
    static final List<InfoLoader<?>> LOADERS = new ArrayList<>();

    public static PacksInfoLoader PACKS = new PacksInfoLoader("pack_info", (p, n) -> new PackInfo(p, ContentPackType.FOLDER), new SubInfoTypesRegistry<>());

    public static ObjectLoader<ModularVehicleInfo, ItemCar> WHEELED_VEHICLES = new ObjectLoader<>("vehicle_", ModularVehicleInfo::new, ItemCar::getItemForCar, new SubInfoTypesRegistry<>());
    public static ObjectLoader<ModularVehicleInfo, ItemTrailer> TRAILERS = new ObjectLoader<>("trailer_", ModularVehicleInfo::new, ItemTrailer::new, WHEELED_VEHICLES.getSubInfoTypesRegistry());
    public static ObjectLoader<ModularVehicleInfo, ItemBoat> BOATS = new ObjectLoader<>("boat_", ModularVehicleInfo::new, ItemBoat::new, WHEELED_VEHICLES.getSubInfoTypesRegistry());
    public static ObjectLoader<ModularVehicleInfo, ItemHelicopter> HELICOPTERS = new ObjectLoader<>("helicopter_", ModularVehicleInfo::new, ItemHelicopter::getItemForCar, new SubInfoTypesRegistry<>());
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
