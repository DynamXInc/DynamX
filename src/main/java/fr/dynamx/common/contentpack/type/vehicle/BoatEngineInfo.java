package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import lombok.Getter;
import lombok.Setter;

@RegisteredSubInfoType(name = "boat_engine", registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
public class BoatEngineInfo extends SubInfoType<ModularVehicleInfo> {
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    @Getter
    private Vector3f position;
    @PackFileProperty(configNames = "AccelerationForceFactor")
    @Getter
    private float accelerationFactor;
    @PackFileProperty(configNames = "BrakeForceFactor")
    @Getter
    private float brakeFactor;
    @PackFileProperty(configNames = "SteerForceFactor")
    @Getter
    private float steerFactor;
    @PackFileProperty(configNames = "AccelerationForce", required = false)
    @Getter
    private float accelerationForce;
    @PackFileProperty(configNames = "BrakeForce", required = false)
    @Getter
    private float brakeForce;
    @PackFileProperty(configNames = "SteerForce", required = false)
    @Getter
    private float steerForce;

    public BoatEngineInfo(ModularVehicleInfo owner) {
        super(owner);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        if (getPosition() == null)
            throw new IllegalArgumentException("AttachPoint not configured ! In trailer of " + owner.toString());
        owner.addSubProperty(this);
    }

    @Override
    public String getName() {
        return "BoatEngineInfo";
    }
}