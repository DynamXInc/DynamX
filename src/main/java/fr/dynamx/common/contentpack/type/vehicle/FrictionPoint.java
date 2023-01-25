package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;

@RegisteredSubInfoType(name = "forcepoint", registries = {SubInfoTypeRegistries.WHEELED_VEHICLES, SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class FrictionPoint extends SubInfoType<ModularVehicleInfo> {
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    private Vector3f position;
    @PackFileProperty(configNames = "Intensity", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F)
    private Vector3f intensity;

    public FrictionPoint(ModularVehicleInfo owner) {
        super(owner);
    }

    @Override
    public String getName() {
        return "FrictionPoint";
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        owner.addFrictionPoint(this);
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getIntensity() {
        return intensity;
    }
}
