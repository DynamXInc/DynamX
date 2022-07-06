package fr.dynamx.api.contentpack.registry;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.contentpack.PackInfo;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;
import fr.dynamx.common.contentpack.parts.*;
import fr.dynamx.common.contentpack.type.ParticleEmitterInfo;
import fr.dynamx.common.contentpack.type.objects.PropObject;
import fr.dynamx.common.contentpack.type.vehicle.*;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry for ModularVehicles sub info categories, such as shapes, wheels, seats, trailer attach or steering wheel <br>
 * You can create your custom registries
 *
 * @see SubInfoTypeEntry
 * @see ISubInfoTypeOwner
 * @see fr.dynamx.api.contentpack.object.subinfo.ISubInfoType
 */
public class SubInfoTypesRegistry<T extends ISubInfoTypeOwner<?>> {
    private final Map<String, SubInfoTypeEntry<T>> ENTRIES = new LinkedHashMap<>();

    /*
    Built-in DynamX sub info types
     */
    public static void initBuiltinSubInfoTypes() {
        SubInfoTypesRegistry<ModularVehicleInfoBuilder> wheeledVehicles = DynamXObjectLoaders.WHEELED_VEHICLES.getSubInfoTypesRegistry();
        wheeledVehicles.addSubInfoType(new SubInfoTypeEntry<>("shape", PartShape::new, false));
        wheeledVehicles.addSubInfoType(new SubInfoTypeEntry<>("trailer", TrailerAttachInfo.class));
        wheeledVehicles.addSubInfoType(new SubInfoTypeEntry<>("steeringwheel", SteeringWheelInfo.class));
        wheeledVehicles.addSubInfoType(new SubInfoTypeEntry<>("wheel", PartWheel::new, false));
        wheeledVehicles.addSubInfoType(new SubInfoTypeEntry<>("caterpillar", CaterpillarInfo.class));
        wheeledVehicles.addSubInfoType(new SubInfoTypeEntry<>("seat", PartSeat::new, false));
        wheeledVehicles.addSubInfoType(new SubInfoTypeEntry<>("door", PartDoor::new, false));
        wheeledVehicles.addSubInfoType(new SubInfoTypeEntry<>("light", PartLightSource::new, false));
        wheeledVehicles.addSubInfoType(new SubInfoTypeEntry<>("storage", PartStorage::new));
        wheeledVehicles.addSubInfoType(new SubInfoTypeEntry<>("propscontainer", PartPropsContainer::new));
        wheeledVehicles.addSubInfoType(new SubInfoTypeEntry<>("forcepoint", FrictionPoint.class, false));
        wheeledVehicles.addSubInfoType(new SubInfoTypeEntry<>("emitter", ParticleEmitterInfo::new, false));

        wheeledVehicles.addSubInfoType(new SubInfoTypeEntry<>("boat_engine", BoatEngineInfo.class));
        wheeledVehicles.addSubInfoType(new SubInfoTypeEntry<>("float", PartFloat::new, false));

        DynamXObjectLoaders.ENGINES.addSubInfoType(new SubInfoTypeEntry<>("gear", GearInfo::new, false));
        DynamXObjectLoaders.ENGINES.addSubInfoType(new SubInfoTypeEntry<>("point", RPMPower.class, false));

        DynamXObjectLoaders.BLOCKS.addSubInfoType(new SubInfoTypeEntry<>("shape", PartShape::new, false));
        DynamXObjectLoaders.BLOCKS.addSubInfoType(new SubInfoTypeEntry<>("prop", PropObject::new, false));

        DynamXObjectLoaders.PACKS.addSubInfoType(new SubInfoTypeEntry<>("RequiredAddon", PackInfo.RequiredAddonInfo::new, false));
        //DynamXObjectLoaders.PROPS.addSubInfoType(new SubInfoTypeEntry<>("shape", PartShape::new, false));
    }

    /**
     * Registers a sub info type, its key should be unique or an IllegalArgumentException is thrown <br>
     * Should be called before any content pack is loaded (in addons initialization for example)
     */
    public void addSubInfoType(SubInfoTypeEntry<T> entry) {
        if (ENTRIES.containsKey(entry.getKey()))
            throw new IllegalArgumentException("Sub info type entry with name " + entry.getKey() + " is already registered !");
        ENTRIES.put(entry.getKey(), entry);
    }

    /**
     * @return A collection of all registered sub info types
     */
    public Collection<SubInfoTypeEntry<T>> getRegisteredEntries() {
        return ENTRIES.values();
    }
}
