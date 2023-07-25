package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import lombok.Getter;

@RegisteredSubInfoType(name = "BoatPropeller", registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
public class BoatPropellerInfo extends SubInfoType<ModularVehicleInfo> {
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    @Getter
    private Vector3f position;
    @PackFileProperty(configNames = {"AccelerationForce", "ForwardForce"})
    @Getter
    private float accelerationForce;
    @PackFileProperty(configNames = {"BrakeForce", "BackwardForce"})
    @Getter
    private float brakeForce;
    @PackFileProperty(configNames = "SteerForce")
    @Getter
    private float steerForce;
    @PackFileProperty(configNames = "LinearDamping", required = false, defaultValue = "0.6")
    @Getter
    private float linearDamping = 0.6f;
    @PackFileProperty(configNames = "AngularDamping", required = false, defaultValue = "0.7")
    @Getter
    private float angularDamping = 0.6f;

    public BoatPropellerInfo(ModularVehicleInfo owner) {
        super(owner);
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        if (getPosition() == null)
            throw new IllegalArgumentException("Position not configured ! In boat propeller of " + owner.toString());
        owner.addSubProperty(this);
    }

    @Override
    public String getName() {
        return "BoatPropeller";
    }
}