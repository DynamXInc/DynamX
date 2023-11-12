package fr.dynamx.common.contentpack.type.vehicle;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.subinfo.SubInfoType;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.BoatPropellerModule;
import fr.dynamx.common.entities.vehicles.BoatEntity;
import lombok.Getter;

@RegisteredSubInfoType(name = "BoatPropeller", registries = SubInfoTypeRegistries.WHEELED_VEHICLES)
public class BoatPropellerInfo extends SubInfoType<ModularVehicleInfo> {
    @PackFileProperty(configNames = "Position", type = DefinitionType.DynamXDefinitionTypes.VECTOR3F_INVERSED_Y, description = "common.position")
    @Getter
    private Vector3f position;
    @PackFileProperty(configNames = {"AccelerationForce", "ForwardForce"}, description = "BoatPropellerInfo.AccelerationForce")
    @Getter
    private float accelerationForce;
    @PackFileProperty(configNames = {"BrakeForce", "BackwardForce"}, description = "BoatPropellerInfo.BrakeForce")
    @Getter
    private float brakeForce;
    @PackFileProperty(configNames = "SteerForce")
    @Getter
    private float steerForce;

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

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        if(!(entity instanceof BoatEntity))
            throw new IllegalStateException("The entity " + entity + " has PartSeats, but isn't a boat !");
        if(!modules.hasModuleOfClass(BoatPropellerModule.class))
            modules.add(new BoatPropellerModule((BoatEntity<?>) entity));
    }
}