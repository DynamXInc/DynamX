package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;

@RegisteredSubInfoType(name = "boat_engine", registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
/**
 * Info of the trailer attach point of a vehicle
 */
public class BoatEngineInfo extends SubInfoType<ModularVehicleInfoBuilder> {
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    private Vector3f position;

    public BoatEngineInfo(ISubInfoTypeOwner<ModularVehicleInfoBuilder> owner) {
        super(owner);
    }

    @Override
    public void appendTo(ModularVehicleInfoBuilder owner) {
        if (getPosition() == null)
            throw new IllegalArgumentException("AttachPoint not configured ! In trailer of " + owner.toString());
        owner.addSubProperty(this);
    }

    public Vector3f getPosition() {
        return position;
    }

    @Override
    public String getName() {
        return "BoatEngineInfo";
    }
}