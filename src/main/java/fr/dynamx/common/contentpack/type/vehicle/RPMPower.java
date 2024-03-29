package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;

/**
 * Power point of the rpm graph of an {@link BaseEngineInfo}
 */
@RegisteredSubInfoType(name = "point", registries = SubInfoTypeRegistries.CAR_ENGINES, strictName = false)
public class RPMPower extends SubInfoType<BaseEngineInfo> {
    @PackFileProperty(configNames = "RPMPower", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_0Z)
    private Vector3f rpmPower; //It's a Vector3f because of the Spline

    public RPMPower(ISubInfoTypeOwner<BaseEngineInfo> owner) {
        super(owner);
    }

    public Vector3f getRpmPower() {
        return rpmPower;
    }

    @Override
    public void appendTo(BaseEngineInfo owner) {
        owner.addPoint(this);
    }

    @Override
    public String getName() {
        return "RPM point";
    }
}
