package fr.dynamx.common.contentpack.type.vehicle;

import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;
import fr.dynamx.common.entities.modules.CarEngineModule;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Engine contained in an engine file
 */
public class CarEngineInfo extends BaseEngineInfo {
    @Getter
    @Setter
    @PackFileProperty(configNames = "SteeringMethod", required = false, defaultValue = "0", description = "The steering method of the vehicle. 0: direct, 1: interpolated, 2: mix of both")
    public int steeringMethod = 0;

    @Getter
    @Setter
    @PackFileProperty(configNames = "TurnSpeed", required = false, defaultValue = "0.09", description = "The wheel's rotation speed")
    public float turnSpeed = 0.09f;

    public List<GearInfo> gears = new ArrayList<>();

    byte i = 0;

    public CarEngineInfo(String packName, String name) {
        super(packName, name);
    }

    void addGear(GearInfo gear) {
        gear.setId(i);
        gears.add(i, gear);
        i++;
    }

    @Override
    public void addModules(PackPhysicsEntity<?, ?> entity, ModuleListBuilder modules) {
        modules.add(new CarEngineModule((BaseVehicleEntity<?>) entity, this));
    }
}

