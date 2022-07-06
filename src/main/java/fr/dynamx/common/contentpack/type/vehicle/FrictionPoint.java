package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.common.contentpack.loader.ModularVehicleInfoBuilder;

public class FrictionPoint extends SubInfoType<ModularVehicleInfoBuilder>
{
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y)
    private Vector3f position;
    @PackFileProperty(configNames = "Intensity", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F)
    private Vector3f intensity;

    public FrictionPoint(ISubInfoTypeOwner<ModularVehicleInfoBuilder> owner) {
        super(owner);
    }

    @Override
    public String getName() {
        return "FrictionPoint in "+getOwner().getName();
    }

    @Override
    public void appendTo(ModularVehicleInfoBuilder owner) {
        owner.addFrictionPoint(this);
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getIntensity() {
        return intensity;
    }
}
