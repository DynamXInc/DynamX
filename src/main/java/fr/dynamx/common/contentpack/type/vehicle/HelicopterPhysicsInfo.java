package fr.dynamx.common.contentpack.type.vehicle;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.engines.HelicopterEngineModule;
import fr.dynamx.common.entities.modules.engines.PlaneEngineModule;
import fr.dynamx.common.entities.vehicles.PlaneEntity;
import lombok.Getter;

@Getter
@RegisteredSubInfoType(name = "HelicopterPhysics", registries = {SubInfoTypeRegistries.HELICOPTER})
public class HelicopterPhysicsInfo extends SubInfoType<ModularVehicleInfo> {
    @PackFileProperty(configNames = "MinPower", defaultValue = "0.4f")
    protected float minPower = 0.4f;
    @PackFileProperty(configNames = "InclinedGravityFactor", defaultValue = "1.8f")
    protected float inclinedGravityFactor = 1.8f;
    @PackFileProperty(configNames = "ThrustForce", defaultValue = "3000")
    protected float thrustForce = 3000;
    @PackFileProperty(configNames = "VerticalThrustCompensation", defaultValue = "2000")
    protected float verticalThrustCompensation = 2000;
    @PackFileProperty(configNames = "BrakeForce", defaultValue = "200")
    protected float brakeForce = 500;
    @PackFileProperty(configNames = "MouseYawForce", defaultValue = "2600")
    protected float mouseYawForce = 2600;
    @PackFileProperty(configNames = "MousePitchForce", defaultValue = "2000")
    protected float mousePitchForce = 2000;
    @PackFileProperty(configNames = "MouseRollForce", defaultValue = "400")
    protected float mouseRollForce = 400;
    @PackFileProperty(configNames = "RollForce", defaultValue = "6000")
    protected float rollForce = 6000;
    @PackFileProperty(configNames = "EngineStartupTime", defaultValue = "300 (15 secondes)")
    protected int engineStartupTime = 20 * 15;

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
        if (entity instanceof PlaneEntity) {
            modules.add(new PlaneEngineModule((BaseVehicleEntity<?>) entity));
        } else {
            modules.add(new HelicopterEngineModule((BaseVehicleEntity<?>) entity));
        }
    }
}
