package fr.dynamx.common.contentpack;

import fr.dynamx.api.contentpack.ContentPackType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.common.contentpack.loader.SubInfoTypesRegistry;
import fr.dynamx.common.blocks.DynamXBlock;
import fr.dynamx.common.contentpack.loader.*;
import fr.dynamx.common.contentpack.type.vehicle.*;
import fr.dynamx.common.contentpack.type.objects.ArmorObject;
import fr.dynamx.common.contentpack.type.objects.BlockObject;
import fr.dynamx.common.contentpack.type.objects.ItemObject;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.items.DynamXItem;
import fr.dynamx.common.items.DynamXItemArmor;
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
    static final List<InfoLoader<?, ?>> LOADERS = new ArrayList<>();

    public static PacksInfoLoader PACKS = new PacksInfoLoader("pack_info", (p, n) -> new PackInfo(p, ContentPackType.FOLDER), new SubInfoTypesRegistry<>());

    public static BuildableInfoLoader<ModularVehicleInfoBuilder, ModularVehicleInfo<?>, ItemCar<?>> WHEELED_VEHICLES = new BuildableInfoLoader<>("vehicle_", ModularVehicleInfoBuilder::new, ItemCar::getItemForCar, new SubInfoTypesRegistry<>());
    public static BuildableInfoLoader<ModularVehicleInfoBuilder, ModularVehicleInfo<?>, ItemTrailer<?>> TRAILERS = new BuildableInfoLoader<>("trailer_", ModularVehicleInfoBuilder::new, ItemTrailer::new, WHEELED_VEHICLES.getSubInfoTypesRegistry());
    public static BuildableInfoLoader<ModularVehicleInfoBuilder, ModularVehicleInfo<?>, ItemBoat<?>> BOATS = new BuildableInfoLoader<>("boat_", ModularVehicleInfoBuilder::new, ItemBoat::new, WHEELED_VEHICLES.getSubInfoTypesRegistry());
    public static ObjectLoader<BlockObject<?>, DynamXBlock<?>, BlockObject<?>> BLOCKS = new ObjectLoader<>("block", BlockObject::new, new SubInfoTypesRegistry<>());
    public static ObjectLoader<ItemObject<?>, DynamXItem<?>, ISubInfoTypeOwner.Empty> ITEMS = new ObjectLoader<>("item", ItemObject::new, new SubInfoTypesRegistry<>());
    public static ObjectLoader<ArmorObject<?>, DynamXItemArmor<?>, ArmorObject<?>> ARMORS = new ObjectLoader<>("armor", ArmorObject::new, new SubInfoTypesRegistry<>());
    public static ObjectLoader<PropObject<?>, ItemProps<?>, PropObject<?>> PROPS = new ObjectLoader<>("prop", PropObject::new, new SubInfoTypesRegistry<>());
    public static InfoLoader<PartWheelInfo, PartWheelInfo> WHEELS = new InfoLoader<>("wheel", PartWheelInfo::new, new SubInfoTypesRegistry<>());
    public static InfoLoader<EngineInfo, EngineInfo> ENGINES = new InfoLoader<>("engine", EngineInfo::new, new SubInfoTypesRegistry<>());
    public static SoundInfoLoader SOUNDS = new SoundInfoLoader("sounds", SoundListInfo::new);

    static {
        DynamXObjectLoaders.getLoaders().add(DynamXObjectLoaders.SOUNDS);
    }

    public static List<InfoLoader<?, ?>> getLoaders() {
        return LOADERS;
    }
}
