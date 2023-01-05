package fr.dynamx.common.contentpack.parts;

import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.IModuleContainer;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.physics.entities.modules.HelicopterPhysicsHandler;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import lombok.Getter;


@RegisteredSubInfoType(name = "handle", registries = {SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartHandle extends BasePart<ModularVehicleInfo> {

    @Getter
    @PackFileProperty(configNames = "PartName")
    private String PartName = "Handle";

    @Override
    public void appendTo(ModularVehicleInfo owner) {
        super.appendTo(owner);
        owner.addRenderedParts(PartName);
    }

    public PartHandle(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
    }

    @Override
    public void addPart(BaseVehicleEntity<?> vehicle) {
        //todo clean condition
        if(!(vehicle instanceof IModuleContainer.IPropulsionContainer) || !(((IModuleContainer.IPropulsionContainer) vehicle).getPropulsion().getPhysicsHandler()  instanceof HelicopterPhysicsHandler))
            throw new IllegalStateException("The entity " + vehicle + " has PartRotor, but does not implement IHavePropulsion or the propulsion is not a RotorModule !");
        ((HelicopterPhysicsHandler) ((IModuleContainer.IPropulsionContainer<?>) vehicle).getPropulsion().getPhysicsHandler()).addHandle(this);
    }

    @Override
    public void removePart(BaseVehicleEntity<?> vehicle) {
        ((HelicopterPhysicsHandler) ((IModuleContainer.IPropulsionContainer<?>) vehicle).getPropulsion().getPhysicsHandler()).removeHandle(this);
    }

    @Override
    public DynamXDebugOption getDebugOption() {
        return DynamXDebugOptions.HANDLES;
    }

    @Override
    public String getName() {
        return "PartRotor named " + getPartName();
    }

}
