package fr.dynamx.common.contentpack.parts;

import com.jme3.math.Vector3f;
import fr.dynamx.api.contentpack.object.part.BasePart;
import fr.dynamx.api.contentpack.registry.*;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.HelicopterPartModule;
import fr.dynamx.utils.debug.DynamXDebugOption;
import fr.dynamx.utils.debug.DynamXDebugOptions;
import lombok.Getter;


@RegisteredSubInfoType(name = "rotor", registries = {SubInfoTypeRegistries.HELICOPTER}, strictName = false)
public class PartRotor extends BasePart<ModularVehicleInfo> {

    @Getter
    @PackFileProperty(configNames = "Rotation", required = false)
    private Vector3f Rotation = new Vector3f();
    @Getter
    @PackFileProperty(configNames = "RotationSpeed", required = false, defaultValue = "0.0")
    private float RotationSpeed = 15.0f;
    @Getter
    @PackFileProperty(configNames = "PartName")
    private String PartName = "Rotor";


    @Override
    public void appendTo(ModularVehicleInfo owner) {
        super.appendTo(owner);
        owner.addRenderedParts(PartName);
    }

    @Override
    public void addModules(BaseVehicleEntity<?> entity, ModuleListBuilder modules) {
        if(!modules.hasModuleOfClass(HelicopterPartModule.class)) {
            modules.add(new HelicopterPartModule(entity));
        }

    }

    public PartRotor(ModularVehicleInfo owner, String partName) {
        super(owner, partName);
    }

    @Override
    public DynamXDebugOption getDebugOption() {
        return DynamXDebugOptions.ROTORS;
    }

    @Override
    public String getName() {
        return "PartRotor named " + getPartName();
    }


}
