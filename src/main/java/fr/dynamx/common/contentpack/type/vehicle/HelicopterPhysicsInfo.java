package fr.dynamx.common.contentpack.type.vehicle;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.contentpack.DynamXObjectLoaders;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.HelicopterEngineModule;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@RegisteredSubInfoType(name = "HelicopterPhysics", registries = {SubInfoTypeRegistries.HELICOPTER})
public class HelicopterPhysicsInfo extends SubInfoType<ModularVehicleInfo> {

    @Getter
    @PackFileProperty(configNames = "MinPower", defaultValue = "0.4f")
    private float minPower = 0.4f;
    @Getter
    @PackFileProperty(configNames = "InclinedGravityFactor", defaultValue = "1.8f")
    private float inclinedGravityFactor = 1.8f;
    @Getter
    @PackFileProperty(configNames = "ThrustForce", defaultValue = "3000")
    private float thrustForce = 3000;
    @Getter
    @PackFileProperty(configNames = "VerticalThrustCompensation", defaultValue = "2000")
    private float verticalThrustCompensation = 2000;
    @Getter
    @PackFileProperty(configNames = "BrakeForce", defaultValue = "200")
    private float brakeForce = 500;
    @Getter
    @PackFileProperty(configNames = "MouseYawForce", defaultValue = "2600")
    private float mouseYawForce = 2600;
    @Getter
    @PackFileProperty(configNames = "MousePitchForce", defaultValue = "2000")
    private float mousePitchForce = 2000;
    @Getter
    @PackFileProperty(configNames = "MouseRollForce", defaultValue = "400")
    private float mouseRollForce = 400;
    @Getter
    @PackFileProperty(configNames = "RollForce", defaultValue = "6000")
    private float rollForce = 6000;

    public HelicopterPhysicsInfo(ISubInfoTypeOwner<ModularVehicleInfo> owner) {
        super(owner);
    }

    @Override
    public String getName() {
        return "HelicopterPhysics of " + getOwner().getName();
    }

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        owner.addSubProperty(this);
    }

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        modules.add(new HelicopterEngineModule((BaseVehicleEntity<?>) entity));
    }
}
