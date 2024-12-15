package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import lombok.Getter;

/**
 * Power/torque point of the rpm graph of an {@link BaseEngineInfo}
 */
@Getter
@RegisteredSubInfoType(name = "point", registries = SubInfoTypeRegistries.CAR_ENGINES, strictName = false)
public class RPMPower extends SubInfoType<BaseEngineInfo> {
    /**
     * Newer torque support (and better physically)
     */
    @PackFileProperty(configNames = "RPMTorque", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_0Z, required = false)
    private Vector3f rpmTorque; //It's a Vector3f because of the Spline
    /**
     * Legacy power curve support
     */
    @PackFileProperty(configNames = "RPMPower", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_0Z, required = false)
    private Vector3f rpmPower; //It's a Vector3f because of the Spline

    public RPMPower(ISubInfoTypeOwner<BaseEngineInfo> owner) {
        super(owner);
    }

    @Override
    public void appendTo(BaseEngineInfo owner) {
        if (rpmTorque == null && rpmPower == null) {
            throw new IllegalArgumentException("Either RPMTorque or RPMPower must be specified in " + getName());
        }
        owner.addPoint(rpmPower != null ? BaseEngineInfo.EngineConfigType.POWER : BaseEngineInfo.EngineConfigType.TORQUE, this);
    }

    @Override
    public String getName() {
        return "RPM point";
    }
}
